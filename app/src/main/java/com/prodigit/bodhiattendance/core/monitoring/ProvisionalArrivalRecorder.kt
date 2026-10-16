package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.location.Location
import android.os.SystemClock
import androidx.core.location.LocationCompat
import com.google.gson.Gson
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.entity.ProvisionalArrivalEntity
import com.prodigit.bodhiattendance.data.local.security.BootSessionManager
import com.prodigit.bodhiattendance.data.remote.*
import com.prodigit.bodhiattendance.domain.attendance.*
import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/** Separate Room outbox: no attendance events or runtime transitions are created here. */
class ProvisionalArrivalRecorder(context: Context) {
    private val app = context.applicationContext
    private val db = AttendanceDatabase.getInstance(app)
    private val dao = db.provisionalArrivalDao()
    private val session = SessionManager(app)
    private val devices = DeviceIdManager(app)
    private val gson = Gson()

    suspend fun record(location: Location, office: AttendanceLocationConfig, distance: Double, disconnected: Boolean) {
        val user = session.getUserId()
        val company = session.getCompanyId()
        val device = devices.getDeviceId() ?: return
        val boot = BootSessionManager(app).getBootId()
        val provider = AttendanceEngineRuntimeProvider.getInstance(app)
        val state = provider.runtimeSession.getSnapshot()
        val cachedAnchor = provider.trustedTimeManager.getCurrentAnchor() ?: return
        val anchor = db.timeAnchorDao().getTimeAnchorByUuid(cachedAnchor.anchorUuid) ?: return
        val observed = location.elapsedRealtimeNanos / 1_000_000L
        val now = SystemClock.elapsedRealtime()
        if (GpsEvidenceStatus.rejection(location) != null || observed < anchor.elapsedRealtimeMs) return
        val trusted = runCatching { Math.addExact(Instant.parse(anchor.serverTime).toEpochMilli(), observed - anchor.elapsedRealtimeMs) }.getOrNull() ?: return
        val permit = OfflineWorkSessionStore(app).permitForEvent(user, company, device, boot, trusted) ?: return
        if (!ProvisionalArrivalPolicy.canSave(
                state.mode == AttendanceEngineRuntimeMode.READY && state.userId == user && state.deviceId == device && state.attendanceState == AttendanceState.NOT_CHECKED_IN,
                office.wifiRequired == true, office.bleRequired != false, disconnected, LocationCompat.isMock(location),
                distance, location.accuracy.toDouble(), office.geofenceRadiusMeters, office.maxAccuracyMeters ?: Double.NaN,
                observed, now, anchor.elapsedRealtimeMs, anchor.bootId == boot && anchor.deviceId == device, true)) return
        val id = UUID.randomUUID().toString()
        val trustedNow = Math.addExact(trusted, now - observed)
        val item = ProvisionalArrivalItem(id, office.id, boot, anchor.anchorUuid, observed, now,
            location.latitude, location.longitude, location.accuracy.toDouble(), false, true,
            System.currentTimeMillis() - trustedNow, permit)
        val day = Instant.ofEpochMilli(trusted).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate().toString()
        val saved = dao.recordFirst(ProvisionalArrivalEntity(id, user, company, device, day, office.id, trusted, gson.toJson(item)))
        if (saved != -1L) {
            AttendanceDiagnostics(app).record("PROVISIONAL_ARRIVAL_SAVED",
                "GPS-only arrival saved for manager review; id=$id; officeId=${office.id}; trustedTime=${Instant.ofEpochMilli(trusted)}; Wi-Fi unavailable; cause unverified. No confirmed check-in.", observed)
        }
        AttendanceBackgroundSync.schedule(app)
    }

    suspend fun upload(): Boolean = uploadMutex.withLock {
        val user = session.getUserId()
        val device = devices.getDeviceId() ?: return@withLock true
        if (user <= 0) return@withLock true
        val rows = dao.pending(user, device)
        if (rows.isEmpty()) return@withLock true
        val token = AndroidTokenManager(app).usableAccessToken() ?: return@withLock false
        var complete = true
        for (row in rows) {
            // Never upload another signed-in user's outbox using a changed session.
            if (session.getUserId() != user || devices.getDeviceId() != device) return@withLock false
            try {
                val payload = gson.fromJson(row.payloadJson, ProvisionalArrivalItem::class.java)
                val reply = RetrofitClient.presenceEvidenceApi.uploadProvisional("Bearer $token", ProvisionalArrivalUpload(device, payload))
                val body = reply.body()
                if (reply.isSuccessful && body?.success == true && body.requestId != null) {
                    dao.acknowledge(row.id, user, device, "UPLOADED", body.requestId, "Uploaded to CRM. Open Requests for the manager’s decision.")
                    AttendanceDiagnostics(app).record("PROVISIONAL_ARRIVAL_UPLOADED", "Arrival ${row.id} linked to review request ${body.requestId}; attendance is not automatically confirmed")
                } else if (reply.code() in setOf(409, 422)) {
                    dao.acknowledge(row.id, user, device, "MANUAL_REVIEW_REQUIRED", null,
                        "Original evidence could not be validated by CRM. Submit an attendance regularization request; this record remains saved.")
                    AttendanceDiagnostics(app).record("PROVISIONAL_ARRIVAL_REVIEW_REQUIRED", "Arrival ${row.id} needs manual regularization; HTTP ${reply.code()}")
                } else complete = false
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { complete = false }
        }
        complete && dao.pending(user, device).isEmpty()
    }
    companion object { private val uploadMutex = Mutex() }
}

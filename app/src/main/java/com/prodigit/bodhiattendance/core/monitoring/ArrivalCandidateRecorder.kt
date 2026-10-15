package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.entity.ArrivalCandidateEntity
import com.prodigit.bodhiattendance.data.remote.ArrivalCandidateBatch
import com.prodigit.bodhiattendance.data.remote.ArrivalCandidateItem
import com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeProvider
import com.prodigit.bodhiattendance.domain.attendance.AttendanceState
import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/** Approved Wi-Fi is arrival evidence only, never permission to backdate CHECK_IN. */
class ArrivalCandidateRecorder(context: Context) {
    private val app = context.applicationContext
    private val session = SessionManager(app)
    private val devices = DeviceIdManager(app)
    private val dao = AttendanceDatabase.getInstance(app).arrivalCandidateDao()
    private val provider = AttendanceEngineRuntimeProvider.getInstance(app)

    suspend fun record(bssid: String?, observedElapsedMs: Long, offices: List<AttendanceLocationConfig>) {
        val id = devices.getDeviceId() ?: return
        val state = provider.runtimeSession.getSnapshot()
        if (state.userId != session.getUserId() || state.deviceId != id || state.attendanceState != AttendanceState.NOT_CHECKED_IN) return
        val detected = bssid?.takeIf { it.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
            it != "02:00:00:00:00:00" && it != "00:00:00:00:00:00" } ?: return
        val office = offices.filter { candidate -> candidate.wifiAccessPoints.any { it.bssid.equals(detected, true) } }.singleOrNull() ?: return
        val anchor = provider.trustedTimeManager.getCurrentAnchor() ?: return
        if (observedElapsedMs < anchor.elapsedRealtimeMs || SystemClock.elapsedRealtime() - observedElapsedMs !in 0..60_000L) return
        val trustedInstant = Instant.parse(anchor.serverTime).plusMillis(observedElapsedMs - anchor.elapsedRealtimeMs)
        // A day key deduplicates observations, never determines attendance timing.
        val day = trustedInstant.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
        val key = "${session.getUserId()}|$id|${anchor.bootId}|${office.id}|$day"
        val candidateId = UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
        val inserted = dao.recordFirst(ArrivalCandidateEntity(candidateId, session.getUserId(), session.getCompanyId(),
            id, office.id, anchor.bootId, anchor.anchorUuid, observedElapsedMs, detected.lowercase()))
        if (inserted != -1L) Log.i("ARRIVAL_EVIDENCE", "First approved Wi-Fi arrival saved for CRM review; officeId=${office.id}")
    }

    suspend fun upload() {
        val deviceId = devices.getDeviceId() ?: return
        val userId = session.getUserId()
        if (userId <= 0) return
        val rows = dao.pending(userId, deviceId)
        if (rows.isEmpty()) return
        val token = AndroidTokenManager(app).usableAccessToken() ?: return
        val reply = RetrofitClient.presenceEvidenceApi.uploadArrivals("Bearer $token", ArrivalCandidateBatch(deviceId,
            rows.map { ArrivalCandidateItem(it.id, it.officeId, it.bootId, it.anchorUuid, it.observedElapsedMs, it.wifiBssid) }))
        if (reply.isSuccessful && reply.body()?.success == true) {
            dao.markUploaded(userId, deviceId, reply.body()?.accepted.orEmpty())
        } else Log.w("ARRIVAL_EVIDENCE", "Arrival upload deferred: HTTP ${reply.code()}")
    }
}

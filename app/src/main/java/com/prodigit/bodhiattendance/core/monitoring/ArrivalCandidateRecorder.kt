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
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/** Continuous approved Wi-Fi timestamps arrival; GPS confirmation is still required. */
class ArrivalCandidateRecorder(context: Context) {
    private val app = context.applicationContext
    private val session = SessionManager(app)
    private val devices = DeviceIdManager(app)
    private val dao = AttendanceDatabase.getInstance(app).arrivalCandidateDao()
    private val provider = AttendanceEngineRuntimeProvider.getInstance(app)

    suspend fun resetContinuity() = continuityMutex.withLock {
        val device = devices.getDeviceId() ?: return@withLock
        dao.invalidate(session.getUserId(), device)
    }

    suspend fun record(bssid: String?, observedElapsedMs: Long, offices: List<AttendanceLocationConfig>) = continuityMutex.withLock {
        val id = devices.getDeviceId() ?: return@withLock
        val user = session.getUserId()
        val now = SystemClock.elapsedRealtime()
        val state = provider.runtimeSession.getSnapshot()
        val office = offices.filter { candidate -> candidate.wifiRequired == true &&
            bssid != null && candidate.wifiAccessPoints.any { it.bssid.equals(bssid, true) } }.singleOrNull()
        val boot = com.prodigit.bodhiattendance.data.local.security.BootSessionManager(app).getBootId()
        val old = dao.active(user, id)
        if (state.userId != user || state.deviceId != id || state.attendanceState != AttendanceState.NOT_CHECKED_IN || office == null) {
            dao.invalidate(user, id)
            return@withLock
        }
        if (old != null && old.officeId == office.id && old.wifiBssid.equals(bssid, true) &&
            old.bootId == boot && now - old.lastSeenElapsedMs in 0..180_000L) {
            dao.seen(old.id, now)
            return@withLock
        }
        dao.invalidate(user, id)
        val anchor = provider.trustedTimeManager.getCurrentAnchor() ?: return@withLock
        val observed = observedElapsedMs.takeIf { now - it in 0..60_000L } ?: now
        if (anchor.bootId != boot || observed < anchor.elapsedRealtimeMs) return@withLock
        dao.recordFirst(ArrivalCandidateEntity(UUID.randomUUID().toString(), user, session.getCompanyId(),
            id, office.id, boot, anchor.anchorUuid, observed, bssid!!.lowercase(), active = true, lastSeenElapsedMs = now))
        AttendanceDiagnostics(app).record("ARRIVAL_PENDING", "Approved office Wi-Fi arrival saved; officeId=${office.id}; elapsedMs=$observed; awaiting GPS")
    }

    suspend fun confirmation(officeId: Long, bssid: String?, confirmedMs: Long): com.prodigit.bodhiattendance.data.remote.dto.attendance.WifiArrivalProof? = continuityMutex.withLock {
        val device = devices.getDeviceId() ?: return@withLock null
        val row = dao.active(session.getUserId(), device) ?: return@withLock null
        val boot = com.prodigit.bodhiattendance.data.local.security.BootSessionManager(app).getBootId()
        if (row.officeId != officeId || row.bootId != boot || !row.wifiBssid.equals(bssid, true) ||
            confirmedMs < row.observedElapsedMs || confirmedMs - row.lastSeenElapsedMs !in -60_000L..180_000L) return@withLock null
        val anchor = provider.trustedTimeManager.getAnchorByUuid(row.anchorUuid) ?: return@withLock null
        if (anchor.bootId != boot || anchor.deviceId != device || row.observedElapsedMs < anchor.elapsedRealtimeMs) return@withLock null
        val start = Instant.parse(anchor.serverTime).plusMillis(row.observedElapsedMs - anchor.elapsedRealtimeMs)
        val confirmed = Instant.parse(anchor.serverTime).plusMillis(confirmedMs - anchor.elapsedRealtimeMs)
        if (start.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate() != confirmed.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()) return@withLock null
        com.prodigit.bodhiattendance.data.remote.dto.attendance.WifiArrivalProof(row.id, row.officeId, row.anchorUuid,
            row.bootId, row.observedElapsedMs, confirmedMs, row.wifiBssid)
    }

    suspend fun hasPending(): Boolean {
        val device = devices.getDeviceId() ?: return false
        return dao.active(session.getUserId(), device) != null
    }

    companion object { private val continuityMutex = kotlinx.coroutines.sync.Mutex() }

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

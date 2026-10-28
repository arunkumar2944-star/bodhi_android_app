package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.entity.AttendanceDiagnosticEntity
import com.prodigit.bodhiattendance.data.local.security.BootSessionManager
import com.prodigit.bodhiattendance.data.remote.DiagnosticBatch
import com.prodigit.bodhiattendance.data.remote.DiagnosticItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

/** Diagnostic deviceTime is untrusted phone time, never an attendance timestamp. */
class AttendanceDiagnostics(context: Context) {
    private val app = context.applicationContext
    private val dao = AttendanceDatabase.getInstance(app).attendanceDiagnosticDao()
    suspend fun record(category: String, message: String, observedElapsedMs: Long = SystemClock.elapsedRealtime()) {
        recordPersisted(category, message, observedElapsedMs)
    }

    internal suspend fun recordPersisted(category: String, message: String,
                                         observedElapsedMs: Long = SystemClock.elapsedRealtime()): Boolean {
        Log.i(if (category.startsWith("ENGINE")) "ATTENDANCE_ENGINE" else "ATTENDANCE_DIAGNOSTICS", "$category: $message")
        try {
            val user = SessionManager(app).getUserId()
            val device = DeviceIdManager(app).getDeviceId() ?: return false
            if (user <= 0) return false
            dao.insert(AttendanceDiagnosticEntity(UUID.randomUUID().toString(), user, device,
                BootSessionManager(app).getBootId(), observedElapsedMs, Instant.now().toString(),
                category.take(40), message.take(1500)))
            dao.trim()
            return true
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            Log.w("ATTENDANCE_DIAGNOSTICS", "Diagnostic persistence unavailable")
            return false
        }
    }
    /** Read-only local persistence summary; sent via the existing authenticated diagnostic outbox. */
    suspend fun auditLocalPersistence(): Boolean {
        val user = SessionManager(app).getUserId()
        val company = SessionManager(app).getCompanyId()
        val device = DeviceIdManager(app).getDeviceId() ?: return false
        if (user <= 0 || company <= 0) return false
        return try {
            val events = AttendanceDatabase.getInstance(app).attendanceEventDao()
            val counts = PresenceEvidenceJournal(app).candidateCounts(user, company, device)
            val starts = events.countLocalBreakStarts(user, device)
            val ends = events.countLocalBreakEnds(user, device)
            val unsettled = events.countUnsettledEvents(user, device)
            val monitorState = AttendanceMonitorStatus.state.value
            val lastStartGateResult = app.getSharedPreferences(
                "attendance_monitor_start_diagnostics", Context.MODE_PRIVATE
            ).getString("last_result", "NOT_RECORDED") ?: "NOT_RECORDED"
            recordPersisted("LOCAL_EVIDENCE_AUDIT",
                "roomBreakStarts=$starts; roomBreakEnds=$ends; roomUnsettled=$unsettled; sqliteCandidates=${counts.first}; sqliteActiveCandidates=${counts.second}; monitorRunning=${monitorState.running}; verificationPending=${monitorState.verificationPending}; lastStartGateResult=${lastStartGateResult.take(300)}")
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { false }
    }

    suspend fun hasPending(): Boolean {
        val user = SessionManager(app).getUserId()
        val device = DeviceIdManager(app).getDeviceId() ?: return false
        return user > 0 && dao.pending(user, device).isNotEmpty()
    }

    suspend fun upload(): Boolean = uploadMutex.withLock {
        val user = SessionManager(app).getUserId()
        val device = DeviceIdManager(app).getDeviceId() ?: return@withLock true
        if (user <= 0) return@withLock true
        if (dao.pending(user, device).isEmpty()) return@withLock true
        val token = AndroidTokenManager(app).usableAccessToken() ?: run {
            Log.w("ATTENDANCE_DIAGNOSTICS", "Diagnostic upload deferred: access token unavailable")
            return@withLock false
        }
        try {
            deliverDiagnosticOutbox(
                pending = { dao.pending(user, device) },
                identify = { it.id },
                send = { rows ->
                    val response = RetrofitClient.presenceEvidenceApi.uploadDiagnostics("Bearer $token",
                        DiagnosticBatch(device, rows.map { DiagnosticItem(it.id, it.bootId, it.observedElapsedMs,
                            it.deviceTime, it.category, it.message) }))
                    if (response.isSuccessful && response.body()?.success == true) {
                        val accepted = response.body()?.accepted.orEmpty().toSet()
                        Log.i("ATTENDANCE_DIAGNOSTICS", "Upload HTTP ${response.code()}: ${accepted.intersect(rows.map { it.id }.toSet()).size}/${rows.size} acknowledged")
                        accepted
                    } else {
                        Log.w("ATTENDANCE_DIAGNOSTICS", "Diagnostic upload deferred: HTTP ${response.code()}")
                        emptySet()
                    }
                },
                acknowledge = { ids -> dao.acknowledge(user, device, ids) }
            )
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            Log.w("ATTENDANCE_DIAGNOSTICS", "Diagnostic upload retained for retry: ${e.javaClass.simpleName}")
            false
        }
    }
    companion object { private val uploadMutex = Mutex() }
}

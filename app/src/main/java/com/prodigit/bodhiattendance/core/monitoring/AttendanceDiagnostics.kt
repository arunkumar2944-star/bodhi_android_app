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
        Log.i(if (category.startsWith("ENGINE")) "ATTENDANCE_ENGINE" else "ATTENDANCE_DIAGNOSTICS", message)
        try {
            val user = SessionManager(app).getUserId()
            val device = DeviceIdManager(app).getDeviceId() ?: return
            if (user <= 0) return
            dao.insert(AttendanceDiagnosticEntity(UUID.randomUUID().toString(), user, device,
                BootSessionManager(app).getBootId(), observedElapsedMs, Instant.now().toString(),
                category.take(40), message.take(1500)))
            dao.trim()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Log.w("ATTENDANCE_DIAGNOSTICS", "Diagnostic persistence unavailable") }
    }
    suspend fun upload() = uploadMutex.withLock {
        val user = SessionManager(app).getUserId()
        val device = DeviceIdManager(app).getDeviceId() ?: return@withLock
        val token = AndroidTokenManager(app).usableAccessToken() ?: return@withLock
        val rows = dao.pending(user, device)
        if (rows.isEmpty()) return@withLock
        val response = RetrofitClient.presenceEvidenceApi.uploadDiagnostics("Bearer $token",
            DiagnosticBatch(device, rows.map { DiagnosticItem(it.id, it.bootId, it.observedElapsedMs,
                it.deviceTime, it.category, it.message) }))
        if (response.isSuccessful && response.body()?.success == true) {
            dao.acknowledge(user, device, response.body()?.accepted.orEmpty().intersect(rows.map { it.id }.toSet()).toList())
        } else Log.w("ATTENDANCE_DIAGNOSTICS", "Diagnostic upload deferred: HTTP ${response.code()}")
    }
    companion object { private val uploadMutex = Mutex() }
}

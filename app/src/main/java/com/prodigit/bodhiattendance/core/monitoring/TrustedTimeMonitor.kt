package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.os.SystemClock
import com.google.gson.Gson
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.security.BootSessionManager
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shared read-only status for both health screens and diagnostic evidence. */
class TrustedTimeMonitor(context: Context) {
    private val app = context.applicationContext
    suspend fun read(): TrustedClockEvidence {
        val boot = BootSessionManager(app).getBootId()
        val device = DeviceIdManager(app).getDeviceId()
        val cached = TimeAnchorStorage(app).getAnchor()
        val anchor = try {
            val dao = AttendanceDatabase.getInstance(app).timeAnchorDao()
            val persisted = cached?.let { dao.getTimeAnchorByUuid(it.anchorUuid) }
            persisted?.takeIf { it.deviceId == device && it.bootId == boot }
                ?: device?.let { dao.getLatestValidTimeAnchor(it, boot) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
        return TrustedClockEvidence.calculate(anchor?.serverTime, anchor?.elapsedRealtimeMs,
            anchor?.anchorUuid, anchor != null && anchor.bootId == boot && anchor.deviceId == device,
            SystemClock.elapsedRealtime(), System.currentTimeMillis())
    }

    suspend fun report(snapshot: TrustedClockEvidence? = null) = reportMutex.withLock {
        val current = snapshot ?: read()
        // Persist status transitions so repeated GPS fixes do not flood diagnostics.
        val session = com.prodigit.bodhiattendance.core.session.SessionManager(app)
        if (session.getUserId() <= 0) return@withLock
        val preferences = app.getSharedPreferences("trusted_clock_diagnostics", Context.MODE_PRIVATE)
        val signature = "${session.getUserId()}:${DeviceIdManager(app).getDeviceId()}:${BootSessionManager(app).getBootId()}:${current.status}:${current.anchorUuid}"
        if (preferences.getString("signature", null) == signature) return@withLock
        AttendanceDiagnostics(app).record(when {
            !current.available -> "TRUSTED_TIME_UNAVAILABLE"
            current.mismatch -> "CLOCK_MISMATCH"
            else -> "TRUSTED_TIME_AVAILABLE"
        }, Gson().toJson(current))
        preferences.edit().putString("signature", signature).apply()
        AttendanceBackgroundSync.schedule(app)
    }
    companion object { private val reportMutex = Mutex() }
}

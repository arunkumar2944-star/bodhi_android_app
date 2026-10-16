package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex

/** Best-effort immediate network delivery, with a durable JobScheduler retry. No sensors. */
object AttendanceDiagnosticDelivery {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = Mutex()

    fun request(context: Context) {
        val app = context.applicationContext
        AttendanceBackgroundSync.schedule(app)
        if (!active.tryLock()) return
        scope.launch {
            try {
                withTimeoutOrNull(10_000L) { AttendanceDiagnostics(app).upload() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("ATTENDANCE_DIAGNOSTICS", "Immediate upload deferred: ${e.javaClass.simpleName}") }
            finally { active.unlock() }
        }
    }
}

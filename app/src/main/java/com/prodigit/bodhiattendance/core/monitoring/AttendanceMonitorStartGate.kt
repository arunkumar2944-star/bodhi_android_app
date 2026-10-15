package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.util.Log
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.session.*
import com.prodigit.bodhiattendance.domain.attendance.*
import kotlinx.coroutines.*

/** Start only from a visible Activity, after startup and privacy validation. */
object AttendanceMonitorStartGate {
    private var startupReady = false
    private var activityResumed = false
    private var startRequested = false
    private var task: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Synchronized fun onStartupReady(context: Context) { startupReady = true; tryStart(context.applicationContext) }
    @Synchronized fun onActivityResumed(context: Context) { activityResumed = true; tryStart(context.applicationContext) }
    @Synchronized fun onActivityPaused() { activityResumed = false }
    @Synchronized fun onServiceStopped() { startRequested = false }
    @Synchronized fun reset() { task?.cancel(); task = null; startupReady = false; startRequested = false }
    private fun tryStart(context: Context) {
        if (!startupReady || !activityResumed || startRequested || task?.isActive == true) return
        task = scope.launch {
            try {
                val provider = AttendanceEngineRuntimeProvider.getInstance(context)
                val restored = provider.runtimeSession.getSnapshot()
                if (AttendancePrivacyPolicy.needsMorningReconciliation(context) ||
                    (!AttendancePrivacyPolicy.isPaused(context) && restored.attendanceState == AttendanceState.CHECKED_OUT)) {
                    // Upload yesterday's pending checkout before asking for today's authoritative state.
                    if (!AttendanceBackgroundSync.run(context, diagnostics = false)) return@launch
                    val session = SessionManager(context)
                    val device = DeviceIdManager(context).getDeviceId() ?: return@launch
                    val token = AndroidTokenManager(context).usableAccessToken() ?: return@launch
                    provider.runtimeSession.initialize(session.getUserId(), device)
                    val result = provider.reconciliationManager.reconcile(token, session.getUserId(), session.getCompanyId(), device)
                    if (result !is AttendanceEngineBootstrapResult.Ready) return@launch
                    val state = provider.runtimeSession.applyReconciledState(session.getUserId(), device, result)
                    if (state.attendanceState == AttendanceState.CHECKED_OUT) {
                        AttendancePrivacyPolicy.pauseAfterCheckout(context)
                        context.stopService(android.content.Intent(context, AttendancePresenceService::class.java))
                        return@launch
                    }
                    AttendancePrivacyPolicy.resumeAfterServerValidation(context)
                }
                if (provider.runtimeSession.getSnapshot().attendanceState == AttendanceState.CHECKED_OUT) {
                    AttendancePrivacyPolicy.pauseAfterCheckout(context)
                    context.stopService(android.content.Intent(context, AttendancePresenceService::class.java))
                    return@launch
                }
                if (AttendancePrivacyPolicy.isPaused(context)) return@launch
                if (provider.runtimeSession.getSnapshot().mode != AttendanceEngineRuntimeMode.READY) return@launch
                synchronized(this@AttendanceMonitorStartGate) {
                    if (startupReady && activityResumed) startRequested = AttendancePresenceService.start(context)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("ATTENDANCE_MONITOR", "Monitoring resume deferred", e) }
        }
    }
}

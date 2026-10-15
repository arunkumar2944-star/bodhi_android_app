package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.content.Intent
import android.util.Log
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.session.*
import com.prodigit.bodhiattendance.domain.attendance.*
import kotlinx.coroutines.*

/** Only a visible Activity may authorize a new monitoring session. */
object AttendanceMonitorStartGate {
    private var startupReady = false
    private var activityResumed = false
    private var startRequested = false
    private var task: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Synchronized fun onStartupReady(context: Context) { startupReady = true; tryStart(context.applicationContext) }
    @Synchronized fun onActivityResumed(context: Context) { activityResumed = true; tryStart(context.applicationContext) }
    @Synchronized fun onActivityPaused() { activityResumed = false; task?.cancel(); task = null }
    @Synchronized fun onServiceStopped() { startRequested = false }
    @Synchronized fun isVisible(): Boolean = startupReady && activityResumed
    @Synchronized fun reset() { task?.cancel(); task = null; startupReady = false; startRequested = false }
    private fun tryStart(context: Context) {
        if (!isVisible() || task?.isActive == true) return
        task = scope.launch {
            try {
                AttendancePrivacyPolicy.removeLegacyAutomation(context)
                if (!AttendanceBackgroundSync.run(context, diagnostics = false)) return@launch
                val day = AttendanceDayGate.fetch(context)
                if (day.monitoringMode == "PAUSED" || !AttendancePrivacyPolicy.canResumeOn(context, day.date)) {
                    if (day.status == "CHECKED_OUT") AttendancePrivacyPolicy.pauseAfterCheckout(context, day.date)
                    context.stopService(Intent(context, AttendancePresenceService::class.java))
                    return@launch
                }
                if (startRequested && AttendanceMonitorStatus.state.value.running) return@launch
                val session = SessionManager(context)
                val device = DeviceIdManager(context).getDeviceId() ?: return@launch
                val token = AndroidTokenManager(context).usableAccessToken() ?: return@launch
                val provider = AttendanceEngineRuntimeProvider.getInstance(context)
                provider.runtimeSession.initialize(session.getUserId(), device)
                val result = provider.reconciliationManager.reconcile(token, session.getUserId(), session.getCompanyId(), device)
                if (result !is AttendanceEngineBootstrapResult.Ready) return@launch
                val state = provider.runtimeSession.applyReconciledState(session.getUserId(), device, result)
                if (state.attendanceState == AttendanceState.CHECKED_OUT) {
                    AttendancePrivacyPolicy.pauseAfterCheckout(context, day.date)
                    context.stopService(Intent(context, AttendancePresenceService::class.java))
                    return@launch
                }
                if (state.mode != AttendanceEngineRuntimeMode.READY) return@launch
                currentCoroutineContext().ensureActive()
                synchronized(this@AttendanceMonitorStartGate) {
                    if (isVisible()) {
                        AttendancePrivacyPolicy.resumeAfterServerValidation(context)
                        startRequested = AttendancePresenceService.start(context, day.date)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("ATTENDANCE_MONITOR", "Open-app monitoring start deferred", e) }
        }
    }
}

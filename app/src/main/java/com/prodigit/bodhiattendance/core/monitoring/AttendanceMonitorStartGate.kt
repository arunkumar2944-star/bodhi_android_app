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
    private const val START_DIAGNOSTICS_PREFS = "attendance_monitor_start_diagnostics"
    private const val LAST_START_RESULT = "last_result"

    private fun rememberStartResult(context: Context, result: String) {
        context.getSharedPreferences(START_DIAGNOSTICS_PREFS, Context.MODE_PRIVATE)
            .edit().putString(LAST_START_RESULT, result.take(300)).apply()
    }

    private suspend fun traceStartResult(context: Context, result: String) {
        rememberStartResult(context, result)
        AttendanceDiagnostics(context).record("MONITOR_START_GATE", result.take(1500))
    }

    @Synchronized fun onStartupReady(context: Context) {
        startupReady = true
        if (!activityResumed) rememberStartResult(context.applicationContext, "WAITING:ACTIVITY_NOT_RESUMED")
        tryStart(context.applicationContext)
    }
    @Synchronized fun onActivityResumed(context: Context) {
        activityResumed = true
        if (!startupReady) rememberStartResult(context.applicationContext, "WAITING:STARTUP_NOT_READY")
        tryStart(context.applicationContext)
    }
    @Synchronized fun onActivityPaused() { activityResumed = false; task?.cancel(); task = null }
    @Synchronized fun onServiceStopped() { startRequested = false }
    @Synchronized fun isVisible(): Boolean = startupReady && activityResumed
    @Synchronized fun reset() { task?.cancel(); task = null; startupReady = false; startRequested = false }
    private fun tryStart(context: Context) {
        if (!isVisible() || task?.isActive == true) return
        task = scope.launch {
            try {
                rememberStartResult(context, "ATTEMPT:VISIBLE_START_GATE")
                AttendancePrivacyPolicy.removeLegacyAutomation(context)
                if (!AttendanceBackgroundSync.run(context, diagnostics = false)) {
                    traceStartResult(context, "BLOCKED:PRESTART_SYNC_NOT_SETTLED")
                    return@launch
                }
                traceStartResult(context, "PASS:PRESTART_SYNC")
                val day = AttendanceDayGate.fetch(context)
                if (day.monitoringMode == "PAUSED" || !AttendancePrivacyPolicy.canResumeOn(context, day.date)) {
                    traceStartResult(context, "BLOCKED:DAILY_POLICY:status=" + day.status + ";mode=" + day.monitoringMode)
                    if (day.status == "CHECKED_OUT") AttendancePrivacyPolicy.pauseAfterCheckout(context, day.date)
                    context.stopService(Intent(context, AttendancePresenceService::class.java))
                    return@launch
                }
                if (startRequested && AttendanceMonitorStatus.state.value.running) {
                    traceStartResult(context, "ALREADY_RUNNING:VERIFY_REQUESTED")
                    AttendanceVerificationSignals.request("Visible app opened; refresh CRM attendance state")
                    return@launch
                }
                val session = SessionManager(context)
                val device = DeviceIdManager(context).getDeviceId()
                if (device == null) {
                    traceStartResult(context, "BLOCKED:DEVICE_ID_UNAVAILABLE")
                    return@launch
                }
                val token = AndroidTokenManager(context).usableAccessToken()
                if (token == null) {
                    traceStartResult(context, "BLOCKED:ACCESS_TOKEN_UNAVAILABLE")
                    return@launch
                }
                val provider = AttendanceEngineRuntimeProvider.getInstance(context)
                val initialized = provider.runtimeSession.initialize(session.getUserId(), device)
                val officeContext = AttendanceOfficeContext(context)
                val boot = com.prodigit.bodhiattendance.data.local.security.BootSessionManager(context).getBootId()
                val oldOffice = officeContext.read(session.getUserId(), session.getCompanyId(), device, day.date, boot)
                val restored = (initialized as? AttendanceEngineRuntimeStartResult.Ready)?.snapshot
                val resumePhase = restored?.phase?.takeIf { oldOffice != null }
                val result = provider.reconciliationManager.reconcile(token, session.getUserId(), session.getCompanyId(), device, resumePhase)
                if (result !is AttendanceEngineBootstrapResult.Ready) {
                    traceStartResult(context, "BLOCKED:RECONCILIATION_NOT_READY:" + result.javaClass.simpleName)
                    return@launch
                }
                val state = provider.runtimeSession.applyReconciledState(session.getUserId(), device, result)
                if (state.attendanceState == AttendanceState.CHECKED_OUT) {
                    traceStartResult(context, "BLOCKED:SERVER_CONFIRMED_CHECKED_OUT")
                    AttendancePrivacyPolicy.pauseAfterCheckout(context, day.date)
                    context.stopService(Intent(context, AttendancePresenceService::class.java))
                    return@launch
                }
                if (state.mode != AttendanceEngineRuntimeMode.READY) {
                    traceStartResult(context, "BLOCKED:RUNTIME_MODE_" + state.mode)
                    return@launch
                }
                if (state.attendanceState in setOf(AttendanceState.WORKING, AttendanceState.ON_BREAK)) {
                    result.officeId?.takeIf { it > 0 }?.let { office ->
                        officeContext.save(session.getUserId(), session.getCompanyId(), device, day.date, boot, office)
                    }
                }
                currentCoroutineContext().ensureActive()
                var serviceStartRequested: Boolean? = null
                synchronized(this@AttendanceMonitorStartGate) {
                    if (isVisible()) {
                        AttendancePrivacyPolicy.resumeAfterServerValidation(context)
                        serviceStartRequested = AttendancePresenceService.start(context, day.date)
                        startRequested = serviceStartRequested == true
                    }
                }
                when (serviceStartRequested) {
                    true -> traceStartResult(context, "START_REQUEST_ACCEPTED:workDate=" + day.date + "; service execution not yet confirmed")
                    false -> traceStartResult(context, "BLOCKED:SERVICE_START_RETURNED_FALSE")
                    null -> traceStartResult(context, "BLOCKED:ACTIVITY_NOT_VISIBLE_AT_SERVICE_START")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                traceStartResult(context, "DEFERRED:EXCEPTION_" + e.javaClass.simpleName)
                Log.w("ATTENDANCE_MONITOR", "Open-app monitoring start deferred", e)
            }
        }
    }
}

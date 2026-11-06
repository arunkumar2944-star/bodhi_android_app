package com.prodigit.bodhiattendance.core.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.prodigit.bodhiattendance.core.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Recovers background attendance synchronization and monitoring state upon device reboot
 * or app package update.
 */
class AttendanceBootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                Log.i("ATTENDANCE_BOOT", "Boot/Update recovery triggered: $action")
                val appContext = context.applicationContext
                val userId = SessionManager(appContext).getUserId()
                if (userId <= 0 || AttendancePrivacyPolicy.isPaused(appContext)) {
                    Log.i("ATTENDANCE_BOOT", "User not logged in or monitoring paused; skipping service start")
                    return@launch
                }

                // 1. Schedule durable background synchronization for any queued Room events
                AttendanceBackgroundSync.schedule(appContext)

                // 2. Trigger system recovery check
                AttendanceMonitorStartGate.onSystemRecovery(appContext)
            } catch (e: Exception) {
                Log.w("ATTENDANCE_BOOT", "Boot recovery failed: ${e.javaClass.simpleName}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.util.Log

/**
 * Starts the location foreground service only when both conditions hold:
 * validated attendance startup and a foreground-resumed Activity.
 *
 * A background service launch must not be attempted merely because
 * asynchronous authentication finished.
 */
object AttendanceMonitorStartGate {
    private var startupReady = false
    private var activityResumed = false
    private var startRequested = false

    @Synchronized
    fun onStartupReady(context: Context) {
        startupReady = true
        tryStart(context)
    }

    @Synchronized
    fun onActivityResumed(context: Context) {
        activityResumed = true
        tryStart(context)
    }

    @Synchronized
    fun onActivityPaused() {
        activityResumed = false
    }

    @Synchronized
    fun reset() {
        startupReady = false
        startRequested = false
    }

    private fun tryStart(context: Context) {
        if (!startupReady || !activityResumed || startRequested) return
        // Service.start() handles permission failures, and reports whether
        // a request was dispatched. Do not permanently latch on failure.
        startRequested = AttendancePresenceService.start(context)
        Log.i("ATTENDANCE_MONITOR", "Foreground-eligible service launch requested=$startRequested")
    }
}

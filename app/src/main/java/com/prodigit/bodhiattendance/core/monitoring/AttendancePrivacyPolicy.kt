package com.prodigit.bodhiattendance.core.monitoring

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import java.time.LocalDate
import java.time.ZoneId

/** Checkout pauses tracking until a visible app opening and fresh server validation. */
object AttendancePrivacyPolicy {
    @Volatile private var checkoutShutdownRequested = false
    private fun prefs(c: Context) = c.getSharedPreferences("attendance_privacy", Context.MODE_PRIVATE)
    const val CLOSE_AFTER_CHECKOUT = "com.prodigit.bodhiattendance.CLOSE_AFTER_CHECKOUT"
    fun isPaused(c: Context): Boolean = prefs(c).getBoolean("paused", false)
    fun canResumeOn(c: Context, serverDay: String): Boolean = !isPaused(c) ||
        WorkdayPrivacySchedule.canResume(prefs(c).getString("checkout_day", null), serverDay)
    fun pauseAfterCheckout(c: Context, serverDay: String = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString()) {
        if (!isPaused(c)) {
            prefs(c).edit().putBoolean("paused", true).putString("checkout_day", serverDay).commit()
            checkoutShutdownRequested = true
            c.sendBroadcast(Intent(CLOSE_AFTER_CHECKOUT).setPackage(c.packageName))
        }
        removeLegacyAutomation(c)
        AttendanceBackgroundSync.schedule(c) // Saved records only; no sensors or monitoring startup.
    }
    fun terminateAfterCheckoutCleanup(c: Context) {
        if (!checkoutShutdownRequested) return
        checkoutShutdownRequested = false
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isPaused(c)) android.os.Process.killProcess(android.os.Process.myPid())
        }, 500L)
    }
    fun resumeAfterServerValidation(c: Context) {
        prefs(c).edit().putBoolean("paused", false).apply()
        removeLegacyAutomation(c)
    }
    /** Cancel persisted alarms/jobs/geofences from earlier automatic-start builds. Never start work. */
    fun removeLegacyAutomation(c: Context) {
        val oldAlarm = PendingIntent.getBroadcast(c, 25301, Intent(c, AttendanceMorningReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (oldAlarm != null) {
            requireNotNull(c.getSystemService(AlarmManager::class.java)).cancel(oldAlarm)
            oldAlarm.cancel()
        }
        requireNotNull(c.getSystemService(NotificationManager::class.java)).cancel(25301)
        prefs(c).edit().remove("reminder_at").remove("resume_at").apply()
        AttendanceDayGate.cancelLegacyAutomation(c)
    }
    fun cancel(c: Context) {
        removeLegacyAutomation(c)
        prefs(c).edit().clear().apply()
        AttendanceBackgroundSync.cancel(c)
        AttendanceDayGate.clearStatus(c)
    }
}

/** Kept only to consume/cancel pending broadcasts from previous APKs. */
class AttendanceMorningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { AttendancePrivacyPolicy.removeLegacyAutomation(context) }
}

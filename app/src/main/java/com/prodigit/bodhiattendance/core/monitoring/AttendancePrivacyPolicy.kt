package com.prodigit.bodhiattendance.core.monitoring

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.prodigit.bodhiattendance.MainActivity
import com.prodigit.bodhiattendance.R
import com.prodigit.bodhiattendance.core.session.SessionManager
import java.time.*

/** The alarm starts verification, never creates attendance or authorizes office presence. */
object AttendancePrivacyPolicy {
    @Volatile private var checkoutShutdownRequested = false
    private const val PREFS = "attendance_privacy"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun isPaused(c: Context): Boolean = prefs(c).getBoolean("paused", false)
    fun needsMorningReconciliation(c: Context): Boolean = isPaused(c) &&
        WorkdayPrivacySchedule.canResume(prefs(c).getString("checkout_day", ""),
            prefs(c).getLong("reminder_at", 0), Instant.now())
    fun hasBackgroundLocation(c: Context): Boolean = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun hasExactAlarmAccess(c: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        requireNotNull(c.getSystemService(AlarmManager::class.java)).canScheduleExactAlarms()
    fun automaticRestartReady(c: Context): Boolean = hasBackgroundLocation(c) && hasExactAlarmAccess(c)
    const val CLOSE_AFTER_CHECKOUT = "com.prodigit.bodhiattendance.CLOSE_AFTER_CHECKOUT"
    fun pauseAfterCheckout(c: Context) {
        if (!isPaused(c)) {
            val next = WorkdayPrivacySchedule.nextReminder(Instant.now())
            prefs(c).edit().putBoolean("paused", true).putString("checkout_day", LocalDate.now(ZoneId.of("Asia/Kolkata")).toString())
                .putLong("reminder_at", next).commit()
            checkoutShutdownRequested = true
            c.sendBroadcast(Intent(CLOSE_AFTER_CHECKOUT).setPackage(c.packageName))
        }
        scheduleReminder(c)
        AttendanceBackgroundSync.schedule(c)
    }
    /** Self-termination is not Android force-stop: the OS-owned alarm and saved jobs survive. */
    fun terminateAfterCheckoutCleanup(c: Context) {
        if (!checkoutShutdownRequested) return
        checkoutShutdownRequested = false
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isPaused(c)) android.os.Process.killProcess(android.os.Process.myPid())
        }, 500L)
    }
    fun scheduleNextMorning(c: Context) {
        prefs(c).edit().putLong("reminder_at", WorkdayPrivacySchedule.nextReminder(Instant.now())).commit()
        scheduleReminder(c)
    }
    fun resumeAfterServerValidation(c: Context) {
        prefs(c).edit().putBoolean("paused", false).remove("reminder_at").apply()
        requireNotNull(c.getSystemService(AlarmManager::class.java)).cancel(reminderIntent(c))
        requireNotNull(c.getSystemService(NotificationManager::class.java)).cancel(25301)
    }
    fun cancel(c: Context) {
        requireNotNull(c.getSystemService(AlarmManager::class.java)).cancel(reminderIntent(c))
        requireNotNull(c.getSystemService(NotificationManager::class.java)).cancel(25301)
        prefs(c).edit().clear().apply()
        AttendanceBackgroundSync.cancel(c)
    }
    private fun reminderIntent(c: Context) = PendingIntent.getBroadcast(c, 25301,
        Intent(c, AttendanceMorningReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun scheduleReminder(c: Context) {
        val time = prefs(c).getLong("reminder_at", 0)
        if (!isPaused(c) || time <= 0) return
        val manager = requireNotNull(c.getSystemService(AlarmManager::class.java))
        val trigger = time.coerceAtLeast(System.currentTimeMillis() + 1000L)
        try {
            if (automaticRestartReady(c)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, reminderIntent(c))
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, reminderIntent(c))
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, reminderIntent(c))
        }
    }
    fun notifyMorning(c: Context) {
        if (!isPaused(c) || SessionManager(c).getUserId() <= 0) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = requireNotNull(c.getSystemService(NotificationManager::class.java))
        manager.createNotificationChannel(NotificationChannel("attendance_morning", "Workday reminder", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(c, 25301, Intent(c, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(25301, NotificationCompat.Builder(c, "attendance_morning").setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Ready for your workday?")
            .setContentText("Open Bodhi Attendance to resume monitoring. Location is currently paused.")
            .setContentIntent(open).setAutoCancel(true).build())
    }
}

class AttendanceMorningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            AttendancePrivacyPolicy.scheduleReminder(context)
            return
        }
        if (!AttendancePrivacyPolicy.needsMorningReconciliation(context)) return
        if (!AttendancePrivacyPolicy.automaticRestartReady(context) ||
            !AttendancePresenceService.startMorning(context)) AttendancePrivacyPolicy.notifyMorning(context)
    }
}

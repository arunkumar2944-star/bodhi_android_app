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

/** Wall-clock times schedule a reminder only; they never authorize attendance. */
object AttendancePrivacyPolicy {
    private const val PREFS = "attendance_privacy"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun isPaused(c: Context): Boolean = prefs(c).getBoolean("paused", false)
    fun needsMorningReconciliation(c: Context): Boolean = isPaused(c) &&
        WorkdayPrivacySchedule.requiresFreshState(prefs(c).getString("checkout_day", ""), Instant.now())
    fun pauseAfterCheckout(c: Context) {
        if (!isPaused(c)) {
            val next = WorkdayPrivacySchedule.nextReminder(Instant.now())
            prefs(c).edit().putBoolean("paused", true).putString("checkout_day", LocalDate.now(ZoneId.of("Asia/Kolkata")).toString())
                .putLong("reminder_at", next).apply()
        }
        scheduleReminder(c)
        AttendanceBackgroundSync.schedule(c)
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
        requireNotNull(c.getSystemService(AlarmManager::class.java)).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            time.coerceAtLeast(System.currentTimeMillis() + 1000L), reminderIntent(c))
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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) AttendancePrivacyPolicy.scheduleReminder(context)
        else AttendancePrivacyPolicy.notifyMorning(context)
    }
}

package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.prodigit.bodhiattendance.data.remote.DailyEligibility
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Workday and shift-aware scheduler for initiating 8:30 AM IST (or employee-specific shift) monitoring.
 */
object AttendanceWorkdayScheduler {
    private const val PREFS_NAME = "attendance_workday_scheduler"
    private const val KEY_SCHEDULED_START_MS = "scheduled_start_ms"
    private const val KEY_ACTUAL_START_MS = "actual_start_ms"
    private const val KEY_LAST_CHECK_MS = "last_check_ms"
    private const val KEY_STOPPED_REASON = "stopped_reason"
    private const val KEY_POLICY_VERSION = "policy_version"
    private const val KEY_LAST_FGS_ERROR = "last_fgs_error"

    private val IST_ZONE = ZoneId.of("Asia/Kolkata")

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Supported workday eligibility states. */
    enum class WorkdayState {
        WORKING_DAY,
        APPROVED_FULL_DAY_LEAVE,
        APPROVED_HALF_DAY_LEAVE,
        PENDING_LEAVE,
        REJECTED_LEAVE,
        COMPANY_HOLIDAY,
        WEEKLY_OFF,
        OUTSIDE_SHIFT,
        ELIGIBILITY_UNKNOWN
    }

    /** Map DailyEligibility DTO to WorkdayState. */
    fun resolveState(eligibility: DailyEligibility?): WorkdayState {
        if (eligibility == null) return WorkdayState.ELIGIBILITY_UNKNOWN
        return when (eligibility.status) {
            "APPROVED_LEAVE" -> WorkdayState.APPROVED_FULL_DAY_LEAVE
            "APPROVED_HALF_DAY" -> WorkdayState.APPROVED_HALF_DAY_LEAVE
            "HOLIDAY" -> WorkdayState.COMPANY_HOLIDAY
            "WEEKLY_OFF" -> WorkdayState.WEEKLY_OFF
            "EXPECTED_AT_OFFICE", "AT_WORK", "UNCONFIRMED_ABSENCE" -> WorkdayState.WORKING_DAY
            else -> if (eligibility.monitoringMode == "PAUSED") WorkdayState.APPROVED_FULL_DAY_LEAVE else WorkdayState.WORKING_DAY
        }
    }

    /** Calculate next scheduled shift start epoch milliseconds. Default is 08:30 AM IST. */
    fun calculateNextShiftStartMs(
        nowIso: LocalDateTime = LocalDateTime.now(IST_ZONE),
        shiftStartTimeStr: String = "08:30:00"
    ): Long {
        val shiftTime = try {
            LocalTime.parse(shiftStartTimeStr, DateTimeFormatter.ISO_LOCAL_TIME)
        } catch (_: Exception) {
            LocalTime.of(8, 30, 0)
        }

        var targetDateTime = LocalDateTime.of(nowIso.toLocalDate(), shiftTime)
        if (nowIso.isAfter(targetDateTime)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        return targetDateTime.atZone(IST_ZONE).toInstant().toEpochMilli()
    }

    fun recordScheduledStart(context: Context, scheduledMs: Long, policyVersion: Int = 2) {
        prefs(context).edit()
            .putLong(KEY_SCHEDULED_START_MS, scheduledMs)
            .putInt(KEY_POLICY_VERSION, policyVersion)
            .putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
            .apply()

        scheduleShiftStartAlarm(context, scheduledMs)
    }

    fun scheduleShiftStartAlarm(context: Context, triggerMs: Long) {
        if (triggerMs <= System.currentTimeMillis()) return
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            val intent = Intent(context, AttendanceBootReceiver::class.java).apply {
                action = "com.prodigit.bodhiattendance.ACTION_SHIFT_START"
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                25305,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            Log.i("ATTENDANCE_SCHEDULER", "Shift start alarm set for epoch ms: $triggerMs")
        } catch (e: Exception) {
            Log.w("ATTENDANCE_SCHEDULER", "Failed to set shift start alarm: ${e.javaClass.simpleName}")
        }
    }

    fun recordActualStart(context: Context) {
        prefs(context).edit()
            .putLong(KEY_ACTUAL_START_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordStopReason(context: Context, reason: String) {
        prefs(context).edit()
            .putString(KEY_STOPPED_REASON, reason)
            .apply()
    }

    fun recordFgsError(context: Context, error: String) {
        prefs(context).edit()
            .putString(KEY_LAST_FGS_ERROR, error)
            .apply()
        Log.w("ATTENDANCE_SCHEDULER", "Background FGS restriction error: $error")
    }

    fun getStoppedReason(context: Context): String? =
        prefs(context).getString(KEY_STOPPED_REASON, null)

    fun getScheduledStartMs(context: Context): Long =
        prefs(context).getLong(KEY_SCHEDULED_START_MS, -1L)

    fun getActualStartMs(context: Context): Long =
        prefs(context).getLong(KEY_ACTUAL_START_MS, -1L)
}

package com.prodigit.bodhiattendance.core.monitoring

import java.time.Instant
import java.time.ZoneId

/** Calendar scheduling only; authoritative server state controls attendance. */
object WorkdayPrivacySchedule {
    private val zone = ZoneId.of("Asia/Kolkata")
    fun nextReminder(now: Instant): Long = now.atZone(zone).toLocalDate().plusDays(1)
        .atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
    fun canResume(checkoutDay: String?, scheduledAt: Long, now: Instant): Boolean =
        scheduledAt > 0 && now.toEpochMilli() >= scheduledAt && requiresFreshState(checkoutDay, now)
    fun requiresFreshState(checkoutDay: String?, now: Instant): Boolean =
        checkoutDay != now.atZone(zone).toLocalDate().toString()
}

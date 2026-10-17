package com.prodigit.bodhiattendance.core.monitoring

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A server-owned workday, advanced using monotonic time; phone wall time is irrelevant. */
object WorkdayBoundary {
    fun deadlineElapsed(workDate: String, serverTimeMs: Long, receivedElapsedMs: Long): Long {
        require(receivedElapsedMs >= 0L)
        val date = LocalDate.parse(workDate)
        val serverDate = Instant.ofEpochMilli(serverTimeMs).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
        require(serverDate == date) { "Server time does not belong to the authorized workday" }
        val nextMidnight = date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
        return Math.addExact(receivedElapsedMs, Math.subtractExact(nextMidnight, serverTimeMs))
    }

    fun expired(deadlineElapsedMs: Long?, nowElapsedMs: Long): Boolean =
        deadlineElapsedMs == null || nowElapsedMs >= deadlineElapsedMs
}

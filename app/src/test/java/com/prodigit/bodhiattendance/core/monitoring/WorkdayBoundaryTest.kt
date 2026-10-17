package com.prodigit.bodhiattendance.core.monitoring

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class WorkdayBoundaryTest {
    @Test fun midnightIstStopsTheOldDayEvenWhenOffline() {
        val server = Instant.parse("2026-10-16T18:29:30Z").toEpochMilli()
        val deadline = WorkdayBoundary.deadlineElapsed("2026-10-16", server, 100_000L)
        assertEquals(130_000L, deadline)
        assertFalse(WorkdayBoundary.expired(deadline, 129_999L))
        assertTrue(WorkdayBoundary.expired(deadline, 130_000L))
        assertTrue(WorkdayBoundary.expired(deadline, 200_000L))
    }
    @Test fun absentAuthorizationNeverAllowsSensorsToMarkAttendance() {
        assertTrue(WorkdayBoundary.expired(null, 0L))
    }
    @Test(expected = IllegalArgumentException::class)
    fun nextDayResponseCannotAuthorizeTheOldWorkday() {
        WorkdayBoundary.deadlineElapsed("2026-10-16", Instant.parse("2026-10-16T18:30:00Z").toEpochMilli(), 100L)
    }
    @Test fun monthAndYearBoundariesUseTheIstCalendar() {
        assertEquals(1_001L, WorkdayBoundary.deadlineElapsed("2026-12-31",
            Instant.parse("2026-12-31T18:29:59Z").toEpochMilli(), 1L))
    }
}

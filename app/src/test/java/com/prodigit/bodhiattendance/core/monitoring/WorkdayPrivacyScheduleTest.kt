package com.prodigit.bodhiattendance.core.monitoring

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WorkdayPrivacyScheduleTest {
    @Test fun eveningCheckoutSchedulesNextMorningInIndia() {
        assertEquals(Instant.parse("2026-10-16T02:30:00Z").toEpochMilli(), WorkdayPrivacySchedule.nextReminder(Instant.parse("2026-10-15T13:00:00Z")))
    }
    @Test fun indiaDateBoundaryIsUsedInsteadOfUtcDate() {
        assertEquals(Instant.parse("2026-10-17T02:30:00Z").toEpochMilli(), WorkdayPrivacySchedule.nextReminder(Instant.parse("2026-10-15T19:00:00Z")))
    }
    @Test fun freshStateRequiredOnlyAfterIndiaDateChanges() {
        assertFalse(WorkdayPrivacySchedule.requiresFreshState("2026-10-15", Instant.parse("2026-10-15T18:00:00Z")))
        assertTrue(WorkdayPrivacySchedule.requiresFreshState("2026-10-15", Instant.parse("2026-10-15T19:00:00Z")))
    }
}

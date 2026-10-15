package com.prodigit.bodhiattendance.core.monitoring

import org.junit.Assert.*
import org.junit.Test

class WorkdayPrivacyScheduleTest {
    @Test fun sameDayCheckoutCannotResume() {
        assertFalse(WorkdayPrivacySchedule.canResume("2026-10-15", "2026-10-15"))
    }
    @Test fun laterServerDayCanResumeWithoutAnEightAmBoundary() {
        assertTrue(WorkdayPrivacySchedule.canResume("2026-10-15", "2026-10-16"))
        assertTrue(WorkdayPrivacySchedule.canResume("2026-10-15", "2026-10-19"))
    }
    @Test fun oldOrMissingCheckoutDateDoesNotBypassValidation() {
        assertFalse(WorkdayPrivacySchedule.canResume("2026-10-16", "2026-10-15"))
        assertFalse(WorkdayPrivacySchedule.canResume(null, "2026-10-16"))
    }
}

package com.prodigit.bodhiattendance.domain.attendance

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class OnDutyWindowTest {
    private val today = LocalDate.parse("2026-10-15")
    @Test fun todayAndDayFifteenAreIncluded() {
        assertTrue(OnDutyWindow.isVisible("2026-10-15", "APPROVED", today))
        assertTrue(OnDutyWindow.isVisible("2026-10-30", "APPROVED", today))
    }
    @Test fun historicalAndDistantAndCancelledAssignmentsDoNotShowMenu() {
        assertFalse(OnDutyWindow.isVisible("2026-10-14", "APPROVED", today))
        assertFalse(OnDutyWindow.isVisible("2026-10-31", "APPROVED", today))
        assertFalse(OnDutyWindow.isVisible("2026-10-15", "CANCELLED", today))
        assertFalse(OnDutyWindow.isVisible("bad", "APPROVED", today))
    }
}

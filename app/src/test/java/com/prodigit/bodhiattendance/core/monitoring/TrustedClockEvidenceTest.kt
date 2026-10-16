package com.prodigit.bodhiattendance.core.monitoring

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TrustedClockEvidenceTest {
    private val server = "2026-10-16T06:00:00Z"
    private val ms = Instant.parse(server).toEpochMilli()
    @Test fun changingPhoneTimeDoesNotChangeAttendanceTime() {
        val correct = TrustedClockEvidence.calculate(server, 100L, "anchor", true, 60_100L, ms + 60_000L)
        val changed = TrustedClockEvidence.calculate(server, 100L, "anchor", true, 60_100L, ms - 7_200_000L)
        assertEquals(correct.trustedTimeMs, changed.trustedTimeMs)
        assertEquals(ms + 60_000L, changed.trustedTimeMs)
        assertTrue(changed.available)
        assertTrue(changed.mismatch)
        assertFalse(correct.mismatch)
    }
    @Test fun restoringPhoneTimeClearsMismatchWithoutChangingAnchor() {
        val restored = TrustedClockEvidence.calculate(server, 100L, "anchor", true, 100L, ms)
        assertEquals("AVAILABLE", restored.status)
        assertEquals("anchor", restored.anchorUuid)
    }
    @Test fun wrongBootOrDeviceCannotUseAnOldReference() {
        assertFalse(TrustedClockEvidence.calculate(server, 100L, "anchor", false, 200L, ms).available)
    }
    @Test fun rebootElapsedRegressionMalformedOrMissingAnchorsStayUnavailable() {
        for (clock in listOf(
            TrustedClockEvidence.calculate(server, 100L, "anchor", true, 50L, ms),
            TrustedClockEvidence.calculate("invalid", 100L, "anchor", true, 200L, ms),
            TrustedClockEvidence.calculate(null, null, null, true, 200L, ms),
            TrustedClockEvidence.calculate(server, -1L, "anchor", true, 200L, ms)
        )) { assertFalse(clock.available); assertNull(clock.trustedTimeMs) }
    }
}

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
    @Test fun calendarDayChangeKeepsSameBootAnchorAndDetectsFakePhoneDate() {
        val twoDays = 2L * 24 * 60 * 60 * 1000
        val nextDay = TrustedClockEvidence.calculate(server, 100L, "anchor", true,
            100L + twoDays, ms + twoDays - 7_200_000L)
        assertEquals(ms + twoDays, nextDay.trustedTimeMs)
        assertEquals("anchor", nextDay.anchorUuid)
        assertTrue(nextDay.mismatch)
    }

    @Test fun renewedServerAnchorCannotAdoptTheChangedPhoneClock() {
        val elapsed = 7_200_100L
        val wrongPhoneTime = ms - 3_600_000L
        val original = TrustedClockEvidence.calculate(server, 100L, "original", true,
            elapsed, wrongPhoneTime)
        val refreshed = TrustedClockEvidence.calculate("2026-10-16T08:00:00Z", elapsed,
            "new-server-anchor", true, elapsed, wrongPhoneTime)
        assertEquals(original.trustedTimeMs, refreshed.trustedTimeMs)
        assertTrue(original.mismatch)
        assertTrue(refreshed.mismatch)
    }

}

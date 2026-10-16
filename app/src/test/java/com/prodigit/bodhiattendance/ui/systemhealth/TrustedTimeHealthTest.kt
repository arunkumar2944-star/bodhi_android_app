package com.prodigit.bodhiattendance.ui.systemhealth

import com.prodigit.bodhiattendance.core.monitoring.TrustedClockEvidence
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TrustedTimeHealthTest {
    private val server = "2026-10-16T06:00:00Z"
    private val ms = Instant.parse(server).toEpochMilli()
    private fun clock(offsetMs: Long) = TrustedClockEvidence.calculate(
        server, 100L, "anchor-a", true, 60_100L, ms + 60_000L + offsetMs
    )

    @Test fun clockAheadOrBehindRequiresActionEvenWhenAttendanceTimeIsAvailable() {
        for (offset in listOf(-7_200_000L, 7_200_000L)) {
            val evidence = clock(offset)
            assertTrue(evidence.available)
            assertEquals(HealthStatus.NOT_READY, trustedTimeHealth(evidence).status)
            assertTrue(trustedTimeHealth(evidence).message.contains("automatic date & time"))
            assertEquals(ms + 60_000L, evidence.trustedTimeMs)
        }
    }

    @Test fun correctedClockIsReadyWithoutReplacingTheAnchor() {
        val changed = clock(-7_200_000L)
        val corrected = clock(0L)
        assertEquals(HealthStatus.NOT_READY, trustedTimeHealth(changed).status)
        assertEquals(HealthStatus.READY, trustedTimeHealth(corrected).status)
        assertEquals(changed.anchorUuid, corrected.anchorUuid)
        assertEquals(changed.trustedTimeMs, corrected.trustedTimeMs)
    }

    @Test fun missingOrPreviousBootReferenceRequiresServerVerification() {
        for (evidence in listOf(
            TrustedClockEvidence.calculate(null, null, null, true, 100L, ms),
            TrustedClockEvidence.calculate(server, 100L, "old-boot", false, 200L, ms)
        )) {
            assertEquals(HealthStatus.NOT_READY, trustedTimeHealth(evidence).status)
            assertTrue(trustedTimeHealth(evidence).message.contains("server verification required"))
        }
    }

    @Test fun toleranceBoundaryDoesNotHideLargerOffsets() {
        for (offset in listOf(-60_000L, 60_000L)) {
            assertEquals(HealthStatus.READY, trustedTimeHealth(clock(offset)).status)
        }
        for (offset in listOf(-60_001L, 60_001L)) {
            assertEquals(HealthStatus.NOT_READY, trustedTimeHealth(clock(offset)).status)
        }
    }
}

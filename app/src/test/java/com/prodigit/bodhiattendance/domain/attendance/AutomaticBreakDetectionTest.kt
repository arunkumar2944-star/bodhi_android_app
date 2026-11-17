package com.prodigit.bodhiattendance.domain.attendance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticBreakDetectionTest {

    private val policy = AttendanceEnginePolicy(
        gracePeriodMs = 180_000L, // 3 minutes
        maximumAbsenceMs = 5_400_000L, // 90 minutes
        wifiRequired = true,
        bleRequired = false
    )

    private fun officePresence(inside: Boolean, wifiMatched: Boolean, elapsedMs: Long) = OfficePresenceSnapshot(
        geofenceVerified = inside,
        wifiVerified = wifiMatched,
        beaconVerified = false,
        capturedAtElapsedRealtimeMs = elapsedMs,
        observationUncertain = false
    )

    @Test
    fun wifiLossAloneDoesNotStartBreak() {
        val snapshot = officePresence(inside = true, wifiMatched = false, elapsedMs = 1000L)
        assertTrue(!snapshot.isValidOfficePresence(policy))
    }

    @Test
    fun presenceLossStartsAbsenceGrace() {
        val startElapsed = 1000L
        val outsidePresence = officePresence(inside = false, wifiMatched = false, elapsedMs = startElapsed)
        val decision = AttendancePresenceDecisionEngine.evaluate(
            attendanceState = AttendanceState.WORKING,
            currentPhase = AttendanceEnginePhase.Present,
            presence = outsidePresence,
            policy = policy
        )

        assertTrue(decision is AttendanceEngineDecision.NoAttendanceEvent)
        val noEvent = decision as AttendanceEngineDecision.NoAttendanceEvent
        val nextPhase = noEvent.nextPhase
        assertTrue(nextPhase is AttendanceEnginePhase.AbsenceGrace)
        assertEquals(startElapsed, (nextPhase as AttendanceEnginePhase.AbsenceGrace).absenceStartedElapsedRealtimeMs)
    }

    @Test
    fun graceCompletionGeneratesBreakStartEventBackdatedToExitTime() {
        val startElapsed = 1000L
        val gracePhase = AttendanceEnginePhase.AbsenceGrace(absenceStartedElapsedRealtimeMs = startElapsed)

        // 3 minutes and 1 second later (181,000 ms)
        val laterElapsed = startElapsed + 181_000L
        val outsidePresence = officePresence(inside = false, wifiMatched = false, elapsedMs = laterElapsed)

        val decision = AttendancePresenceDecisionEngine.evaluate(
            attendanceState = AttendanceState.WORKING,
            currentPhase = gracePhase,
            presence = outsidePresence,
            policy = policy
        )

        assertTrue(decision is AttendanceEngineDecision.CreateAttendanceEvent)
        val createEvent = decision as AttendanceEngineDecision.CreateAttendanceEvent
        assertEquals(AttendanceEventType.BREAK_START, createEvent.eventType)
        assertEquals(startElapsed, createEvent.eventElapsedRealtimeMs) // Backdated to initial exit
    }

    @Test
    fun returnDuringBreakGeneratesBreakEndEvent() {
        val startElapsed = 1000L
        val breakPhase = AttendanceEnginePhase.BreakActive(
            absenceStartedElapsedRealtimeMs = startElapsed,
            breakConfirmedElapsedRealtimeMs = startElapsed + 181_000L
        )

        val returnElapsed = startElapsed + 600_000L // Returned 10 mins later
        val insidePresence = officePresence(inside = true, wifiMatched = true, elapsedMs = returnElapsed)

        val decision = AttendancePresenceDecisionEngine.evaluate(
            attendanceState = AttendanceState.ON_BREAK,
            currentPhase = breakPhase,
            presence = insidePresence,
            policy = policy
        )

        assertTrue(decision is AttendanceEngineDecision.CreateAttendanceEvent)
        val createEvent = decision as AttendanceEngineDecision.CreateAttendanceEvent
        assertEquals(AttendanceEventType.BREAK_END, createEvent.eventType)
        assertEquals(returnElapsed, createEvent.eventElapsedRealtimeMs)
        assertEquals(AttendanceEnginePhase.Present, createEvent.nextPhase)
    }

    @Test
    fun maximumAbsenceExceededGeneratesCheckoutBackdatedToExitTime() {
        val startElapsed = 1000L
        val breakPhase = AttendanceEnginePhase.BreakActive(
            absenceStartedElapsedRealtimeMs = startElapsed,
            breakConfirmedElapsedRealtimeMs = startElapsed + 181_000L
        )

        // 90 minutes and 1 second later (5,401,000 ms)
        val checkoutElapsed = startElapsed + 5_401_000L
        val outsidePresence = officePresence(inside = false, wifiMatched = false, elapsedMs = checkoutElapsed)

        val decision = AttendancePresenceDecisionEngine.evaluate(
            attendanceState = AttendanceState.ON_BREAK,
            currentPhase = breakPhase,
            presence = outsidePresence,
            policy = policy
        )

        assertTrue(decision is AttendanceEngineDecision.ClassifyCheckout)
        val checkoutDecision = decision as AttendanceEngineDecision.ClassifyCheckout
        assertEquals(startElapsed, checkoutDecision.effectiveCheckoutElapsedRealtimeMs) // Backdated to initial exit
    }
}

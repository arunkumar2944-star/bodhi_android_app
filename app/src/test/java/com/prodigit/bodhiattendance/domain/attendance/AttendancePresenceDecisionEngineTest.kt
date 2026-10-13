package com.prodigit.bodhiattendance.domain.attendance

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class AttendancePresenceDecisionEngineTest {


    // ========================================================
    // TEST POLICY
    //
    // 3 minutes  = 180,000 ms
    // 90 minutes = 5,400,000 ms
    // ========================================================

    private val policy =
        AttendanceEnginePolicy(
            gracePeriodMs = 3 * 60 * 1000L,
            maximumAbsenceMs = 90 * 60 * 1000L,
            wifiRequired = true,
            bleRequired = false
        )


    // ========================================================
    // TEST 1
    //
    // NOT_CHECKED_IN
    // +
    // Geofence verified
    // +
    // Wi-Fi verified
    //
    // => CHECK_IN
    // ========================================================

    @Test
    fun validPresence_whenNotCheckedIn_createsCheckIn() {

        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = true,
                wifiVerified = true,
                capturedAtElapsedRealtimeMs = 1_000_000L
            )


        val result =
            AttendancePresenceDecisionEngine.evaluate(
                attendanceState =
                    AttendanceState.NOT_CHECKED_IN,

                currentPhase =
                    AttendanceEnginePhase.NotMonitoring,

                presence =
                    snapshot,

                policy =
                    policy
            )


        assertTrue(
            result is AttendanceEngineDecision.CreateAttendanceEvent
        )


        result as AttendanceEngineDecision.CreateAttendanceEvent


        assertEquals(
            AttendanceEventType.CHECK_IN,
            result.eventType
        )


        assertEquals(
            AttendanceState.WORKING,
            result.expectedNextAttendanceState
        )


        assertEquals(
            1_000_000L,
            result.eventElapsedRealtimeMs
        )
    }


    // ========================================================
    // TEST 2
    //
    // WORKING
    // +
    // presence lost
    //
    // => start grace
    //
    // NO attendance event
    // ========================================================

    @Test
    fun presenceLost_whenWorking_startsGraceWithoutAttendanceEvent() {

        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = false,
                wifiVerified = true,
                capturedAtElapsedRealtimeMs = 2_000_000L
            )


        val result =
            AttendancePresenceDecisionEngine.evaluate(
                attendanceState =
                    AttendanceState.WORKING,

                currentPhase =
                    AttendanceEnginePhase.Present,

                presence =
                    snapshot,

                policy =
                    policy
            )


        assertTrue(
            result is AttendanceEngineDecision.NoAttendanceEvent
        )


        result as AttendanceEngineDecision.NoAttendanceEvent


        assertTrue(
            result.nextPhase is
                    AttendanceEnginePhase.AbsenceGrace
        )


        val grace =
            result.nextPhase as
                    AttendanceEnginePhase.AbsenceGrace


        assertEquals(
            2_000_000L,
            grace.absenceStartedElapsedRealtimeMs
        )
    }


    // ========================================================
    // TEST 3
    //
    // Employee returns before 3 minutes.
    //
    // => grace cancelled
    // => PRESENT
    // => NO BREAK_START
    // ========================================================

    @Test
    fun presenceReturnsBeforeGraceEnds_doesNotCreateBreakStart() {

        val absenceStarted =
            3_000_000L


        val gracePhase =
            AttendanceEnginePhase.AbsenceGrace(
                absenceStartedElapsedRealtimeMs =
                    absenceStarted
            )


        // Returns after 2 minutes.

        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = true,
                wifiVerified = true,
                capturedAtElapsedRealtimeMs =
                    absenceStarted +
                            (2 * 60 * 1000L)
            )


        val result =
            AttendancePresenceDecisionEngine.evaluate(
                attendanceState =
                    AttendanceState.WORKING,

                currentPhase =
                    gracePhase,

                presence =
                    snapshot,

                policy =
                    policy
            )


        assertTrue(
            result is AttendanceEngineDecision.NoAttendanceEvent
        )


        result as AttendanceEngineDecision.NoAttendanceEvent


        assertEquals(
            AttendanceEnginePhase.Present,
            result.nextPhase
        )
    }


    // ========================================================
    // TEST 4
    //
    // Employee remains absent for 3 minutes.
    //
    // => BREAK_START
    // ========================================================

    @Test
    fun absenceReachesGracePeriod_createsBreakStart() {

        val absenceStarted =
            4_000_000L


        val gracePhase =
            AttendanceEnginePhase.AbsenceGrace(
                absenceStartedElapsedRealtimeMs =
                    absenceStarted
            )


        val threeMinutesLater =
            absenceStarted +
                    (3 * 60 * 1000L)


        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = false,
                wifiVerified = false,
                capturedAtElapsedRealtimeMs =
                    threeMinutesLater
            )


        val result =
            AttendancePresenceDecisionEngine.evaluate(
                attendanceState =
                    AttendanceState.WORKING,

                currentPhase =
                    gracePhase,

                presence =
                    snapshot,

                policy =
                    policy
            )


        assertTrue(
            result is AttendanceEngineDecision.CreateAttendanceEvent
        )


        result as AttendanceEngineDecision.CreateAttendanceEvent


        assertEquals(
            AttendanceEventType.BREAK_START,
            result.eventType
        )


        assertEquals(
            AttendanceState.ON_BREAK,
            result.expectedNextAttendanceState
        )


        assertEquals(
            threeMinutesLater,
            result.eventElapsedRealtimeMs
        )


        assertTrue(
            result.nextPhase is
                    AttendanceEnginePhase.BreakActive
        )
    }


    // ========================================================
    // TEST 5
    //
    // ON_BREAK
    // +
    // presence restored
    //
    // => BREAK_END
    // ========================================================

    @Test
    fun presenceRestored_whenOnBreak_createsBreakEnd() {

        val absenceStarted =
            5_000_000L


        val breakStarted =
            absenceStarted +
                    (3 * 60 * 1000L)


        val breakPhase =
            AttendanceEnginePhase.BreakActive(
                absenceStartedElapsedRealtimeMs =
                    absenceStarted,

                breakConfirmedElapsedRealtimeMs =
                    breakStarted
            )


        val returnedAt =
            absenceStarted +
                    (20 * 60 * 1000L)


        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = true,
                wifiVerified = true,
                capturedAtElapsedRealtimeMs =
                    returnedAt
            )


        val result =
            AttendancePresenceDecisionEngine.evaluate(
                attendanceState =
                    AttendanceState.ON_BREAK,

                currentPhase =
                    breakPhase,

                presence =
                    snapshot,

                policy =
                    policy
            )


        assertTrue(
            result is AttendanceEngineDecision.CreateAttendanceEvent
        )


        result as AttendanceEngineDecision.CreateAttendanceEvent


        assertEquals(
            AttendanceEventType.BREAK_END,
            result.eventType
        )


        assertEquals(
            AttendanceState.WORKING,
            result.expectedNextAttendanceState
        )


        assertEquals(
            returnedAt,
            result.eventElapsedRealtimeMs
        )
    }


    // ========================================================
    // TEST 6
    //
    // Employee remains absent for 89 minutes.
    //
    // => still BREAK_ACTIVE
    // => NO CHECK_OUT
    // ========================================================

    @Test
    fun absenceBelowMaximum_doesNotClassifyCheckout() {

        val absenceStarted =
            6_000_000L


        val breakPhase =
            AttendanceEnginePhase.BreakActive(
                absenceStartedElapsedRealtimeMs =
                    absenceStarted,

                breakConfirmedElapsedRealtimeMs =
                    absenceStarted +
                            (3 * 60 * 1000L)
            )


        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = false,
                wifiVerified = false,
                capturedAtElapsedRealtimeMs =
                    absenceStarted +
                            (89 * 60 * 1000L)
            )


        val result =
            AttendancePresenceDecisionEngine.evaluate(
                attendanceState =
                    AttendanceState.ON_BREAK,

                currentPhase =
                    breakPhase,

                presence =
                    snapshot,

                policy =
                    policy
            )


        assertTrue(
            result is AttendanceEngineDecision.NoAttendanceEvent
        )


        result as AttendanceEngineDecision.NoAttendanceEvent


        assertTrue(
            result.nextPhase is
                    AttendanceEnginePhase.BreakActive
        )
    }


    // ========================================================
    // TEST 7
    //
    // Employee remains absent for 90 minutes.
    //
    // => classify CHECK_OUT
    //
    // IMPORTANT:
    //
    // effective checkout time must be ORIGINAL absence time,
    // NOT 90-minute decision time.
    // ========================================================

    @Test
    fun absenceReachesMaximum_classifiesOriginalExitAsCheckout() {

        val absenceStarted =
            7_000_000L


        val breakPhase =
            AttendanceEnginePhase.BreakActive(
                absenceStartedElapsedRealtimeMs =
                    absenceStarted,

                breakConfirmedElapsedRealtimeMs =
                    absenceStarted +
                            (3 * 60 * 1000L)
            )


        val classifiedAt =
            absenceStarted +
                    (90 * 60 * 1000L)


        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = false,
                wifiVerified = false,
                capturedAtElapsedRealtimeMs =
                    classifiedAt
            )


        val result =
            AttendancePresenceDecisionEngine.evaluate(
                attendanceState =
                    AttendanceState.ON_BREAK,

                currentPhase =
                    breakPhase,

                presence =
                    snapshot,

                policy =
                    policy
            )


        assertTrue(
            result is AttendanceEngineDecision.ClassifyCheckout
        )


        result as AttendanceEngineDecision.ClassifyCheckout


        assertEquals(
            absenceStarted,
            result.effectiveCheckoutElapsedRealtimeMs
        )


        assertEquals(
            classifiedAt,
            result.classifiedAtElapsedRealtimeMs
        )
    }


    // ========================================================
    // TEST 8
    //
    // Geofence true
    // Wi-Fi false
    //
    // This is NOT valid office presence.
    //
    // Both are mandatory in V1.
    // ========================================================

    @Test
    fun geofenceOnly_isNotValidOfficePresence() {

        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = true,
                wifiVerified = false,
                capturedAtElapsedRealtimeMs = 8_000_000L
            )


        assertEquals(
            false,
            snapshot.isValidOfficePresence
        )
    }


    // ========================================================
    // TEST 9
    //
    // Wi-Fi true
    // Geofence false
    //
    // Also NOT valid presence.
    // ========================================================

    @Test
    fun wifiOnly_isNotValidOfficePresence() {

        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = false,
                wifiVerified = true,
                capturedAtElapsedRealtimeMs = 9_000_000L
            )


        assertEquals(
            false,
            snapshot.isValidOfficePresence
        )
    }


    // ========================================================
    // TEST 10
    //
    // Both verified.
    // ========================================================

    @Test
    fun geofenceAndWifi_isValidOfficePresence() {

        val snapshot =
            OfficePresenceSnapshot(
                geofenceVerified = true,
                wifiVerified = true,
                capturedAtElapsedRealtimeMs = 10_000_000L
            )


        assertEquals(
            true,
            snapshot.isValidOfficePresence
        )
    }
}

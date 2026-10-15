package com.prodigit.bodhiattendance.domain.attendance

import org.junit.Assert.*
import org.junit.Test

class DeparturePhaseRecoveryTest {
    @Test fun matchingWorkingStateRetainsOriginalGrace() {
        val phase = AttendanceEnginePhase.AbsenceGrace(1000L)
        assertEquals(phase, DeparturePhaseRecovery.compatible(AttendanceState.WORKING, phase))
    }

    @Test fun matchingBreakStateRetainsOriginalCheckoutTiming() {
        val phase = AttendanceEnginePhase.BreakActive(1000L, 181000L)
        assertEquals(phase, DeparturePhaseRecovery.compatible(AttendanceState.ON_BREAK, phase))
    }

    @Test fun serverStateChangeDoesNotRestoreOldDeparturePhase() {
        assertNull(DeparturePhaseRecovery.compatible(AttendanceState.CHECKED_OUT, AttendanceEnginePhase.AbsenceGrace(1000L)))
        assertNull(DeparturePhaseRecovery.compatible(AttendanceState.WORKING, AttendanceEnginePhase.BreakActive(1000L, 181000L)))
        assertNull(DeparturePhaseRecovery.compatible(AttendanceState.ON_BREAK, AttendanceEnginePhase.AbsenceGrace(1000L)))
        assertNull(DeparturePhaseRecovery.compatible(AttendanceState.NOT_CHECKED_IN, AttendanceEnginePhase.AbsenceGrace(1000L)))
    }

    @Test fun missingOrBootInvalidatedTimingCannotBeInvented() {
        assertNull(DeparturePhaseRecovery.compatible(AttendanceState.WORKING, null))
        assertNull(DeparturePhaseRecovery.compatible(AttendanceState.ON_BREAK, null))
    }
}

package com.prodigit.bodhiattendance.domain.attendance

/** Only the visible-start gate supplies boot-validated, same-workday Room timing. */
object DeparturePhaseRecovery {
    fun compatible(state: AttendanceState, phase: AttendanceEnginePhase?): AttendanceEnginePhase? = when {
        state == AttendanceState.WORKING && phase is AttendanceEnginePhase.AbsenceGrace -> phase
        state == AttendanceState.ON_BREAK && phase is AttendanceEnginePhase.BreakActive -> phase
        else -> null
    }
}

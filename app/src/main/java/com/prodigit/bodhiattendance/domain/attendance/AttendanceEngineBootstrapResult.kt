package com.prodigit.bodhiattendance.domain.attendance


// ============================================================
// ATTENDANCE ENGINE BOOTSTRAP RESULT
// ============================================================

sealed class AttendanceEngineBootstrapResult {


    // ========================================================
    // RECONCILIATION COMPLETED
    //
    // Fresh server state has been normalized and persisted
    // locally.
    // ========================================================

    data class Ready(

        val attendanceState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase

    ) : AttendanceEngineBootstrapResult()


    // ========================================================
    // TEMPORARY FAILURE
    //
    // Examples:
    //
    // - network unavailable
    // - Express temporarily unavailable
    // - time-anchor request failed
    //
    // Runtime must remain paused.
    // ========================================================

    data class RetryLater(

        val reason:
        String,

        val error:
        Throwable? = null

    ) : AttendanceEngineBootstrapResult()


    // ========================================================
    // UNSAFE / UNSUPPORTED SERVER STATE
    //
    // Automatic attendance must remain blocked.
    // ========================================================

    data class Blocked(

        val reason:
        String

    ) : AttendanceEngineBootstrapResult()


    // ========================================================
    // UNEXPECTED FAILURE
    // ========================================================

    data class Failure(

        val error:
        Throwable

    ) : AttendanceEngineBootstrapResult()
}
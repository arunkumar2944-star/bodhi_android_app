package com.prodigit.bodhiattendance.data.remote.dto.attendance


// ============================================================
// ATTENDANCE BOOTSTRAP RESPONSE
//
// Returned by:
//
// GET /api/v1/attendance/bootstrap-state
//
// Purpose:
//
// Gives Android the authoritative current attendance state from
// Express/PostgreSQL during:
//
// - first startup
// - local state unavailable
// - device reboot
// - stale local state
// - backend rejection recovery
// - persistence recovery
// ============================================================

data class AttendanceBootstrapResponse(

    val success: Boolean,

    val message: String,

    val data: AttendanceBootstrapData?
)


// ============================================================
// BOOTSTRAP DATA
// ============================================================

data class AttendanceBootstrapData(

    // --------------------------------------------------------
    // Current authoritative attendance state
    //
    // Expected values:
    //
    // NOT_CHECKED_IN
    // WORKING
    // ON_BREAK
    // CHECKED_OUT
    // ON_DUTY
    // --------------------------------------------------------

    val attendanceState: String,


    // --------------------------------------------------------
    // Latest accepted attendance event
    //
    // Expected:
    //
    // CHECK_IN
    // BREAK_START
    // BREAK_END
    // CHECK_OUT
    //
    // Null when no attendance event exists.
    // --------------------------------------------------------

    val latestEventType: String?,

    val lastEvent: AttendanceBootstrapLastEvent? = null,


    // --------------------------------------------------------
    // Current/most recent attendance summary ID
    // --------------------------------------------------------

    val attendanceId: Long?,


    // --------------------------------------------------------
    // Manager-approved outside work status
    // --------------------------------------------------------

    val onDutyActive: Boolean,


    // --------------------------------------------------------
    // Optional office currently associated with attendance.
    // --------------------------------------------------------

    val officeId: Long? = null,


    // --------------------------------------------------------
    // Optional server timestamp.
    //
    // This is informational here.
    //
    // Trusted attendance timing still uses TimeAnchorManager.
    // --------------------------------------------------------

    val serverTime: String? = null
)

data class AttendanceBootstrapLastEvent(val eventType: String? = null)

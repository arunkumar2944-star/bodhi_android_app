package com.prodigit.bodhiattendance.domain.attendance


// ============================================================
// AUTHORITATIVE ATTENDANCE BOOTSTRAP STATE
//
// Normalized Android-side representation of the current
// attendance state returned by Express.
//
// IMPORTANT:
//
// This is NOT a Retrofit DTO.
//
// The concrete backend repository may map any Express response
// shape into this model.
//
// This keeps the attendance engine independent from API JSON
// structure.
// ============================================================

data class AttendanceBootstrapState(

    // ========================================================
    // CURRENT AUTHORITATIVE STATE
    // ========================================================

    val attendanceState:
    AttendanceState,


    // ========================================================
    // LATEST ACCEPTED EVENT
    //
    // Null means Express currently has no automatic attendance
    // event for the active/current attendance context.
    // ========================================================

    val latestEventType:
    AttendanceEventType? = null,


    // ========================================================
    // OPTIONAL ACTIVE ATTENDANCE ID
    // ========================================================

    val attendanceId:
    Long? = null,


    // ========================================================
    // OUTSIDE WORK / ON DUTY
    //
    // Future server bootstrap implementation may map approved
    // outside-work state here.
    // ========================================================

    val onDutyActive:
    Boolean = false
)
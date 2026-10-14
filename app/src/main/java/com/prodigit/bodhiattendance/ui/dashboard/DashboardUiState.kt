package com.prodigit.bodhiattendance.ui.dashboard

import com.prodigit.bodhiattendance.ui.components.AttendanceTimelineItem


// ============================================================
// EMPLOYEE WORK STATUS
// ============================================================

enum class AttendanceWorkStatus {

    UNKNOWN,

    IN_OFFICE,

    ON_BREAK,

    ON_DUTY,

    NOT_CHECKED_IN,

    CHECKED_OUT
}


// ============================================================
// DASHBOARD UI STATE
// ============================================================

data class DashboardUiState(

    // ------------------------------------------------------------
    // LOADING
    // ------------------------------------------------------------

    val isLoading: Boolean = true,


    // ------------------------------------------------------------
    // EMPLOYEE
    // ------------------------------------------------------------

    val employeeName: String = "",

    val employeeId: String = "",

    val designation: String = "",


    // ------------------------------------------------------------
    // CURRENT ATTENDANCE STATUS
    // ------------------------------------------------------------

    val workStatus: AttendanceWorkStatus =
        AttendanceWorkStatus.UNKNOWN,

    val officeName: String = "",

    val statusSince: String? = null,


    // ------------------------------------------------------------
    // SYSTEM HEALTH
    // ------------------------------------------------------------

    val gpsReady: Boolean = false,

    val bleReady: Boolean = false,

    val syncReady: Boolean = false,


    // ------------------------------------------------------------
    // TODAY ATTENDANCE
    // ------------------------------------------------------------

    val checkInTime: String? = null,

    val checkInStatus: String? = null,

    val checkOutTime: String? = null,

    val hasCheckedOut: Boolean = false,


    // ------------------------------------------------------------
    // WORK / BREAK SUMMARY
    // ------------------------------------------------------------

    val netWorkHours: String = "--",

    val teaBreakDuration: String = "--",

    val lunchBreakDuration: String = "--",

    val otherBreakDuration: String = "--",


    // ------------------------------------------------------------
    // TIMELINE
    // ------------------------------------------------------------

    val timelineEvents:
    List<AttendanceTimelineItem> =
        emptyList(),


    // ------------------------------------------------------------
    // NOTIFICATIONS
    // ------------------------------------------------------------

    val notificationCount: Int = 0,


    // ------------------------------------------------------------
    // ERROR
    // ------------------------------------------------------------

    val errorMessage: String? = null
)
package com.prodigit.bodhiattendance.ui.dashboard

import androidx.lifecycle.ViewModel
import com.prodigit.bodhiattendance.ui.components.AttendanceTimelineItem
import com.prodigit.bodhiattendance.ui.components.AttendanceTimelineType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class DashboardViewModel : ViewModel() {


    // ============================================================
    // UI STATE
    // ============================================================

    private val _uiState =
        MutableStateFlow(
            DashboardUiState()
        )


    val uiState:
            StateFlow<DashboardUiState> =
        _uiState.asStateFlow()


    // ============================================================
    // TEMPORARY DASHBOARD TEST DATA
    //
    // IMPORTANT:
    // This data is ONLY for UI development.
    //
    // Later this entire section will be replaced with:
    //
    // Repository
    //      ↓
    // Room database
    //      ↓
    // Attendance API
    //      ↓
    // DashboardUiState
    //
    // ============================================================

    init {

        loadTestDashboard()
    }


    private fun loadTestDashboard() {

        _uiState.value =
            DashboardUiState(

                isLoading = false,


                // ------------------------------------------------
                // EMPLOYEE
                // ------------------------------------------------

                employeeName =
                    "Arun Kumar",

                employeeId =
                    "EMP001",

                designation =
                    "Android Developer",


                // ------------------------------------------------
                // CURRENT STATUS
                // ------------------------------------------------

                workStatus =
                    AttendanceWorkStatus.IN_OFFICE,

                officeName =
                    "Main Office",

                statusSince =
                    "09:12 AM",


                // ------------------------------------------------
                // SYSTEM HEALTH
                // ------------------------------------------------

                gpsReady =
                    true,

                bleReady =
                    true,

                syncReady =
                    true,


                // ------------------------------------------------
                // TODAY ATTENDANCE
                // ------------------------------------------------

                checkInTime =
                    "09:12 AM",

                checkInStatus =
                    "On Time",

                checkOutTime =
                    null,

                hasCheckedOut =
                    false,


                // ------------------------------------------------
                // WORK SUMMARY
                // ------------------------------------------------

                netWorkHours =
                    "7h 45m",

                teaBreakDuration =
                    "15m",

                lunchBreakDuration =
                    "45m",

                otherBreakDuration =
                    "0m",


                // ------------------------------------------------
                // TIMELINE
                // ------------------------------------------------

                timelineEvents =
                    listOf(

                        AttendanceTimelineItem(

                            type =
                                AttendanceTimelineType
                                    .CHECK_IN,

                            time =
                                "09:12",

                            title =
                                "Checked In",

                            description =
                                "Main Office"
                        ),


                        AttendanceTimelineItem(

                            type =
                                AttendanceTimelineType
                                    .TEA_BREAK,

                            time =
                                "11:15",

                            title =
                                "Tea Break",

                            description =
                                "Break started"
                        ),


                        AttendanceTimelineItem(

                            type =
                                AttendanceTimelineType
                                    .BACK_TO_WORK,

                            time =
                                "11:30",

                            title =
                                "Back in Office",

                            description =
                                "Office presence detected"
                        ),


                        AttendanceTimelineItem(

                            type =
                                AttendanceTimelineType
                                    .LUNCH_BREAK,

                            time =
                                "01:15",

                            title =
                                "Lunch Break",

                            description =
                                "Lunch break started"
                        ),


                        AttendanceTimelineItem(

                            type =
                                AttendanceTimelineType
                                    .BACK_TO_WORK,

                            time =
                                "02:00",

                            title =
                                "Back to Work",

                            description =
                                "Office presence detected"
                        )
                    ),


                // ------------------------------------------------
                // NOTIFICATIONS
                // ------------------------------------------------

                notificationCount =
                    3
            )
    }
}
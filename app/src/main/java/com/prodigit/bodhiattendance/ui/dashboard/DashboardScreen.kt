package com.prodigit.bodhiattendance.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel


import com.prodigit.bodhiattendance.ui.components.AttendanceTimelineCard
import com.prodigit.bodhiattendance.ui.components.BreakSummaryRow
import com.prodigit.bodhiattendance.ui.components.EmployeeStatusCard
import com.prodigit.bodhiattendance.ui.components.QuickAccessCard
import com.prodigit.bodhiattendance.ui.components.TodayAttendanceCard

import com.prodigit.bodhiattendance.ui.attendance.TimeAnchorViewModel
import com.prodigit.bodhiattendance.ui.attendance.AttendanceEventViewModel


// ============================================================
// DASHBOARD SCREEN
//
// Dashboard is BODY CONTENT only.
//
// The following are owned by MainAppLayout:
//
// - AppBackground
// - AppMasterHeader
// - Scaffold
// - AppBottomNavigation
//
// Dashboard must NOT create another application
// header or bottom navigation.
// ============================================================

@Composable
fun DashboardScreen(

    onAttendanceClick: () -> Unit = {},

    onOnDutyClick: () -> Unit = {},

    onRequestsClick: () -> Unit = {},

    onSystemHealthClick: () -> Unit = {},

    viewModel: DashboardViewModel = viewModel(),

    timeAnchorViewModel: TimeAnchorViewModel = viewModel(),

    attendanceEventViewModel: AttendanceEventViewModel = viewModel()

) {

    // ========================================================
    // DASHBOARD STATE
    // ========================================================

    val uiState by
    viewModel
        .uiState
        .collectAsState()


    // ========================================================
    // ATTENDANCE VIEW REDIRECT
    // ========================================================

//    val openAttendanceView: () -> Unit = {
//
//        navController.navigate("attendance_view") {
//
//            // Prevent multiple copies if user taps repeatedly
//            launchSingleTop = true
//        }
//    }


    // ========================================================
    // LOADING
    // ========================================================

    if (uiState.isLoading) {

        Box(
            modifier =
                Modifier.fillMaxSize(),

            contentAlignment =
                Alignment.Center
        ) {

            CircularProgressIndicator()
        }

    } else {


        // ====================================================
        // DASHBOARD BODY
        // ====================================================

        DashboardContent(

            uiState =
                uiState,

            onAttendanceClick =
                onAttendanceClick,

            onOnDutyClick =
                onOnDutyClick,

            onRequestsClick =
                onRequestsClick,

            onSystemHealthClick =
                onSystemHealthClick,

            onTestTimeAnchorClick = {

                timeAnchorViewModel
                    .createTimeAnchor()
            },

            onTestCheckInClick = {

                attendanceEventViewModel.testAttendanceEvent(
                    eventType = "BREAK_START"
                )
            }
        )
    }
}


// ============================================================
// DASHBOARD CONTENT
// ============================================================

@Composable
private fun DashboardContent(

    modifier: Modifier = Modifier,

    uiState: DashboardUiState,

    onAttendanceClick: () -> Unit,

    onOnDutyClick: () -> Unit,

    onRequestsClick: () -> Unit,

    onSystemHealthClick: () -> Unit,

    onTestTimeAnchorClick: () -> Unit,

    onTestCheckInClick: () -> Unit

) {

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize(),

        contentPadding =
            PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 30.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                16.dp
            )

    ) {


        // ====================================================
        // CURRENT EMPLOYEE STATUS
        // ====================================================

        item {

            EmployeeStatusCard(

                status =
                    uiState.workStatus,

                officeName =
                    uiState.officeName,

                statusSince =
                    uiState.statusSince,

                gpsReady =
                    uiState.gpsReady,

                bleReady =
                    uiState.bleReady,

                syncReady =
                    uiState.syncReady
            )
        }


        // ====================================================
        // TODAY ATTENDANCE
        // ====================================================

        item {

            TodayAttendanceCard(

                checkInTime =
                    uiState.checkInTime,

                checkInStatus =
                    uiState.checkInStatus,

                checkOutTime =
                    uiState.checkOutTime,

                hasCheckedOut =
                    uiState.hasCheckedOut,

                onViewDetailsClick =
                    onAttendanceClick
            )
        }


        // ====================================================
        // WORK SUMMARY
        // ====================================================

        item {

            BreakSummaryRow(

                netWorkHours =
                    uiState.netWorkHours,

                teaBreakDuration =
                    uiState.teaBreakDuration,

                lunchBreakDuration =
                    uiState.lunchBreakDuration,

                otherBreakDuration =
                    uiState.otherBreakDuration
            )
        }


        // ====================================================
        // TODAY ACTIVITY
        // ====================================================

        item {

            AttendanceTimelineCard(

                events =
                    uiState.timelineEvents,

                onViewAllClick =
                    onAttendanceClick
            )
        }


        // ====================================================
        // QUICK ACCESS
        // ====================================================

        item {

            QuickAccessCard(

                onAttendanceHistoryClick =
                    onAttendanceClick,

                onOnDutyClick =
                    onOnDutyClick,

                onRequestsClick =
                    onRequestsClick,

                onSystemHealthClick =
                    onSystemHealthClick
            )
        }


        // ====================================================
        // ERROR MESSAGE
        // ====================================================

        if (!uiState.errorMessage.isNullOrBlank()) {

            item {

                Text(

                    text =
                        uiState.errorMessage,

                    modifier =
                        Modifier.fillMaxWidth(),

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }
        }


        // ====================================================
        // TEMPORARY ATTENDANCE EVENT TEST
        //
        // DEVELOPMENT ONLY.
        // ====================================================

//        item {
//
//            Button(
//
//                onClick =
//                    onTestCheckInClick,
//
//                modifier =
//                    Modifier.fillMaxWidth()
//
//            ) {
//
//                Text(
//                    text = "Test BREAK_START"
//                )
//            }
//        }


        // ====================================================
        // BOTTOM SPACE
        // ====================================================

        item {

            Spacer(

                modifier =
                    Modifier.height(
                        8.dp
                    )
            )
        }
    }
}
package com.prodigit.bodhiattendance.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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



import com.prodigit.bodhiattendance.ui.components.AttendanceTimelineCard
import com.prodigit.bodhiattendance.ui.components.BreakSummaryRow
import com.prodigit.bodhiattendance.ui.components.EmployeeStatusCard
import com.prodigit.bodhiattendance.ui.components.QuickAccessCard
import com.prodigit.bodhiattendance.ui.components.TodayAttendanceCard



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
    showOnDuty: Boolean = false

) {

    // ========================================================
    // DASHBOARD STATE
    // ========================================================

    val live = com.prodigit.bodhiattendance.ui.live.liveUiState()
    val day = live.attendance?.let { read -> read.days.firstOrNull { it.date == read.today } }
    val holiday = live.attendance?.let { read -> com.prodigit.bodhiattendance.ui.live.holidayPresentation(read.today, read, live.onDuty) }
    val holidayToday = holiday != null
    val uiState = DashboardUiState(
        isLoading = live.loading,
        employeeName = listOfNotNull(live.profile?.first_name, live.profile?.last_name).joinToString(" "),
        employeeId = live.profile?.id?.toString().orEmpty(),
        designation = live.profile?.role.orEmpty(),
        workStatus = when {
            live.attendance == null -> AttendanceWorkStatus.UNKNOWN
            day?.state == "WORKING" -> AttendanceWorkStatus.IN_OFFICE
            day?.state == "ON_BREAK" -> AttendanceWorkStatus.ON_BREAK
            day?.state == "CHECKED_OUT" -> AttendanceWorkStatus.CHECKED_OUT
            day?.state == "ON_DUTY" -> AttendanceWorkStatus.ON_DUTY
            else -> AttendanceWorkStatus.NOT_CHECKED_IN
        },
        officeName = day?.officeName ?: if (holidayToday) "Company holiday — attendance not required" else if (live.attendance == null) "Office unavailable" else "No verified attendance today",
        statusSince = day?.events?.lastOrNull()?.timeMs?.let { com.prodigit.bodhiattendance.ui.live.displayTime(it) },
        syncReady = live.localEvents?.let { rows -> rows.none { it.syncStatus != "SYNCED" } } ?: false,
        checkInTime = day?.checkInTimeMs?.let { com.prodigit.bodhiattendance.ui.live.displayTime(it) },
        checkInStatus = if (day?.checkInTimeMs != null) "Confirmed by CRM" else null,
        checkOutTime = day?.checkOutTimeMs?.let { com.prodigit.bodhiattendance.ui.live.displayTime(it) },
        hasCheckedOut = day?.checkOutTimeMs != null,
        netWorkHours = day?.let { com.prodigit.bodhiattendance.ui.live.durationText(it.workMinutes) } ?: "--",
        otherBreakDuration = day?.let { com.prodigit.bodhiattendance.ui.live.durationText(it.breakMinutes) } ?: "--",
        timelineEvents = day?.events.orEmpty().mapNotNull { event ->
            val type = when (event.type) {
                "CHECK_IN" -> com.prodigit.bodhiattendance.ui.components.AttendanceTimelineType.CHECK_IN
                "BREAK_START" -> com.prodigit.bodhiattendance.ui.components.AttendanceTimelineType.BREAK_START
                "BREAK_END" -> com.prodigit.bodhiattendance.ui.components.AttendanceTimelineType.BACK_TO_WORK
                "CHECK_OUT" -> com.prodigit.bodhiattendance.ui.components.AttendanceTimelineType.CHECK_OUT
                else -> return@mapNotNull null
            }
            com.prodigit.bodhiattendance.ui.components.AttendanceTimelineItem(type,
                com.prodigit.bodhiattendance.ui.live.displayTime(event.timeMs), event.type.replace('_', ' '), "Confirmed by CRM")
        },
        errorMessage = live.errors["attendance"] ?: live.errors["all"]
    )

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
            holidayToday = holidayToday,
            holidayTitle = holiday?.title,
            holidayMessage = holiday?.message,

            onAttendanceClick =
                onAttendanceClick,

            onOnDutyClick =
                onOnDutyClick,

            onRequestsClick =
                onRequestsClick,

            onSystemHealthClick =
                onSystemHealthClick,
            showOnDuty = showOnDuty
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
    holidayToday: Boolean,
    holidayTitle: String?,
    holidayMessage: String?,

    onAttendanceClick: () -> Unit,

    onOnDutyClick: () -> Unit,

    onRequestsClick: () -> Unit,

    onSystemHealthClick: () -> Unit,
    showOnDuty: Boolean

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

        if (holidayToday) item {
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(holidayTitle ?: "Company Holiday", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(holidayMessage ?: "Regular attendance is not required today.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        // ====================================================
        // CURRENT EMPLOYEE STATUS
        // ====================================================

        item {

            EmployeeStatusCard(
                showReadiness = false,

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
                    onSystemHealthClick,
                showOnDuty = showOnDuty
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
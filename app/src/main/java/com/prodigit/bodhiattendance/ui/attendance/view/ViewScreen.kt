package com.prodigit.bodhiattendance.ui.attendance.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Sync

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// ATTENDANCE VIEW ITEM
// ============================================================

data class AttendanceViewItem(

    val date: String,

    val day: String,

    val checkIn: String,

    val checkOut: String,

    val workDuration: String,

    val status: String,

    val officeName: String,

    val totalBreak: String,

    val teaBreakStart: String = "--",

    val teaBreakEnd: String = "--",

    val lunchBreakStart: String = "--",

    val lunchBreakEnd: String = "--"
)


// ============================================================
// ATTENDANCE SCREEN
//
// BODY-ONLY SCREEN
//
// MainAppLayout owns:
//
// - AppBackground
// - Master Header
// - Scaffold
// - Bottom Navigation
//
// This file owns:
//
// - Attendance History
// - Attendance Details
// ============================================================

@Composable
fun ViewScreen(

    modifier: Modifier = Modifier

) {


    // ========================================================
    // SAMPLE DATA
    //
    // Temporary until CRM attendance APIs are connected.
    // ========================================================

    val attendanceList =
        remember {

            listOf(

                AttendanceViewItem(

                    date =
                        "06 Oct 2026",

                    day =
                        "Tuesday",

                    checkIn =
                        "09:12 AM",

                    checkOut =
                        "--",

                    workDuration =
                        "05h 48m",

                    status =
                        "Working",

                    officeName =
                        "Main Office",

                    totalBreak =
                        "46 min",

                    teaBreakStart =
                        "11:15 AM",

                    teaBreakEnd =
                        "11:30 AM",

                    lunchBreakStart =
                        "01:00 PM",

                    lunchBreakEnd =
                        "01:31 PM"
                ),


                AttendanceViewItem(

                    date =
                        "05 Oct 2026",

                    day =
                        "Monday",

                    checkIn =
                        "09:08 AM",

                    checkOut =
                        "06:02 PM",

                    workDuration =
                        "08h 12m",

                    status =
                        "Present",

                    officeName =
                        "Main Office",

                    totalBreak =
                        "42 min",

                    teaBreakStart =
                        "11:05 AM",

                    teaBreakEnd =
                        "11:17 AM",

                    lunchBreakStart =
                        "01:10 PM",

                    lunchBreakEnd =
                        "01:40 PM"
                ),


                AttendanceViewItem(

                    date =
                        "04 Oct 2026",

                    day =
                        "Sunday",

                    checkIn =
                        "--",

                    checkOut =
                        "--",

                    workDuration =
                        "--",

                    status =
                        "Holiday",

                    officeName =
                        "--",

                    totalBreak =
                        "--"
                ),


                AttendanceViewItem(

                    date =
                        "03 Oct 2026",

                    day =
                        "Saturday",

                    checkIn =
                        "09:02 AM",

                    checkOut =
                        "06:00 PM",

                    workDuration =
                        "08h 18m",

                    status =
                        "Present",

                    officeName =
                        "Main Office",

                    totalBreak =
                        "40 min",

                    teaBreakStart =
                        "11:10 AM",

                    teaBreakEnd =
                        "11:20 AM",

                    lunchBreakStart =
                        "01:05 PM",

                    lunchBreakEnd =
                        "01:35 PM"
                ),


                AttendanceViewItem(

                    date =
                        "02 Oct 2026",

                    day =
                        "Friday",

                    checkIn =
                        "--",

                    checkOut =
                        "--",

                    workDuration =
                        "--",

                    status =
                        "Holiday",

                    officeName =
                        "--",

                    totalBreak =
                        "--"
                )
            )
        }


    // ========================================================
    // SELECTED ATTENDANCE
    // ========================================================

    var selectedAttendance by
    remember {

        mutableStateOf<AttendanceViewItem?>(
            null
        )
    }


    BackHandler(enabled = selectedAttendance != null) {
        selectedAttendance = null
    }

    // ========================================================
    // HISTORY / DETAILS
    // ========================================================

    if (
        selectedAttendance ==
        null
    ) {

        AttendanceHistoryScreenContent(

            modifier =
                modifier,

            attendanceList =
                attendanceList,

            onAttendanceClick = { selectedAttendance = it },
            onDateClick = { date ->
                selectedAttendance = attendanceList.firstOrNull { it.date == date }
                    ?: AttendanceViewItem(
                        date = date,
                        day = SimpleDateFormat("EEEE", Locale.ENGLISH).format(
                            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).parse(date)!!
                        ),
                        checkIn = "--", checkOut = "--", workDuration = "--",
                        status = "No Record", officeName = "--", totalBreak = "--"
                    )
            }
        )

    } else {

        AttendanceDetailsScreenContent(

            modifier =
                modifier,

            attendance =
                selectedAttendance!!,

            onBackClick = {

                selectedAttendance =
                    null
            }
        )
    }
}


// ============================================================
// ATTENDANCE HISTORY
// ============================================================

@Composable
private fun AttendanceHistoryScreenContent(

    modifier: Modifier,

    attendanceList: List<AttendanceViewItem>,

    onAttendanceClick: (AttendanceViewItem) -> Unit,
    onDateClick: (String) -> Unit

) {


    var expandedDate by
    remember {

        mutableStateOf(
            attendanceList
                .firstOrNull()
                ?.date
        )
    }


    LazyColumn(

        modifier =
            modifier
                .fillMaxSize(),

        contentPadding =
            PaddingValues(

                start =
                    16.dp,

                end =
                    16.dp,

                top =
                    14.dp,

                bottom =
                    28.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )

    ) {


        // ====================================================
        // HEADER
        // ====================================================

        item {

            AttendanceHistoryHeader()
        }


        // ====================================================
        // EMPLOYEE + MONTH
        // ====================================================

        item {

            AttendanceFilterCard()
        }


        // ====================================================
        // WEEK STRIP
        // ====================================================

        item {

            AttendanceWeekStrip(attendanceList = attendanceList, onDateClick = onDateClick)
        }


        // ====================================================
        // MONTH SUMMARY
        // ====================================================

        item {

            MonthlySummarySection()
        }


        // ====================================================
        // SECTION TITLE
        // ====================================================

        item {

            Text(

                text =
                    "Attendance Records",

                color =
                    Color(
                        0xFF0C1745
                    ),

                fontSize =
                    17.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }


        // ====================================================
        // RECORDS
        // ====================================================

        items(
            attendanceList
        ) { attendance ->


            AttendanceDayCard(

                attendance =
                    attendance,

                expanded =
                    expandedDate ==
                            attendance.date,

                onExpandClick = {

                    expandedDate =

                        if (
                            expandedDate ==
                            attendance.date
                        ) {

                            null

                        } else {

                            attendance.date
                        }
                },

                onViewDetailsClick = {

                    onAttendanceClick(
                        attendance
                    )
                }
            )
        }


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


// ============================================================
// PAGE HEADER
// ============================================================

@Composable
private fun AttendanceHistoryHeader() {

    Column {

        Text(

            text =
                "Attendance History",

            color =
                Color(
                    0xFF0A1640
                ),

            fontSize =
                25.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(

            modifier =
                Modifier.height(
                    2.dp
                )
        )


        Text(

            text =
                "View your complete attendance records",

            color =
                Color(
                    0xFF66708F
                ),

            fontSize =
                14.sp
        )
    }
}


// ============================================================
// WEEK STRIP
// ============================================================

@Composable
private fun AttendanceWeekStrip(
    attendanceList: List<AttendanceViewItem>,
    onDateClick: (String) -> Unit
) {
    val parser = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.ENGLISH) }
    val reference = remember {
        Calendar.getInstance().apply {
            time = parser.parse(attendanceList.firstOrNull()?.date ?: "06 Oct 2026")
                ?: java.util.Date()
            add(Calendar.DAY_OF_MONTH, -4)
        }
    }
    var weekOffset by remember { mutableStateOf(0) }
    var chosenDate by remember { mutableStateOf(attendanceList.firstOrNull()?.date) }
    val days = (0..5).map { index ->
        (reference.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, weekOffset * 6 + index)
        }
    }
    AttendanceBaseCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            WeekNavigationButton(Icons.Outlined.KeyboardArrowLeft) { weekOffset-- }
            days.forEach { calendar ->
                val key = parser.format(calendar.time)
                val status = attendanceList.firstOrNull { it.date == key }?.status
                WeekDayItem(
                    modifier = Modifier.weight(1f),
                    day = dayFormat.format(calendar.time),
                    date = calendar.get(Calendar.DAY_OF_MONTH).toString(),
                    statusColor = when (status) {
                        "Present", "Working" -> Color(0xFF1AB455)
                        "Absent" -> Color(0xFFF03649)
                        else -> Color(0xFF98A1BB)
                    },
                    selected = chosenDate == key,
                    onClick = { chosenDate = key; onDateClick(key) }
                )
            }
            WeekNavigationButton(Icons.Outlined.KeyboardArrowRight) { weekOffset++ }
        }
    }
}

@Composable
private fun WeekNavigationButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(11.dp),
        color = Color(0xFFF2F7FF),
        border = BorderStroke(1.dp, Color(0xFFD9E7FC))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = "Change week", tint = Color(0xFF1267DF))
        }
    }
}

@Composable
private fun WeekDayItem(
    modifier: Modifier = Modifier,
    day: String,
    date: String,
    statusColor: Color,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) Color(0xFFEDF5FF) else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 1.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day, fontSize = 11.sp,
            color = if (selected) Color(0xFF1267DF) else Color(0xFF68718D))
        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(10.dp),
            color = if (selected) Color(0xFF116BF0) else Color.Transparent) {
            Text(date, modifier = Modifier.padding(horizontal = 3.dp, vertical = 5.dp),
                color = if (selected) Color.White else Color(0xFF131D44),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.size(7.dp).background(statusColor, CircleShape))
    }
}


// ============================================================
// MONTH SUMMARY
// ============================================================

@Composable
private fun MonthlySummarySection() {

    Column(

        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )

    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )

        ) {

            SummaryCard(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                title =
                    "Present",

                value =
                    "22",

                subtitle =
                    "Days this month",

                icon =
                    Icons.Outlined.CalendarMonth,

                accent =
                    Color(
                        0xFF12A84F
                    ),

                background =
                    Color(
                        0xFFF0FFF6
                    )
            )


            SummaryCard(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                title =
                    "Absent",

                value =
                    "1",

                subtitle =
                    "Day this month",

                icon =
                    Icons.Outlined.CalendarMonth,

                accent =
                    Color(
                        0xFFF03649
                    ),

                background =
                    Color(
                        0xFFFFF3F4
                    )
            )
        }


        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )

        ) {

            SummaryCard(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                title =
                    "Total Break",

                value =
                    "18h 25m",

                subtitle =
                    "This month",

                icon =
                    Icons.Outlined.Coffee,

                accent =
                    Color(
                        0xFF7138F1
                    ),

                background =
                    Color(
                        0xFFF8F3FF
                    )
            )


            SummaryCard(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                title =
                    "Work Hours",

                value =
                    "176h 30m",

                subtitle =
                    "This month",

                icon =
                    Icons.Outlined.AccessTime,

                accent =
                    Color(
                        0xFF156DE8
                    ),

                background =
                    Color(
                        0xFFF1F8FF
                    )
            )
        }
    }
}


// ============================================================
// SUMMARY CARD
// ============================================================

@Composable
private fun SummaryCard(

    modifier: Modifier,

    title: String,

    value: String,

    subtitle: String,

    icon: ImageVector,

    accent: Color,

    background: Color

) {

    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    background
            ),

        border =
            BorderStroke(

                width =
                    1.dp,

                color =
                    accent.copy(
                        alpha =
                            0.12f
                    )
            ),

        elevation =
            CardDefaults.cardElevation(

                defaultElevation =
                    1.dp
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    14.dp
                )

        ) {

            Surface(

                modifier =
                    Modifier.size(
                        35.dp
                    ),

                shape =
                    CircleShape,

                color =
                    accent.copy(
                        alpha =
                            0.12f
                    )

            ) {

                Box(

                    contentAlignment =
                        Alignment.Center

                ) {

                    Icon(

                        imageVector =
                            icon,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                19.dp
                            ),

                        tint =
                            accent
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Text(

                text =
                    title,

                color =
                    Color(
                        0xFF17214A
                    ),

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.SemiBold
            )


            Text(

                text =
                    value,

                color =
                    accent,

                fontSize =
                    21.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(

                text =
                    subtitle,

                color =
                    Color(
                        0xFF737B94
                    ),

                fontSize =
                    10.sp
            )
        }
    }
}


// ============================================================
// ATTENDANCE DAY CARD
// ============================================================

@Composable
private fun AttendanceDayCard(

    attendance: AttendanceViewItem,

    expanded: Boolean,

    onExpandClick: () -> Unit,

    onViewDetailsClick: () -> Unit

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    Color.White.copy(
                        alpha =
                            0.93f
                    )
            ),

        border =
            BorderStroke(

                width =
                    1.dp,

                color =
                    Color(
                        0xFFE0E9F8
                    )
            ),

        elevation =
            CardDefaults.cardElevation(

                defaultElevation =
                    2.dp
            )

    ) {

        Column {


            // =================================================
            // HEADER
            // =================================================

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onExpandClick()
                        }
                        .padding(
                            15.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                Column(

                    modifier =
                        Modifier.weight(
                            1f
                        )

                ) {

                    Text(

                        text =
                            "${attendance.day}, ${attendance.date}",

                        color =
                            Color(
                                0xFF0C1742
                            ),

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }


                StatusChip(

                    status =
                        attendance.status
                )


                Spacer(

                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )


                Text(

                    text =
                        if (
                            attendance.workDuration ==
                            "--"
                        ) {

                            "--"

                        } else {

                            attendance.workDuration
                        },

                    color =
                        Color(
                            0xFF4D5775
                        ),

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(

                    modifier =
                        Modifier.width(
                            3.dp
                        )
                )


                Icon(

                    imageVector =

                        if (
                            expanded
                        ) {

                            Icons.Outlined.KeyboardArrowUp

                        } else {

                            Icons.Outlined.KeyboardArrowDown
                        },

                    contentDescription =
                        null,

                    tint =
                        Color(
                            0xFF55617F
                        )
                )
            }


            // =================================================
            // EXPANDED TIMELINE
            // =================================================

            if (
                expanded
            ) {

                HorizontalDivider(

                    color =
                        Color(
                            0xFFE7EDF6
                        )
                )


                if (
                    attendance.status ==
                    "Holiday"
                ) {

                    Text(

                        text =
                            "No attendance events for this day.",

                        modifier =
                            Modifier.padding(
                                16.dp
                            ),

                        color =
                            Color(
                                0xFF69728C
                            ),

                        fontSize =
                            13.sp
                    )

                } else {

                    Column(

                        modifier =
                            Modifier.padding(

                                start =
                                    15.dp,

                                end =
                                    15.dp,

                                top =
                                    8.dp,

                                bottom =
                                    8.dp
                            )

                    ) {


                        AttendanceTimelineRow(

                            icon =
                                Icons.Outlined.Login,

                            iconColor =
                                Color(
                                    0xFF0AA94F
                                ),

                            time =
                                attendance.checkIn,

                            title =
                                "Check In",

                            subtitle =
                                "${attendance.officeName} (Auto)",

                            trailing =
                                "On Time"
                        )


                        if (
                            attendance.teaBreakStart !=
                            "--"
                        ) {

                            AttendanceTimelineRow(

                                icon =
                                    Icons.Outlined.Coffee,

                                iconColor =
                                    Color(
                                        0xFF1971E8
                                    ),

                                time =
                                    attendance.teaBreakStart,

                                title =
                                    "Tea Break Start",

                                subtitle =
                                    "Auto Detected",

                                trailing =
                                    "15 mins"
                            )
                        }


                        if (
                            attendance.teaBreakEnd !=
                            "--"
                        ) {

                            AttendanceTimelineRow(

                                icon =
                                    Icons.Outlined.Coffee,

                                iconColor =
                                    Color(
                                        0xFF1971E8
                                    ),

                                time =
                                    attendance.teaBreakEnd,

                                title =
                                    "Tea Break End",

                                subtitle =
                                    "Auto Detected",

                                trailing =
                                    "-"
                            )
                        }


                        if (
                            attendance.lunchBreakStart !=
                            "--"
                        ) {

                            AttendanceTimelineRow(

                                icon =
                                    Icons.Outlined.Restaurant,

                                iconColor =
                                    Color(
                                        0xFF7437ED
                                    ),

                                time =
                                    attendance.lunchBreakStart,

                                title =
                                    "Lunch Break Start",

                                subtitle =
                                    "Auto Detected",

                                trailing =
                                    "31 mins"
                            )
                        }


                        if (
                            attendance.lunchBreakEnd !=
                            "--"
                        ) {

                            AttendanceTimelineRow(

                                icon =
                                    Icons.Outlined.Restaurant,

                                iconColor =
                                    Color(
                                        0xFF7437ED
                                    ),

                                time =
                                    attendance.lunchBreakEnd,

                                title =
                                    "Lunch Break End",

                                subtitle =
                                    "Auto Detected",

                                trailing =
                                    "-"
                            )
                        }


                        AttendanceTimelineRow(

                            icon =
                                Icons.Outlined.Logout,

                            iconColor =
                                Color(
                                    0xFFF03A4E
                                ),

                            time =
                                attendance.checkOut,

                            title =
                                "Check Out",

                            subtitle =

                                if (
                                    attendance.checkOut ==
                                    "--"
                                ) {

                                    "Will be recorded automatically"

                                } else {

                                    "${attendance.officeName} (Auto)"
                                },

                            trailing =

                                if (
                                    attendance.checkOut ==
                                    "--"
                                ) {

                                    "Pending"

                                } else {

                                    "On Time"
                                }
                        )


                        Spacer(

                            modifier =
                                Modifier.height(
                                    6.dp
                                )
                        )


                        Surface(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onViewDetailsClick()
                                    },

                            shape =
                                RoundedCornerShape(
                                    13.dp
                                ),

                            color =
                                Color(
                                    0xFFF3F8FF
                                )

                        ) {

                            Row(

                                modifier =
                                    Modifier.padding(

                                        horizontal =
                                            13.dp,

                                        vertical =
                                            10.dp
                                    ),

                                verticalAlignment =
                                    Alignment.CenterVertically

                            ) {

                                Text(

                                    text =
                                        "View full attendance details",

                                    modifier =
                                        Modifier.weight(
                                            1f
                                        ),

                                    color =
                                        Color(
                                            0xFF1267DF
                                        ),

                                    fontSize =
                                        12.sp,

                                    fontWeight =
                                        FontWeight.SemiBold
                                )


                                Icon(

                                    imageVector =
                                        Icons.Outlined.ChevronRight,

                                    contentDescription =
                                        null,

                                    tint =
                                        Color(
                                            0xFF1267DF
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ============================================================
// TIMELINE ROW
// ============================================================

@Composable
private fun AttendanceTimelineRow(

    icon: ImageVector,

    iconColor: Color,

    time: String,

    title: String,

    subtitle: String,

    trailing: String

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(

                    vertical =
                        8.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically

    ) {


        Text(

            text =
                time,

            modifier =
                Modifier.width(
                    68.dp
                ),

            color =
                Color(
                    0xFF626C88
                ),

            fontSize =
                11.sp
        )


        Surface(

            modifier =
                Modifier.size(
                    35.dp
                ),

            shape =
                CircleShape,

            color =
                iconColor.copy(
                    alpha =
                        0.12f
                )

        ) {

            Box(

                contentAlignment =
                    Alignment.Center

            ) {

                Icon(

                    imageVector =
                        icon,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(
                            19.dp
                        ),

                    tint =
                        iconColor
                )
            }
        }


        Spacer(

            modifier =
                Modifier.width(
                    9.dp
                )
        )


        Column(

            modifier =
                Modifier.weight(
                    1f
                )

        ) {

            Text(

                text =
                    title,

                color =
                    Color(
                        0xFF121C45
                    ),

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.SemiBold
            )


            Text(

                text =
                    subtitle,

                color =
                    Color(
                        0xFF737C96
                    ),

                fontSize =
                    10.sp
            )
        }


        Surface(

            shape =
                RoundedCornerShape(
                    50
                ),

            color =

                if (
                    trailing ==
                    "On Time"
                ) {

                    Color(
                        0xFFE5F8EB
                    )

                } else {

                    Color(
                        0xFFF1F5FA
                    )
                }

        ) {

            Text(

                text =
                    trailing,

                modifier =
                    Modifier.padding(

                        horizontal =
                            9.dp,

                        vertical =
                            6.dp
                    ),

                color =

                    if (
                        trailing ==
                        "On Time"
                    ) {

                        Color(
                            0xFF088939
                        )

                    } else {

                        Color(
                            0xFF4F5874
                        )
                    },

                fontSize =
                    10.sp
            )
        }
    }
}


// ============================================================
// DETAILS SCREEN
// ============================================================

@Composable
private fun AttendanceDetailsScreenContent(

    modifier: Modifier,

    attendance: AttendanceViewItem,

    onBackClick: () -> Unit

) {

    if (attendance.status == "No Record") {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Attendance Details", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(attendance.date, fontSize = 16.sp)
            Text("No attendance record available for this date.", color = Color(0xFF68718C))
        }
        return
    }

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize(),

        contentPadding =
            PaddingValues(

                start =
                    16.dp,

                end =
                    16.dp,

                top =
                    12.dp,

                bottom =
                    28.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )

    ) {


        // ====================================================
        // LOCAL PAGE HEADER
        // ====================================================

        item {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                IconButton(

                    onClick =
                        onBackClick

                ) {

                    Icon(

                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,

                        contentDescription =
                            "Back",

                        tint =
                            Color(
                                0xFF0B1744
                            )
                    )
                }


                Column {

                    Text(

                        text =
                            "Attendance Details",

                        color =
                            Color(
                                0xFF0B1744
                            ),

                        fontSize =
                            22.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(

                        text =
                            "Complete daily attendance activity",

                        color =
                            Color(
                                0xFF68718C
                            ),

                        fontSize =
                            13.sp
                    )
                }
            }
        }


        // ====================================================
        // DAY SUMMARY
        // ====================================================

        item {

            AttendanceBaseCard {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Column(

                        modifier =
                            Modifier.weight(
                                1f
                            )

                    ) {

                        Text(

                            text =
                                attendance.date,

                            color =
                                Color(
                                    0xFF0B1744
                                ),

                            fontSize =
                                19.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(

                            text =
                                attendance.day,

                            color =
                                Color(
                                    0xFF68718C
                                ),

                            fontSize =
                                13.sp
                        )
                    }


                    StatusChip(

                        status =
                            attendance.status
                    )
                }
            }
        }


        // ====================================================
        // ATTENDANCE TIME
        // ====================================================

        item {

            DetailSectionCard(

                title =
                    "Attendance Time"

            ) {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )

                ) {

                    DetailMetricCard(

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        label =
                            "Check In",

                        value =
                            attendance.checkIn,

                        icon =
                            Icons.Outlined.Login,

                        accent =
                            Color(
                                0xFF0AAA4E
                            )
                    )


                    DetailMetricCard(

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        label =
                            "Check Out",

                        value =
                            attendance.checkOut,

                        icon =
                            Icons.Outlined.Logout,

                        accent =
                            Color(
                                0xFFF03B4E
                            )
                    )
                }
            }
        }


        // ====================================================
        // OFFICE
        // ====================================================

        item {

            DetailInfoCard(

                icon =
                    Icons.Outlined.Business,

                title =
                    "Office",

                value =
                    attendance.officeName,

                accent =
                    Color(
                        0xFF176DE5
                    )
            )
        }


        // ====================================================
        // BREAK TIMELINE
        // ====================================================

        item {

            DetailSectionCard(

                title =
                    "Break Timeline"

            ) {

                BreakTimelineItem(

                    icon =
                        Icons.Outlined.Coffee,

                    title =
                        "Tea Break",

                    start =
                        attendance.teaBreakStart,

                    end =
                        attendance.teaBreakEnd,

                    accent =
                        Color(
                            0xFF176DE5
                        )
                )


                HorizontalDivider(

                    modifier =
                        Modifier.padding(

                            vertical =
                                11.dp
                        ),

                    color =
                        Color(
                            0xFFE9EDF4
                        )
                )


                BreakTimelineItem(

                    icon =
                        Icons.Outlined.Restaurant,

                    title =
                        "Lunch Break",

                    start =
                        attendance.lunchBreakStart,

                    end =
                        attendance.lunchBreakEnd,

                    accent =
                        Color(
                            0xFF7438ED
                        )
                )
            }
        }


        // ====================================================
        // WORK SUMMARY
        // ====================================================

        item {

            DetailSectionCard(

                title =
                    "Work Summary"

            ) {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )

                ) {

                    DetailMetricCard(

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        label =
                            "Total Break",

                        value =
                            attendance.totalBreak,

                        icon =
                            Icons.Outlined.Coffee,

                        accent =
                            Color(
                                0xFF7040E8
                            )
                    )


                    DetailMetricCard(

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        label =
                            "Net Work",

                        value =
                            attendance.workDuration,

                        icon =
                            Icons.Outlined.AccessTime,

                        accent =
                            Color(
                                0xFF0F6DE8
                            )
                    )
                }
            }
        }


        // ====================================================
        // SYNC
        // ====================================================

        item {

            DetailInfoCard(

                icon =
                    Icons.Outlined.Sync,

                title =
                    "Sync Status",

                value =
                    "Synced",

                accent =
                    Color(
                        0xFF0AA650
                    )
            )
        }
    }
}


// ============================================================
// BREAK TIMELINE ITEM
// ============================================================

@Composable
private fun BreakTimelineItem(

    icon: ImageVector,

    title: String,

    start: String,

    end: String,

    accent: Color

) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Surface(

            modifier =
                Modifier.size(
                    40.dp
                ),

            shape =
                CircleShape,

            color =
                accent.copy(
                    alpha =
                        0.12f
                )

        ) {

            Box(

                contentAlignment =
                    Alignment.Center

            ) {

                Icon(

                    imageVector =
                        icon,

                    contentDescription =
                        null,

                    tint =
                        accent
                )
            }
        }


        Spacer(

            modifier =
                Modifier.width(
                    12.dp
                )
        )


        Column(

            modifier =
                Modifier.weight(
                    1f
                )

        ) {

            Text(

                text =
                    title,

                color =
                    Color(
                        0xFF121C43
                    ),

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.SemiBold
            )


            Text(

                text =
                    "$start  →  $end",

                color =
                    Color(
                        0xFF6E7690
                    ),

                fontSize =
                    12.sp
            )
        }
    }
}


// ============================================================
// DETAIL SECTION CARD
// ============================================================

@Composable
private fun DetailSectionCard(

    title: String,

    content: @Composable () -> Unit

) {

    AttendanceBaseCard {

        Text(

            text =
                title,

            color =
                Color(
                    0xFF0D1742
                ),

            fontSize =
                16.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(

            modifier =
                Modifier.height(
                    13.dp
                )
        )


        content()
    }
}


// ============================================================
// DETAIL METRIC CARD
// ============================================================

@Composable
private fun DetailMetricCard(

    modifier: Modifier,

    label: String,

    value: String,

    icon: ImageVector,

    accent: Color

) {

    Surface(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                16.dp
            ),

        color =
            accent.copy(
                alpha =
                    0.07f
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    13.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally

        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    accent
            )


            Spacer(

                modifier =
                    Modifier.height(
                        7.dp
                    )
            )


            Text(

                text =
                    label,

                color =
                    Color(
                        0xFF707892
                    ),

                fontSize =
                    11.sp
            )


            Text(

                text =
                    value,

                color =
                    Color(
                        0xFF111A40
                    ),

                fontSize =
                    15.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// DETAIL INFO CARD
// ============================================================

@Composable
private fun DetailInfoCard(

    icon: ImageVector,

    title: String,

    value: String,

    accent: Color

) {

    AttendanceBaseCard {

        Row(

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Surface(

                modifier =
                    Modifier.size(
                        42.dp
                    ),

                shape =
                    CircleShape,

                color =
                    accent.copy(
                        alpha =
                            0.12f
                    )

            ) {

                Box(

                    contentAlignment =
                        Alignment.Center

                ) {

                    Icon(

                        imageVector =
                            icon,

                        contentDescription =
                            null,

                        tint =
                            accent
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier.width(
                        12.dp
                    )
            )


            Column {

                Text(

                    text =
                        title,

                    color =
                        Color(
                            0xFF727A92
                        ),

                    fontSize =
                        11.sp
                )


                Text(

                    text =
                        value,

                    color =
                        Color(
                            0xFF111A40
                        ),

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}


// ============================================================
// STATUS CHIP
// ============================================================

@Composable
private fun StatusChip(

    status: String

) {


    val backgroundColor: Color

    val textColor: Color


    when (
        status.lowercase()
    ) {


        "present" -> {

            backgroundColor =
                Color(
                    0xFFE3F8EA
                )

            textColor =
                Color(
                    0xFF078637
                )
        }


        "working" -> {

            backgroundColor =
                Color(
                    0xFFE7F3FF
                )

            textColor =
                Color(
                    0xFF1267DF
                )
        }


        "absent" -> {

            backgroundColor =
                Color(
                    0xFFFFE8EB
                )

            textColor =
                Color(
                    0xFFE42338
                )
        }


        "holiday" -> {

            backgroundColor =
                Color(
                    0xFFF0F2F7
                )

            textColor =
                Color(
                    0xFF737C96
                )
        }


        else -> {

            backgroundColor =
                Color(
                    0xFFF0F2F7
                )

            textColor =
                Color(
                    0xFF5E667E
                )
        }
    }


    Surface(

        shape =
            RoundedCornerShape(
                50
            ),

        color =
            backgroundColor

    ) {

        Text(

            text =
                status,

            modifier =
                Modifier.padding(

                    horizontal =
                        10.dp,

                    vertical =
                        5.dp
                ),

            color =
                textColor,

            fontSize =
                10.sp,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}


// ============================================================
// BASE CARD
// ============================================================

@Composable
private fun AttendanceBaseCard(

    content: @Composable ColumnScope.() -> Unit

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    Color.White.copy(
                        alpha =
                            0.92f
                    )
            ),

        border =
            BorderStroke(

                width =
                    1.dp,

                color =
                    Color(
                        0xFFE0E9F8
                    )
            ),

        elevation =
            CardDefaults.cardElevation(

                defaultElevation =
                    2.dp
            )

    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),

            content =
                content
        )
    }
}

// ============================================================
// ATTENDANCE FILTER CARD
// ============================================================

@Composable
private fun AttendanceFilterCard() {

    AttendanceBaseCard {

        // =====================================================
        // MONTH
        // =====================================================

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Surface(

                modifier =
                    Modifier.size(
                        42.dp
                    ),

                shape =
                    RoundedCornerShape(
                        13.dp
                    ),

                color =
                    Color(
                        0xFFEAF3FF
                    )

            ) {

                Box(

                    contentAlignment =
                        Alignment.Center

                ) {

                    Icon(

                        imageVector =
                            Icons.Outlined.CalendarMonth,

                        contentDescription =
                            null,

                        tint =
                            Color(
                                0xFF1267DF
                            )
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier.width(
                        12.dp
                    )
            )


            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )

            ) {

                Text(

                    text =
                        "Attendance Period",

                    color =
                        Color(
                            0xFF727B95
                        ),

                    fontSize =
                        11.sp
                )


                Text(

                    text =
                        "October 2026",

                    color =
                        Color(
                            0xFF101B46
                        ),

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Surface(

                shape =
                    RoundedCornerShape(
                        12.dp
                    ),

                color =
                    Color(
                        0xFFF4F8FF
                    ),

                border =
                    BorderStroke(

                        width =
                            1.dp,

                        color =
                            Color(
                                0xFFD9E7FC
                            )
                    )

            ) {

                Icon(

                    imageVector =
                        Icons.Outlined.KeyboardArrowDown,

                    contentDescription =
                        "Change month",

                    modifier =
                        Modifier.padding(
                            8.dp
                        ),

                    tint =
                        Color(
                            0xFF57617E
                        )
                )
            }
        }


        Spacer(

            modifier =
                Modifier.height(
                    14.dp
                )
        )


        HorizontalDivider(

            color =
                Color(
                    0xFFE7EDF6
                )
        )


        Spacer(

            modifier =
                Modifier.height(
                    14.dp
                )
        )


        // =====================================================
        // OFFICE + SYNC
        // =====================================================

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically

        ) {


            // =================================================
            // OFFICE
            // =================================================

            Row(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Surface(

                    modifier =
                        Modifier.size(
                            34.dp
                        ),

                    shape =
                        CircleShape,

                    color =
                        Color(
                            0xFFF0F6FF
                        )

                ) {

                    Box(

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Icon(

                            imageVector =
                                Icons.Outlined.LocationOn,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(
                                    18.dp
                                ),

                            tint =
                                Color(
                                    0xFF1267DF
                                )
                        )
                    }
                }


                Spacer(

                    modifier =
                        Modifier.width(
                            9.dp
                        )
                )


                Column {

                    Text(

                        text =
                            "Office",

                        color =
                            Color(
                                0xFF7A829A
                            ),

                        fontSize =
                            10.sp
                    )


                    Text(

                        text =
                            "Main Office",

                        color =
                            Color(
                                0xFF17214A
                            ),

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            // =================================================
            // SYNC
            // =================================================

            Surface(

                shape =
                    RoundedCornerShape(
                        50
                    ),

                color =
                    Color(
                        0xFFE7F8EC
                    )

            ) {

                Row(

                    modifier =
                        Modifier.padding(

                            horizontal =
                                10.dp,

                            vertical =
                                7.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Icon(

                        imageVector =
                            Icons.Outlined.Sync,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                14.dp
                            ),

                        tint =
                            Color(
                                0xFF0A9A43
                            )
                    )


                    Spacer(

                        modifier =
                            Modifier.width(
                                5.dp
                            )
                    )


                    Text(

                        text =
                            "Synced",

                        color =
                            Color(
                                0xFF087F38
                            ),

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }


        Spacer(

            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Text(

            text =
                "Attendance is recorded automatically",

            color =
                Color(
                    0xFF7A829A
                ),

            fontSize =
                10.sp
        )
    }
}
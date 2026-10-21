package com.prodigit.bodhiattendance.ui.onduty

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import java.text.DateFormatSymbols
import java.util.Calendar


// ============================================================
// ON DUTY MODEL
// ============================================================

data class OnDutyAssignment(

    val day: Int,

    val month: Int, // Calendar.JANUARY = 0

    val year: Int,

    val dutyType: String,

    val startTime: String,

    val endTime: String,

    val location: String,

    val purpose: String,

    val assignedBy: String,

    val status: String,

    val remarks: String
)


// ============================================================
// ON DUTY SCREEN
//
// BODY ONLY
//
// CRM:
// Admin / Manager creates On Duty.
//
// ANDROID:
// Employee views only.
// ============================================================

@Composable
fun OnDutyScreen(

    modifier: Modifier = Modifier

) {


    val live = com.prodigit.bodhiattendance.ui.live.liveUiState()
    if (live.onDuty == null) {
        com.prodigit.bodhiattendance.ui.live.DataUnavailable("On Duty", live.errors["onDuty"] ?: live.errors["all"], live.loading)
        return
    }
    val allAssignments = live.onDuty.map { item ->
        val date = java.time.LocalDate.parse(item.date)
        OnDutyAssignment(date.dayOfMonth, date.monthValue - 1, date.year, item.dutyType,
            item.startTime, item.endTime, item.location, item.purpose, item.assignedBy, item.status, item.remarks)
    }

    // ========================================================
    // CURRENT DISPLAY MONTH
    // ========================================================

    var displayMonth by
    remember {

        mutableIntStateOf(
            Calendar.getInstance().get(Calendar.MONTH)
        )
    }


    var displayYear by
    remember {

        mutableIntStateOf(
            Calendar.getInstance().get(Calendar.YEAR)
        )
    }


    // ========================================================
    // SELECTED ASSIGNMENT
    // ========================================================

    var selectedAssignment by
    remember {

        mutableStateOf<OnDutyAssignment?>(
            allAssignments.firstOrNull {
                it.month == displayMonth && it.year == displayYear
            }
        )
    }


    LaunchedEffect(allAssignments, displayMonth, displayYear) {
        val old = selectedAssignment
        selectedAssignment = allAssignments.firstOrNull { row ->
            old != null && row.month == displayMonth && row.year == displayYear && row.day == old.day && row.month == old.month && row.year == old.year && row.startTime == old.startTime
        } ?: allAssignments.firstOrNull { it.month == displayMonth && it.year == displayYear }
    }

    // ========================================================
    // ASSIGNMENTS FOR CURRENT MONTH
    // ========================================================

    val monthAssignments =
        allAssignments.filter {

            it.month == displayMonth &&
                    it.year == displayYear
        }


    // ========================================================
    // PREVIOUS MONTH
    // ========================================================

    fun previousMonth() {

        if (
            displayMonth ==
            Calendar.JANUARY
        ) {

            displayMonth =
                Calendar.DECEMBER

            displayYear -=
                1

        } else {

            displayMonth -=
                1
        }


        selectedAssignment =
            null
    }


    // ========================================================
    // NEXT MONTH
    // ========================================================

    fun nextMonth() {

        if (
            displayMonth ==
            Calendar.DECEMBER
        ) {

            displayMonth =
                Calendar.JANUARY

            displayYear +=
                1

        } else {

            displayMonth +=
                1
        }


        selectedAssignment =
            null
    }


    // ========================================================
    // UI
    // ========================================================

    LazyColumn(

        modifier =
            modifier.fillMaxSize(),

        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 28.dp
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

            PageHeader()
        }


        // ====================================================
        // SUMMARY
        // ====================================================

        item {

            OnDutyHeroCard(

                totalAssignments =
                    monthAssignments.size
            )
        }


        // ====================================================
        // MONTH
        // ====================================================

        item {

            MonthControlCard(

                month =
                    displayMonth,

                year =
                    displayYear,

                onPreviousMonth = {

                    previousMonth()
                },

                onNextMonth = {

                    nextMonth()
                }
            )
        }


        // ====================================================
        // CALENDAR
        // ====================================================

        item {

            CalendarCard(

                month =
                    displayMonth,

                year =
                    displayYear,

                assignments =
                    monthAssignments,

                selectedDate =
                    selectedAssignment?.day,

                onDateSelected = { day ->

                    selectedAssignment =
                        monthAssignments
                            .firstOrNull {

                                it.day ==
                                        day
                            }
                }
            )
        }


        // ====================================================
        // NO ASSIGNMENTS
        // ====================================================

        if (
            monthAssignments.isEmpty()
        ) {

            item {

                EmptyOnDutyCard(
                    month = displayMonth,
                    year = displayYear
                )
            }

        } else {


            // =================================================
            // SELECTED ASSIGNMENT
            // =================================================

            if (
                selectedAssignment !=
                null
            ) {

                item {

                    SectionHeader(

                        title =
                            "Selected Assignment",

                        subtitle =
                            "On Duty details assigned from CRM"
                    )
                }


                item {

                    ModernOnDutyDetailsCard(

                        assignment =
                            selectedAssignment!!
                    )
                }

            } else {

                item {

                    SelectDateHintCard()
                }
            }
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
private fun PageHeader() {

    Column {

        Text(
            text = "On Duty",
            color = Color(0xFF0D1745),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )


        Text(
            text = "View duties assigned to you by Admin or Manager",
            color = Color(0xFF6D7692),
            fontSize = 14.sp
        )
    }
}


// ============================================================
// HERO CARD
// ============================================================

@Composable
private fun OnDutyHeroCard(

    totalAssignments: Int

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                24.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    Color(
                        0xFFF5F1FF
                    )
            ),

        border =
            BorderStroke(
                1.dp,
                Color(0xFFE6DEFF)
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    18.dp
                )

        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Surface(

                    modifier =
                        Modifier.size(
                            50.dp
                        ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    color =
                        Color(
                            0xFFE8E0FF
                        )

                ) {

                    Box(

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.BusinessCenter,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.size(
                                    26.dp
                                ),
                            tint =
                                Color(
                                    0xFF6541E8
                                )
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            13.dp
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
                            "Assigned On Duty",
                        color =
                            Color(
                                0xFF171F49
                            ),
                        fontSize =
                            15.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Text(
                        text =
                            "$totalAssignments days this month",
                        color =
                            Color(
                                0xFF6745E9
                            ),
                        fontSize =
                            22.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Surface(

                    shape =
                        RoundedCornerShape(
                            50
                        ),

                    color =
                        Color(
                            0xFFE5F8EB
                        )

                ) {

                    Row(

                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 7.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Verified,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.size(
                                    14.dp
                                ),
                            tint =
                                Color(
                                    0xFF0B9844
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
                                "CRM Assigned",
                            color =
                                Color(
                                    0xFF087E39
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
                        14.dp
                    )
            )


            Text(
                text =
                    "On Duty assignments are created in CRM and displayed here as read-only records.",
                color =
                    Color(
                        0xFF666F8A
                    ),
                fontSize =
                    12.sp
            )
        }
    }
}


// ============================================================
// MONTH CONTROL
// ============================================================

@Composable
private fun MonthControlCard(

    month: Int,

    year: Int,

    onPreviousMonth: () -> Unit,

    onNextMonth: () -> Unit

) {

    BaseCard {

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
                        "Viewing Month",
                    color =
                        Color(
                            0xFF747D98
                        ),
                    fontSize =
                        10.sp
                )


                Text(
                    text =
                        "${monthName(month)} $year",
                    color =
                        Color(
                            0xFF141E48
                        ),
                    fontSize =
                        17.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }


            SmallIconButton(

                icon =
                    Icons.Outlined.KeyboardArrowLeft,

                contentDescription =
                    "Previous month",

                onClick =
                    onPreviousMonth
            )


            Spacer(
                modifier =
                    Modifier.width(
                        6.dp
                    )
            )


            SmallIconButton(

                icon =
                    Icons.Outlined.KeyboardArrowRight,

                contentDescription =
                    "Next month",

                onClick =
                    onNextMonth
            )
        }
    }
}


// ============================================================
// SMALL BUTTON
// ============================================================

@Composable
private fun SmallIconButton(

    icon: ImageVector,

    contentDescription: String,

    onClick: () -> Unit

) {

    Surface(

        modifier =
            Modifier
                .size(
                    36.dp
                )
                .clickable {

                    onClick()
                },

        shape =
            RoundedCornerShape(
                11.dp
            ),

        color =
            Color(
                0xFFF4F8FF
            ),

        border =
            BorderStroke(
                1.dp,
                Color(0xFFDCE8FA)
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
                    contentDescription,
                tint =
                    Color(
                        0xFF57617F
                    )
            )
        }
    }
}


// ============================================================
// CALENDAR
// ============================================================

@Composable
private fun CalendarCard(

    month: Int,

    year: Int,

    assignments: List<OnDutyAssignment>,

    selectedDate: Int?,

    onDateSelected: (Int) -> Unit

) {

    val assignedDates =
        assignments
            .map {
                it.day
            }
            .toSet()


    val daysInMonth =
        getDaysInMonth(
            month = month,
            year = year
        )


    val firstDayOffset =
        getFirstDayOffset(
            month = month,
            year = year
        )


    val cells =
        buildList<Int?> {

            repeat(
                firstDayOffset
            ) {

                add(
                    null
                )
            }


            for (
            day in 1..daysInMonth
            ) {

                add(
                    day
                )
            }
        }


    BaseCard {

        Row(

            modifier =
                Modifier.fillMaxWidth()

        ) {

            listOf(
                "SUN",
                "MON",
                "TUE",
                "WED",
                "THU",
                "FRI",
                "SAT"
            ).forEach { day ->

                Box(

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    contentAlignment =
                        Alignment.Center

                ) {

                    Text(
                        text =
                            day,
                        color =
                            Color(
                                0xFF7B849C
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
                    12.dp
                )
        )


        cells
            .chunked(
                7
            )
            .forEach { week ->

                Row(

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    repeat(
                        7
                    ) { index ->

                        val day =
                            week.getOrNull(
                                index
                            )


                        Box(

                            modifier =
                                Modifier.weight(
                                    1f
                                ),

                            contentAlignment =
                                Alignment.Center

                        ) {

                            if (
                                day != null
                            ) {

                                CalendarDayCell(

                                    day =
                                        day,

                                    isOnDuty =
                                        assignedDates.contains(
                                            day
                                        ),

                                    selected =
                                        selectedDate ==
                                                day,

                                    onClick = {

                                        onDateSelected(
                                            day
                                        )
                                    }
                                )
                            }
                        }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )
            }


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        HorizontalDivider(
            color =
                Color(
                    0xFFE8EDF5
                )
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        Row(

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            LegendItem(
                color =
                    Color(
                        0xFF6541E8
                    ),
                label =
                    "On Duty assigned"
            )


            Spacer(
                modifier =
                    Modifier.width(
                        18.dp
                    )
            )


            LegendItem(
                color =
                    Color(
                        0xFFCED4E2
                    ),
                label =
                    "Normal day"
            )
        }
    }
}


// ============================================================
// CALENDAR CELL
// ============================================================

@Composable
private fun CalendarDayCell(

    day: Int,

    isOnDuty: Boolean,

    selected: Boolean,

    onClick: () -> Unit

) {

    Column(

        modifier =
            Modifier
                .size(
                    44.dp
                )
                .clickable(
                    enabled =
                        isOnDuty
                ) {

                    onClick()
                },

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center

    ) {

        Surface(

            modifier =
                Modifier.size(
                    32.dp
                ),

            shape =
                RoundedCornerShape(
                    11.dp
                ),

            color =

                when {

                    selected ->

                        Color(
                            0xFF6541E8
                        )


                    isOnDuty ->

                        Color(
                            0xFFECE7FF
                        )


                    else ->

                        Color.Transparent
                },

            border =

                if (
                    isOnDuty &&
                    !selected
                ) {

                    BorderStroke(
                        1.dp,
                        Color(
                            0xFFD9CFFF
                        )
                    )

                } else {

                    null
                }

        ) {

            Box(

                contentAlignment =
                    Alignment.Center

            ) {

                Text(
                    text =
                        day.toString(),
                    color =

                        when {

                            selected ->
                                Color.White

                            isOnDuty ->
                                Color(
                                    0xFF5937D8
                                )

                            else ->
                                Color(
                                    0xFF232C4F
                                )
                        },
                    fontSize =
                        12.sp,
                    fontWeight =

                        if (
                            isOnDuty
                        ) {

                            FontWeight.Bold

                        } else {

                            FontWeight.Medium
                        }
                )
            }
        }


        if (
            isOnDuty
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )


            Box(

                modifier =
                    Modifier
                        .size(
                            5.dp
                        )
                        .background(
                            color =
                                Color(
                                    0xFF6541E8
                                ),
                            shape =
                                CircleShape
                        )
            )
        }
    }
}


// ============================================================
// EMPTY STATE
// ============================================================

@Composable
private fun EmptyOnDutyCard(

    month: Int,

    year: Int

) {

    BaseCard {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 14.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally

        ) {

            Surface(

                modifier =
                    Modifier.size(
                        54.dp
                    ),

                shape =
                    CircleShape,

                color =
                    Color(
                        0xFFF1F4FA
                    )

            ) {

                Box(

                    contentAlignment =
                        Alignment.Center

                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.BusinessCenter,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                27.dp
                            ),
                        tint =
                            Color(
                                0xFF8A93AA
                            )
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Text(
                text =
                    "No On Duty assignments",
                color =
                    Color(
                        0xFF172149
                    ),
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    "No assignments were created for ${monthName(month)} $year.",
                color =
                    Color(
                        0xFF737C95
                    ),
                fontSize =
                    12.sp
            )
        }
    }
}


// ============================================================
// SELECT DATE MESSAGE
// ============================================================

@Composable
private fun SelectDateHintCard() {

    Surface(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        color =
            Color(
                0xFFF3F6FC
            ),

        border =
            BorderStroke(
                1.dp,
                Color(
                    0xFFE3E9F3
                )
            )

    ) {

        Row(

            modifier =
                Modifier.padding(
                    15.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Info,
                contentDescription =
                    null,
                tint =
                    Color(
                        0xFF1267DF
                    )
            )


            Spacer(
                modifier =
                    Modifier.width(
                        10.dp
                    )
            )


            Text(
                text =
                    "Tap a highlighted On Duty date to view its assignment.",
                color =
                    Color(
                        0xFF58627F
                    ),
                fontSize =
                    12.sp
            )
        }
    }
}


// ============================================================
// SECTION HEADER
// ============================================================

@Composable
private fun SectionHeader(

    title: String,

    subtitle: String

) {

    Column {

        Text(
            text =
                title,
            color =
                Color(
                    0xFF111B46
                ),
            fontSize =
                17.sp,
            fontWeight =
                FontWeight.Bold
        )


        Text(
            text =
                subtitle,
            color =
                Color(
                    0xFF747D96
                ),
            fontSize =
                11.sp
        )
    }
}


// ============================================================
// ASSIGNMENT DETAILS
// ============================================================

@Composable
private fun ModernOnDutyDetailsCard(

    assignment: OnDutyAssignment

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            BorderStroke(
                1.dp,
                Color(
                    0xFFE1E8F5
                )
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    17.dp
                )

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
                            48.dp
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),

                    color =
                        Color(
                            0xFFEDE8FF
                        )

                ) {

                    Box(

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Text(
                            text =
                                assignment.day.toString(),
                            color =
                                Color(
                                    0xFF5D3DDB
                                ),
                            fontSize =
                                18.sp,
                            fontWeight =
                                FontWeight.Bold
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
                            assignment.dutyType,
                        color =
                            Color(
                                0xFF111B45
                            ),
                        fontSize =
                            17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(
                        text =
                            "${assignment.day} ${monthName(assignment.month)} ${assignment.year}",
                        color =
                            Color(
                                0xFF717A93
                            ),
                        fontSize =
                            12.sp
                    )
                }


                Surface(

                    shape =
                        RoundedCornerShape(
                            50
                        ),

                    color =
                        Color(
                            0xFFE5F8EB
                        )

                ) {

                    Text(
                        text =
                            assignment.status,
                        modifier =
                            Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 6.dp
                            ),
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


            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
                    )
            )


            HorizontalDivider(
                color =
                    Color(
                        0xFFE8EDF5
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            DetailRow(
                icon =
                    Icons.Outlined.Schedule,
                title =
                    "Duty Time",
                value =
                    "${assignment.startTime} - ${assignment.endTime}",
                accent =
                    Color(
                        0xFF176DE5
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            DetailRow(
                icon =
                    Icons.Outlined.LocationOn,
                title =
                    "Location",
                value =
                    assignment.location,
                accent =
                    Color(
                        0xFFEE8A19
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            DetailRow(
                icon =
                    Icons.Outlined.BusinessCenter,
                title =
                    "Purpose",
                value =
                    assignment.purpose,
                accent =
                    Color(
                        0xFF6541E8
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            DetailRow(
                icon =
                    Icons.Outlined.Person,
                title =
                    "Assigned By",
                value =
                    assignment.assignedBy,
                accent =
                    Color(
                        0xFF0CA05A
                    )
            )


            if (
                assignment.remarks.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            15.dp
                        )
                )


                Surface(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    color =
                        Color(
                            0xFFF6F8FC
                        ),

                    border =
                        BorderStroke(
                            1.dp,
                            Color(
                                0xFFE7ECF4
                            )
                        )

                ) {

                    Column(

                        modifier =
                            Modifier.padding(
                                13.dp
                            )

                    ) {

                        Text(
                            text =
                                "Manager Remarks",
                            color =
                                Color(
                                    0xFF717A94
                                ),
                            fontSize =
                                10.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    4.dp
                                )
                        )


                        Text(
                            text =
                                assignment.remarks,
                            color =
                                Color(
                                    0xFF20294D
                                ),
                            fontSize =
                                12.sp,
                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            // =================================================
            // READ ONLY INFORMATION
            //
            // NOT CLICKABLE
            // =================================================

            Surface(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                color =
                    Color(
                        0xFFF1F5FC
                    )

            ) {

                Row(

                    modifier =
                        Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 11.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Verified,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                17.dp
                            ),
                        tint =
                            Color(
                                0xFF1267DF
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )


                    Column {

                        Text(
                            text =
                                "CRM controlled assignment",
                            color =
                                Color(
                                    0xFF3C4766
                                ),
                            fontSize =
                                11.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )


                        Text(
                            text =
                                "This assignment can only be modified by Admin or Manager.",
                            color =
                                Color(
                                    0xFF737C94
                                ),
                            fontSize =
                                10.sp
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// DETAIL ROW
// ============================================================

@Composable
private fun DetailRow(

    icon: ImageVector,

    title: String,

    value: String,

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
                RoundedCornerShape(
                    13.dp
                ),

            color =
                accent.copy(
                    alpha =
                        0.10f
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
                            20.dp
                        ),
                    tint =
                        accent
                )
            }
        }


        Spacer(
            modifier =
                Modifier.width(
                    11.dp
                )
        )


        Column {

            Text(
                text =
                    title,
                color =
                    Color(
                        0xFF7A8299
                    ),
                fontSize =
                    10.sp
            )


            Text(
                text =
                    value,
                color =
                    Color(
                        0xFF172149
                    ),
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}


// ============================================================
// LEGEND
// ============================================================

@Composable
private fun LegendItem(

    color: Color,

    label: String

) {

    Row(

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Box(

            modifier =
                Modifier
                    .size(
                        8.dp
                    )
                    .background(
                        color =
                            color,
                        shape =
                            CircleShape
                    )
        )


        Spacer(
            modifier =
                Modifier.width(
                    6.dp
                )
        )


        Text(
            text =
                label,
            color =
                Color(
                    0xFF6F7891
                ),
            fontSize =
                11.sp
        )
    }
}


// ============================================================
// BASE CARD
// ============================================================

@Composable
private fun BaseCard(

    content:
    @Composable
    ColumnScope.() -> Unit

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            BorderStroke(
                1.dp,
                Color(
                    0xFFE0E8F5
                )
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
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
// CALENDAR HELPERS
// ============================================================

private fun monthName(

    month: Int

): String {

    return DateFormatSymbols()
        .months[
        month
    ]
}


// ============================================================
// DAYS IN MONTH
// ============================================================

private fun getDaysInMonth(

    month: Int,

    year: Int

): Int {

    val calendar =
        Calendar.getInstance()


    calendar.set(
        Calendar.YEAR,
        year
    )


    calendar.set(
        Calendar.MONTH,
        month
    )


    return calendar.getActualMaximum(
        Calendar.DAY_OF_MONTH
    )
}


// ============================================================
// MONTH FIRST-DAY OFFSET
//
// Calendar.SUNDAY = 1
// We need:
// Sunday = 0
// Monday = 1
// ...
// Saturday = 6
// ============================================================

private fun getFirstDayOffset(

    month: Int,

    year: Int

): Int {

    val calendar =
        Calendar.getInstance()


    calendar.set(
        Calendar.YEAR,
        year
    )


    calendar.set(
        Calendar.MONTH,
        month
    )


    calendar.set(
        Calendar.DAY_OF_MONTH,
        1
    )


    return calendar.get(
        Calendar.DAY_OF_WEEK
    ) - 1
}
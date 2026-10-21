package com.prodigit.bodhiattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// TIMELINE EVENT TYPE
// ============================================================

enum class AttendanceTimelineType {

    CHECK_IN,

    BREAK_START,

    LUNCH_BREAK,

    BACK_TO_WORK,

    CHECK_OUT
}


// ============================================================
// TIMELINE MODEL
//
// UI model only.
// Later this can be mapped from backend / Room attendance events.
// ============================================================

data class AttendanceTimelineItem(

    val type: AttendanceTimelineType,

    val time: String,

    val title: String,

    val description: String? = null
)


// ============================================================
// ATTENDANCE TIMELINE CARD
// ============================================================

@Composable
fun AttendanceTimelineCard(

    events: List<AttendanceTimelineItem>,

    onViewAllClick: () -> Unit = {}

) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(24.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
        ) {

            // ====================================================
            // HEADER
            // ====================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text =
                            "Today's Activity",

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )


                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )


                    Text(
                        text =
                            "Automatic attendance timeline",

                        fontSize =
                            10.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                androidx.compose.material3.Surface(
                    onClick =
                        onViewAllClick,

                    shape =
                        RoundedCornerShape(50.dp),

                    color =
                        Color(0xFF1769E0)
                            .copy(alpha = 0.08f)
                ) {

                    Text(
                        text =
                            "View all",

                        modifier =
                            Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 7.dp
                            ),

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            Color(0xFF1769E0)
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // ====================================================
            // EMPTY STATE
            // ====================================================

            if (events.isEmpty()) {

                EmptyTimeline()

            } else {

                // A delayed final-departure classification may persist BREAK_START and
                // CHECK_OUT at the same original departure time. Display a single
                // checkout row while retaining both immutable audit events in Room/CRM.
                val displayedEvents = events.filterIndexed { index, item ->
                    !(item.type == AttendanceTimelineType.BREAK_START &&
                        events.getOrNull(index + 1)?.let { following ->
                            following.type == AttendanceTimelineType.CHECK_OUT &&
                                following.time == item.time
                        } == true)
                }
                displayedEvents.forEachIndexed {
                        index,
                        event ->

                    TimelineEventRow(

                        event =
                            event,

                        showLine =
                            index !=
                                    displayedEvents.lastIndex
                    )
                }
            }
        }
    }
}


// ============================================================
// TIMELINE EVENT
// ============================================================

@Composable
private fun TimelineEventRow(

    event: AttendanceTimelineItem,

    showLine: Boolean

) {

    val visual =
        getTimelineVisual(
            event.type
        )


    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.Top
    ) {

        // ========================================================
        // TIME
        // ========================================================

        Text(
            text =
                event.time,

            modifier =
                Modifier.padding(
                    top = 5.dp
                ),

            fontSize =
                11.sp,

            fontWeight =
                FontWeight.SemiBold,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.size(12.dp)
        )


        // ========================================================
        // TIMELINE INDICATOR
        // ========================================================

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            visual.color.copy(
                                alpha = 0.12f
                            )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        visual.icon,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(17.dp),

                    tint =
                        visual.color
                )
            }


            if (showLine) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                width = 2.dp,
                                height = 34.dp
                            )
                            .background(
                                visual.color.copy(
                                    alpha = 0.20f
                                )
                            )
                )
            }
        }


        Spacer(
            modifier =
                Modifier.size(11.dp)
        )


        // ========================================================
        // EVENT INFORMATION
        // ========================================================

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        top = 4.dp,
                        bottom =
                            if (showLine) {
                                14.dp
                            } else {
                                2.dp
                            }
                    )
        ) {

            Text(
                text =
                    event.title,

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )


            if (
                !event.description
                    .isNullOrBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )


                Text(
                    text =
                        event.description,

                    fontSize =
                        10.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}


// ============================================================
// EMPTY TIMELINE
// ============================================================

@Composable
private fun EmptyTimeline() {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 20.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier =
                Modifier
                    .size(46.dp)
                    .background(
                        color =
                            Color(0xFF1769E0)
                                .copy(
                                    alpha = 0.08f
                                ),

                        shape =
                            CircleShape
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Work,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(22.dp),

                tint =
                    Color(0xFF1769E0)
            )
        }


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Text(
            text =
                "No activity yet",

            fontSize =
                13.sp,

            fontWeight =
                FontWeight.SemiBold
        )


        Spacer(
            modifier =
                Modifier.height(3.dp)
        )


        Text(
            text =
                "Attendance events will appear automatically",

            fontSize =
                10.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}


// ============================================================
// TIMELINE VISUAL MODEL
// ============================================================

private data class TimelineVisual(

    val icon: ImageVector,

    val color: Color
)


// ============================================================
// EVENT VISUAL MAPPING
// ============================================================

private fun getTimelineVisual(
    type: AttendanceTimelineType
): TimelineVisual {

    return when (type) {

        AttendanceTimelineType.CHECK_IN ->

            TimelineVisual(
                icon =
                    Icons.Outlined.Login,

                color =
                    Color(0xFF159957)
            )


        AttendanceTimelineType.BREAK_START ->

            TimelineVisual(
                icon =
                    Icons.Outlined.Coffee,

                color =
                    Color(0xFF2589E8)
            )


        AttendanceTimelineType.LUNCH_BREAK ->

            TimelineVisual(
                icon =
                    Icons.Outlined.Restaurant,

                color =
                    Color(0xFF8B5CF6)
            )


        AttendanceTimelineType.BACK_TO_WORK ->

            TimelineVisual(
                icon =
                    Icons.Outlined.Work,

                color =
                    Color(0xFF1769E0)
            )


        AttendanceTimelineType.CHECK_OUT ->

            TimelineVisual(
                icon =
                    Icons.Outlined.Logout,

                color =
                    Color(0xFFF59E0B)
            )
    }
}
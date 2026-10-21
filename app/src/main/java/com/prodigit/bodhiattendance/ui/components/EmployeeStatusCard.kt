package com.prodigit.bodhiattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prodigit.bodhiattendance.ui.dashboard.AttendanceWorkStatus


// ============================================================
// COLORS
// ============================================================

private val StatusGreen =
    Color(0xFF16A05D)

private val StatusGreenDark =
    Color(0xFF08783F)

private val StatusBlue =
    Color(0xFF1769E0)

private val StatusOrange =
    Color(0xFFF59E0B)

private val StatusGray =
    Color(0xFF64748B)

private val CardBorder =
    Color(0xFFDCE7F5)


// ============================================================
// EMPLOYEE STATUS CARD
//
// Main live attendance status card.
//
// IMPORTANT:
// This component only DISPLAYS attendance/device state.
// It does not perform GPS, BLE or attendance processing.
// ============================================================

@Composable
fun EmployeeStatusCard(

    status: AttendanceWorkStatus,

    officeName: String,

    statusSince: String?,

    gpsReady: Boolean = false,

    bleReady: Boolean = false,

    syncReady: Boolean = false,

    showReadiness: Boolean = true

) {

    val statusInfo =
        getStatusInfo(status)


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = CardBorder,
                    shape =
                        RoundedCornerShape(24.dp)
                ),

        shape =
            RoundedCornerShape(24.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        statusInfo.backgroundColor,
                                        MaterialTheme.colorScheme.surface
                                    )
                            )
                    )
                    .padding(20.dp)
        ) {

            // ====================================================
            // LIVE STATUS HEADER
            // ====================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    // Pulsing animation can be added later.
                    Box(
                        modifier =
                            Modifier
                                .size(13.dp)
                                .background(
                                    color =
                                        statusInfo.color,
                                    shape =
                                        CircleShape
                                )
                    )


                    Spacer(
                        modifier =
                            Modifier.size(9.dp)
                    )


                    Column {

                        Text(
                            text =
                                statusInfo.title,

                            fontSize = 18.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                statusInfo.color
                        )


                        Text(
                            text =
                                statusInfo.subtitle,

                            fontSize = 11.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }


                if (
                    status ==
                    AttendanceWorkStatus.IN_OFFICE
                ) {

                    Surface(
                        shape =
                            RoundedCornerShape(
                                50.dp
                            ),

                        color =
                            StatusGreen.copy(
                                alpha = 0.13f
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier.padding(
                                    horizontal = 11.dp,
                                    vertical = 6.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(6.dp)
                                        .background(
                                            color =
                                                StatusGreen,
                                            shape =
                                                CircleShape
                                        )
                            )


                            Spacer(
                                modifier =
                                    Modifier.size(5.dp)
                            )


                            Text(
                                text = "LIVE",

                                fontSize = 10.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    StatusGreenDark
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            // ====================================================
            // OFFICE
            // ====================================================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier =
                        Modifier.size(38.dp),

                    shape =
                        RoundedCornerShape(12.dp),

                    color =
                        statusInfo.color.copy(
                            alpha = 0.10f
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
                                Modifier.size(20.dp),

                            tint =
                                statusInfo.color
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.size(11.dp)
                )


                Column {

                    Text(
                        text =
                            officeName.ifBlank {
                                "Office"
                            },

                        fontSize = 15.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )


                    if (
                        !statusSince.isNullOrBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(2.dp)
                        )


                        Text(
                            text =
                                getSinceText(
                                    status,
                                    statusSince
                                ),

                            fontSize = 12.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            // ====================================================
            // DEVICE / ATTENDANCE HEALTH
            // ====================================================

            if (showReadiness) Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                color =
                    MaterialTheme.colorScheme.surface
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 11.dp
                            ),

                    horizontalArrangement =
                        Arrangement.SpaceEvenly,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    StatusIndicator(
                        icon =
                            Icons.Outlined.LocationOn,

                        label =
                            "GPS",

                        ready =
                            gpsReady
                    )


                    StatusIndicator(
                        icon =
                            Icons.Outlined.Bluetooth,

                        label =
                            "BLE",

                        ready =
                            bleReady
                    )


                    StatusIndicator(
                        icon =
                            Icons.Outlined.CloudDone,

                        label =
                            "SYNC",

                        ready =
                            syncReady
                    )
                }
            }
        }
    }
}


// ============================================================
// STATUS INDICATOR
// ============================================================

@Composable
private fun StatusIndicator(

    icon: ImageVector,

    label: String,

    ready: Boolean

) {

    val indicatorColor =
        if (ready) {
            StatusGreen
        } else {
            StatusOrange
        }


    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(27.dp)
                    .background(
                        color =
                            indicatorColor.copy(
                                alpha = 0.12f
                            ),
                        shape =
                            CircleShape
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = icon,

                contentDescription = null,

                modifier =
                    Modifier.size(15.dp),

                tint =
                    indicatorColor
            )
        }


        Spacer(
            modifier =
                Modifier.size(6.dp)
        )


        Column {

            Text(
                text = label,

                fontSize = 9.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )


            Text(
                text =
                    if (ready) {
                        "Ready"
                    } else {
                        "Check"
                    },

                fontSize = 8.sp,

                color =
                    indicatorColor
            )
        }
    }
}


// ============================================================
// STATUS INFORMATION
// ============================================================

private data class StatusInfo(

    val title: String,

    val subtitle: String,

    val color: Color,

    val backgroundColor: Color
)


// ============================================================
// STATUS MAPPING
// ============================================================

private fun getStatusInfo(
    status: AttendanceWorkStatus
): StatusInfo {

    return when (status) {

        AttendanceWorkStatus.IN_OFFICE ->

            StatusInfo(
                title =
                    "You're In Office",

                subtitle =
                    "Automatic attendance active",

                color =
                    StatusGreenDark,

                backgroundColor =
                    Color(0xFFE8F8EF)
            )


        AttendanceWorkStatus.ON_BREAK ->

            StatusInfo(
                title =
                    "On Break",

                subtitle =
                    "Break detected automatically",

                color =
                    StatusOrange,

                backgroundColor =
                    Color(0xFFFFF6E5)
            )


        AttendanceWorkStatus.ON_DUTY ->

            StatusInfo(
                title =
                    "On Duty",

                subtitle =
                    "Outside duty active",

                color =
                    StatusBlue,

                backgroundColor =
                    Color(0xFFEAF3FF)
            )


        AttendanceWorkStatus.NOT_CHECKED_IN ->

            StatusInfo(
                title =
                    "Not Checked In",

                subtitle =
                    "No accepted check-in today",

                color =
                    StatusGray,

                backgroundColor =
                    Color(0xFFF1F5F9)
            )


        AttendanceWorkStatus.CHECKED_OUT ->

            StatusInfo(
                title =
                    "Checked Out",

                subtitle =
                    "Today's attendance completed",

                color =
                    StatusGray,

                backgroundColor =
                    Color(0xFFF1F5F9)
            )


        AttendanceWorkStatus.UNKNOWN ->

            StatusInfo(
                title =
                    "Status Unavailable",

                subtitle =
                    "CRM attendance data has not loaded",

                color =
                    StatusBlue,

                backgroundColor =
                    Color(0xFFEDF5FF)
            )
    }
}


// ============================================================
// STATUS TIME TEXT
// ============================================================

private fun getSinceText(

    status: AttendanceWorkStatus,

    time: String

): String {

    return when (status) {

        AttendanceWorkStatus.IN_OFFICE ->
            "Working since $time"

        AttendanceWorkStatus.ON_BREAK ->
            "Break started at $time"

        AttendanceWorkStatus.ON_DUTY ->
            "On duty since $time"

        AttendanceWorkStatus.CHECKED_OUT ->
            "Checked out at $time"

        AttendanceWorkStatus.NOT_CHECKED_IN ->
            "Last attendance update $time"

        AttendanceWorkStatus.UNKNOWN ->
            "Last update $time"
    }
}
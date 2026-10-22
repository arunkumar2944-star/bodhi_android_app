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
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// COLORS
// ============================================================

private val AttendanceBlue =
    Color(0xFF1769E0)

private val AttendanceGreen =
    Color(0xFF159957)

private val AttendanceOrange =
    Color(0xFFF59E0B)

private val AttendanceGray =
    Color(0xFF64748B)

private val AttendanceBorder =
    MaterialTheme.colorScheme.outlineVariant


// ============================================================
// TODAY ATTENDANCE CARD
// ============================================================

@Composable
fun TodayAttendanceCard(

    checkInTime: String?,

    checkInStatus: String?,

    checkOutTime: String?,

    hasCheckedOut: Boolean,

    selectedDate: String = "Today",

    onViewDetailsClick: () -> Unit = {}

) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(24.dp)
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
                defaultElevation = 5.dp
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

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(38.dp)
                                .background(
                                    color =
                                        AttendanceBlue.copy(
                                            alpha = 0.10f
                                        ),
                                    shape =
                                        RoundedCornerShape(12.dp)
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Today,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(20.dp),

                            tint =
                                AttendanceBlue
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.size(10.dp)
                    )


                    Column {

                        Text(
                            text =
                                "Today's Attendance",

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )


                        Text(
                            text =
                                "Automatic attendance",

                            fontSize =
                                10.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }


                Surface(
                    shape =
                        RoundedCornerShape(50.dp),

                    color =
                        AttendanceBlue.copy(
                            alpha = 0.08f
                        )
                ) {

                    Text(
                        text =
                            selectedDate,

                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            AttendanceBlue
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            // ====================================================
            // CHECK-IN + CHECK-OUT
            // ====================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                AttendanceEventBox(
                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Outlined.Login,

                    title =
                        "CHECK IN",

                    time =
                        checkInTime
                            ?: "--:--",

                    status =
                        checkInStatus
                            ?: "Waiting",

                    eventColor =
                        AttendanceGreen,

                    completed =
                        !checkInTime.isNullOrBlank()
                )


                AttendanceEventBox(
                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Outlined.Logout,

                    title =
                        "CHECK OUT",

                    time =
                        checkOutTime
                            ?: "--:--",

                    status =
                        if (hasCheckedOut) {
                            "Completed"
                        } else {
                            "Pending"
                        },

                    eventColor =
                        if (hasCheckedOut) {
                            AttendanceGreen
                        } else {
                            AttendanceOrange
                        },

                    completed =
                        hasCheckedOut
                )
            }


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // AUTOMATIC ATTENDANCE INFO
            // ====================================================

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(14.dp),

                color =
                    AttendanceBlue.copy(
                        alpha = 0.055f
                    ),

                onClick =
                    onViewDetailsClick
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 13.dp,
                                vertical = 10.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(7.dp)
                                .background(
                                    color =
                                        AttendanceBlue,
                                    shape =
                                        CircleShape
                                )
                    )


                    Spacer(
                        modifier =
                            Modifier.size(8.dp)
                    )


                    Text(
                        text =
                            "Attendance is recorded automatically",

                        modifier =
                            Modifier.weight(1f),

                        fontSize =
                            10.sp,

                        color =
                            AttendanceGray
                    )


                    Text(
                        text =
                            "Details  ›",

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            AttendanceBlue
                    )
                }
            }
        }
    }
}


// ============================================================
// ATTENDANCE EVENT BOX
// ============================================================

@Composable
private fun AttendanceEventBox(

    modifier: Modifier,

    icon: ImageVector,

    title: String,

    time: String,

    status: String,

    eventColor: Color,

    completed: Boolean

) {

    Surface(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(18.dp),

        color =
            eventColor.copy(
                alpha =
                    if (completed) {
                        0.075f
                    } else {
                        0.045f
                    }
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 15.dp
                )
        ) {

            // ----------------------------------------------------
            // ICON
            // ----------------------------------------------------

            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(
                            color =
                                eventColor.copy(
                                    alpha = 0.12f
                                ),
                            shape =
                                RoundedCornerShape(11.dp)
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        icon,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(19.dp),

                    tint =
                        eventColor
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            // ----------------------------------------------------
            // LABEL
            // ----------------------------------------------------

            Text(
                text =
                    title,

                fontSize =
                    9.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    AttendanceGray,

                letterSpacing =
                    0.6.sp
            )


            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )


            // ----------------------------------------------------
            // TIME
            // ----------------------------------------------------

            Text(
                text =
                    time,

                fontSize =
                    21.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )


            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )


            // ----------------------------------------------------
            // STATUS
            // ----------------------------------------------------

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .background(
                                color =
                                    eventColor,
                                shape =
                                    CircleShape
                            )
                )


                Spacer(
                    modifier =
                        Modifier.size(5.dp)
                )


                Text(
                    text =
                        status,

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        eventColor
                )
            }
        }
    }
}
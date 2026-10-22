package com.prodigit.bodhiattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// QUICK ACCESS
// ============================================================

@Composable
fun QuickAccessCard(

    onAttendanceHistoryClick: () -> Unit = {},

    onOnDutyClick: () -> Unit = {},

    onRequestsClick: () -> Unit = {},

    onSystemHealthClick: () -> Unit = {},
    showOnDuty: Boolean = false

) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        // ========================================================
        // TITLE
        // ========================================================

        Text(
            text = "Quick Access",

            fontSize = 13.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        // ========================================================
        // QUICK ACCESS CARD
        // ========================================================

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(22.dp),

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

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 16.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceEvenly,

                verticalAlignment =
                    Alignment.Top
            ) {

                QuickAccessItem(
                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Outlined.History,

                    label =
                        "History",

                    color =
                        Color(0xFF1769E0),

                    onClick =
                        onAttendanceHistoryClick
                )


                if (showOnDuty) QuickAccessItem(
                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Outlined.WorkOutline,

                    label =
                        "On Duty",

                    color =
                        Color(0xFF159957),

                    onClick =
                        onOnDutyClick
                )


                QuickAccessItem(
                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Outlined.Assignment,

                    label =
                        "Requests",

                    color =
                        Color(0xFF8B5CF6),

                    onClick =
                        onRequestsClick
                )


                QuickAccessItem(
                    modifier =
                        Modifier.weight(1f),

                    icon =
                        Icons.Outlined.HealthAndSafety,

                    label =
                        "System\nHealth",

                    color =
                        Color(0xFFF59E0B),

                    onClick =
                        onSystemHealthClick
                )
            }
        }
    }
}


// ============================================================
// QUICK ACCESS ITEM
// ============================================================

@Composable
private fun QuickAccessItem(

    modifier: Modifier,

    icon: ImageVector,

    label: String,

    color: Color,

    onClick: () -> Unit

) {

    Column(
        modifier =
            modifier
                .clickable(
                    onClick = onClick
                )
                .padding(
                    vertical = 4.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        // ========================================================
        // ICON CONTAINER
        // ========================================================

        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        color =
                            color.copy(
                                alpha = 0.11f
                            ),

                        shape =
                            RoundedCornerShape(
                                15.dp
                            )
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    icon,

                contentDescription =
                    label,

                modifier =
                    Modifier.size(23.dp),

                tint =
                    color
            )
        }


        Spacer(
            modifier =
                Modifier.height(7.dp)
        )


        // ========================================================
        // LABEL
        // ========================================================

        Text(
            text =
                label,

            fontSize =
                9.sp,

            fontWeight =
                FontWeight.Medium,

            textAlign =
                TextAlign.Center,

            lineHeight =
                11.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )
    }
}
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WorkHistory
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// COLORS
// ============================================================

private val WorkBlue =
    Color(0xFF1769E0)

private val TeaBlue =
    Color(0xFF2589E8)

private val LunchPurple =
    Color(0xFF8B5CF6)

private val BreakOrange =
    Color(0xFFF59E0B)


// ============================================================
// BREAK / WORK SUMMARY
// ============================================================

@Composable
fun BreakSummaryRow(

    netWorkHours: String,

    teaBreakDuration: String,

    lunchBreakDuration: String,

    otherBreakDuration: String

) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        // ========================================================
        // SECTION TITLE
        // ========================================================

        Text(
            text = "Work Summary",

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
        // FIRST ROW
        // ========================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            SummaryItem(
                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Outlined.WorkHistory,

                title =
                    "Net Work",

                value =
                    netWorkHours,

                color =
                    WorkBlue
            )


            SummaryItem(
                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Outlined.Coffee,

                title =
                    "Tea (not classified)",

                value =
                    teaBreakDuration,

                color =
                    TeaBlue
            )
        }


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        // ========================================================
        // SECOND ROW
        // ========================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            SummaryItem(
                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Outlined.Restaurant,

                title =
                    "Lunch (not classified)",

                value =
                    lunchBreakDuration,

                color =
                    LunchPurple
            )


            SummaryItem(
                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Outlined.Timer,

                title =
                    "Total Breaks",

                value =
                    otherBreakDuration,

                color =
                    BreakOrange
            )
        }
    }
}


// ============================================================
// SUMMARY ITEM
// ============================================================

@Composable
private fun SummaryItem(

    modifier: Modifier,

    icon: ImageVector,

    title: String,

    value: String,

    color: Color

) {

    Card(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White.copy(
                        alpha = 0.94f
                    )
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 13.dp,
                        vertical = 14.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // ====================================================
            // ICON
            // ====================================================

            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            color =
                                color.copy(
                                    alpha = 0.11f
                                ),

                            shape =
                                RoundedCornerShape(
                                    13.dp
                                )
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
                        Modifier.size(21.dp),

                    tint =
                        color
                )
            }


            Spacer(
                modifier =
                    Modifier.size(10.dp)
            )


            // ====================================================
            // VALUE
            // ====================================================

            Column {

                Text(
                    text =
                        value,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )


                Spacer(
                    modifier =
                        Modifier.height(1.dp)
                )


                Text(
                    text =
                        title,

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
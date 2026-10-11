package com.prodigit.bodhiattendance.ui.systemhealth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Sync

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.prodigit.bodhiattendance.ui.systemhealth.HealthItemState
import com.prodigit.bodhiattendance.ui.systemhealth.HealthStatus


// ============================================================
// SYSTEM HEALTH GROUP CARD
//
// Modern grouped container used by:
//
// - Device & Security
// - Device Services
// - Attendance Environment
// - Data & Sync
//
// Instead of rendering every health check as a large separate
// elevated card, related checks are placed inside one container.
// ============================================================

@Composable
fun SystemHealthGroupCard(

    modifier: Modifier = Modifier,

    content: @Composable () -> Unit

) {

    Card(

        modifier =
            modifier
                .fillMaxWidth(),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
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
                        vertical = 4.dp
                    )

        ) {

            content()
        }
    }
}


// ============================================================
// SYSTEM HEALTH CARD
//
// This function name is intentionally retained.
//
// Existing SystemHealthScreen code can therefore continue using:
//
// SystemHealthCard(...)
//
// without breaking while we redesign the remaining files.
//
// The visual implementation is now a compact enterprise-style
// health row instead of a separate large card.
// ============================================================

@Composable
fun SystemHealthCard(

    item: HealthItemState,

    icon: ImageVector,

    modifier: Modifier = Modifier,

    displayTitle: String? = null,

    showDivider: Boolean = false

) {

    // ========================================================
    // USER-FACING TITLE
    //
    // Allows us to keep domain/internal names unchanged while
    // presenting clearer wording in the UI.
    //
    // Example:
    //
    // batteryOptimization
    // internal title -> Battery Optimization
    //
    // screen title   -> Background Operation
    // ========================================================

    val title =
        displayTitle
            ?: item.title


    // ========================================================
    // STATUS APPEARANCE
    // ========================================================

    val statusColor =
        when (item.status) {

            HealthStatus.READY ->
                Color(
                    0xFF16834A
                )

            HealthStatus.NOT_READY ->
                MaterialTheme
                    .colorScheme
                    .error

            HealthStatus.NOT_CONFIGURED ->
                Color(
                    0xFF687386
                )

            HealthStatus.NOT_REQUIRED ->
                Color(0xFF687386)

            HealthStatus.CHECKING ->
                MaterialTheme
                    .colorScheme
                    .primary
        }


    // ========================================================
    // STATUS TEXT
    //
    // More employee-friendly wording than the previous:
    //
    // Attention
    // Pending
    //
    // ========================================================

    val statusText =
        when (item.status) {

            HealthStatus.READY ->
                "Ready"

            HealthStatus.NOT_READY ->
                "Action"

            HealthStatus.NOT_CONFIGURED ->
                "Pending"

            HealthStatus.NOT_REQUIRED ->
                "N/A"

            HealthStatus.CHECKING ->
                "Checking"
        }


    // ========================================================
    // STATUS ICON
    // ========================================================

    val statusIcon =
        when (item.status) {

            HealthStatus.READY ->
                Icons.Outlined.CheckCircle

            HealthStatus.NOT_READY ->
                Icons.Outlined.ErrorOutline

            HealthStatus.NOT_CONFIGURED ->
                Icons.Outlined.HourglassEmpty

            HealthStatus.NOT_REQUIRED ->
                Icons.Outlined.CheckCircle

            HealthStatus.CHECKING ->
                Icons.Outlined.Sync
        }


    // ========================================================
    // ROW
    // ========================================================

    Column(

        modifier =
            modifier
                .fillMaxWidth()

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 13.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {


            // =================================================
            // SERVICE ICON
            //
            // Smaller than the old 48dp icon container.
            // This gives the page a more compact dashboard look.
            // =================================================

            Surface(

                modifier =
                    Modifier
                        .size(
                            42.dp
                        ),

                shape =
                    RoundedCornerShape(
                        13.dp
                    ),

                color =
                    statusColor.copy(
                        alpha = 0.09f
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
                            title,

                        modifier =
                            Modifier
                                .size(
                                    21.dp
                                ),

                        tint =
                            statusColor
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier
                        .width(
                            12.dp
                        )
            )


            // =================================================
            // TITLE + DESCRIPTION
            // =================================================

            Column(

                modifier =
                    Modifier
                        .weight(
                            1f
                        )

            ) {

                Text(

                    text =
                        title,

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )


                if (item.message.isNotBlank()) {

                    Spacer(

                        modifier =
                            Modifier
                                .height(
                                    3.dp
                                )
                    )


                    Text(

                        text =
                            item.message,

                        fontSize =
                            11.sp,

                        lineHeight =
                            15.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        maxLines =
                            2,

                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier
                        .width(
                            10.dp
                        )
            )


            // =================================================
            // STATUS CHIP
            // =================================================

            Surface(

                shape =
                    RoundedCornerShape(
                        50
                    ),

                color =
                    statusColor.copy(
                        alpha = 0.09f
                    )

            ) {

                Row(

                    modifier =
                        Modifier
                            .padding(
                                horizontal = 9.dp,
                                vertical = 6.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.Center

                ) {

                    Icon(

                        imageVector =
                            statusIcon,

                        contentDescription =
                            statusText,

                        modifier =
                            Modifier
                                .size(
                                    14.dp
                                ),

                        tint =
                            statusColor
                    )


                    Spacer(

                        modifier =
                            Modifier
                                .width(
                                    4.dp
                                )
                    )


                    Text(

                        text =
                            statusText,

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            statusColor,

                        maxLines =
                            1
                    )
                }
            }
        }


        // ====================================================
        // OPTIONAL DIVIDER
        //
        // Used when multiple rows are placed inside the same
        // SystemHealthGroupCard.
        // ====================================================

        if (showDivider) {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()

            ) {

                // Keep divider aligned after the icon area.

                Spacer(

                    modifier =
                        Modifier
                            .width(
                                70.dp
                            )
                )


                Divider(

                    modifier =
                        Modifier
                            .weight(
                                1f
                            ),

                    thickness =
                        0.5.dp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                            .copy(
                                alpha = 0.65f
                            )
                )


                Spacer(

                    modifier =
                        Modifier
                            .width(
                                16.dp
                            )
                )
            }
        }
    }
}


// ============================================================
// SMALL STATUS DOT
//
// Reusable later by:
// - summary card
// - dashboard
// - system status indicators
//
// Not required by the current screen yet.
// ============================================================

@Composable
fun HealthStatusDot(

    status: HealthStatus,

    modifier: Modifier = Modifier

) {

    val color =
        when (status) {

            HealthStatus.READY ->
                Color(
                    0xFF16834A
                )

            HealthStatus.NOT_READY ->
                MaterialTheme
                    .colorScheme
                    .error

            HealthStatus.NOT_CONFIGURED ->
                Color(
                    0xFF687386
                )

            HealthStatus.NOT_REQUIRED ->
                Color(0xFF687386)

            HealthStatus.CHECKING ->
                MaterialTheme
                    .colorScheme
                    .primary
        }


    Surface(

        modifier =
            modifier
                .size(
                    8.dp
                ),

        shape =
            CircleShape,

        color =
            color

    ) {}
}

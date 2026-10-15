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
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HourglassEmpty

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.prodigit.bodhiattendance.ui.systemhealth.HealthStatus
import com.prodigit.bodhiattendance.ui.systemhealth.SystemHealthUiState


// ============================================================
// SYSTEM HEALTH SUMMARY CARD
//
// Modern readiness overview.
//
// IMPORTANT:
//
// This component does not decide individual health states.
//
// It only summarizes the HealthItemState values already
// calculated by SystemHealthViewModel.
//
// TOTAL CURRENT CHECKS = 14
// ============================================================

@Composable
fun SystemHealthSummaryCard(

    uiState: SystemHealthUiState,

    modifier: Modifier = Modifier

) {

    // GPS and trusted device readiness are evaluated; BLE is optional.
    // Internet and Sync are operational diagnostics, not prerequisites
    // for an OFFLINE attendance capture.
    // Wi-Fi contributes only when VERIFIED assigned office requires it.
    val healthItems = buildList {
        add(uiState.locationPermission)
        add(uiState.backgroundLocationPermission)
        add(uiState.locationService)
        add(uiState.registeredDevice)
        add(uiState.trustedTime)
        add(uiState.batteryOptimization)
        add(uiState.attendanceConfiguration)
        add(uiState.offlineStorage)
        if (uiState.officeWifiRequirement == true) {
            add(uiState.officeWifi)
        }
        if (uiState.officeBleRequirement == true) {
            add(uiState.officeBeacon)
            add(uiState.bluetoothPermission)
            add(uiState.bluetoothService)
        }
    }.filter { it.status != HealthStatus.NOT_REQUIRED }

    val readyCount = healthItems.count { it.status == HealthStatus.READY }
    val attentionCount = healthItems.count { it.status == HealthStatus.NOT_READY }
    val pendingCount = healthItems.count {
        it.status == HealthStatus.NOT_CONFIGURED ||
            it.status == HealthStatus.CHECKING
    }
    val totalCount = healthItems.size
    val policyPending = uiState.officeWifiRequirement == null || uiState.officeBleRequirement == null
    val readinessFraction = if (totalCount > 0) {
        readyCount.toFloat() / totalCount.toFloat()
    } else 0f
    val readinessPercent = (readinessFraction * 100).toInt()

    // No claim of full readiness while the assigned office is unknown.
    val overallReady = attentionCount == 0 &&
        pendingCount == 0 && !policyPending
    val statusColor = when {
        attentionCount > 0 -> MaterialTheme.colorScheme.error
        overallReady -> Color(0xFF16834A)
        else -> Color(0xFF687386)
    }
    val statusTitle = when {
        attentionCount > 0 -> "Attention Required"
        policyPending -> "Office Policy Pending"
        pendingCount > 0 -> "Verification Pending"
        else -> "Checks Ready"
    }
    val statusMessage = buildString {
        append("$attentionCount action required • $pendingCount pending")
        if (policyPending) append(" • Office assignment not verified")
        append(if (uiState.officeBleRequirement == true) " • BLE required by CRM" else if (uiState.officeBleRequirement == false) " • BLE not required by CRM" else " • BLE policy pending")
    }

    // ========================================================
    // HERO CARD
    // ========================================================

    Card(

        modifier =
            modifier
                .fillMaxWidth(),

        shape =
            RoundedCornerShape(
                26.dp
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
                    4.dp
            )

    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        20.dp
                    )

        ) {


            // =================================================
            // TOP HEADER
            // =================================================

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                // =============================================
                // HEALTH ICON
                // =============================================

                Surface(

                    modifier =
                        Modifier
                            .size(
                                50.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    color =
                        statusColor.copy(
                            alpha = 0.10f
                        )

                ) {

                    Box(

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Icon(

                            imageVector =
                                Icons.Outlined
                                    .HealthAndSafety,

                            contentDescription =
                                "System Readiness",

                            modifier =
                                Modifier
                                    .size(
                                        26.dp
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
                                13.dp
                            )
                )


                // =============================================
                // TITLE
                // =============================================

                Column(

                    modifier =
                        Modifier
                            .weight(
                                1f
                            )

                ) {

                    Text(

                        text =
                            "System Readiness",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    Spacer(

                        modifier =
                            Modifier
                                .height(
                                    2.dp
                                )
                    )


                    Text(

                        text =
                            statusTitle,

                        fontSize =
                            19.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            statusColor
                    )
                }


                // =============================================
                // READINESS PERCENTAGE
                // =============================================

                Column(

                    horizontalAlignment =
                        Alignment.End

                ) {

                    Text(

                        text =
                            "$readinessPercent%",

                        fontSize =
                            26.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )


                    Text(

                        text =
                            "CHECKED",

                        fontSize =
                            9.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing =
                            0.8.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            // =================================================
            // STATUS MESSAGE
            // =================================================

            Spacer(

                modifier =
                    Modifier
                        .height(
                            14.dp
                        )
            )


            Text(

                text =
                    statusMessage,

                fontSize =
                    12.sp,

                lineHeight =
                    16.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


            // =================================================
            // PROGRESS BAR
            // =================================================

            Spacer(

                modifier =
                    Modifier
                        .height(
                            14.dp
                        )
            )


            LinearProgressIndicator(

                progress = {

                    readinessFraction
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            7.dp
                        ),

                color =
                    statusColor,

                trackColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant,

                strokeCap =
                    androidx.compose.ui.graphics.StrokeCap.Round
            )


            Spacer(

                modifier =
                    Modifier
                        .height(
                            18.dp
                        )
            )


            // =================================================
            // COUNTERS
            // =================================================

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )

            ) {


                ModernHealthCounter(

                    modifier =
                        Modifier
                            .weight(
                                1f
                            ),

                    count =
                        readyCount,

                    label =
                        "Ready",

                    icon =
                        Icons.Outlined.CheckCircle,

                    color =
                        Color(
                            0xFF16834A
                        )
                )


                ModernHealthCounter(

                    modifier =
                        Modifier
                            .weight(
                                1f
                            ),

                    count =
                        pendingCount,

                    label =
                        "Pending",

                    icon =
                        Icons.Outlined.HourglassEmpty,

                    color =
                        Color(
                            0xFF687386
                        )
                )


                ModernHealthCounter(

                    modifier =
                        Modifier
                            .weight(
                                1f
                            ),

                    count =
                        attentionCount,

                    label =
                        "Action",

                    icon =
                        Icons.Outlined.ErrorOutline,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }
        }
    }
}


// ============================================================
// MODERN HEALTH COUNTER
// ============================================================

@Composable
private fun ModernHealthCounter(

    count: Int,

    label: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    color: Color,

    modifier: Modifier = Modifier

) {

    Surface(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                15.dp
            ),

        color =
            color.copy(
                alpha = 0.07f
            )

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 10.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.Center

        ) {

            Surface(

                modifier =
                    Modifier
                        .size(
                            28.dp
                        ),

                shape =
                    CircleShape,

                color =
                    color.copy(
                        alpha = 0.11f
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
                            label,

                        modifier =
                            Modifier
                                .size(
                                    15.dp
                                ),

                        tint =
                            color
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier
                        .width(
                            7.dp
                        )
            )


            Column {

                Text(

                    text =
                        count.toString(),

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        color
                )


                Text(

                    text =
                        label,

                    fontSize =
                        9.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines =
                        1
                )
            }
        }
    }
}


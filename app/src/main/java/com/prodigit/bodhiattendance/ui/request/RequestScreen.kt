package com.prodigit.bodhiattendance.ui.requests

import androidx.compose.material3.MaterialTheme

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SyncProblem

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// REQUEST TYPE
// ============================================================

enum class EmployeeRequestType {

    REGULARIZATION,

    PERMISSION,

    ATTENDANCE_ISSUE
}


// ============================================================
// REQUEST STATUS
// ============================================================

enum class EmployeeRequestStatus {

    PENDING,

    APPROVED,

    REJECTED
}


// ============================================================
// REQUEST ITEM
// ============================================================

data class EmployeeRequestItem(

    val id: String,

    val type: EmployeeRequestType,

    val title: String,

    val date: String,

    val description: String,

    val status: EmployeeRequestStatus
)


// ============================================================
// REQUEST SCREEN
//
// BODY ONLY
//
// MainAppLayout owns:
//
// - App background
// - Global header
// - Scaffold
// - Bottom navigation
// ============================================================

@Composable
fun RequestScreen(

    modifier: Modifier = Modifier,

    onRegularizationClick: () -> Unit = {},

    onPermissionClick: () -> Unit = {},

    onAttendanceIssueClick: () -> Unit = {},

    onViewAllRequestsClick: () -> Unit = {},

    onRequestClick: (EmployeeRequestItem) -> Unit = {}

) {

    val live = com.prodigit.bodhiattendance.ui.live.liveUiState()
    if (live.requests == null) {
        com.prodigit.bodhiattendance.ui.live.DataUnavailable("Requests", live.errors["requests"] ?: live.errors["all"], live.loading)
        return
    }
    val requests = live.requests.mapNotNull { item ->
        val type = EmployeeRequestType.entries.firstOrNull { it.name == item.type } ?: return@mapNotNull null
        val status = EmployeeRequestStatus.entries.firstOrNull { it.name == item.status } ?: return@mapNotNull null
        EmployeeRequestItem(item.id, type, item.type.replace('_', ' '),
            com.prodigit.bodhiattendance.ui.live.displayDate(item.requestDate), item.description, status)
    }

    val pendingCount =
        requests.count {
            it.status == EmployeeRequestStatus.PENDING
        }


    val approvedCount =
        requests.count {
            it.status == EmployeeRequestStatus.APPROVED
        }


    val rejectedCount =
        requests.count {
            it.status == EmployeeRequestStatus.REJECTED
        }


    LazyColumn(

        modifier =
            modifier.fillMaxSize(),

        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 30.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                15.dp
            )

    ) {


        // ====================================================
        // PAGE HEADER
        // ====================================================

        item {

            RequestHeader()
        }


        // ====================================================
        // HERO CARD
        // ====================================================

        item {

            RequestHeroCard(

                total =
                    requests.size,

                pending =
                    pendingCount
            )
        }


        // ====================================================
        // NEW REQUEST HEADER
        // ====================================================

        item {

            SectionHeader(

                title =
                    "New Request",

                subtitle =
                    "Select the type of attendance request"
            )
        }


        // ====================================================
        // REGULARIZATION
        // ====================================================

        item {

            PremiumRequestCard(

                icon =
                    Icons.Outlined.FactCheck,

                title =
                    "Attendance Regularization",

                subtitle =
                    "Correct missed or incorrect attendance",

                supportingText =
                    "Check-in • Check-out • Break • Attendance status",

                accent =
                    Color(
                        0xFF156FE8
                    ),

                lightAccent =
                    MaterialTheme.colorScheme.surfaceVariant,

                onClick =
                    onRegularizationClick
            )
        }


        // ====================================================
        // PERMISSION
        // ====================================================

        item {

            PremiumRequestCard(

                icon =
                    Icons.Outlined.AccessTime,

                title =
                    "Permission / Short-Time",

                subtitle =
                    "Late arrival or early departure",

                supportingText =
                    "30 • 45 • 60 minutes only",

                accent =
                    Color(
                        0xFFF09318
                    ),

                lightAccent =
                    MaterialTheme.colorScheme.surfaceVariant,

                onClick =
                    onPermissionClick
            )
        }


        // ====================================================
        // ATTENDANCE ISSUE
        // ====================================================

        item {

            PremiumRequestCard(

                icon =
                    Icons.Outlined.SyncProblem,

                title =
                    "Report Attendance Issue",

                subtitle =
                    "Report automatic attendance problems",

                supportingText =
                    "GPS • BLE • Sync • Device • Recording",

                accent =
                    Color(
                        0xFF0A9F5A
                    ),

                lightAccent =
                    MaterialTheme.colorScheme.surfaceVariant,

                onClick =
                    onAttendanceIssueClick
            )
        }


        // ====================================================
        // MY REQUESTS HEADER
        // ====================================================

        item {

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
                            "My Requests",

                        color =
                            MaterialTheme.colorScheme.onSurface,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(

                        text =
                            "Track request progress and decisions",

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,

                        fontSize =
                            11.sp
                    )
                }


                Surface(

                    modifier =
                        Modifier.clickable {

                            onViewAllRequestsClick()
                        },

                    shape =
                        RoundedCornerShape(
                            50
                        ),

                    color =
                        MaterialTheme.colorScheme.surfaceVariant

                ) {

                    Row(

                        modifier =
                            Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 7.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {

                        Text(

                            text =
                                "View All",

                            color =
                                Color(
                                    0xFF1267DF
                                ),

                            fontSize =
                                11.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    2.dp
                                )
                        )


                        Icon(

                            imageVector =
                                Icons.Outlined.ChevronRight,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(
                                    16.dp
                                ),

                            tint =
                                Color(
                                    0xFF1267DF
                                )
                        )
                    }
                }
            }
        }


        // ====================================================
        // STATUS ANALYTICS
        // ====================================================

        item {

            RequestAnalyticsCard(

                pending =
                    pendingCount,

                approved =
                    approvedCount,

                rejected =
                    rejectedCount
            )
        }


        // ====================================================
        // RECENT REQUESTS
        // ====================================================

        item {

            SectionHeader(

                title =
                    "Recent Activity",

                subtitle =
                    "Latest attendance requests and decisions"
            )
        }


        requests.forEach { request ->

            item {

                ModernRecentRequestCard(

                    request =
                        request,

                    onClick = {

                        onRequestClick(
                            request
                        )
                    }
                )
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
// HEADER
// ============================================================

@Composable
private fun RequestHeader() {

    Column {

        Text(

            text =
                "Requests",

            color =
                MaterialTheme.colorScheme.onSurface,

            fontSize =
                26.sp,

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
                "Manage attendance corrections and exceptions",

            color =
                MaterialTheme.colorScheme.onSurfaceVariant,

            fontSize =
                14.sp
        )
    }
}


// ============================================================
// HERO CARD
// ============================================================

@Composable
private fun RequestHeroCard(

    total: Int,

    pending: Int

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

                containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.88f
                    )
            ),

        border =
            BorderStroke(
                1.dp,
                Color(
                    0xFFDCE8F7
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
                    .background(

                        brush =
                            Brush.horizontalGradient(

                                colors =
                                    listOf(
                                        Color(
                                            0xFFF4F9FF
                                        ),
                                        Color(
                                            0xFFEEF7FF
                                        ),
                                        Color(
                                            0xFFF9FBFF
                                        )
                                    )
                            )
                    )
                    .padding(
                        18.dp
                    )

        ) {


            // =================================================
            // HEADER
            // =================================================

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
                            15.dp
                        ),

                    color =
                        Color(
                            0xFFE2F0FF
                        )

                ) {

                    Box(

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Icon(

                            imageVector =
                                Icons.Outlined.Assignment,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(
                                    25.dp
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
                            "Employee Request Centre",

                        color =
                            MaterialTheme.colorScheme.onSurface,

                        fontSize =
                            16.sp,

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
                            "Attendance-related requests only",

                        color =
                            Color(
                                0xFF68728E
                            ),

                        fontSize =
                            11.sp
                    )
                }


                Surface(

                    shape =
                        RoundedCornerShape(
                            50
                        ),

                    color =
                        Color(
                            0xFFE6F7EC
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
                                Icons.Outlined.Shield,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(
                                    14.dp
                                ),

                            tint =
                                Color(
                                    0xFF0A9143
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
                                "CRM Reviewed",

                            color =
                                Color(
                                    0xFF087B38
                                ),

                            fontSize =
                                9.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            }


            Spacer(

                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            // =================================================
            // DESCRIPTION
            // =================================================

            Text(

                text =
                    "Submit attendance corrections, short-time permission and attendance issues.",

                color =
                    Color(
                        0xFF65708B
                    ),

                fontSize =
                    11.sp
            )


            Spacer(

                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            // =================================================
            // METRICS
            // =================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )

            ) {

                LightHeroMetric(

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    value =
                        total.toString(),

                    label =
                        "Total Requests",

                    accent =
                        Color(
                            0xFF1267DF
                        ),

                    background =
                        MaterialTheme.colorScheme.surfaceVariant
                )


                LightHeroMetric(

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    value =
                        pending.toString(),

                    label =
                        "Awaiting Decision",

                    accent =
                        Color(
                            0xFFE68A12
                        ),

                    background =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}


// ============================================================
// HERO METRIC
// ============================================================

@Composable
private fun LightHeroMetric(

    modifier: Modifier,

    value: String,

    label: String,

    accent: Color,

    background: Color

) {

    Surface(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                16.dp
            ),

        color =
            background,

        border =
            BorderStroke(
                1.dp,
                accent.copy(
                    alpha = 0.12f
                )
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    12.dp
                )

        ) {

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


            Spacer(

                modifier =
                    Modifier.height(
                        2.dp
                    )
            )


            Text(

                text =
                    label,

                color =
                    Color(
                        0xFF646E89
                    ),

                fontSize =
                    10.sp
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
                    0xFF747D97
                ),

            fontSize =
                11.sp
        )
    }
}


// ============================================================
// PREMIUM REQUEST CARD
// ============================================================

@Composable
private fun PremiumRequestCard(

    icon: ImageVector,

    title: String,

    subtitle: String,

    supportingText: String,

    accent: Color,

    lightAccent: Color,

    onClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {

                    onClick()
                },

        shape =
            RoundedCornerShape(
                21.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha =
                            0.96f
                    )
            ),

        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    3.dp
            )

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        15.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {


            // =================================================
            // ACCENT ICON
            // =================================================

            Surface(

                modifier =
                    Modifier.size(
                        52.dp
                    ),

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                color =
                    lightAccent

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
                                26.dp
                            ),

                        tint =
                            accent
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(
                        13.dp
                    )
            )


            // =================================================
            // CONTENT
            // =================================================

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
                            0xFF111B44
                        ),

                    fontSize =
                        15.sp,

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
                        subtitle,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    fontSize =
                        11.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                Surface(

                    shape =
                        RoundedCornerShape(
                            50
                        ),

                    color =
                        lightAccent

                ) {

                    Text(

                        text =
                            supportingText,

                        modifier =
                            Modifier.padding(
                                horizontal = 9.dp,
                                vertical = 5.dp
                            ),

                        color =
                            accent,

                        fontSize =
                            9.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )


            Surface(

                modifier =
                    Modifier.size(
                        34.dp
                    ),

                shape =
                    CircleShape,

                color =
                    Color(
                        0xFFF4F7FC
                    )

            ) {

                Box(

                    contentAlignment =
                        Alignment.Center

                ) {

                    Icon(

                        imageVector =
                            Icons.Outlined.ChevronRight,

                        contentDescription =
                            null,

                        tint =
                            accent
                    )
                }
            }
        }
    }
}


// ============================================================
// REQUEST ANALYTICS
// ============================================================

@Composable
private fun RequestAnalyticsCard(

    pending: Int,

    approved: Int,

    rejected: Int

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                21.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha =
                            0.95f
                    )
            ),

        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )

    ) {

        Column(

            modifier =
                Modifier.padding(
                    14.dp
                )

        ) {

            Text(

                text =
                    "Request Status",

                color =
                    MaterialTheme.colorScheme.onSurface,

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )

            ) {

                AnalyticsItem(

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    icon =
                        Icons.Outlined.HourglassTop,

                    label =
                        "Pending",

                    value =
                        pending,

                    accent =
                        Color(
                            0xFFE88E14
                        ),

                    background =
                        Color(
                            0xFFFFF3DD
                        )
                )


                AnalyticsItem(

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    icon =
                        Icons.Outlined.DoneAll,

                    label =
                        "Approved",

                    value =
                        approved,

                    accent =
                        Color(
                            0xFF08954A
                        ),

                    background =
                        Color(
                            0xFFE7F8ED
                        )
                )


                AnalyticsItem(

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    icon =
                        Icons.Outlined.ReportProblem,

                    label =
                        "Rejected",

                    value =
                        rejected,

                    accent =
                        Color(
                            0xFFE43B4D
                        ),

                    background =
                        Color(
                            0xFFFFE9EC
                        )
                )
            }
        }
    }
}


// ============================================================
// ANALYTICS ITEM
// ============================================================

@Composable
private fun AnalyticsItem(

    modifier: Modifier,

    icon: ImageVector,

    label: String,

    value: Int,

    accent: Color,

    background: Color

) {

    Surface(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                16.dp
            ),

        color =
            background

    ) {

        Column(

            modifier =
                Modifier.padding(
                    vertical = 12.dp,
                    horizontal = 8.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally

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


            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )


            Text(

                text =
                    value.toString(),

                color =
                    accent,

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(

                text =
                    label,

                color =
                    Color(
                        0xFF656E88
                    ),

                fontSize =
                    9.sp
            )
        }
    }
}


// ============================================================
// RECENT REQUEST CARD
// ============================================================

@Composable
private fun ModernRecentRequestCard(

    request: EmployeeRequestItem,

    onClick: () -> Unit

) {

    val visual =
        requestVisual(
            request.type
        )


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {

                    onClick()
                },

        shape =
            RoundedCornerShape(
                19.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha =
                            0.96f
                    )
            ),

        border =
            BorderStroke(
                1.dp,
                Color(
                    0xFFE3E9F4
                )
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )

    ) {

        Column {


            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            14.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                Surface(

                    modifier =
                        Modifier.size(
                            43.dp
                        ),

                    shape =
                        RoundedCornerShape(
                            13.dp
                        ),

                    color =
                        visual.background

                ) {

                    Box(

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Icon(

                            imageVector =
                                visual.icon,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(
                                    21.dp
                                ),

                            tint =
                                visual.accent
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            11.dp
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
                            request.title,

                        color =
                            Color(
                                0xFF151E47
                            ),

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(

                        text =
                            request.date,

                        color =
                            Color(
                                0xFF7B839A
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
                            request.description,

                        color =
                            Color(
                                0xFF626B85
                            ),

                        fontSize =
                            10.sp,

                        maxLines =
                            1
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            6.dp
                        )
                )


                StatusChip(
                    status =
                        request.status
                )
            }


            HorizontalDivider(
                color =
                    Color(
                        0xFFEEF1F6
                    )
            )


            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Icon(

                    imageVector =
                        Icons.Outlined.Description,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(
                            14.dp
                        ),

                    tint =
                        Color(
                            0xFF8790A6
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
                        "View request details",

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    fontSize =
                        10.sp
                )


                Icon(

                    imageVector =
                        Icons.Outlined.ChevronRight,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(
                            17.dp
                        ),

                    tint =
                        Color(
                            0xFF7C85A0
                        )
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

    status: EmployeeRequestStatus

) {

    val text: String
    val accent: Color
    val background: Color


    when (
        status
    ) {

        EmployeeRequestStatus.PENDING -> {

            text =
                "Pending"

            accent =
                Color(
                    0xFFD9850E
                )

            background =
                Color(
                    0xFFFFF1CE
                )
        }


        EmployeeRequestStatus.APPROVED -> {

            text =
                "Approved"

            accent =
                Color(
                    0xFF07893E
                )

            background =
                Color(
                    0xFFE2F7E9
                )
        }


        EmployeeRequestStatus.REJECTED -> {

            text =
                "Rejected"

            accent =
                Color(
                    0xFFE12F43
                )

            background =
                Color(
                    0xFFFFE5E9
                )
        }
    }


    Surface(

        shape =
            RoundedCornerShape(
                50
            ),

        color =
            background

    ) {

        Text(

            text =
                text,

            modifier =
                Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 5.dp
                ),

            color =
                accent,

            fontSize =
                9.sp,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}


// ============================================================
// VISUAL MODEL
// ============================================================

private data class RequestVisual(

    val icon: ImageVector,

    val accent: Color,

    val background: Color
)


// ============================================================
// REQUEST VISUAL
// ============================================================

private fun requestVisual(

    type: EmployeeRequestType

): RequestVisual {

    return when (
        type
    ) {

        EmployeeRequestType.REGULARIZATION ->

            RequestVisual(
                icon =
                    Icons.Outlined.FactCheck,
                accent =
                    Color(
                        0xFF156FE8
                    ),
                background =
                    MaterialTheme.colorScheme.surfaceVariant
            )


        EmployeeRequestType.PERMISSION ->

            RequestVisual(
                icon =
                    Icons.Outlined.AccessTime,
                accent =
                    Color(
                        0xFFF09318
                    ),
                background =
                    MaterialTheme.colorScheme.surfaceVariant
            )


        EmployeeRequestType.ATTENDANCE_ISSUE ->

            RequestVisual(
                icon =
                    Icons.Outlined.SyncProblem,
                accent =
                    Color(
                        0xFF0A9F5A
                    ),
                background =
                    MaterialTheme.colorScheme.surfaceVariant
            )
    }
}
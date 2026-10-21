
package com.prodigit.bodhiattendance.ui.requests

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================
// MODELS - Can later be mapped from CRM API DTOs
// =====================================================

enum class BodhiRequestType(val label: String) {
    PERMISSION("Permission / Short Time"),
    REGULARIZATION("Attendance Regularization"),
    ATTENDANCE_ISSUE("Attendance Issue")
}

enum class BodhiRequestStatus(val label: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

data class BodhiRequest(
    val id: String,
    val type: BodhiRequestType,
    val status: BodhiRequestStatus,
    val requestDate: String,
    val description: String,
    val durationMinutes: Int? = null,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val reviewRemarks: String? = null
)

// =====================================================
// COLORS
// =====================================================

private val PageBg = MaterialTheme.colorScheme.surfaceVariant
private val TextDark = MaterialTheme.colorScheme.onSurface
private val TextMuted = MaterialTheme.colorScheme.onSurfaceVariant
private val BrandBlue = Color(0xFF1769E8)
private val Stroke = MaterialTheme.colorScheme.outlineVariant

private fun statusColor(status: BodhiRequestStatus): Color =
    when (status) {
        BodhiRequestStatus.PENDING -> Color(0xFFB96E00)
        BodhiRequestStatus.APPROVED -> Color(0xFF16865C)
        BodhiRequestStatus.REJECTED -> Color(0xFFD64154)
    }

private fun statusBackground(
    status: BodhiRequestStatus
): Color = when (status) {
    BodhiRequestStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
    BodhiRequestStatus.APPROVED -> MaterialTheme.colorScheme.surfaceVariant
    BodhiRequestStatus.REJECTED -> MaterialTheme.colorScheme.surfaceVariant
}

private fun requestColor(type: BodhiRequestType): Color =
    when (type) {
        BodhiRequestType.PERMISSION -> Color(0xFFE6A12E)
        BodhiRequestType.REGULARIZATION -> BrandBlue
        BodhiRequestType.ATTENDANCE_ISSUE -> Color(0xFF14986D)
    }

// =====================================================
// REQUEST HISTORY SCREEN
// =====================================================

@Composable
fun RequestHistoryScreen(
    requests: List<BodhiRequest>,
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember {
        mutableStateOf("All")
    }

    val filters = listOf(
        "All", "Pending", "Approved", "Rejected"
    )

    val visibleRequests = remember(requests, selectedFilter) {
        if (selectedFilter == "All") {
            requests
        } else {
            requests.filter {
                it.status.label == selectedFilter
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
    ) {

        RequestChildHeader(
            title = "Request History",
            subtitle = "Track your attendance requests",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {


                    Text(
                        "My Requests",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 19.sp
                    )

                    Text(
                        "Check your submitted requests and " +
                                "management decisions.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filters.forEach { filter ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selectedFilter == filter,
                                onClick = {
                                    selectedFilter = filter
                                },
                                label = {
                                    Text(
                                        filter,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults
                                    .filterChipColors(
                                        selectedContainerColor =
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        selectedLabelColor =
                                            BrandBlue
                                    )
                            )
                        }
                    }

                    Text(
                        "${visibleRequests.size} requests",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            if (visibleRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No requests found",
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                "There are no requests " +
                                        "for this status.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            items(
                items = visibleRequests,
                key = { it.id }
            ) { request ->
                RequestHistoryCard(
                    request = request,
                    onClick = {
                        onRequestClick(request.id)
                    }
                )
            }
        }
    }
}

// =====================================================
// REUSABLE REQUEST CARD
// =====================================================

@Composable
private fun RequestHistoryCard(
    request: BodhiRequest,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Stroke),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            requestColor(request.type)
                                .copy(alpha = 0.10f),
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (request.type) {
                            BodhiRequestType.PERMISSION ->
                                Icons.Default.Schedule
                            BodhiRequestType.REGULARIZATION ->
                                Icons.Default.EditCalendar
                            BodhiRequestType.ATTENDANCE_ISSUE ->
                                Icons.Default.ReportProblem
                        },
                        contentDescription = null,
                        tint = requestColor(request.type)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.type.label,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        request.id,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                RequestStatusBadge(request.status)
            }

            Text(
                request.description,
                fontSize = 12.sp,
                color = TextMuted
            )

            HorizontalDivider(color = Stroke)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    request.requestDate,
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "View Details",
                        color = BrandBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =====================================================
// REUSABLE CHILD HEADER
// =====================================================

@Composable
fun RequestChildHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 6.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark
                )
            }

            Column {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

// =====================================================
// STATUS BADGE
// =====================================================

@Composable
fun RequestStatusBadge(
    status: BodhiRequestStatus
) {
    Surface(
        color = statusBackground(status),
        shape = CircleShape
    ) {
        Text(
            status.label,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            color = statusColor(status),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

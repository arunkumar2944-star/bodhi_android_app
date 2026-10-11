
package com.prodigit.bodhiattendance.ui.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DetailBackground = Color(0xFFF5F9FE)
private val DetailText = Color(0xFF17243D)
private val DetailMuted = Color(0xFF718198)
private val DetailBorder = Color(0xFFE2E9F3)
private val DetailBlue = Color(0xFF1769E8)

@Composable
fun RequestDetailsScreen(
    request: BodhiRequest?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDemo: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DetailBackground)
    ) {
        RequestChildHeader(
            title = "Request Details",
            subtitle = "Request information and review",
            onBack = onBack
        )

        if (request == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Request not found",
                    color = DetailMuted
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (isDemo) {
                DemoNotice()
            }

            // ============================================
            // REQUEST STATUS CARD
            // ============================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    DetailBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        when (request.status) {
                            BodhiRequestStatus.APPROVED ->
                                Icons.Default.CheckCircle
                            BodhiRequestStatus.REJECTED ->
                                Icons.Default.Cancel
                            BodhiRequestStatus.PENDING ->
                                Icons.Default.PendingActions
                        },
                        contentDescription = null,
                        tint = when (request.status) {
                            BodhiRequestStatus.APPROVED ->
                                Color(0xFF16865C)
                            BodhiRequestStatus.REJECTED ->
                                Color(0xFFD64154)
                            BodhiRequestStatus.PENDING ->
                                Color(0xFFC3831F)
                        },
                        modifier = Modifier.size(40.dp)
                    )

                    Text(
                        request.type.label,
                        color = DetailText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Request ID: ${request.id}",
                        color = DetailMuted,
                        fontSize = 12.sp
                    )

                    RequestStatusBadge(request.status)
                }
            }

            // ============================================
            // DETAILS CARD
            // ============================================

            DetailSectionCard(
                title = "Request Information"
            ) {
                DetailRow(
                    "Request Type",
                    request.type.label
                )

                DetailRow(
                    "Attendance / Permission Date",
                    request.requestDate
                )

                if (request.durationMinutes != null) {
                    DetailRow(
                        "Duration",
                        "${request.durationMinutes} minutes"
                    )
                }

                HorizontalDivider(color = DetailBorder)

                Text(
                    "Request Description",
                    fontWeight = FontWeight.Bold,
                    color = DetailText,
                    fontSize = 13.sp
                )

                Text(
                    request.description,
                    color = DetailMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }

            // ============================================
            // MANAGEMENT REVIEW
            // ============================================

            DetailSectionCard(
                title = "CRM Review"
            ) {

                DetailRow(
                    "Current Status",
                    request.status.label
                )

                if (request.status ==
                    BodhiRequestStatus.PENDING
                ) {
                    Text(
                        "Awaiting management review.",
                        color = DetailMuted,
                        fontSize = 13.sp
                    )
                } else {
                    DetailRow(
                        "Reviewed By",
                        request.reviewedBy
                            ?: "Not available"
                    )

                    DetailRow(
                        "Reviewed On",
                        request.reviewedAt
                            ?: "Not available"
                    )

                    Text(
                        "Reviewer Remarks",
                        fontWeight = FontWeight.Bold,
                        color = DetailText,
                        fontSize = 13.sp
                    )

                    Text(
                        request.reviewRemarks
                            ?: "No remarks provided.",
                        color = DetailMuted,
                        fontSize = 13.sp
                    )
                }
            }

            // ============================================
            // INFO NOTE
            // ============================================

            Surface(
                color = Color(0xFFE8F2FF),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = DetailBlue
                    )

                    Text(
                        "Attendance corrections and decisions " +
                                "are controlled by the CRM. " +
                                "This screen is read-only.",
                        color = DetailText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            DetailBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                color = DetailText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = DetailBorder)

            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            color = DetailMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            value,
            color = DetailText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

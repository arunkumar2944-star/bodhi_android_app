package com.prodigit.bodhiattendance.ui.request

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prodigit.bodhiattendance.ui.common.AppBackground
import com.prodigit.bodhiattendance.ui.common.AppMasterHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Blue = Color(0xFF1769E8)
private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveOnDutyWfhRequestScreen(
    initialType: String = "LEAVE", // "LEAVE", "ON_DUTY", "WORK_FROM_HOME"
    onBack: () -> Unit,
    onSubmit: (type: String, date: String, description: String, subtype: String?, requestedTime: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var leaveSubtype by remember { mutableStateOf("FULL_DAY") } // "FULL_DAY", "FIRST_HALF", "SECOND_HALF"
    var requestDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var reason by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dutyStartTime by remember { mutableStateOf("09:00") }
    var dutyEndTime by remember { mutableStateOf("17:00") }
    var isSubmitting by remember { mutableStateOf(false) }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp)
            ) {
                AppMasterHeader()
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
                        }
                        Text(
                            text = when (selectedType) {
                                "ON_DUTY" -> "Apply for On Duty"
                                "WORK_FROM_HOME" -> "Apply for Work From Home"
                                else -> "Apply for Leave"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                    }

                    // Request Category Selector
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Request Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedType == "LEAVE",
                                    onClick = { selectedType = "LEAVE" },
                                    label = { Text("Leave") },
                                    leadingIcon = { Icon(Icons.Outlined.EventBusy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedType == "ON_DUTY",
                                    onClick = { selectedType = "ON_DUTY" },
                                    label = { Text("On Duty") },
                                    leadingIcon = { Icon(Icons.Outlined.BusinessCenter, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedType == "WORK_FROM_HOME",
                                    onClick = { selectedType = "WORK_FROM_HOME" },
                                    label = { Text("WFH") },
                                    leadingIcon = { Icon(Icons.Outlined.HomeWork, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Form Content
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Subtype for Leave
                            if (selectedType == "LEAVE") {
                                Text("Leave Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = leaveSubtype == "FULL_DAY",
                                        onClick = { leaveSubtype = "FULL_DAY" },
                                        label = { Text("Full Day") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = leaveSubtype == "FIRST_HALF",
                                        onClick = { leaveSubtype = "FIRST_HALF" },
                                        label = { Text("First Half") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = leaveSubtype == "SECOND_HALF",
                                        onClick = { leaveSubtype = "SECOND_HALF" },
                                        label = { Text("Second Half") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Request Date
                            OutlinedTextField(
                                value = requestDate,
                                onValueChange = { requestDate = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Location for On Duty
                            if (selectedType == "ON_DUTY") {
                                OutlinedTextField(
                                    value = location,
                                    onValueChange = { location = it },
                                    label = { Text("Duty Location / Client Site") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = dutyStartTime,
                                        onValueChange = { dutyStartTime = it },
                                        label = { Text("Start Time (HH:mm)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = dutyEndTime,
                                        onValueChange = { dutyEndTime = it },
                                        label = { Text("End Time (HH:mm)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }

                            // Reason / Purpose
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = {
                                    Text(when (selectedType) {
                                        "ON_DUTY" -> "Purpose of On Duty Visit"
                                        "WORK_FROM_HOME" -> "Reason for Work From Home"
                                        else -> "Reason for Leave"
                                    })
                                },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Submit Button
                            Button(
                                onClick = {
                                    if (reason.trim().length < 5) return@Button
                                    isSubmitting = true
                                    val fullDescription = if (selectedType == "ON_DUTY" && location.isNotBlank()) {
                                        "Location: ${location.trim()} · Time: $dutyStartTime–$dutyEndTime · ${reason.trim()}"
                                    } else {
                                        reason.trim()
                                    }
                                    val subtype = if (selectedType == "LEAVE") leaveSubtype else null
                                    val reqTime = if (selectedType == "ON_DUTY") "$dutyStartTime-$dutyEndTime" else null

                                    onSubmit(selectedType, requestDate, fullDescription, subtype, reqTime)
                                },
                                enabled = reason.trim().length >= 5 && !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                Text(
                                    text = if (isSubmitting) "Submitting Request…" else "Submit Request",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

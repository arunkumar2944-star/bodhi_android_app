
package com.prodigit.bodhiattendance.ui.requests

import androidx.compose.material3.MaterialTheme

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// =======================================================
// BODHI COLORS
// =======================================================

private val RegularizationBlue = Color(0xFF1769E8)
private val RegularizationDark = Color(0xFF0B438F)
private val RegularizationLight = MaterialTheme.colorScheme.surfaceVariant
private val RegularizationBackground = MaterialTheme.colorScheme.surfaceVariant
private val RegularizationText = MaterialTheme.colorScheme.onSurface
private val RegularizationMuted = MaterialTheme.colorScheme.onSurfaceVariant
private val RegularizationBorder = MaterialTheme.colorScheme.outlineVariant

// =======================================================
// DATA MODEL
// =======================================================

enum class RegularizationType(val label: String) {
    MISSED_CHECK_IN("Missed Check-In"),
    MISSED_CHECK_OUT("Missed Check-Out"),
    MISSED_BREAK_START("Missed Break Start"),
    MISSED_BREAK_END("Missed Break End"),
    INCORRECT_STATUS("Incorrect Attendance Status")
}

data class AttendanceRegularizationRequestData(
    val attendanceDate: String, // yyyy-MM-dd
    val correctionType: RegularizationType,
    val requestedTime: String,  // HH:mm, 24-hour
    val reason: String,
    val evidenceUri: Uri?
)

// =======================================================
// MAIN SCREEN
// =======================================================

@Composable
fun AttendanceRegularizationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSubmit: (AttendanceRegularizationRequestData) -> Unit = {}
) {
    val submission = com.prodigit.bodhiattendance.ui.live.liveUiState()
    val context = LocalContext.current

    val today = remember { Calendar.getInstance() }

    val apiDateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    val displayDateFormat = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    }

    var attendanceDate by remember {
        mutableStateOf(apiDateFormat.format(today.time))
    }

    var displayDate by remember {
        mutableStateOf(displayDateFormat.format(today.time))
    }

    var correctionType by remember {
        mutableStateOf(RegularizationType.MISSED_CHECK_IN)
    }

    var requestedTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var evidenceUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) evidenceUri = uri
    }

    fun openDatePicker() {
        val now = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                attendanceDate =
                    apiDateFormat.format(selected.time)
                displayDate =
                    displayDateFormat.format(selected.time)

                // Clear time when the date changes.
                requestedTime = ""
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    fun openTimePicker() {
        val now = Calendar.getInstance()

        TimePickerDialog(
            context,
            { _, hour, minute ->
                requestedTime = String.format(
                    Locale.US,
                    "%02d:%02d",
                    hour,
                    minute
                )
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            false
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RegularizationBackground)
    ) {

        // Child header: global master header already exists.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = RegularizationDark
                )
            }

            Column {
                Text(
                    "Attendance Regularization",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RegularizationText
                )

                Text(
                    "Request an attendance correction",
                    fontSize = 12.sp,
                    color = RegularizationMuted
                )
            }
        }

        HorizontalDivider(color = RegularizationBorder)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ============================================
            // INTRODUCTION
            // ============================================

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = RegularizationLight
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = RegularizationBlue,
                        modifier = Modifier.size(27.dp)
                    )

                    Column {
                        Text(
                            "Correct Attendance Records",
                            color = RegularizationDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Use this form to request a correction " +
                                    "for missing or incorrect check-in, " +
                                    "check-out or break events.",
                            color = RegularizationText,
                            fontSize = 12.sp,
                            lineHeight = 19.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "CRM approval is required.",
                            fontWeight = FontWeight.SemiBold,
                            color = RegularizationBlue,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ============================================
            // DATE
            // ============================================

            RegularizationHeading(
                "Attendance Date",
                "Select the date requiring correction"
            )

            RegularizationValueCard(
                value = displayDate,
                icon = {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = RegularizationBlue
                    )
                },
                onClick = ::openDatePicker
            )

            // ============================================
            // CORRECTION TYPE
            // ============================================

            RegularizationHeading(
                "Correction Type",
                "Choose the attendance event"
            )

            var typeExpanded by remember {
                mutableStateOf(false)
            }

            Box(modifier = Modifier.fillMaxWidth()) {

                OutlinedCard(
                    onClick = { typeExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        RegularizationBorder
                    ),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            correctionType.label,
                            modifier = Modifier.weight(1f),
                            color = RegularizationText,
                            fontWeight = FontWeight.Medium
                        )

                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select type",
                            tint = RegularizationBlue
                        )
                    }
                }

                DropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = {
                        typeExpanded = false
                    }
                ) {
                    RegularizationType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                correctionType = type
                                typeExpanded = false
                            },
                            trailingIcon = {
                                if (correctionType == type) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = RegularizationBlue
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // ============================================
            // REQUESTED TIME
            // ============================================

            RegularizationHeading(
                "Requested Event Time",
                "Choose the actual time of the event"
            )

            RegularizationValueCard(
                value = if (requestedTime.isBlank()) {
                    "Select Time"
                } else {
                    requestedTime
                },
                icon = {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = RegularizationBlue
                    )
                },
                onClick = ::openTimePicker
            )

            // ============================================
            // REASON
            // ============================================

            RegularizationHeading(
                "Reason for Correction",
                "Explain what happened"
            )

            OutlinedTextField(
                value = reason,
                onValueChange = {
                    if (it.length <= 250) {
                        reason = it
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Example: I entered the office, " +
                                "but my check-in was not recorded.",
                        fontSize = 13.sp
                    )
                },
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                supportingText = {
                    Text("${reason.length}/250")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RegularizationBlue,
                    unfocusedBorderColor = RegularizationBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // ============================================
            // EVIDENCE
            // ============================================

            RegularizationHeading(
                "Supporting Evidence",
                "Optional screenshot or image"
            )

            OutlinedCard(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    RegularizationBorder
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = RegularizationBlue
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (evidenceUri == null) {
                                "Attachments unavailable"
                            } else {
                                "Image Selected"
                            },
                            color = RegularizationText,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            if (evidenceUri == null) {
                                "Current CRM API supports text requests only"
                            } else {
                                "Current CRM API supports text requests only"
                            },
                            color = RegularizationMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (evidenceUri != null) {
                TextButton(
                    onClick = { evidenceUri = null }
                ) {
                    Text("Remove attachment")
                }
            }

            // ============================================
            // CRM APPROVAL NOTE
            // ============================================

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = RegularizationBlue
                    )

                    Column {
                        Text(
                            "Management Review",
                            color = RegularizationDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "Submitting this request will not " +
                                    "change your attendance immediately. " +
                                    "The correction must be reviewed " +
                                    "and approved through CRM.",
                            color = RegularizationMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ============================================
            // SUBMIT REQUEST
            // ============================================

            submission.submissionMessage?.let { Text(it) }
            Button(
                enabled = !submission.submitting,
                onClick = {
                    when {
                        requestedTime.isBlank() -> {
                            Toast.makeText(
                                context,
                                "Select the requested event time",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        reason.trim().length < 10 -> {
                            Toast.makeText(
                                context,
                                "Please enter at least 10 characters",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            onSubmit(
                                AttendanceRegularizationRequestData(
                                    attendanceDate = attendanceDate,
                                    correctionType = correctionType,
                                    requestedTime = requestedTime,
                                    reason = reason.trim(),
                                    evidenceUri = evidenceUri
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegularizationBlue
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    "Submit Regularization",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// =======================================================
// REUSABLE FIELD TITLE
// =======================================================

@Composable
private fun RegularizationHeading(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            color = RegularizationText,
            fontSize = 14.sp
        )

        Text(
            subtitle,
            color = RegularizationMuted,
            fontSize = 12.sp
        )
    }
}

// =======================================================
// REUSABLE DATE / TIME CARD
// =======================================================

@Composable
private fun RegularizationValueCard(
    value: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            RegularizationBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()

            Text(
                value,
                modifier = Modifier.weight(1f),
                color = RegularizationText,
                fontWeight = FontWeight.Medium
            )

            Text(
                "›",
                color = RegularizationMuted,
                fontSize = 24.sp
            )
        }
    }
}

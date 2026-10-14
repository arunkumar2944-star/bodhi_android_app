
package com.prodigit.bodhiattendance.ui.requests

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

// ====================================================
// COLORS
// ====================================================

private val IssueGreen = Color(0xFF15976D)
private val IssueDarkGreen = Color(0xFF087A59)
private val IssueLightGreen = Color(0xFFE9F9F1)
private val PageBackground = Color(0xFFF6FAFF)
private val DarkText = Color(0xFF17243D)
private val SecondaryText = Color(0xFF78859A)
private val FieldBorder = Color(0xFFE1E8F1)
private val BodhiBlue = Color(0xFF1769D8)

// ====================================================
// DATA MODEL
// ====================================================

data class AttendanceIssueRequestData(
    val issueDate: String,
    val issueType: String,
    val affectedEvent: String,
    val description: String,
    val evidenceUri: Uri?
)

private val issueTypes = listOf(
    "GPS / Location Issue",
    "BLE Beacon Issue",
    "Attendance Sync Issue",
    "Device / Bluetooth Issue",
    "Event Recording Issue",
    "Other Technical Issue"
)

private val attendanceEvents = listOf(
    "Check In",
    "Check Out",
    "Break Start",
    "Break End",
    "Other"
)

// ====================================================
// MAIN SCREEN
// ====================================================

@Composable
fun AttendanceIssueScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSubmit: (AttendanceIssueRequestData) -> Unit = {}
) {
    val submission = com.prodigit.bodhiattendance.ui.live.liveUiState()
    val context = LocalContext.current

    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    }

    var selectedDate by remember {
        mutableStateOf(
            dateFormatter.format(Calendar.getInstance().time)
        )
    }

    var issueType by remember { mutableStateOf("") }
    var affectedEvent by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var evidenceUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            evidenceUri = uri
        }
    }

    fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                selectedDate = dateFormatter.format(selected.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Prevent selecting future dates.
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {

        // ================================================
        // CHILD SCREEN HEADER
        // ================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    start = 6.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DarkText
                )
            }

            Column {
                Text(
                    text = "Report Attendance Issue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Text(
                    text = "Automatic attendance support",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        }

        HorizontalDivider(color = FieldBorder)

        // ================================================
        // SCROLLABLE BODY
        // ================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ============================================
            // INTRODUCTION CARD
            // ============================================

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = IssueLightGreen
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFC7EBDA)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                IssueGreen,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Column {
                        Text(
                            "Attendance Issue",
                            color = IssueDarkGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Report automatic attendance problems " +
                                    "related to GPS, BLE beacons, " +
                                    "synchronization or device recording.",
                            color = DarkText,
                            fontSize = 12.sp,
                            lineHeight = 19.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Your request will be reviewed in CRM.",
                            color = IssueDarkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ============================================
            // ISSUE DETAILS
            // ============================================

            IssueSectionHeading(
                title = "Issue Details",
                subtitle = "Provide information about the problem"
            )

            IssueLabel("Issue Date", required = true)

            IssueClickableField(
                value = selectedDate,
                icon = Icons.Default.CalendarMonth,
                onClick = { showDatePicker() }
            )

            IssueLabel("Issue Category", required = true)

            IssueDropdown(
                selectedValue = issueType,
                placeholder = "Select issue category",
                options = issueTypes,
                onSelected = { issueType = it }
            )

            IssueLabel("Affected Attendance Event", required = true)

            IssueDropdown(
                selectedValue = affectedEvent,
                placeholder = "Select attendance event",
                options = attendanceEvents,
                onSelected = { affectedEvent = it }
            )

            // ============================================
            // DESCRIPTION
            // ============================================

            IssueSectionHeading(
                title = "Issue Description",
                subtitle = "Explain what went wrong"
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    if (it.length <= 250) description = it
                },
                placeholder = {
                    Text(
                        "Example: I was inside the office, " +
                                "but my check-in was not recorded.",
                        fontSize = 13.sp,
                        color = SecondaryText
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IssueGreen,
                    unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                supportingText = {
                    Text(
                        "${description.length}/250",
                        modifier = Modifier.fillMaxWidth(),
                        color = SecondaryText
                    )
                }
            )

            // ============================================
            // SUPPORTING EVIDENCE
            // ============================================

            IssueSectionHeading(
                title = "Supporting Evidence",
                subtitle = "Text submission supported; attachment API is pending"
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                enabled = false,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, FieldBorder),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(
                                IssueLightGreen,
                                RoundedCornerShape(13.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = IssueGreen
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (evidenceUri == null)
                                "Attachments unavailable"
                            else
                                "Image Selected",
                            color = DarkText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            if (evidenceUri == null)
                                "Current CRM API supports text requests only"
                            else
                                "Tap to change attachment",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = IssueGreen
                    )
                }
            }

            if (evidenceUri != null) {
                TextButton(
                    onClick = { evidenceUri = null }
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Remove Attachment")
                }
            }

            // ============================================
            // CRM REVIEW NOTICE
            // ============================================

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEAF3FF)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = BodhiBlue
                    )

                    Column {
                        Text(
                            "CRM Review Required",
                            fontWeight = FontWeight.Bold,
                            color = BodhiBlue,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "This report does not automatically " +
                                    "change your attendance. " +
                                    "Management will review it.",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ============================================
            // SUBMIT
            // ============================================

            submission.submissionMessage?.let { Text(it) }
            Button(
                enabled = !submission.submitting,
                onClick = {
                    when {
                        issueType.isBlank() -> {
                            Toast.makeText(
                                context,
                                "Select an issue category",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        affectedEvent.isBlank() -> {
                            Toast.makeText(
                                context,
                                "Select an attendance event",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        description.isBlank() -> {
                            Toast.makeText(
                                context,
                                "Please describe the issue",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            onSubmit(
                                AttendanceIssueRequestData(
                                    issueDate = selectedDate,
                                    issueType = issueType,
                                    affectedEvent = affectedEvent,
                                    description = description.trim(),
                                    evidenceUri = evidenceUri
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IssueGreen
                )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    "Submit Request",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ====================================================
// SECTION HEADING
// ====================================================

@Composable
private fun IssueSectionHeading(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            color = DarkText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            subtitle,
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}

// ====================================================
// FIELD LABEL
// ====================================================

@Composable
private fun IssueLabel(
    title: String,
    required: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkText
        )

        if (required) {
            Text(
                " *",
                fontSize = 14.sp,
                color = Color(0xFFE53935)
            )
        }
    }
}

// ====================================================
// CLICKABLE DATE FIELD
// ====================================================

@Composable
private fun IssueClickableField(
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, FieldBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = IssueGreen
            )

            Spacer(Modifier.width(12.dp))

            Text(
                value,
                modifier = Modifier.weight(1f),
                color = DarkText,
                fontSize = 14.sp
            )

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryText
            )
        }
    }
}

// ====================================================
// DROPDOWN
// ====================================================

@Composable
private fun IssueDropdown(
    selectedValue: String,
    placeholder: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, FieldBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue.ifBlank { placeholder },
                    modifier = Modifier.weight(1f),
                    color = if (selectedValue.isBlank())
                        SecondaryText else DarkText,
                    fontSize = 14.sp
                )

                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    tint = IssueGreen
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

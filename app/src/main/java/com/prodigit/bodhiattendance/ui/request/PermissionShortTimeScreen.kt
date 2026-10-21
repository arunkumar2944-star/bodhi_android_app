
package com.crm.attendance.ui.screens.request

import androidx.compose.material3.MaterialTheme

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.prodigit.bodhiattendance.ui.common.mouseWheelScroll
// ======================================================
// COLORS
// ======================================================

private val PrimaryBlue = Color(0xFF0866E8)
private val DeepBlue = Color(0xFF0349AC)
private val LightBlue = Color(0xFFEAF3FF)
private val PageBackground = Color(0xFFF7F9FD)
private val DarkText = Color(0xFF142038)
private val SecondaryText = Color(0xFF718096)
private val BorderColor = Color(0xFFE2E8F0)
private val Orange = Color(0xFFFF762D)
private val OrangeLight = Color(0xFFFFF2E8)

// ======================================================
// MODELS
// ======================================================

enum class PermissionType {
    LATE_ARRIVAL,
    EARLY_LEAVE
}

data class PermissionRequestData(
    val type: PermissionType,
    val date: String,
    val expectedTime: String,
    val durationMinutes: Int,
    val reason: String
)

// ======================================================
// MAIN PERMISSION SCREEN
// ======================================================

@Composable
fun PermissionShortTimeScreen(
    modifier: Modifier = Modifier,
    shiftStartTime: String = "Not supplied by CRM",
    shiftEndTime: String = "Not supplied by CRM",
    onBack: () -> Unit = {},
    onSubmit: (PermissionRequestData) -> Unit = {}
) {

    val submission = com.prodigit.bodhiattendance.ui.live.liveUiState()
    val context = LocalContext.current

    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    }

    var selectedDate by remember {
        mutableStateOf(
            dateFormat.format(Calendar.getInstance().time)
        )
    }

    var permissionType by remember {
        mutableStateOf(PermissionType.LATE_ARRIVAL)
    }

    var duration by remember {
        mutableIntStateOf(30)
    }

    var reason by remember {
        mutableStateOf("")
    }

    val expectedTime =
        if (permissionType == PermissionType.LATE_ARRIVAL)
            shiftStartTime
        else
            shiftEndTime

    val datePicker = {

        val calendar = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->

                val selectedCalendar =
                    Calendar.getInstance().apply {
                        set(year, month, day)
                    }

                selectedDate = dateFormat.format(
                    selectedCalendar.time
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {

        // ==================================================
        // GRADIENT CHILD HEADER
        // ==================================================

        PermissionHeader(
            onBack = onBack
        )

        // ==================================================
        // SCROLLABLE FORM
        // ==================================================

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .mouseWheelScroll(scrollState)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 18.dp, bottom = 24.dp)
        ) {

            // ==============================================
            // POLICY NOTICE
            // ==============================================

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = OrangeLight
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFFFDCC7)
                )
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Orange),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {

                        Text(
                            "Permission Policy",
                            color = DarkText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Text(
                            "Allowed only for 30–60 minutes",
                            color = Color(0xFFC55417),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Text(
                            "• Arriving late by 30–60 minutes\n" +
                                    "• Leaving 30–60 minutes before " +
                                    "the expected closing time",
                            color = Color(0xFF526071),
                            fontSize = 12.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // ==============================================
            // REQUEST TYPE
            // ==============================================

            PermissionSectionTitle(
                title = "Request Type",
                description = "Choose your permission type"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                PermissionSelectionCard(
                    title = "Late Arrival",
                    subtitle = "Arrive late",
                    symbol = "↗",
                    selected =
                        permissionType == PermissionType.LATE_ARRIVAL,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        permissionType = PermissionType.LATE_ARRIVAL
                    }
                )

                PermissionSelectionCard(
                    title = "Early Leave",
                    subtitle = "Leave early",
                    symbol = "↘",
                    selected =
                        permissionType == PermissionType.EARLY_LEAVE,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        permissionType = PermissionType.EARLY_LEAVE
                    }
                )
            }

            // ==============================================
            // DATE
            // ==============================================

            PermissionSectionTitle(
                title = "Date",
                description = "Select permission date"
            )

            PermissionInfoField(
                value = selectedDate,
                subtitle = "Permission date",
                icon = {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                },
                onClick = datePicker
            )

            // ==============================================
            // EXPECTED TIME
            // ==============================================

            PermissionSectionTitle(
                title = "Expected Time",
                description = "Based on assigned shift"
            )

            PermissionInfoField(
                value = expectedTime,
                subtitle =
                    if (permissionType ==
                        PermissionType.LATE_ARRIVAL)
                        "Expected shift start"
                    else
                        "Expected shift end",
                icon = {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                }
            )

            // ==============================================
            // DURATION
            // ==============================================

            PermissionSectionTitle(
                title = "Duration",
                description = "30–60 minutes only"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf(30, 45, 60).forEach { minutes ->

                    DurationOption(
                        minutes = minutes,
                        selected = duration == minutes,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            duration = minutes
                        }
                    )
                }
            }

            // ==============================================
            // REASON
            // ==============================================

            PermissionSectionTitle(
                title = "Reason",
                description = "Explain your request"
            )

            OutlinedTextField(
                value = reason,
                onValueChange = {
                    if (it.length <= 200) {
                        reason = it
                    }
                },
                placeholder = {
                    Text(
                        "Enter reason for permission...",
                        fontSize = 13.sp,
                        color = SecondaryText
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                supportingText = {
                    Text(
                        "${reason.length}/200",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            )

            // ==============================================
            // APPROVAL NOTICE
            // ==============================================

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LightBlue
                )
            ) {

                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )

                    Column {

                        Text(
                            "Admin Approval Required",
                            color = DeepBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "Your request will be sent to " +
                                    "CRM for approval. You will be " +
                                    "notified after review.",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ==============================================
            // SUBMIT BUTTON
            // ==============================================

            submission.submissionMessage?.let { Text(it) }
            Button(
                enabled = !submission.submitting,
                onClick = {

                    if (reason.isBlank()) {

                        Toast.makeText(
                            context,
                            "Please enter a reason",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        onSubmit(
                            PermissionRequestData(
                                type = permissionType,
                                date = selectedDate,
                                expectedTime = expectedTime,
                                durationMinutes = duration,
                                reason = reason.trim()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 5.dp
                )
            ) {

                Icon(
                    Icons.Default.Send,
                    contentDescription = null
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    "Submit Request",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ======================================================
// GRADIENT HEADER
// ======================================================

@Composable
private fun PermissionHeader(
    onBack: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        DeepBlue,
                        PrimaryBlue,
                        Color(0xFF4097FF)
                    )
                )
            )
            .padding(
                start = 12.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = 22.dp
            )
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(5.dp))

            Column {

                Text(
                    "Permission / Short Time",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "Late arrival or early leave request",
                    color = MaterialTheme.colorScheme.surface,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ======================================================
// SECTION HEADING
// ======================================================

@Composable
private fun PermissionSectionTitle(
    title: String,
    description: String
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Text(
            description,
            fontSize = 12.sp,
            color = SecondaryText
        )
    }
}

// ======================================================
// PERMISSION TYPE CARD
// ======================================================

@Composable
private fun PermissionSelectionCard(
    title: String,
    subtitle: String,
    symbol: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = if (selected) 5.dp else 2.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) LightBlue else Color.White
        ),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) PrimaryBlue else BorderColor
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected)
                                Color(0xFFD4E6FF)
                            else
                                OrangeLight
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        symbol,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (selected) PrimaryBlue
                            else Orange
                    )
                }

                if (selected) {

                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                title,
                color = DarkText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Text(
                subtitle,
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
    }
}

// ======================================================
// DATE / TIME FIELD
// ======================================================

@Composable
private fun PermissionInfoField(
    value: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {

    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightBlue),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    value,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    fontSize = 15.sp
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    subtitle,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }

            if (onClick != null) {

                Text(
                    "›",
                    fontSize = 28.sp,
                    color = SecondaryText
                )
            }
        }
    }
}

// ======================================================
// DURATION BUTTON
// ======================================================

@Composable
private fun DurationOption(
    minutes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) LightBlue else Color.White
        ),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) PrimaryBlue else BorderColor
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 14.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "$minutes",
                color =
                    if (selected) DeepBlue else DarkText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(3.dp))

            Text(
                "Minutes",
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
    }
}

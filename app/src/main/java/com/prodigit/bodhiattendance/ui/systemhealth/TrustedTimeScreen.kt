package com.prodigit.bodhiattendance.ui.systemhealth

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

private val TimeBlue = Color(0xFF1769E8)
private val TimeText = Color(0xFF142343)
private val TimeMuted = Color(0xFF718098)
private val TimeBorder = Color(0xFFDEE8F5)

private data class LocalTimeSnapshot(
    val capturedAt: Long,
    val automaticTime: Boolean?,
    val automaticZone: Boolean?,
    val zone: String,
    val elapsedRealtime: Long
)

private fun getTimeSnapshot(context: android.content.Context): LocalTimeSnapshot {
    fun setting(name: String): Boolean? = try {
        Settings.Global.getInt(context.contentResolver, name, -1)
            .takeIf { it != -1 }?.let { it == 1 }
    } catch (_: Exception) {
        null
    }
    return LocalTimeSnapshot(
        capturedAt = System.currentTimeMillis(),
        automaticTime = setting(Settings.Global.AUTO_TIME),
        automaticZone = setting(Settings.Global.AUTO_TIME_ZONE),
        zone = TimeZone.getDefault().id,
        elapsedRealtime = SystemClock.elapsedRealtime()
    )
}

/** Read-only UI: device settings are not proof of trusted/server time. */
@Composable
fun TrustedTimeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var snapshot by remember { mutableStateOf(getTimeSnapshot(context)) }
    fun refresh() { snapshot = getTimeSnapshot(context) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openTimeSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: SecurityException) {
            // Some managed devices disallow changing system time settings.
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = TimeText)
                }
                Column(Modifier.weight(1f)) {
                    Text("Trusted Time", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TimeText)
                    Text("Device clock and time verification", fontSize = 12.sp, color = TimeMuted)
                }
            }
        }
        item {
            TimePanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = Color(0xFFEAF3FF), shape = RoundedCornerShape(16.dp)) {
                        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Schedule, null, tint = TimeBlue, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Clock verification", fontWeight = FontWeight.Bold, color = TimeText)
                        Text("Server verification pending", fontSize = 12.sp, color = Color(0xFFB27A18))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Automatic time settings can be checked locally, but a trusted clock needs a verified reference. This device has not been verified against server time here.",
                    color = TimeMuted, fontSize = 12.sp, lineHeight = 18.sp
                )
            }
        }
        item {
            TimeSection("DEVICE CLOCK", "Information read directly from this phone")
            Spacer(Modifier.height(8.dp))
            TimePanel {
                TimeDetail("Local date and time", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(snapshot.capturedAt)))
                HorizontalDivider(color = TimeBorder)
                TimeDetail("Time zone", snapshot.zone)
                HorizontalDivider(color = TimeBorder)
                TimeDetail("Automatic date & time", yesNo(snapshot.automaticTime))
                HorizontalDivider(color = TimeBorder)
                TimeDetail("Automatic time zone", yesNo(snapshot.automaticZone))
            }
        }
        item {
            TimeSection("VERIFICATION", "Trustworthy timestamp checks for attendance")
            Spacer(Modifier.height(8.dp))
            TimePanel {
                TimeDetail("Trusted server reference", "Not connected")
                HorizontalDivider(color = TimeBorder)
                TimeDetail("Clock difference", "Not measured")
                HorizontalDivider(color = TimeBorder)
                TimeDetail("Last server verification", "Never")
                HorizontalDivider(color = TimeBorder)
                TimeDetail("Monotonic clock", "Available")
            }
        }
        item {
            TimePanel(background = Color(0xFFEBF4FF)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = TimeBlue)
                    Column {
                        Text("Why this matters", fontWeight = FontWeight.Bold, color = TimeText)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Changing the phone's date or time must not allow employees to backdate attendance. When offline, records should keep their original capture time and monotonic elapsed-time evidence for later server validation.",
                            color = Color(0xFF536985), fontSize = 12.sp, lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = ::openTimeSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TimeBlue),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Date & Time Settings")
            }
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = ::refresh,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh Device Clock")
            }
        }
        item {
            Text(
                "Prototype: This page does not change system settings, certify timestamps, or create attendance records. Automatic time alone is not proof of clock accuracy.",
                fontSize = 11.sp, lineHeight = 16.sp, color = TimeMuted
            )
        }
    }
}

private fun yesNo(value: Boolean?): String = when (value) {
    true -> "On"
    false -> "Off"
    null -> "Unavailable"
}

@Composable
private fun TimePanel(
    background: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, TimeBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
private fun TimeSection(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 11.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold, color = TimeBlue)
        Text(subtitle, fontSize = 11.sp, color = TimeMuted)
    }
}

@Composable
private fun TimeDetail(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 12.sp, color = TimeMuted)
        Text(value, modifier = Modifier.weight(1.1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TimeText)
    }
}

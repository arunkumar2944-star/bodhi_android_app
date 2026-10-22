package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.material3.MaterialTheme

import android.app.ActivityManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BatteryBlue = Color(0xFF1769E8)
private val BatteryInk: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
private val BatteryMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val BatteryBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val BatteryAmber = Color(0xFFB86B0B)
private val BatteryGreen = Color(0xFF17804E)

private data class BatteryDeviceStatus(
    val dozeExempt: Boolean?,
    val backgroundRestricted: Boolean?,
    val powerSaverEnabled: Boolean?
)

private fun readBatteryStatus(context: Context): BatteryDeviceStatus {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return BatteryDeviceStatus(
        dozeExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName)
        } else null,
        backgroundRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isBackgroundRestricted
        } else false,
        powerSaverEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager?.isPowerSaveMode
        } else null
    )
}

/**
 * Read-only device guidance screen. The app does not grant its own exemptions.
 * Actual attendance monitoring remains in the attendance engine.
 * MainAppLayout supplies the global header and bottom navigation.
 */
@Composable
fun BatteryOptimizationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(readBatteryStatus(context)) }
    var settingsError by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = readBatteryStatus(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        status = readBatteryStatus(context)
    }

    fun launchSettings(intent: Intent) {
        try {
            settingsError = null
            settingsLauncher.launch(intent)
        } catch (_: Exception) {
            settingsError = "This settings page is unavailable on your device. Open Android Settings and search for Battery or App battery usage."
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = BatteryInk)
                }
                Column(Modifier.weight(1f)) {
                    Text("Battery Optimization", fontWeight = FontWeight.Bold, fontSize = 23.sp, color = BatteryInk)
                    Text("Background attendance reliability", fontSize = 12.sp, color = BatteryMuted)
                }
                IconButton(onClick = { status = readBatteryStatus(context) }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh battery status", tint = BatteryBlue)
                }
            }
        }

        item {
            BatteryPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = BatteryBlue, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Background operation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BatteryInk)
                        Text("Review Android power restrictions for reliable monitoring.", fontSize = 12.sp, lineHeight = 17.sp, color = BatteryMuted)
                    }
                }
                Spacer(Modifier.height(16.dp))
                BatteryStatusRow(
                    title = "Battery optimization exemption",
                    description = when (status.dozeExempt) {
                        true -> "Android reports this app exempt from standard Doze battery optimization."
                        false -> "Android battery optimization applies to this app."
                        null -> "Not available on this Android version or device."
                    },
                    label = when (status.dozeExempt) { true -> "Exempt"; false -> "Optimized"; null -> "Unknown" },
                    tint = when (status.dozeExempt) { true -> BatteryGreen; false -> BatteryAmber; null -> BatteryMuted }
                )
                HorizontalDivider(color = BatteryBorder)
                BatteryStatusRow(
                    title = "App background restriction",
                    description = "Android reports whether background work is restricted for this app.",
                    label = when (status.backgroundRestricted) { true -> "Restricted"; false -> "Not restricted"; null -> "Unknown" },
                    tint = if (status.backgroundRestricted == false) BatteryGreen else BatteryAmber
                )
                HorizontalDivider(color = BatteryBorder)
                BatteryStatusRow(
                    title = "System Battery Saver",
                    description = "Device-wide power saving can limit background activity.",
                    label = when (status.powerSaverEnabled) { true -> "On"; false -> "Off"; null -> "Unknown" },
                    tint = when (status.powerSaverEnabled) { true -> BatteryAmber; false -> BatteryGreen; null -> BatteryMuted }
                )
            }
        }

        item {
            BatteryPanel {
                Text("Why this matters", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BatteryInk)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bodhi Attendance relies on background location and office proximity checks configured in CRM. Android and phone manufacturers may delay scans or background execution to save battery. Battery settings alone cannot guarantee continuous detection.",
                    fontSize = 12.sp, lineHeight = 19.sp, color = BatteryMuted
                )
            }
        }

        item {
            Text("Device settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BatteryInk)
            Spacer(Modifier.height(8.dp))
            BatteryPanel {
                Button(
                    onClick = {
                        launchSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BatteryBlue)
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Battery Optimization Settings")
                }
                Spacer(Modifier.height(9.dp))
                OutlinedButton(
                    onClick = {
                        launchSettings(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, BatteryBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = BatteryBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("Open App Info / App Battery Usage", color = BatteryBlue)
                }
                Text(
                    "Settings names differ by manufacturer. If available, review background battery restrictions for this app. Android may require employee approval.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = BatteryMuted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }

        if (settingsError != null) {
            item {
                Text(settingsError.orEmpty(), color = BatteryAmber, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }

        item {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = BatteryBlue)
                    Column {
                        Text("Important limitation", color = BatteryInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Doze exemption, Battery Saver status and manufacturer-specific background restrictions are different. This page cannot detect or override every manufacturer restriction. Always use System Health to review the full attendance environment.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, BatteryBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun BatteryStatusRow(title: String, description: String, label: String, tint: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), color = BatteryInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Surface(color = tint.copy(alpha = 0.10f), shape = RoundedCornerShape(50)) {
                Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(description, color = BatteryMuted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
    }
}


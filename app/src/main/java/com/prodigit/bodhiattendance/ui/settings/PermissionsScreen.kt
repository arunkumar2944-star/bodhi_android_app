package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.material3.MaterialTheme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private val Blue = Color(0xFF1769E8)
private val Ink = MaterialTheme.colorScheme.onSurface
private val Muted = MaterialTheme.colorScheme.onSurfaceVariant
private val Stroke = MaterialTheme.colorScheme.outlineVariant

data class PermissionCheck(
    val title: String,
    val description: String,
    val granted: Boolean,
    val notRequired: Boolean = false
)

private fun hasPermission(context: Context, name: String): Boolean =
    ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED

private fun getPermissionChecks(context: Context): List<PermissionCheck> {
    val fine = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val nearbyRequired = Build.VERSION.SDK_INT >= 31
    val nearby = !nearbyRequired || (
            hasPermission(context, Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            )
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val locationEnabled = if (Build.VERSION.SDK_INT >= 28) {
        locationManager.isLocationEnabled
    } else {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothEnabled = try {
        // On Android 12+, reading adapter state may require BLUETOOTH_CONNECT.
        if (nearbyRequired && !hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) false
        else bluetoothManager?.adapter?.isEnabled == true
    } catch (_: SecurityException) { false }

    return listOf(
        PermissionCheck("Precise Location", "Fine location permission for GPS-based presence", fine),
        PermissionCheck("Nearby Devices", "Bluetooth scan and connect permissions", nearby,
            notRequired = !nearbyRequired),
        PermissionCheck("Location Service", "Android device location is turned on", locationEnabled),
        PermissionCheck("Bluetooth Service", "Bluetooth radio is turned on", bluetoothEnabled)
    )
}

@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dailyStatus by com.prodigit.bodhiattendance.core.monitoring.AttendanceDayGate.state.collectAsState()
    val offline by com.prodigit.bodhiattendance.core.monitoring.OfflineWorkSessionStore.state.collectAsState()
    val monitoring by com.prodigit.bodhiattendance.core.monitoring.AttendanceMonitorStatus.state.collectAsState()
    var checks by remember { mutableStateOf(getPermissionChecks(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checks = getPermissionChecks(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openSettings(action: String) {
        try {
            context.startActivity(Intent(action).apply {
                if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = Color.White, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink)
                }
                Column {
                    Text("Permissions & Services", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Required access for automatic attendance", fontSize = 12.sp, color = Muted)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Device access overview", color = Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "These checks reflect Android device settings. Office beacon, Wi-Fi router, " +
                                    "registration and synchronization are checked separately in System Health.",
                            color = Muted, fontSize = 12.sp, lineHeight = 18.sp
                        )
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Location privacy", fontWeight = FontWeight.Bold, color = Ink)
                        Text(if (com.prodigit.bodhiattendance.core.monitoring.AttendancePrivacyPolicy.isPaused(context))
                            "Location monitoring is paused after checkout."
                            else if (offline.offline) "Offline monitoring: events saved on phone, pending CRM validation."
                            else if (monitoring.verificationPending) "Verification pending: retrying CRM; GPS is paused."
                            else if (monitoring.running) "Attendance monitoring is running for your work session."
                            else "Attendance monitoring is not running.", color = Muted)
                        Text("Daily eligibility: " + dailyStatus.replace('_', ' '), color = Muted)
                        Text("Open the app each workday to start. CRM excludes holidays, approved leave and manager-recorded absence. Precise While using location is enough; monitoring then continues with a visible notification until checkout. There is no automatic morning restart. Saved events may still upload while tracking is paused.", color = Muted, fontSize = 12.sp)
                    }
                }
            }
            items(checks, key = { it.title }) { check ->
                PermissionStatusCard(check)
            }
            item {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Open App Settings", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Location Settings") }
                OutlinedButton(
                    onClick = { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Bluetooth Settings") }
                TextButton(
                    onClick = { checks = getPermissionChecks(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Refresh checks") }
                Text(
                    "Permissions must be granted by the employee. This page cannot silently " +
                            "enable location, Bluetooth or background access.",
                    color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(check: PermissionCheck) {
    val okay = check.granted || check.notRequired
    val status = if (check.notRequired) "Not Required" else if (check.granted) "Ready" else "Action"
    val statusColor = if (okay) Color(0xFF137D54) else Color(0xFFB94242)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Stroke)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (okay) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(if (okay) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                        contentDescription = null, tint = statusColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(check.title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 13.sp)
                Spacer(Modifier.height(3.dp))
                Text(check.description, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(5.dp))
            Text(status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

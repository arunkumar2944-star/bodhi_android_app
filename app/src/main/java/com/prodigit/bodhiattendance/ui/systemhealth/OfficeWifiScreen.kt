package com.prodigit.bodhiattendance.ui.systemhealth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private data class WifiDiagnostic(
    val enabled: Boolean?, val ssid: String?, val bssid: String?,
    val preciseLocationGranted: Boolean, val locationEnabled: Boolean
)

@Suppress("DEPRECATION")
private fun readWifiDiagnostic(context: Context): WifiDiagnostic {
    val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val location = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val locationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) location.isLocationEnabled
        else location.isProviderEnabled(LocationManager.GPS_PROVIDER) || location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    val info = try { manager.connectionInfo } catch (_: SecurityException) { null }
    return WifiDiagnostic(
        enabled = try { manager.isWifiEnabled } catch (_: SecurityException) { null },
        ssid = info?.ssid?.trim('"')?.takeUnless { it == "<unknown ssid>" || it.isBlank() },
        bssid = info?.bssid?.takeUnless { it == "02:00:00:00:00:00" || it == "00:00:00:00:00:00" },
        preciseLocationGranted = permission, locationEnabled = locationEnabled
    )
}

/** Diagnostics for the office linked by CRM to today's accepted check-in. */
@Composable
fun OfficeWifiScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val live = com.prodigit.bodhiattendance.ui.live.liveUiState()
    val day = live.attendance?.let { read -> read.days.firstOrNull { it.date == read.today } }
    val office = live.configuration?.locations?.firstOrNull { it.id == day?.officeId }
    var detected by remember(context) { mutableStateOf(readWifiDiagnostic(context)) }
    var settingsError by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        detected = readWifiDiagnostic(context)
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) detected = readWifiDiagnostic(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        detected = readWifiDiagnostic(context)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun openSettings(intent: Intent) {
        try { settingsError = null; launcher.launch(intent) }
        catch (_: Exception) { settingsError = "Open Android Settings and search for Wi-Fi or this app's permissions." }
    }
    val health = officeWifiHealth(office?.wifiRequired, office?.wifi.orEmpty().map { it.bssid }, detected.bssid, detected.enabled)
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Office Wi-Fi Router", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Connection and router verification for your checked-in office.")
        }
        item {
            WifiPanel {
                WifiRow("Checked-in office", day?.officeName ?: office?.locationName ?: "Not available")
                WifiRow("Wi-Fi requirement", when (office?.wifiRequired) { true -> "Required"; false -> "Not required"; null -> "Not available" })
                Text(health.message, color = when (health.status) {
                    HealthStatus.NOT_READY -> MaterialTheme.colorScheme.error
                    HealthStatus.READY -> androidx.compose.ui.graphics.Color(0xFF17804E)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                })
                if (office == null) Text("The checked-in office or its CRM configuration is unavailable. Refresh System Health to reload it.")
            }
        }
        item {
            WifiPanel {
                Text("Current connection", fontWeight = FontWeight.Bold)
                WifiRow("Wi-Fi", when (detected.enabled) { true -> "On"; false -> "Off"; null -> "Unknown" })
                WifiRow("Network name", detected.ssid ?: "Unavailable")
                WifiRow("Router BSSID", detected.bssid ?: "Unavailable")
                WifiRow("Precise location permission", if (detected.preciseLocationGranted) "Granted" else "Required to read router identity")
                WifiRow("Location services", if (detected.locationEnabled) "On" else "Off")
                Text("Android may hide router identity without location permission or location services. An unavailable identity is not a confirmed router mismatch.")
            }
        }
        if (office != null) item {
            WifiPanel {
                Text("Approved routers — ${office.locationName}", fontWeight = FontWeight.Bold)
                if (office.wifi.isEmpty()) Text("No approved routers configured for this office.")
                office.wifi.forEach { router ->
                    WifiRow(router.name, router.ssid ?: "Network name not supplied")
                    WifiRow("BSSID", router.bssid)
                    HorizontalDivider()
                }
            }
        }
        item {
            Button(onClick = { openSettings(Intent(Settings.ACTION_WIFI_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Android Wi-Fi Settings")
            }
            OutlinedButton(onClick = {
                openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Open App Permissions") }
            TextButton(onClick = { detected = readWifiDiagnostic(context) }) { Text("Refresh connection") }
            settingsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun WifiPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun WifiRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

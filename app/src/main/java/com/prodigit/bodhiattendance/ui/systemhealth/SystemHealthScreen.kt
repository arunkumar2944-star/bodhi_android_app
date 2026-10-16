package com.prodigit.bodhiattendance.ui.systemhealth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prodigit.bodhiattendance.ui.systemhealth.components.SystemHealthCard
import com.prodigit.bodhiattendance.ui.systemhealth.components.SystemHealthGroupCard
import com.prodigit.bodhiattendance.ui.systemhealth.components.SystemHealthSummaryCard

/**
 * Read-only diagnostic view. SystemHealthViewModel supplies the health checks.
 * No event is recorded, no attendance status changes, no inferred readiness.
 * The global header and bottom navigation are already in MainAppLayout.
 */
@Composable
fun SystemHealthScreen(
    viewModel: SystemHealthViewModel,
    onBack: () -> Unit = {},
    onRegisteredDeviceClick: () -> Unit = {},
    onPermissionsClick: () -> Unit = {},
    onBatteryOptimizationClick: () -> Unit = {},
    onTrustedTimeClick: () -> Unit = {},
    onAttendanceConfigurationClick: () -> Unit = {},
    onOfficeBeaconClick: () -> Unit = {},
    onOfficeWifiClick: () -> Unit = {}
) {
    val diagnostics by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val liveModel = com.prodigit.bodhiattendance.ui.live.liveUiViewModel()
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner, viewModel, liveModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHealth()
                scope.launch { liveModel.refresh() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshHealth()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val live = com.prodigit.bodhiattendance.ui.live.liveUiState()
    val day = live.attendance?.let { read -> read.days.firstOrNull { it.date == read.today } }
    val office = live.configuration?.locations?.firstOrNull { it.id == day?.officeId }
    val uiState = diagnostics.copy(
        attendanceConfiguration = HealthItemState(
            status = if (live.configuration == null || live.configuration.locations.isEmpty()) HealthStatus.NOT_CONFIGURED else HealthStatus.READY,
            title = "Attendance Configuration", message = live.configuration?.let { "${it.locations.size} CRM offices loaded" } ?: "Configuration unavailable"),
        officeWifiRequirement = office?.wifiRequired,
        officeBleRequirement = office?.bleRequired,
        officeWifi = officeWifiHealth(office?.wifiRequired, office?.wifi.orEmpty().map { it.bssid }, diagnostics.detectedBssid, diagnostics.wifiEnabled),
        officeBeacon = officeBeaconHealth(office?.bleRequired),
        bluetoothPermission = when (office?.bleRequired) {
            true -> diagnostics.bluetoothPermission
            false -> HealthItemState(HealthStatus.NOT_REQUIRED, "Bluetooth Permission", "Not required by this CRM office policy")
            null -> HealthItemState(HealthStatus.NOT_CONFIGURED, "Bluetooth Permission", "Current office policy unavailable")
        },
        bluetoothService = when (office?.bleRequired) {
            true -> diagnostics.bluetoothService
            false -> HealthItemState(HealthStatus.NOT_REQUIRED, "Bluetooth", "Not required by this CRM office policy")
            null -> HealthItemState(HealthStatus.NOT_CONFIGURED, "Bluetooth", "Current office policy unavailable")
        }
    )
    val blue = Color(0xFF1769E8)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("System Health", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF142343))
                    Text("Live device and attendance diagnostics", fontSize = 12.sp, color = Color(0xFF728099))
                }
                TextButton(onClick = onBack) { Text("Back") }
            }
        }

        item { SystemHealthSummaryCard(uiState = uiState) }
        item {
            HealthExplanation(
                title = "How to read this page",
                message = "Ready means a specific check passed; Action means it needs attention; Pending can mean configuration or verification is unavailable. The percentage summarizes checks — it does not guarantee an attendance event."
            )
        }

        item {
            HealthTitle("DEVICE & SECURITY", "Identity, trusted clock, background operation")
            Spacer(Modifier.height(8.dp))
            SystemHealthGroupCard {
                // Open the same read-only device page used by Settings.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRegisteredDeviceClick)
                ) {
                    SystemHealthCard(
                        item = uiState.registeredDevice,
                        icon = Icons.Outlined.PhoneAndroid,
                        showDivider = true
                    )
                }
                Box(Modifier.fillMaxWidth().clickable(onClick = onTrustedTimeClick)) {
                    SystemHealthCard(uiState.trustedTime, Icons.Outlined.VerifiedUser, showDivider = true)
                }
                Box(Modifier.fillMaxWidth().clickable(onClick = onBatteryOptimizationClick)) {
                    SystemHealthCard(uiState.batteryOptimization, Icons.Outlined.BatterySaver, displayTitle = "Background Operation", showDivider = false)
                }
            }
        }

        item {
            HealthTitle("DEVICE SERVICES", "GPS and optional device services; office requirements come from CRM")
            Spacer(Modifier.height(8.dp))
            SystemHealthGroupCard {
                Box(Modifier.fillMaxWidth().clickable(onClick = onPermissionsClick)) {
                    SystemHealthCard(uiState.locationPermission, Icons.Outlined.LocationOn, showDivider = true)
                }
                Box(Modifier.fillMaxWidth().clickable(onClick = onPermissionsClick)) {
                    SystemHealthCard(uiState.backgroundLocationPermission, Icons.Outlined.MyLocation, showDivider = true)
                }
                SystemHealthCard(uiState.gpsIntegrity, Icons.Outlined.GpsFixed, showDivider = true)
                Box(Modifier.fillMaxWidth().clickable(onClick = onPermissionsClick)) {
                    SystemHealthCard(uiState.locationService, Icons.Outlined.GpsFixed, showDivider = true)
                }
                Box(Modifier.fillMaxWidth().clickable(onClick = onPermissionsClick)) {
                    SystemHealthCard(uiState.bluetoothPermission, Icons.Outlined.Bluetooth, showDivider = true)
                }
                Box(Modifier.fillMaxWidth().clickable(onClick = onPermissionsClick)) {
                    SystemHealthCard(uiState.bluetoothService, Icons.Outlined.WifiTethering, showDivider = true)
                }
                SystemHealthCard(uiState.network, Icons.Outlined.Wifi, displayTitle = "Internet Connection", showDivider = false)
            }
        }

        item {
            HealthTitle("ATTENDANCE ENVIRONMENT", "Office requirements are supplied by CRM")
            Spacer(Modifier.height(8.dp))
            SystemHealthGroupCard {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOfficeBeaconClick)
                ) {
                    SystemHealthCard(
                        item = uiState.officeBeacon,
                        icon = Icons.Outlined.WifiTethering,
                        showDivider = true
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOfficeWifiClick)
                ) {
                    SystemHealthCard(
                        item = uiState.officeWifi,
                        icon = Icons.Outlined.Router,
                        displayTitle = "Office Wi-Fi Router",
                        showDivider = false
                    )
                }

            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Wi-Fi and BLE requirements are read from the active CRM office policy. Unknown policy or sensor evidence is not treated as verified.",
                fontSize = 11.sp, lineHeight = 16.sp, color = Color(0xFF75839B)
            )
        }

        item {
            HealthTitle("DATA & SYNC", "Local event persistence and synchronization")
            Spacer(Modifier.height(8.dp))
            SystemHealthGroupCard {
                SystemHealthCard(uiState.offlineStorage, Icons.Outlined.Inventory2, showDivider = true)
                SystemHealthCard(uiState.sync, Icons.Outlined.CloudSync, showDivider = false)
            }
        }

        item {
            Button(
                onClick = { if (!uiState.isRefreshing) { viewModel.refreshHealth(); scope.launch { liveModel.refresh() } } },
                enabled = !uiState.isRefreshing,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = blue)
            ) {
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Checking System...")
                } else {
                    Icon(Icons.Outlined.Security, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh System Health", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Checks use local device information and any currently available attendance configuration. Missing configuration must not be displayed as verified.",
                color = Color(0xFF74829A), fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun HealthTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 11.sp, letterSpacing = .6.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1769E8))
        Text(description, fontSize = 11.sp, color = Color(0xFF718098))
    }
}

@Composable
private fun HealthExplanation(title: String, message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF4FF)),
        border = BorderStroke(1.dp, Color(0xFFD5E6FC)),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFF1769E8))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF142343))
                Spacer(Modifier.height(4.dp))
                Text(message, fontSize = 11.sp, lineHeight = 17.sp, color = Color(0xFF536985))
            }
        }
    }
}



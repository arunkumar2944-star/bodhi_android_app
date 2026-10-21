package com.prodigit.bodhiattendance.ui.systemhealth

import androidx.compose.material3.MaterialTheme

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PolicyBlue = Color(0xFF1769E8)
private val PolicyInk = MaterialTheme.colorScheme.onSurface
private val PolicyMuted = MaterialTheme.colorScheme.onSurfaceVariant
private val PolicyBorder = MaterialTheme.colorScheme.outlineVariant

/** Displays CRM configuration verbatim; never changes attendance policy. */
@Composable
fun AttendanceConfigurationScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val live = com.prodigit.bodhiattendance.ui.live.liveUiState()
    val configuration = live.configuration
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Attendance Configuration", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = PolicyInk)
            Text("Active CRM offices; this list does not imply employee assignment", color = PolicyMuted)
        }
        if (configuration == null) item { com.prodigit.bodhiattendance.ui.live.DataUnavailable("Configuration", live.errors["configuration"] ?: live.errors["all"], live.loading) }
        else {
            item { Text("Configuration version: ${configuration.configurationVersion ?: "Not supplied"}") }
            if (configuration.locations.isEmpty()) item { Text("CRM returned no active offices") }
            configuration.locations.forEach { location ->
                item(key = location.id) {
                    PolicyPanel {
                        Text(location.locationName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PolicyInk)
                        PolicyRow(Icons.Outlined.LocationOn, "Coordinates", "${location.latitude}, ${location.longitude}")
                        PolicyRow(Icons.Outlined.GpsFixed, "Geofence radius", "${location.geofenceRadiusMeters} m")
                        PolicyRow(Icons.Outlined.GpsFixed, "Maximum GPS accuracy", "${location.maxAccuracyMeters} m")
                        PolicyRow(Icons.Outlined.Router, "Wi-Fi required", requirementText(location.wifiRequired))
                        PolicyRow(Icons.Outlined.Bluetooth, "BLE required", requirementText(location.bleRequired))
                        PolicyRow(Icons.Outlined.Schedule, "Absence grace", location.gracePeriodMinutes?.let { "$it min" } ?: "Not supplied")
                        PolicyRow(Icons.Outlined.Schedule, "Maximum absence", location.maximumAbsenceMinutes?.let { "$it min" } ?: "Not supplied")
                        location.wifi.forEach { wifi -> PolicyRow(Icons.Outlined.Router, wifi.name, wifi.bssid) }
                        location.beacons.forEach { beacon -> PolicyRow(Icons.Outlined.Bluetooth, beacon.name, beacon.identifier) }
                    }
                }
            }
        }
    }
}
private fun requirementText(value: Boolean?): String = when(value) { true -> "Required"; false -> "Not required"; null -> "Not supplied by CRM" }

@Composable
private fun PolicyPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PolicyBorder)
    ) { Column(Modifier.fillMaxWidth().padding(15.dp), content = content) }
}

@Composable
private fun PolicyTitle(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp, color = PolicyBlue)
}

@Composable
private fun PolicyRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PolicyBlue, modifier = Modifier.size(19.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 12.sp, color = PolicyMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PolicyInk)
    }
}

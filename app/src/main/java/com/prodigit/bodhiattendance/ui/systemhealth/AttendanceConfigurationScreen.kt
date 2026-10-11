package com.prodigit.bodhiattendance.ui.systemhealth

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
private val PolicyInk = Color(0xFF142343)
private val PolicyMuted = Color(0xFF718098)
private val PolicyBorder = Color(0xFFDEE8F5)

/** Office-specific presence requirements, supplied by configuration rather than inferred from sensors. */
enum class AttendanceOfficePolicy(val label: String, val wifiRequired: Boolean) {
    MAIN_OFFICE("Main Office", true),
    KNOWLEDGE_CENTRE("Knowledge Centre", false)
}

/** A null policy means the user's assigned-office policy has not been loaded. */
data class OfficeAttendanceConfig(
    val policy: AttendanceOfficePolicy? = null,
    val geofenceRadiusMeters: Int? = null,
    val officeRouterId: String? = null,
    val beaconConfigured: Boolean? = null,
    val lastUpdated: String? = null
)

/**
 * Read-only prototype: never modifies actual assigned office or attendance events.
 * For an MD demo, select demoPolicy explicitly; do not use it to calculate real health readiness.
 */
@Composable
fun AttendanceConfigurationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    config: OfficeAttendanceConfig = OfficeAttendanceConfig(),
    demoPolicy: AttendanceOfficePolicy? = null
) {
    val policy = config.policy ?: demoPolicy
    val demo = config.policy == null && demoPolicy != null
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 48.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text("Attendance Configuration", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = PolicyInk)
                    Text("Office-specific verification requirements", color = PolicyMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            PolicyPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Business, contentDescription = null, tint = PolicyBlue, modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(policy?.label ?: "Assigned office not loaded", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PolicyInk)
                        Text(
                            when {
                                demo -> "PROTOTYPE PREVIEW — not employee assignment"
                                policy == null -> "Pending configuration"
                                else -> "Assigned office policy"
                            },
                            fontSize = 11.sp, color = PolicyMuted
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("This screen displays requirements, not evidence that an employee is present.", fontSize = 12.sp, color = PolicyMuted)
            }
        }
        item {
            PolicyTitle("PRESENCE REQUIREMENTS")
            Spacer(Modifier.height(8.dp))
            PolicyPanel {
                PolicyRow(Icons.Outlined.GpsFixed, "GPS / Geofence", if (policy == null) "Policy not loaded" else "Required")
                HorizontalDivider(color = PolicyBorder)
                PolicyRow(Icons.Outlined.Router, "Office Wi-Fi", when (policy) {
                    AttendanceOfficePolicy.MAIN_OFFICE -> "Required"
                    AttendanceOfficePolicy.KNOWLEDGE_CENTRE -> "Not Required"
                    null -> "Policy not loaded"
                })
                HorizontalDivider(color = PolicyBorder)
                PolicyRow(Icons.Outlined.Bluetooth, "Office BLE Beacon", "Optional")
            }
        }
        item {
            PolicyTitle("CONFIGURATION DETAILS")
            Spacer(Modifier.height(8.dp))
            PolicyPanel {
                PolicyRow(Icons.Outlined.LocationOn, "Geofence radius", config.geofenceRadiusMeters?.let { "$it m" } ?: "Not loaded")
                HorizontalDivider(color = PolicyBorder)
                PolicyRow(Icons.Outlined.Router, "Configured router", when {
                    policy == AttendanceOfficePolicy.KNOWLEDGE_CENTRE -> "Not Required"
                    else -> config.officeRouterId ?: "Not loaded"
                })
                HorizontalDivider(color = PolicyBorder)
                PolicyRow(Icons.Outlined.Bluetooth, "Beacon installed", when (config.beaconConfigured) {
                    true -> "Yes (optional)"
                    false -> "No (optional)"
                    null -> "Unknown (optional)"
                })
                HorizontalDivider(color = PolicyBorder)
                PolicyRow(Icons.Outlined.Update, "Last updated", config.lastUpdated ?: "Not available")
            }
        }
        item {
            Surface(
                color = Color(0xFFEAF3FF),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFFD5E6FC))
            ) {
                Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = PolicyBlue)
                    Column {
                        Text("How this policy works", fontWeight = FontWeight.Bold, color = PolicyInk)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            when (policy) {
                                AttendanceOfficePolicy.MAIN_OFFICE -> "GPS and the configured office Wi-Fi router are both required signals. Wi-Fi enabled alone is not enough."
                                AttendanceOfficePolicy.KNOWLEDGE_CENTRE -> "GPS is the required signal; office Wi-Fi is not required. GPS alone cannot reliably prove presence inside a small room."
                                null -> "Assigned-office configuration has not been loaded. The app must not assume a GPS-only or Wi-Fi policy."
                            },
                            fontSize = 12.sp, lineHeight = 18.sp, color = Color(0xFF536985)
                        )
                    }
                }
            }
        }
        item {
            Text(
                "BLE does not block attendance readiness under either office policy. Optional and Not Required checks must be excluded from the readiness denominator. Actual attendance events are validated by the attendance engine, not this screen.",
                color = PolicyMuted, fontSize = 11.sp, lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun PolicyPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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

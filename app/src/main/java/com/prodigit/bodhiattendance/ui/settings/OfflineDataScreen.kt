package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import com.prodigit.bodhiattendance.ui.live.*

@Composable
fun OfflineDataScreen(onBack: () -> Unit) {
    val live = liveUiState()
    val model = liveUiViewModel()
    val rows = live.localEvents
    val context = LocalContext.current.applicationContext
    val session = com.prodigit.bodhiattendance.core.session.SessionManager(context)
    val user = session.getUserId()
    val device = com.prodigit.bodhiattendance.core.device.DeviceIdManager(context).getDeviceId().orEmpty()
    val provisionalFlow = remember(user, device) {
        com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase.getInstance(context)
            .provisionalArrivalDao().observe(user, device)
    }
    val provisional by provisionalFlow.collectAsState(initial = emptyList())
    val offline by com.prodigit.bodhiattendance.core.monitoring.OfflineWorkSessionStore.state.collectAsState()
    PrototypePage("Offline Data", "Actual Room attendance records for this installation", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.Storage, "Offline work session", "Authorization and pending attendance", ProtoPurple)
            Text(offline.message)
            offline.authorizedUntilMs?.let { PrototypeDetail("Authorized until", displayTime(it)) }
            PrototypeDetail("Mode", if (offline.offline) "Offline — pending CRM validation" else "Online / awaiting authorization")
            PrototypeDetail("Offline events pending", rows?.count { it.offlinePermit != null && it.syncStatus != "SYNCED" && it.syncStatus != "REJECTED" }?.toString() ?: "Loading")
            Text("Start each workday online. A validated session can continue offline for up to 12 hours, ending earlier at midnight IST or checkout. Unknown GPS never marks attendance.")
        }
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.Storage, "Arrivals awaiting review", "GPS evidence when office Wi-Fi is unavailable", ProtoPurple)
            PrototypeDetail("Saved arrivals", provisional.size.toString())
            Text("These records do not confirm check-in. The original server-anchored time is retained and uploaded for manager review. Wi-Fi loss does not prove a power cut.")
        }
        provisional.take(30).forEach { arrival ->
            PrototypePanel {
                PrototypeDetail("GPS-only arrival", displayTime(arrival.trustedTimeMs))
                PrototypeDetail("Office", arrival.officeId.toString())
                PrototypeDetail("Status", when (arrival.status) {
                    "SAVED_LOCALLY" -> "Saved on phone — upload pending"
                    "UPLOADED" -> "Uploaded — see Requests for decision"
                    else -> "Manual regularization needed"
                })
                arrival.message?.let { Text(it) }
                arrival.requestId?.let { PrototypeDetail("Request ID", it) }
            }
        }
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.Storage, "Offline event storage", "Saved locally before upload", ProtoPurple)
            PrototypeDetail("Stored records", rows?.size?.toString() ?: "Loading")
            PrototypeDetail("Waiting for upload", rows?.count { it.syncStatus in setOf("PENDING", "SYNCING", "FAILED_RETRYABLE") }?.toString() ?: "Loading")
            PrototypeDetail("Rejected records", rows?.count { it.syncStatus == "REJECTED" }?.toString() ?: "Loading")
        }
        rows?.filter { it.syncStatus != "SYNCED" }?.take(100)?.forEach { event ->
            PrototypePanel {
                PrototypeDetail(event.eventType, event.syncStatus)
                Text(displayTime(event.eventTime))
                event.syncError?.let { Text(it) }
            }
        }
        TextButton(onClick = { model.clearDisplayData() }) { Text("Clear display cache and reload CRM") }
        PrototypeInfo("Reloading clears screen data only. Attendance records, pending uploads, trusted time and device registration are retained.")
    }
}

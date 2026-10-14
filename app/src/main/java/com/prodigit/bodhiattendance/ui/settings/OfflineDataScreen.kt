package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.runtime.Composable
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
    PrototypePage("Offline Data", "Actual Room attendance records for this installation", onBack) {
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

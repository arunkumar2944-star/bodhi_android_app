package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import com.prodigit.bodhiattendance.ui.live.*

@Composable
fun SyncStatusScreen(onBack: () -> Unit) {
    val rows = liveUiState().localEvents
    PrototypePage("Sync Status", "Room upload status", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.CloudSync, "Sync overview", "Based on actual server acknowledgments", ProtoGreen)
            PrototypeDetail("Last successful upload", rows?.mapNotNull { it.syncedAt }?.maxOrNull()?.let(::displayTime) ?: "No acknowledgment recorded")
            PrototypeDetail("Queued / uploading", rows?.count { it.syncStatus in setOf("PENDING", "SYNCING", "FAILED_RETRYABLE") }?.toString() ?: "Loading")
            PrototypeDetail("Retryable failures", rows?.count { it.syncStatus == "FAILED_RETRYABLE" }?.toString() ?: "Loading")
            PrototypeDetail("Rejected", rows?.count { it.syncStatus == "REJECTED" }?.toString() ?: "Loading")
            PrototypeDetail("Acknowledged", rows?.count { it.syncStatus == "SYNCED" }?.toString() ?: "Loading")
        }
        PrototypeInfo("Only CRM acknowledgment marks an event SYNCED. Rejected events remain in the local audit history.")
    }
}

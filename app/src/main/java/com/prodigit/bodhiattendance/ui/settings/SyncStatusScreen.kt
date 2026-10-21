package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.prodigit.bodhiattendance.core.monitoring.AttendanceBackgroundSync
import com.prodigit.bodhiattendance.ui.live.*

/**
 * Read-only Room status plus a network-constrained retry request.
 * Scheduling never marks an event as synchronized or creates attendance.
 */
@Composable
fun SyncStatusScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val live = liveUiState()
    val model = liveUiViewModel()
    val scope = rememberCoroutineScope()
    val rows = live.localEvents
    var retryMessage by remember { mutableStateOf<String?>(null) }
    val queued = rows?.count { it.syncStatus in setOf("PENDING", "SYNCING", "FAILED_RETRYABLE") } ?: 0
    PrototypePage("Sync Status", "Attendance upload and CRM acknowledgments", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.CloudSync, "Sync overview", "Based on actual Room state and server acknowledgments", ProtoGreen)
            PrototypeDetail("Last successful upload", rows?.mapNotNull { it.syncedAt }?.maxOrNull()?.let(::displayTime) ?: "No acknowledgment recorded")
            PrototypeDetail("Queued / uploading", rows?.count { it.syncStatus in setOf("PENDING", "SYNCING", "FAILED_RETRYABLE") }?.toString() ?: "Loading")
            PrototypeDetail("Retryable failures", rows?.count { it.syncStatus == "FAILED_RETRYABLE" }?.toString() ?: "Loading")
            PrototypeDetail("Rejected — needs review", rows?.count { it.syncStatus == "REJECTED" }?.toString() ?: "Loading")
            PrototypeDetail("Acknowledged by CRM", rows?.count { it.syncStatus == "SYNCED" }?.toString() ?: "Loading")
        }
        if (rows != null && queued == 0) PrototypeInfo("No attendance uploads are currently queued. Rejected records, if any, still require review.")
        PrototypeAction("Request background upload retry", onClick = {
            runCatching { AttendanceBackgroundSync.schedule(context) }
                .onSuccess { retryMessage = "Network-constrained upload requested. This does not confirm delivery; wait for CRM acknowledgment." }
                .onFailure { retryMessage = "Could not schedule the upload: ${it.message ?: "unknown error"}" }
        }, enabled = rows != null && queued > 0)
        retryMessage?.let { Text(it) }
        PrototypeAction("Refresh displayed status", onClick = { scope.launch { model.refresh() } })
        PrototypeInfo("Only CRM acknowledgment marks an event SYNCED. Background uploads depend on network, Android scheduling, and current device authorization. Rejected records remain in local audit history.")
    }
}

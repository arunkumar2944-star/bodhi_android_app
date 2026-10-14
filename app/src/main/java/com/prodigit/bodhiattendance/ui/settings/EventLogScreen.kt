package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import com.prodigit.bodhiattendance.ui.live.*

@Composable
fun EventLogScreen(onBack: () -> Unit) {
    val rows = liveUiState().localEvents
    PrototypePage("Event Log", "Actual attendance events saved on this device", onBack) {
        if (rows == null) Text("Loading local events…")
        else if (rows.isEmpty()) Text("No local attendance events recorded")
        rows?.take(100)?.forEach { event ->
            PrototypePanel {
                PrototypeDetail(event.eventType, event.syncStatus)
                PrototypeDetail("Event time", java.time.Instant.ofEpochMilli(event.eventTime).toString())
                PrototypeDetail("UUID", event.eventUuid)
                PrototypeDetail("Upload attempts", event.syncAttempts.toString())
                event.syncError?.let { Text(it) }
            }
        }
    }
}

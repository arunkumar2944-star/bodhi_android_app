package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.core.session.SessionManager
import androidx.compose.material3.Text
import com.prodigit.bodhiattendance.ui.live.*

@Composable
fun EventLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val userId = SessionManager(context).getUserId()
    val flow = remember(userId) { AttendanceDatabase.getInstance(context).attendanceDiagnosticDao().recent(userId) }
    val diagnostics by flow.collectAsState(initial = emptyList())
    val rows = liveUiState().localEvents
    PrototypePage("Event Log", "Actual attendance events saved on this device", onBack) {
        Text("Diagnostics (phone time; attendance time remains trusted server time)")
        diagnostics.forEach { entry ->
            PrototypePanel {
                PrototypeDetail(entry.category, if (entry.uploaded) "Uploaded" else "Pending upload")
                PrototypeDetail("Phone time", java.time.Instant.parse(entry.deviceTime).atZone(java.time.ZoneId.of("Asia/Kolkata")).toString())
                Text(entry.message)
                PrototypeDetail("Boot / elapsed ms", "${entry.bootId} / ${entry.observedElapsedMs}")
            }
        }
        Text("Attendance events")
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

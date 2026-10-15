package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.ui.live.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EventLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val rows = liveUiState().localEvents
    var times by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(rows) {
        val dao = AttendanceDatabase.getInstance(context).timeAnchorDao()
        times = rows.orEmpty().mapNotNull { event ->
            val time = event.effectiveTimeEpochMs ?: event.timeAnchorId?.let { id ->
                val anchor = dao.getTimeAnchorByUuid(id)
                val elapsed = event.eventElapsedRealtimeMs
                if (anchor != null && elapsed != null && elapsed >= anchor.elapsedRealtimeMs)
                    runCatching { Instant.parse(anchor.serverTime).plusMillis(elapsed - anchor.elapsedRealtimeMs).toEpochMilli() }.getOrNull()
                else null
            }
            time?.let { event.eventUuid to it }
        }.toMap()
    }
    val labels = mapOf("CHECK_IN" to "Checked in", "BREAK_START" to "Break started", "BREAK_END" to "Break ended", "CHECK_OUT" to "Checked out")
    val reasons = mapOf("CHECK_IN" to "Office presence confirmed", "BREAK_START" to "Outside office after the confirmation period", "BREAK_END" to "Office presence restored", "CHECK_OUT" to "Office departure confirmed")
    val format = remember { DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a").withZone(ZoneId.of("Asia/Kolkata")) }
    PrototypePage("Event Log", "Your attendance timeline · India time", onBack) {
        if (rows == null) Text("Loading attendance events…")
        else if (rows.isEmpty()) Text("No attendance events recorded")
        rows?.filter { it.eventType in labels }?.take(100)?.forEach { event ->
            PrototypePanel {
                Text(labels[event.eventType].orEmpty())
                Text(times[event.eventUuid]?.let { format.format(Instant.ofEpochMilli(it)) } ?: "Time verification pending")
                Text(event.reason ?: reasons[event.eventType].orEmpty())
                if (event.syncStatus == "REJECTED") Text("Not accepted — attendance review required")
                else if (event.syncStatus != "SYNCED") Text("Waiting to sync")
            }
        }
    }
}

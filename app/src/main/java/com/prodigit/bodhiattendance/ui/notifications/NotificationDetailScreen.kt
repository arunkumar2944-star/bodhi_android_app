package com.prodigit.bodhiattendance.ui.notifications

import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.prodigit.bodhiattendance.data.remote.MobileNotification
import com.prodigit.bodhiattendance.ui.live.*
import kotlinx.coroutines.CancellationException

@Composable
fun NotificationDetailScreen(id: String, onBack: () -> Unit) {
    val model = liveUiViewModel()
    var detail by remember(id) { mutableStateOf<MobileNotification?>(null) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var attempt by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id, attempt) {
        error = null
        try { detail = model.openNotification(id) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Notification unavailable" }
    }
    PrototypePage("Notification details", "Request decision", onBack) {
        if (error != null) {
            Text(error.orEmpty())
            Button(onClick = { attempt++ }) { Text("Retry") }
        } else if (detail == null) CircularProgressIndicator()
        else detail?.let { row ->
            PrototypePanel {
                Text(row.title, style = MaterialTheme.typography.titleLarge)
                row.requestDate?.let { PrototypeDetail("Request date", it) }
                row.requestDescription?.takeIf { it.isNotBlank() }?.let { PrototypeDetail("Your request", it) }
                PrototypeDetail("Decision", row.status.orEmpty())
                PrototypeDetail("Remarks", row.description.ifBlank { "No additional remarks" })
                row.reviewedBy?.takeIf { it.isNotBlank() }?.let { PrototypeDetail("Reviewed by", it) }
                row.time?.let { PrototypeDetail("Decision time", it) }
            }
        }
    }
}

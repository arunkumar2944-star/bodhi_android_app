package com.prodigit.bodhiattendance.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prodigit.bodhiattendance.ui.live.*

@Composable
fun NotificationsScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}, onNotificationClick: (String) -> Unit = {}) {
    val live = liveUiState()
    var showRead by remember { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showRead, onClick = { showRead = false }, label = { Text("Unread (${live.notificationUnreadCount})") })
                FilterChip(selected = showRead, onClick = { showRead = true }, label = { Text("Read") })
            }
        }
        val rows = live.notifications?.filter { if (showRead) it.readAt != null else it.readAt == null }
        if (rows == null) item { DataUnavailable("Notifications", live.errors["notifications"] ?: live.errors["all"], live.loading) }
        else if (rows.isEmpty()) item { Text(if (showRead) "No read notifications" else "You're all caught up") }
        else items(rows, key = { it.id }) { notification ->
            Card(onClick = { onNotificationClick(notification.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(notification.title, style = MaterialTheme.typography.titleMedium)
                    Text(notification.description.ifBlank { "View request decision" })
                    notification.time?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text("View details", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

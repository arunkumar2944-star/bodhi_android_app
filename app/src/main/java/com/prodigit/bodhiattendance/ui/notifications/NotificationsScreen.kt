package com.prodigit.bodhiattendance.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prodigit.bodhiattendance.ui.live.*

@Composable
fun NotificationsScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    val live = liveUiState()
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            Text("CRM request decisions")
        }
        val rows = live.notifications
        if (rows == null) item { DataUnavailable("Notifications", live.errors["notifications"] ?: live.errors["all"], live.loading) }
        else if (rows.isEmpty()) item { Text("No request decisions available") }
        else items(rows, key = { it.id }) { notification ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(notification.title, style = MaterialTheme.typography.titleMedium)
                    Text(notification.description)
                    notification.time?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

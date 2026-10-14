package com.prodigit.bodhiattendance.ui.live

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DataUnavailable(title: String, error: String?, loading: Boolean) {
    val model = liveUiViewModel()
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (loading) CircularProgressIndicator()
        Text(error ?: if (loading) "Loading from CRM…" else "Data unavailable")
        TextButton(onClick = { model.clearDisplayData() }) { Text("Refresh from CRM") }
    }
}

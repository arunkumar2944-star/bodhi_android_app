package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Standalone Settings child screen (MD prototype; no CRM API integration).

@Composable
fun SyncStatusScreen(onBack: () -> Unit) {
    PrototypePage("Sync Status", "Attendance upload monitoring", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.CloudSync, "Sync overview", "Automatic device-to-CRM synchronization", ProtoGreen)
            PrototypeDetail("Connection to sync service", "Not checked")
            PrototypeDetail("Last successful sync", "Not available")
            PrototypeDetail("Queued uploads", "Not loaded")
            PrototypeDetail("Failed uploads", "Not loaded")
        }
        Spacer(Modifier.height(15.dp))
        PrototypePanel {
            Text("How synchronization works", color = ProtoNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            PrototypeDetail("1", "Events validated locally")
            PrototypeDetail("2", "Events saved to Room")
            PrototypeDetail("3", "Automatic retry on connectivity")
            PrototypeDetail("4", "CRM acknowledges final state")
        }
        Spacer(Modifier.height(15.dp))
        PrototypeInfo("PROTOTYPE: No upload is started here. Never mark a record as synced until the server acknowledges it. Automatic retry belongs to the sync worker, not this UI.")
    }
}

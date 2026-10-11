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
fun OfflineDataScreen(onBack: () -> Unit) {
    PrototypePage("Offline Data", "Device queue and saved attendance events", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.Storage, "Offline event storage", "Room database integration is planned", ProtoPurple)
            PrototypeDetail("Pending uploads", "Not connected")
            PrototypeDetail("Stored records", "Not loaded")
            PrototypeDetail("Failed events", "Not loaded")
        }
        Spacer(Modifier.height(15.dp))
        PrototypePanel {
            Text("Event queue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ProtoNavy)
            Text("No records loaded in prototype mode", fontSize = 13.sp, color = ProtoMuted)
            Text("Once connected, this page will show timestamp, event type, sync state, and retry details without editing attendance records.", fontSize = 12.sp, lineHeight = 18.sp, color = ProtoMuted)
        }
        Spacer(Modifier.height(15.dp))
        PrototypeInfo("The UI intentionally does not show '0 Pending' or 'All Synced' without querying the actual local Room queue. Offline attendance sync remains automatic in production.")
    }
}

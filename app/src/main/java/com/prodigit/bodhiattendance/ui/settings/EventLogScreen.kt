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
fun EventLogScreen(onBack: () -> Unit) {
    PrototypePage("Event Log", "Read-only attendance and device event history", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.ReceiptLong, "System event history", "GPS, BLE, attendance engine, sync and security", ProtoBlue)
            PrototypeDetail("Event source", "Local audit repository (planned)")
            PrototypeDetail("Records loaded", "Not connected")
        }
        Spacer(Modifier.height(15.dp))
        PrototypePanel {
            Text("Event timeline", color = ProtoNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Connect the audit/event repository to display actual entries here.", color = ProtoMuted, fontSize = 12.sp, lineHeight = 18.sp)
            Text("Typical entry fields: time • source • event • outcome • details", color = ProtoMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(15.dp))
        PrototypeInfo("This page never generates a CHECK_IN, BREAK or CHECK_OUT event. It displays records only after they have been recorded by the legitimate attendance engine.")
    }
}

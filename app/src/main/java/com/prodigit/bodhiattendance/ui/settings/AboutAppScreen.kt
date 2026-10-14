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
fun AboutAppScreen(onBack: () -> Unit) {
    PrototypePage("About App", "Application details and privacy", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.Info, "Bodhi Attendance", "Automatic Attendance System", ProtoBlue)
            PrototypeDetail("Application", "Bodhi Attendance")
            PrototypeDetail("Platform", "Android • Jetpack Compose")
            PrototypeDetail("Build type", com.prodigit.bodhiattendance.BuildConfig.BUILD_TYPE)
            PrototypeDetail("App version", com.prodigit.bodhiattendance.BuildConfig.VERSION_NAME)
        }
        Spacer(Modifier.height(15.dp))
        PrototypePanel {
            Text("Privacy & attendance", color = ProtoNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Automatic attendance uses GPS/geofencing and office-specific Wi-Fi router verification. BLE beacon evidence is optional. Employee identity, device registration, permissions and audit events require secure handling. Production data processing and employee policy must be reviewed by the company.", color = ProtoMuted, fontSize = 12.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(15.dp))
        PrototypeNotice("Attendance values come from CRM; queue and audit values come from Room. Unavailable features are labeled explicitly.")
    }
}

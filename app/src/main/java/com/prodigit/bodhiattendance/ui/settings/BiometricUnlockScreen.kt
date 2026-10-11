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
fun BiometricUnlockScreen(onBack: () -> Unit) {
    var preview by remember { mutableStateOf(false) }
    PrototypePage("Biometric Unlock", "Fingerprint and device authentication", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.Fingerprint, "Biometric security", "Fast access with supported Android device biometrics", ProtoGreen)
            PrototypeDetail("Configuration", if (preview) "Enabled (preview)" else "Not configured")
            PrototypeDetail("Authentication", "Android BiometricPrompt (planned)")
        }
        Spacer(Modifier.height(15.dp))
        PrototypePanel {
            Text("Unlock preferences", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ProtoNavy)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Use biometrics (UI preview)", color = ProtoNavy, fontWeight = FontWeight.SemiBold)
                    Text("Visual preview only; no fingerprint prompt is invoked", color = ProtoMuted, fontSize = 11.sp)
                }
                Switch(checked = preview, onCheckedChange = { preview = it })
            }
        }
        Spacer(Modifier.height(15.dp))
        PrototypeInfo("PROTOTYPE ONLY: This switch does not enable authentication. Production unlock must use AndroidX BiometricPrompt, device capability checks, and a secure fallback. No fingerprint data is stored by Bodhi Attendance.")
    }
}

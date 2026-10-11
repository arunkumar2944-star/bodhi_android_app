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
fun AppLockPinScreen(onBack: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    var first by remember { mutableStateOf("") }
    var entry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var previewEnabled by remember { mutableStateOf(false) }
    PrototypePage("App Lock / PIN", "Local app-access protection", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.Lock, "Protect your app", "A PIN can help protect the foreground interface.", ProtoPurple)
            PrototypeDetail("PIN state", if (previewEnabled) "Enabled (preview)" else "Not configured")
            PrototypeDetail("PIN length", "4 digits")
        }
        Spacer(Modifier.height(15.dp))
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.Pin, "PIN setup", "Create and confirm a four-digit PIN", ProtoBlue)
            if (phase == 0) {
                PrototypeAction(
                    label = if (previewEnabled) "Change PIN (preview)" else "Create PIN (preview)",
                    onClick = { first = ""; entry = ""; message = ""; phase = 1 }
                )
                if (previewEnabled) {
                    TextButton(onClick = { previewEnabled = false; phase = 0; message = "Preview PIN disabled" }) {
                        Text("Disable preview PIN", color = Color(0xFFD64054))
                    }
                }
            } else {
                Text(if (phase == 1) "Enter new PIN" else "Confirm new PIN", color = ProtoNavy, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = entry,
                    onValueChange = { input -> entry = input.filter(Char::isDigit).take(4) },
                    label = { Text("4-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PrototypeAction(
                    label = if (phase == 1) "Continue" else "Confirm PIN",
                    onClick = {
                        if (phase == 1) { first = entry; entry = ""; phase = 2; message = "" }
                        else if (entry == first) { previewEnabled = true; phase = 0; message = "PIN setup preview completed"; first = ""; entry = "" }
                        else { entry = ""; message = "PIN values don't match. Try again." }
                    },
                    enabled = entry.length == 4
                )
                TextButton(onClick = { phase = 0; first = ""; entry = ""; message = "" }) { Text("Cancel") }
            }
            if (message.isNotEmpty()) Text(message, fontSize = 12.sp, color = if (message.contains("don't")) Color(0xFFBA314B) else ProtoGreen)
        }
        Spacer(Modifier.height(15.dp))
        PrototypeNotice("PROTOTYPE ONLY: PIN values are not securely stored and app locking is not enforced. Implement Android Keystore-backed verification and lock lifecycle before production. Background attendance must continue independently of UI unlock.")
    }
}

package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.material3.MaterialTheme

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

// Shared, presentation-only UI components. No persistence or attendance writes.
// UI prototype for the MD presentation. No backend, credential persistence, or attendance mutation.
internal val ProtoNavy = Color(0xFF142343)
internal val ProtoBlue = Color(0xFF1769E8)
internal val ProtoMuted = Color(0xFF75839B)
internal val ProtoBorder = Color(0xFFDEE8F5)
internal val ProtoPurple = Color(0xFF7650DC)
internal val ProtoGreen = Color(0xFF138968)

@Composable
internal fun PrototypePage(title: String, subtitle: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = ProtoNavy) }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = ProtoNavy)
                Text(subtitle, fontSize = 11.sp, color = ProtoMuted)
            }
        }
        Spacer(Modifier.height(17.dp))
        content()
        Spacer(Modifier.height(45.dp))
    }
}

/**
 * Compatibility helper for existing Settings child screens.
 *
 * Both PrototypeInfo(...) and PrototypeNotice(...)
 * display the same informational component.
 */
@Composable
internal fun PrototypeInfo(text: String) {
    PrototypeNotice(text = text)
}

@Composable
internal fun PrototypePanel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ProtoBorder), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun PrototypeHeading(icon: ImageVector, title: String, description: String, tint: Color = ProtoBlue) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = tint.copy(alpha = .09f)) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = ProtoNavy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(description, color = ProtoMuted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
internal fun PrototypeNotice(text: String) {
    Surface(color = Color(0xFFEBF4FF), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color(0xFFD5E6FC))) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Outlined.Info, null, tint = ProtoBlue, modifier = Modifier.size(19.dp))
            Text(text, color = Color(0xFF536985), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
internal fun PrototypeDetail(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text(value, color = ProtoNavy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun PrototypeAction(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(49.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ProtoBlue), shape = RoundedCornerShape(14.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}


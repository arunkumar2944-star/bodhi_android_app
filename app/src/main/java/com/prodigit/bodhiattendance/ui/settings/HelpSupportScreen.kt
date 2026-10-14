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

private data class HelpTopic(val question: String, val answer: String)
private val faq = listOf(
    HelpTopic("Why didn't my attendance appear?", "Check System Health, location and Bluetooth permissions, office presence, and the offline sync queue. Report missed attendance through Attendance Issue."),
    HelpTopic("Why is background location necessary?", "The automatic attendance engine may need location checks while the app is not on screen. Android still controls when background work can run."),
    HelpTopic("Does Wi-Fi have to be enabled?", "Office Wi-Fi verification depends on the assigned location's configuration. The Knowledge Centre may have no router requirement."),
    HelpTopic("How do I change my registered device?", "Contact your administrator. Device replacement must be authorized through the CRM's device management policy."),
    HelpTopic("What happens when internet is unavailable?", "Events already recorded in Room retain their sync status. This build still needs CRM connectivity to load policy before evaluation; an offline period can delay event creation. Report any missed attendance.")
)

@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    var expanded by remember { mutableIntStateOf(-1) }
    PrototypePage("Help & Support", "Quick help for automatic attendance", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.HelpOutline, "Troubleshooting", "Answers to common attendance questions")
            faq.forEachIndexed { index, topic ->
                HorizontalDivider(color = ProtoBorder)
                Row(Modifier.fillMaxWidth().clickable { expanded = if (expanded == index) -1 else index }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.question, color = ProtoNavy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(if (expanded == index) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = ProtoBlue)
                }
                if (expanded == index) Text(topic.answer, color = ProtoMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(15.dp))
        PrototypeInfo("For a missed check-in or technical problem, open Requests → Report Attendance Issue. Employee helpdesk contact details will be added after CRM confirmation.")
    }
}

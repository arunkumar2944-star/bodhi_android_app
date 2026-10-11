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
fun PersonalInformationScreen(onBack: () -> Unit) {
    PrototypePage("Personal Information", "Read-only CRM-managed employee profile", onBack) {
        PrototypePanel {
            PrototypeHeading(Icons.Outlined.PersonOutline, "Employee profile", "Information is owned by the CRM", ProtoBlue)
            PrototypeDetail("Employee name", "Not loaded")
            PrototypeDetail("Employee ID", "Not loaded")
            PrototypeDetail("Department", "Not loaded")
            PrototypeDetail("Assigned office", "Not loaded")
            PrototypeDetail("Email", "Not loaded")
        }
        Spacer(Modifier.height(15.dp))
        PrototypeInfo("Profile editing, changing the login password, and leave management are handled in React CRM. Android only displays confirmed profile values.")
    }
}

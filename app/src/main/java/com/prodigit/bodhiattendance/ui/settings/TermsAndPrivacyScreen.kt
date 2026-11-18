package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prodigit.bodhiattendance.ui.common.AppBackground
import com.prodigit.bodhiattendance.ui.common.AppMasterHeader

private val Blue = Color(0xFF1769E8)
private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndPrivacyScreen(
    initialTab: Int = 0, // 0 for Privacy Policy, 1 for Terms & Conditions
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp)
            ) {
                AppMasterHeader()
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = if (selectedTab == 0) "Privacy Policy" else "Terms & Conditions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Tab Selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Blue
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Privacy Policy", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Outlined.PrivacyTip, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Terms & Conditions", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Outlined.Description, contentDescription = null) }
                        )
                    }

                    // Scrollable Policy Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selectedTab == 0) {
                            PrivacyPolicyContent()
                        } else {
                            TermsAndConditionsContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Bodhi Attendance Privacy Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
            Text("Last updated: November 2026", style = MaterialTheme.typography.bodySmall, color = Muted)

            HorizontalDivider()

            PolicySection("1. Overview", "Bodhi Attendance ('the Application') is an enterprise workplace attendance verification system developed for Prodigit company employees. We prioritize employee privacy while ensuring accurate, reliable workplace attendance validation.")

            PolicySection("2. Information We Collect", "• Location Data: Fine and background location data is used exclusively to verify presence inside registered office geofence boundaries during authorized work shifts.\n• Wi-Fi Information: Connected BSSIDs are evaluated to verify office router presence.\n• Device Identifiers: Installation UUIDs and hardware security baselines bind registered devices to your account.\n• Account Information: User ID, name, email, and role.")

            PolicySection("3. Background Location & Privacy Safeguards", "Location monitoring operates ONLY during active, scheduled work shifts (default 8:30 AM – 5:30 PM IST or assigned shift hours). Monitoring is automatically PAUSED during approved full-day leave, company holidays, assigned weekly offs, or after shift end. We NEVER sell, share, or track employee locations outside work hours.")

            PolicySection("4. Data Security & Encryption", "Sensitive refresh and access credentials are encrypted on the device using Android KeyStore AES-256-GCM encryption. All network communication with the backend uses secure HTTPS TLS 1.2+ encryption.")

            PolicySection("5. Data Retention & Account Deletion", "Attendance records are retained per enterprise compliance policies. Employees may request data review, regularization, or account data removal through their HR administrator.")
        }
    }
}

@Composable
private fun TermsAndConditionsContent() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Bodhi Attendance Terms & Conditions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
            Text("Last updated: November 2026", style = MaterialTheme.typography.bodySmall, color = Muted)

            HorizontalDivider()

            PolicySection("1. Acceptance of Terms", "By registering, logging in, or using Bodhi Attendance, you agree to comply with these enterprise terms and conditions and your employer's attendance guidelines.")

            PolicySection("2. Device Registration & Single Device Binding", "Employees may only register one active Android device at a time. Changing devices or resetting application data requires manager-approved device recovery verification.")

            PolicySection("3. Attendance Integrity & Anti-Spoofing Policy", "Coordinate spoofing, mock location tools, router BSSID emulation, or device identity manipulation are strictly prohibited. Violations trigger diagnostic audit flags in CRM.")

            PolicySection("4. Automatic Monitoring & Permissions", "You agree to grant required location, notification, and battery optimization permissions to enable automatic check-in, short break detection, and end-of-day checkout.")

            PolicySection("5. Policy Revisions", "Prodigit reserves the right to update these terms as workplace guidelines or Android system requirements evolve.")
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted, lineHeight = 20.sp)
    }
}

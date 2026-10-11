package com.prodigit.bodhiattendance.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Settings is for managing preferences and opening configuration screens.
 * SystemHealthScreen is the source of readiness diagnostics; this page does NOT
 * invent device/office readiness values. MainAppLayout owns header, background,
 * Scaffold and bottom navigation.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    // Compatibility with your existing AppNavigation; unused and not displayed.
    // Remove this argument from AppNavigation whenever convenient.
    onChangePasswordClick: () -> Unit = {},
    onAppLockClick: () -> Unit = {},
    onBiometricClick: () -> Unit = {},
    onRegisteredDeviceClick: () -> Unit = {},
    onPermissionsClick: () -> Unit = {},
    onBatteryOptimizationClick: () -> Unit = {},
    onOfflineDataClick: () -> Unit = {},
    onSyncStatusClick: () -> Unit = {},
    onHelpSupportClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onSystemHealthClick: () -> Unit = {},
    onEventLogClick: () -> Unit = {},
    onPersonalInformationClick: () -> Unit = {}
) {
    val blue = Color(0xFF1769E8)
    val green = Color(0xFF15976D)
    val orange = Color(0xFFE49A29)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Column {
                Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF122044))
                Spacer(Modifier.height(3.dp))
                Text("Security, device access and application preferences", fontSize = 12.sp, color = Color(0xFF77849A))
            }
        }
        item {
            SettingsPanel {
                SettingsLine(Icons.Outlined.Security, "App & Attendance Health", "Check actual readiness and required actions", blue, onSystemHealthClick)
                HorizontalDivider(color = Color(0xFFEAF0F8))
                Text(
                    "Readiness is calculated on the System Health page — not assumed here.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 11.sp, color = Color(0xFF68788F)
                )
            }
        }
        item { SettingsHeading("Security", "Protect local app access; CRM manages login passwords") }
        item {
            SettingsPanel {
                SettingsLine(Icons.Outlined.Lock, "App Lock / PIN", "Configure local app unlock", Color(0xFF7650DC), onAppLockClick)
                SettingsLine(Icons.Outlined.Fingerprint, "Biometric Unlock", "Use supported device biometrics", green, onBiometricClick, divider = false)
            }
        }
        item { SettingsHeading("Device & Attendance", "Manage permissions, registration and diagnostics") }
        item {
            SettingsPanel {
                SettingsLine(Icons.Outlined.PhoneAndroid, "Registered Device", "Review this installation's registration", blue, onRegisteredDeviceClick)
                SettingsLine(Icons.Outlined.VerifiedUser, "Permissions", "Location, nearby devices and background access", green, onPermissionsClick)
                SettingsLine(Icons.Outlined.BatterySaver, "Battery Optimization", "Review background execution restrictions", orange, onBatteryOptimizationClick)
                SettingsLine(Icons.Outlined.Info, "Event Log", "Inspect local attendance and app events", blue, onEventLogClick, divider = false)
            }
        }

        item {
            SettingsHeading(
                "Attendance Readiness",
                "Quick guide — use System Health for real statuses"
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSystemHealthClick() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.96f)
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFDEE8F5)
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        ReadinessTile(
                            Icons.Outlined.LocationOn,
                            "GPS",
                            Modifier.weight(1f)
                        )
                        ReadinessTile(
                            Icons.Outlined.Router,
                            "Wi-Fi",
                            Modifier.weight(1f)
                        )
                        ReadinessTile(
                            Icons.Outlined.Bluetooth,
                            "BLE",
                            Modifier.weight(1f)
                        )
                        ReadinessTile(
                            Icons.Outlined.Sync,
                            "Sync",
                            Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Office Wi-Fi may not be required at some locations.",
                        modifier = Modifier.padding(
                            start = 13.dp,
                            end = 13.dp,
                            bottom = 10.dp
                        ),
                        color = Color(0xFF77849A),
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "View Detailed System Health",
                            color = Color(0xFF1769E8),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.width(4.dp))

                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF1769E8)
                        )
                    }
                }
            }
        }

        item { SettingsHeading("Data & Sync", "Local events and upload diagnostics") }
        item {
            SettingsPanel {
                SettingsLine(Icons.Outlined.Storage, "Offline Data", "View locally saved events and queue", Color(0xFF7650DC), onOfflineDataClick)
                SettingsLine(Icons.Outlined.CloudSync, "Sync Status", "See last sync and pending uploads", green, onSyncStatusClick, divider = false)
            }
        }
        item { SettingsHeading("Account", "Read-only CRM-managed employee details") }
        item {
            SettingsPanel {
                SettingsLine(Icons.Outlined.PermDeviceInformation, "Personal Information", "View profile and assigned office", blue, onPersonalInformationClick, divider = false)
            }
        }
        item { SettingsHeading("Support", "Guides and application information") }
        item {
            SettingsPanel {
                SettingsLine(Icons.Outlined.HelpOutline, "Help & Support", "Attendance and device troubleshooting", blue, onHelpSupportClick)
                SettingsLine(Icons.Outlined.Info, "About App", "Version and privacy information", Color(0xFF77849A), onAboutClick, divider = false)
            }
        }
        item { SettingsHeading("Session", "Manage your signed-in session") }
        item {
            SettingsPanel {
                SettingsLine(Icons.Outlined.Logout, "Logout", "Sign out after confirmation", Color(0xFFD64054), onLogoutClick, divider = false)
            }
        }
    }
}

@Composable
private fun SettingsHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF142343))
        Text(subtitle, fontSize = 11.sp, color = Color(0xFF7D899C))
    }
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .96f)),
        border = BorderStroke(1.dp, Color(0xFFDEE8F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) { Column(content = content) }
}

@Composable
private fun SettingsLine(icon: ImageVector, title: String, description: String, tint: Color, onClick: () -> Unit, divider: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = .1f)) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = Color(0xFF142343), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(description, fontSize = 11.sp, color = Color(0xFF738197))
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF95A1B3), modifier = Modifier.size(19.dp))
    }
    if (divider) HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = Color(0xFFF0F3F8))
}

@Composable
private fun ReadinessTile(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color(0xFFF0F6FF), RoundedCornerShape(13.dp)).padding(vertical = 12.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1769E8), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF243555))
        Text("View", fontSize = 9.sp, color = Color(0xFF7C8CA4))
    }
}

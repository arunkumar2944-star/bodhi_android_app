package com.prodigit.bodhiattendance.ui.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prodigit.bodhiattendance.R
import com.prodigit.bodhiattendance.core.monitoring.AttendanceMonitorStatus
import com.prodigit.bodhiattendance.ui.common.AppBackground

private val AccessNavy = Color(0xFF142D55)
private val AccessBlue = Color(0xFF234D8C)
private val AccessMuted = Color(0xFF65758D)

/** Presentation only. Authentication stays in the existing Android unlock flow. */
@Composable
fun AppLockedScreen(
    biometricEnabled: Boolean,
    message: String,
    onUnlock: () -> Unit,
    onPinUnlock: () -> Unit
) {
    val monitoring by AttendanceMonitorStatus.state.collectAsState()

    AppBackground {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painterResource(R.drawable.bodhi_multiverse_logo_transparent),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp)
                    )
                    Column {
                        Text("Bodhi Attendance", color = AccessNavy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Automatic Attendance System", color = AccessMuted, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(30.dp))

                Card(
                    modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                    border = BorderStroke(1.dp, Color(0xFFDCE6F2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(84.dp)
                                .background(Brush.linearGradient(listOf(Color(0xFFE9F1FC), Color(0xFFDCE9FC))), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null,
                                tint = AccessBlue, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("SECURE ACCESS", color = AccessBlue, fontSize = 10.sp,
                            letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Welcome back", color = AccessNavy, fontSize = 28.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text("Unlock to view your attendance", color = AccessNavy,
                            fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (biometricEnabled) "Verify with your fingerprint, biometric or phone screen lock."
                            else "Use your phone PIN, pattern or password to continue.",
                            color = AccessMuted, fontSize = 12.sp, lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(26.dp))
                        Button(
                            onClick = onUnlock,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccessBlue),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp)
                        ) {
                            Icon(if (biometricEnabled) Icons.Outlined.Fingerprint else Icons.Outlined.Lock,
                                contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(if (biometricEnabled) "Unlock securely" else "Unlock with phone PIN",
                                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }
                        if (biometricEnabled) {
                            TextButton(onClick = onPinUnlock, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Use phone PIN instead", color = AccessBlue)
                            }
                        }
                        if (message.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                color = Color(0xFFFFF5E6), shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
                            ) {
                                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Icon(Icons.Outlined.Info, contentDescription = null,
                                        tint = Color(0xFF9B6514), modifier = Modifier.size(18.dp))
                                    Text(message, color = Color(0xFF80591D), fontSize = 12.sp,
                                        lineHeight = 18.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = Color(0xFFE7EDF5))
                        Spacer(Modifier.height(17.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.size(7.dp).background(
                                if (monitoring.running) Color(0xFF25835B) else AccessMuted, CircleShape))
                            Text(
                                if (monitoring.running) "Attendance monitoring continues" else "Your session stays signed in",
                                color = if (monitoring.running) Color(0xFF277352) else AccessMuted,
                                fontSize = 11.sp, textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.widthIn(max = 440.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(Icons.Outlined.Shield, contentDescription = null,
                        tint = AccessMuted, modifier = Modifier.size(15.dp))
                    Text("Protected with your Android screen lock", color = AccessMuted,
                        fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

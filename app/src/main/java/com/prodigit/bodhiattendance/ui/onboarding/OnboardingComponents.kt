package com.prodigit.bodhiattendance.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


val OnboardingNavy = Color(0xFF102F63)
val OnboardingBlue = Color(0xFF0877F9)
val OnboardingDeepBlue = Color(0xFF0757C7)

val OnboardingGreen = Color(0xFF0BA968)
val OnboardingGreenBackground = Color(0xFFE8F8F0)

val OnboardingOrange = Color(0xFFE67800)
val OnboardingOrangeBackground = Color(0xFFFFF2E3)

val OnboardingTextSecondary = Color(0xFF536887)
val OnboardingDivider = Color(0xFFE9EFF7)


// ============================================================
// SECTION HEADER
// ============================================================

@Composable
fun OnboardingSectionHeader(
    title: String,
    status: String,
    completed: Boolean
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .width(4.dp)
                .height(25.dp)
                .background(
                    OnboardingBlue,
                    RoundedCornerShape(10.dp)
                )
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            letterSpacing = 1.3.sp,
            fontWeight = FontWeight.Bold,
            color = OnboardingTextSecondary
        )

        Surface(
            shape = RoundedCornerShape(50.dp),
            color =
                if (completed)
                    OnboardingGreenBackground
                else
                    Color(0xFFF0F4FA)
        ) {

            Text(
                text = status,
                modifier = Modifier.padding(
                    horizontal = 11.dp,
                    vertical = 5.dp
                ),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color =
                    if (completed)
                        OnboardingGreen
                    else
                        OnboardingTextSecondary
            )
        }
    }
}


// ============================================================
// SETUP PROGRESS CARD
// ============================================================

@Composable
fun SetupProgressCard(
    completed: Int,
    total: Int = 5
) {

    val progress =
        completed.toFloat() / total.toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),

        shape = RoundedCornerShape(22.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.94f)
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 15.dp
            )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Setup Progress",
                        color = OnboardingNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Complete all $total steps",
                        color = OnboardingTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFFEAF3FF)
                ) {

                    Text(
                        text = "$completed of $total",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        color = OnboardingBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = OnboardingBlue,
                trackColor = Color(0xFFE7EEF7)
            )
        }
    }
}


// ============================================================
// INDIVIDUAL SETUP CARD
// ============================================================

@Composable
fun ModernSetupCard(
    icon: ImageVector,
    title: String,
    description: String,
    ready: Boolean,
    actionText: String,
    actionEnabled: Boolean = true,
    iconBackground: Color,
    iconColor: Color,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.07f)
            ),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = actionEnabled && !ready
                ) {
                    onClick()
                }
                .padding(
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {


            // ICON

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconBackground,
                        shape = RoundedCornerShape(15.dp)
                    ),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(25.dp)
                )
            }


            Spacer(
                modifier = Modifier.width(13.dp)
            )


            // TEXT

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = OnboardingNavy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = description,
                    color = OnboardingTextSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }


            Spacer(
                modifier = Modifier.width(8.dp)
            )


            // STATUS

            if (ready) {

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = OnboardingGreenBackground
                ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 6.dp
                        ),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    OnboardingGreen,
                                    CircleShape
                                ),

                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text(
                            text = "Enabled",
                            color = OnboardingGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        )
                    }
                }

            } else if (actionEnabled) {

                Surface(
                    modifier = Modifier.clickable {
                        onClick()
                    },

                    shape = RoundedCornerShape(50.dp),

                    color = Color(0xFFEAF3FF)
                ) {

                    Row(
                        modifier = Modifier.padding(
                            start = 10.dp,
                            end = 6.dp,
                            top = 6.dp,
                            bottom = 6.dp
                        ),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = actionText,
                            color = OnboardingBlue,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Icon(
                            imageVector =
                                Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = OnboardingBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

            } else {

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = OnboardingOrangeBackground
                ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 6.dp
                        ),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Schedule,
                            contentDescription = null,
                            tint = OnboardingOrange,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        Text(
                            text = actionText,
                            color = OnboardingOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// SECURITY CARD
// ============================================================

@Composable
fun AttendanceSecurityCard(
    icon: ImageVector
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                Color.White.copy(alpha = 0.90f)
        )
    ) {

        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFE6F4FF),
                                Color(0xFFDCEBFF)
                            )
                        ),
                        RoundedCornerShape(14.dp)
                    ),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnboardingBlue,
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "Secure & Reliable Attendance",
                    color = OnboardingNavy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text =
                        "Location, Bluetooth and device information are used only for attendance.",

                    color = OnboardingTextSecondary,
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
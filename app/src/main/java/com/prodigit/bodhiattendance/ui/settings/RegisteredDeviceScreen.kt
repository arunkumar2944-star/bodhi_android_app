
package com.prodigit.bodhiattendance.ui.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DeviceBlue = Color(0xFF1267DF)
private val DeviceText = Color(0xFF152443)
private val DeviceMuted = Color(0xFF718096)
private val DeviceBorder = Color(0xFFE1E8F3)
private val DeviceBackground = Color(0xFFF5F9FF)

data class RegisteredDeviceUiState(
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val installationId: String? = null,
    val registeredAt: String? = null,
    val isActive: Boolean? = null
)

@Composable
fun RegisteredDeviceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    device: RegisteredDeviceUiState =
        RegisteredDeviceUiState(
            deviceName = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            androidVersion =
                Build.VERSION.RELEASE ?: "Unknown"
        )
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeviceBackground)
    ) {

        Surface(
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DeviceText
                    )
                }

                Column {
                    Text(
                        "Registered Device",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = DeviceText
                    )

                    Text(
                        "Device identity and registration",
                        fontSize = 12.sp,
                        color = DeviceMuted
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Device overview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(
                    1.dp, DeviceBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Surface(
                        color = Color(0xFFEAF3FF),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.PhoneAndroid,
                                contentDescription = null,
                                tint = DeviceBlue,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        device.deviceName,
                        color = DeviceText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    val status = when (device.isActive) {
                        true -> "Active"
                        false -> "Inactive"
                        null -> "Not verified"
                    }

                    val statusColor = when (device.isActive) {
                        true -> Color(0xFF16865C)
                        false -> Color(0xFFD64154)
                        null -> Color(0xFF966517)
                    }

                    Text(
                        status,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Registration is controlled by " +
                                "the attendance system.",
                        fontSize = 12.sp,
                        color = DeviceMuted
                    )
                }
            }

            Text(
                "Device Information",
                color = DeviceText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(
                    1.dp, DeviceBorder
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DeviceDetailRow(
                        "Device Name", device.deviceName
                    )
                    HorizontalDivider(color = DeviceBorder)

                    DeviceDetailRow(
                        "Manufacturer", device.manufacturer
                    )
                    HorizontalDivider(color = DeviceBorder)

                    DeviceDetailRow(
                        "Model", device.model
                    )
                    HorizontalDivider(color = DeviceBorder)

                    DeviceDetailRow(
                        "Android Version",
                        device.androidVersion
                    )
                    HorizontalDivider(color = DeviceBorder)

                    DeviceDetailRow(
                        "Installation ID",
                        device.installationId
                            ?: "Not loaded"
                    )
                    HorizontalDivider(color = DeviceBorder)

                    DeviceDetailRow(
                        "Registered On",
                        device.registeredAt
                            ?: "Not available"
                    )
                }
            }

            // Device policy
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEAF3FF)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Security,
                        contentDescription = null,
                        tint = DeviceBlue
                    )

                    Column {
                        Text(
                            "Device Security Policy",
                            color = DeviceText,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Only one active device is " +
                                    "allowed per employee. " +
                                    "Contact your administrator " +
                                    "to authorize a device change.",
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = DeviceMuted
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = DeviceBlue,
                    modifier = Modifier.size(17.dp)
                )

                Text(
                    "This page is read-only. " +
                            "It does not create, remove or " +
                            "change a device registration.",
                    fontSize = 11.sp,
                    color = DeviceMuted,
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DeviceDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = DeviceMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            value,
            color = DeviceText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

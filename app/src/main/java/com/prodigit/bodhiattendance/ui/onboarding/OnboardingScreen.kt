package com.prodigit.bodhiattendance.ui.onboarding

import com.prodigit.bodhiattendance.R

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel


// ============================================================================
// ONBOARDING SCREEN
// ============================================================================

@Composable
fun OnboardingScreen(
    onSetupCompleted: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {

    val context =
        LocalContext.current

    val lifecycleOwner =
        LocalLifecycleOwner.current

    val uiState by
    viewModel.uiState


    // =========================================================================
    // NAVIGATE ONLY AFTER SERVER CONFIRMS ONBOARDING
    // =========================================================================

    LaunchedEffect(
        uiState.onboardingCompleted
    ) {

        if (uiState.onboardingCompleted) {

            onSetupCompleted()
        }
    }


    // =========================================================================
    // FIRST-TIME ONBOARDING IS MANDATORY
    // =========================================================================

    BackHandler(
        enabled = true
    ) {

        // Intentionally empty.
        //
        // User cannot skip onboarding by pressing Back.
        // All required setup must be completed.
    }


    // =========================================================================
    // FOREGROUND LOCATION PERMISSION
    // =========================================================================

    val foregroundLocationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) {

            viewModel.refreshSystemStatus()
        }


    // =========================================================================
    // BACKGROUND LOCATION PERMISSION
    // =========================================================================

    val backgroundLocationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) {

            viewModel.refreshSystemStatus()
        }


    // =========================================================================
    // BLUETOOTH PERMISSIONS
    // =========================================================================

    val bluetoothPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) {

            viewModel.refreshSystemStatus()
        }


    // =========================================================================
    // NOTIFICATION PERMISSION
    // =========================================================================

    val notificationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) {

            viewModel.refreshSystemStatus()
        }


    // =========================================================================
    // INITIAL STATUS CHECK
    // =========================================================================

    LaunchedEffect(Unit) {

        viewModel.refreshSystemStatus()
    }


    // =========================================================================
    // REFRESH STATUS AFTER RETURNING FROM ANDROID SETTINGS
    // =========================================================================

    DisposableEffect(
        lifecycleOwner
    ) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (
                    event ==
                    Lifecycle.Event.ON_RESUME
                ) {

                    viewModel.refreshSystemStatus()
                }
            }


        lifecycleOwner.lifecycle
            .addObserver(observer)


        onDispose {

            lifecycleOwner.lifecycle
                .removeObserver(observer)
        }
    }


    // =========================================================================
    // SETUP PROGRESS
    // =========================================================================

    val completedSteps =
        listOf(

            uiState.locationReady,

            uiState.bluetoothReady,

            uiState.notificationPermissionGranted,

            uiState.offlineStorageReady,

            uiState.deviceRegistered

        ).count {
            it
        }


    // =========================================================================
    // REQUIRED PERMISSION PROGRESS
    // =========================================================================

    val permissionCompletedSteps =
        listOf(

            uiState.locationReady,

            uiState.bluetoothReady,

            uiState.notificationPermissionGranted

        ).count {
            it
        }


    // =========================================================================
    // DEVICE SETUP PROGRESS
    // =========================================================================

    val deviceCompletedSteps =
        listOf(

            uiState.offlineStorageReady,

            uiState.deviceRegistered

        ).count {
            it
        }


    // =========================================================================
    // MAIN SCREEN
    // =========================================================================

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {


        // =====================================================================
        // SAME BACKGROUND AS LOGIN + LAUNCH
        // =====================================================================

        Image(
            painter =
                painterResource(
                    id =
                        R.drawable.launch_background
                ),

            contentDescription =
                null,

            modifier =
                Modifier.fillMaxSize(),

            contentScale =
                ContentScale.Crop
        )


        // =====================================================================
        // SCROLLABLE CONTENT
        // =====================================================================

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 18.dp
                    )
        ) {


            // =================================================================
            // TOP SPACE
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        42.dp
                    )
            )


            // =================================================================
            // BRANDING
            // =================================================================

            Column(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {


                Image(
                    painter =
                        painterResource(
                            id =
                                R.drawable
                                    .bodhi_multiverse_logo_transparent
                        ),

                    contentDescription =
                        "Bodhi Logo",

                    modifier =
                        Modifier.size(
                            58.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )


                Text(
                    text =
                        "BODHI ATTENDANCE",

                    color =
                        OnboardingNavy,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold,

                    letterSpacing =
                        1.sp
                )


                Text(
                    text =
                        "Multiple Universe, One Vision",

                    color =
                        OnboardingTextSecondary,

                    fontSize =
                        9.sp
                )
            }


            // =================================================================
            // PAGE TITLE
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            Text(
                text =
                    "Let's Get You Started",

                color =
                    OnboardingNavy,

                fontSize =
                    25.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    "Complete the required setup to enable secure and automatic attendance.",

                color =
                    OnboardingTextSecondary,

                fontSize =
                    11.sp,

                lineHeight =
                    15.sp
            )


            // =================================================================
            // PROGRESS
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            SetupProgressCard(
                completed =
                    completedSteps,

                total =
                    5
            )


            // =================================================================
            // REQUIRED PERMISSIONS
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        26.dp
                    )
            )


            OnboardingSectionHeader(

                title =
                    "REQUIRED PERMISSIONS",

                status =
                    "$permissionCompletedSteps / 3 Complete",

                completed =
                    permissionCompletedSteps == 3
            )


            Spacer(
                modifier =
                    Modifier.height(
                        11.dp
                    )
            )


            // =================================================================
            // LOCATION
            // =================================================================

            ModernSetupCard(

                icon =
                    Icons.Default.LocationOn,

                title =
                    "Location (GPS)",

                description =
                    when {


                        !uiState
                            .locationPermissionGranted ->

                            "Allow precise location for office geofence detection"


                        !uiState
                            .locationServiceEnabled ->

                            "Turn on device location service"


                        else ->

                            "Ready: monitoring starts while the app is visible and stops after checkout"
                    },


                ready =
                    uiState.locationReady,


                actionText =
                    when {


                        !uiState
                            .locationPermissionGranted ->

                            "Allow"


                        !uiState
                            .locationServiceEnabled ->

                            "Turn On"


                        else ->

                            "Enabled"
                    },


                actionEnabled =
                    true,


                iconBackground =
                    Color(
                        0xFFFFE8E8
                    ),


                iconColor =
                    Color(
                        0xFFFF4055
                    ),


                onClick = {


                    when {


                        // =====================================================
                        // FOREGROUND LOCATION
                        // =====================================================

                        !uiState
                            .locationPermissionGranted -> {


                            foregroundLocationLauncher
                                .launch(

                                    arrayOf(

                                        Manifest.permission
                                            .ACCESS_FINE_LOCATION,

                                        Manifest.permission
                                            .ACCESS_COARSE_LOCATION
                                    )
                                )
                        }


                        // =====================================================
                        // LOCATION SERVICE
                        // =====================================================

                        !uiState
                            .locationServiceEnabled -> {


                            context.startActivity(

                                Intent(
                                    Settings
                                        .ACTION_LOCATION_SOURCE_SETTINGS
                                )
                            )
                        }
                    }
                }
            )


            Text("Choose Precise location and While using the app first. For automatic 8 AM restart without opening the app, separately allow background location and Alarms & reminders. Tracking stops after checkout even with background permission.", style = MaterialTheme.typography.bodySmall)
            Text("After checkout, the screen closes and location monitoring stops. Next day at 8:00 AM IST, the app verifies the new workday and resumes monitoring in the background. A monitoring notification remains visible.", style = MaterialTheme.typography.bodySmall)

            androidx.compose.material3.TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${context.packageName}")))
            }) { Text("Enable automatic restart: Allow all the time") }
            if (Build.VERSION.SDK_INT >= 31) {
                androidx.compose.material3.TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:${context.packageName}")))
                }) { Text("Enable automatic restart: Alarms & reminders") }
            }

            // =================================================================
            // BLUETOOTH
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        11.dp
                    )
            )


            ModernSetupCard(

                icon =
                    Icons.Default.Bluetooth,

                title =
                    "Bluetooth",

                description =
                    when {


                        !uiState
                            .bluetoothPermissionGranted ->

                            "Allow nearby devices for BLE beacon detection"


                        !uiState
                            .bluetoothEnabled ->

                            "Turn on Bluetooth for office beacon detection"


                        else ->

                            "Ready for office BLE beacon detection"
                    },


                ready =
                    uiState.bluetoothReady,


                actionText =
                    when {


                        !uiState
                            .bluetoothPermissionGranted ->

                            "Allow"


                        !uiState
                            .bluetoothEnabled ->

                            "Turn On"


                        else ->

                            "Enabled"
                    },


                actionEnabled =
                    true,


                iconBackground =
                    Color(
                        0xFFE7F1FF
                    ),


                iconColor =
                    Color(
                        0xFF0877F9
                    ),


                onClick = {


                    when {


                        // =====================================================
                        // BLUETOOTH PERMISSION
                        // =====================================================

                        !uiState
                            .bluetoothPermissionGranted -> {


                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.S
                            ) {


                                bluetoothPermissionLauncher
                                    .launch(

                                        arrayOf(

                                            Manifest.permission
                                                .BLUETOOTH_SCAN,

                                            Manifest.permission
                                                .BLUETOOTH_CONNECT
                                        )
                                    )


                            } else {


                                foregroundLocationLauncher
                                    .launch(

                                        arrayOf(

                                            Manifest.permission
                                                .ACCESS_FINE_LOCATION,

                                            Manifest.permission
                                                .ACCESS_COARSE_LOCATION
                                        )
                                    )
                            }
                        }


                        // =====================================================
                        // BLUETOOTH OFF
                        // =====================================================

                        !uiState
                            .bluetoothEnabled -> {


                            context.startActivity(

                                Intent(
                                    Settings
                                        .ACTION_BLUETOOTH_SETTINGS
                                )
                            )
                        }
                    }
                }
            )


            // =================================================================
            // NOTIFICATIONS
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        11.dp
                    )
            )


            ModernSetupCard(

                icon =
                    Icons.Default.Notifications,

                title =
                    "Notifications",

                description =
                    if (
                        uiState
                            .notificationPermissionGranted
                    ) {


                        "Attendance alerts and important notifications enabled"


                    } else {


                        "Required for attendance alerts and important updates"
                    },


                ready =
                    uiState
                        .notificationPermissionGranted,


                actionText =
                    if (
                        uiState
                            .notificationPermissionGranted
                    ) {


                        "Enabled"


                    } else {


                        "Allow"
                    },


                actionEnabled =
                    true,


                iconBackground =
                    Color(
                        0xFFF0E8FF
                    ),


                iconColor =
                    Color(
                        0xFF7253D8
                    ),


                onClick = {


                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.TIRAMISU
                    ) {


                        notificationLauncher
                            .launch(

                                Manifest.permission
                                    .POST_NOTIFICATIONS
                            )


                    } else {


                        viewModel
                            .refreshSystemStatus()
                    }
                }
            )


            // =================================================================
            // DEVICE & ATTENDANCE
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        27.dp
                    )
            )


            OnboardingSectionHeader(

                title =
                    "DEVICE & ATTENDANCE",

                status =
                    "$deviceCompletedSteps / 2 Complete",

                completed =
                    deviceCompletedSteps == 2
            )


            Spacer(
                modifier =
                    Modifier.height(
                        11.dp
                    )
            )


            // =================================================================
            // OFFLINE STORAGE
            // =================================================================

            ModernSetupCard(

                icon =
                    Icons.Default.Storage,

                title =
                    "Offline Storage",

                description =
                    if (
                        uiState
                            .offlineStorageReady
                    ) {


                        "Local attendance database is ready"


                    } else {


                        "Preparing secure offline attendance storage"
                    },


                ready =
                    uiState
                        .offlineStorageReady,


                actionText =
                    if (
                        uiState
                            .offlineStorageReady
                    ) {


                        "Ready"


                    } else {


                        "Pending"
                    },


                actionEnabled =
                    false,


                iconBackground =
                    Color(
                        0xFFFFF0DA
                    ),


                iconColor =
                    Color(
                        0xFFF28A00
                    ),


                onClick = {

                    // No user action.
                    //
                    // Room initialization updates
                    // offlineStorageReady automatically.
                }
            )


            // =================================================================
            // DEVICE REGISTRATION
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        11.dp
                    )
            )


            ModernSetupCard(

                icon =
                    Icons.Default.PhoneAndroid,

                title =
                    "Device Registration",

                description =
                    when {

                        uiState.deviceRegistered ->

                            "This phone is securely registered"


                        uiState.deviceRegistrationLoading ->

                            "Registering and verifying this device..."


                        else ->

                            "Register this device for secure attendance tracking"
                    },


                ready =
                    uiState.deviceRegistered,


                actionText =
                    when {

                        uiState.deviceRegistered ->

                            "Registered"


                        uiState.deviceRegistrationLoading ->

                            "Registering..."


                        else ->

                            "Register"
                    },


                actionEnabled =
                    !uiState.deviceRegistered &&
                            !uiState.deviceRegistrationLoading,


                iconBackground =
                    Color(
                        0xFFEDE9FF
                    ),


                iconColor =
                    Color(
                        0xFF6252D8
                    ),


                onClick = {

                    viewModel.registerDevice()
                }
            )


            // =================================================================
            // SECURITY INFORMATION
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            AttendanceSecurityCard(

                icon =
                    Icons.Default.VerifiedUser
            )


            // =================================================================
            // ERROR MESSAGE
            // =================================================================

            uiState
                .errorMessage
                ?.let { errorMessage ->


                    Spacer(
                        modifier =
                            Modifier.height(
                                13.dp
                            )
                    )


                    Text(
                        text =
                            errorMessage,

                        color =
                            Color(
                                0xFFD32F2F
                            ),

                        fontSize =
                            11.sp,

                        lineHeight =
                            15.sp
                    )
                }


            // =================================================================
            // COMPLETE SETUP BUTTON
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            Button(

                onClick = {


                    // =========================================================
                    // IMPORTANT:
                    //
                    // We DO NOT call markOnboardingCompleted() here.
                    //
                    // completeOnboarding() first calls Express.
                    //
                    // Only after the server confirms:
                    //
                    // ONBOARDING_COMPLETED
                    // or
                    // ALREADY_ONBOARDED
                    //
                    // will the ViewModel set:
                    //
                    // onboardingCompleted = true
                    // =========================================================

                    if (
                        uiState.canContinue &&
                        !uiState.onboardingCompleting
                    ) {


                        viewModel
                            .completeOnboarding()
                    }
                },


                enabled =

                    uiState.canContinue &&

                            !uiState
                                .onboardingCompleting,


                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            56.dp
                        ),


                shape =
                    RoundedCornerShape(
                        18.dp
                    ),


                colors =
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                OnboardingDeepBlue,

                            contentColor =
                                Color.White,

                            disabledContainerColor =
                                Color(
                                    0xFFB7C9E3
                                ),

                            disabledContentColor =
                                Color.White.copy(
                                    alpha = 0.80f
                                )
                        )
            ) {


                // =============================================================
                // SERVER COMPLETION REQUEST RUNNING
                // =============================================================

                if (
                    uiState
                        .onboardingCompleting
                ) {


                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(
                                21.dp
                            ),

                        strokeWidth =
                            2.dp,

                        color =
                            Color.White
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                10.dp
                            )
                    )


                    Text(

                        text =
                            "Completing Setup...",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                } else {


                    // =========================================================
                    // NORMAL BUTTON
                    // =========================================================

                    Text(

                        text =
                            if (
                                uiState.canContinue
                            ) {

                                "Complete Setup"

                            } else {

                                "Complete Setup to Continue"
                            },


                        fontSize =
                            14.sp,


                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
                            )
                    )


                    Icon(

                        imageVector =
                            Icons.Default.ArrowForward,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                19.dp
                            )
                    )
                }
            }


            // =================================================================
            // BOTTOM SPACE
            // =================================================================

            Spacer(
                modifier =
                    Modifier.height(
                        45.dp
                    )
            )
        }
    }
}


// ============================================================================
// OPEN APP SETTINGS
//
// Android 11+ background location:
//
// Permissions
//      ↓
// Location
//      ↓
// Allow all the time
// ============================================================================

private fun openApplicationSettings(
    context: Context
) {

    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {

            data =
                Uri.fromParts(
                    "package",
                    context.packageName,
                    null
                )
        }


    context.startActivity(
        intent
    )
}
package com.prodigit.bodhiattendance.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prodigit.bodhiattendance.ui.attendance.view.ViewScreen
import com.prodigit.bodhiattendance.ui.settings.SettingsScreen
import com.prodigit.bodhiattendance.ui.settings.RegisteredDeviceScreen
import com.prodigit.bodhiattendance.ui.auth.LoginScreen
import com.prodigit.bodhiattendance.ui.common.BottomNavigationDestination
import com.prodigit.bodhiattendance.ui.dashboard.DashboardScreen
import com.prodigit.bodhiattendance.ui.launch.BodhiLaunchScreen
import com.prodigit.bodhiattendance.ui.layout.MainAppLayout
import com.prodigit.bodhiattendance.ui.onboarding.OnboardingScreen
import com.prodigit.bodhiattendance.ui.onduty.OnDutyScreen
import com.prodigit.bodhiattendance.ui.requests.RequestScreen
import com.prodigit.bodhiattendance.ui.startup.StartupDestination
import com.prodigit.bodhiattendance.ui.startup.StartupViewModel
import com.prodigit.bodhiattendance.ui.systemhealth.SystemHealthScreen
import com.prodigit.bodhiattendance.ui.systemhealth.SystemHealthViewModel
import com.crm.attendance.ui.screens.request.PermissionShortTimeScreen
import com.prodigit.bodhiattendance.ui.requests.AttendanceIssueScreen
import com.prodigit.bodhiattendance.ui.requests.AttendanceRegularizationScreen
import com.prodigit.bodhiattendance.ui.requests.RequestHistoryScreen
import com.prodigit.bodhiattendance.ui.requests.RequestDetailsScreen
import com.prodigit.bodhiattendance.ui.requests.RequestDemoData
import com.prodigit.bodhiattendance.ui.settings.PermissionsScreen
import com.prodigit.bodhiattendance.ui.settings.BatteryOptimizationScreen
import com.prodigit.bodhiattendance.ui.settings.AppLockPinScreen
import com.prodigit.bodhiattendance.ui.settings.BiometricUnlockScreen
import com.prodigit.bodhiattendance.ui.settings.OfflineDataScreen
import com.prodigit.bodhiattendance.ui.settings.SyncStatusScreen
import com.prodigit.bodhiattendance.ui.settings.EventLogScreen
import com.prodigit.bodhiattendance.ui.settings.PersonalInformationScreen
import com.prodigit.bodhiattendance.ui.settings.HelpSupportScreen
import com.prodigit.bodhiattendance.ui.settings.AboutAppScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.prodigit.bodhiattendance.ui.systemhealth.TrustedTimeScreen
import com.prodigit.bodhiattendance.ui.systemhealth.AttendanceConfigurationScreen



// ============================================================
// OUTER APPLICATION ROUTES
// ============================================================

private const val ROUTE_LAUNCH =
    "launch"

private const val ROUTE_LOGIN =
    "login"

private const val ROUTE_ONBOARDING =
    "onboarding"

private const val ROUTE_MAIN =
    "main"

private const val ROUTE_DEVICE_BLOCKED =
    "device_blocked"


// ============================================================
// AUTHENTICATED APPLICATION ROUTES
// ============================================================

private const val ROUTE_DASHBOARD =
    "dashboard"

private const val ROUTE_SYSTEM_HEALTH =
    "system_health"

private const val ROUTE_ATTENDANCE =
    "attendance"

private const val ROUTE_ON_DUTY =
    "on_duty"

private const val ROUTE_REQUESTS =
    "requests"
private const val ROUTE_PERMISSION_SHORT_TIME = "permission_short_time"

private const val ROUTE_ATTENDANCE_ISSUE = "attendance_issue"

private const val ROUTE_ATTENDANCE_REGULARIZATION =
    "attendance_regularization"

private const val ROUTE_REQUEST_HISTORY = "request_history"
private const val ROUTE_REQUEST_DETAILS = "request_details/{requestId}"

private const val ROUTE_SETTINGS =
    "settings"

private const val ROUTE_REGISTERED_DEVICE =
    "registered_device"

private const val ROUTE_PERMISSIONS = "permissions"
private const val ROUTE_BATTERY_OPTIMIZATION =
    "battery_optimization"
private const val ROUTE_APP_LOCK = "app_lock_pin"
private const val ROUTE_BIOMETRIC = "biometric_unlock"
private const val ROUTE_OFFLINE_DATA = "offline_data"
private const val ROUTE_SYNC_STATUS = "sync_status"
private const val ROUTE_EVENT_LOG = "event_log"
private const val ROUTE_PERSONAL_INFORMATION = "personal_information"
private const val ROUTE_HELP_SUPPORT = "help_support"
private const val ROUTE_ABOUT_APP = "about_app"


private const val ROUTE_TRUSTED_TIME =
    "trusted_time"

private const val ROUTE_ATTENDANCE_CONFIGURATION =
    "attendance_configuration"


// ============================================================
// APPLICATION NAVIGATION
//
// OUTER navigation handles:
//
// - launch
// - login
// - onboarding
// - authenticated main application
// - blocked-device screen
//
// IMPORTANT:
//
// ROUTE_LAUNCH belongs ONLY to this outer NavHost.
// ============================================================

@Composable
fun AppNavigation() {

    val navController =
        rememberNavController()


    NavHost(

        navController =
            navController,

        startDestination =
            ROUTE_LAUNCH

    ) {


        // ====================================================
        // STARTUP
        // ====================================================

        composable(
            ROUTE_LAUNCH
        ) {

            StartupRoute(
                navController =
                    navController
            )
        }


        // ====================================================
        // LOGIN
        // ====================================================

        composable(
            ROUTE_LOGIN
        ) {

            LoginScreen(

                onLoginSuccess = {

                    navController.navigate(
                        ROUTE_LAUNCH
                    ) {

                        popUpTo(
                            ROUTE_LOGIN
                        ) {

                            inclusive =
                                true
                        }

                        launchSingleTop =
                            true
                    }
                }
            )
        }


        // ====================================================
        // ONBOARDING
        // ====================================================

        composable(
            ROUTE_ONBOARDING
        ) {

            OnboardingScreen(

                onSetupCompleted = {

                    navController.navigate(
                        ROUTE_MAIN
                    ) {

                        popUpTo(
                            ROUTE_ONBOARDING
                        ) {

                            inclusive =
                                true
                        }

                        launchSingleTop =
                            true
                    }
                }
            )
        }


        // ====================================================
        // MAIN AUTHENTICATED APPLICATION
        // ====================================================

        composable(
            ROUTE_MAIN
        ) {

            MainApplication()
        }


        // ====================================================
        // DEVICE BLOCKED
        // ====================================================

        composable(
            ROUTE_DEVICE_BLOCKED
        ) {

            DeviceBlockedScreen()
        }
    }
}


// ============================================================
// MAIN AUTHENTICATED APPLICATION
//
// MainAppLayout is created only here.
//
// MainAppLayout owns:
//
// - AppBackground
// - AppMasterHeader
// - Scaffold
// - Bottom navigation
//
// Screens inside this NavHost are BODY-ONLY.
//
// IMPORTANT:
//
// This INNER NavHost must start at ROUTE_DASHBOARD.
//
// ROUTE_LAUNCH is NOT part of this inner graph.
// ============================================================

@Composable
private fun MainApplication() {

    val mainNavController =
        rememberNavController()

    var showLogoutDialog by remember { mutableStateOf(false) }


    // ========================================================
    // CURRENT ROUTE
    // ========================================================

    val backStackEntry by
    mainNavController
        .currentBackStackEntryAsState()


    val currentRoute =
        backStackEntry
            ?.destination
            ?.route


    // ========================================================
    // SELECTED BOTTOM NAVIGATION DESTINATION
    // ========================================================

    val selectedDestination =
        when (currentRoute) {


            ROUTE_ATTENDANCE -> {

                BottomNavigationDestination.ATTENDANCE
            }


            ROUTE_ON_DUTY -> {

                BottomNavigationDestination.ON_DUTY
            }


            ROUTE_REQUESTS,
            ROUTE_PERMISSION_SHORT_TIME,
            ROUTE_ATTENDANCE_ISSUE,
            ROUTE_ATTENDANCE_REGULARIZATION,
            ROUTE_REQUEST_HISTORY,
            ROUTE_REQUEST_DETAILS -> {
                BottomNavigationDestination.REQUESTS
            }


            ROUTE_SETTINGS ,
            ROUTE_REGISTERED_DEVICE,
            ROUTE_PERMISSIONS,
            ROUTE_BATTERY_OPTIMIZATION,
            ROUTE_APP_LOCK,
            ROUTE_BIOMETRIC,
            ROUTE_OFFLINE_DATA,
            ROUTE_SYNC_STATUS,
            ROUTE_EVENT_LOG,
            ROUTE_PERSONAL_INFORMATION,
            ROUTE_HELP_SUPPORT,
            ROUTE_ABOUT_APP -> {

                BottomNavigationDestination.SETTINGS
            }


            // System Health is accessed from Dashboard.
            // Keep Dashboard selected in bottom navigation.

            ROUTE_SYSTEM_HEALTH -> {

                BottomNavigationDestination.DASHBOARD
            }


            else -> {

                BottomNavigationDestination.DASHBOARD
            }
        }


    // ========================================================
    // MASTER APPLICATION LAYOUT
    // ========================================================

    MainAppLayout(

        selectedDestination =
            selectedDestination,


        // ====================================================
        // BOTTOM NAVIGATION
        // ====================================================

        onDestinationSelected = { destination ->

            when (destination) {


                // =============================================
                // DASHBOARD
                // =============================================

                BottomNavigationDestination.DASHBOARD -> {

                    mainNavController.navigate(
                        ROUTE_DASHBOARD
                    ) {

                        popUpTo(
                            ROUTE_DASHBOARD
                        ) {

                            inclusive =
                                false
                        }

                        launchSingleTop =
                            true
                    }
                }


                // =============================================
                // ATTENDANCE
                // =============================================

                BottomNavigationDestination.ATTENDANCE -> {

                    mainNavController.navigate(
                        ROUTE_ATTENDANCE
                    ) {

                        launchSingleTop =
                            true
                    }
                }


                // =============================================
                // ON DUTY
                // =============================================

                BottomNavigationDestination.ON_DUTY -> {

                    mainNavController.navigate(
                        ROUTE_ON_DUTY
                    ) {

                        launchSingleTop =
                            true
                    }
                }


                // =============================================
                // REQUESTS
                // =============================================

                BottomNavigationDestination.REQUESTS -> {

                    mainNavController.navigate(
                        ROUTE_REQUESTS
                    ) {

                        launchSingleTop =
                            true
                    }
                }


                // =============================================
                // SETTINGS
                // =============================================

                BottomNavigationDestination.SETTINGS -> {

                    mainNavController.navigate(
                        ROUTE_SETTINGS
                    ) {

                        launchSingleTop =
                            true
                    }
                }
            }
        },


        // ====================================================
        // GLOBAL NOTIFICATION
        //
        // Notification belongs to AppMasterHeader.
        //
        // Later we will connect this to shared application
        // notification state instead of DashboardViewModel.
        // ====================================================

        notificationCount =
            0,

        onNotificationClick = {

            // Notification route will be connected later.
        }

    ) {


        // ====================================================
        // MAIN SCREEN NAVIGATION
        //
        // MainAppLayout already handles Scaffold padding.
        //
        // IMPORTANT:
        //
        // This is the authenticated INNER navigation graph.
        //
        // The correct start destination is ROUTE_DASHBOARD.
        //
        // ROUTE_LAUNCH belongs only to AppNavigation().
        // ====================================================

        NavHost(

            navController =
                mainNavController,

            startDestination =
                ROUTE_DASHBOARD,

            modifier =
                Modifier.fillMaxSize()

        ) {


            // =================================================
            // DASHBOARD
            // =================================================

            composable(
                ROUTE_DASHBOARD
            ) {

                DashboardScreen(


                    // =========================================
                    // ATTENDANCE
                    // =========================================

                    onAttendanceClick = {

                        mainNavController.navigate(
                            ROUTE_ATTENDANCE
                        ) {

                            launchSingleTop =
                                true
                        }
                    },


                    // =========================================
                    // ON DUTY
                    // =========================================

                    onOnDutyClick = {

                        mainNavController.navigate(
                            ROUTE_ON_DUTY
                        ) {

                            launchSingleTop =
                                true
                        }
                    },


                    // =========================================
                    // REQUESTS
                    // =========================================

                    onRequestsClick = {

                        mainNavController.navigate(
                            ROUTE_REQUESTS
                        ) {

                            launchSingleTop =
                                true
                        }
                    },


                    // =========================================
                    // SYSTEM HEALTH
                    // =========================================

                    onSystemHealthClick = {

                        mainNavController.navigate(
                            ROUTE_SYSTEM_HEALTH
                        ) {

                            launchSingleTop =
                                true
                        }
                    }


//                    // =========================================
//                    // SETTINGS
//                    // =========================================
//
//                    onSettingsClick = {
//
//                        mainNavController.navigate(
//                            ROUTE_SETTINGS
//                        ) {
//
//                            launchSingleTop =
//                                true
//                        }
//                    }
                )
            }


            // =================================================
            // SYSTEM HEALTH
            // =================================================





            composable(ROUTE_SYSTEM_HEALTH) {

                val systemHealthViewModel: SystemHealthViewModel =
                    viewModel()

                SystemHealthScreen(
                    viewModel = systemHealthViewModel,

                    onBack = {
                        mainNavController.popBackStack()
                    },

                    onRegisteredDeviceClick = {
                        mainNavController.navigate(
                            ROUTE_REGISTERED_DEVICE
                        ) {
                            launchSingleTop = true
                        }
                    },

                    onPermissionsClick = {
                        mainNavController.navigate(
                            ROUTE_PERMISSIONS
                        ) {
                            launchSingleTop = true
                        }
                    },

                    onBatteryOptimizationClick = {
                        mainNavController.navigate(
                            ROUTE_BATTERY_OPTIMIZATION
                        ) {
                            launchSingleTop = true
                        }
                    },

                    onTrustedTimeClick = {
                        mainNavController.navigate(
                            ROUTE_TRUSTED_TIME
                        ) {
                            launchSingleTop = true
                        }
                    },

                    onAttendanceConfigurationClick = {
                        mainNavController.navigate(
                            ROUTE_ATTENDANCE_CONFIGURATION
                        ) {
                            launchSingleTop = true
                        }
                    },

                    onOfficeBeaconClick = {
                        mainNavController.navigate(
                            ROUTE_ATTENDANCE_CONFIGURATION
                        ) {
                            launchSingleTop = true
                        }
                    },

                    onOfficeWifiClick = {
                        mainNavController.navigate(
                            ROUTE_ATTENDANCE_CONFIGURATION
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }



            composable(ROUTE_TRUSTED_TIME) {
                TrustedTimeScreen(
                    onBack = {
                        mainNavController.popBackStack()
                    }
                )
            }

            composable(ROUTE_ATTENDANCE_CONFIGURATION) {
                AttendanceConfigurationScreen(
                    onBack = {
                        mainNavController.popBackStack()
                    }
                )
            }




            // =================================================
            // ATTENDANCE
            // TEMPORARY SCREEN
            // =================================================

            composable(
                ROUTE_ATTENDANCE
            ) {

                ViewScreen()

            }


            // =================================================
            // ON DUTY
            // TEMPORARY SCREEN
            // =================================================

            composable(
                ROUTE_ON_DUTY
            ) {

                OnDutyScreen()
            }


            // =================================================
            // REQUESTS
            // TEMPORARY SCREEN
            // =================================================

            composable(
                ROUTE_REQUESTS
            ) {
                RequestScreen(
                    onRegularizationClick = {
                        mainNavController.navigate(
                            ROUTE_ATTENDANCE_REGULARIZATION
                        ) {
                            launchSingleTop = true
                        }
                    },

                    onPermissionClick = {
                        mainNavController.navigate(ROUTE_PERMISSION_SHORT_TIME) {
                            launchSingleTop = true
                        }
                    },

                    onAttendanceIssueClick = {
                        mainNavController.navigate(ROUTE_ATTENDANCE_ISSUE) {
                            launchSingleTop = true
                           }
                        },

                    onViewAllRequestsClick = {
                        mainNavController.navigate(ROUTE_REQUEST_HISTORY) {
                            launchSingleTop = true
                        }
                    },

                    onRequestClick = {
                        // The existing callback has no request ID.
                        // Open history until RequestScreen supplies an ID.
                        mainNavController.navigate(ROUTE_REQUEST_HISTORY) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(ROUTE_PERMISSION_SHORT_TIME) {
                PermissionShortTimeScreen(
                    onBack = {
                        mainNavController.popBackStack()
                    },
                    onSubmit = { request ->
                        android.util.Log.d(
                            "PermissionRequest",
                            "Form validated: $request"
                        )
                    }
                )
            }

            composable(ROUTE_ATTENDANCE_ISSUE) {

                AttendanceIssueScreen(
                    onBack = {
                        mainNavController.popBackStack()
                    },

                    onSubmit = { request ->

                        // TODO:
                        // Send request through ViewModel / Repository.
                        // Handle API loading, success and failure.

                        android.util.Log.d(
                            "AttendanceIssue",
                            "Form validated: $request"
                        )
                    }
                )
            }

            composable(ROUTE_ATTENDANCE_REGULARIZATION) {

                AttendanceRegularizationScreen(
                    onBack = {
                        mainNavController.popBackStack()
                    },

                    onSubmit = { request ->
                        // UI stage only.
                        // Next: RequestViewModel -> Repository -> CRM API

                        android.util.Log.d(
                            "AttendanceRegularization",
                            "Validated form: $request"
                        )
                    }
                )
            }

            composable(ROUTE_REQUEST_HISTORY) {

                RequestHistoryScreen(
                    requests = RequestDemoData.requests,
                    isDemo = true,
                    onBack = {
                        mainNavController.popBackStack()
                    },
                    onRequestClick = { requestId ->

                        mainNavController.navigate(
                            "request_details/$requestId"
                        )
                    }
                )
            }

            composable(ROUTE_REQUEST_DETAILS) { entry ->

                val requestId = entry.arguments
                    ?.getString("requestId")

                val selectedRequest = RequestDemoData.requests
                    .firstOrNull { it.id == requestId }

                RequestDetailsScreen(
                    request = selectedRequest,
                    isDemo = true,
                    onBack = {
                        mainNavController.popBackStack()
                    }
                )
            }


            // =================================================
            // SETTINGS
            // TEMPORARY SCREEN
            // =================================================

            composable(
                ROUTE_SETTINGS
            ) {

                SettingsScreen(
                    onSystemHealthClick = {
                        mainNavController.navigate("system_health")
                    },

                    onChangePasswordClick = {
                        // child page later
                    },

                    onAppLockClick = { mainNavController.navigate(ROUTE_APP_LOCK) { launchSingleTop = true } },

                    onBiometricClick = { mainNavController.navigate(ROUTE_BIOMETRIC) { launchSingleTop = true } },

                    onRegisteredDeviceClick = {
                        mainNavController.navigate(ROUTE_REGISTERED_DEVICE) {
                            launchSingleTop = true
                        }
                    },

                    onPermissionsClick = {
                        mainNavController.navigate(ROUTE_PERMISSIONS) {
                            launchSingleTop = true
                        }
                    },

                    onBatteryOptimizationClick = {
                        mainNavController.navigate(
                            ROUTE_BATTERY_OPTIMIZATION
                        ) {
                            launchSingleTop = true
                        }
                    },

                    onOfflineDataClick = { mainNavController.navigate(ROUTE_OFFLINE_DATA) { launchSingleTop = true } },

                    onSyncStatusClick = { mainNavController.navigate(ROUTE_SYNC_STATUS) { launchSingleTop = true } },

                    onHelpSupportClick = { mainNavController.navigate(ROUTE_HELP_SUPPORT) { launchSingleTop = true } },

                    onAboutClick = { mainNavController.navigate(ROUTE_ABOUT_APP) { launchSingleTop = true } },

                    onLogoutClick = { showLogoutDialog = true },
                    onEventLogClick = { mainNavController.navigate(ROUTE_EVENT_LOG) { launchSingleTop = true } },
                    onPersonalInformationClick = { mainNavController.navigate(ROUTE_PERSONAL_INFORMATION) { launchSingleTop = true } }
                )
            }

            composable(ROUTE_REGISTERED_DEVICE) {
                RegisteredDeviceScreen(
                    onBack = {
                        mainNavController.popBackStack()
                    }
                )
            }
            composable(ROUTE_PERMISSIONS) {
                PermissionsScreen(
                    onBack = {
                        mainNavController.popBackStack()
                    }
                )
            }

            composable(ROUTE_APP_LOCK) { AppLockPinScreen(onBack = { mainNavController.popBackStack() }) }
            composable(ROUTE_BIOMETRIC) { BiometricUnlockScreen(onBack = { mainNavController.popBackStack() }) }
            composable(ROUTE_OFFLINE_DATA) { OfflineDataScreen(onBack = { mainNavController.popBackStack() }) }
            composable(ROUTE_SYNC_STATUS) { SyncStatusScreen(onBack = { mainNavController.popBackStack() }) }
            composable(ROUTE_EVENT_LOG) { EventLogScreen(onBack = { mainNavController.popBackStack() }) }
            composable(ROUTE_PERSONAL_INFORMATION) { PersonalInformationScreen(onBack = { mainNavController.popBackStack() }) }
            composable(ROUTE_HELP_SUPPORT) { HelpSupportScreen(onBack = { mainNavController.popBackStack() }) }
            composable(ROUTE_ABOUT_APP) { AboutAppScreen(onBack = { mainNavController.popBackStack() }) }

            composable(ROUTE_BATTERY_OPTIMIZATION) {
                BatteryOptimizationScreen(
                    onBack = {
                        mainNavController.popBackStack()
                    }
                )
            }

        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign out of Bodhi Attendance?") },
            text = { Text("This is a prototype confirmation. Your real session will not be cleared in this demonstration build.") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Confirm (preview)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}


// ============================================================
// STARTUP ROUTE
// ============================================================

@Composable
private fun StartupRoute(

    navController: NavHostController,

    startupViewModel: StartupViewModel =
        viewModel()

) {

    val destination by
    startupViewModel
        .destination
        .collectAsState()


    val networkError by
    startupViewModel
        .networkError
        .collectAsState()


    // ========================================================
    // LAUNCH SCREEN
    //
    // With temporary UI development mode enabled in
    // StartupViewModel, this screen exists only momentarily
    // before navigation to ROUTE_MAIN.
    //
    // Normal production startup remains unchanged.
    // ========================================================

    BodhiLaunchScreen(

        networkError =
            networkError
    )


    // ========================================================
    // STARTUP DESTINATION
    // ========================================================

    LaunchedEffect(
        destination
    ) {

        when (destination) {


            // =================================================
            // LOADING
            // =================================================

            StartupDestination.LOADING -> {

                // Remain on launch screen.
            }


            // =================================================
            // LOGIN
            // =================================================

            StartupDestination.LOGIN -> {

                navController.navigate(
                    ROUTE_LOGIN
                ) {

                    popUpTo(
                        ROUTE_LAUNCH
                    ) {

                        inclusive =
                            true
                    }

                    launchSingleTop =
                        true
                }
            }


            // =================================================
            // ONBOARDING
            // =================================================

            StartupDestination.ONBOARDING -> {

                navController.navigate(
                    ROUTE_ONBOARDING
                ) {

                    popUpTo(
                        ROUTE_LAUNCH
                    ) {

                        inclusive =
                            true
                    }

                    launchSingleTop =
                        true
                }
            }


            // =================================================
            // DASHBOARD
            //
            // Dashboard belongs inside MainApplication().
            //
            // Therefore navigate to ROUTE_MAIN.
            // MainApplication's inner NavHost then starts at
            // ROUTE_DASHBOARD.
            // =================================================

            StartupDestination.DASHBOARD -> {

                navController.navigate(
                    ROUTE_MAIN
                ) {

                    popUpTo(
                        ROUTE_LAUNCH
                    ) {

                        inclusive =
                            true
                    }

                    launchSingleTop =
                        true
                }
            }


            // =================================================
            // DEVICE BLOCKED
            // =================================================

            StartupDestination.DEVICE_BLOCKED -> {

                navController.navigate(
                    ROUTE_DEVICE_BLOCKED
                ) {

                    popUpTo(
                        ROUTE_LAUNCH
                    ) {

                        inclusive =
                            true
                    }

                    launchSingleTop =
                        true
                }
            }
        }
    }
}


// ============================================================
// TEMPORARY MAIN PAGE
// ============================================================

@Composable
private fun TemporaryMainPage(

    title: String

) {

    Column(

        modifier =
            Modifier.fillMaxSize(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center

    ) {

        Text(
            text =
                title
        )
    }
}


// ============================================================
// DEVICE BLOCKED SCREEN
// ============================================================

@Composable
private fun DeviceBlockedScreen() {

    Column(

        modifier =
            Modifier.fillMaxSize(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center

    ) {

        Text(
            text =
                "Device Access Disabled"
        )

        Text(
            text =
                "Please contact your administrator."
        )
    }
}

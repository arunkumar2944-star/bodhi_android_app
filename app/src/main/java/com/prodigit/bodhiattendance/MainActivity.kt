package com.prodigit.bodhiattendance

import android.os.Bundle
import com.prodigit.bodhiattendance.core.monitoring.AttendanceMonitorStartGate
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.prodigit.bodhiattendance.ui.navigation.AppNavigation
import com.prodigit.bodhiattendance.ui.theme.CRMAttendanceTheme
class MainActivity : FragmentActivity() {
    private val checkoutReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == com.prodigit.bodhiattendance.core.monitoring.AttendancePrivacyPolicy.CLOSE_AFTER_CHECKOUT) {
                finishAndRemoveTask()
            }
        }
    }
    override fun onDestroy() {
        unregisterReceiver(checkoutReceiver)
        super.onDestroy()
    }


    override fun onResume() {
        super.onResume()
        AttendanceMonitorStartGate.onActivityResumed(applicationContext)
    }

    override fun onPause() {
        AttendanceMonitorStartGate.onActivityPaused()
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)
        com.prodigit.bodhiattendance.core.monitoring.AttendancePrivacyPolicy.removeLegacyAutomation(applicationContext)
        androidx.core.content.ContextCompat.registerReceiver(this, checkoutReceiver,
            android.content.IntentFilter(com.prodigit.bodhiattendance.core.monitoring.AttendancePrivacyPolicy.CLOSE_AFTER_CHECKOUT),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        handleCalibrationIntent(intent)
        handleUiResetIntent(intent)

        // =====================================================
        // APP UI
        // =====================================================
        setContent {

            CRMAttendanceTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {

                    AppNavigation()
                }
            }
        }
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCalibrationIntent(intent)
        handleUiResetIntent(intent)
        if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            AttendanceMonitorStartGate.onActivityResumed(applicationContext)
        }
    }

    private fun handleUiResetIntent(intent: android.content.Intent?) {
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("clear_ui_cache", false) == true) {
            intent.removeExtra("clear_ui_cache")
            androidx.lifecycle.ViewModelProvider(this)[com.prodigit.bodhiattendance.ui.live.LiveUiViewModel::class.java].clearDisplayData()
        }
    }

    private fun handleCalibrationIntent(intent: android.content.Intent?) {
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("home_calibration", false) == true) {
            intent.removeExtra("home_calibration")
            com.prodigit.bodhiattendance.core.monitoring.HomeLocationCalibration.start(applicationContext)
        }
    }
}
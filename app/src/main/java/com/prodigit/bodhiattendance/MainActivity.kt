package com.prodigit.bodhiattendance

import android.os.Bundle
import com.prodigit.bodhiattendance.core.monitoring.AttendanceMonitorStartGate
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.prodigit.bodhiattendance.ui.navigation.AppNavigation
import com.prodigit.bodhiattendance.ui.theme.CRMAttendanceTheme
class MainActivity : ComponentActivity() {

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
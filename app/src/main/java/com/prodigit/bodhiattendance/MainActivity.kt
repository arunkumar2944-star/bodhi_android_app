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
}
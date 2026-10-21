package com.prodigit.bodhiattendance

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
    private val clockReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == android.content.Intent.ACTION_TIME_CHANGED) captureClockDiagnostics(timeChanged = true)
        }
    }

    private fun captureClockDiagnostics(timeChanged: Boolean = false) {
        lifecycleScope.launch {
            try {
                val app = applicationContext
                if (com.prodigit.bodhiattendance.core.session.SessionManager(app).getUserId() <= 0) return@launch
                val monitor = com.prodigit.bodhiattendance.core.monitoring.TrustedTimeMonitor(app)
                val clock = monitor.read()
                if (timeChanged) {
                    // System notification proves a clock adjustment, not who made it or why.
                    com.prodigit.bodhiattendance.core.monitoring.AttendanceDiagnostics(app)
                        .record("DEVICE_TIME_CHANGED", com.google.gson.Gson().toJson(clock))
                }
                monitor.report(clock)
                com.prodigit.bodhiattendance.core.monitoring.AttendanceDiagnosticDelivery.request(app)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { android.util.Log.w("ATTENDANCE_DIAGNOSTICS", "Clock check deferred: ${e.javaClass.simpleName}") }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(clockReceiver)
        unregisterReceiver(checkoutReceiver)
        super.onDestroy()
    }


    override fun onResume() {
        super.onResume()
        captureClockDiagnostics()
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
        androidx.core.content.ContextCompat.registerReceiver(this, clockReceiver,
            android.content.IntentFilter(android.content.Intent.ACTION_TIME_CHANGED),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        handleCalibrationIntent(intent)
        handleUiResetIntent(intent)

        // =====================================================
        // APP UI
        // =====================================================
        setContent {

            // Existing surfaces/cards use a light branded palette. Keep text
            // contrast reliable even when the device is in dark mode.
            val context = androidx.compose.ui.platform.LocalContext.current
            val appearance by com.prodigit.bodhiattendance.ui.theme.AppearancePreference
                .observe(context).collectAsState()
            val isDark = when (appearance) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            CRMAttendanceTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize().imePadding(),
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
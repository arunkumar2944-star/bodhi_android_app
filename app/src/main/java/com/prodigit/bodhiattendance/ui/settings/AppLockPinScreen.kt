package com.prodigit.bodhiattendance.ui.settings
import androidx.compose.runtime.Composable
@Composable
fun AppLockPinScreen(onBack: () -> Unit) {
    PrototypePage("App Lock / PIN", "Local access security", onBack) {
        PrototypeInfo("App lock is not configured. PIN protection is unavailable in this build.")
    }
}

package com.prodigit.bodhiattendance.ui.settings
import androidx.compose.runtime.Composable
@Composable
fun BiometricUnlockScreen(onBack: () -> Unit) {
    PrototypePage("Biometric Unlock", "Local access security", onBack) {
        PrototypeInfo("Biometric app unlock is unavailable in this build.")
    }
}

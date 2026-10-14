package com.prodigit.bodhiattendance.ui.settings
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.biometric.BiometricManager
import com.prodigit.bodhiattendance.ui.security.*

@Composable
fun BiometricUnlockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = rememberAccessPreferences()
    val enabled = accessPreference(prefs,"biometric_enabled")
    val lockEnabled = accessPreference(prefs,"lock_enabled")
    val available = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    var message by remember { mutableStateOf("") }
    val authenticate = rememberAppAuthentication(!enabled, {
        val saved = prefs.edit().putBoolean("biometric_enabled", !enabled).commit()
        message = if (saved) "Biometric preference saved" else "Preference could not be saved"
    }, { message = it })
    PrototypePage("Biometric Unlock", "Android system biometric authentication", onBack) {
        PrototypeDetail("Biometric unlock", if (enabled) "Enabled" else "Disabled")
        PrototypeDetail("Enrolled strong biometric", if (available) "Available" else "Unavailable; check Android security settings")
        PrototypeInfo("Enable App Lock first. Your phone PIN remains available as a fallback. Attendance runs independently of app unlock.")
        Button(onClick = authenticate, enabled = lockEnabled && (enabled || available)) { Text(if(enabled) "Disable biometric unlock" else "Enable biometric unlock") }
        if (message.isNotBlank()) Text(message)
    }
}

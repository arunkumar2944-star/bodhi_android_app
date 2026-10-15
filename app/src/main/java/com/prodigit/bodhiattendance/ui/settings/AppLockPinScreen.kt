package com.prodigit.bodhiattendance.ui.settings
import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.prodigit.bodhiattendance.ui.security.*

@Composable
fun AppLockPinScreen(onBack: () -> Unit) {
    val prefs = rememberAccessPreferences()
    val enabled = accessPreference(prefs,"lock_enabled")
    var message by remember { mutableStateOf("") }
    val authenticate = rememberAppAuthentication(false, {
        val saved = prefs.edit().putBoolean("lock_enabled", !enabled).commit()
        message = if (saved) "App lock preference saved" else "App lock preference could not be saved"
    }, { message = it })
    PrototypePage("App Lock / PIN", "Protect app access using your phone screen lock", onBack) {
        PrototypeDetail("App lock", if (enabled) "Enabled" else "Disabled")
        PrototypeInfo("Uses your existing Android PIN, pattern or password. Bodhi does not create or store a separate PIN. Background attendance continues while the UI is locked.")
        PrototypeInfo("Rotation keeps the screen unlocked. Returning within 60 seconds avoids another prompt; locking the phone or a longer background stay requires authentication.")
        Button(onClick = authenticate) { Text(if (enabled) "Disable app lock" else "Enable app lock") }
        if (message.isNotBlank()) Text(message)
    }
}


package com.prodigit.bodhiattendance.ui.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val LOCK_PREFS = "bodhi_app_access"

@Composable
fun rememberAccessPreferences(): SharedPreferences {
    val context = LocalContext.current
    return remember(context) { context.getSharedPreferences(LOCK_PREFS, Context.MODE_PRIVATE) }
}
@Composable
fun accessPreference(prefs: SharedPreferences, key: String): Boolean {
    var value by remember(prefs, key) { mutableStateOf(prefs.getBoolean(key, false)) }
    DisposableEffect(prefs,key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) value = prefs.getBoolean(key,false)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}
/** System authentication only: the app never receives or stores a phone PIN. */
@Composable
fun rememberAppAuthentication(useBiometric: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit): () -> Unit {
    val activity = LocalContext.current as FragmentActivity
    val success by rememberUpdatedState(onSuccess)
    val failure by rememberUpdatedState(onError)
    val keyguard = remember(activity) { requireNotNull(activity.getSystemService(KeyguardManager::class.java)) }
    val credential = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) success() else failure("Unlock cancelled; attendance monitoring continues")
    }
    @Suppress("DEPRECATION")
    fun launchCredential() {
        val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Bodhi Attendance", "Use your phone PIN, pattern or password")
        if (intent == null) failure("Set a screen lock in Android Settings first")
        else credential.launch(intent)
    }
    val prompt = remember(activity) {
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { success() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { failure(errString.toString()) }
            override fun onAuthenticationFailed() { failure("Biometric not recognised; try again or use phone PIN") }
        })
    }
    return {
        if (!keyguard.isDeviceSecure) failure("Set a phone PIN, pattern or password in Android Settings first")
        else if (useBiometric && BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock Bodhi Attendance")
                .setSubtitle("Verify with an enrolled biometric")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Cancel").build())
        } else launchCredential()
    }
}

/** UI access lock does not stop the foreground attendance service or clear session data. */
@Composable
fun AppAccessGate(content: @Composable () -> Unit) {
    val prefs = rememberAccessPreferences()
    val enabled = accessPreference(prefs,"lock_enabled")
    val biometric = accessPreference(prefs,"biometric_enabled")
    var locked by remember(enabled) { mutableStateOf(enabled) }
    var message by remember { mutableStateOf("") }
    val activity = LocalContext.current as FragmentActivity
    DisposableEffect(activity, enabled) {
        if (enabled) activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,enabled) {
        val observer = LifecycleEventObserver { _, event -> if (enabled && event == Lifecycle.Event.ON_STOP) locked = true }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val unlock = rememberAppAuthentication(biometric, { locked = false; message = "" }, { message = it })
    val pinUnlock = rememberAppAuthentication(false, { locked = false; message = "" }, { message = it })
    if (enabled && locked) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.Center) {
                Text("Bodhi Attendance is locked", style = MaterialTheme.typography.headlineSmall)
                Text("Automatic attendance monitoring continues in the background.")
                Button(onClick = unlock) { Text(if (biometric) "Unlock" else "Unlock with phone PIN") }
                if (biometric) TextButton(onClick = pinUnlock) { Text("Use phone PIN / pattern / password") }
                if (message.isNotBlank()) Text(message)
            }
        }
    } else content()
}

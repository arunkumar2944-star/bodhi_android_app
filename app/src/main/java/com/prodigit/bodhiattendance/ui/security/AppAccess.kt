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
    val activity = LocalContext.current as FragmentActivity
    val access: AppAccessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(viewModelStoreOwner = activity)
    LaunchedEffect(enabled) { access.configure(enabled) }
    DisposableEffect(activity, access) {
        onDispose {
            // Preserve rotation, but never carry an unlock through leaving
            // the authenticated UI (for example logout and sign-in again).
            if (!activity.isChangingConfigurations) access.reset()
        }
    }
    var message by remember { mutableStateOf("") }
    DisposableEffect(activity, enabled) {
        if (enabled) activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, enabled, activity, access) {
        val keyguard = activity.getSystemService(KeyguardManager::class.java)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                access.onBackground(android.os.SystemClock.elapsedRealtime(), activity.isChangingConfigurations)
            } else if (event == Lifecycle.Event.ON_RESUME) {
                access.onForeground(android.os.SystemClock.elapsedRealtime(), keyguard?.isKeyguardLocked == true)
            }
        }
        val screenReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: android.content.Intent?) {
                if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) access.screenLocked()
            }
        }
        lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(activity, screenReceiver,
            android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            lifecycle.removeObserver(observer)
            activity.unregisterReceiver(screenReceiver)
        }
    }
    val unlock = rememberAppAuthentication(biometric, { access.authenticated(); message = "" }, { message = it })
    val pinUnlock = rememberAppAuthentication(false, { access.authenticated(); message = "" }, { message = it })
    if (enabled && access.locked) {
        AppLockedScreen(
            biometricEnabled = biometric,
            message = message,
            onUnlock = unlock,
            onPinUnlock = pinUnlock
        )
    } else content()
}


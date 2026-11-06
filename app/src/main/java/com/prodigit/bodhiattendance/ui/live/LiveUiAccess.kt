package com.prodigit.bodhiattendance.ui.live

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun liveUiViewModel(): LiveUiViewModel {
    val activity = (LocalActivity.current as? ComponentActivity)
        ?: (LocalContext.current.findActivity() as? ComponentActivity)
    return if (activity != null) {
        viewModel(viewModelStoreOwner = activity)
    } else {
        viewModel()
    }
}

@Composable
fun liveUiState(): LiveUiState {
    val model = liveUiViewModel()
    val state by model.state.collectAsState()
    return state
}
// Install once in the authenticated navigation graph; polling pauses with the UI.
@Composable
fun RefreshLiveUi(model: LiveUiViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(model, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            android.util.Log.d("BODHI_UI_SYNC", "Foreground data refresh started")
            try {
                while (true) { model.refresh(); delay(30_000) }
            } finally {
                android.util.Log.d("BODHI_UI_SYNC", "Foreground data refresh stopped; in-flight reads may be cancelled")
            }
        }
    }
}

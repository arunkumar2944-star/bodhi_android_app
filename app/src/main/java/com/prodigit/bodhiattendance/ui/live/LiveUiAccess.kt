package com.prodigit.bodhiattendance.ui.live

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun liveUiViewModel(): LiveUiViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ComponentActivity)

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
            while (true) { model.refresh(); delay(30_000) }
        }
    }
}

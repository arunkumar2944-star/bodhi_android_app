package com.prodigit.bodhiattendance.ui.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Activity-retained UI access only. Never persisted across process death. */
class AppAccessViewModel : ViewModel() {
    var locked by mutableStateOf(true)
        private set
    private var enabled = false
    private var backgroundAt: Long? = null

    fun configure(value: Boolean) {
        if (enabled == value) return
        enabled = value
        locked = value
        backgroundAt = null
    }

    fun authenticated() { locked = false; backgroundAt = null }

    fun onBackground(now: Long, changingConfiguration: Boolean) {
        if (enabled && !locked && !changingConfiguration && backgroundAt == null) backgroundAt = now
    }

    fun onForeground(now: Long, deviceLocked: Boolean) {
        if (enabled && (deviceLocked || backgroundAt?.let { now - it >= GRACE_MS } == true)) locked = true
        backgroundAt = null
    }

    fun screenLocked() { if (enabled) locked = true }

    fun reset() { locked = enabled; backgroundAt = null }

    companion object { const val GRACE_MS = 60_000L }
}

package com.prodigit.bodhiattendance.ui.security

import org.junit.Assert.*
import org.junit.Test

class AppAccessViewModelTest {
    private fun unlocked() = AppAccessViewModel().apply { configure(true); authenticated() }

    @Test fun rotationKeepsUnlock() {
        val state = unlocked()
        state.onBackground(1_000, changingConfiguration = true)
        state.configure(true)
        state.onForeground(100_000, deviceLocked = false)
        assertFalse(state.locked)
    }

    @Test fun quickSettingsVisitKeepsUnlock() {
        val state = unlocked()
        state.onBackground(1_000, false)
        state.onForeground(60_999, false)
        assertFalse(state.locked)
    }

    @Test fun fullGracePeriodRequiresAuthentication() {
        val state = unlocked()
        state.onBackground(1_000, false)
        state.onForeground(61_000, false)
        assertTrue(state.locked)
    }

    @Test fun screenOffLocksImmediately() {
        val state = unlocked()
        state.screenLocked()
        state.onForeground(2_000, false)
        assertTrue(state.locked)
    }

    @Test fun lockedDeviceIgnoresGracePeriod() {
        val state = unlocked()
        state.onBackground(1_000, false)
        state.onForeground(1_001, true)
        assertTrue(state.locked)
    }

    @Test fun newProcessStartsLocked() {
        val state = AppAccessViewModel()
        state.configure(true)
        assertTrue(state.locked)
    }

    @Test fun successfulAuthenticationClearsOldBackgroundTimer() {
        val state = unlocked()
        state.onBackground(1_000, false)
        state.authenticated()
        state.onForeground(100_000, false)
        assertFalse(state.locked)
    }
}

package com.prodigit.bodhiattendance.data.local.security


// ============================================================
// BOOT SESSION PROVIDER
//
// Small abstraction around boot-session information.
//
// Production:
//      BootSessionManager
//
// Unit tests:
//      FakeBootSessionProvider
//
// This lets trusted-time / engine-state logic be tested without
// requiring Android SystemClock or Settings.Global.
// ============================================================

interface BootSessionProvider {


    // Stable identifier for the current Android boot session.
    fun getBootId(): String


    // Current Android monotonic elapsed realtime.
    fun getElapsedRealtimeMs(): Long
}
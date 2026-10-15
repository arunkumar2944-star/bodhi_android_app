package com.prodigit.bodhiattendance.data.remote.dto.attendance


// ============================================================
// ATTENDANCE EVENT REQUEST
//
// Must match Express recordAttendanceEvent request body.
//
// Trusted attendance time:
//
// server time anchor
// +
// eventElapsedRealtimeMs delta
//
// deviceEventTime is evidence only and is NOT authoritative.
// ============================================================

data class AttendanceEventRequest(

    // Event identity / idempotency
    val eventUuid: String,

    // Registered Android installation
    val deviceId: String,

    // CHECK_IN / BREAK_START / BREAK_END / CHECK_OUT
    val eventType: String,

    // Device wall-clock evidence only
    val deviceEventTime: String? = null,


    // ========================================================
    // TRUSTED TIME
    // ========================================================

    val timeAnchorUuid: String,

    val eventElapsedRealtimeMs: Long,

    val bootId: String,


    // ========================================================
    // GPS
    // ========================================================

    val latitude: Double? = null,

    val longitude: Double? = null,

    val accuracyMeters: Double? = null,

    val insideGeofence: Boolean = false,


    // ========================================================
    // BLE
    // ========================================================

    val beaconDetected: Boolean = false,

    val beaconId: String? = null,

    val beaconRssi: Int? = null,


    // ========================================================
    // WI-FI ROUTER CONFIRMATION
    // ========================================================

    val wifiVerified: Boolean = false,

    val wifiBssid: String? = null,
    val arrivalProof: WifiArrivalProof? = null
)

/** Connection continuity is observed by the registered client, not independent router attestation. */
data class WifiArrivalProof(
    val id: String,
    val officeId: Long,
    val anchorUuid: String,
    val bootId: String,
    val observedElapsedMs: Long,
    val confirmedElapsedMs: Long,
    val wifiBssid: String,
    val continuityConfirmed: Boolean = true
)

package com.prodigit.bodhiattendance.domain.model


// ============================================================
// ATTENDANCE EVIDENCE
//
// Represents the physical-presence evidence collected by
// Android before an attendance event is created.
//
// IMPORTANT:
//
// This class does NOT decide whether attendance should be
// marked.
//
// Individual evidence providers populate this model.
//
// Attendance Engine will later evaluate the evidence and
// decide whether an attendance event should be generated.
// ============================================================

data class AttendanceEvidence(


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
    // WI-FI
    // ========================================================

    val wifiVerified: Boolean = false,

    val wifiBssid: String? = null
)
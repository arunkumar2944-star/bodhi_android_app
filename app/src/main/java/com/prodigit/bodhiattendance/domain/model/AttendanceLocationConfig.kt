package com.prodigit.bodhiattendance.domain.model


// ============================================================
// ATTENDANCE LOCATION CONFIGURATION
//
// Domain representation of an attendance-enabled workplace.
//
// Configuration originates from React CRM:
//
// React CRM
//      ↓
// Express
//      ↓
// Android
//
// Android must NOT hard-code:
//
// - office coordinates
// - geofence radius
// - BLE identifiers
// - Wi-Fi BSSIDs
//
// GPS, BLE and Wi-Fi evidence providers will consume this
// configuration later.
// ============================================================

data class AttendanceLocationConfig(

    val id: Long,

    val locationName: String,

    val locationCode: String,


    // ========================================================
    // GPS / GEOFENCE
    // ========================================================

    val latitude: Double,

    val longitude: Double,

    val geofenceRadiusMeters: Double,

    val maxAccuracyMeters: Double?,


    // ========================================================
    // BLE
    // ========================================================

    val beacons: List<AttendanceBeaconConfig> =
        emptyList(),


    // ========================================================
    // WI-FI
    // ========================================================

    val wifiAccessPoints: List<AttendanceWifiConfig> =
        emptyList()
)


// ============================================================
// BLE CONFIGURATION
// ============================================================

data class AttendanceBeaconConfig(

    val id: Long,

    val name: String,

    val identifier: String,

    val expectedMinRssi: Int? = null
)


// ============================================================
// WI-FI CONFIGURATION
// ============================================================

data class AttendanceWifiConfig(

    val id: Long,

    val name: String,

    val ssid: String? = null,

    val bssid: String
)
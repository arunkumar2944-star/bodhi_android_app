package com.prodigit.bodhiattendance.data.remote.dto.attendance


// ============================================================
// ATTENDANCE LOCATION DTO
//
// Remote representation of one attendance-enabled workplace.
//
// Example:
//
// Main Office
// Knowledge Centre
// Future Branch
// ============================================================

data class AttendanceLocationDto(

    val id: Long,

    val locationName: String,

    val locationCode: String,

    val latitude: Double,

    val longitude: Double,

    val geofenceRadiusMeters: Double,

    val maxAccuracyMeters: Double? = null,

    val beacons: List<AttendanceBeaconDto> =
        emptyList(),

    val wifi: List<AttendanceWifiDto> =
        emptyList()
)
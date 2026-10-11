package com.prodigit.bodhiattendance.data.remote.dto.attendance


// ============================================================
// WI-FI ACCESS POINT DTO
//
// BSSID is the physical access-point identifier used for
// attendance evidence.
//
// SSID is primarily informational/display data.
// ============================================================

data class AttendanceWifiDto(

    val id: Long,

    val name: String,

    val ssid: String? = null,

    val bssid: String
)
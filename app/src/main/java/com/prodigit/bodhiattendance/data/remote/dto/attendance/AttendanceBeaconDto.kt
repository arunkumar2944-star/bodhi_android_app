package com.prodigit.bodhiattendance.data.remote.dto.attendance


// ============================================================
// BLE BEACON DTO
//
// Represents one active BLE beacon configured for an
// attendance location from React CRM.
// ============================================================

data class AttendanceBeaconDto(

    val id: Long,

    val name: String,

    val identifier: String,

    val expectedMinRssi: Int? = null
)
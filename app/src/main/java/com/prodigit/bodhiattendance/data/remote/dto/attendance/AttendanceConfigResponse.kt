package com.prodigit.bodhiattendance.data.remote.dto.attendance



// ============================================================
// ACTIVE ATTENDANCE CONFIG RESPONSE
//
// Response from:
//
// GET /api/v1/attendance/locations/active
//
// Contains all active attendance locations available to the
// authenticated employee/company.
// ============================================================

data class AttendanceConfigResponse(

    val success: Boolean,

    val message: String? = null,

    val data: List<AttendanceLocationDto> = emptyList()
)
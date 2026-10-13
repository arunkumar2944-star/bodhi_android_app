package com.prodigit.bodhiattendance.data.remote.dto.attendance

import com.google.gson.annotations.SerializedName

data class AttendanceConfigResponse(
    val success: Boolean,
    val count: Int = 0,
    val data: AttendanceConfigDataDto? = null
)

data class AttendanceConfigDataDto(
    @SerializedName("configuration_version") val configurationVersion: String? = null,
    @SerializedName("server_time") val serverTime: String? = null,
    val locations: List<AttendanceLocationDto> = emptyList()
)

package com.prodigit.bodhiattendance.data.remote.dto.attendance

import com.google.gson.annotations.SerializedName

data class AttendanceBeaconDto(
    @SerializedName("beacon_id") val id: Long,
    @SerializedName("beacon_name") val name: String,
    @SerializedName("beacon_identifier") val identifier: String,
    @SerializedName("expected_min_rssi") val expectedMinRssi: Int? = null
)

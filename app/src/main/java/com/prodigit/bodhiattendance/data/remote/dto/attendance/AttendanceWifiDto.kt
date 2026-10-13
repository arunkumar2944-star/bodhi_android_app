package com.prodigit.bodhiattendance.data.remote.dto.attendance

import com.google.gson.annotations.SerializedName

data class AttendanceWifiDto(
    @SerializedName("wifi_id") val id: Long,
    @SerializedName("wifi_name") val name: String,
    val ssid: String? = null,
    val bssid: String
)

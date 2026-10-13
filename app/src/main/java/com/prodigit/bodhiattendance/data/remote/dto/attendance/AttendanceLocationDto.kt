package com.prodigit.bodhiattendance.data.remote.dto.attendance

import com.google.gson.annotations.SerializedName

data class AttendanceLocationDto(
    @SerializedName("location_id") val id: Long,
    @SerializedName("location_name") val locationName: String,
    @SerializedName("location_code") val locationCode: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("geofence_radius_meters") val geofenceRadiusMeters: Double,
    @SerializedName("max_accuracy_meters") val maxAccuracyMeters: Double,
    @SerializedName("wifi_required") val wifiRequired: Boolean,
    @SerializedName("ble_required") val bleRequired: Boolean,
    @SerializedName("is_enabled") val isEnabled: Boolean,
    val beacons: List<AttendanceBeaconDto> = emptyList(),
    val wifi: List<AttendanceWifiDto> = emptyList()
)

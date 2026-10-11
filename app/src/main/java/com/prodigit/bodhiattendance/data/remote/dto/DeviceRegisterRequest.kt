package com.prodigit.bodhiattendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DeviceRegisterRequest(

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("device_name")
    val deviceName: String
)
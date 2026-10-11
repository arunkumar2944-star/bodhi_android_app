package com.prodigit.bodhiattendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DeviceOnboardingRequest(

    @SerializedName("device_id")
    val deviceId: String
)
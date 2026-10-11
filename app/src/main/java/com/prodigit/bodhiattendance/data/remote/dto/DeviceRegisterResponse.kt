package com.prodigit.bodhiattendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DeviceRegisterResponse(

    val success: Boolean,

    val status: String,

    val message: String,

    val device: RegisteredDevice?
)


data class RegisteredDevice(

    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("company_id")
    val companyId: Int?,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("device_name")
    val deviceName: String?,

    @SerializedName("is_onboarded")
    val isOnboarded: Boolean,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("created_at")
    val createdAt: String?
)
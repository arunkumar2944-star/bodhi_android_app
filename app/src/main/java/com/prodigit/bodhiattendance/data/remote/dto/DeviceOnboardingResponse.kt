package com.prodigit.bodhiattendance.data.remote.dto

data class DeviceOnboardingResponse(

    val success: Boolean,

    val status: String,

    val message: String,

    val device: RegisteredDevice?
)
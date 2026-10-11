package com.prodigit.bodhiattendance.domain.repository

import com.prodigit.bodhiattendance.data.remote.dto.CurrentDeviceResponse
import com.prodigit.bodhiattendance.data.remote.dto.DeviceOnboardingResponse
import com.prodigit.bodhiattendance.data.remote.dto.DeviceRegisterResponse


interface DeviceRepository {


    // ============================================================
    // REGISTER DEVICE
    // ============================================================

    suspend fun registerDevice(
        token: String,
        deviceId: String,
        deviceName: String
    ): Result<DeviceRegisterResponse>


    // ============================================================
    // COMPLETE ONBOARDING
    // ============================================================

    suspend fun completeOnboarding(
        token: String,
        deviceId: String
    ): Result<DeviceOnboardingResponse>


    // ============================================================
    // GET CURRENT DEVICE
    // ============================================================

    suspend fun getCurrentDevice(
        token: String,
        deviceId: String
    ): Result<CurrentDeviceResponse>
}
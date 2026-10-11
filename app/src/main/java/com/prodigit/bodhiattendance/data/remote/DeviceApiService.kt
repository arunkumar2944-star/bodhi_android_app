package com.prodigit.bodhiattendance.data.remote

import com.prodigit.bodhiattendance.data.remote.dto.CurrentDeviceResponse
import com.prodigit.bodhiattendance.data.remote.dto.DeviceOnboardingRequest
import com.prodigit.bodhiattendance.data.remote.dto.DeviceOnboardingResponse
import com.prodigit.bodhiattendance.data.remote.dto.DeviceRegisterRequest
import com.prodigit.bodhiattendance.data.remote.dto.DeviceRegisterResponse

import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query


interface DeviceApiService {


    // ============================================================
    // REGISTER DEVICE
    // ============================================================

    @POST("api/v1/devices/register")
    suspend fun registerDevice(

        @Header("Authorization")
        authorization: String,

        @Body
        request: DeviceRegisterRequest

    ): Response<DeviceRegisterResponse>


    // ============================================================
    // COMPLETE DEVICE ONBOARDING
    // ============================================================

    @PATCH("api/v1/devices/onboarding/complete")
    suspend fun completeOnboarding(

        @Header("Authorization")
        authorization: String,

        @Body
        request: DeviceOnboardingRequest

    ): Response<DeviceOnboardingResponse>


    // ============================================================
    // GET CURRENT DEVICE STATUS
    //
    // GET:
    // /api/v1/devices/current?device_id=xxxx
    // ============================================================

    @GET("api/v1/devices/current")
    suspend fun getCurrentDevice(

        @Header("Authorization")
        authorization: String,

        @Query("device_id")
        deviceId: String

    ): Response<CurrentDeviceResponse>
}
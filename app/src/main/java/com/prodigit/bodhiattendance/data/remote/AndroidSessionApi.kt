package com.prodigit.bodhiattendance.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class AndroidDeviceSessionRequest(@SerializedName("device_id") val deviceId: String)
data class AndroidRefreshRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("refresh_token") val refreshToken: String
)
data class AndroidSessionResponse(
    val success: Boolean,
    val token: String?,
    @SerializedName("refresh_token") val refreshToken: String?
)

interface AndroidSessionApi {
    @POST("api/v1/auth/android/activate")
    suspend fun activate(@Header("Authorization") authorization: String,
                         @Body request: AndroidDeviceSessionRequest): Response<AndroidSessionResponse>

    @POST("api/v1/auth/android/refresh")
    suspend fun refresh(@Body request: AndroidRefreshRequest): Response<AndroidSessionResponse>

    @POST("api/v1/auth/android/revoke")
    suspend fun revoke(@Header("Authorization") authorization: String,
                       @Body request: AndroidRefreshRequest): Response<AndroidSessionResponse>
}

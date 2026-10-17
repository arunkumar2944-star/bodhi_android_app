package com.prodigit.bodhiattendance.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class AttendanceHeartbeatRequest(
    @SerializedName("device_id") val deviceId: String
)

data class AttendanceHeartbeatResponse(
    val success: Boolean,
    val status: String?,
    val recovered: Boolean?
)

interface AttendanceHeartbeatApi {
    @POST("api/v1/attendance/monitor/stop")
    suspend fun stop(@Header("Authorization") authorization: String,
                     @Body request: AttendanceHeartbeatRequest): Response<AttendanceHeartbeatResponse>

    @POST("api/v1/attendance/monitor/heartbeat")
    suspend fun send(
        @Header("Authorization") authorization: String,
        @Body request: AttendanceHeartbeatRequest
    ): Response<AttendanceHeartbeatResponse>
}

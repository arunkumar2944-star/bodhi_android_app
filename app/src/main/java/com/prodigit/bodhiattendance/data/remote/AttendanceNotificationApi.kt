package com.prodigit.bodhiattendance.data.remote

import com.prodigit.bodhiattendance.core.messaging.FcmTokenRegistrationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AttendanceNotificationApi {
    @POST("api/v1/attendance/notifications/fcm-token")
    suspend fun registerFcmToken(
        @Header("Authorization") token: String,
        @Body body: FcmTokenRegistrationRequest
    ): Response<MobileReply<NotificationTokenReceipt>>
}

data class NotificationTokenReceipt(
    val message: String?
)

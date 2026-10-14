package com.prodigit.bodhiattendance.data.remote

import retrofit2.Response
import retrofit2.http.*

// Dedicated employee read models; event-writing APIs remain owned by the engine.
interface MobileUiApi {
    @GET("api/v1/auth/me") suspend fun profile(@Header("Authorization") token: String): Response<ProfileReply>
    @GET("api/v1/attendance/mobile/attendance") suspend fun attendance(@Header("Authorization") token: String): Response<MobileReply<AttendanceReadModel>>
    @GET("api/v1/attendance/mobile/requests") suspend fun requests(@Header("Authorization") token: String): Response<MobileReply<List<MobileRequest>>>
    @POST("api/v1/attendance/mobile/requests") suspend fun submit(@Header("Authorization") token: String, @Body request: MobileRequestSubmission): Response<MobileReply<RequestReceipt>>
    @GET("api/v1/attendance/mobile/notifications") suspend fun notifications(@Header("Authorization") token: String): Response<MobileReply<List<MobileNotification>>>
    @GET("api/v1/attendance/mobile/on-duty") suspend fun onDuty(@Header("Authorization") token: String): Response<MobileReply<List<MobileDuty>>>
}
data class MobileReply<T>(val success: Boolean, val data: T?, val message: String?)
data class ProfileReply(val success: Boolean, val user: MobileProfile?)
data class MobileProfile(val id: Int, val first_name: String?, val last_name: String?, val email: String?, val phone: String?, val role: String?)
data class AttendanceReadModel(val today: String, val serverTimeMs: Long, val days: List<MobileAttendanceDay>)
data class MobileAttendanceDay(val date: String, val officeName: String?, val officeId: Long?, val state: String, val checkInTimeMs: Long?, val checkOutTimeMs: Long?, val workMinutes: Int?, val breakMinutes: Int?, val events: List<MobileEvent>)
data class MobileEvent(val uuid: String, val type: String, val timeMs: Long)
data class MobileRequest(val id: String, val type: String, val status: String, val requestDate: String, val description: String, val durationMinutes: Int?, val reviewedBy: String?, val reviewedAt: String?, val reviewRemarks: String?)
data class MobileRequestSubmission(val id: String, val type: String, val requestDate: String, val description: String, val durationMinutes: Int? = null, val subtype: String? = null, val requestedTime: String? = null)
data class RequestReceipt(val id: String)
data class MobileNotification(val id: String, val title: String, val description: String, val time: String?)
data class MobileDuty(val date: String, val dutyType: String, val startTime: String, val endTime: String, val location: String, val purpose: String, val assignedBy: String, val status: String, val remarks: String)

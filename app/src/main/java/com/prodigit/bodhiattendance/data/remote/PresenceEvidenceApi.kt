package com.prodigit.bodhiattendance.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class PresenceEvidenceItem(
    val id: String,
    @SerializedName("office_id") val officeId: Long?,
    @SerializedName("observed_elapsed_ms") val observedElapsedMs: Long,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("accuracy_meters") val accuracyMeters: Double,
    @SerializedName("wifi_matched") val wifiMatched: Boolean,
    val classification: String
)
data class PresenceEvidenceBatch(
    @SerializedName("device_id") val deviceId: String,
    val observations: List<PresenceEvidenceItem>
)
data class PresenceEvidenceUploadResponse(val success: Boolean, val accepted: List<String>?)

data class ArrivalCandidateItem(
    val id: String,
    @SerializedName("office_id") val officeId: Long,
    @SerializedName("boot_id") val bootId: String,
    @SerializedName("anchor_uuid") val anchorUuid: String,
    @SerializedName("observed_elapsed_ms") val observedElapsedMs: Long,
    @SerializedName("wifi_bssid") val wifiBssid: String
)
data class ArrivalCandidateBatch(
    @SerializedName("device_id") val deviceId: String,
    val candidates: List<ArrivalCandidateItem>
)

interface PresenceEvidenceApi {
    @POST("api/v1/attendance/presence-evidence/arrivals")
    suspend fun uploadArrivals(@Header("Authorization") token: String,
                              @Body batch: ArrivalCandidateBatch): Response<PresenceEvidenceUploadResponse>

    @POST("api/v1/attendance/presence-evidence")
    suspend fun upload(@Header("Authorization") token: String,
                       @Body batch: PresenceEvidenceBatch): Response<PresenceEvidenceUploadResponse>
}


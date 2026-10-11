package com.prodigit.bodhiattendance.data.remote

import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceBootstrapResponse
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceConfigResponse
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventRequest
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventResponse
import com.prodigit.bodhiattendance.data.remote.dto.attendance.CreateTimeAnchorRequest
import com.prodigit.bodhiattendance.data.remote.dto.attendance.CreateTimeAnchorResponse
import com.prodigit.bodhiattendance.data.remote.dto.attendance.TimeAnchorRequest
import com.prodigit.bodhiattendance.data.remote.dto.attendance.TimeAnchorResponse

import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query


interface AttendanceApiService {


    // ============================================================
    // CREATE TRUSTED TIME ANCHOR
    //
    // Creates a server-backed trusted time reference for the
    // current Android boot session.
    //
    // Android sends:
    //
    // - deviceId
    // - elapsedRealtimeMs
    // - bootId
    //
    // Express returns:
    //
    // - anchorUuid
    // - serverTime
    // - elapsedRealtimeMs
    // - bootId
    //
    // Phone wall-clock time is NOT authoritative.
    // ============================================================

    @POST("api/v1/attendance/time-anchor")
    suspend fun createTimeAnchor(

        @Header("Authorization")
        authorization: String,

        @Body
        request: TimeAnchorRequest

    ): Response<TimeAnchorResponse>


    // ============================================================
    // RECORD ATTENDANCE EVENT
    //
    // Records automatic attendance events:
    //
    // CHECK_IN
    // BREAK_START
    // BREAK_END
    // CHECK_OUT
    //
    // Express performs:
    //
    // - JWT validation
    // - registered-device validation
    // - attendance state-machine validation
    // - trusted-time reconstruction
    // - duplicate/idempotency validation
    // - GPS / Wi-Fi / optional BLE evidence validation
    // - PostgreSQL persistence
    //
    // IMPORTANT:
    //
    // Android stores the event in Room BEFORE attempting this API.
    //
    // If the network fails, the SAME event UUID and trusted-time
    // evidence are retried later.
    // ============================================================

    @POST("api/v1/attendance/event")
    suspend fun recordAttendanceEvent(

        @Header("Authorization")
        authorization: String,

        @Body
        request: AttendanceEventRequest

    ): Response<AttendanceEventResponse>


    // ============================================================
    // GET ACTIVE ATTENDANCE CONFIGURATION
    //
    // Android read-only configuration endpoint.
    //
    // Configuration is managed from React CRM and stored by
    // Express/PostgreSQL.
    //
    // Returns active:
    //
    // - attendance locations
    // - GPS / geofence configuration
    // - BLE beacon configuration
    // - Wi-Fi access-point configuration
    //
    // Android must NOT create / update / delete configuration.
    //
    // Final V1 attendance presence rule:
    //
    // geofenceVerified
    //          &&
    // wifiVerified
    //
    // BLE remains optional / disabled for V1.
    // ============================================================

    @GET("api/v1/attendance/locations/active")
    suspend fun getActiveAttendanceConfig(

        @Header("Authorization")
        authorization: String

    ): Response<AttendanceConfigResponse>


    // ============================================================
    // GET AUTHORITATIVE ATTENDANCE BOOTSTRAP STATE
    //
    // Used whenever Android cannot safely continue only from
    // locally persisted attendance-engine state.
    //
    // Examples:
    //
    // - first installation / no local engine state
    // - application data recovery
    // - phone reboot
    // - stored engine state is invalid
    // - stale local state
    // - backend rejected an attendance event
    // - engine-state persistence recovery
    //
    // Android sends:
    //
    // Authorization: Bearer <token>
    // deviceId: app-generated installation UUID
    //
    // Android does NOT send userId or companyId.
    //
    // Express derives:
    //
    // JWT
    //   ↓
    // authenticated user
    //   ↓
    // company
    //   ↓
    // registered device
    //   ↓
    // authoritative attendance state
    //
    // Expected states:
    //
    // NOT_CHECKED_IN
    // WORKING
    // ON_BREAK
    // CHECKED_OUT
    // ON_DUTY
    //
    // This response is consumed by:
    //
    // AttendanceBootstrapRepositoryImpl
    //            ↓
    // AttendanceEngineReconciliationManager
    //            ↓
    // AttendanceEngineRuntimeSession
    // ============================================================

    @GET("api/v1/attendance/bootstrap-state")
    suspend fun getBootstrapState(

        @Header("Authorization")
        authorization: String,

        @Query("deviceId")
        deviceId: String

    ): Response<AttendanceBootstrapResponse>

    @POST("api/v1/attendance/time-anchor")
    suspend fun createTimeAnchor(
        @Body request: CreateTimeAnchorRequest
    ): Response<CreateTimeAnchorResponse>
}
package com.prodigit.bodhiattendance.data.repository

import com.prodigit.bodhiattendance.data.remote.AttendanceApiService
import com.prodigit.bodhiattendance.domain.attendance.AttendanceBootstrapRepository
import com.prodigit.bodhiattendance.domain.attendance.AttendanceBootstrapState
import com.prodigit.bodhiattendance.domain.attendance.AttendanceEventType
import com.prodigit.bodhiattendance.domain.attendance.AttendanceState

import kotlinx.coroutines.CancellationException

import retrofit2.HttpException


// ============================================================
// ATTENDANCE BOOTSTRAP REPOSITORY IMPLEMENTATION
//
// Connects:
//
// AttendanceEngineReconciliationManager
//              ↓
// AttendanceBootstrapRepository
//              ↓
// Retrofit AttendanceApiService
//              ↓
// Express.js
//              ↓
// PostgreSQL
//
// Purpose:
//
// Obtain the authoritative current attendance state from Express
// and convert the API DTO into the domain model used by the
// attendance engine.
//
// IMPORTANT:
//
// Android does NOT decide the authoritative server state here.
//
// This repository only:
//
// - calls Express
// - validates required response data
// - converts String values into domain enums
// - returns normalized AttendanceBootstrapState
// ============================================================

class AttendanceBootstrapRepositoryImpl(

    private val api: AttendanceApiService

) : AttendanceBootstrapRepository {


    // ========================================================
    // GET CURRENT AUTHORITATIVE ATTENDANCE STATE
    // ========================================================

    override suspend fun getCurrentAttendanceState(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String

    ): Result<AttendanceBootstrapState> {


        return try {


            // =================================================
            // BASIC LOCAL VALIDATION
            //
            // userId and companyId are validated locally because
            // they belong to the currently authenticated Android
            // runtime context.
            //
            // They are NOT sent to Express.
            // =================================================

            if (token.isBlank()) {

                return Result.failure(

                    IllegalStateException(
                        "Authentication token is unavailable"
                    )
                )
            }


            if (userId <= 0) {

                return Result.failure(

                    IllegalStateException(
                        "Valid authenticated user ID is required"
                    )
                )
            }


            if (companyId <= 0) {

                return Result.failure(

                    IllegalStateException(
                        "Valid company ID is required"
                    )
                )
            }


            if (deviceId.isBlank()) {

                return Result.failure(

                    IllegalStateException(
                        "Registered device ID is required"
                    )
                )
            }


            // =================================================
            // CALL EXPRESS
            // =================================================

            val response =
                api.getBootstrapState(

                    authorization =
                        "Bearer $token",

                    deviceId =
                        deviceId
                )


            // =================================================
            // HTTP FAILURE
            //
            // Examples:
            //
            // 401 unauthorized
            // 403 device rejected
            // 404 resource unavailable
            // 500 backend failure
            // =================================================

            if (!response.isSuccessful) {

                return Result.failure(
                    HttpException(response)
                )
            }


            // =================================================
            // BODY VALIDATION
            // =================================================

            val body =
                response.body()
                    ?: return Result.failure(

                        IllegalStateException(
                            "Empty attendance bootstrap response"
                        )
                    )


            // =================================================
            // BUSINESS FAILURE
            //
            // HTTP may still be successful while backend reports
            // success=false.
            // =================================================

            if (!body.success) {

                return Result.failure(

                    IllegalStateException(
                        body.message
                    )
                )
            }


            // =================================================
            // REQUIRE BOOTSTRAP DATA
            // =================================================

            val data =
                body.data
                    ?: return Result.failure(

                        IllegalStateException(
                            "Attendance bootstrap data is missing"
                        )
                    )


            // =================================================
            // CONVERT ATTENDANCE STATE
            // =================================================

            val attendanceState =
                parseAttendanceState(
                    value =
                        data.attendanceState
                )


            // =================================================
            // CONVERT LATEST EVENT TYPE
            //
            // Null is valid when the employee has no attendance
            // event yet.
            // =================================================

            val latestEventType =
                parseAttendanceEventType(
                    value =
                        data.latestEventType ?: data.lastEvent?.eventType
                )


            // =================================================
            // BUILD DOMAIN MODEL
            // =================================================

            Result.success(

                AttendanceBootstrapState(

                    attendanceState =
                        attendanceState,

                    latestEventType =
                        latestEventType,

                    attendanceId =
                        data.attendanceId,

                    onDutyActive =
                        data.onDutyActive
                )
            )


        } catch (
            e: CancellationException
        ) {

            // Never swallow coroutine cancellation.
            throw e


        } catch (
            e: Exception
        ) {

            Result.failure(
                e
            )
        }
    }


    // ========================================================
    // PARSE ATTENDANCE STATE
    // ========================================================

    private fun parseAttendanceState(

        value: String

    ): AttendanceState {


        return when (
            value.trim()
                .uppercase()
        ) {


            "NOT_CHECKED_IN" ->
                AttendanceState.NOT_CHECKED_IN


            "WORKING" ->
                AttendanceState.WORKING


            "ON_BREAK" ->
                AttendanceState.ON_BREAK


            "CHECKED_OUT" ->
                AttendanceState.CHECKED_OUT


            "ON_DUTY" ->
                AttendanceState.ON_DUTY


            else -> {

                throw IllegalStateException(
                    "Unsupported attendance state returned by backend: $value"
                )
            }
        }
    }


    // ========================================================
    // PARSE ATTENDANCE EVENT TYPE
    // ========================================================

    private fun parseAttendanceEventType(

        value: String?

    ): AttendanceEventType? {


        if (
            value.isNullOrBlank()
        ) {

            return null
        }


        return when (
            value.trim()
                .uppercase()
        ) {


            "CHECK_IN" ->
                AttendanceEventType.CHECK_IN


            "BREAK_START" ->
                AttendanceEventType.BREAK_START


            "BREAK_END" ->
                AttendanceEventType.BREAK_END


            "CHECK_OUT" ->
                AttendanceEventType.CHECK_OUT


            else -> {

                throw IllegalStateException(
                    "Unsupported attendance event type returned by backend: $value"
                )
            }
        }
    }
}
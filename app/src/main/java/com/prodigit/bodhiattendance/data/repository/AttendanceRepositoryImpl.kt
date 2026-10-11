package com.prodigit.bodhiattendance.data.repository

import com.google.gson.Gson

import com.prodigit.bodhiattendance.data.remote.AttendanceApiService

import com.prodigit.bodhiattendance.data.remote.dto.attendance.TimeAnchorRequest
import com.prodigit.bodhiattendance.data.remote.dto.attendance.TimeAnchorResponse
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventRequest
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventResponse
import com.prodigit.bodhiattendance.domain.repository.AttendanceRepository

import kotlinx.coroutines.CancellationException

import retrofit2.HttpException


class AttendanceRepositoryImpl(

    private val api: AttendanceApiService

) : AttendanceRepository {


    // ============================================================
    // CREATE TRUSTED TIME ANCHOR
    //
    // Android provides:
    //
    // - registered device ID
    // - SystemClock.elapsedRealtime()
    // - current boot-session ID
    //
    // Express validates the registered device and creates a
    // server-controlled time anchor.
    //
    // The returned anchor will later be attached to attendance
    // events:
    //
    // CHECK_IN
    // BREAK_START
    // BREAK_END
    // CHECK_OUT
    // ============================================================

    override suspend fun createTimeAnchor(

        token: String,

        deviceId: String,

        elapsedRealtimeMs: Long,

        bootId: String

    ): Result<TimeAnchorResponse> {


        return try {


            // ====================================================
            // BUILD REQUEST
            // ====================================================

            val request =
                TimeAnchorRequest(

                    deviceId =
                        deviceId,

                    elapsedRealtimeMs =
                        elapsedRealtimeMs,

                    bootId =
                        bootId
                )


            // ====================================================
            // CALL EXPRESS API
            // ====================================================

            val response =
                api.createTimeAnchor(

                    authorization =
                        "Bearer $token",

                    request =
                        request
                )


            // ====================================================
            // HTTP SUCCESS
            // ====================================================

            if (response.isSuccessful) {


                val body =
                    response.body()


                if (body != null) {


                    Result.success(
                        body
                    )


                } else {


                    Result.failure(

                        IllegalStateException(
                            "Empty time anchor response"
                        )
                    )
                }


            } else {


                // =================================================
                // BACKEND ERROR
                //
                // Examples:
                //
                // DEVICE_NOT_REGISTERED
                // DEVICE_NOT_ONBOARDED
                // DEVICE_DISABLED
                // INVALID_ELAPSED_REALTIME
                // =================================================

                val errorBody =
                    response
                        .errorBody()
                        ?.string()


                val parsedError =
                    try {


                        Gson().fromJson(

                            errorBody,

                            TimeAnchorResponse::class.java
                        )


                    } catch (e: Exception) {


                        null
                    }


                // Keep the same pattern currently used by
                // DeviceRepositoryImpl.
                if (parsedError != null) {


                    Result.success(
                        parsedError
                    )


                } else {


                    Result.failure(
                        HttpException(response)
                    )
                }
            }


        } catch (e: CancellationException) {


            // Never convert coroutine cancellation into
            // an ordinary API/network error.

            throw e


        } catch (e: Exception) {


            Result.failure(e)
        }
    }

    // ============================================================
// RECORD ATTENDANCE EVENT
//
// Sends a prepared attendance event to Express.
//
// IMPORTANT:
//
// HTTP failure does not automatically mean network failure.
//
// Express can intentionally return non-2xx responses for
// business rules such as:
//
// INVALID_ATTENDANCE_TRANSITION
// DEVICE_DISABLED
// INVALID_TIME_ANCHOR
//
// We therefore attempt to parse the backend response first.
// ============================================================

    override suspend fun recordAttendanceEvent(

        token: String,

        request: AttendanceEventRequest

    ): Result<AttendanceEventResponse> {


        return try {


            // ====================================================
            // CALL EXPRESS
            // ====================================================

            val response =
                api.recordAttendanceEvent(

                    authorization =
                        "Bearer $token",

                    request =
                        request
                )


            // ====================================================
            // HTTP SUCCESS
            // ====================================================

            if (response.isSuccessful) {


                val body =
                    response.body()


                if (body != null) {


                    Result.success(
                        body
                    )


                } else {


                    Result.failure(

                        IllegalStateException(
                            "Empty attendance event response"
                        )
                    )
                }


            } else {


                // =================================================
                // BACKEND BUSINESS ERROR
                //
                // Example:
                //
                // {
                //   "success": false,
                //   "code": "INVALID_ATTENDANCE_TRANSITION",
                //   "message": "...",
                //   "currentState": "CHECK_OUT",
                //   "requestedEvent": "CHECK_IN",
                //   "allowedNextEvents": []
                // }
                //
                // This information is useful to Android, so don't
                // discard it just because HTTP status is non-2xx.
                // =================================================

                val errorBody =
                    response
                        .errorBody()
                        ?.string()


                val parsedError =
                    try {


                        Gson().fromJson(

                            errorBody,

                            AttendanceEventResponse::class.java
                        )


                    } catch (e: Exception) {


                        null
                    }


                if (parsedError != null) {


                    Result.success(
                        parsedError
                    )


                } else {


                    Result.failure(
                        HttpException(response)
                    )
                }
            }


        } catch (e: CancellationException) {


            // Coroutine cancellation must propagate normally.

            throw e


        } catch (e: Exception) {


            // Actual network / serialization / unexpected failure.

            Result.failure(e)
        }
    }
}
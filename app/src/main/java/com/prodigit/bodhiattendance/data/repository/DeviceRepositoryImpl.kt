package com.prodigit.bodhiattendance.data.repository

import com.google.gson.Gson
import com.prodigit.bodhiattendance.data.remote.DeviceApiService

import com.prodigit.bodhiattendance.data.remote.dto.CurrentDeviceResponse
import com.prodigit.bodhiattendance.data.remote.dto.DeviceOnboardingRequest
import com.prodigit.bodhiattendance.data.remote.dto.DeviceOnboardingResponse
import com.prodigit.bodhiattendance.data.remote.dto.DeviceRegisterRequest
import com.prodigit.bodhiattendance.data.remote.dto.DeviceRegisterResponse

import com.prodigit.bodhiattendance.domain.repository.DeviceRepository

import kotlinx.coroutines.CancellationException

import retrofit2.HttpException


class DeviceRepositoryImpl(
    private val api: DeviceApiService
) : DeviceRepository {


    // ============================================================
    // REGISTER DEVICE
    // ============================================================

    override suspend fun registerDevice(
        token: String,
        deviceId: String,
        deviceName: String
    ): Result<DeviceRegisterResponse> {

        return try {

            val request =
                DeviceRegisterRequest(
                    deviceId = deviceId,
                    deviceName = deviceName
                )


            val response =
                api.registerDevice(
                    authorization = "Bearer $token",
                    request = request
                )


            // ====================================================
            // HTTP SUCCESS
            // ====================================================

            if (response.isSuccessful) {

                val body =
                    response.body()


                if (body != null) {

                    Result.success(body)

                } else {

                    Result.failure(
                        IllegalStateException(
                            "Empty device registration response"
                        )
                    )
                }


            } else {

                // =================================================
                // BACKEND BUSINESS ERROR
                //
                // Examples:
                //
                // DEVICE_ALREADY_ASSIGNED
                // USER_HAS_ACTIVE_DEVICE
                // DEVICE_REGISTRATION_CONFLICT
                // =================================================

                val errorBody =
                    response
                        .errorBody()
                        ?.string()


                val parsedError =
                    try {

                        Gson().fromJson(
                            errorBody,
                            DeviceRegisterResponse::class.java
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

            throw e


        } catch (e: Exception) {

            Result.failure(e)
        }
    }


    // ============================================================
    // COMPLETE DEVICE ONBOARDING
    // ============================================================

    override suspend fun completeOnboarding(
        token: String,
        deviceId: String
    ): Result<DeviceOnboardingResponse> {

        return try {

            val request =
                DeviceOnboardingRequest(
                    deviceId = deviceId
                )


            val response =
                api.completeOnboarding(

                    authorization =
                        "Bearer $token",

                    request =
                        request
                )


            // ====================================================
            // HTTP SUCCESS
            //
            // Expected:
            //
            // ONBOARDING_COMPLETED
            // ALREADY_ONBOARDED
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
                            "Empty onboarding response"
                        )
                    )
                }


            } else {

                // =================================================
                // BACKEND BUSINESS ERROR
                //
                // We still parse the backend JSON so the ViewModel
                // can display/use the returned status.
                // =================================================

                val errorBody =
                    response
                        .errorBody()
                        ?.string()


                val parsedError =
                    try {

                        Gson().fromJson(
                            errorBody,
                            DeviceOnboardingResponse::class.java
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

            throw e


        } catch (e: Exception) {

            Result.failure(e)
        }
    }


    // ============================================================
    // GET CURRENT DEVICE
    //
    // Used during app startup.
    //
    // Android sends:
    //
    // JWT
    // +
    // local installation device ID
    //
    // Backend verifies that the exact device belongs to the
    // authenticated user.
    //
    // Possible statuses:
    //
    // DEVICE_FOUND
    // DEVICE_NOT_REGISTERED
    // DEVICE_DISABLED
    // ============================================================

    override suspend fun getCurrentDevice(
        token: String,
        deviceId: String
    ): Result<CurrentDeviceResponse> {

        return try {

            val response =
                api.getCurrentDevice(

                    authorization =
                        "Bearer $token",

                    deviceId =
                        deviceId
                )


            // ====================================================
            // HTTP SUCCESS
            //
            // Normally:
            //
            // 200 DEVICE_FOUND
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
                            "Empty current device response"
                        )
                    )
                }


            } else {

                // =================================================
                // IMPORTANT
                //
                // These are valid startup states, even though they
                // use non-2xx HTTP responses:
                //
                // 404 DEVICE_NOT_REGISTERED
                // 403 DEVICE_DISABLED
                //
                // Parse the JSON instead of throwing it away.
                // =================================================

                val errorBody =
                    response
                        .errorBody()
                        ?.string()


                val parsedError =
                    try {

                        Gson().fromJson(
                            errorBody,
                            CurrentDeviceResponse::class.java
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

            // Coroutine cancellation must not be converted
            // into a normal network failure.

            throw e


        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
package com.prodigit.bodhiattendance.ui.device

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.data.repository.DeviceRepositoryImpl
import com.prodigit.bodhiattendance.domain.repository.DeviceRepository
import kotlinx.coroutines.launch

class DeviceRegistrationViewModel(
    application: Application
) : AndroidViewModel(application) {

    // ============================================================
    // SESSION
    // ============================================================

    private val sessionManager =
        SessionManager(application.applicationContext)


    // ============================================================
    // DEVICE ID
    // ============================================================

    private val deviceIdManager =
        DeviceIdManager(application.applicationContext)


    // ============================================================
    // REPOSITORY
    // ============================================================

    private val repository: DeviceRepository =
        DeviceRepositoryImpl(
            RetrofitClient.deviceApi
        )


    // ============================================================
    // UI STATE
    // ============================================================

    var uiState =
        androidx.compose.runtime.mutableStateOf(
            DeviceRegistrationUiState()
        )
        private set


    // ============================================================
    // REGISTER DEVICE
    // ============================================================

    fun registerDevice() {

        if (uiState.value.isLoading) {
            return
        }


        // --------------------------------------------------------
        // GET SAVED JWT
        // --------------------------------------------------------

        val token =
            sessionManager.getToken()


        if (token.isNullOrBlank()) {

            uiState.value =
                uiState.value.copy(
                    error = "Login session not available"
                )

            return
        }


        // --------------------------------------------------------
        // GET INSTALLATION DEVICE ID
        // --------------------------------------------------------

        val deviceId =
            deviceIdManager.getOrCreateDeviceId()


        // --------------------------------------------------------
        // GET DEVICE NAME
        // --------------------------------------------------------

        val manufacturer =
            Build.MANUFACTURER
                ?.trim()
                .orEmpty()


        val model =
            Build.MODEL
                ?.trim()
                .orEmpty()


        val deviceName =
            when {

                manufacturer.isBlank() &&
                        model.isBlank() -> {

                    "Android Device"
                }


                model.startsWith(
                    manufacturer,
                    ignoreCase = true
                ) -> {

                    model
                }


                else -> {

                    "$manufacturer $model".trim()
                }
            }


        Log.d(
            "DEVICE_REGISTER",
            "Starting device registration"
        )

        Log.d(
            "DEVICE_REGISTER",
            "Device ID: $deviceId"
        )

        Log.d(
            "DEVICE_REGISTER",
            "Device Name: $deviceName"
        )


        // --------------------------------------------------------
        // CALL API
        // --------------------------------------------------------

        viewModelScope.launch {

            uiState.value =
                uiState.value.copy(
                    isLoading = true,
                    error = null,
                    message = null
                )


            val result =
                repository.registerDevice(
                    token = token,
                    deviceId = deviceId,
                    deviceName = deviceName
                )


            result.fold(

                onSuccess = { response ->

                    Log.d(
                        "DEVICE_REGISTER",
                        "Status: ${response.status}"
                    )


                    when (response.status) {

                        // ----------------------------------------
                        // NEW REGISTRATION
                        // ----------------------------------------

                        "REGISTERED" -> {

                            uiState.value =
                                DeviceRegistrationUiState(
                                    isLoading = false,
                                    isSuccessful = true,
                                    status = response.status,
                                    message = response.message,
                                    device = response.device
                                )
                        }


                        // ----------------------------------------
                        // EXISTING REGISTRATION
                        // ----------------------------------------

                        "ALREADY_REGISTERED" -> {

                            uiState.value =
                                DeviceRegistrationUiState(
                                    isLoading = false,
                                    isSuccessful = true,
                                    status = response.status,
                                    message = response.message,
                                    device = response.device
                                )
                        }


                        // ----------------------------------------
                        // DEVICE BELONGS TO ANOTHER USER
                        // ----------------------------------------

                        "DEVICE_ALREADY_ASSIGNED" -> {

                            uiState.value =
                                DeviceRegistrationUiState(
                                    isLoading = false,
                                    status = response.status,
                                    error = response.message
                                )
                        }


                        // ----------------------------------------
                        // USER HAS ANOTHER ACTIVE DEVICE
                        // ----------------------------------------

                        "USER_HAS_ACTIVE_DEVICE" -> {

                            uiState.value =
                                DeviceRegistrationUiState(
                                    isLoading = false,
                                    status = response.status,
                                    error = response.message
                                )
                        }


                        else -> {

                            uiState.value =
                                DeviceRegistrationUiState(
                                    isLoading = false,
                                    status = response.status,
                                    error = response.message
                                )
                        }
                    }
                },


                onFailure = { error ->

                    Log.e(
                        "DEVICE_REGISTER",
                        "Device registration failed",
                        error
                    )


                    uiState.value =
                        DeviceRegistrationUiState(
                            isLoading = false,
                            error =
                                "Unable to register device. Please try again."
                        )
                }
            )
        }
    }
}
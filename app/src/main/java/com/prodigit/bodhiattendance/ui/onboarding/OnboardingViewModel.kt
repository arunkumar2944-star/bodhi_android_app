package com.prodigit.bodhiattendance.ui.onboarding

import android.app.Application
import android.os.Build
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.permission.OnboardingPermissionManager
import com.prodigit.bodhiattendance.core.session.SessionManager

import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.repository.RegisteredDeviceStore
import com.prodigit.bodhiattendance.data.repository.DeviceRepositoryImpl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class OnboardingViewModel(
    application: Application
) : AndroidViewModel(application) {


    // ============================================================
    // APPLICATION CONTEXT
    // ============================================================

    private val appContext =
        application.applicationContext


    // ============================================================
    // PERMISSION MANAGER
    // ============================================================

    private val permissionManager =
        OnboardingPermissionManager(
            appContext
        )


    // ============================================================
    // SESSION MANAGER
    // ============================================================

    private val sessionManager =
        SessionManager(
            appContext
        )


    // ============================================================
    // DEVICE ID MANAGER
    // ============================================================

    private val registeredDeviceStore = RegisteredDeviceStore(appContext)

    private val deviceIdManager =
        DeviceIdManager(
            appContext
        )


    // ============================================================
    // DEVICE REPOSITORY
    //
    // Uses our existing:
    //
    // RetrofitClient.deviceApi
    //        ↓
    // DeviceApiService
    //        ↓
    // POST /api/v1/devices/register
    // ============================================================

    private val deviceRepository =
        DeviceRepositoryImpl(
            RetrofitClient.deviceApi
        )


    // ============================================================
    // UI STATE
    // ============================================================

    var uiState =
        androidx.compose.runtime.mutableStateOf(
            OnboardingUiState()
        )
        private set


    // ============================================================
    // INITIALIZATION
    // ============================================================

    init {

        // Check Location, Bluetooth and Notifications
        refreshSystemStatus()

        // Check Room offline database
        checkOfflineStorage()

        // Automatically verify/register this installation
        checkDeviceRegistration()
    }

    private fun checkDeviceRegistration() {

        // No valid login session means we cannot verify the device.
        val token =
            sessionManager.getToken()

        if (token.isNullOrBlank()) {

            Log.d(
                "DEVICE_REGISTER",
                "Skipping device verification - no session"
            )

            return
        }


        // We should already have an installation UUID after login.
        val deviceId =
            deviceIdManager.getDeviceId()


        if (deviceId.isNullOrBlank()) {

            Log.d(
                "DEVICE_REGISTER",
                "No installation device ID found"
            )

            return
        }


        Log.d(
            "DEVICE_REGISTER",
            "Automatically checking registered device"
        )

        Log.d(
            "DEVICE_REGISTER",
            "Local Device ID: $deviceId"
        )


        // Reuse our existing registration logic.
        registerDevice()
    }
    // ============================================================
    // REFRESH CURRENT DEVICE STATUS
    // ============================================================

    fun refreshSystemStatus() {

        uiState.value =
            uiState.value.copy(

                locationPermissionGranted =
                    permissionManager
                        .hasPreciseLocationPermission(),

                backgroundLocationGranted =
                    permissionManager
                        .hasBackgroundLocationPermission(),

                locationServiceEnabled =
                    permissionManager
                        .isLocationServiceEnabled(),

                bluetoothPermissionGranted =
                    permissionManager
                        .hasBluetoothPermission(),

                bluetoothEnabled =
                    permissionManager
                        .isBluetoothEnabled(),

                notificationPermissionGranted =
                    permissionManager
                        .hasNotificationPermission()
            )
    }


    // ============================================================
    // OFFLINE STORAGE CHECK
    // ============================================================

    private fun checkOfflineStorage() {

        viewModelScope.launch {

            Log.d(
                "ONBOARDING_DEBUG",
                "Checking offline Room storage"
            )


            val ready =
                withContext(Dispatchers.IO) {

                    try {

                        val database =
                            AttendanceDatabase.getInstance(
                                appContext
                            )


                        // Force Room / SQLite to actually open.
                        database
                            .openHelper
                            .writableDatabase


                        Log.d(
                            "ONBOARDING_DEBUG",
                            "Offline Room storage READY"
                        )


                        true

                    } catch (e: Exception) {

                        Log.e(
                            "ONBOARDING_DEBUG",
                            "Offline Room storage FAILED",
                            e
                        )


                        false
                    }
                }


            updateOfflineStorageState(
                ready
            )


            Log.d(
                "ONBOARDING_DEBUG",
                "offlineStorageReady = $ready"
            )
        }
    }


    // ============================================================
    // MANUAL OFFLINE STORAGE REFRESH
    // ============================================================

    fun refreshOfflineStorage() {

        checkOfflineStorage()
    }


    // ============================================================
    // OFFLINE STORAGE STATE
    // ============================================================

    fun updateOfflineStorageState(
        ready: Boolean
    ) {

        uiState.value =
            uiState.value.copy(
                offlineStorageReady = ready
            )
    }


    // ============================================================
    // REGISTER DEVICE
    // ============================================================

    fun registerDevice() {


        // --------------------------------------------------------
        // Prevent duplicate taps / simultaneous API calls.
        // --------------------------------------------------------

        if (
            uiState.value.deviceRegistrationLoading
        ) {

            return
        }


        // --------------------------------------------------------
        // Already registered during this ViewModel lifecycle.
        // --------------------------------------------------------

        if (
            uiState.value.deviceRegistered
        ) {

            return
        }


        clearError()


        // --------------------------------------------------------
        // GET JWT
        // --------------------------------------------------------

        val token =
            sessionManager.getToken()


        if (token.isNullOrBlank()) {

            Log.e(
                "DEVICE_REGISTER",
                "No login session token available"
            )


            setError(
                "Login session expired. Please sign in again."
            )

            return
        }


        // --------------------------------------------------------
        // GET INSTALLATION DEVICE ID
        //
        // IMPORTANT:
        // getOrCreateDeviceId() will reuse the existing UUID.
        // It will NOT generate another UUID if one already exists.
        // --------------------------------------------------------

        val deviceId =
            deviceIdManager
                .getOrCreateDeviceId()


        // --------------------------------------------------------
        // DEVICE NAME
        //
        // Example:
        //
        // Samsung SM-S918B
        // Google Pixel 8
        // --------------------------------------------------------

        val deviceName =
            buildDeviceName()


        Log.d(
            "DEVICE_REGISTER",
            "Preparing device registration"
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
        // SHOW LOADING
        // --------------------------------------------------------

        setDeviceRegistrationLoading(
            true
        )


        // --------------------------------------------------------
        // CALL EXPRESS API
        // --------------------------------------------------------

        viewModelScope.launch {

            try {

                val result =
                    deviceRepository
                        .registerDevice(

                            token =
                                token,

                            deviceId =
                                deviceId,

                            deviceName =
                                deviceName
                        )


                result.fold(

                    // =================================================
                    // REPOSITORY RETURNED RESPONSE
                    // =================================================

                    onSuccess = { response ->


                        Log.d(
                            "DEVICE_REGISTER",
                            "Response status: ${response.status}"
                        )

                        Log.d(
                            "DEVICE_REGISTER",
                            "Response message: ${response.message}"
                        )


                        val registeredDevice =
                            response.device


                        // =================================================
                        // VALID SUCCESS STATUS
                        // =================================================

                        val acceptedStatus =

                            response.status ==
                                    "REGISTERED" ||

                                    response.status ==
                                    "ALREADY_REGISTERED"


                        // =================================================
                        // VERIFY SAME INSTALLATION UUID
                        // =================================================

                        val sameDevice =

                            registeredDevice != null &&

                                    registeredDevice.deviceId ==
                                    deviceId


                        // =================================================
                        // VERIFY DEVICE ACTIVE
                        // =================================================

                        val activeDevice =

                            registeredDevice
                                ?.isActive == true


                        // =================================================
                        // FINAL REGISTRATION DECISION
                        // =================================================

                        if (
                            response.success &&
                            acceptedStatus &&
                            sameDevice &&
                            activeDevice &&
                            registeredDevice?.userId == sessionManager.getUserId()
                        ) {


                            Log.d(
                                "DEVICE_REGISTER",
                                "Device verified successfully"
                            )


                            Log.d(
                                "DEVICE_REGISTER",
                                "Registered DB ID: ${registeredDevice?.id}"
                            )


                            registeredDeviceStore.saveVerified(
                                userId = sessionManager.getUserId(),
                                registrationId = registeredDevice!!.id,
                                deviceId = deviceId,
                                companyId = registeredDevice.companyId,
                                onboarded = registeredDevice.isOnboarded
                            )

                            updateDeviceRegistrationState(
                                true
                            )


                            clearError()


                        } else {


                            // ---------------------------------------------
                            // Important:
                            //
                            // DeviceRepositoryImpl can return a parsed
                            // backend business error as Result.success().
                            //
                            // Example:
                            //
                            // DEVICE_ALREADY_ASSIGNED
                            //
                            // Therefore Result.success() alone DOES NOT mean
                            // that onboarding device registration succeeded.
                            // ---------------------------------------------


                            Log.e(
                                "DEVICE_REGISTER",
                                "Device registration rejected: ${response.status}"
                            )


                            updateDeviceRegistrationState(
                                false
                            )


                            setError(

                                response.message
                                    .ifBlank {

                                        "Device registration failed"
                                    }
                            )
                        }
                    },


                    // =================================================
                    // NETWORK / UNEXPECTED FAILURE
                    // =================================================

                    onFailure = { exception ->


                        Log.e(
                            "DEVICE_REGISTER",
                            "Device registration failed",
                            exception
                        )


                        updateDeviceRegistrationState(
                            false
                        )


                        setError(

                            when (exception) {

                                is java.net.SocketTimeoutException ->

                                    "Server connection timed out. Please try again."


                                is java.net.ConnectException ->

                                    "Unable to connect to the server."


                                is java.net.UnknownHostException ->

                                    "Network connection unavailable."


                                else ->

                                    exception.message
                                        ?: "Unable to register device."
                            }
                        )
                    }
                )


            } catch (e: CancellationException) {

                // Never swallow coroutine cancellation.
                throw e


            } catch (e: Exception) {


                Log.e(
                    "DEVICE_REGISTER",
                    "Unexpected registration error",
                    e
                )


                updateDeviceRegistrationState(
                    false
                )


                setError(
                    "Unable to register device."
                )
            }
        }
    }

    fun completeOnboarding() {

        // ============================================================
        // DON'T ALLOW DUPLICATE REQUESTS
        // ============================================================

        if (uiState.value.onboardingCompleting) {
            return
        }


        // ============================================================
        // ALL REQUIREMENTS MUST BE COMPLETE
        // ============================================================

        if (!uiState.value.canContinue) {

            setError(
                "Complete all required setup before continuing."
            )

            return
        }

        // ============================================================
        // JWT
        // ============================================================

        val token =
            sessionManager.getToken()


        if (token.isNullOrBlank()) {

            setError(
                "Login session expired. Please sign in again."
            )

            return
        }


        // ============================================================
        // DEVICE ID
        // ============================================================

        val deviceId =
            deviceIdManager.getDeviceId()


        if (deviceId.isNullOrBlank()) {

            setError(
                "Device ID is unavailable."
            )

            return
        }


        setOnboardingCompleting(
            true
        )


        clearError()


        viewModelScope.launch {

            try {

                Log.d(
                    "ONBOARDING_COMPLETE",
                    "Completing onboarding"
                )


                val result =
                    deviceRepository
                        .completeOnboarding(

                            token =
                                token,

                            deviceId =
                                deviceId
                        )


                result.fold(

                    onSuccess = { response ->


                        Log.d(
                            "ONBOARDING_COMPLETE",
                            "Status: ${response.status}"
                        )


                        val acceptedStatus =

                            response.status ==
                                    "ONBOARDING_COMPLETED" ||

                                    response.status ==
                                    "ALREADY_ONBOARDED"


                        val returnedDevice =
                            response.device


                        val sameDevice =

                            returnedDevice != null &&

                                    returnedDevice.deviceId ==
                                    deviceId


                        val activeDevice =

                            returnedDevice
                                ?.isActive == true


                        val onboarded =

                            returnedDevice
                                ?.isOnboarded == true


                        // =============================================
                        // FINAL SERVER VERIFICATION
                        // =============================================

                        if (
                            response.success &&
                            acceptedStatus &&
                            sameDevice &&
                            activeDevice &&
                            onboarded
                        ) {

                            Log.d(
                                "ONBOARDING_COMPLETE",
                                "Onboarding completed successfully"
                            )


                            markOnboardingCompleted()


                        } else {

                            setOnboardingCompleting(
                                false
                            )


                            setError(

                                response.message
                                    .ifBlank {

                                        "Unable to complete setup."
                                    }
                            )
                        }
                    },


                    onFailure = { exception ->


                        Log.e(
                            "ONBOARDING_COMPLETE",
                            "Onboarding request failed",
                            exception
                        )


                        setOnboardingCompleting(
                            false
                        )


                        setError(

                            exception.message
                                ?: "Unable to complete setup."
                        )
                    }
                )


            } catch (e: CancellationException) {

                throw e


            } catch (e: Exception) {


                Log.e(
                    "ONBOARDING_COMPLETE",
                    "Unexpected onboarding error",
                    e
                )


                setOnboardingCompleting(
                    false
                )


                setError(
                    "Unable to complete setup."
                )
            }
        }
    }

    // ============================================================
    // BUILD DEVICE NAME
    // ============================================================

    private fun buildDeviceName(): String {

        val manufacturer =
            Build.MANUFACTURER
                ?.trim()
                .orEmpty()


        val model =
            Build.MODEL
                ?.trim()
                .orEmpty()


        return when {

            manufacturer.isBlank() &&
                    model.isBlank() ->

                "Android Device"


            manufacturer.isBlank() ->

                model


            model.isBlank() ->

                manufacturer


            model.startsWith(
                manufacturer,
                ignoreCase = true
            ) ->

                model


            else ->

                "$manufacturer $model"
        }
    }


    // ============================================================
    // DEVICE REGISTRATION STATE
    // ============================================================

    fun updateDeviceRegistrationState(
        registered: Boolean
    ) {

        uiState.value =
            uiState.value.copy(

                deviceRegistered =
                    registered,

                deviceRegistrationLoading =
                    false
            )
    }


    fun setDeviceRegistrationLoading(
        loading: Boolean
    ) {

        uiState.value =
            uiState.value.copy(
                deviceRegistrationLoading =
                    loading
            )
    }


    // ============================================================
    // ERROR
    // ============================================================

    fun setError(
        message: String
    ) {

        uiState.value =
            uiState.value.copy(
                errorMessage =
                    message
            )
    }


    fun clearError() {

        uiState.value =
            uiState.value.copy(
                errorMessage =
                    null
            )
    }


    // ============================================================
    // ONBOARDING
    // ============================================================

    fun setOnboardingCompleting(
        completing: Boolean
    ) {

        uiState.value =
            uiState.value.copy(
                onboardingCompleting =
                    completing
            )
    }


    fun markOnboardingCompleted() {

        uiState.value =
            uiState.value.copy(

                onboardingCompleting =
                    false,

                onboardingCompleted =
                    true,

                errorMessage =
                    null
            )
    }
}
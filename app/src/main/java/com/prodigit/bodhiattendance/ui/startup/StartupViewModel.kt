package com.prodigit.bodhiattendance.ui.startup

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.monitoring.AttendanceMonitorStartGate
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager

import com.prodigit.bodhiattendance.data.repository.DeviceRepositoryImpl

import com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineBootstrapResult
import com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeProvider
import com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeStartResult

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class StartupViewModel(
    application: Application
) : AndroidViewModel(application) {


    private val appContext =
        application.applicationContext


    // ============================================================
    // SESSION
    // ============================================================

    private val tokenManager = AndroidTokenManager(appContext)

    private val sessionManager =
        SessionManager(
            appContext
        )


    // ============================================================
    // DEVICE ID
    // ============================================================

    private val deviceIdManager =
        DeviceIdManager(
            appContext
        )


    // ============================================================
    // DEVICE REPOSITORY
    // ============================================================

    private val deviceRepository =
        DeviceRepositoryImpl(
            RetrofitClient.deviceApi
        )


    // ============================================================
    // STARTUP RETRY JOB
    //
    // Prevents multiple parallel retry coroutines.
    // ============================================================

    private var retryJob: Job? =
        null


    // ============================================================
    // ATTENDANCE ENGINE RUNTIME PROVIDER
    //
    // Shared dependency provider.
    //
    // StartupViewModel does NOT create duplicate:
    //
    // - BootSessionManager
    // - TimeAnchorManager
    // - Room repositories
    // - AttendanceRepository
    //
    // ============================================================

    private val attendanceRuntimeProvider =
        AttendanceEngineRuntimeProvider(
            appContext
        )


    private val attendanceRuntimeSession =
        attendanceRuntimeProvider
            .runtimeSession


    private val attendanceReconciliationManager =
        attendanceRuntimeProvider
            .reconciliationManager


    // ============================================================
    // TRUSTED TIME MANAGER
    //
    // Uses the same TimeAnchorManager owned by the central
    // AttendanceEngineRuntimeProvider.
    // ============================================================

    private val timeAnchorManager =
        attendanceRuntimeProvider
            .trustedTimeManager


    // ============================================================
    // DESTINATION
    // ============================================================

    private val _destination =
        MutableStateFlow(
            StartupDestination.LOADING
        )


    val destination:
            StateFlow<StartupDestination> =
        _destination.asStateFlow()


    // ============================================================
    // NETWORK ERROR
    // ============================================================

    private val _networkError =
        MutableStateFlow(false)


    val networkError:
            StateFlow<Boolean> =
        _networkError.asStateFlow()


    // ============================================================
    // INITIALIZATION
    // ============================================================

    init {
        // All app launches must validate the session, registered device,
        // trusted time and attendance runtime before opening the dashboard.
        checkStartupState()
    }


    private fun checkStartupState() {

        viewModelScope.launch {

            Log.d(
                "STARTUP_DEBUG",
                "Bodhi launch screen started"
            )


            // ====================================================
            // KEEP EXISTING 9 SECOND LAUNCH SCREEN
            // ====================================================

            delay(9000)


            Log.d(
                "STARTUP_DEBUG",
                "9 second launch period completed"
            )


            // ====================================================
            // WAIT FOR INTERNET
            // ====================================================

            waitForInternet()


            Log.d(
                "STARTUP_DEBUG",
                "Internet connection available"
            )


            _networkError.value =
                false


            // ====================================================
            // SESSION + DEVICE VALIDATION
            // ====================================================

            checkSessionAndDevice()
        }
    }


    // ============================================================
    // WAIT FOR INTERNET
    // ============================================================

    private suspend fun waitForInternet() {

        while (
            !isInternetAvailable()
        ) {

            Log.d(
                "STARTUP_DEBUG",
                "No internet - staying on launch screen"
            )


            _networkError.value =
                true


            _destination.value =
                StartupDestination.LOADING


            delay(3000)
        }


        _networkError.value =
            false


        Log.d(
            "STARTUP_DEBUG",
            "Internet restored"
        )
    }


    // ============================================================
    // SESSION + DEVICE STARTUP VALIDATION
    // ============================================================

    private suspend fun checkSessionAndDevice() {


        // ========================================================
        // STEP 1 — LOCAL SESSION
        // ========================================================

        val token = tokenManager.usableAccessToken()
        if (token.isNullOrBlank()) {
            if (tokenManager.hasRefreshCredential()) {
                Log.w("STARTUP_DEBUG", "Token temporarily unavailable; retrying without clearing saved identity")
                _networkError.value = true
                _destination.value = StartupDestination.LOADING
                retryDeviceValidation()
            } else {
                Log.w("STARTUP_DEBUG", "No valid access or refresh token; login required")
                _destination.value = StartupDestination.LOGIN
            }
            return
        }

        // ========================================================
        // STEP 3 — INSTALLATION DEVICE ID
        // ========================================================

        val deviceId =
            deviceIdManager
                .getOrCreateDeviceId()


        Log.d(
            "STARTUP_DEBUG",
            "Checking registered device: $deviceId"
        )


        // ========================================================
        // STEP 4 — DEVICE BACKEND VALIDATION
        // ========================================================

        val result =
            deviceRepository
                .getCurrentDevice(

                    token =
                        token,

                    deviceId =
                        deviceId
                )


        // ========================================================
        // NETWORK / API FAILURE
        // ========================================================

        result.onFailure { error ->

            Log.e(
                "STARTUP_DEBUG",
                "Current device check failed",
                error
            )


            _networkError.value =
                true


            _destination.value =
                StartupDestination.LOADING


            retryDeviceValidation()
        }


        // ========================================================
        // BACKEND RESPONSE
        // ========================================================

        result.onSuccess { response ->


            Log.d(
                "STARTUP_DEBUG",
                "Device API status: ${response.status}"
            )


            when (
                response.status
            ) {


                // =================================================
                // DEVICE FOUND
                // =================================================

                "DEVICE_FOUND" -> {


                    val device =
                        response.device


                    // =============================================
                    // DEVICE BODY VALIDATION
                    // =============================================

                    if (
                        device == null
                    ) {

                        Log.e(
                            "STARTUP_DEBUG",
                            "DEVICE_FOUND but device body is null"
                        )


                        _networkError.value =
                            true


                        _destination.value =
                            StartupDestination.LOADING


                        retryDeviceValidation()

                        return@onSuccess
                    }


                    // =============================================
                    // DEVICE ID MUST MATCH
                    // =============================================

                    if (
                        device.deviceId !=
                        deviceId
                    ) {

                        Log.e(
                            "STARTUP_DEBUG",
                            "Device ID mismatch"
                        )


                        _destination.value =
                            StartupDestination.ONBOARDING

                        return@onSuccess
                    }


                    // =============================================
                    // DEVICE ACTIVE
                    // =============================================

                    if (
                        !device.isActive
                    ) {

                        Log.d(
                            "STARTUP_DEBUG",
                            "Device is disabled"
                        )


                        _destination.value =
                            StartupDestination.DEVICE_BLOCKED

                        return@onSuccess
                    }


                    // =============================================
                    // ONBOARDING
                    // =============================================

                    if (
                        !device.isOnboarded
                    ) {

                        Log.d(
                            "STARTUP_DEBUG",
                            "Device found but onboarding incomplete"
                        )


                        _destination.value =
                            StartupDestination.ONBOARDING

                        return@onSuccess
                    }


                    // =============================================
                    // USER / COMPANY
                    // =============================================

                    val userId =
                        sessionManager
                            .getUserId()


                    val companyId =
                        sessionManager
                            .getCompanyId()


                    if (
                        userId <= 0
                    ) {

                        Log.e(
                            "STARTUP_DEBUG",
                            "Invalid authenticated user ID during attendance startup"
                        )


                        sessionManager
                            .clearSession()


                        _destination.value =
                            StartupDestination.LOGIN

                        return@onSuccess
                    }


                    if (
                        companyId <= 0
                    ) {

                        Log.e(
                            "STARTUP_DEBUG",
                            "Invalid company ID during attendance startup"
                        )


                        sessionManager
                            .clearSession()


                        _destination.value =
                            StartupDestination.LOGIN

                        return@onSuccess
                    }


                    // Provision a rotating refresh credential after the
                    // backend has confirmed this registered device.
                    if (!tokenManager.activateOnApprovedDevice()) {
                        Log.w("STARTUP_DEBUG", "Android refresh session unavailable; retrying before runtime activation")
                        _networkError.value = true
                        _destination.value = StartupDestination.LOADING
                        retryDeviceValidation()
                        return@onSuccess
                    }

                    // =================================================
                    // RESTORE LOCAL RUNTIME BEFORE SELECTING ANCHOR OWNER
                    // =================================================

                    Log.d(
                        "STARTUP_DEBUG",
                        "Checking local attendance runtime; reconciliation owns fresh anchor creation"
                    )


                    val attendanceStartResult =
                        attendanceRuntimeSession
                            .initialize(

                                userId =
                                    userId,

                                deviceId =
                                    deviceId
                            )


                    Log.d(
                        "STARTUP_DEBUG",
                        "Attendance runtime initialization result: $attendanceStartResult"
                    )


                    when (
                        attendanceStartResult
                    ) {


                        // =============================================
                        // RUNTIME READY
                        // =============================================

                        is AttendanceEngineRuntimeStartResult.Ready -> {

                            // =================================================
                            // RESTORED RUNTIME — ENSURE TRUSTED TIME
                            // =================================================

                            Log.d(
                                "STARTUP_DEBUG",
                                "Local runtime restored - creating fresh server trusted-time anchor"
                            )


                            val anchorResult =
                                timeAnchorManager
                                    .createFreshAnchor(

                                        token =
                                            token,

                                        deviceId =
                                            deviceId
                                    )


                            // =================================================
                            // TRUSTED TIME FAILURE
                            // =================================================

                            if (
                                anchorResult.isFailure
                            ) {

                                val error =
                                    anchorResult
                                        .exceptionOrNull()


                                Log.e(
                                    "STARTUP_DEBUG",
                                    "Trusted TimeAnchor initialization failed",
                                    error
                                )


                                _networkError.value =
                                    true


                                _destination.value =
                                    StartupDestination.LOADING


                                retryDeviceValidation()

                                return@onSuccess
                            }


                            // =================================================
                            // TRUSTED TIME READY
                            // =================================================

                            val anchor =
                                anchorResult
                                    .getOrThrow()


                            Log.d(
                                "STARTUP_DEBUG",
                                "Trusted TimeAnchor READY"
                            )


                            Log.d(
                                "STARTUP_DEBUG",
                                "TimeAnchor UUID: ${anchor.anchorUuid}"
                            )


                            Log.d(
                                "STARTUP_DEBUG",
                                "TimeAnchor bootId: ${anchor.bootId}"
                            )


                            Log.d(
                                "STARTUP_DEBUG",
                                "TimeAnchor elapsedRealtimeMs: ${anchor.elapsedRealtimeMs}"
                            )


                            Log.d(
                                "STARTUP_DEBUG",
                                "TimeAnchor serverTime: ${anchor.serverTime}"
                            )




                            Log.d(
                                "STARTUP_DEBUG",
                                "Attendance runtime READY - moving to DASHBOARD"
                            )


                            _networkError.value =
                                false


                            _destination.value =
                                StartupDestination.DASHBOARD

                            // Independent foreground location observer; never checks in on dashboard navigation.
                            AttendanceMonitorStartGate.onStartupReady(appContext)
                        }


                        // =============================================
                        // STATE UNAVAILABLE
                        // =============================================

                        AttendanceEngineRuntimeStartResult.StateUnavailable -> {


                            Log.d(
                                "STARTUP_DEBUG",
                                "Attendance runtime state unavailable - starting reconciliation"
                            )


                            reconcileAttendanceRuntime(

                                token =
                                    token,

                                userId =
                                    userId,

                                companyId =
                                    companyId,

                                deviceId =
                                    deviceId
                            )
                        }


                        // =============================================
                        // PHONE REBOOT
                        // =============================================

                        is AttendanceEngineRuntimeStartResult
                        .RebootReconciliationRequired -> {


                            Log.d(
                                "STARTUP_DEBUG",
                                "Attendance runtime reboot detected - starting reconciliation"
                            )


                            reconcileAttendanceRuntime(

                                token =
                                    token,

                                userId =
                                    userId,

                                companyId =
                                    companyId,

                                deviceId =
                                    deviceId
                            )
                        }


                        // =============================================
                        // INVALID LOCAL STATE
                        // =============================================

                        is AttendanceEngineRuntimeStartResult
                        .InvalidStateReconciliationRequired -> {


                            Log.e(
                                "STARTUP_DEBUG",
                                "Attendance runtime invalid state: ${attendanceStartResult.reason}"
                            )


                            reconcileAttendanceRuntime(

                                token =
                                    token,

                                userId =
                                    userId,

                                companyId =
                                    companyId,

                                deviceId =
                                    deviceId
                            )
                        }


                        // =============================================
                        // FAILURE
                        // =============================================

                        is AttendanceEngineRuntimeStartResult.Failure -> {


                            Log.e(
                                "STARTUP_DEBUG",
                                "Attendance runtime initialization failed",
                                attendanceStartResult.error
                            )


                            _networkError.value =
                                true


                            _destination.value =
                                StartupDestination.LOADING


                            retryDeviceValidation()
                        }
                    }
                }


                // =================================================
                // DEVICE NOT REGISTERED
                // =================================================

                "DEVICE_NOT_REGISTERED" -> {


                    Log.d(
                        "STARTUP_DEBUG",
                        "This installation is not registered - moving to ONBOARDING"
                    )


                    _networkError.value =
                        false


                    _destination.value =
                        StartupDestination.ONBOARDING
                }


                // =================================================
                // DEVICE DISABLED
                // =================================================

                "DEVICE_DISABLED" -> {


                    Log.d(
                        "STARTUP_DEBUG",
                        "Registered device is disabled"
                    )


                    _networkError.value =
                        false


                    _destination.value =
                        StartupDestination.DEVICE_BLOCKED
                }


                // =================================================
                // UNKNOWN DEVICE RESPONSE
                // =================================================

                else -> {


                    Log.e(
                        "STARTUP_DEBUG",
                        "Unexpected device status: ${response.status}"
                    )


                    _networkError.value =
                        true


                    _destination.value =
                        StartupDestination.LOADING


                    retryDeviceValidation()
                }
            }
        }
    }


    // ============================================================
    // RECONCILE ATTENDANCE RUNTIME
    // ============================================================

    private suspend fun reconcileAttendanceRuntime(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String

    ) {


        Log.d(
            "STARTUP_DEBUG",
            "Starting attendance runtime reconciliation"
        )


        val reconciliationResult =
            attendanceReconciliationManager
                .reconcile(

                    token =
                        token,

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId
                )


        // ========================================================
        // RECONCILIATION SUCCESS
        // ========================================================

        if (
            reconciliationResult
                    is AttendanceEngineBootstrapResult.Ready
        ) {


            Log.d(
                "STARTUP_DEBUG",
                "Attendance reconciliation completed successfully"
            )


            val runtimeSnapshot =
                attendanceRuntimeSession
                    .applyReconciledState(

                        userId =
                            userId,

                        deviceId =
                            deviceId,

                        bootstrapResult =
                            reconciliationResult
                    )


            Log.d(
                "STARTUP_DEBUG",
                "Attendance runtime reconciled: $runtimeSnapshot"
            )


            _networkError.value =
                false


            _destination.value =
                StartupDestination.DASHBOARD

            AttendanceMonitorStartGate.onStartupReady(appContext)


            return
        }


        // ========================================================
        // RECONCILIATION NOT READY
        // ========================================================

        Log.e(
            "STARTUP_DEBUG",
            "Attendance reconciliation did not produce READY state: $reconciliationResult"
        )


        _networkError.value =
            true


        _destination.value =
            StartupDestination.LOADING


        retryDeviceValidation()
    }


    // ============================================================
    // RETRY STARTUP
    // ============================================================

    private fun retryDeviceValidation() {


        if (
            retryJob?.isActive ==
            true
        ) {


            Log.d(
                "STARTUP_DEBUG",
                "Startup retry already active - skipping duplicate retry"
            )


            return
        }


        retryJob =
            viewModelScope.launch {


                Log.d(
                    "STARTUP_DEBUG",
                    "Startup retry scheduled"
                )


                delay(3000)


                waitForInternet()


                _networkError.value =
                    false


                Log.d(
                    "STARTUP_DEBUG",
                    "Retrying session + device + trusted-time + attendance startup"
                )


                // Release retry guard before retrying.
                retryJob =
                    null


                checkSessionAndDevice()
            }
    }


    // ============================================================
    // INTERNET CHECK
    // ============================================================

    private fun isInternetAvailable(): Boolean {


        val connectivityManager =
            appContext.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager


        val network =
            connectivityManager
                .activeNetwork
                ?: return false


        val capabilities =
            connectivityManager
                .getNetworkCapabilities(
                    network
                )
                ?: return false


        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) &&
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
    }
}
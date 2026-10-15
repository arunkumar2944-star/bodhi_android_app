package com.prodigit.bodhiattendance.ui.systemhealth

import android.Manifest
import android.app.Application
import android.app.ActivityManager
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.util.Log

import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.SessionManager

import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.entity.AttendanceEventEntity
import com.prodigit.bodhiattendance.data.local.repository.OfflineAttendanceRepository

import com.prodigit.bodhiattendance.data.local.security.BootSessionManager
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorStorage

import com.prodigit.bodhiattendance.data.repository.DeviceRepositoryImpl
import com.prodigit.bodhiattendance.domain.repository.DeviceRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import java.util.UUID


// ============================================================
// SYSTEM HEALTH VIEW MODEL
//
// Responsible for checking:
//
// - Location permission
// - Background location permission
// - Location service
// - Bluetooth permission
// - Bluetooth service
// - Internet connection
// - Battery optimization
// - Trusted time
// - Registered device
// - Offline Room storage
// - Attendance synchronization queue
//
// The following checks remain pending until CRM configuration
// APIs are available:
//
// - Attendance configuration
// - Office BLE beacon
// - Office Wi-Fi
//
// IMPORTANT:
//
// System Health is diagnostic.
//
// It must NOT:
//
// - register a new device
// - create a new device ID
// - create a new trusted time anchor
// - mark attendance
// ============================================================

class SystemHealthViewModel(
    application: Application
) : AndroidViewModel(application) {


    // ============================================================
    // APPLICATION CONTEXT
    // ============================================================

    private val appContext =
        application.applicationContext


    // ============================================================
    // SESSION
    //
    // Used only to read the currently authenticated JWT.
    // ============================================================

    private val sessionManager =
        SessionManager(
            appContext
        )


    // ============================================================
    // DEVICE ID
    //
    // IMPORTANT:
    //
    // System Health only READS the existing installation ID.
    //
    // It must NOT call getOrCreateDeviceId() while performing
    // device-health verification.
    // ============================================================

    private val deviceIdManager =
        DeviceIdManager(
            appContext
        )


    // ============================================================
    // DEVICE REPOSITORY
    //
    // Express remains authoritative for whether the current
    // installation is:
    //
    // - registered
    // - active
    // - onboarded
    // ============================================================

    private val deviceRepository: DeviceRepository =
        DeviceRepositoryImpl(
            RetrofitClient.deviceApi
        )


    // ============================================================
    // TRUSTED TIME COMPONENTS
    //
    // System Health only READS trusted-time state.
    //
    // It does NOT create a new time anchor.
    // ============================================================

    private val bootSessionManager =
        BootSessionManager(
            appContext
        )


    private val timeAnchorStorage =
        TimeAnchorStorage(
            appContext
        )


    // ============================================================
    // LOCAL DATABASE
    // ============================================================

    private val attendanceDatabase =
        AttendanceDatabase.getInstance(
            appContext
        )


    // ============================================================
    // OFFLINE ATTENDANCE REPOSITORY
    // ============================================================

    private val offlineAttendanceRepository =
        OfflineAttendanceRepository(
            attendanceDatabase.attendanceEventDao()
        )


    // ============================================================
    // UI STATE
    // ============================================================

    private val _uiState =
        MutableStateFlow(
            SystemHealthUiState()
        )


    val uiState: StateFlow<SystemHealthUiState> =
        _uiState.asStateFlow()


    // ============================================================
    // INITIALIZATION
    // ============================================================

    init {

        // Perform the initial complete health check.

        refreshHealth()


        // Continuously observe Room synchronization state.

        observeSyncStatus()
    }


    // ============================================================
    // DEVELOPMENT TEST ONLY
    //
    // Creates one temporary offline attendance event.
    //
    // PURPOSE:
    //
    // AttendanceEventEntity
    //          ↓
    // OfflineAttendanceRepository
    //          ↓
    // Room
    //          ↓
    // observeUnsyncedCount()
    //          ↓
    // System Health
    //
    // REMOVE BEFORE PRODUCTION RELEASE.
    // ============================================================

    // ============================================================
    // REFRESH SYSTEM HEALTH
    // ============================================================

    /**
     * Call ONLY after the employee's assigned office policy is verified from
     * CRM / trustworthy cached configuration. Never derive this from SSID,
     * connected Wi-Fi, GPS position, or a hard-coded location name.
     *
     * required == null: assignment unknown
     * required == true: office Wi-Fi required (verification still pending)
     * required == false: office Wi-Fi not required
     */
    fun setVerifiedOfficeWifiRequirement(required: Boolean?) {
        _uiState.value = _uiState.value.copy(
            officeWifiRequirement = required,
            officeWifi = wifiPolicyDiagnostic(required)
        )
    }

    private fun wifiPolicyDiagnostic(required: Boolean?): HealthItemState =
        when (required) {
            true -> HealthItemState(
                status = HealthStatus.NOT_CONFIGURED,
                title = "Office Wi-Fi",
                message = "Required; approved router verification not connected"
            )
            false -> HealthItemState(
                status = HealthStatus.NOT_REQUIRED,
                title = "Office Wi-Fi",
                message = "Not required at the assigned GPS-only location"
            )
            null -> HealthItemState(
                status = HealthStatus.NOT_CONFIGURED,
                title = "Office Wi-Fi",
                message = "Office policy not yet verified"
            )
        }

    fun refreshHealth() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isRefreshing = true
                )


            try {

                // ====================================================
                // DEVICE / ANDROID CHECKS
                // ====================================================

                val locationPermission =
                    checkLocationPermission()


                val backgroundLocationPermission =
                    checkBackgroundLocationPermission()


                val locationService =
                    checkLocationService()


                val bluetoothPermission = checkBluetoothPermission()
                val bluetoothService = checkBluetoothService()

                val network =
                    checkNetwork()


                val batteryOptimization =
                    checkBatteryOptimization()


                // ====================================================
                // TRUSTED TIME
                // ====================================================

                val trustedTime =
                    checkTrustedTime()


                // ====================================================
                // REGISTERED DEVICE
                //
                // This now performs REAL backend verification.
                // ====================================================

                val registeredDevice =
                    checkRegisteredDevice()


                // ====================================================
                // ROOM / OFFLINE STORAGE
                // ====================================================

                val offlineStorage =
                    checkOfflineStorage()


                // ====================================================
                // DATA SYNC
                // ====================================================

                val syncResult =
                    checkDataSync()


                // ====================================================
                // UPDATE UI
                // ====================================================

                _uiState.value =
                    _uiState.value.copy(

                        locationPermission =
                            locationPermission,

                        backgroundLocationPermission =
                            backgroundLocationPermission,

                        locationService =
                            locationService,

                        bluetoothPermission =
                            bluetoothPermission,

                        bluetoothService =
                            bluetoothService,

                        network =
                            network,

                        batteryOptimization =
                            batteryOptimization,

                        detectedBssid = readWifiBssid(),
                        wifiEnabled = readWifiEnabled(),


                        // ============================================
                        // TRUSTED TIME
                        // ============================================

                        trustedTime =
                            trustedTime,


                        // ============================================
                        // REGISTERED DEVICE
                        // ============================================

                        registeredDevice =
                            registeredDevice,


                        // ============================================
                        // ATTENDANCE CONFIGURATION
                        //
                        // CRM configuration API is still pending.
                        //
                        // Do NOT report READY until real configuration
                        // has been downloaded and validated.
                        // ============================================

                        attendanceConfiguration =
                            HealthItemState(

                                status =
                                    HealthStatus.NOT_CONFIGURED,

                                title =
                                    "Attendance Configuration",

                                message =
                                    "Attendance configuration not available"
                            ),


                        // The screen resolves the current office requirement from CRM.
                        officeBeacon = HealthItemState(
                            status = HealthStatus.NOT_CONFIGURED,
                            title = "Office Beacon",
                            message = "Current office policy not yet resolved"
                        ),

                        // Never infer assignment from the device's current Wi-Fi.
                        officeWifi = wifiPolicyDiagnostic(
                            _uiState.value.officeWifiRequirement
                        ),

                        // ============================================
                        // OFFLINE STORAGE
                        // ============================================

                        offlineStorage =
                            offlineStorage,


                        // ============================================
                        // DATA SYNC
                        // ============================================

                        sync =
                            syncResult.first,

                        pendingSyncCount =
                            syncResult.second,


                        // ============================================
                        // REFRESH FINISHED
                        // ============================================

                        isRefreshing =
                            false
                    )


            } catch (exception: CancellationException) {

                throw exception


            } catch (exception: Exception) {

                Log.e(
                    "SYSTEM_HEALTH",
                    "Unexpected health refresh failure",
                    exception
                )


                _uiState.value =
                    _uiState.value.copy(
                        isRefreshing = false
                    )
            }
        }
    }


    // ============================================================
    // REGISTERED DEVICE HEALTH CHECK
    //
    // Verification:
    //
    // JWT
    // +
    // existing installation device ID
    //          ↓
    // DeviceRepository.getCurrentDevice()
    //          ↓
    // Express / PostgreSQL
    //
    // IMPORTANT:
    //
    // Backend is authoritative.
    //
    // A UUID existing in SharedPreferences does NOT prove that
    // the installation is still registered or active.
    // ============================================================

    private suspend fun checkRegisteredDevice():
            HealthItemState {


        // ========================================================
        // AUTHENTICATED SESSION
        // ========================================================

        val token =
            sessionManager.getToken()


        if (token.isNullOrBlank()) {

            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Registered Device",

                message =
                    "Authenticated session unavailable"
            )
        }


        // ========================================================
        // EXISTING LOCAL DEVICE ID
        //
        // DO NOT use getOrCreateDeviceId() here.
        //
        // System Health must never silently generate another
        // installation identity.
        // ========================================================

        val localDeviceId =
            deviceIdManager.getDeviceId()


        if (localDeviceId.isNullOrBlank()) {

            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Registered Device",

                message =
                    "Local device identity unavailable"
            )
        }


        // ========================================================
        // VERIFY DEVICE WITH BACKEND
        // ========================================================

        val result =
            deviceRepository.getCurrentDevice(

                token =
                    token,

                deviceId =
                    localDeviceId
            )


        // ========================================================
        // NETWORK / SERIALIZATION / UNEXPECTED FAILURE
        //
        // Do not confuse a connectivity failure with
        // DEVICE_NOT_REGISTERED.
        // ========================================================

        if (result.isFailure) {

            val error =
                result.exceptionOrNull()


            Log.e(
                "SYSTEM_HEALTH_DEVICE",
                "Unable to verify registered device",
                error
            )


            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Registered Device",

                message =
                    "Unable to verify device registration"
            )
        }


        // ========================================================
        // READ RESPONSE
        // ========================================================

        val response =
            result.getOrNull()
                ?: return HealthItemState(

                    status =
                        HealthStatus.NOT_READY,

                    title =
                        "Registered Device",

                    message =
                        "Device verification response unavailable"
                )


        // ========================================================
        // NORMALIZE BACKEND STATUS
        // ========================================================

        val status =
            response.status
                .trim()
                .uppercase()


        // ========================================================
        // DEVICE FOUND
        // ========================================================

        if (status == "DEVICE_FOUND") {

            val device =
                response.device


            // Backend claimed DEVICE_FOUND but returned no device.

            if (device == null) {

                return HealthItemState(

                    status =
                        HealthStatus.NOT_READY,

                    title =
                        "Registered Device",

                    message =
                        "Registered device details unavailable"
                )
            }


            // ====================================================
            // DEFENSIVE DEVICE-ID MATCH
            //
            // Android verifies that the backend returned the exact
            // installation that was requested.
            // ====================================================

            if (device.deviceId != localDeviceId) {

                Log.e(
                    "SYSTEM_HEALTH_DEVICE",
                    "Device ID mismatch. " +
                            "local=$localDeviceId " +
                            "server=${device.deviceId}"
                )


                return HealthItemState(

                    status =
                        HealthStatus.NOT_READY,

                    title =
                        "Registered Device",

                    message =
                        "Registered device identity mismatch"
                )
            }


            // ====================================================
            // ACTIVE CHECK
            // ====================================================

            if (!device.isActive) {

                return HealthItemState(

                    status =
                        HealthStatus.NOT_READY,

                    title =
                        "Registered Device",

                    message =
                        "Registered device is disabled"
                )
            }


            // ====================================================
            // ONBOARDING CHECK
            // ====================================================

            if (!device.isOnboarded) {

                return HealthItemState(

                    status =
                        HealthStatus.NOT_READY,

                    title =
                        "Registered Device",

                    message =
                        "Device onboarding is incomplete"
                )
            }


            // ====================================================
            // VERIFIED
            // ====================================================

            return HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Registered Device",

                message =
                    device.deviceName
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            "$it is registered and active"
                        }
                        ?: "This device is registered and active"
            )
        }


        // ========================================================
        // DEVICE NOT REGISTERED
        // ========================================================

        if (status == "DEVICE_NOT_REGISTERED") {

            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Registered Device",

                message =
                    "This device is not registered"
            )
        }


        // ========================================================
        // DEVICE DISABLED
        // ========================================================

        if (status == "DEVICE_DISABLED") {

            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Registered Device",

                message =
                    "This device has been disabled"
            )
        }


        // ========================================================
        // UNKNOWN BACKEND STATUS
        //
        // Fail closed.
        //
        // Never report READY for a status Android does not
        // understand.
        // ========================================================

        return HealthItemState(

            status =
                HealthStatus.NOT_READY,

            title =
                "Registered Device",

            message =
                response.message
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: "Unable to verify device registration"
        )
    }


    // ============================================================
    // TRUSTED TIME HEALTH CHECK
    //
    // Checks:
    //
    // 1. Stored trusted time anchor exists.
    // 2. Anchor belongs to CURRENT Android boot session.
    //
    // IMPORTANT:
    //
    // This method does NOT create or refresh a time anchor.
    // ============================================================

    private fun checkTrustedTime():
            HealthItemState {


        // ========================================================
        // READ STORED TIME ANCHOR
        // ========================================================

        val anchor =
            timeAnchorStorage
                .getAnchor()


        if (anchor == null) {

            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Trusted Time",

                message =
                    "Trusted time anchor unavailable"
            )
        }


        // ========================================================
        // CURRENT BOOT SESSION
        // ========================================================

        val currentBootId =
            bootSessionManager
                .getBootId()


        // ========================================================
        // VERIFY BOOT SESSION
        //
        // Anchor:
        // bootId = A
        //
        // Phone restarts:
        // current bootId = B
        //
        // A != B
        //
        // Old anchor must not be trusted.
        // ========================================================

        if (anchor.bootId != currentBootId) {

            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Trusted Time",

                message =
                    "Trusted time must be refreshed after device restart"
            )
        }


        // ========================================================
        // READY
        // ========================================================

        return HealthItemState(

            status =
                HealthStatus.READY,

            title =
                "Trusted Time",

            message =
                "Server-backed trusted time available"
        )
    }


    // ============================================================
    // OBSERVE DATA SYNC STATUS
    //
    // Room Flow automatically updates System Health whenever:
    //
    // - PENDING event inserted
    // - FAILED event created
    // - event becomes SYNCED
    // ============================================================

    private fun observeSyncStatus() {

        viewModelScope.launch {

            offlineAttendanceRepository
                .observeUnsyncedCount()
                .collectLatest { count ->


                    val syncState =

                        if (count == 0) {

                            HealthItemState(

                                status =
                                    HealthStatus.READY,

                                title =
                                    "Data Sync",

                                message =
                                    "No pending local events (server not verified)"
                            )

                        } else {

                            HealthItemState(

                                status =
                                    HealthStatus.NOT_READY,

                                title =
                                    "Data Sync",

                                message =
                                    if (count == 1) {

                                        "1 attendance event waiting to sync"

                                    } else {

                                        "$count attendance events waiting to sync"
                                    }
                            )
                        }


                    _uiState.value =
                        _uiState.value.copy(

                            sync =
                                syncState,

                            pendingSyncCount =
                                count
                        )
                }
        }
    }


    // ============================================================
    // OFFLINE STORAGE
    //
    // Actually accesses Room.
    //
    // If the DAO query succeeds, local attendance storage is
    // available.
    // ============================================================

    private suspend fun checkOfflineStorage():
            HealthItemState {

        return try {

            offlineAttendanceRepository
                .getUnsyncedCount()


            HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Offline Storage",

                message =
                    "Local attendance storage ready"
            )


        } catch (exception: CancellationException) {

            throw exception


        } catch (exception: Exception) {

            Log.e(
                "SYSTEM_HEALTH_ROOM",
                "Offline storage health check failed",
                exception
            )


            HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Offline Storage",

                message =
                    "Local attendance storage unavailable"
            )
        }
    }


    // ============================================================
    // DATA SYNC
    //
    // Checks the current Room queue.
    //
    // WorkManager will later send unsynchronized events to the
    // Express API.
    // ============================================================

    private suspend fun checkDataSync():
            Pair<HealthItemState, Int> {

        return try {

            val count =
                offlineAttendanceRepository
                    .getUnsyncedCount()


            if (count == 0) {

                Pair(

                    HealthItemState(

                        status =
                            HealthStatus.READY,

                        title =
                            "Data Sync",

                        message =
                            "No pending local events (server not verified)"
                    ),

                    0
                )


            } else {

                Pair(

                    HealthItemState(

                        status =
                            HealthStatus.NOT_READY,

                        title =
                            "Data Sync",

                        message =
                            if (count == 1) {

                                "1 attendance event waiting to sync"

                            } else {

                                "$count attendance events waiting to sync"
                            }
                    ),

                    count
                )
            }


        } catch (exception: CancellationException) {

            throw exception


        } catch (exception: Exception) {

            Log.e(
                "SYSTEM_HEALTH_SYNC",
                "Unable to read sync queue",
                exception
            )


            Pair(

                HealthItemState(

                    status =
                        HealthStatus.NOT_READY,

                    title =
                        "Data Sync",

                    message =
                        "Unable to read synchronization status"
                ),

                0
            )
        }
    }


    // ============================================================
    // LOCATION PERMISSION
    // ============================================================

    private fun checkLocationPermission():
            HealthItemState {


        val fineLocationGranted =
            ContextCompat.checkSelfPermission(

                appContext,

                Manifest.permission.ACCESS_FINE_LOCATION

            ) == PackageManager.PERMISSION_GRANTED


        return if (fineLocationGranted) {

            HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Location Permission",

                message =
                    "Precise location permission granted"
            )

        } else {

            HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Location Permission",

                message =
                    "Location permission required"
            )
        }
    }


    // ============================================================
    // BACKGROUND LOCATION PERMISSION
    // ============================================================

    private fun checkBackgroundLocationPermission():
            HealthItemState {


        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.Q
        ) {

            return HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Background Location",

                message =
                    "Supported by Android version"
            )
        }


        val granted =
            ContextCompat.checkSelfPermission(

                appContext,

                Manifest.permission.ACCESS_BACKGROUND_LOCATION

            ) == PackageManager.PERMISSION_GRANTED


        return if (granted) {

            HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Background Location",

                message =
                    "Background location allowed"
            )

        } else {

            HealthItemState(

                status =
                    HealthStatus.NOT_REQUIRED,

                title =
                    "Background Location",

                message =
                    "Optional geofence alerts; start monitoring by opening the app"
            )
        }
    }


    // ============================================================
    // LOCATION SERVICE
    // ============================================================

    private fun checkLocationService():
            HealthItemState {


        val locationManager =
            appContext.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager


        val enabled =
            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) ||
                    locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    )


        return if (enabled) {

            HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Location Service",

                message =
                    "Location services enabled"
            )

        } else {

            HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Location Service",

                message =
                    "Turn on location services"
            )
        }
    }


    // ============================================================
    // BLUETOOTH PERMISSION
    // ============================================================

    private fun checkBluetoothPermission():
            HealthItemState {


        val granted =

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                    .all { ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED }

            } else {

                ContextCompat.checkSelfPermission(

                    appContext,

                    Manifest.permission.ACCESS_FINE_LOCATION

                ) == PackageManager.PERMISSION_GRANTED
            }


        return if (granted) {

            HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Bluetooth Permission",

                message =
                    "Bluetooth scanning permission granted"
            )

        } else {

            HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Bluetooth Permission",

                message =
                    "Bluetooth permission required"
            )
        }
    }


    // ============================================================
    // BLUETOOTH SERVICE
    // ============================================================

    private fun checkBluetoothService():
            HealthItemState {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return HealthItemState(HealthStatus.NOT_READY, "Bluetooth", "Nearby devices permission is required")
        }

        val bluetoothManager =
            appContext.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager


        val bluetoothAdapter: BluetoothAdapter? =
            bluetoothManager.adapter


        if (bluetoothAdapter == null) {

            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Bluetooth",

                message =
                    "Bluetooth not supported on this device"
            )
        }


        return if (bluetoothAdapter.isEnabled) {

            HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Bluetooth",

                message =
                    "Bluetooth enabled"
            )

        } else {

            HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Bluetooth",

                message =
                    "Turn on Bluetooth"
            )
        }
    }


    // ============================================================
    // INTERNET CONNECTION
    // ============================================================

    private fun checkNetwork():
            HealthItemState {


        val connectivityManager =
            appContext.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager


        val activeNetwork =
            connectivityManager.activeNetwork


        if (activeNetwork == null) {

            return HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Internet Connection",

                message =
                    "No network connection"
            )
        }


        val capabilities =
            connectivityManager.getNetworkCapabilities(
                activeNetwork
            )


        val hasInternet =
            capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true &&
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )


        return if (hasInternet) {

            HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Internet Connection",

                message =
                    "Internet available"
            )

        } else {

            HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Internet Connection",

                message =
                    "Internet unavailable"
            )
        }
    }


    private fun readWifiEnabled(): Boolean? = try {
        (appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    } catch (_: SecurityException) { null }

    @Suppress("DEPRECATION")
    private fun readWifiBssid(): String? = try {
        (appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo?.bssid
    } catch (_: SecurityException) { null }

    // ============================================================
    // BATTERY OPTIMIZATION
    //
    // Automatic attendance requires reliable background
    // processing.
    // ============================================================

    private fun checkBatteryOptimization():
            HealthItemState {


        val powerManager =
            appContext.getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager


        val packageName =
            appContext.packageName


        val unrestricted =
            powerManager.isIgnoringBatteryOptimizations(
                packageName
            )


        val backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isBackgroundRestricted
        if (backgroundRestricted || powerManager.isPowerSaveMode) {
            return HealthItemState(HealthStatus.NOT_READY, "Battery Optimization",
                if (backgroundRestricted) "Allow background battery usage for this app" else "Turn off Battery Saver for reliable background monitoring")
        }
        return if (unrestricted) {

            HealthItemState(

                status =
                    HealthStatus.READY,

                title =
                    "Battery Optimization",

                message =
                    "Doze exemption active; no detected Android power restrictions"
            )

        } else {

            HealthItemState(

                status =
                    HealthStatus.NOT_READY,

                title =
                    "Battery Optimization",

                message =
                    "Battery optimization may restrict attendance"
            )
        }
    }
}


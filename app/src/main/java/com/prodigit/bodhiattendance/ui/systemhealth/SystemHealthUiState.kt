package com.prodigit.bodhiattendance.ui.systemhealth


// ============================================================
// SYSTEM HEALTH STATUS
//
// Common status used by every System Health item.
// ============================================================

enum class HealthStatus {

    // Requirement is available and working
    READY,

    // Requirement is unavailable / disabled / failed
    NOT_READY,

    // Requirement has not yet been configured
    NOT_CONFIGURED,

    // Status is currently being checked
    CHECKING,

    // Office policy confirms this check is not mandatory
    NOT_REQUIRED
}


// ============================================================
// INDIVIDUAL HEALTH ITEM
//
// Represents one item displayed on the System Health screen.
//
// Example:
//
// Location Permission
// READY
// "Precise location permission granted"
// ============================================================

data class HealthItemState(

    val status: HealthStatus =
        HealthStatus.CHECKING,

    val title: String = "",

    val message: String = ""
)


// ============================================================
// SYSTEM HEALTH UI STATE
//
// Represents the complete current health of the Android
// automatic-attendance environment.
//
// IMPORTANT:
//
// This state does NOT make attendance decisions.
//
// It only reports whether the supporting components required
// by the attendance system are currently available.
// ============================================================

data class SystemHealthUiState(


    // ========================================================
    // LOCATION
    // ========================================================

    val locationPermission: HealthItemState =
        HealthItemState(
            title = "Location Permission"
        ),


    val backgroundLocationPermission: HealthItemState =
        HealthItemState(
            title = "Background Location"
        ),


    val locationService: HealthItemState =
        HealthItemState(
            title = "Location Service"
        ),


    // ========================================================
    // BLUETOOTH
    // ========================================================

    val bluetoothPermission: HealthItemState =
        HealthItemState(
            title = "Bluetooth Permission"
        ),


    val bluetoothService: HealthItemState =
        HealthItemState(
            title = "Bluetooth"
        ),


    // ========================================================
    // NETWORK
    // ========================================================

    val network: HealthItemState =
        HealthItemState(
            title = "Internet Connection"
        ),


    // ========================================================
    // BATTERY OPTIMIZATION
    //
    // Automatic attendance depends on reliable background
    // operation.
    // ========================================================

    val batteryOptimization: HealthItemState =
        HealthItemState(
            title = "Battery Optimization"
        ),


    // ========================================================
    // OFFICE BEACON
    //
    // This remains NOT_CONFIGURED until the BLE configuration
    // is supplied by the CRM / Express configuration system.
    // ========================================================

    val officeBeacon: HealthItemState =
        HealthItemState(

            status =
                HealthStatus.NOT_REQUIRED,

            title =
                "Office Beacon",

            message =
                "Optional BLE evidence; not required for attendance"
        ),


    // Last detected RSSI.
    //
    // This will be populated later by the BLE evidence layer.

    val beaconRssi: Int? =
        null,


    // ========================================================
    // OFFICE WI-FI
    //
    // This remains NOT_CONFIGURED until approved office
    // router/access-point configuration is supplied by CRM.
    // ========================================================

    // null = no verified assignment / requirement information yet.
    // true = verified Main Office-type Wi-Fi requirement.
    // false = verified GPS-only office policy.
    val officeWifiRequirement: Boolean? = null,

    val officeWifi: HealthItemState =
        HealthItemState(

            status =
                HealthStatus.NOT_CONFIGURED,

            title =
                "Office Wi-Fi",

            message =
                "Office Wi-Fi not configured"
        ),


    // Last detected Wi-Fi BSSID.
    //
    // Example:
    //
    // AA:BB:CC:DD:EE:FF

    val detectedBssid: String? =
        null,


    // ========================================================
    // REGISTERED DEVICE
    //
    // Verifies that this Android installation/device is
    // registered for the authenticated employee.
    //
    // Device registration is already handled by our
    // onboarding flow.
    //
    // System Health only verifies its current state.
    // ========================================================

    val registeredDevice: HealthItemState =
        HealthItemState(
            title = "Registered Device"
        ),


    // ========================================================
    // TRUSTED TIME
    //
    // Verifies that a valid server-backed time anchor exists
    // for the CURRENT Android boot session.
    //
    // Attendance timestamps continue to use:
    //
    // server time anchor
    //        +
    // SystemClock.elapsedRealtime()
    //
    // The Android wall clock is NOT authoritative.
    // ========================================================

    val trustedTime: HealthItemState =
        HealthItemState(
            title = "Trusted Time"
        ),


    // ========================================================
    // ATTENDANCE CONFIGURATION
    //
    // Verifies whether CRM-managed attendance configuration
    // is available locally.
    //
    // Future production flow:
    //
    // React CRM
    //      ↓
    // Express
    //      ↓
    // AttendanceConfigRepository
    //      ↓
    // AttendanceConfigManager
    //      ↓
    // Room
    //
    // Configuration contains:
    //
    // - office / knowledge-centre locations
    // - geofence configuration
    // - BLE beacon configuration
    // - approved Wi-Fi BSSIDs
    //
    // This is different from checking whether the employee
    // currently passes GPS/BLE/Wi-Fi verification.
    // ========================================================

    val attendanceConfiguration: HealthItemState =
        HealthItemState(

            status =
                HealthStatus.NOT_CONFIGURED,

            title =
                "Attendance Configuration",

            message =
                "Attendance configuration not available"
        ),


    // ========================================================
    // OFFLINE STORAGE
    //
    // Reports whether the local Room attendance database can
    // be accessed.
    // ========================================================

    val offlineStorage: HealthItemState =
        HealthItemState(
            title = "Offline Storage"
        ),


    // ========================================================
    // DATA SYNC
    //
    // Reports whether locally stored attendance events are
    // waiting to be synchronized with Express.
    // ========================================================

    val sync: HealthItemState =
        HealthItemState(
            title = "Data Sync"
        ),


    // Number of PENDING / FAILED local attendance events
    // currently waiting for synchronization.

    val pendingSyncCount: Int =
        0,


    // ========================================================
    // SCREEN STATE
    // ========================================================

    val isRefreshing: Boolean =
        false
)
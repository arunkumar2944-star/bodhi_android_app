package com.prodigit.bodhiattendance.ui.onboarding


data class OnboardingUiState(

    // ============================================================
    // LOCATION
    // ============================================================

    val locationPermissionGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val locationServiceEnabled: Boolean = false,


    // ============================================================
    // BLUETOOTH / BLE
    // ============================================================

    val bluetoothPermissionGranted: Boolean = false,
    val bluetoothEnabled: Boolean = false,


    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    val notificationPermissionGranted: Boolean = false,


    // ============================================================
    // OFFLINE DATABASE
    // ============================================================

    val offlineStorageReady: Boolean = false,


    // ============================================================
    // DEVICE REGISTRATION
    // ============================================================

    val deviceRegistered: Boolean = false,
    val deviceRegistrationLoading: Boolean = false,


    // ============================================================
    // FINAL ONBOARDING
    // ============================================================

    val onboardingCompleting: Boolean = false,
    val onboardingCompleted: Boolean = false,


    // ============================================================
    // ERROR
    // ============================================================

    val errorMessage: String? = null

) {


    // ============================================================
    // LOCATION READY
    //
    // Location is considered ready only when:
    //
    // 1. Precise location permission granted
    // 2. Background location permission granted
    // 3. Device location service switched ON
    // ============================================================

    val locationReady: Boolean
        get() =
            locationPermissionGranted &&
                    backgroundLocationGranted &&
                    locationServiceEnabled


    // ============================================================
    // BLUETOOTH READY
    //
    // BLE is considered ready only when:
    //
    // 1. Required Bluetooth permission granted
    // 2. Bluetooth itself switched ON
    // ============================================================

    val bluetoothReady: Boolean
        get() =
            bluetoothPermissionGranted &&
                    bluetoothEnabled


    // ============================================================
    // CAN CONTINUE
    //
    // ALL five mandatory onboarding requirements must be ready.
    //
    // 1. Location
    // 2. Bluetooth
    // 3. Notifications
    // 4. Offline Room database
    // 5. Device registration
    // ============================================================

    val canContinue: Boolean
        get() =
            locationReady &&
                    bluetoothReady &&
                    notificationPermissionGranted &&
                    offlineStorageReady &&
                    deviceRegistered
}
package com.prodigit.bodhiattendance.core.permission

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

class OnboardingPermissionManager(
    private val context: Context
) {

    // ============================================================
    // FOREGROUND LOCATION
    // ============================================================

    fun hasForegroundLocationPermission(): Boolean {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }


    // ============================================================
    // PRECISE LOCATION
    // Important for our geofence attendance flow
    // ============================================================

    fun hasPreciseLocationPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    // ============================================================
    // BACKGROUND LOCATION
    // ============================================================

    fun hasBackgroundLocationPermission(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        } else {

            // Before Android 10 there is no separate
            // background-location runtime permission.

            hasForegroundLocationPermission()
        }
    }


    // ============================================================
    // LOCATION / GPS SERVICE
    // ============================================================

    fun isLocationServiceEnabled(): Boolean {

        val locationManager =
            context.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        return try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                locationManager.isLocationEnabled

            } else {

                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                ) ||
                        locationManager.isProviderEnabled(
                            LocationManager.NETWORK_PROVIDER
                        )
            }

        } catch (e: Exception) {

            false
        }
    }


    // ============================================================
    // BLUETOOTH PERMISSION
    // ============================================================

    fun hasBluetoothPermission(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            val scanGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

            val connectGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

            scanGranted && connectGranted

        } else {

            // Android <= 11 BLE scanning depends on
            // location permission.

            hasForegroundLocationPermission()
        }
    }


    // ============================================================
    // BLUETOOTH ENABLED
    // ============================================================

    fun isBluetoothEnabled(): Boolean {

        val bluetoothManager =
            context.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as? BluetoothManager
                ?: return false

        val adapter: BluetoothAdapter =
            bluetoothManager.adapter
                ?: return false

        return try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                false

            } else {

                adapter.isEnabled
            }

        } catch (e: SecurityException) {

            false
        }
    }


    // ============================================================
    // NOTIFICATION PERMISSION
    // ============================================================

    fun hasNotificationPermission(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        } else {

            // No POST_NOTIFICATIONS runtime permission
            // before Android 13.

            true
        }
    }
}
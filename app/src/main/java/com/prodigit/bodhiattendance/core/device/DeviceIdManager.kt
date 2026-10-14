package com.prodigit.bodhiattendance.core.device

import android.content.Context
import java.util.UUID

class DeviceIdManager(
    context: Context
) {

    companion object {

        private const val PREF_NAME =
            "device_preferences"

        private const val KEY_DEVICE_ID =
            "installation_device_id"
    }


    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )


    // ============================================================
    // GET OR CREATE DEVICE ID
    // ============================================================

    fun getOrCreateDeviceId(): String {

        // Check whether this installation already has an ID.
        val existingDeviceId =
            preferences.getString(
                KEY_DEVICE_ID,
                null
            )


        // Existing ID found.
        // NEVER generate another one.
        if (!existingDeviceId.isNullOrBlank()) {

            return existingDeviceId
        }


        // ========================================================
        // FIRST INSTALLATION
        // ========================================================

        val newDeviceId =
            UUID.randomUUID().toString()


        // Persist the generated UUID.
        preferences
            .edit()
            .putString(
                KEY_DEVICE_ID,
                newDeviceId
            )
            .apply()


        return newDeviceId
    }


    // ============================================================
    // GET EXISTING DEVICE ID
    // ============================================================

    fun saveExistingDeviceId(deviceId: String) {
        require(deviceId.isNotBlank())
        val oldId = getDeviceId()
        check(oldId == null || oldId == deviceId) { "Installation UUID conflict" }
        check(preferences.edit().putString(KEY_DEVICE_ID, deviceId).commit()) {
            "Could not persist confirmed device UUID"
        }
    }

    fun getDeviceId(): String? {

        return preferences.getString(
            KEY_DEVICE_ID,
            null
        )
    }


    // ============================================================
    // CHECK WHETHER DEVICE ID EXISTS
    // ============================================================

    fun hasDeviceId(): Boolean {

        return !getDeviceId()
            .isNullOrBlank()
    }
}
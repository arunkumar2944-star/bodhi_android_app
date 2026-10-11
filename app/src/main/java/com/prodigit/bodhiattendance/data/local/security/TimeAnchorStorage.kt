package com.prodigit.bodhiattendance.data.local.security

import android.content.Context


// ============================================================
// TIME ANCHOR STORAGE
//
// Stores the latest trusted server time anchor locally.
//
// The anchor connects:
//
// serverTime
// +
// anchor elapsedRealtime
// +
// bootId
//
// Attendance events later reference anchorUuid and send their
// own eventElapsedRealtimeMs.
//
// This is small security/session state, so SharedPreferences is
// sufficient. Attendance events themselves remain in Room.
// ============================================================

class TimeAnchorStorage(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )


    // ========================================================
    // SAVE ANCHOR
    // ========================================================

    fun saveAnchor(
        anchorUuid: String,
        serverTime: String,
        elapsedRealtimeMs: Long,
        bootId: String
    ) {

        preferences
            .edit()
            .putString(
                KEY_ANCHOR_UUID,
                anchorUuid
            )
            .putString(
                KEY_SERVER_TIME,
                serverTime
            )
            .putLong(
                KEY_ELAPSED_REALTIME,
                elapsedRealtimeMs
            )
            .putString(
                KEY_BOOT_ID,
                bootId
            )
            .apply()
    }


    // ========================================================
    // GET CURRENT ANCHOR
    // ========================================================

    fun getAnchor(): StoredTimeAnchor? {

        val anchorUuid =
            preferences.getString(
                KEY_ANCHOR_UUID,
                null
            ) ?: return null


        val serverTime =
            preferences.getString(
                KEY_SERVER_TIME,
                null
            ) ?: return null


        val bootId =
            preferences.getString(
                KEY_BOOT_ID,
                null
            ) ?: return null


        val elapsedRealtimeMs =
            preferences.getLong(
                KEY_ELAPSED_REALTIME,
                -1L
            )


        if (elapsedRealtimeMs < 0L) {
            return null
        }


        return StoredTimeAnchor(
            anchorUuid = anchorUuid,
            serverTime = serverTime,
            elapsedRealtimeMs = elapsedRealtimeMs,
            bootId = bootId
        )
    }


    // ========================================================
    // CLEAR ANCHOR
    //
    // Used when:
    //
    // - logout
    // - device changes
    // - anchor becomes invalid
    // - boot session changes
    // ========================================================

    fun clearAnchor() {

        preferences
            .edit()
            .clear()
            .apply()
    }


    companion object {

        private const val PREFS_NAME =
            "trusted_time_anchor"

        private const val KEY_ANCHOR_UUID =
            "anchor_uuid"

        private const val KEY_SERVER_TIME =
            "server_time"

        private const val KEY_ELAPSED_REALTIME =
            "elapsed_realtime_ms"

        private const val KEY_BOOT_ID =
            "boot_id"
    }
}


// ============================================================
// STORED TIME ANCHOR
// ============================================================

data class StoredTimeAnchor(

    val anchorUuid: String,

    val serverTime: String,

    val elapsedRealtimeMs: Long,

    val bootId: String
)
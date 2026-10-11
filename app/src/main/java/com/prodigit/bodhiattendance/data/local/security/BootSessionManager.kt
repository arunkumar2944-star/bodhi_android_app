package com.prodigit.bodhiattendance.data.local.security

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings

import java.util.UUID


// ============================================================
// BOOT SESSION MANAGER
//
// Provides a stable app boot-session UUID.
//
// Requirements:
//
// 1. Same Android boot
//      -> same bootId
//
// 2. Phone reboot
//      -> new bootId
//
// 3. App/process restart without phone reboot
//      -> same bootId
//
// Primary reboot detection:
//
// Settings.Global.BOOT_COUNT
//
// Fallback:
//
// SystemClock.elapsedRealtime() regression.
//
// The bootId is used by:
//
// - trusted server time anchors
// - attendance events
// - persisted attendance-engine runtime state
//
// This prevents monotonic timestamps from different Android
// boot sessions being mixed together.
// ============================================================

class BootSessionManager(
    context: Context
) : BootSessionProvider {


    private val appContext =
        context.applicationContext


    private val preferences =
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )


    // ========================================================
    // GET CURRENT BOOT ID
    // ========================================================

    override  fun getBootId(): String {


        val currentElapsedRealtime =
            SystemClock.elapsedRealtime()


        val currentBootCount =
            getSystemBootCount()


        val savedBootId =
            preferences.getString(
                KEY_BOOT_ID,
                null
            )


        val savedElapsedRealtime =
            preferences.getLong(
                KEY_LAST_ELAPSED_REALTIME,
                -1L
            )


        val savedBootCount =
            preferences.getInt(
                KEY_BOOT_COUNT,
                UNKNOWN_BOOT_COUNT
            )


        // ====================================================
        // PRIMARY REBOOT DETECTION
        //
        // Android BOOT_COUNT increments across device boots.
        //
        // Only compare it when:
        //
        // - current BOOT_COUNT is available
        // - we previously stored a BOOT_COUNT
        //
        // On the first run after introducing this newer logic,
        // savedBootCount may not yet exist. In that case we keep
        // compatibility with the previous boot session and use
        // elapsedRealtime fallback.
        // ====================================================

        val rebootDetectedByBootCount =

            currentBootCount != UNKNOWN_BOOT_COUNT &&
                    savedBootCount != UNKNOWN_BOOT_COUNT &&
                    currentBootCount != savedBootCount


        // ====================================================
        // FALLBACK REBOOT DETECTION
        //
        // elapsedRealtime starts again near zero after reboot.
        //
        // This remains useful when BOOT_COUNT is unavailable or
        // when upgrading from an older app version that had not
        // stored BOOT_COUNT yet.
        //
        // NOTE:
        //
        // BOOT_COUNT is preferred because elapsedRealtime-only
        // detection is not sufficient in every timing scenario.
        // ====================================================

        val rebootDetectedByElapsedRealtime =

            savedElapsedRealtime >= 0L &&
                    currentElapsedRealtime <
                    savedElapsedRealtime


        // ====================================================
        // FINAL REBOOT RESULT
        // ====================================================

        val deviceRebooted =

            rebootDetectedByBootCount ||
                    rebootDetectedByElapsedRealtime


        // ====================================================
        // CREATE OR REUSE BOOT SESSION UUID
        // ====================================================

        val bootId =

            if (
                savedBootId == null ||
                deviceRebooted
            ) {

                UUID.randomUUID()
                    .toString()

            } else {

                savedBootId
            }


        // ====================================================
        // SAVE CURRENT BOOT STATE
        //
        // apply() is adequate here because this information is
        // operational local state.
        //
        // The trusted attendance timestamp itself still comes
        // from server anchor + monotonic elapsed realtime.
        // ====================================================

        preferences
            .edit()
            .putString(
                KEY_BOOT_ID,
                bootId
            )
            .putLong(
                KEY_LAST_ELAPSED_REALTIME,
                currentElapsedRealtime
            )
            .putInt(
                KEY_BOOT_COUNT,
                currentBootCount
            )
            .apply()


        return bootId
    }


    // ========================================================
    // GET CURRENT MONOTONIC TIME
    //
    // Used for:
    //
    // time-anchor elapsedRealtimeMs
    // attendance event eventElapsedRealtimeMs
    // engine grace-period timing
    // break timing
    //
    // Never use System.currentTimeMillis() as authoritative
    // attendance duration/time evidence.
    // ========================================================

    override  fun getElapsedRealtimeMs(): Long {

        return SystemClock.elapsedRealtime()
    }


    // ========================================================
    // GET SYSTEM BOOT COUNT
    //
    // Android Settings.Global.BOOT_COUNT was added in API 24.
    //
    // Return UNKNOWN_BOOT_COUNT if unavailable.
    //
    // We deliberately fail safely and allow the elapsedRealtime
    // fallback rather than crashing attendance processing.
    // ========================================================

    private fun getSystemBootCount(): Int {


        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.N
        ) {

            return UNKNOWN_BOOT_COUNT
        }


        return try {

            Settings.Global.getInt(
                appContext.contentResolver,
                Settings.Global.BOOT_COUNT,
                UNKNOWN_BOOT_COUNT
            )

        } catch (e: Exception) {

            UNKNOWN_BOOT_COUNT
        }
    }


    companion object {


        private const val PREFS_NAME =
            "trusted_time_boot_session"


        private const val KEY_BOOT_ID =
            "boot_id"


        private const val KEY_LAST_ELAPSED_REALTIME =
            "last_elapsed_realtime"


        // Added to strengthen reboot detection.
        private const val KEY_BOOT_COUNT =
            "boot_count"


        private const val UNKNOWN_BOOT_COUNT =
            -1
    }
}
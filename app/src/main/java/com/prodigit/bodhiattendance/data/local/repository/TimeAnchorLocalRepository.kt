package com.prodigit.bodhiattendance.data.local.repository

import com.prodigit.bodhiattendance.data.local.dao.TimeAnchorDao
import com.prodigit.bodhiattendance.data.local.entity.TimeAnchorEntity


// ============================================================
// TIME ANCHOR LOCAL REPOSITORY
//
// Single access layer between the attendance trusted-time
// system and the local Room time_anchors table.
//
// Used later by:
//
// - Trusted Time Manager
// - Attendance Engine
// - Offline attendance event creation
// - Attendance synchronization
// - System Health
//
// UI / ViewModels should NOT directly access TimeAnchorDao.
//
// IMPORTANT:
//
// A time anchor is trusted only when:
//
// - it was issued by Express
// - deviceId matches the registered Android installation
// - bootId matches the required Android boot session
//
// Phone wall-clock time must NOT be used as authoritative
// attendance time.
//
// Historical attendance events may use an older persisted anchor
// from the SAME boot session when their effective monotonic event
// time occurred before the currently active anchor.
// ============================================================

class TimeAnchorLocalRepository(

    private val timeAnchorDao: TimeAnchorDao

) {


    // ========================================================
    // SAVE TIME ANCHOR
    //
    // Called after Express successfully creates / returns a
    // trusted server time anchor.
    //
    // If the same anchorUuid is saved again, Room replaces the
    // existing row because the DAO uses REPLACE.
    // ========================================================

    suspend fun saveTimeAnchor(
        anchor: TimeAnchorEntity
    ) {

        timeAnchorDao
            .saveTimeAnchor(
                anchor = anchor
            )
    }


    // ========================================================
    // GET TIME ANCHOR BY UUID
    //
    // Used when an attendance event contains:
    //
    // attendance_events.timeAnchorId
    //
    // The value stored in timeAnchorId corresponds to:
    //
    // time_anchors.anchorUuid
    //
    // This is especially important during offline retry because
    // the ORIGINAL anchor referenced by the event must be reused.
    // ========================================================

    suspend fun getTimeAnchorByUuid(
        anchorUuid: String
    ): TimeAnchorEntity? {

        return timeAnchorDao
            .getTimeAnchorByUuid(
                anchorUuid = anchorUuid
            )
    }


    // ========================================================
    // GET LATEST VALID TIME ANCHOR
    //
    // Returns the newest trusted anchor belonging to:
    //
    // - the registered device
    // - the requested boot session
    //
    // If no matching anchor exists, null is returned.
    //
    // This is important after a device reboot because Android
    // SystemClock.elapsedRealtime() resets for the new boot.
    //
    // Normally used for CURRENT-TIME attendance events.
    // ========================================================

    suspend fun getLatestValidTimeAnchor(
        deviceId: String,
        bootId: String
    ): TimeAnchorEntity? {

        return timeAnchorDao
            .getLatestValidTimeAnchor(

                deviceId =
                    deviceId,

                bootId =
                    bootId
            )
    }


    // ========================================================
    // GET BEST ANCHOR FOR HISTORICAL EVENT
    //
    // Returns the newest trusted anchor that existed at or
    // before the effective monotonic attendance event time.
    //
    // Required conditions:
    //
    // - same registered device
    // - same Android boot session
    // - anchor.elapsedRealtimeMs <= eventElapsedRealtimeMs
    //
    // Example:
    //
    // Anchor A             = 18:00
    // Employee leaves      = 18:25
    // Anchor B             = 19:00
    // CHECK_OUT classified = 19:55
    //
    // Effective CHECK_OUT time:
    //
    // 18:25
    //
    // Correct anchor:
    //
    // Anchor A ✅
    //
    // Incorrect:
    //
    // Anchor B ❌
    //
    // because Anchor B was created AFTER the effective event.
    //
    // This preserves the original trusted checkout time while
    // still allowing newer anchors to be created later.
    // ========================================================

    suspend fun getBestAnchorForEvent(

        deviceId: String,

        bootId: String,

        eventElapsedRealtimeMs: Long

    ): TimeAnchorEntity? {

        return timeAnchorDao
            .getBestAnchorForEvent(

                deviceId =
                    deviceId,

                bootId =
                    bootId,

                eventElapsedRealtimeMs =
                    eventElapsedRealtimeMs
            )
    }


    // ========================================================
    // DELETE OLD BOOT ANCHORS
    //
    // Removes anchors belonging to previous boot sessions for
    // the same registered device.
    //
    // IMPORTANT:
    //
    // We should NOT automatically call this every time we save
    // an anchor.
    //
    // Old attendance events may still reference old anchors,
    // including events waiting for synchronization.
    //
    // Cleanup must therefore be controlled by the retention /
    // synchronization layer.
    //
    // In production, old anchors should only be removed when
    // they are no longer referenced by retained attendance
    // events.
    // ========================================================

    suspend fun deleteOldBootAnchors(
        deviceId: String,
        currentBootId: String
    ): Int {

        return timeAnchorDao
            .deleteOldBootAnchors(

                deviceId =
                    deviceId,

                currentBootId =
                    currentBootId
            )
    }


    // ========================================================
    // DELETE ALL TIME ANCHORS
    //
    // Reserved for controlled local-state reset.
    //
    // IMPORTANT:
    //
    // Do NOT automatically use this for normal logout because:
    //
    // - PENDING events may reference anchors
    // - FAILED_RETRYABLE events may reference anchors
    // - retained SYNCED events may reference anchors
    // - retained REJECTED events may reference anchors
    //
    // Deleting referenced anchors would prevent trusted-time
    // reconstruction during later synchronization/audit.
    // ========================================================

    suspend fun deleteAllTimeAnchors() {

        timeAnchorDao
            .deleteAllTimeAnchors()
    }
}
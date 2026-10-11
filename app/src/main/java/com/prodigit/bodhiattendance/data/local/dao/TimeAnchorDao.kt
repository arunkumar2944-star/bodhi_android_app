package com.prodigit.bodhiattendance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.prodigit.bodhiattendance.data.local.entity.TimeAnchorEntity


// ============================================================
// TIME ANCHOR DAO
//
// Provides local persistence for trusted server time anchors.
//
// The latest valid anchor can be used by the attendance engine
// while offline.
//
// Historical attendance events can also select the newest anchor
// that existed BEFORE the event's effective monotonic time.
//
// IMPORTANT:
//
// An anchor is valid only when:
//
// - deviceId matches the current registered device
// - bootId matches the required Android boot session
//
// Phone wall-clock time is NOT used to determine whether the
// trusted anchor belongs to the current boot session.
// ============================================================

@Dao
interface TimeAnchorDao {


    // ========================================================
    // SAVE / UPDATE TIME ANCHOR
    //
    // REPLACE allows the same server anchor to be safely
    // persisted again without creating duplicates.
    //
    // anchorUuid is the unique server-generated identifier.
    // ========================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun saveTimeAnchor(
        anchor: TimeAnchorEntity
    )


    // ========================================================
    // GET ANCHOR BY UUID
    //
    // Attendance events reference a trusted server time anchor
    // using:
    //
    // attendance_events.timeAnchorId
    //
    // which contains:
    //
    // time_anchors.anchorUuid
    //
    // This is especially important during offline retry because
    // AttendanceSyncManager must reload the ORIGINAL anchor that
    // was used when the event was created.
    // ========================================================

    @Query(
        """
        SELECT *
        FROM time_anchors
        WHERE anchorUuid = :anchorUuid
        LIMIT 1
        """
    )
    suspend fun getTimeAnchorByUuid(
        anchorUuid: String
    ): TimeAnchorEntity?


    // ========================================================
    // GET LATEST ANCHOR FOR CURRENT DEVICE + BOOT SESSION
    //
    // Used for normal current-time attendance events.
    //
    // We intentionally require both:
    //
    // deviceId
    // +
    // bootId
    //
    // This prevents an anchor belonging to another registered
    // installation or previous phone boot from being reused.
    //
    // elapsedRealtimeMs is monotonic within one Android boot.
    //
    // Therefore the largest elapsedRealtimeMs is the newest
    // anchor for that device + boot session.
    // ========================================================

    @Query(
        """
        SELECT *
        FROM time_anchors
        WHERE deviceId = :deviceId
          AND bootId = :bootId
        ORDER BY elapsedRealtimeMs DESC
        LIMIT 1
        """
    )
    suspend fun getLatestValidTimeAnchor(
        deviceId: String,
        bootId: String
    ): TimeAnchorEntity?


    // ========================================================
    // GET BEST ANCHOR FOR HISTORICAL ATTENDANCE EVENT
    //
    // Finds the newest trusted anchor that:
    //
    // - belongs to the same registered device
    // - belongs to the same Android boot session
    // - existed at or before the effective attendance event time
    //
    // This is required for delayed attendance classifications.
    //
    // Example:
    //
    // Anchor A             = 18:00
    // Employee leaves      = 18:25
    // Anchor B             = 19:00
    // CHECK_OUT classified = 19:55
    //
    // Effective checkout time is still 18:25.
    //
    // Therefore:
    //
    // Anchor A ✅
    //
    // because:
    //
    // 18:00 <= 18:25
    //
    //
    // Anchor B ❌
    //
    // because:
    //
    // 19:00 > 18:25
    //
    // We ORDER BY elapsedRealtimeMs DESC so if several anchors
    // existed before the event, we select the closest/latest
    // valid one.
    //
    // This lets Express safely reconstruct:
    //
    // anchor.serverTime
    // +
    // (
    //     eventElapsedRealtimeMs
    //     -
    //     anchor.elapsedRealtimeMs
    // )
    //
    // without changing the employee's original event time.
    // ========================================================

    @Query(
        """
        SELECT *
        FROM time_anchors
        WHERE deviceId = :deviceId
          AND bootId = :bootId
          AND elapsedRealtimeMs <= :eventElapsedRealtimeMs
        ORDER BY elapsedRealtimeMs DESC
        LIMIT 1
        """
    )
    suspend fun getBestAnchorForEvent(
        deviceId: String,
        bootId: String,
        eventElapsedRealtimeMs: Long
    ): TimeAnchorEntity?


    // ========================================================
    // DELETE OLD ANCHORS FOR DEVICE
    //
    // When Android obtains a trusted anchor after a new boot,
    // anchors belonging to previous boot sessions are no longer
    // usable for NEW monotonic calculations.
    //
    // Example:
    //
    // Previous bootId = ABC
    // Current bootId  = XYZ
    //
    // IMPORTANT:
    //
    // DO NOT automatically call this when a new boot occurs.
    //
    // Existing offline attendance events may still reference an
    // old anchor and need it during later synchronization.
    //
    // According to our retention policy, old anchors should be
    // deleted only when they are no longer referenced by retained
    // attendance events.
    //
    // Therefore this method is intended only for controlled
    // cleanup.
    // ========================================================

    @Query(
        """
        DELETE FROM time_anchors
        WHERE deviceId = :deviceId
          AND bootId != :currentBootId
        """
    )
    suspend fun deleteOldBootAnchors(
        deviceId: String,
        currentBootId: String
    ): Int


    // ========================================================
    // DELETE ALL TIME ANCHORS
    //
    // Can later be used when:
    //
    // - registered device is fully reset
    // - application security state is intentionally cleared
    // - local attendance state is completely reset
    //
    // IMPORTANT:
    //
    // Normal logout should NOT blindly delete time anchors,
    // because retained offline attendance events may still
    // reference them.
    // ========================================================

    @Query(
        """
        DELETE FROM time_anchors
        """
    )
    suspend fun deleteAllTimeAnchors()
}
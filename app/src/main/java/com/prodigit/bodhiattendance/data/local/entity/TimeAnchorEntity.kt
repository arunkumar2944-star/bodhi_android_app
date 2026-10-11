package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


// ============================================================
// TIME ANCHOR ENTITY
//
// Persists a trusted server-issued time anchor.
//
// Backend provides:
//
// anchorUuid
// serverTime
// elapsedRealtimeMs
// bootId
//
// Android also stores deviceId so anchors can be scoped to the
// registered installation.
//
// Phone wall-clock time must NEVER be used as authoritative
// attendance time.
// ============================================================

@Entity(
    tableName = "time_anchors"
)
data class TimeAnchorEntity(

    // ========================================================
    // SERVER ANCHOR UUID
    // ========================================================

    @PrimaryKey
    val anchorUuid: String,


    // ========================================================
    // REGISTERED DEVICE
    //
    // deviceId comes from the request/current installation.
    // ========================================================

    val deviceId: String,


    // ========================================================
    // SERVER TIME
    //
    // Original ISO timestamp returned by Express.
    //
    // Example:
    // 2026-10-05T12:30:00.000Z
    //
    // We preserve the server value exactly instead of
    // converting it during persistence.
    // ========================================================

    val serverTime: String,


    // ========================================================
    // MONOTONIC CLOCK VALUE
    //
    // Android SystemClock.elapsedRealtime() value that was sent
    // when requesting this server anchor.
    // ========================================================

    val elapsedRealtimeMs: Long,


    // ========================================================
    // BOOT SESSION
    //
    // The anchor can only be trusted during this boot session.
    // ========================================================

    val bootId: String,


    // ========================================================
    // LOCAL STORAGE METADATA
    //
    // This field is NOT attendance time.
    // It only records when this row was persisted locally.
    // ========================================================

    val storedAtEpochMs: Long
)
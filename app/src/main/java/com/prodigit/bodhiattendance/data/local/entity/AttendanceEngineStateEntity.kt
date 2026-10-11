package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.Index


// ============================================================
// ATTENDANCE ENGINE STATE ENTITY
//
// Stores the CURRENT runtime state of the automatic attendance
// engine.
//
// IMPORTANT:
//
// This is NOT attendance history.
//
// Real attendance history remains:
//
// CHECK_IN
// BREAK_START
// BREAK_END
// CHECK_OUT
//
// inside:
//
// attendance_events
//
// This table only lets the automatic engine safely recover after:
//
// - process death
// - app restart
// - background termination
//
// without creating fake attendance events.
// ============================================================

@Entity(
    tableName = "attendance_engine_state",

    primaryKeys = [
        "userId",
        "deviceId"
    ],

    indices = [
        Index(
            value = ["deviceId"]
        )
    ]
)
data class AttendanceEngineStateEntity(


    // ========================================================
    // EMPLOYEE / DEVICE
    // ========================================================

    val userId: Int,

    val companyId: Int,

    val deviceId: String,


    // ========================================================
    // CURRENT ATTENDANCE STATE
    //
    // Expected values:
    //
    // NOT_CHECKED_IN
    // WORKING
    // ON_BREAK
    // CHECKED_OUT
    // ON_DUTY
    //
    // This is local engine state/cache.
    //
    // Express remains final authority.
    // ========================================================

    val attendanceState: String,


    // ========================================================
    // INTERNAL ENGINE PHASE
    //
    // Expected values:
    //
    // NOT_MONITORING
    // PRESENT
    // ABSENCE_GRACE
    // BREAK_ACTIVE
    // CHECKOUT_CLASSIFIED
    // ON_DUTY_BYPASS
    //
    // These are NOT attendance events.
    // ========================================================

    val phaseType: String,


    // ========================================================
    // ORIGINAL ABSENCE START
    //
    // Set when valid office presence is first lost.
    //
    // Example:
    //
    // employee leaves office at 18:25
    //
    // this stores the monotonic elapsed realtime representing
    // 18:25.
    //
    // Used by:
    //
    // - 3-minute grace
    // - 90-minute checkout classification
    // ========================================================

    val absenceStartedElapsedRealtimeMs: Long? = null,


    // ========================================================
    // BREAK CONFIRMATION TIME
    //
    // Example:
    //
    // absence starts 18:25
    //
    // grace completes 18:28
    //
    // BREAK_START confirmed at 18:28
    //
    // This field stores the monotonic time corresponding to
    // 18:28.
    // ========================================================

    val breakConfirmedElapsedRealtimeMs: Long? = null,


    // ========================================================
    // CHECKOUT CLASSIFICATION TIME
    //
    // Example:
    //
    // original exit = 18:25
    //
    // 90-minute decision = 19:55
    //
    // This field stores 19:55's monotonic value.
    //
    // The effective attendance checkout time is still the
    // ORIGINAL absence time, not this value.
    // ========================================================

    val checkoutClassifiedAtElapsedRealtimeMs: Long? = null,


    // ========================================================
    // BOOT SESSION
    //
    // CRITICAL.
    //
    // Android elapsedRealtime values are only comparable inside
    // the same boot session.
    //
    // Therefore every persisted engine state must remember the
    // BootSessionManager bootId that created it.
    //
    // On restore:
    //
    // stored bootId == current bootId
    //
    //     safe to reuse monotonic values
    //
    // stored bootId != current bootId
    //
    //     DO NOT compare old elapsedRealtime values with new
    //     elapsedRealtime values.
    //
    // Reboot recovery will be handled separately.
    // ========================================================

    val bootId: String,


    // ========================================================
    // LOCAL METADATA
    //
    // Phone wall clock.
    //
    // This is NOT authoritative attendance time.
    //
    // It is only useful for:
    //
    // - debugging
    // - cleanup
    // - diagnostics
    // ========================================================

    val updatedAtEpochMs: Long
)
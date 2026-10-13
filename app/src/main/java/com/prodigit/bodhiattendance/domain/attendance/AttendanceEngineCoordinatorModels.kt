package com.prodigit.bodhiattendance.domain.attendance


// ============================================================
// ATTENDANCE ENGINE EVIDENCE
//
// Raw evidence captured for one attendance-engine evaluation.
//
// V1 valid presence:
//
// geofenceVerified && wifiVerified
//
// BLE remains evidence only.
// ============================================================

data class AttendanceEngineEvidence(

    // ========================================================
    // GPS / GEOFENCE
    // ========================================================

    val latitude: Double? = null,

    val longitude: Double? = null,

    val accuracyMeters: Double? = null,

    val geofenceVerified: Boolean = false,


    // ========================================================
    // WI-FI
    // ========================================================

    val wifiVerified: Boolean = false,

    val wifiBssid: String? = null,


    // ========================================================
    // BLE
    //
    // Optional / disabled from V1 presence decision.
    // ========================================================

    val beaconDetected: Boolean = false,

    val beaconId: String? = null,

    val beaconRssi: Int? = null,


    // ========================================================
    // MONOTONIC CAPTURE TIME
    //
    // Android elapsedRealtime().
    //
    // Phone wall-clock is NOT authoritative.
    // ========================================================

    val capturedAtElapsedRealtimeMs: Long

) {


    // ========================================================
    // CONVERT TO PURE PRESENCE SNAPSHOT
    // ========================================================

    fun toOfficePresenceSnapshot():
            OfficePresenceSnapshot {

        return OfficePresenceSnapshot(

            geofenceVerified =
                geofenceVerified,

            wifiVerified =
                wifiVerified,

            beaconVerified = beaconDetected,

            capturedAtElapsedRealtimeMs =
                capturedAtElapsedRealtimeMs
        )
    }
}


// ============================================================
// ATTENDANCE ENGINE EXECUTION RESULT
//
// Represents the result of one coordinator evaluation.
// ============================================================

sealed class AttendanceEngineExecutionResult {


    // ========================================================
    // NO ATTENDANCE EVENT REQUIRED
    //
    // Examples:
    //
    // - employee remains present
    // - absence grace started
    // - absence grace still running
    // - grace cancelled after return
    // - break still active
    // - already checked out
    // - ON_DUTY bypass
    //
    // Attendance state normally remains unchanged.
    //
    // Engine phase may change.
    // ========================================================

    data class NoEvent(

        val attendanceState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase,

        val reason:
        String

    ) : AttendanceEngineExecutionResult()


    // ========================================================
    // ATTENDANCE EVENT DURABLY STORED
    //
    // Represents either:
    //
    // StoredAndSynced
    //
    // OR
    //
    // StoredForRetry
    //
    // In both cases:
    //
    // attendance_events contains the real event.
    //
    // Therefore the local attendance state may advance.
    //
    //
    // ENGINE STATE PERSISTENCE
    //
    // engineStatePersisted tells the runtime whether the matching
    // attendance_engine_state row was also saved successfully.
    //
    // This distinction is VERY important.
    //
    // Example:
    //
    // BREAK_START successfully saved
    //        ↓
    // event exists in Room
    //        ↓
    // engine-state save fails
    //
    // We MUST NOT behave as if BREAK_START did not happen.
    //
    // Otherwise another evaluation could generate a duplicate
    // BREAK_START event.
    //
    // Therefore:
    //
    // engineStatePersisted = false
    //
    // means:
    //
    // event exists
    // +
    // intended local state is known
    // +
    // engine-state persistence/reconciliation is required
    //
    // It does NOT mean EventNotStored.
    // ========================================================

    data class EventStored(

        val eventUuid:
        String,

        val eventType:
        AttendanceEventType,

        val previousState:
        AttendanceState,

        val currentState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase,

        val waitingForSync:
        Boolean,

        val reason:
        String,


        // ====================================================
        // ENGINE STATE DURABILITY
        //
        // false by default because the current coordinator does
        // not yet call AttendanceEngineStateStore.
        //
        // In the next step the persistent coordinator will set
        // this to true after saveCurrentState() succeeds.
        // ====================================================

        val engineStatePersisted:
        Boolean = false,


        // ====================================================
        // ENGINE STATE PERSISTENCE ERROR
        //
        // Non-null only when:
        //
        // attendance event already exists
        //
        // BUT
        //
        // attendance_engine_state could not be updated.
        // ====================================================

        val engineStatePersistenceError:
        String? = null

    ) : AttendanceEngineExecutionResult()


    // ========================================================
    // EVENT WAS NOT STORED
    //
    // No attendance event exists in Room.
    //
    // Attendance state MUST NOT advance.
    //
    // Example:
    //
    // WORKING + AbsenceGrace
    //        ↓
    // BREAK_START attempted
    //        ↓
    // trusted anchor unavailable / Room error
    //        ↓
    // remain WORKING + AbsenceGrace
    // ========================================================

    data class EventNotStored(

        val attendanceState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase,

        val attemptedEvent:
        AttendanceEventType,

        val error:
        Throwable

    ) : AttendanceEngineExecutionResult()


    // ========================================================
    // LOCAL TRANSITION BLOCKED
    //
    // Invalid normal transitions stop BEFORE:
    //
    // - Room insertion
    // - API request
    // ========================================================

    data class TransitionBlocked(

        val attendanceState:
        AttendanceState,

        val attemptedEvent:
        AttendanceEventType,

        val phase:
        AttendanceEnginePhase,

        val reason:
        String

    ) : AttendanceEngineExecutionResult()


    // ========================================================
    // SERVER REJECTED STORED EVENT
    //
    // Event exists locally but Express rejected it.
    //
    // Local attendance state MUST NOT blindly advance.
    //
    // Server/local reconciliation is required.
    // ========================================================

    data class ReconciliationRequired(

        val eventUuid:
        String,

        val attemptedEvent:
        AttendanceEventType,

        val attendanceState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase,

        val code:
        String?,

        val message:
        String

    ) : AttendanceEngineExecutionResult()


    // ========================================================
    // ENGINE FAILURE
    //
    // Used when processing failed before we can safely commit a
    // new engine state.
    // ========================================================

    data class EngineFailure(

        val attendanceState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase,

        val error:
        Throwable

    ) : AttendanceEngineExecutionResult()
}
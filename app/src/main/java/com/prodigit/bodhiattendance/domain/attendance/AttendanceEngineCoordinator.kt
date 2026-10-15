package com.prodigit.bodhiattendance.domain.attendance

import com.prodigit.bodhiattendance.data.local.repository.AttendanceEngineStateStore
import com.prodigit.bodhiattendance.data.local.security.AttendanceEventManager
import com.prodigit.bodhiattendance.data.local.security.AttendanceEventRecordResult

import kotlinx.coroutines.CancellationException


// ============================================================
// ATTENDANCE ENGINE COORDINATOR
//
// Connects:
//
// Attendance state
//        +
// Current engine phase
//        +
// GPS / Geofence evidence
//        +
// Office Wi-Fi evidence
//        +
// CRM attendance policy
//        ↓
// AttendancePresenceDecisionEngine
//        ↓
// AttendanceStateMachine
//        ↓
// AttendanceEventManager
//        ↓
// Room-first attendance event
//        ↓
// Express synchronization
//        ↓
// AttendanceEngineStateStore
//
// ============================================================
//
// ENGINE STATE DURABILITY
//
// Safe state/phase changes are persisted into:
//
// attendance_engine_state
//
// This allows:
//
// - AbsenceGrace to survive process death
// - BreakActive to survive process death
// - original absence timestamps to survive process death
// - CHECKED_OUT state to survive process death
//
// IMPORTANT:
//
// attendance_events
//
// and
//
// attendance_engine_state
//
// are separate durable records.
//
// Therefore:
//
// if an attendance event is successfully stored,
// but saving attendance_engine_state fails afterward,
//
// the attendance event STILL EXISTS.
//
// We must NEVER convert that outcome to EventNotStored.
//
// Instead:
//
// EventStored(
//     engineStatePersisted = false
// )
//
// is returned.
//
// Runtime integration must then avoid generating another event
// until state persistence / reconciliation has been repaired.
// ============================================================

class AttendanceEngineCoordinator(

    private val attendanceEventManager:
    AttendanceEventManager,

    private val attendanceEngineStateStore:
    AttendanceEngineStateStore

) {


    // ========================================================
    // EVALUATE CURRENT ATTENDANCE CONDITION
    // ========================================================

    suspend fun evaluate(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String,

        currentAttendanceState:
        AttendanceState,

        currentPhase:
        AttendanceEnginePhase,

        evidence:
        AttendanceEngineEvidence,

        policy:
        AttendanceEnginePolicy

    ): AttendanceEngineExecutionResult {


        return try {


            // =================================================
            // BASIC RUNTIME VALIDATION
            // =================================================

            if (
                token.isBlank()
            ) {

                return AttendanceEngineExecutionResult
                    .EngineFailure(

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        error =
                            IllegalStateException(
                                "Authenticated session token is unavailable"
                            )
                    )
            }


            if (
                userId <= 0
            ) {

                return AttendanceEngineExecutionResult
                    .EngineFailure(

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        error =
                            IllegalStateException(
                                "Valid authenticated user ID is required"
                            )
                    )
            }


            if (
                companyId <= 0
            ) {

                return AttendanceEngineExecutionResult
                    .EngineFailure(

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        error =
                            IllegalStateException(
                                "Valid company ID is required"
                            )
                    )
            }


            if (
                deviceId.isBlank()
            ) {

                return AttendanceEngineExecutionResult
                    .EngineFailure(

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        error =
                            IllegalStateException(
                                "Registered device ID is unavailable"
                            )
                    )
            }


            if (
                evidence
                    .capturedAtElapsedRealtimeMs <
                0L
            ) {

                return AttendanceEngineExecutionResult
                    .EngineFailure(

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        error =
                            IllegalStateException(
                                "Presence snapshot monotonic time is invalid"
                            )
                    )
            }


            // =================================================
            // RAW EVIDENCE -> PRESENCE SNAPSHOT
            // =================================================

            val presenceSnapshot =
                evidence
                    .toOfficePresenceSnapshot()


            // =================================================
            // PURE BUSINESS DECISION
            // =================================================

            val decision =
                AttendancePresenceDecisionEngine
                    .evaluate(

                        attendanceState =
                            currentAttendanceState,

                        currentPhase =
                            currentPhase,

                        presence =
                            presenceSnapshot,

                        policy =
                            policy
                    )


            // =================================================
            // EXECUTE DECISION
            // =================================================

            when (decision) {


                // =============================================
                // NO ATTENDANCE EVENT
                //
                // The attendance state stays the same.
                //
                // The engine phase MAY change.
                //
                // Examples:
                //
                // Present -> AbsenceGrace
                //
                // AbsenceGrace -> Present
                //
                // BreakActive -> BreakActive
                //
                // CHECKED_OUT -> NotMonitoring
                // =============================================

                is AttendanceEngineDecision
                .NoAttendanceEvent -> {


                    executeNoEventDecision(

                        userId =
                            userId,

                        companyId =
                            companyId,

                        deviceId =
                            deviceId,

                        currentAttendanceState =
                            currentAttendanceState,

                        currentPhase =
                            currentPhase,

                        decision =
                            decision
                    )
                }


                // =============================================
                // NORMAL ATTENDANCE EVENT
                //
                // CHECK_IN
                // BREAK_START
                // BREAK_END
                // =============================================

                is AttendanceEngineDecision
                .CreateAttendanceEvent -> {


                    executeNormalAttendanceEvent(

                        token =
                            token,

                        userId =
                            userId,

                        companyId =
                            companyId,

                        deviceId =
                            deviceId,

                        currentAttendanceState =
                            currentAttendanceState,

                        currentPhase =
                            currentPhase,

                        decision =
                            decision,

                        evidence =
                            evidence
                    )
                }


                // =============================================
                // SPECIAL 90-MINUTE CHECKOUT CLASSIFICATION
                // =============================================

                is AttendanceEngineDecision
                .ClassifyCheckout -> {


                    executeCheckoutClassification(

                        token =
                            token,

                        userId =
                            userId,

                        companyId =
                            companyId,

                        deviceId =
                            deviceId,

                        currentAttendanceState =
                            currentAttendanceState,

                        currentPhase =
                            currentPhase,

                        decision =
                            decision,

                        evidence =
                            evidence
                    )
                }


                // =============================================
                // BLOCKED
                //
                // No attendance event.
                //
                // Do not mutate or persist a different state.
                // =============================================

                is AttendanceEngineDecision
                .Blocked -> {


                    AttendanceEngineExecutionResult
                        .TransitionBlocked(

                            attendanceState =
                                currentAttendanceState,

                            attemptedEvent =
                                inferAttemptedEvent(
                                    currentAttendanceState
                                ),

                            phase =
                                currentPhase,

                            reason =
                                decision.reason
                        )
                }
            }


        } catch (
            e: CancellationException
        ) {


            throw e


        } catch (
            e: Exception
        ) {


            AttendanceEngineExecutionResult
                .EngineFailure(

                    attendanceState =
                        currentAttendanceState,

                    phase =
                        currentPhase,

                    error =
                        e
                )
        }
    }


    // ========================================================
    // EXECUTE NO-EVENT DECISION
    //
    // Used for internal engine-phase changes that do NOT create
    // attendance history rows.
    //
    // Examples:
    //
    // WORKING + Present
    //        ↓ presence lost
    // WORKING + AbsenceGrace
    //
    //
    // WORKING + AbsenceGrace
    //        ↓ presence restored
    // WORKING + Present
    //
    // These phase changes must survive process death.
    // ========================================================

    private suspend fun executeNoEventDecision(

        userId: Int,

        companyId: Int,

        deviceId: String,

        currentAttendanceState:
        AttendanceState,

        currentPhase:
        AttendanceEnginePhase,

        decision:
        AttendanceEngineDecision.NoAttendanceEvent

    ): AttendanceEngineExecutionResult {


        // =====================================================
        // IF NOTHING CHANGED
        //
        // Avoid unnecessary Room writes on every GPS/Wi-Fi
        // observation.
        // =====================================================

        if (
            currentPhase ==
            decision.nextPhase
        ) {

            return AttendanceEngineExecutionResult
                .NoEvent(

                    attendanceState =
                        currentAttendanceState,

                    phase =
                        decision.nextPhase,

                    reason =
                        decision.reason
                )
        }


        // =====================================================
        // PHASE CHANGED
        //
        // Persist BEFORE reporting the new phase as committed.
        // =====================================================

        val persistenceResult =
            persistEngineState(

                userId =
                    userId,

                companyId =
                    companyId,

                deviceId =
                    deviceId,

                attendanceState =
                    currentAttendanceState,

                phase =
                    decision.nextPhase
            )


        // =====================================================
        // STATE PERSISTENCE FAILED
        //
        // No attendance event exists.
        //
        // Therefore it is safe to keep using the OLD phase.
        // =====================================================

        if (
            persistenceResult.isFailure
        ) {

            return AttendanceEngineExecutionResult
                .EngineFailure(

                    attendanceState =
                        currentAttendanceState,

                    phase =
                        currentPhase,

                    error =
                        persistenceResult
                            .exceptionOrNull()
                            ?: IllegalStateException(
                                "Unable to persist attendance engine phase"
                            )
                )
        }


        // =====================================================
        // SAFE PHASE COMMIT
        // =====================================================

        return AttendanceEngineExecutionResult
            .NoEvent(

                attendanceState =
                    currentAttendanceState,

                phase =
                    decision.nextPhase,

                reason =
                    decision.reason
            )
    }


    // ========================================================
    // EXECUTE NORMAL ATTENDANCE EVENT
    //
    // CHECK_IN
    // BREAK_START
    // BREAK_END
    // ========================================================

    private suspend fun executeNormalAttendanceEvent(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String,

        currentAttendanceState:
        AttendanceState,

        currentPhase:
        AttendanceEnginePhase,

        decision:
        AttendanceEngineDecision.CreateAttendanceEvent,

        evidence:
        AttendanceEngineEvidence

    ): AttendanceEngineExecutionResult {


        // =====================================================
        // FINAL TRANSITION VALIDATION
        //
        // Invalid normal transitions must stop BEFORE Room.
        // =====================================================

        val transition =
            AttendanceStateMachine
                .validate(

                    currentState =
                        currentAttendanceState,

                    eventType =
                        decision.eventType
                )


        when (transition) {


            is AttendanceTransitionResult
            .Blocked -> {


                return AttendanceEngineExecutionResult
                    .TransitionBlocked(

                        attendanceState =
                            currentAttendanceState,

                        attemptedEvent =
                            decision.eventType,

                        phase =
                            currentPhase,

                        reason =
                            transition.reason
                    )
            }


            is AttendanceTransitionResult
            .Allowed -> {


                if (
                    transition.nextState !=
                    decision.expectedNextAttendanceState
                ) {

                    return AttendanceEngineExecutionResult
                        .TransitionBlocked(

                            attendanceState =
                                currentAttendanceState,

                            attemptedEvent =
                                decision.eventType,

                            phase =
                                currentPhase,

                            reason =
                                "Attendance decision/state-machine next-state mismatch"
                        )
                }
            }
        }


        // =====================================================
        // ROOM-FIRST ATTENDANCE EVENT
        // =====================================================

        val recordResult =
            attendanceEventManager
                .recordEvent(

                    token =
                        token,

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    eventType =
                        decision
                            .eventType
                            .name,

                    arrivalProof = if (decision.eventType == AttendanceEventType.CHECK_IN) evidence.arrivalProof else null,
                    attendanceReason = decision.reason,
                    eventElapsedRealtimeMsOverride =
                        decision
                            .eventElapsedRealtimeMs,


                    // =========================================
                    // GPS
                    // =========================================

                    latitude =
                        evidence.latitude,

                    longitude =
                        evidence.longitude,

                    accuracyMeters =
                        evidence.accuracyMeters,

                    insideGeofence =
                        evidence.geofenceVerified,


                    // =========================================
                    // BLE
                    // =========================================

                    beaconDetected =
                        evidence.beaconDetected,

                    beaconId =
                        evidence.beaconId,

                    beaconRssi =
                        evidence.beaconRssi,


                    // =========================================
                    // WI-FI
                    // =========================================

                    wifiVerified =
                        evidence.wifiVerified,

                    wifiBssid =
                        evidence.wifiBssid
                )


        // =====================================================
        // INTERPRET EVENT RECORD OUTCOME
        // =====================================================

        return when (recordResult) {


            // =================================================
            // NO ATTENDANCE EVENT EXISTS
            //
            // Keep OLD attendance state + OLD phase.
            // =================================================

            is AttendanceEventRecordResult
            .NotStored -> {


                AttendanceEngineExecutionResult
                    .EventNotStored(

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        attemptedEvent =
                            decision.eventType,

                        error =
                            recordResult.error
                    )
            }


            // =================================================
            // EVENT EXISTS + EXPRESS ACCEPTED
            // =================================================

            is AttendanceEventRecordResult
            .StoredAndSynced -> {


                buildStoredEventResult(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    eventUuid =
                        recordResult.eventUuid,

                    eventType =
                        decision.eventType,

                    previousState =
                        currentAttendanceState,

                    currentState =
                        decision
                            .expectedNextAttendanceState,

                    phase =
                        decision.nextPhase,

                    waitingForSync =
                        false,

                    reason =
                        decision.reason
                )
            }


            // =================================================
            // EVENT EXISTS LOCALLY
            //
            // Remote sync will retry later.
            //
            // We MUST advance local state because Room already
            // contains the genuine attendance event.
            // =================================================

            is AttendanceEventRecordResult
            .StoredForRetry -> {


                buildStoredEventResult(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    eventUuid =
                        recordResult.eventUuid,

                    eventType =
                        decision.eventType,

                    previousState =
                        currentAttendanceState,

                    currentState =
                        decision
                            .expectedNextAttendanceState,

                    phase =
                        decision.nextPhase,

                    waitingForSync =
                        true,

                    reason =
                        decision.reason
                )
            }


            // =================================================
            // EXPRESS REJECTED
            //
            // Do NOT advance local state.
            // =================================================

            is AttendanceEventRecordResult
            .StoredRejected -> {


                AttendanceEngineExecutionResult
                    .ReconciliationRequired(

                        eventUuid =
                            recordResult.eventUuid,

                        attemptedEvent =
                            decision.eventType,

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        code =
                            recordResult
                                .response
                                .code,

                        message =
                            recordResult.error
                    )
            }
        }
    }


    // ========================================================
    // EXECUTE SPECIAL DELAYED CHECKOUT
    //
    // Normal StateMachine intentionally does NOT allow:
    //
    // ON_BREAK -> CHECK_OUT
    //
    // This is a separate attendance-engine policy classification.
    // ========================================================

    private suspend fun executeCheckoutClassification(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String,

        currentAttendanceState:
        AttendanceState,

        currentPhase:
        AttendanceEnginePhase,

        decision:
        AttendanceEngineDecision.ClassifyCheckout,

        evidence:
        AttendanceEngineEvidence

    ): AttendanceEngineExecutionResult {


        // =====================================================
        // POLICY SAFETY
        // =====================================================

        if (
            currentAttendanceState !=
            AttendanceState.ON_BREAK
        ) {

            return AttendanceEngineExecutionResult
                .TransitionBlocked(

                    attendanceState =
                        currentAttendanceState,

                    attemptedEvent =
                        AttendanceEventType.CHECK_OUT,

                    phase =
                        currentPhase,

                    reason =
                        "Automatic checkout classification requires ON_BREAK attendance state"
                )
        }


        if (
            currentPhase !is
                    AttendanceEnginePhase.BreakActive
        ) {

            return AttendanceEngineExecutionResult
                .TransitionBlocked(

                    attendanceState =
                        currentAttendanceState,

                    attemptedEvent =
                        AttendanceEventType.CHECK_OUT,

                    phase =
                        currentPhase,

                    reason =
                        "Automatic checkout classification requires BreakActive timing context"
                )
        }


        // =====================================================
        // CREATE CHECK_OUT USING ORIGINAL EXIT TIME
        // =====================================================

        val recordResult =
            attendanceEventManager
                .recordEvent(

                    token =
                        token,

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    eventType =
                        AttendanceEventType
                            .CHECK_OUT
                            .name,

                    attendanceReason = decision.reason,
                    eventElapsedRealtimeMsOverride =
                        decision
                            .effectiveCheckoutElapsedRealtimeMs,


                    // =========================================
                    // CURRENT EVIDENCE
                    // =========================================

                    latitude =
                        evidence.latitude,

                    longitude =
                        evidence.longitude,

                    accuracyMeters =
                        evidence.accuracyMeters,

                    insideGeofence =
                        evidence.geofenceVerified,

                    beaconDetected =
                        evidence.beaconDetected,

                    beaconId =
                        evidence.beaconId,

                    beaconRssi =
                        evidence.beaconRssi,

                    wifiVerified =
                        evidence.wifiVerified,

                    wifiBssid =
                        evidence.wifiBssid
                )


        return when (recordResult) {


            // =================================================
            // NO CHECKOUT EVENT EXISTS
            //
            // Remain ON_BREAK + BreakActive.
            // =================================================

            is AttendanceEventRecordResult
            .NotStored -> {


                AttendanceEngineExecutionResult
                    .EventNotStored(

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        attemptedEvent =
                            AttendanceEventType.CHECK_OUT,

                        error =
                            recordResult.error
                    )
            }


            // =================================================
            // CHECKOUT STORED + SYNCED
            // =================================================

            is AttendanceEventRecordResult
            .StoredAndSynced -> {


                buildStoredEventResult(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    eventUuid =
                        recordResult.eventUuid,

                    eventType =
                        AttendanceEventType.CHECK_OUT,

                    previousState =
                        AttendanceState.ON_BREAK,

                    currentState =
                        AttendanceState.CHECKED_OUT,

                    phase =
                        decision.nextPhase,

                    waitingForSync =
                        false,

                    reason =
                        decision.reason
                )
            }


            // =================================================
            // CHECKOUT STORED OFFLINE
            // =================================================

            is AttendanceEventRecordResult
            .StoredForRetry -> {


                buildStoredEventResult(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    eventUuid =
                        recordResult.eventUuid,

                    eventType =
                        AttendanceEventType.CHECK_OUT,

                    previousState =
                        AttendanceState.ON_BREAK,

                    currentState =
                        AttendanceState.CHECKED_OUT,

                    phase =
                        decision.nextPhase,

                    waitingForSync =
                        true,

                    reason =
                        decision.reason
                )
            }


            // =================================================
            // SERVER REJECTED
            // =================================================

            is AttendanceEventRecordResult
            .StoredRejected -> {


                AttendanceEngineExecutionResult
                    .ReconciliationRequired(

                        eventUuid =
                            recordResult.eventUuid,

                        attemptedEvent =
                            AttendanceEventType.CHECK_OUT,

                        attendanceState =
                            currentAttendanceState,

                        phase =
                            currentPhase,

                        code =
                            recordResult
                                .response
                                .code,

                        message =
                            recordResult.error
                    )
            }
        }
    }


    // ========================================================
    // BUILD STORED EVENT RESULT
    //
    // At this point:
    //
    // attendance event already EXISTS in attendance_events.
    //
    // Now persist matching engine state.
    //
    // IMPORTANT:
    //
    // Failure here cannot undo the attendance event.
    // ========================================================

    private suspend fun buildStoredEventResult(

        userId: Int,

        companyId: Int,

        deviceId: String,

        eventUuid: String,

        eventType:
        AttendanceEventType,

        previousState:
        AttendanceState,

        currentState:
        AttendanceState,

        phase:
        AttendanceEnginePhase,

        waitingForSync: Boolean,

        reason: String

    ): AttendanceEngineExecutionResult.EventStored {


        val persistenceResult =
            persistEngineState(

                userId =
                    userId,

                companyId =
                    companyId,

                deviceId =
                    deviceId,

                attendanceState =
                    currentState,

                phase =
                    phase
            )


        // =====================================================
        // ENGINE STATE SAVED
        // =====================================================

        if (
            persistenceResult.isSuccess
        ) {

            return AttendanceEngineExecutionResult
                .EventStored(

                    eventUuid =
                        eventUuid,

                    eventType =
                        eventType,

                    previousState =
                        previousState,

                    currentState =
                        currentState,

                    phase =
                        phase,

                    waitingForSync =
                        waitingForSync,

                    reason =
                        reason,

                    engineStatePersisted =
                        true,

                    engineStatePersistenceError =
                        null
                )
        }


        // =====================================================
        // ATTENDANCE EVENT EXISTS
        //
        // BUT ENGINE STATE SAVE FAILED.
        //
        // DO NOT return EventNotStored.
        //
        // The runtime must preserve currentState + phase in
        // memory and enter persistence-recovery/reconciliation
        // mode before generating another event.
        // =====================================================

        val error =
            persistenceResult
                .exceptionOrNull()


        return AttendanceEngineExecutionResult
            .EventStored(

                eventUuid =
                    eventUuid,

                eventType =
                    eventType,

                previousState =
                    previousState,

                currentState =
                    currentState,

                phase =
                    phase,

                waitingForSync =
                    waitingForSync,

                reason =
                    reason,

                engineStatePersisted =
                    false,

                engineStatePersistenceError =
                    error?.message
                        ?: "Attendance event stored but engine state persistence failed"
            )
    }


    // ========================================================
    // PERSIST ENGINE STATE SAFELY
    // ========================================================

    private suspend fun persistEngineState(

        userId: Int,

        companyId: Int,

        deviceId: String,

        attendanceState:
        AttendanceState,

        phase:
        AttendanceEnginePhase

    ): Result<Unit> {


        return try {


            attendanceEngineStateStore
                .saveCurrentState(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    attendanceState =
                        attendanceState,

                    phase =
                        phase
                )


            Result.success(
                Unit
            )


        } catch (
            e: CancellationException
        ) {


            throw e


        } catch (
            e: Exception
        ) {


            Result.failure(
                e
            )
        }
    }


    // ========================================================
    // DIAGNOSTIC EVENT TYPE FOR GENERIC BLOCKED RESULT
    //
    // No Room event is created from this value.
    // ========================================================

    private fun inferAttemptedEvent(

        attendanceState:
        AttendanceState

    ): AttendanceEventType {


        return when (
            attendanceState
        ) {


            AttendanceState.NOT_CHECKED_IN ->
                AttendanceEventType.CHECK_IN


            AttendanceState.WORKING ->
                AttendanceEventType.BREAK_START


            AttendanceState.ON_BREAK ->
                AttendanceEventType.BREAK_END


            AttendanceState.CHECKED_OUT ->
                AttendanceEventType.CHECK_OUT


            AttendanceState.ON_DUTY ->
                AttendanceEventType.CHECK_OUT
        }
    }
}
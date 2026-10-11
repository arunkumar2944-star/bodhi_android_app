package com.prodigit.bodhiattendance.domain.attendance

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


// ============================================================
// ATTENDANCE ENGINE RUNTIME MODE
// ============================================================

enum class AttendanceEngineRuntimeMode {

    UNINITIALIZED,

    READY,

    RECONCILIATION_REQUIRED,

    PERSISTENCE_RECOVERY_REQUIRED
}


// ============================================================
// RUNTIME SNAPSHOT
// ============================================================

data class AttendanceEngineRuntimeSnapshot(

    val mode: AttendanceEngineRuntimeMode,

    val userId: Int?,

    val deviceId: String?,

    val attendanceState: AttendanceState?,

    val phase: AttendanceEnginePhase?,

    val reason: String?
)


// ============================================================
// RUNTIME INITIALIZATION RESULT
// ============================================================

sealed class AttendanceEngineRuntimeStartResult {


    data class Ready(

        val snapshot: AttendanceEngineRuntimeSnapshot

    ) : AttendanceEngineRuntimeStartResult()


    object StateUnavailable :
        AttendanceEngineRuntimeStartResult()


    data class RebootReconciliationRequired(

        val storedAttendanceState: AttendanceState,

        val storedBootId: String,

        val currentBootId: String

    ) : AttendanceEngineRuntimeStartResult()


    data class InvalidStateReconciliationRequired(

        val reason: String

    ) : AttendanceEngineRuntimeStartResult()


    data class Failure(

        val error: Throwable

    ) : AttendanceEngineRuntimeStartResult()
}


// ============================================================
// RUNTIME EVALUATION RESULT
// ============================================================

sealed class AttendanceEngineRuntimeEvaluationResult {


    data class Executed(

        val result: AttendanceEngineExecutionResult,

        val snapshot: AttendanceEngineRuntimeSnapshot

    ) : AttendanceEngineRuntimeEvaluationResult()


    data class Blocked(

        val snapshot: AttendanceEngineRuntimeSnapshot,

        val reason: String

    ) : AttendanceEngineRuntimeEvaluationResult()
}


// ============================================================
// ATTENDANCE ENGINE RUNTIME SESSION
//
// Owns the active in-memory automatic-attendance state.
//
// Responsibilities:
//
// - restore persisted engine state
// - verify restored state against latest durable local event
// - hold current AttendanceState
// - hold current AttendanceEnginePhase
// - serialize evaluations
// - apply coordinator results
// - stop automatic events during reconciliation
// ============================================================

class AttendanceEngineRuntimeSession(

    private val runtimeRestorer:
    AttendanceEngineRuntimeRestorer,

    private val startupConsistencyGuard:
    AttendanceEngineStartupConsistencyGuard,

    private val coordinator:
    AttendanceEngineCoordinator

) {


    // ========================================================
    // SERIALIZATION
    //
    // Prevent GPS and Wi-Fi callbacks from evaluating the same
    // state simultaneously.
    // ========================================================

    private val evaluationMutex =
        Mutex()


    // ========================================================
    // BOUND IDENTITY
    // ========================================================

    private var boundUserId: Int? =
        null

    private var boundDeviceId: String? =
        null


    // ========================================================
    // CURRENT ENGINE STATE
    // ========================================================

    private var currentAttendanceState: AttendanceState? =
        null

    private var currentPhase: AttendanceEnginePhase? =
        null


    // ========================================================
    // CURRENT RUNTIME MODE
    // ========================================================

    private var runtimeMode:
            AttendanceEngineRuntimeMode =
        AttendanceEngineRuntimeMode.UNINITIALIZED


    private var runtimeReason: String? =
        null


    // ========================================================
    // INITIALIZE
    //
    // Startup sequence:
    //
    // Room engine state
    //        ↓
    // safe same-boot restore?
    //        ↓
    // startup consistency guard
    //        ↓
    // compare restored state with latest durable event
    //        ↓
    // consistent?
    //
    // YES -> READY
    //
    // NO  -> RECONCILIATION_REQUIRED
    // ========================================================

    suspend fun initialize(

        userId: Int,

        deviceId: String

    ): AttendanceEngineRuntimeStartResult {


        return evaluationMutex.withLock {


            // =================================================
            // USER VALIDATION
            // =================================================

            if (userId <= 0) {

                resetInMemory()

                return@withLock AttendanceEngineRuntimeStartResult
                    .Failure(

                        error =
                            IllegalStateException(
                                "Valid authenticated user ID is required"
                            )
                    )
            }


            // =================================================
            // DEVICE VALIDATION
            // =================================================

            if (deviceId.isBlank()) {

                resetInMemory()

                return@withLock AttendanceEngineRuntimeStartResult
                    .Failure(

                        error =
                            IllegalStateException(
                                "Registered device ID is required"
                            )
                    )
            }


            // =================================================
            // BIND USER + DEVICE
            // =================================================

            boundUserId =
                userId

            boundDeviceId =
                deviceId


            // =================================================
            // RESTORE STATE
            // =================================================

            val restoreResult =
                try {

                    runtimeRestorer
                        .restore(

                            userId =
                                userId,

                            deviceId =
                                deviceId
                        )

                } catch (
                    e: CancellationException
                ) {

                    throw e

                } catch (
                    e: Exception
                ) {

                    currentAttendanceState =
                        null

                    currentPhase =
                        null

                    runtimeMode =
                        AttendanceEngineRuntimeMode
                            .RECONCILIATION_REQUIRED

                    runtimeReason =
                        e.message
                            ?: "Attendance runtime restoration failed"


                    return@withLock AttendanceEngineRuntimeStartResult
                        .Failure(
                            error = e
                        )
                }


            // =================================================
            // MAP RESTORE RESULT
            // =================================================

            when (restoreResult) {


                // =============================================
                // SAFE SAME-BOOT RESTORE
                //
                // IMPORTANT:
                //
                // A valid engine-state row alone is NOT enough.
                //
                // Example crash window:
                //
                // BREAK_START saved in attendance_events
                //        ↓
                // process dies
                //        ↓
                // attendance_engine_state still says WORKING
                //
                // Therefore run consistency guard before READY.
                // =============================================

                is AttendanceEngineRuntimeRestoreResult
                .Ready -> {


                    val consistencyResult =
                        try {

                            startupConsistencyGuard
                                .check(

                                    userId =
                                        userId,

                                    deviceId =
                                        deviceId,

                                    restoredAttendanceState =
                                        restoreResult
                                            .attendanceState,

                                    restoredPhase =
                                        restoreResult
                                            .phase
                                )

                        } catch (
                            e: CancellationException
                        ) {

                            throw e

                        } catch (
                            e: Exception
                        ) {

                            currentAttendanceState =
                                null

                            currentPhase =
                                null

                            runtimeMode =
                                AttendanceEngineRuntimeMode
                                    .RECONCILIATION_REQUIRED

                            runtimeReason =
                                e.message
                                    ?: "Attendance startup consistency check failed"


                            return@withLock AttendanceEngineRuntimeStartResult
                                .Failure(
                                    error = e
                                )
                        }


                    when (consistencyResult) {


                        // =====================================
                        // ENGINE STATE + LATEST EVENT AGREE
                        // =====================================

                        is AttendanceEngineStartupConsistencyResult
                        .Consistent -> {


                            currentAttendanceState =
                                consistencyResult
                                    .attendanceState

                            currentPhase =
                                consistencyResult
                                    .phase

                            runtimeMode =
                                AttendanceEngineRuntimeMode.READY

                            runtimeReason =
                                null


                            AttendanceEngineRuntimeStartResult
                                .Ready(

                                    snapshot =
                                        createSnapshot()
                                )
                        }


                        // =====================================
                        // NO LOCAL ATTENDANCE EVENT
                        //
                        // Restore result already passed:
                        //
                        // - boot validation
                        // - monotonic validation
                        // - phase reconstruction
                        //
                        // Therefore locally restored state may
                        // continue.
                        //
                        // Typical example:
                        //
                        // NOT_CHECKED_IN
                        // +
                        // NotMonitoring
                        // =====================================

                        is AttendanceEngineStartupConsistencyResult
                        .NoLocalAttendanceEvent -> {


                            currentAttendanceState =
                                consistencyResult
                                    .attendanceState

                            currentPhase =
                                consistencyResult
                                    .phase

                            runtimeMode =
                                AttendanceEngineRuntimeMode.READY

                            runtimeReason =
                                null


                            AttendanceEngineRuntimeStartResult
                                .Ready(

                                    snapshot =
                                        createSnapshot()
                                )
                        }


                        // =====================================
                        // STALE ENGINE STATE
                        //
                        // Example:
                        //
                        // engine state:
                        // WORKING
                        //
                        // latest durable event:
                        // BREAK_START
                        //
                        // Runtime must NOT continue.
                        // =====================================

                        is AttendanceEngineStartupConsistencyResult
                        .ReconciliationRequired -> {


                            // Preserve restored state only for
                            // diagnostics.
                            //
                            // Automatic evaluation is blocked by
                            // runtimeMode.

                            currentAttendanceState =
                                consistencyResult
                                    .restoredAttendanceState

                            currentPhase =
                                consistencyResult
                                    .restoredPhase

                            runtimeMode =
                                AttendanceEngineRuntimeMode
                                    .RECONCILIATION_REQUIRED

                            runtimeReason =
                                consistencyResult
                                    .reason


                            AttendanceEngineRuntimeStartResult
                                .InvalidStateReconciliationRequired(

                                    reason =
                                        consistencyResult
                                            .reason
                                )
                        }


                        // =====================================
                        // UNKNOWN LOCAL EVENT TYPE
                        // =====================================

                        is AttendanceEngineStartupConsistencyResult
                        .InvalidLocalEvent -> {


                            currentAttendanceState =
                                null

                            currentPhase =
                                null

                            runtimeMode =
                                AttendanceEngineRuntimeMode
                                    .RECONCILIATION_REQUIRED

                            runtimeReason =
                                consistencyResult
                                    .reason


                            AttendanceEngineRuntimeStartResult
                                .InvalidStateReconciliationRequired(

                                    reason =
                                        consistencyResult
                                            .reason
                                )
                        }


                        // =====================================
                        // CONSISTENCY CHECK STORAGE FAILURE
                        // =====================================

                        is AttendanceEngineStartupConsistencyResult
                        .Failure -> {


                            currentAttendanceState =
                                null

                            currentPhase =
                                null

                            runtimeMode =
                                AttendanceEngineRuntimeMode
                                    .RECONCILIATION_REQUIRED

                            runtimeReason =
                                consistencyResult
                                    .error
                                    .message
                                    ?: "Attendance startup consistency check failed"


                            AttendanceEngineRuntimeStartResult
                                .Failure(

                                    error =
                                        consistencyResult
                                            .error
                                )
                        }
                    }
                }


                // =============================================
                // NO LOCAL ENGINE STATE
                //
                // IMPORTANT:
                //
                // Never guess NOT_CHECKED_IN.
                //
                // Employee may already have an active attendance
                // session on Express.
                // =============================================

                AttendanceEngineRuntimeRestoreResult
                    .StateUnavailable -> {


                    currentAttendanceState =
                        null

                    currentPhase =
                        null

                    runtimeMode =
                        AttendanceEngineRuntimeMode
                            .RECONCILIATION_REQUIRED

                    runtimeReason =
                        "No persisted attendance engine state is available"


                    AttendanceEngineRuntimeStartResult
                        .StateUnavailable
                }


                // =============================================
                // PHONE REBOOT
                //
                // elapsedRealtime() restarted from zero.
                //
                // Old grace/break timestamps cannot be reused.
                // =============================================

                is AttendanceEngineRuntimeRestoreResult
                .RebootReconciliationRequired -> {


                    currentAttendanceState =
                        null

                    currentPhase =
                        null

                    runtimeMode =
                        AttendanceEngineRuntimeMode
                            .RECONCILIATION_REQUIRED

                    runtimeReason =
                        "Android boot session changed; authoritative attendance reconciliation is required"


                    AttendanceEngineRuntimeStartResult
                        .RebootReconciliationRequired(

                            storedAttendanceState =
                                restoreResult
                                    .storedAttendanceState,

                            storedBootId =
                                restoreResult
                                    .storedBootId,

                            currentBootId =
                                restoreResult
                                    .currentBootId
                        )
                }


                // =============================================
                // INVALID LOCAL STATE
                // =============================================

                is AttendanceEngineRuntimeRestoreResult
                .InvalidStateReconciliationRequired -> {


                    currentAttendanceState =
                        null

                    currentPhase =
                        null

                    runtimeMode =
                        AttendanceEngineRuntimeMode
                            .RECONCILIATION_REQUIRED

                    runtimeReason =
                        restoreResult.reason


                    AttendanceEngineRuntimeStartResult
                        .InvalidStateReconciliationRequired(

                            reason =
                                restoreResult.reason
                        )
                }


                // =============================================
                // RESTORE FAILURE
                // =============================================

                is AttendanceEngineRuntimeRestoreResult
                .Failure -> {


                    currentAttendanceState =
                        null

                    currentPhase =
                        null

                    runtimeMode =
                        AttendanceEngineRuntimeMode
                            .RECONCILIATION_REQUIRED

                    runtimeReason =
                        restoreResult
                            .error
                            .message
                            ?: "Attendance runtime restoration failed"


                    AttendanceEngineRuntimeStartResult
                        .Failure(

                            error =
                                restoreResult.error
                        )
                }
            }
        }
    }


    // ========================================================
    // PROCESS ONE PRESENCE OBSERVATION
    // ========================================================

    suspend fun evaluate(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String,

        evidence:
        AttendanceEngineEvidence,

        policy:
        AttendanceEnginePolicy

    ): AttendanceEngineRuntimeEvaluationResult {


        return evaluationMutex.withLock {


            // =================================================
            // RUNTIME MUST BE READY
            // =================================================

            if (
                runtimeMode !=
                AttendanceEngineRuntimeMode.READY
            ) {

                return@withLock AttendanceEngineRuntimeEvaluationResult
                    .Blocked(

                        snapshot =
                            createSnapshot(),

                        reason =
                            runtimeReason
                                ?: "Attendance runtime is not ready"
                    )
            }


            // =================================================
            // USER MUST MATCH
            // =================================================

            if (
                boundUserId !=
                userId
            ) {

                return@withLock AttendanceEngineRuntimeEvaluationResult
                    .Blocked(

                        snapshot =
                            createSnapshot(),

                        reason =
                            "Attendance runtime user does not match authenticated user"
                    )
            }


            // =================================================
            // DEVICE MUST MATCH
            // =================================================

            if (
                boundDeviceId !=
                deviceId
            ) {

                return@withLock AttendanceEngineRuntimeEvaluationResult
                    .Blocked(

                        snapshot =
                            createSnapshot(),

                        reason =
                            "Attendance runtime device does not match registered device"
                    )
            }


            // =================================================
            // CURRENT ATTENDANCE STATE
            // =================================================

            val attendanceState =
                currentAttendanceState
                    ?: return@withLock AttendanceEngineRuntimeEvaluationResult
                        .Blocked(

                            snapshot =
                                createSnapshot(),

                            reason =
                                "Current attendance state is unavailable"
                        )


            // =================================================
            // CURRENT ENGINE PHASE
            // =================================================

            val phase =
                currentPhase
                    ?: return@withLock AttendanceEngineRuntimeEvaluationResult
                        .Blocked(

                            snapshot =
                                createSnapshot(),

                            reason =
                                "Current attendance engine phase is unavailable"
                        )


            // =================================================
            // EXECUTE COORDINATOR
            // =================================================

            val result =
                try {

                    coordinator
                        .evaluate(

                            token =
                                token,

                            userId =
                                userId,

                            companyId =
                                companyId,

                            deviceId =
                                deviceId,

                            currentAttendanceState =
                                attendanceState,

                            currentPhase =
                                phase,

                            evidence =
                                evidence,

                            policy =
                                policy
                        )

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
                                attendanceState,

                            phase =
                                phase,

                            error =
                                e
                        )
                }


            // =================================================
            // APPLY RESULT TO MEMORY
            // =================================================

            applyExecutionResult(
                result = result
            )


            // =================================================
            // RETURN FINAL RUNTIME RESULT
            // =================================================

            AttendanceEngineRuntimeEvaluationResult
                .Executed(

                    result =
                        result,

                    snapshot =
                        createSnapshot()
                )
        }
    }


    // ========================================================
    // APPLY COORDINATOR RESULT
    // ========================================================

    private fun applyExecutionResult(

        result:
        AttendanceEngineExecutionResult

    ) {


        when (result) {


            // =================================================
            // SAFE NO-EVENT PHASE CHANGE
            //
            // Coordinator already persisted the phase.
            // =================================================

            is AttendanceEngineExecutionResult
            .NoEvent -> {


                currentAttendanceState =
                    result.attendanceState

                currentPhase =
                    result.phase
            }


            // =================================================
            // DURABLE ATTENDANCE EVENT EXISTS
            // =================================================

            is AttendanceEngineExecutionResult
            .EventStored -> {


                // =============================================
                // ALWAYS ADVANCE MEMORY
                //
                // attendance_events already contains the event.
                // =============================================

                currentAttendanceState =
                    result.currentState

                currentPhase =
                    result.phase


                // =============================================
                // EVENT + ENGINE STATE PERSISTED
                // =============================================

                if (
                    result.engineStatePersisted
                ) {

                    runtimeMode =
                        AttendanceEngineRuntimeMode.READY

                    runtimeReason =
                        null
                }


                // =============================================
                // EVENT EXISTS
                //
                // ENGINE STATE SAVE FAILED
                //
                // Do NOT allow another automatic event.
                // =============================================

                else {

                    runtimeMode =
                        AttendanceEngineRuntimeMode
                            .PERSISTENCE_RECOVERY_REQUIRED

                    runtimeReason =
                        result
                            .engineStatePersistenceError
                            ?: "Attendance event exists but engine state persistence requires recovery"
                }
            }


            // =================================================
            // EVENT NOT STORED
            //
            // Keep current in-memory state unchanged.
            // =================================================

            is AttendanceEngineExecutionResult
            .EventNotStored -> {

                // No mutation.
            }


            // =================================================
            // LOCAL STATE/PHASE INCONSISTENCY
            //
            // Stop automatic processing.
            // =================================================

            is AttendanceEngineExecutionResult
            .TransitionBlocked -> {


                runtimeMode =
                    AttendanceEngineRuntimeMode
                        .RECONCILIATION_REQUIRED

                runtimeReason =
                    result.reason
            }


            // =================================================
            // SERVER REJECTION
            //
            // Durable local event exists but server disagrees.
            // =================================================

            is AttendanceEngineExecutionResult
            .ReconciliationRequired -> {


                runtimeMode =
                    AttendanceEngineRuntimeMode
                        .RECONCILIATION_REQUIRED

                runtimeReason =
                    result.message
            }


            // =================================================
            // ENGINE FAILURE
            //
            // Preserve last successfully committed state.
            // =================================================

            is AttendanceEngineExecutionResult
            .EngineFailure -> {

                // No mutation.
            }
        }
    }

    // ========================================================
// APPLY RECONCILED BOOTSTRAP STATE
//
// Called only after AttendanceEngineReconciliationManager
// successfully:
//
// - obtained authoritative server state
// - created fresh trusted-time anchor
// - persisted reconciled engine state
//
// Therefore it is safe to return runtime to READY.
// ========================================================

    suspend fun applyReconciledState(

        userId: Int,

        deviceId: String,

        bootstrapResult:
        AttendanceEngineBootstrapResult.Ready

    ): AttendanceEngineRuntimeSnapshot {


        return evaluationMutex.withLock {


            // =====================================================
            // SESSION IDENTITY MUST MATCH
            // =====================================================

            if (
                boundUserId !=
                userId
            ) {

                throw IllegalStateException(
                    "Reconciled user does not match attendance runtime user"
                )
            }


            if (
                boundDeviceId !=
                deviceId
            ) {

                throw IllegalStateException(
                    "Reconciled device does not match attendance runtime device"
                )
            }


            // =====================================================
            // APPLY SAFE RECONCILED STATE
            // =====================================================

            currentAttendanceState =
                bootstrapResult
                    .attendanceState

            currentPhase =
                bootstrapResult
                    .phase

            runtimeMode =
                AttendanceEngineRuntimeMode.READY

            runtimeReason =
                null


            createSnapshot()
        }
    }


    // ========================================================
    // GET CURRENT SNAPSHOT
    // ========================================================

    suspend fun getSnapshot():
            AttendanceEngineRuntimeSnapshot {


        return evaluationMutex.withLock {

            createSnapshot()
        }
    }


    // ========================================================
    // CLEAR IN-MEMORY RUNTIME ONLY
    //
    // This does NOT delete:
    //
    // attendance_events
    // attendance_engine_state
    // time_anchors
    //
    // Persistent logout/reset remains separate.
    // ========================================================

    suspend fun clearInMemory() {


        evaluationMutex.withLock {

            resetInMemory()
        }
    }


    // ========================================================
    // CREATE SNAPSHOT
    // ========================================================

    private fun createSnapshot():
            AttendanceEngineRuntimeSnapshot {


        return AttendanceEngineRuntimeSnapshot(

            mode =
                runtimeMode,

            userId =
                boundUserId,

            deviceId =
                boundDeviceId,

            attendanceState =
                currentAttendanceState,

            phase =
                currentPhase,

            reason =
                runtimeReason
        )
    }


    // ========================================================
    // RESET MEMORY
    // ========================================================

    private fun resetInMemory() {


        boundUserId =
            null

        boundDeviceId =
            null

        currentAttendanceState =
            null

        currentPhase =
            null

        runtimeMode =
            AttendanceEngineRuntimeMode.UNINITIALIZED

        runtimeReason =
            null
    }
}
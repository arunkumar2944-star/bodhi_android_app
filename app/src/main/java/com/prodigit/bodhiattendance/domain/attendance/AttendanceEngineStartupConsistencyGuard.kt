package com.prodigit.bodhiattendance.domain.attendance

import com.prodigit.bodhiattendance.data.local.entity.AttendanceEventEntity
import com.prodigit.bodhiattendance.data.local.repository.OfflineAttendanceRepository

import kotlinx.coroutines.CancellationException


// ============================================================
// STARTUP CONSISTENCY RESULT
//
// Compares:
//
// restored attendance engine state
//
// against
//
// latest durable local attendance event.
//
// Goal:
//
// prevent duplicate automatic attendance events after a process
// crash that happened after attendance_events was saved but
// before attendance_engine_state was updated.
// ============================================================

sealed class AttendanceEngineStartupConsistencyResult {


    // ========================================================
    // SAFE
    //
    // Restored state is consistent with the latest durable local
    // attendance event.
    // ========================================================

    data class Consistent(

        val attendanceState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase

    ) : AttendanceEngineStartupConsistencyResult()


    // ========================================================
    // NO LOCAL ATTENDANCE EVENT
    //
    // This is not automatically an error.
    //
    // Example:
    //
    // first launch before CHECK_IN.
    // ========================================================

    data class NoLocalAttendanceEvent(

        val attendanceState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase

    ) : AttendanceEngineStartupConsistencyResult()


    // ========================================================
    // STALE ENGINE STATE
    //
    // A newer durable attendance event proves the restored engine
    // state is behind.
    //
    // Runtime must not continue automatically.
    // ========================================================

    data class ReconciliationRequired(

        val restoredAttendanceState:
        AttendanceState,

        val restoredPhase:
        AttendanceEnginePhase,

        val latestEventUuid:
        String,

        val latestEventType:
        String,

        val reason:
        String

    ) : AttendanceEngineStartupConsistencyResult()


    // ========================================================
    // INVALID LOCAL EVENT
    //
    // Example:
    //
    // unknown eventType string.
    // ========================================================

    data class InvalidLocalEvent(

        val eventUuid:
        String,

        val eventType:
        String,

        val reason:
        String

    ) : AttendanceEngineStartupConsistencyResult()


    // ========================================================
    // STORAGE FAILURE
    // ========================================================

    data class Failure(

        val error:
        Throwable

    ) : AttendanceEngineStartupConsistencyResult()
}


// ============================================================
// ATTENDANCE ENGINE STARTUP CONSISTENCY GUARD
//
// IMPORTANT:
//
// This does NOT modify state.
//
// It only verifies whether the restored engine state can safely
// continue.
//
// If inconsistency is detected:
//
// automatic event generation must pause and reconciliation must
// occur.
// ============================================================

class AttendanceEngineStartupConsistencyGuard(

    private val offlineAttendanceRepository:
    OfflineAttendanceRepository

) {


    // ========================================================
    // CHECK
    // ========================================================

    suspend fun check(

        userId: Int,

        deviceId: String,

        restoredAttendanceState:
        AttendanceState,

        restoredPhase:
        AttendanceEnginePhase

    ): AttendanceEngineStartupConsistencyResult {


        // ====================================================
        // BASIC IDENTITY VALIDATION
        // ====================================================

        if (userId <= 0) {

            return AttendanceEngineStartupConsistencyResult
                .Failure(

                    error =
                        IllegalStateException(
                            "Valid authenticated user ID is required"
                        )
                )
        }


        if (deviceId.isBlank()) {

            return AttendanceEngineStartupConsistencyResult
                .Failure(

                    error =
                        IllegalStateException(
                            "Registered device ID is required"
                        )
                )
        }


        // ====================================================
        // LOAD LATEST DURABLE LOCAL EVENT
        // ====================================================

        val latestEvent =
            try {

                offlineAttendanceRepository
                    .getLatestEventForUserDevice(

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

                return AttendanceEngineStartupConsistencyResult
                    .Failure(
                        error = e
                    )
            }


        // ====================================================
        // NO LOCAL EVENT
        // ====================================================

        if (latestEvent == null) {

            return AttendanceEngineStartupConsistencyResult
                .NoLocalAttendanceEvent(

                    attendanceState =
                        restoredAttendanceState,

                    phase =
                        restoredPhase
                )
        }


        // ====================================================
        // MAP LATEST DURABLE EVENT TO EXPECTED ATTENDANCE STATE
        // ====================================================

        val expectedAttendanceState =
            mapEventToAttendanceState(
                event = latestEvent
            )


        if (expectedAttendanceState == null) {

            return AttendanceEngineStartupConsistencyResult
                .InvalidLocalEvent(

                    eventUuid =
                        latestEvent.eventUuid,

                    eventType =
                        latestEvent.eventType,

                    reason =
                        "Latest local attendance event contains an unknown event type"
                )
        }


        // ====================================================
        // ATTENDANCE STATE MISMATCH
        //
        // This is the main crash-window protection.
        //
        // Example:
        //
        // restored state = WORKING
        //
        // latest event = BREAK_START
        //
        // Therefore engine state is stale.
        // ====================================================

        if (
            expectedAttendanceState !=
            restoredAttendanceState
        ) {

            return AttendanceEngineStartupConsistencyResult
                .ReconciliationRequired(

                    restoredAttendanceState =
                        restoredAttendanceState,

                    restoredPhase =
                        restoredPhase,

                    latestEventUuid =
                        latestEvent.eventUuid,

                    latestEventType =
                        latestEvent.eventType,

                    reason =
                        buildString {

                            append(
                                "Restored attendance state "
                            )

                            append(
                                restoredAttendanceState.name
                            )

                            append(
                                " does not match latest durable local event "
                            )

                            append(
                                latestEvent.eventType
                            )

                            append(
                                ", which implies "
                            )

                            append(
                                expectedAttendanceState.name
                            )
                        }
                )
        }


        // ====================================================
        // PHASE CONSISTENCY
        //
        // AttendanceState alone is not enough.
        //
        // Some attendance states require specific engine phase
        // families.
        // ====================================================

        val phaseError =
            validatePhaseForAttendanceState(

                attendanceState =
                    restoredAttendanceState,

                phase =
                    restoredPhase
            )


        if (phaseError != null) {

            return AttendanceEngineStartupConsistencyResult
                .ReconciliationRequired(

                    restoredAttendanceState =
                        restoredAttendanceState,

                    restoredPhase =
                        restoredPhase,

                    latestEventUuid =
                        latestEvent.eventUuid,

                    latestEventType =
                        latestEvent.eventType,

                    reason =
                        phaseError
                )
        }


        // ====================================================
        // SAFE
        // ====================================================

        return AttendanceEngineStartupConsistencyResult
            .Consistent(

                attendanceState =
                    restoredAttendanceState,

                phase =
                    restoredPhase
            )
    }


    // ========================================================
    // EVENT -> ATTENDANCE STATE
    //
    // Uses our final state semantics:
    //
    // CHECK_IN     -> WORKING
    //
    // BREAK_START  -> ON_BREAK
    //
    // BREAK_END    -> WORKING
    //
    // CHECK_OUT    -> CHECKED_OUT
    // ========================================================

    private fun mapEventToAttendanceState(

        event:
        AttendanceEventEntity

    ): AttendanceState? {


        return when (
            event.eventType
        ) {


            AttendanceEventType
                .CHECK_IN
                .name -> {

                AttendanceState.WORKING
            }


            AttendanceEventType
                .BREAK_START
                .name -> {

                AttendanceState.ON_BREAK
            }


            AttendanceEventType
                .BREAK_END
                .name -> {

                AttendanceState.WORKING
            }


            AttendanceEventType
                .CHECK_OUT
                .name -> {

                AttendanceState.CHECKED_OUT
            }


            else -> {

                null
            }
        }
    }


    // ========================================================
    // VALIDATE PHASE AGAINST ATTENDANCE STATE
    //
    // This is intentionally conservative.
    //
    // We only allow combinations that make sense for the
    // automatic engine.
    // ========================================================

    private fun validatePhaseForAttendanceState(

        attendanceState:
        AttendanceState,

        phase:
        AttendanceEnginePhase

    ): String? {


        return when (
            attendanceState
        ) {


            // =================================================
            // NOT CHECKED IN
            //
            // Engine should not be tracking office-exit timing.
            // =================================================

            AttendanceState.NOT_CHECKED_IN -> {


                when (phase) {

                    AttendanceEnginePhase
                        .NotMonitoring -> {

                        null
                    }


                    else -> {

                        "NOT_CHECKED_IN attendance state requires NotMonitoring phase"
                    }
                }
            }


            // =================================================
            // WORKING
            //
            // Valid:
            //
            // Present
            //
            // AbsenceGrace
            //
            // NotMonitoring is tolerated because startup/server
            // reconciliation may have placed the user into
            // WORKING before the first presence observation.
            // =================================================

            AttendanceState.WORKING -> {


                when (phase) {

                    AttendanceEnginePhase
                        .Present,

                    AttendanceEnginePhase
                        .NotMonitoring -> {

                        null
                    }


                    is AttendanceEnginePhase
                    .AbsenceGrace -> {

                        null
                    }


                    else -> {

                        "WORKING attendance state has incompatible engine phase"
                    }
                }
            }


            // =================================================
            // ON BREAK
            //
            // Must retain original absence timing context.
            // =================================================

            AttendanceState.ON_BREAK -> {


                when (phase) {

                    is AttendanceEnginePhase
                    .BreakActive -> {

                        null
                    }


                    else -> {

                        "ON_BREAK attendance state requires BreakActive phase"
                    }
                }
            }


            // =================================================
            // CHECKED OUT
            //
            // Normal completed state:
            //
            // NotMonitoring
            //
            // After automatic maximum-absence classification:
            //
            // CheckoutClassified
            // =================================================

            AttendanceState.CHECKED_OUT -> {


                when (phase) {

                    AttendanceEnginePhase
                        .NotMonitoring -> {

                        null
                    }


                    is AttendanceEnginePhase
                    .CheckoutClassified -> {

                        null
                    }


                    else -> {

                        "CHECKED_OUT attendance state has incompatible engine phase"
                    }
                }
            }


            // =================================================
            // ON DUTY / OUTSIDE WORK
            // =================================================

            AttendanceState.ON_DUTY -> {


                when (phase) {

                    AttendanceEnginePhase
                        .OnDutyBypass -> {

                        null
                    }


                    else -> {

                        "ON_DUTY attendance state requires OnDutyBypass phase"
                    }
                }
            }
        }
    }
}
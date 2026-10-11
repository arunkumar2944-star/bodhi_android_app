package com.prodigit.bodhiattendance.domain.attendance

import com.prodigit.bodhiattendance.data.local.repository.AttendanceEngineRestoreResult
import com.prodigit.bodhiattendance.data.local.repository.AttendanceEngineStateStore

import kotlinx.coroutines.CancellationException


// ============================================================
// ATTENDANCE ENGINE RUNTIME RESTORE RESULT
//
// Higher-level startup result used by the runtime/service layer.
//
// IMPORTANT:
//
// This class does NOT contact Express.
//
// It only decides whether locally persisted automatic-attendance
// state is safe to reuse.
//
// Server reconciliation will be added separately for:
//
// - Android reboot
// - invalid/corrupted local state
// - missing local state when authoritative state is required
// ============================================================

sealed class AttendanceEngineRuntimeRestoreResult {


    // ========================================================
    // SAFE LOCAL RESTORE
    //
    // Same Android boot session.
    //
    // Saved elapsedRealtime values are still meaningful.
    // ========================================================

    data class Ready(

        val attendanceState:
        AttendanceState,

        val phase:
        AttendanceEnginePhase

    ) : AttendanceEngineRuntimeRestoreResult()


    // ========================================================
    // NO LOCAL ENGINE STATE
    //
    // This does NOT automatically mean NOT_CHECKED_IN.
    //
    // Examples:
    //
    // - first installation
    // - local reset
    // - logout/login
    // - Room state never created
    //
    // Runtime must later obtain authoritative attendance state
    // before starting automatic event generation.
    // ========================================================

    object StateUnavailable :
        AttendanceEngineRuntimeRestoreResult()


    // ========================================================
    // PHONE REBOOT DETECTED
    //
    // Old monotonic timestamps cannot be reused.
    //
    // Runtime must:
    //
    // 1. pause attendance event generation
    // 2. obtain fresh trusted time anchor
    // 3. obtain authoritative attendance state from Express
    // 4. reconstruct a safe current engine phase
    // 5. persist new current-boot state
    //
    // Never continue AbsenceGrace / BreakActive timestamps from
    // the previous Android boot.
    // ========================================================

    data class RebootReconciliationRequired(

        val storedAttendanceState:
        AttendanceState,

        val storedBootId:
        String,

        val currentBootId:
        String

    ) : AttendanceEngineRuntimeRestoreResult()


    // ========================================================
    // LOCAL STATE INVALID
    //
    // Examples:
    //
    // - unknown persisted state
    // - missing absence timestamp
    // - missing break confirmation timestamp
    // - future elapsedRealtime value
    // - invalid timestamp ordering
    //
    // Automatic attendance must not generate an event from this
    // state until reconciliation is completed.
    // ========================================================

    data class InvalidStateReconciliationRequired(

        val reason:
        String

    ) : AttendanceEngineRuntimeRestoreResult()


    // ========================================================
    // STORAGE / RUNTIME FAILURE
    // ========================================================

    data class Failure(

        val error:
        Throwable

    ) : AttendanceEngineRuntimeRestoreResult()
}


// ============================================================
// ATTENDANCE ENGINE RUNTIME RESTORER
//
// Startup / process-death recovery boundary.
//
// Flow:
//
// App / foreground service starts
//        ↓
// user + registered device known
//        ↓
// AttendanceEngineRuntimeRestorer
//        ↓
// AttendanceEngineStateStore
//        ↓
// same boot?
//
// YES:
// restore state + phase
//
// NO:
// reconciliation required
//
// IMPORTANT:
//
// This layer NEVER invents:
//
// NOT_CHECKED_IN
// WORKING
// ON_BREAK
// CHECKED_OUT
//
// when local state cannot safely prove it.
//
// Express remains authoritative.
// ============================================================

class AttendanceEngineRuntimeRestorer(

    private val attendanceEngineStateStore:
    AttendanceEngineStateStore

) {


    // ========================================================
    // RESTORE
    // ========================================================

    suspend fun restore(

        userId: Int,

        deviceId: String

    ): AttendanceEngineRuntimeRestoreResult {


        // ====================================================
        // BASIC IDENTITY VALIDATION
        // ====================================================

        if (
            userId <= 0
        ) {

            return AttendanceEngineRuntimeRestoreResult
                .Failure(

                    error =
                        IllegalStateException(
                            "Valid authenticated user ID is required"
                        )
                )
        }


        if (
            deviceId.isBlank()
        ) {

            return AttendanceEngineRuntimeRestoreResult
                .Failure(

                    error =
                        IllegalStateException(
                            "Registered device ID is required"
                        )
                )
        }


        // ====================================================
        // RESTORE FROM STATE STORE
        // ====================================================

        val restoreResult =
            try {

                attendanceEngineStateStore
                    .restoreCurrentState(

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

                return AttendanceEngineRuntimeRestoreResult
                    .Failure(
                        error = e
                    )
            }


        // ====================================================
        // MAP LOW-LEVEL STORE RESULT TO RUNTIME RESULT
        // ====================================================

        return when (
            restoreResult
        ) {


            // =================================================
            // NO LOCAL STATE
            //
            // Do NOT assume NOT_CHECKED_IN.
            // =================================================

            AttendanceEngineRestoreResult
                .NotFound -> {


                AttendanceEngineRuntimeRestoreResult
                    .StateUnavailable
            }


            // =================================================
            // SAFE SAME-BOOT RESTORE
            // =================================================

            is AttendanceEngineRestoreResult
            .Restored -> {


                AttendanceEngineRuntimeRestoreResult
                    .Ready(

                        attendanceState =
                            restoreResult
                                .state
                                .attendanceState,

                        phase =
                            restoreResult
                                .state
                                .phase
                    )
            }


            // =================================================
            // DEVICE REBOOT
            //
            // Old elapsedRealtime values are not reusable.
            // =================================================

            is AttendanceEngineRestoreResult
            .BootSessionChanged -> {


                AttendanceEngineRuntimeRestoreResult
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


            // =================================================
            // CORRUPTED / UNSAFE LOCAL STATE
            // =================================================

            is AttendanceEngineRestoreResult
            .InvalidStoredState -> {


                AttendanceEngineRuntimeRestoreResult
                    .InvalidStateReconciliationRequired(

                        reason =
                            restoreResult
                                .reason
                    )
            }
        }
    }
}
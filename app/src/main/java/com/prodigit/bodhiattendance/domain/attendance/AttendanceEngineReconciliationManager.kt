package com.prodigit.bodhiattendance.domain.attendance

import com.prodigit.bodhiattendance.data.local.repository.AttendanceEngineStateStore
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorManager

import kotlinx.coroutines.CancellationException


// ============================================================
// ATTENDANCE ENGINE RECONCILIATION MANAGER
//
// Repairs/rebuilds automatic attendance runtime state whenever
// local startup cannot safely continue.
//
// Used for:
//
// - StateUnavailable
// - Android reboot
// - invalid persisted engine state
// - stale local engine state
// - backend-rejected event recovery
// - persistence recovery
//
// Flow:
//
// runtime paused
//      ↓
// Express authoritative attendance state
//      ↓
// fresh trusted-time anchor
//      ↓
// construct SAFE current-boot engine phase
//      ↓
// persist AttendanceState + Phase
//      ↓
// return Ready
//
// ============================================================
//
// IMPORTANT:
//
// We NEVER reconstruct old AbsenceGrace or BreakActive timers
// after reboot.
//
// Why?
//
// elapsedRealtime() belongs to one Android boot session only.
//
// After reboot:
//
// WORKING becomes Present
//
// ON_BREAK cannot safely reconstruct the original absence timing
// from current-boot monotonic time.
//
// Therefore ON_BREAK requires conservative handling described
// below.
// ============================================================

class AttendanceEngineReconciliationManager(

    private val bootstrapRepository:
    AttendanceBootstrapRepository,

    private val timeAnchorManager:
    TimeAnchorManager,

    private val attendanceEngineStateStore:
    AttendanceEngineStateStore

) {


    // ========================================================
    // RECONCILE
    // ========================================================

    suspend fun reconcile(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String

    ): AttendanceEngineBootstrapResult {


        // ====================================================
        // BASIC VALIDATION
        // ====================================================

        if (token.isBlank()) {

            return AttendanceEngineBootstrapResult
                .Blocked(
                    reason =
                        "Authenticated session token is unavailable"
                )
        }


        if (userId <= 0) {

            return AttendanceEngineBootstrapResult
                .Blocked(
                    reason =
                        "Valid authenticated user ID is required"
                )
        }


        if (companyId <= 0) {

            return AttendanceEngineBootstrapResult
                .Blocked(
                    reason =
                        "Valid company ID is required"
                )
        }


        if (deviceId.isBlank()) {

            return AttendanceEngineBootstrapResult
                .Blocked(
                    reason =
                        "Registered device ID is required"
                )
        }


        // ====================================================
        // GET AUTHORITATIVE EXPRESS STATE
        // ====================================================

        val serverStateResult =
            try {

                bootstrapRepository
                    .getCurrentAttendanceState(

                        token =
                            token,

                        userId =
                            userId,

                        companyId =
                            companyId,

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

                return AttendanceEngineBootstrapResult
                    .RetryLater(

                        reason =
                            e.message
                                ?: "Unable to obtain authoritative attendance state",

                        error =
                            e
                    )
            }


        if (serverStateResult.isFailure) {

            val error =
                serverStateResult
                    .exceptionOrNull()


            return AttendanceEngineBootstrapResult
                .RetryLater(

                    reason =
                        error?.message
                            ?: "Unable to obtain authoritative attendance state",

                    error =
                        error
                )
        }


        val serverState =
            try {

                serverStateResult
                    .getOrThrow()

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                return AttendanceEngineBootstrapResult
                    .Failure(
                        error = e
                    )
            }


        // ====================================================
        // VALIDATE SERVER STATE CONSISTENCY
        // ====================================================

        val serverValidationError =
            validateServerBootstrapState(
                state = serverState
            )


        if (serverValidationError != null) {

            return AttendanceEngineBootstrapResult
                .Blocked(
                    reason =
                        serverValidationError
                )
        }


        // ====================================================
        // CREATE FRESH TRUSTED TIME ANCHOR
        //
        // Reconciliation establishes a clean current-boot trust
        // context before automatic attendance resumes.
        //
        // TimeAnchorManager already persists the anchor in Room
        // before exposing it to the application.
        // ====================================================

        val anchorResult =
            try {

                timeAnchorManager
                    .createFreshAnchor(

                        token =
                            token,

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

                return AttendanceEngineBootstrapResult
                    .RetryLater(

                        reason =
                            e.message
                                ?: "Unable to create fresh trusted time anchor",

                        error =
                            e
                    )
            }


        if (anchorResult.isFailure) {

            val error =
                anchorResult
                    .exceptionOrNull()


            return AttendanceEngineBootstrapResult
                .RetryLater(

                    reason =
                        error?.message
                            ?: "Unable to create fresh trusted time anchor",

                    error =
                        error
                )
        }


        // ====================================================
        // BUILD SAFE CURRENT-BOOT ENGINE PHASE
        // ====================================================

        val phaseResult =
            buildSafePhase(
                serverState = serverState
            )


        if (phaseResult is SafePhaseResult.Blocked) {

            return AttendanceEngineBootstrapResult
                .Blocked(
                    reason =
                        phaseResult.reason
                )
        }


        val phase =
            (phaseResult as SafePhaseResult.Ready)
                .phase


        // ====================================================
        // PERSIST RECONCILED STATE
        //
        // This writes the CURRENT bootId through the existing
        // AttendanceEngineStateStore.
        // ====================================================

        try {

            attendanceEngineStateStore
                .saveCurrentState(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    attendanceState =
                        serverState
                            .attendanceState,

                    phase =
                        phase
                )

        } catch (
            e: CancellationException
        ) {

            throw e

        } catch (
            e: Exception
        ) {

            return AttendanceEngineBootstrapResult
                .RetryLater(

                    reason =
                        e.message
                            ?: "Unable to persist reconciled attendance engine state",

                    error =
                        e
                )
        }


        // ====================================================
        // READY
        // ====================================================

        return AttendanceEngineBootstrapResult
            .Ready(

                attendanceState =
                    serverState
                        .attendanceState,

                phase =
                    phase
            )
    }


    // ========================================================
    // VALIDATE SERVER BOOTSTRAP STATE
    // ========================================================

    private fun validateServerBootstrapState(

        state:
        AttendanceBootstrapState

    ): String? {


        // =====================================================
        // ON DUTY
        // =====================================================

        if (
            state.onDutyActive &&
            state.attendanceState !=
            AttendanceState.ON_DUTY
        ) {

            return "Server bootstrap reports active On Duty with incompatible attendance state"
        }


        // =====================================================
        // NORMAL EVENT -> STATE CONSISTENCY
        // =====================================================

        return when (
            state.latestEventType
        ) {


            null -> {


                when (
                    state.attendanceState
                ) {

                    AttendanceState.NOT_CHECKED_IN,
                    AttendanceState.ON_DUTY -> {

                        null
                    }


                    else -> {

                        "Server bootstrap has no latest attendance event for state ${state.attendanceState.name}"
                    }
                }
            }


            AttendanceEventType.CHECK_IN -> {


                if (
                    state.attendanceState ==
                    AttendanceState.WORKING
                ) {

                    null

                } else {

                    "CHECK_IN bootstrap event requires WORKING attendance state"
                }
            }


            AttendanceEventType.BREAK_START -> {


                if (
                    state.attendanceState ==
                    AttendanceState.ON_BREAK
                ) {

                    null

                } else {

                    "BREAK_START bootstrap event requires ON_BREAK attendance state"
                }
            }


            AttendanceEventType.BREAK_END -> {


                if (
                    state.attendanceState ==
                    AttendanceState.WORKING
                ) {

                    null

                } else {

                    "BREAK_END bootstrap event requires WORKING attendance state"
                }
            }


            AttendanceEventType.CHECK_OUT -> {


                if (
                    state.attendanceState ==
                    AttendanceState.CHECKED_OUT
                ) {

                    null

                } else {

                    "CHECK_OUT bootstrap event requires CHECKED_OUT attendance state"
                }
            }
        }
    }


    // ========================================================
    // SAFE PHASE RESULT
    // ========================================================

    private sealed class SafePhaseResult {


        data class Ready(

            val phase:
            AttendanceEnginePhase

        ) : SafePhaseResult()


        data class Blocked(

            val reason:
            String

        ) : SafePhaseResult()
    }


    // ========================================================
    // BUILD SAFE PHASE AFTER SERVER RECONCILIATION
    //
    // IMPORTANT:
    //
    // We NEVER manufacture historical monotonic timestamps.
    // ========================================================

    private fun buildSafePhase(

        serverState:
        AttendanceBootstrapState

    ): SafePhaseResult {


        return when (
            serverState.attendanceState
        ) {


            // =================================================
            // NOT CHECKED IN
            // =================================================

            AttendanceState.NOT_CHECKED_IN -> {


                SafePhaseResult.Ready(
                    phase =
                        AttendanceEnginePhase
                            .NotMonitoring
                )
            }


            // =================================================
            // WORKING
            //
            // Fresh reconciliation means we do not know of an
            // active current-boot absence timer.
            //
            // Start safely from Present.
            //
            // The next real sensor observation may immediately
            // move to AbsenceGrace if office presence is absent.
            // =================================================

            AttendanceState.WORKING -> {


                SafePhaseResult.Ready(
                    phase =
                        AttendanceEnginePhase
                            .Present
                )
            }


            // =================================================
            // ON BREAK
            //
            // CRITICAL:
            //
            // BreakActive requires:
            //
            // absenceStartedElapsedRealtimeMs
            // breakConfirmedElapsedRealtimeMs
            //
            // We MUST NOT invent those values after reboot or
            // state loss.
            //
            // Therefore a plain authoritative ON_BREAK state is
            // not enough to resume the 90-minute timer safely.
            //
            // Later, if Express exposes the original effective
            // break-start trusted time + compatible current-boot
            // reconstruction data, this can be expanded.
            //
            // For now pause and require explicit reconciliation
            // rather than generating an incorrect checkout.
            // =================================================

            AttendanceState.ON_BREAK -> {


                SafePhaseResult.Blocked(

                    reason =
                        "Authoritative state is ON_BREAK but original monotonic break timing cannot be safely reconstructed"
                )
            }


            // =================================================
            // CHECKED OUT
            // =================================================

            AttendanceState.CHECKED_OUT -> {


                SafePhaseResult.Ready(
                    phase =
                        AttendanceEnginePhase
                            .NotMonitoring
                )
            }


            // =================================================
            // OUTSIDE WORK
            // =================================================

            AttendanceState.ON_DUTY -> {


                SafePhaseResult.Ready(
                    phase =
                        AttendanceEnginePhase
                            .OnDutyBypass
                )
            }
        }
    }
}
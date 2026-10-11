package com.prodigit.bodhiattendance.data.local.repository

import com.prodigit.bodhiattendance.data.local.entity.AttendanceEngineStateEntity
import com.prodigit.bodhiattendance.data.local.security.BootSessionProvider

import com.prodigit.bodhiattendance.domain.attendance.AttendanceEnginePhase
import com.prodigit.bodhiattendance.domain.attendance.AttendanceState


// ============================================================
// RESTORED ATTENDANCE ENGINE STATE
//
// Runtime representation reconstructed from Room.
//
// This is NOT an attendance event.
// ============================================================

data class RestoredAttendanceEngineState(

    val attendanceState: AttendanceState,

    val phase: AttendanceEnginePhase
)


// ============================================================
// ENGINE STATE RESTORE RESULT
//
// We deliberately distinguish:
//
// - nothing persisted
// - successfully restored
// - device reboot detected
// - invalid/corrupted persisted state
//
// A reboot is NOT treated as a normal restore because Android
// elapsedRealtime() resets after reboot.
// ============================================================

sealed class AttendanceEngineRestoreResult {


    // --------------------------------------------------------
    // NO STORED STATE
    // --------------------------------------------------------

    object NotFound : AttendanceEngineRestoreResult()


    // --------------------------------------------------------
    // SAFE RESTORE
    //
    // Same boot session.
    // Persisted monotonic values are valid to reuse.
    // --------------------------------------------------------

    data class Restored(

        val state: RestoredAttendanceEngineState

    ) : AttendanceEngineRestoreResult()


    // --------------------------------------------------------
    // PHONE REBOOT DETECTED
    //
    // IMPORTANT:
    //
    // Old elapsedRealtime values must NOT be compared with the
    // current boot's elapsedRealtime values.
    //
    // Later the attendance engine will reconcile attendance
    // state with Express and obtain a fresh trusted time anchor.
    // --------------------------------------------------------

    data class BootSessionChanged(

        val storedAttendanceState: AttendanceState,

        val storedBootId: String,

        val currentBootId: String

    ) : AttendanceEngineRestoreResult()


    // --------------------------------------------------------
    // INVALID LOCAL STATE
    //
    // Examples:
    //
    // - unknown enum/string
    // - ABSENCE_GRACE without absence start time
    // - BREAK_ACTIVE without required timestamps
    // - stored monotonic timestamp is in the future
    //
    // Must NOT create an attendance event automatically.
    // --------------------------------------------------------

    data class InvalidStoredState(

        val reason: String

    ) : AttendanceEngineRestoreResult()
}


// ============================================================
// ATTENDANCE ENGINE STATE STORE
//
// Responsible for:
//
// AttendanceState
// +
// AttendanceEnginePhase
//
//              ↕
//
// AttendanceEngineStateEntity
//
// It provides safe persistence/recovery of the CURRENT automatic
// engine state.
//
// IMPORTANT:
//
// This store NEVER creates:
//
// CHECK_IN
// BREAK_START
// BREAK_END
// CHECK_OUT
//
// It only saves/restores engine runtime state.
// ============================================================

class AttendanceEngineStateStore(

    private val repository:
    AttendanceEngineStatePersistence,

    private val bootSessionManager:
    BootSessionProvider

) {


    // ========================================================
    // SAVE CURRENT STATE
    //
    // REPLACE semantics in Room guarantee one current row for:
    //
    // userId + deviceId
    // ========================================================

    suspend fun saveCurrentState(

        userId: Int,

        companyId: Int,

        deviceId: String,

        attendanceState: AttendanceState,

        phase: AttendanceEnginePhase

    ) {


        require(userId > 0) {
            "Valid user ID is required"
        }


        require(companyId > 0) {
            "Valid company ID is required"
        }


        require(deviceId.isNotBlank()) {
            "Valid device ID is required"
        }


        val currentBootId =
            bootSessionManager
                .getBootId()


        val entity =
            createEntity(

                userId =
                    userId,

                companyId =
                    companyId,

                deviceId =
                    deviceId,

                attendanceState =
                    attendanceState,

                phase =
                    phase,

                bootId =
                    currentBootId
            )


        repository
            .saveState(
                state = entity
            )
    }


    // ========================================================
    // RESTORE CURRENT STATE
    //
    // Safe flow:
    //
    // Room row
    //    ↓
    // Parse AttendanceState
    //    ↓
    // Compare bootId
    //    ↓
    // Same boot?
    //
    // YES -> restore monotonic engine phase
    //
    // NO  -> BootSessionChanged
    //
    // Never silently reuse old monotonic timestamps.
    // ========================================================

    suspend fun restoreCurrentState(

        userId: Int,

        deviceId: String

    ): AttendanceEngineRestoreResult {


        val entity =
            repository
                .getState(
                    userId = userId,
                    deviceId = deviceId
                )
                ?: return AttendanceEngineRestoreResult.NotFound


        // ====================================================
        // PARSE ATTENDANCE STATE
        // ====================================================

        val attendanceState =
            try {

                AttendanceState.valueOf(
                    entity.attendanceState
                )

            } catch (e: Exception) {

                return AttendanceEngineRestoreResult
                    .InvalidStoredState(

                        reason =
                            "Unknown stored attendance state: ${entity.attendanceState}"
                    )
            }


        // ====================================================
        // VERIFY BOOT SESSION
        // ====================================================

        val currentBootId =
            bootSessionManager
                .getBootId()


        if (
            entity.bootId !=
            currentBootId
        ) {

            return AttendanceEngineRestoreResult
                .BootSessionChanged(

                    storedAttendanceState =
                        attendanceState,

                    storedBootId =
                        entity.bootId,

                    currentBootId =
                        currentBootId
                )
        }


        // ====================================================
        // RECONSTRUCT ENGINE PHASE
        // ====================================================

        val phaseResult =
            restorePhase(
                entity = entity
            )


        if (
            phaseResult is PhaseRestoreResult.Invalid
        ) {

            return AttendanceEngineRestoreResult
                .InvalidStoredState(

                    reason =
                        phaseResult.reason
                )
        }


        val phase =
            (phaseResult as PhaseRestoreResult.Success)
                .phase


        // ====================================================
        // MONOTONIC SANITY CHECK
        //
        // Same boot session does not automatically mean stored
        // data is valid.
        //
        // Any saved engine timestamp must be <= current
        // elapsedRealtime.
        // ====================================================

        val currentElapsedRealtimeMs =
            bootSessionManager
                .getElapsedRealtimeMs()


        val invalidTimestampReason =
            validateStoredMonotonicTimes(

                entity =
                    entity,

                currentElapsedRealtimeMs =
                    currentElapsedRealtimeMs
            )


        if (invalidTimestampReason != null) {

            return AttendanceEngineRestoreResult
                .InvalidStoredState(

                    reason =
                        invalidTimestampReason
                )
        }


        // ====================================================
        // SAFE RESTORE
        // ====================================================

        return AttendanceEngineRestoreResult
            .Restored(

                state =
                    RestoredAttendanceEngineState(

                        attendanceState =
                            attendanceState,

                        phase =
                            phase
                    )
            )
    }


    // ========================================================
    // CLEAR CURRENT STATE
    //
    // Appropriate for:
    //
    // - logout
    // - deregistration
    // - controlled reset
    //
    // Not used for ordinary attendance transitions.
    // ========================================================

    suspend fun clearCurrentState(

        userId: Int,

        deviceId: String

    ) {

        repository
            .deleteState(

                userId =
                    userId,

                deviceId =
                    deviceId
            )
    }


    // ========================================================
    // CREATE ROOM ENTITY
    // ========================================================

    private fun createEntity(

        userId: Int,

        companyId: Int,

        deviceId: String,

        attendanceState: AttendanceState,

        phase: AttendanceEnginePhase,

        bootId: String

    ): AttendanceEngineStateEntity {


        return when (phase) {


            // ------------------------------------------------
            // NOT MONITORING
            // ------------------------------------------------

            AttendanceEnginePhase.NotMonitoring -> {

                AttendanceEngineStateEntity(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    attendanceState =
                        attendanceState.name,

                    phaseType =
                        PHASE_NOT_MONITORING,

                    absenceStartedElapsedRealtimeMs =
                        null,

                    breakConfirmedElapsedRealtimeMs =
                        null,

                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,

                    bootId =
                        bootId,

                    updatedAtEpochMs =
                        System.currentTimeMillis()
                )
            }


            // ------------------------------------------------
            // PRESENT
            // ------------------------------------------------

            AttendanceEnginePhase.Present -> {

                AttendanceEngineStateEntity(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    attendanceState =
                        attendanceState.name,

                    phaseType =
                        PHASE_PRESENT,

                    absenceStartedElapsedRealtimeMs =
                        null,

                    breakConfirmedElapsedRealtimeMs =
                        null,

                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,

                    bootId =
                        bootId,

                    updatedAtEpochMs =
                        System.currentTimeMillis()
                )
            }


            // ------------------------------------------------
            // ABSENCE GRACE
            // ------------------------------------------------

            is AttendanceEnginePhase.AbsenceGrace -> {

                AttendanceEngineStateEntity(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    attendanceState =
                        attendanceState.name,

                    phaseType =
                        PHASE_ABSENCE_GRACE,

                    absenceStartedElapsedRealtimeMs =
                        phase
                            .absenceStartedElapsedRealtimeMs,

                    breakConfirmedElapsedRealtimeMs =
                        null,

                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,

                    bootId =
                        bootId,

                    updatedAtEpochMs =
                        System.currentTimeMillis()
                )
            }


            // ------------------------------------------------
            // BREAK ACTIVE
            // ------------------------------------------------

            is AttendanceEnginePhase.BreakActive -> {

                AttendanceEngineStateEntity(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    attendanceState =
                        attendanceState.name,

                    phaseType =
                        PHASE_BREAK_ACTIVE,

                    absenceStartedElapsedRealtimeMs =
                        phase
                            .absenceStartedElapsedRealtimeMs,

                    breakConfirmedElapsedRealtimeMs =
                        phase
                            .breakConfirmedElapsedRealtimeMs,

                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,

                    bootId =
                        bootId,

                    updatedAtEpochMs =
                        System.currentTimeMillis()
                )
            }


            // ------------------------------------------------
            // CHECKOUT CLASSIFIED
            // ------------------------------------------------

            is AttendanceEnginePhase.CheckoutClassified -> {

                AttendanceEngineStateEntity(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    attendanceState =
                        attendanceState.name,

                    phaseType =
                        PHASE_CHECKOUT_CLASSIFIED,

                    absenceStartedElapsedRealtimeMs =
                        phase
                            .absenceStartedElapsedRealtimeMs,

                    breakConfirmedElapsedRealtimeMs =
                        null,

                    checkoutClassifiedAtElapsedRealtimeMs =
                        phase
                            .classifiedAtElapsedRealtimeMs,

                    bootId =
                        bootId,

                    updatedAtEpochMs =
                        System.currentTimeMillis()
                )
            }


            // ------------------------------------------------
            // OUTSIDE WORK BYPASS
            // ------------------------------------------------

            AttendanceEnginePhase.OnDutyBypass -> {

                AttendanceEngineStateEntity(

                    userId =
                        userId,

                    companyId =
                        companyId,

                    deviceId =
                        deviceId,

                    attendanceState =
                        attendanceState.name,

                    phaseType =
                        PHASE_ON_DUTY_BYPASS,

                    absenceStartedElapsedRealtimeMs =
                        null,

                    breakConfirmedElapsedRealtimeMs =
                        null,

                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,

                    bootId =
                        bootId,

                    updatedAtEpochMs =
                        System.currentTimeMillis()
                )
            }
        }
    }


    // ========================================================
    // INTERNAL PHASE RESTORE RESULT
    // ========================================================

    private sealed class PhaseRestoreResult {


        data class Success(

            val phase: AttendanceEnginePhase

        ) : PhaseRestoreResult()


        data class Invalid(

            val reason: String

        ) : PhaseRestoreResult()
    }


    // ========================================================
    // RESTORE PHASE FROM ENTITY
    // ========================================================

    private fun restorePhase(

        entity: AttendanceEngineStateEntity

    ): PhaseRestoreResult {


        return when (entity.phaseType) {


            PHASE_NOT_MONITORING -> {

                PhaseRestoreResult.Success(
                    AttendanceEnginePhase.NotMonitoring
                )
            }


            PHASE_PRESENT -> {

                PhaseRestoreResult.Success(
                    AttendanceEnginePhase.Present
                )
            }


            PHASE_ABSENCE_GRACE -> {

                val absenceStarted =
                    entity
                        .absenceStartedElapsedRealtimeMs
                        ?: return PhaseRestoreResult.Invalid(
                            "ABSENCE_GRACE has no original absence start time"
                        )


                PhaseRestoreResult.Success(

                    AttendanceEnginePhase
                        .AbsenceGrace(

                            absenceStartedElapsedRealtimeMs =
                                absenceStarted
                        )
                )
            }


            PHASE_BREAK_ACTIVE -> {

                val absenceStarted =
                    entity
                        .absenceStartedElapsedRealtimeMs
                        ?: return PhaseRestoreResult.Invalid(
                            "BREAK_ACTIVE has no original absence start time"
                        )


                val breakConfirmed =
                    entity
                        .breakConfirmedElapsedRealtimeMs
                        ?: return PhaseRestoreResult.Invalid(
                            "BREAK_ACTIVE has no break confirmation time"
                        )


                PhaseRestoreResult.Success(

                    AttendanceEnginePhase
                        .BreakActive(

                            absenceStartedElapsedRealtimeMs =
                                absenceStarted,

                            breakConfirmedElapsedRealtimeMs =
                                breakConfirmed
                        )
                )
            }


            PHASE_CHECKOUT_CLASSIFIED -> {

                val absenceStarted =
                    entity
                        .absenceStartedElapsedRealtimeMs
                        ?: return PhaseRestoreResult.Invalid(
                            "CHECKOUT_CLASSIFIED has no original absence time"
                        )


                val classifiedAt =
                    entity
                        .checkoutClassifiedAtElapsedRealtimeMs
                        ?: return PhaseRestoreResult.Invalid(
                            "CHECKOUT_CLASSIFIED has no classification time"
                        )


                PhaseRestoreResult.Success(

                    AttendanceEnginePhase
                        .CheckoutClassified(

                            absenceStartedElapsedRealtimeMs =
                                absenceStarted,

                            classifiedAtElapsedRealtimeMs =
                                classifiedAt
                        )
                )
            }


            PHASE_ON_DUTY_BYPASS -> {

                PhaseRestoreResult.Success(
                    AttendanceEnginePhase.OnDutyBypass
                )
            }


            else -> {

                PhaseRestoreResult.Invalid(
                    "Unknown attendance engine phase: ${entity.phaseType}"
                )
            }
        }
    }


    // ========================================================
    // VALIDATE STORED MONOTONIC TIMES
    //
    // All timestamps were captured using elapsedRealtime()
    // during the SAME boot session.
    //
    // Therefore none of them may be greater than the current
    // elapsedRealtime value.
    // ========================================================

    private fun validateStoredMonotonicTimes(

        entity: AttendanceEngineStateEntity,

        currentElapsedRealtimeMs: Long

    ): String? {


        val absenceStarted =
            entity
                .absenceStartedElapsedRealtimeMs


        if (
            absenceStarted != null &&
            absenceStarted > currentElapsedRealtimeMs
        ) {

            return "Stored absence start time is newer than current monotonic time"
        }


        val breakConfirmed =
            entity
                .breakConfirmedElapsedRealtimeMs


        if (
            breakConfirmed != null &&
            breakConfirmed > currentElapsedRealtimeMs
        ) {

            return "Stored break confirmation time is newer than current monotonic time"
        }


        val checkoutClassified =
            entity
                .checkoutClassifiedAtElapsedRealtimeMs


        if (
            checkoutClassified != null &&
            checkoutClassified > currentElapsedRealtimeMs
        ) {

            return "Stored checkout classification time is newer than current monotonic time"
        }


        // ----------------------------------------------------
        // ORDER VALIDATION
        //
        // absence start must happen before/equal break confirm.
        // ----------------------------------------------------

        if (
            absenceStarted != null &&
            breakConfirmed != null &&
            breakConfirmed < absenceStarted
        ) {

            return "Break confirmation time is before original absence time"
        }


        // ----------------------------------------------------
        // Checkout classification must not be before original
        // absence.
        // ----------------------------------------------------

        if (
            absenceStarted != null &&
            checkoutClassified != null &&
            checkoutClassified < absenceStarted
        ) {

            return "Checkout classification time is before original absence time"
        }


        return null
    }


    companion object {


        // ====================================================
        // STORED PHASE VALUES
        //
        // Keep these stable because they are persisted in Room.
        // ====================================================

        private const val PHASE_NOT_MONITORING =
            "NOT_MONITORING"


        private const val PHASE_PRESENT =
            "PRESENT"


        private const val PHASE_ABSENCE_GRACE =
            "ABSENCE_GRACE"


        private const val PHASE_BREAK_ACTIVE =
            "BREAK_ACTIVE"


        private const val PHASE_CHECKOUT_CLASSIFIED =
            "CHECKOUT_CLASSIFIED"


        private const val PHASE_ON_DUTY_BYPASS =
            "ON_DUTY_BYPASS"
    }
}
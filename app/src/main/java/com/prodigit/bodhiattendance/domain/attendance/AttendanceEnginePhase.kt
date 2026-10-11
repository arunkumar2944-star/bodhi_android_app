package com.prodigit.bodhiattendance.domain.attendance


// ============================================================
// ATTENDANCE ENGINE PHASE
//
// Internal runtime state used by the automatic attendance engine.
//
// IMPORTANT:
//
// These are NOT attendance events.
//
// They must NOT create Room rows such as:
//
// ABSENCE_GRACE
// EXIT_PENDING
// CHECKOUT_PENDING
//
// Room should contain only real attendance events:
//
// CHECK_IN
// BREAK_START
// BREAK_END
// CHECK_OUT
//
// Timing in this class uses Android monotonic elapsed realtime,
// never the phone wall clock.
// ============================================================

sealed class AttendanceEnginePhase {


    // ========================================================
    // NOT MONITORING
    //
    // Examples:
    //
    // - employee has not checked in
    // - employee has already checked out
    // - trusted-time/configuration is not ready
    // ========================================================

    object NotMonitoring : AttendanceEnginePhase()


    // ========================================================
    // PRESENT
    //
    // Employee currently has valid office presence.
    //
    // V1 presence rule:
    //
    // Geofence verified
    // AND
    // approved office Wi-Fi/BSSID verified
    //
    // BLE is not part of the V1 attendance decision.
    // ========================================================

    object Present : AttendanceEnginePhase()


    // ========================================================
    // ABSENCE GRACE
    //
    // Presence has just been lost.
    //
    // NO BREAK_START event is created immediately.
    //
    // We preserve the exact monotonic time when presence was
    // first lost.
    //
    // Example:
    //
    // 10:30:00 presence lost
    //
    // phase =
    // AbsenceGrace(
    //     absenceStartedElapsedRealtimeMs = ...
    // )
    //
    // If presence returns before the configured grace period:
    //
    // AbsenceGrace -> Present
    //
    // No attendance event is created.
    // ========================================================

    data class AbsenceGrace(

        val absenceStartedElapsedRealtimeMs: Long

    ) : AttendanceEnginePhase()


    // ========================================================
    // BREAK ACTIVE
    //
    // Presence remained absent beyond the configured grace
    // period and BREAK_START has been confirmed/generated.
    //
    // We preserve TWO monotonic timestamps:
    //
    // absenceStartedElapsedRealtimeMs
    //
    //     Original moment presence was lost.
    //
    // breakConfirmedElapsedRealtimeMs
    //
    //     Moment the grace period completed and the engine
    //     confirmed that this is a real absence/break.
    //
    // Keeping the ORIGINAL absence time is critical for our
    // approved 90-minute checkout classification rule.
    // ========================================================

    data class BreakActive(

        val absenceStartedElapsedRealtimeMs: Long,

        val breakConfirmedElapsedRealtimeMs: Long

    ) : AttendanceEnginePhase()


    // ========================================================
    // CHECKOUT CLASSIFIED
    //
    // Employee did not return within the maximum configured
    // absence duration.
    //
    // Example:
    //
    // Presence lost     = 18:25
    // Grace completes   = 18:28
    // Max absence       = 90 minutes
    // Decision reached  = 19:55
    //
    // Effective checkout time must still be based on the
    // ORIGINAL trusted absence time:
    //
    // 18:25
    //
    // NOT:
    //
    // 19:55
    //
    // This phase preserves both values so the future event/API
    // layer can distinguish:
    //
    // effective attendance time
    //
    // from
    //
    // decision/classification time.
    // ========================================================

    data class CheckoutClassified(

        val absenceStartedElapsedRealtimeMs: Long,

        val classifiedAtElapsedRealtimeMs: Long

    ) : AttendanceEnginePhase()


    // ========================================================
    // OUTSIDE WORK / ON DUTY BYPASS
    //
    // Employee is inside an approved Outside Work window.
    //
    // During this phase:
    //
    // - office geofence loss does not trigger break
    // - office Wi-Fi loss does not trigger break
    // - 3-minute grace logic is bypassed
    // - 90-minute office absence logic is bypassed
    //
    // Outside Work authorization comes from CRM/backend.
    // ========================================================

    object OnDutyBypass : AttendanceEnginePhase()
}


// ============================================================
// ATTENDANCE ENGINE TIMING
//
// Pure helper methods for monotonic-duration calculations.
//
// Duration values are supplied by CRM configuration.
//
// We do NOT hard-code production policy values here.
// ============================================================

object AttendanceEngineTiming {


    // ========================================================
    // ELAPSED ABSENCE
    // ========================================================

    fun getAbsenceDurationMs(

        absenceStartedElapsedRealtimeMs: Long,

        currentElapsedRealtimeMs: Long

    ): Long {


        // Defensive check.
        //
        // elapsedRealtime should never move backwards during the
        // same boot session.

        if (
            currentElapsedRealtimeMs <
            absenceStartedElapsedRealtimeMs
        ) {

            return 0L
        }


        return currentElapsedRealtimeMs -
                absenceStartedElapsedRealtimeMs
    }


    // ========================================================
    // HAS GRACE PERIOD COMPLETED?
    //
    // Production configuration example:
    //
    // gracePeriodMs = 3 minutes
    //
    // But the value will come from attendance policy/config.
    // ========================================================

    fun hasGracePeriodCompleted(

        absenceStartedElapsedRealtimeMs: Long,

        currentElapsedRealtimeMs: Long,

        gracePeriodMs: Long

    ): Boolean {


        if (gracePeriodMs < 0L) {
            return false
        }


        val absenceDuration =
            getAbsenceDurationMs(

                absenceStartedElapsedRealtimeMs =
                    absenceStartedElapsedRealtimeMs,

                currentElapsedRealtimeMs =
                    currentElapsedRealtimeMs
            )


        return absenceDuration >= gracePeriodMs
    }


    // ========================================================
    // HAS MAXIMUM ABSENCE COMPLETED?
    //
    // Production configuration example:
    //
    // maximumAbsenceMs = 90 minutes
    //
    // Again, this comes from CRM configuration.
    //
    // IMPORTANT:
    //
    // Maximum absence is calculated from the ORIGINAL presence
    // loss time, NOT from BREAK_START confirmation time.
    // ========================================================

    fun hasMaximumAbsenceCompleted(

        absenceStartedElapsedRealtimeMs: Long,

        currentElapsedRealtimeMs: Long,

        maximumAbsenceMs: Long

    ): Boolean {


        if (maximumAbsenceMs <= 0L) {
            return false
        }


        val absenceDuration =
            getAbsenceDurationMs(

                absenceStartedElapsedRealtimeMs =
                    absenceStartedElapsedRealtimeMs,

                currentElapsedRealtimeMs =
                    currentElapsedRealtimeMs
            )


        return absenceDuration >= maximumAbsenceMs
    }
}
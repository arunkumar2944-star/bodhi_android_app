package com.prodigit.bodhiattendance.domain.attendance


// ============================================================
// OFFICE PRESENCE SNAPSHOT
//
// Represents one evaluated office-presence observation.
//
// Geofence evidence plus the Wi-Fi / BLE requirements supplied by CRM.
// Missing CRM flags block evaluation; Android does not select office policy.
//
// capturedAtElapsedRealtimeMs is monotonic Android time.
// Phone wall-clock time is NOT used.
// ============================================================

data class OfficePresenceSnapshot(

    val geofenceVerified: Boolean,

    val wifiVerified: Boolean,

    val beaconVerified: Boolean = false,

    val capturedAtElapsedRealtimeMs: Long,

    val observationUncertain: Boolean = false

) {

    fun isValidOfficePresence(policy: AttendanceEnginePolicy): Boolean =
        policy.wifiRequired != null && policy.bleRequired != null &&
            geofenceVerified &&
            (policy.wifiRequired == false || wifiVerified) &&
            (policy.bleRequired == false || beaconVerified)
}


// ============================================================
// ATTENDANCE ENGINE POLICY
//
// These values will eventually come from CRM / Express.
//
// Example production defaults:
//
// gracePeriodMs     = 3 minutes
// maximumAbsenceMs = 90 minutes
//
// We do NOT hard-code those values inside the decision logic.
// ============================================================

data class AttendanceEnginePolicy(

    val gracePeriodMs: Long,

    val maximumAbsenceMs: Long,

    // Must be supplied from the matched CRM office. Null blocks evaluation.
    val wifiRequired: Boolean? = null,
    val bleRequired: Boolean? = null

)


// ============================================================
// ATTENDANCE ENGINE DECISION
//
// IMPORTANT:
//
// A decision is NOT automatically an attendance event.
//
// The integration layer later decides whether to:
//
// - persist an event
// - change only internal engine phase
// - do nothing
//
// This prevents unnecessary Room attendance rows.
// ============================================================

sealed class AttendanceEngineDecision {


    // ========================================================
    // NO REAL ATTENDANCE EVENT
    //
    // Used for:
    //
    // - remaining present
    // - starting grace
    // - cancelling grace
    // - waiting during break
    // - checked-out state
    // - outside-work bypass
    // ========================================================

    data class NoAttendanceEvent(

        val nextPhase: AttendanceEnginePhase,

        val reason: String

    ) : AttendanceEngineDecision()


    // ========================================================
    // CREATE REAL ATTENDANCE EVENT
    //
    // Only genuine attendance transitions reach this result.
    //
    // Examples:
    //
    // CHECK_IN
    // BREAK_START
    // BREAK_END
    //
    // The eventElapsedRealtimeMs will later be passed to our
    // trusted event creation layer.
    // ========================================================

    data class CreateAttendanceEvent(

        val eventType: AttendanceEventType,

        val eventElapsedRealtimeMs: Long,

        val expectedNextAttendanceState: AttendanceState,

        val nextPhase: AttendanceEnginePhase,

        val reason: String

    ) : AttendanceEngineDecision()


    // ========================================================
    // CLASSIFY ORIGINAL EXIT AS CHECKOUT
    //
    // This is deliberately separate from CreateAttendanceEvent.
    //
    // Why?
    //
    // Normal ON_BREAK state allows only BREAK_END.
    //
    // But our approved automatic attendance policy says:
    //
    // if employee does not return within the configured maximum
    // absence period, classify the ORIGINAL office exit as the
    // effective CHECK_OUT.
    //
    // Therefore this is an Attendance Engine classification,
    // not an ordinary ON_BREAK -> CHECK_OUT transition.
    //
    // Example:
    //
    // absence started = 18:25
    // decision reached = 19:55
    //
    // effective CHECK_OUT time = 18:25
    // ========================================================

    data class ClassifyCheckout(

        val effectiveCheckoutElapsedRealtimeMs: Long,

        val classifiedAtElapsedRealtimeMs: Long,

        val nextPhase: AttendanceEnginePhase.CheckoutClassified,

        val reason: String

    ) : AttendanceEngineDecision()


    // ========================================================
    // ENGINE BLOCKED
    //
    // Something required for a safe decision is missing or
    // inconsistent.
    //
    // IMPORTANT:
    //
    // This must NOT create an attendance Room event.
    // ========================================================

    data class Blocked(

        val nextPhase: AttendanceEnginePhase,

        val reason: String

    ) : AttendanceEngineDecision()
}


// ============================================================
// ATTENDANCE PRESENCE DECISION ENGINE
//
// Pure business logic.
//
// It does NOT:
//
// - read GPS itself
// - read Wi-Fi itself
// - access Room
// - call Retrofit
// - create UUIDs
// - call System.currentTimeMillis()
//
// Input:
//
// Attendance state
// Engine phase
// Presence snapshot
// CRM policy
//
// Output:
//
// AttendanceEngineDecision
// ============================================================

object AttendancePresenceDecisionEngine {


    // ========================================================
    // EVALUATE
    // ========================================================

    fun evaluate(

        attendanceState: AttendanceState,

        currentPhase: AttendanceEnginePhase,

        presence: OfficePresenceSnapshot,

        policy: AttendanceEnginePolicy

    ): AttendanceEngineDecision {


        // ====================================================
        // POLICY VALIDATION
        // ====================================================

        if (presence.observationUncertain) {
            return AttendanceEngineDecision.NoAttendanceEvent(
                nextPhase = if (attendanceState == AttendanceState.WORKING && currentPhase is AttendanceEnginePhase.AbsenceGrace)
                    AttendanceEnginePhase.Present else currentPhase,
                reason = "Evidence uncertain; no event; unconfirmed absence grace cancelled"
            )
        }

        if (policy.wifiRequired == null || policy.bleRequired == null) {
            return AttendanceEngineDecision.Blocked(
                nextPhase = currentPhase,
                reason = "Required CRM office policy flags are unavailable"
            )
        }

        if (policy.gracePeriodMs < 0L) {

            return AttendanceEngineDecision.Blocked(

                nextPhase =
                    currentPhase,

                reason =
                    "Grace period configuration is invalid"
            )
        }


        if (policy.maximumAbsenceMs <= 0L) {

            return AttendanceEngineDecision.Blocked(

                nextPhase =
                    currentPhase,

                reason =
                    "Maximum absence configuration is invalid"
            )
        }


        if (
            policy.maximumAbsenceMs <
            policy.gracePeriodMs
        ) {

            return AttendanceEngineDecision.Blocked(

                nextPhase =
                    currentPhase,

                reason =
                    "Maximum absence must be greater than or equal to grace period"
            )
        }


        // ====================================================
        // DECISION BY CURRENT ATTENDANCE STATE
        // ====================================================

        return when (attendanceState) {

            AttendanceState.NOT_CHECKED_IN ->
                evaluateNotCheckedIn(
                    presence = presence,
                    policy = policy
                )


            AttendanceState.WORKING ->
                evaluateWorking(
                    currentPhase = currentPhase,
                    presence = presence,
                    policy = policy
                )


            AttendanceState.ON_BREAK ->
                evaluateOnBreak(
                    currentPhase = currentPhase,
                    presence = presence,
                    policy = policy
                )


            AttendanceState.CHECKED_OUT ->
                AttendanceEngineDecision
                    .NoAttendanceEvent(

                        nextPhase =
                            AttendanceEnginePhase.NotMonitoring,

                        reason =
                            "Employee is already checked out"
                    )


            AttendanceState.ON_DUTY ->
                AttendanceEngineDecision
                    .NoAttendanceEvent(

                        nextPhase =
                            AttendanceEnginePhase.OnDutyBypass,

                        reason =
                            "Approved Outside Work is active; office presence rules are bypassed"
                    )
        }
    }


    // ========================================================
    // NOT CHECKED IN
    //
    // Valid presence:
    //
    // Geofence + Wi-Fi
    //
    // generates CHECK_IN.
    //
    // Invalid presence:
    //
    // do nothing.
    // ========================================================

    private fun evaluateNotCheckedIn(

        presence: OfficePresenceSnapshot,
        policy: AttendanceEnginePolicy

    ): AttendanceEngineDecision {


        if (!presence.isValidOfficePresence(policy)) {

            return AttendanceEngineDecision
                .NoAttendanceEvent(

                    nextPhase =
                        AttendanceEnginePhase.NotMonitoring,

                    reason =
                        "Employee does not yet have valid office presence"
                )
        }


        return createNormalAttendanceEvent(

            currentState =
                AttendanceState.NOT_CHECKED_IN,

            eventType =
                AttendanceEventType.CHECK_IN,

            eventElapsedRealtimeMs =
                presence.capturedAtElapsedRealtimeMs,

            nextPhase =
                AttendanceEnginePhase.Present,

            reason =
                "Valid Geofence and Office Wi-Fi presence detected"
        )
    }


    // ========================================================
    // WORKING
    //
    // PRESENT
    //    ↓ presence lost
    //
    // ABSENCE_GRACE
    //
    // If presence returns during grace:
    //
    //      cancel grace
    //
    // If grace completes:
    //
    //      BREAK_START
    // ========================================================

    private fun evaluateWorking(

        currentPhase: AttendanceEnginePhase,

        presence: OfficePresenceSnapshot,

        policy: AttendanceEnginePolicy

    ): AttendanceEngineDecision {


        // ====================================================
        // OFFICE PRESENCE VALID
        // ====================================================

        if (presence.isValidOfficePresence(policy)) {


            return when (currentPhase) {


                is AttendanceEnginePhase.AbsenceGrace -> {

                    AttendanceEngineDecision
                        .NoAttendanceEvent(

                            nextPhase =
                                AttendanceEnginePhase.Present,

                            reason =
                                "Office presence restored within grace period; absence cancelled"
                        )
                }


                is AttendanceEnginePhase.Present -> {

                    AttendanceEngineDecision
                        .NoAttendanceEvent(

                            nextPhase =
                                currentPhase,

                            reason =
                                "Employee remains present"
                        )
                }


                else -> {

                    // Runtime phase may have been reset after an
                    // application restart.
                    //
                    // Since current attendance state says WORKING
                    // and valid presence exists, PRESENT is safe.

                    AttendanceEngineDecision
                        .NoAttendanceEvent(

                            nextPhase =
                                AttendanceEnginePhase.Present,

                            reason =
                                "Working employee has valid office presence"
                        )
                }
            }
        }


        // ====================================================
        // OFFICE PRESENCE LOST
        // ====================================================

        return when (currentPhase) {


            // ------------------------------------------------
            // START GRACE PERIOD
            //
            // NO Room attendance event.
            // ------------------------------------------------

            AttendanceEnginePhase.Present,
            AttendanceEnginePhase.NotMonitoring -> {

                AttendanceEngineDecision
                    .NoAttendanceEvent(

                        nextPhase =
                            AttendanceEnginePhase
                                .AbsenceGrace(

                                    absenceStartedElapsedRealtimeMs =
                                        presence.capturedAtElapsedRealtimeMs
                                ),

                        reason =
                            "Office presence lost; absence grace period started"
                    )
            }


            // ------------------------------------------------
            // GRACE ALREADY RUNNING
            // ------------------------------------------------

            is AttendanceEnginePhase.AbsenceGrace -> {


                val graceCompleted =
                    AttendanceEngineTiming
                        .hasGracePeriodCompleted(

                            absenceStartedElapsedRealtimeMs =
                                currentPhase
                                    .absenceStartedElapsedRealtimeMs,

                            currentElapsedRealtimeMs =
                                presence
                                    .capturedAtElapsedRealtimeMs,

                            gracePeriodMs =
                                policy.gracePeriodMs
                        )


                if (!graceCompleted) {

                    return AttendanceEngineDecision
                        .NoAttendanceEvent(

                            nextPhase =
                                currentPhase,

                            reason =
                                "Office presence is still absent but grace period has not completed"
                        )
                }


                // =============================================
                // GRACE COMPLETED
                //
                // BREAK_START is created NOW.
                //
                // We still separately preserve the ORIGINAL
                // absence start time for the possible future
                // 90-minute checkout classification.
                // =============================================

                return createNormalAttendanceEvent(

                    currentState =
                        AttendanceState.WORKING,

                    eventType =
                        AttendanceEventType.BREAK_START,

                    eventElapsedRealtimeMs =
                        currentPhase.absenceStartedElapsedRealtimeMs,

                    nextPhase =
                        AttendanceEnginePhase
                            .BreakActive(

                                absenceStartedElapsedRealtimeMs =
                                    currentPhase
                                        .absenceStartedElapsedRealtimeMs,

                                breakConfirmedElapsedRealtimeMs =
                                    presence
                                        .capturedAtElapsedRealtimeMs
                            ),

                    reason =
                        "Office presence remained absent after grace period"
                )
            }


            // ------------------------------------------------
            // PHASE/STATE INCONSISTENCY
            //
            // WORKING should not normally have BreakActive.
            //
            // Do not generate another BREAK_START.
            // ------------------------------------------------

            is AttendanceEnginePhase.BreakActive -> {

                AttendanceEngineDecision.Blocked(

                    nextPhase =
                        currentPhase,

                    reason =
                        "Attendance state is WORKING but engine phase is already BreakActive"
                )
            }


            is AttendanceEnginePhase.CheckoutClassified -> {

                AttendanceEngineDecision.Blocked(

                    nextPhase =
                        currentPhase,

                    reason =
                        "Checkout has already been classified"
                )
            }


            AttendanceEnginePhase.OnDutyBypass -> {

                AttendanceEngineDecision.Blocked(

                    nextPhase =
                        currentPhase,

                    reason =
                        "Outside Work bypass phase is active while attendance state is WORKING"
                )
            }
        }
    }


    // ========================================================
    // ON BREAK
    //
    // Presence restored:
    //
    //      BREAK_END
    //
    // Still absent:
    //
    //      wait
    //
    // Maximum absence reached:
    //
    //      classify ORIGINAL absence as CHECK_OUT
    // ========================================================

    private fun evaluateOnBreak(

        currentPhase: AttendanceEnginePhase,

        presence: OfficePresenceSnapshot,

        policy: AttendanceEnginePolicy

    ): AttendanceEngineDecision {


        // ====================================================
        // BREAK TIMING CONTEXT MUST EXIST
        // ====================================================

        val breakPhase =
            currentPhase as?
                    AttendanceEnginePhase.BreakActive


        if (breakPhase == null) {

            return AttendanceEngineDecision.Blocked(

                nextPhase =
                    currentPhase,

                reason =
                    "Employee is ON_BREAK but original absence timing context is unavailable"
            )
        }


        // ====================================================
        // EMPLOYEE RETURNED TO VALID OFFICE PRESENCE
        // ====================================================

        if (presence.isValidOfficePresence(policy)) {


            return createNormalAttendanceEvent(

                currentState =
                    AttendanceState.ON_BREAK,

                eventType =
                    AttendanceEventType.BREAK_END,

                eventElapsedRealtimeMs =
                    presence.capturedAtElapsedRealtimeMs,

                nextPhase =
                    AttendanceEnginePhase.Present,

                reason =
                    "Valid office presence restored while employee was on break"
            )
        }


        // ====================================================
        // EMPLOYEE REMAINS ABSENT
        // ====================================================

        val maximumAbsenceCompleted =
            AttendanceEngineTiming
                .hasMaximumAbsenceCompleted(

                    absenceStartedElapsedRealtimeMs =
                        breakPhase
                            .absenceStartedElapsedRealtimeMs,

                    currentElapsedRealtimeMs =
                        presence
                            .capturedAtElapsedRealtimeMs,

                    maximumAbsenceMs =
                        policy.maximumAbsenceMs
                )


        if (!maximumAbsenceCompleted) {

            return AttendanceEngineDecision
                .NoAttendanceEvent(

                    nextPhase =
                        breakPhase,

                    reason =
                        "Employee remains absent but maximum absence period has not completed"
                )
        }


        // ====================================================
        // MAXIMUM ABSENCE REACHED
        //
        // Example:
        //
        // 18:25  original presence loss
        // 18:28  BREAK_START confirmation
        // 19:55  90-minute classification point
        //
        // Effective CHECK_OUT remains 18:25.
        // ====================================================

        val checkoutPhase =
            AttendanceEnginePhase
                .CheckoutClassified(

                    absenceStartedElapsedRealtimeMs =
                        breakPhase
                            .absenceStartedElapsedRealtimeMs,

                    classifiedAtElapsedRealtimeMs =
                        presence
                            .capturedAtElapsedRealtimeMs
                )


        return AttendanceEngineDecision
            .ClassifyCheckout(

                effectiveCheckoutElapsedRealtimeMs =
                    breakPhase
                        .absenceStartedElapsedRealtimeMs,

                classifiedAtElapsedRealtimeMs =
                    presence
                        .capturedAtElapsedRealtimeMs,

                nextPhase =
                    checkoutPhase,

                reason =
                    "Maximum absence reached; original office exit must be classified as CHECK_OUT"
            )
    }


    // ========================================================
    // CREATE NORMAL ATTENDANCE EVENT SAFELY
    //
    // Every ordinary event must still pass our central
    // AttendanceStateMachine.
    //
    // Therefore invalid transitions never reach Room.
    // ========================================================

    private fun createNormalAttendanceEvent(

        currentState: AttendanceState,

        eventType: AttendanceEventType,

        eventElapsedRealtimeMs: Long,

        nextPhase: AttendanceEnginePhase,

        reason: String

    ): AttendanceEngineDecision {


        val transition =
            AttendanceStateMachine
                .validate(

                    currentState =
                        currentState,

                    eventType =
                        eventType
                )


        return when (transition) {


            is AttendanceTransitionResult.Allowed -> {

                AttendanceEngineDecision
                    .CreateAttendanceEvent(

                        eventType =
                            eventType,

                        eventElapsedRealtimeMs =
                            eventElapsedRealtimeMs,

                        expectedNextAttendanceState =
                            transition.nextState,

                        nextPhase =
                            nextPhase,

                        reason =
                            reason
                    )
            }


            is AttendanceTransitionResult.Blocked -> {

                AttendanceEngineDecision.Blocked(

                    nextPhase =
                        nextPhase,

                    reason =
                        transition.reason
                )
            }
        }
    }
}
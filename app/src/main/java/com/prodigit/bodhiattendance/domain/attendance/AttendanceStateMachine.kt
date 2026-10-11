package com.prodigit.bodhiattendance.domain.attendance


// ============================================================
// ATTENDANCE STATE
//
// Represents the employee's NORMAL attendance state.
//
// IMPORTANT:
//
// Temporary sensor/timer states such as:
//
// - 3-minute absence grace
// - pending exit
// - waiting for 90-minute absence decision
//
// are NOT attendance states.
//
// Those will belong to AttendanceEngine later.
// ============================================================

enum class AttendanceState {

    // Employee has not checked in for the current attendance day.
    NOT_CHECKED_IN,

    // Employee is currently considered working in the office.
    WORKING,

    // Employee has started a break / temporary absence.
    ON_BREAK,

    // Employee has completed checkout for the attendance day.
    CHECKED_OUT,

    // Employee is inside an approved Outside Work / On Duty window.
    //
    // This is handled through the Outside Work policy and should
    // not be treated as normal physical office presence.
    ON_DUTY
}


// ============================================================
// ATTENDANCE EVENT TYPE
//
// These are REAL attendance events that may be persisted to Room
// and later synchronized with Express.
//
// Internal engine states/timers must NOT be represented as events.
// ============================================================

enum class AttendanceEventType {

    CHECK_IN,

    BREAK_START,

    BREAK_END,

    CHECK_OUT
}


// ============================================================
// TRANSITION RESULT
//
// Gives the caller a structured answer instead of only Boolean.
//
// This is useful later for:
//
// - Attendance Engine
// - debug logging
// - Event Log
// - unit tests
// ============================================================

sealed class AttendanceTransitionResult {


    // --------------------------------------------------------
    // VALID TRANSITION
    // --------------------------------------------------------

    data class Allowed(

        val currentState: AttendanceState,

        val eventType: AttendanceEventType,

        val nextState: AttendanceState

    ) : AttendanceTransitionResult()


    // --------------------------------------------------------
    // INVALID TRANSITION
    //
    // IMPORTANT:
    //
    // When this result is returned:
    //
    // - DO NOT create AttendanceEventEntity
    // - DO NOT insert into Room
    // - DO NOT call Express
    // --------------------------------------------------------

    data class Blocked(

        val currentState: AttendanceState,

        val eventType: AttendanceEventType,

        val reason: String

    ) : AttendanceTransitionResult()
}


// ============================================================
// ATTENDANCE STATE MACHINE
//
// This is the central transition validator.
//
// Production flow:
//
// Sensors / Attendance Engine
//          ↓
// Determine required attendance event
//          ↓
// AttendanceStateMachine.validate()
//          ↓
//      Allowed?
//       /    \
//     NO      YES
//     ↓        ↓
// Ignore    Create event
//            ↓
//          Room
//            ↓
//          Express
//
// Express remains the FINAL authority.
// ============================================================

object AttendanceStateMachine {


    // ========================================================
    // VALIDATE TRANSITION
    // ========================================================

    fun validate(

        currentState: AttendanceState,

        eventType: AttendanceEventType

    ): AttendanceTransitionResult {


        return when (currentState) {


            // =================================================
            // NOT CHECKED IN
            //
            // Only CHECK_IN is valid.
            // =================================================

            AttendanceState.NOT_CHECKED_IN -> {

                when (eventType) {

                    AttendanceEventType.CHECK_IN -> {

                        AttendanceTransitionResult.Allowed(

                            currentState =
                                currentState,

                            eventType =
                                eventType,

                            nextState =
                                AttendanceState.WORKING
                        )
                    }


                    else -> {

                        AttendanceTransitionResult.Blocked(

                            currentState =
                                currentState,

                            eventType =
                                eventType,

                            reason =
                                "Only CHECK_IN is allowed when employee has not checked in"
                        )
                    }
                }
            }


            // =================================================
            // WORKING
            //
            // Valid:
            //
            // BREAK_START
            // CHECK_OUT
            //
            // Invalid:
            //
            // CHECK_IN
            // BREAK_END
            // =================================================

            AttendanceState.WORKING -> {

                when (eventType) {

                    AttendanceEventType.BREAK_START -> {

                        AttendanceTransitionResult.Allowed(

                            currentState =
                                currentState,

                            eventType =
                                eventType,

                            nextState =
                                AttendanceState.ON_BREAK
                        )
                    }


                    AttendanceEventType.CHECK_OUT -> {

                        AttendanceTransitionResult.Allowed(

                            currentState =
                                currentState,

                            eventType =
                                eventType,

                            nextState =
                                AttendanceState.CHECKED_OUT
                        )
                    }


                    else -> {

                        AttendanceTransitionResult.Blocked(

                            currentState =
                                currentState,

                            eventType =
                                eventType,

                            reason =
                                "While WORKING only BREAK_START or CHECK_OUT is allowed"
                        )
                    }
                }
            }


            // =================================================
            // ON BREAK
            //
            // Only BREAK_END is normally valid.
            //
            // IMPORTANT:
            //
            // Our approved 90-minute absence rule is handled by
            // AttendanceEngine later.
            //
            // The engine may classify the ORIGINAL absence as
            // CHECK_OUT after the configured maximum absence.
            //
            // We will not allow random/manual CHECK_OUT from this
            // state through the normal transition path.
            // =================================================

            AttendanceState.ON_BREAK -> {

                when (eventType) {

                    AttendanceEventType.BREAK_END -> {

                        AttendanceTransitionResult.Allowed(

                            currentState =
                                currentState,

                            eventType =
                                eventType,

                            nextState =
                                AttendanceState.WORKING
                        )
                    }


                    else -> {

                        AttendanceTransitionResult.Blocked(

                            currentState =
                                currentState,

                            eventType =
                                eventType,

                            reason =
                                "Only BREAK_END is allowed while employee is on break"
                        )
                    }
                }
            }


            // =================================================
            // CHECKED OUT
            //
            // No more normal attendance events are allowed for
            // the same attendance day.
            //
            // A new day's state must be created separately.
            // =================================================

            AttendanceState.CHECKED_OUT -> {

                AttendanceTransitionResult.Blocked(

                    currentState =
                        currentState,

                    eventType =
                        eventType,

                    reason =
                        "Attendance is already checked out for the current attendance day"
                )
            }


            // =================================================
            // ON DUTY / OUTSIDE WORK
            //
            // Outside Work is controlled by approved CRM policy.
            //
            // During the approved window:
            //
            // - Geofence/Wi-Fi break detection is bypassed
            // - normal automatic office transitions are not
            //   generated from office-presence loss
            //
            // AttendanceEngine will handle entering/leaving this
            // policy window.
            // =================================================

            AttendanceState.ON_DUTY -> {

                AttendanceTransitionResult.Blocked(

                    currentState =
                        currentState,

                    eventType =
                        eventType,

                    reason =
                        "Normal office attendance events are controlled by Outside Work policy while ON_DUTY"
                )
            }
        }
    }


    // ========================================================
    // BOOLEAN HELPER
    //
    // Useful when caller only needs yes/no.
    // ========================================================

    fun isAllowed(

        currentState: AttendanceState,

        eventType: AttendanceEventType

    ): Boolean {

        return validate(
            currentState = currentState,
            eventType = eventType
        ) is AttendanceTransitionResult.Allowed
    }


    // ========================================================
    // GET NEXT STATE
    //
    // Returns null when transition is invalid.
    // ========================================================

    fun getNextState(

        currentState: AttendanceState,

        eventType: AttendanceEventType

    ): AttendanceState? {

        val result =
            validate(
                currentState = currentState,
                eventType = eventType
            )


        return when (result) {

            is AttendanceTransitionResult.Allowed ->
                result.nextState

            is AttendanceTransitionResult.Blocked ->
                null
        }
    }
}
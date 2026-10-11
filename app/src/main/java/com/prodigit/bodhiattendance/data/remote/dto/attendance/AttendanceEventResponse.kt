package com.prodigit.bodhiattendance.data.remote.dto.attendance


// ============================================================
// ATTENDANCE EVENT RESPONSE
//
// Handles:
//
// - successful event
// - duplicate event
// - invalid transition
// - backend validation errors
//
// Examples:
//
// CHECK_IN
// BREAK_START
// BREAK_END
// CHECK_OUT
// ============================================================

data class AttendanceEventResponse(

    val success: Boolean,

    val duplicate: Boolean? = null,

    val code: String? = null,

    val message: String,

    val attendanceEventId: String? = null,

    val attendanceId: Long? = null,

    val eventUuid: String? = null,

    val eventType: String? = null,

    val previousState: String? = null,

    val currentState: String? = null,

    val requestedEvent: String? = null,

    val allowedNextEvents: List<String>? = null,

    val serverReceivedAt: String? = null,

    val effectiveEventTime: String? = null,

    val timeSource: String? = null,

    val timeConfidence: String? = null,

    val timeDriftSeconds: Long? = null
)
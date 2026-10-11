package com.prodigit.bodhiattendance.data.local.security

import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventResponse


// ============================================================
// ATTENDANCE EVENT RECORD RESULT
//
// Represents the COMPLETE outcome of creating an attendance
// event.
//
// IMPORTANT:
//
// Local persistence and remote synchronization are two separate
// concerns.
//
// An event can be successfully created in Room even when the
// network request fails.
//
// The Attendance Engine needs to know this distinction so it can
// advance its local state correctly and avoid generating duplicate
// attendance events while offline.
// ============================================================

sealed class AttendanceEventRecordResult {


    // ========================================================
    // EVENT WAS NOT STORED
    //
    // Examples:
    //
    // - invalid user/company/device
    // - no trusted time anchor
    // - invalid monotonic time
    // - Room insert failed
    //
    // Attendance Engine MUST NOT advance attendance state.
    // ========================================================

    data class NotStored(

        val error: Throwable

    ) : AttendanceEventRecordResult()


    // ========================================================
    // EVENT STORED AND SERVER ACCEPTED
    //
    // Room:
    //
    // SYNCED
    //
    // Attendance Engine MAY safely advance to the expected
    // next attendance state.
    // ========================================================

    data class StoredAndSynced(

        val eventUuid: String,

        val response: AttendanceEventResponse

    ) : AttendanceEventRecordResult()


    // ========================================================
    // EVENT STORED BUT REMOTE SYNC FAILED TEMPORARILY
    //
    // Room:
    //
    // FAILED_RETRYABLE
    //
    // The attendance event itself EXISTS and must NOT be created
    // again.
    //
    // WorkManager will later retry this exact eventUuid.
    //
    // Attendance Engine MAY advance its local attendance state
    // because the real event has already been durably recorded.
    // ========================================================

    data class StoredForRetry(

        val eventUuid: String,

        val error: String

    ) : AttendanceEventRecordResult()


    // ========================================================
    // EVENT STORED BUT EXPRESS REJECTED IT
    //
    // Room:
    //
    // REJECTED
    //
    // Attendance Engine MUST NOT blindly advance state.
    //
    // This indicates Android/server disagreement or invalid
    // evidence and should later trigger state reconciliation.
    // ========================================================

    data class StoredRejected(

        val eventUuid: String,

        val response: AttendanceEventResponse,

        val error: String

    ) : AttendanceEventRecordResult()
}
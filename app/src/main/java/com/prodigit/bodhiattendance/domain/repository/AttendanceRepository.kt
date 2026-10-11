package com.prodigit.bodhiattendance.domain.repository

import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventRequest
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventResponse
import com.prodigit.bodhiattendance.data.remote.dto.attendance.TimeAnchorResponse


interface AttendanceRepository {


    // ============================================================
    // CREATE TRUSTED TIME ANCHOR
    // ============================================================

    suspend fun createTimeAnchor(
        token: String,
        deviceId: String,
        elapsedRealtimeMs: Long,
        bootId: String
    ): Result<TimeAnchorResponse>


    // ============================================================
    // RECORD ATTENDANCE EVENT
    //
    // The request is already prepared by the attendance layer.
    //
    // Repository responsibility:
    //
    // - attach JWT
    // - call Express
    // - parse backend response
    // - preserve business errors
    // ============================================================

    suspend fun recordAttendanceEvent(
        token: String,
        request: AttendanceEventRequest
    ): Result<AttendanceEventResponse>
}
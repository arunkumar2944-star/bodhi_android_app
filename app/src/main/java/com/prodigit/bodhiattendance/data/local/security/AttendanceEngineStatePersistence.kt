package com.prodigit.bodhiattendance.data.local.repository

import com.prodigit.bodhiattendance.data.local.entity.AttendanceEngineStateEntity


// ============================================================
// ATTENDANCE ENGINE STATE PERSISTENCE
//
// Small abstraction around storage of the current engine state.
//
// Production:
//      AttendanceEngineStateRepository
//
// Unit tests:
//      FakeAttendanceEngineStatePersistence
//
// This allows AttendanceEngineStateStore to be tested without
// requiring a real Room database.
// ============================================================

interface AttendanceEngineStatePersistence {


    // ========================================================
    // SAVE / REPLACE CURRENT STATE
    // ========================================================

    suspend fun saveState(
        state: AttendanceEngineStateEntity
    )


    // ========================================================
    // GET CURRENT STATE
    // ========================================================

    suspend fun getState(
        userId: Int,
        deviceId: String
    ): AttendanceEngineStateEntity?


    // ========================================================
    // DELETE CURRENT STATE
    // ========================================================

    suspend fun deleteState(
        userId: Int,
        deviceId: String
    )
}
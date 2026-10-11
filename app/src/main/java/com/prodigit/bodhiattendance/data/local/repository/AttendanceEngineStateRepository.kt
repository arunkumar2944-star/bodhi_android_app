package com.prodigit.bodhiattendance.data.local.repository

import com.prodigit.bodhiattendance.data.local.dao.AttendanceEngineStateDao
import com.prodigit.bodhiattendance.data.local.entity.AttendanceEngineStateEntity

import kotlinx.coroutines.flow.Flow


// ============================================================
// ATTENDANCE ENGINE STATE REPOSITORY
//
// Provides one access layer for the temporary automatic
// attendance-engine state.
//
// IMPORTANT:
//
// This repository is NOT attendance history.
//
// It stores only the current engine state needed for safe
// recovery after:
//
// - process death
// - app restart
// - background termination
//
// Real attendance events continue to use:
//
// OfflineAttendanceRepository
// +
// attendance_events table
// ============================================================

class AttendanceEngineStateRepository(

    private val attendanceEngineStateDao:
    AttendanceEngineStateDao

) : AttendanceEngineStatePersistence {


    // ========================================================
    // SAVE CURRENT ENGINE STATE
    //
    // The DAO uses REPLACE.
//
// Therefore:
//
// userId + deviceId
//
// always represents one current engine-state row.
// ========================================================

    override  suspend fun saveState(
        state: AttendanceEngineStateEntity
    ) {

        attendanceEngineStateDao
            .saveState(
                state = state
            )
    }


    // ========================================================
    // GET CURRENT ENGINE STATE
    //
    // The caller MUST still validate bootId before reusing
    // persisted monotonic timestamps.
// ========================================================

    override   suspend fun getState(
        userId: Int,
        deviceId: String
    ): AttendanceEngineStateEntity? {

        return attendanceEngineStateDao
            .getState(
                userId = userId,
                deviceId = deviceId
            )
    }


    // ========================================================
    // OBSERVE CURRENT ENGINE STATE
    //
// Useful later for:
//
// - System Health
// - debug information
// - engine diagnostics
// ========================================================

    fun observeState(
        userId: Int,
        deviceId: String
    ): Flow<AttendanceEngineStateEntity?> {

        return attendanceEngineStateDao
            .observeState(
                userId = userId,
                deviceId = deviceId
            )
    }


    // ========================================================
    // DELETE CURRENT ENGINE STATE
    //
// Appropriate for:
//
// - logout
// - device deregistration
// - controlled local reset
//
// Normal attendance transitions should SAVE/REPLACE instead.
// ========================================================

    override  suspend fun deleteState(
        userId: Int,
        deviceId: String
    ) {

        attendanceEngineStateDao
            .deleteState(
                userId = userId,
                deviceId = deviceId
            )
    }


    // ========================================================
    // DELETE ALL ENGINE STATES
    //
// Mainly development / full local cleanup.
// ========================================================

    suspend fun deleteAllStates() {

        attendanceEngineStateDao
            .deleteAllStates()
    }


    // ========================================================
    // GET ALL ENGINE STATES
    //
// Useful for:
//
// - development validation
// - migration checking
// - diagnostics
//
// Production attendance logic normally uses getState()
// for the active user/device.
// ========================================================

    suspend fun getAllStates():
            List<AttendanceEngineStateEntity> {

        return attendanceEngineStateDao
            .getAllStates()
    }
}
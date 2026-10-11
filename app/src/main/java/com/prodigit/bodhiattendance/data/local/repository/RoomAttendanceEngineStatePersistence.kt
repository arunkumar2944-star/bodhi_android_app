package com.prodigit.bodhiattendance.data.local.repository

import com.prodigit.bodhiattendance.data.local.dao.AttendanceEngineStateDao
import com.prodigit.bodhiattendance.data.local.entity.AttendanceEngineStateEntity


// ============================================================
// ROOM ATTENDANCE ENGINE STATE PERSISTENCE
//
// Concrete implementation of:
//
// AttendanceEngineStatePersistence
//
// Connects:
//
// AttendanceEngineStateStore
//          ↓
// AttendanceEngineStatePersistence
//          ↓
// RoomAttendanceEngineStatePersistence
//          ↓
// AttendanceEngineStateDao
//          ↓
// Room
// ============================================================

class RoomAttendanceEngineStatePersistence(

    private val attendanceEngineStateDao:
    AttendanceEngineStateDao

) : AttendanceEngineStatePersistence {


    // ========================================================
    // SAVE / REPLACE CURRENT ENGINE STATE
    // ========================================================

    override suspend fun saveState(
        state: AttendanceEngineStateEntity
    ) {

        attendanceEngineStateDao
            .saveState(
                state
            )
    }


    // ========================================================
    // GET CURRENT ENGINE STATE
    // ========================================================

    override suspend fun getState(
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
    // DELETE CURRENT ENGINE STATE
    //
    // Used only when explicitly resetting persisted runtime state.
    // ========================================================

    override suspend fun deleteState(
        userId: Int,
        deviceId: String
    ) {

        attendanceEngineStateDao
            .deleteState(
                userId = userId,
                deviceId = deviceId
            )
    }
}
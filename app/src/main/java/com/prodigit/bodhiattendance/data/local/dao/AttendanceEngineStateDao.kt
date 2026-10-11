package com.prodigit.bodhiattendance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.prodigit.bodhiattendance.data.local.entity.AttendanceEngineStateEntity

import kotlinx.coroutines.flow.Flow


@Dao
interface AttendanceEngineStateDao {


    // ============================================================
    // SAVE / REPLACE CURRENT ENGINE STATE
    //
    // Primary key:
    //
    // userId + deviceId
    //
    // Therefore each employee/device combination has only ONE
    // current engine-state row.
    //
    // This is NOT attendance history.
    // ============================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun saveState(
        state: AttendanceEngineStateEntity
    )


    // ============================================================
    // GET CURRENT ENGINE STATE
    //
    // Used when:
    //
    // - application starts
    // - attendance engine restarts
    // - Android recreates the process
    //
    // IMPORTANT:
    //
    // The caller must still validate:
    //
    // stored bootId == current bootId
    //
    // before reusing monotonic timestamps.
    // ============================================================

    @Query(
        """
        SELECT *
        FROM attendance_engine_state
        WHERE
            userId = :userId
            AND deviceId = :deviceId
        LIMIT 1
        """
    )
    suspend fun getState(
        userId: Int,
        deviceId: String
    ): AttendanceEngineStateEntity?


    // ============================================================
    // OBSERVE CURRENT ENGINE STATE
    //
    // Useful later for:
    //
    // - System Health
    // - debug screen
    // - engine diagnostics
    // ============================================================

    @Query(
        """
        SELECT *
        FROM attendance_engine_state
        WHERE
            userId = :userId
            AND deviceId = :deviceId
        LIMIT 1
        """
    )
    fun observeState(
        userId: Int,
        deviceId: String
    ): Flow<AttendanceEngineStateEntity?>


    // ============================================================
    // DELETE ONE ENGINE STATE
    //
    // Used when:
    //
    // - logout
    // - device deregistration
    // - explicit engine reset
    //
    // Do NOT use this for normal attendance transitions.
    // Normally we replace the current row instead.
    // ============================================================

    @Query(
        """
        DELETE FROM attendance_engine_state
        WHERE
            userId = :userId
            AND deviceId = :deviceId
        """
    )
    suspend fun deleteState(
        userId: Int,
        deviceId: String
    )


    // ============================================================
    // DELETE ALL ENGINE STATES
    //
    // Mainly useful for:
    //
    // - development reset
    // - full local account cleanup
    //
    // Avoid using this casually in production.
    // ============================================================

    @Query(
        """
        DELETE FROM attendance_engine_state
        """
    )
    suspend fun deleteAllStates()


    // ============================================================
    // GET ALL STATES
    //
    // Mostly useful for debugging / migration validation.
    //
    // Normally there should be very few rows because each
    // user/device pair only has one current state.
    // ============================================================

    @Query(
        """
        SELECT *
        FROM attendance_engine_state
        ORDER BY updatedAtEpochMs DESC
        """
    )
    suspend fun getAllStates():
            List<AttendanceEngineStateEntity>
}
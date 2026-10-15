package com.prodigit.bodhiattendance.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.prodigit.bodhiattendance.data.local.entity.AttendanceDiagnosticEntity

@Dao
interface AttendanceDiagnosticDao {
    @Insert suspend fun insert(row: AttendanceDiagnosticEntity)
    @Query("SELECT * FROM attendance_diagnostics WHERE userId=:userId AND deviceId=:deviceId AND uploaded=0 ORDER BY observedElapsedMs LIMIT 30")
    suspend fun pending(userId: Int, deviceId: String): List<AttendanceDiagnosticEntity>
    @Query("UPDATE attendance_diagnostics SET uploaded=1 WHERE userId=:userId AND deviceId=:deviceId AND id IN (:ids)")
    suspend fun acknowledge(userId: Int, deviceId: String, ids: List<String>)
    @Query("SELECT * FROM attendance_diagnostics WHERE userId=:userId ORDER BY rowid DESC LIMIT 200")
    fun recent(userId: Int): Flow<List<AttendanceDiagnosticEntity>>
    // Bounded storage, including offline rows; never deletes attendance events.
    @Query("DELETE FROM attendance_diagnostics WHERE rowid NOT IN (SELECT rowid FROM attendance_diagnostics ORDER BY rowid DESC LIMIT 10000)")
    suspend fun trim()
}

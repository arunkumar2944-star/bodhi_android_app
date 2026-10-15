package com.prodigit.bodhiattendance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prodigit.bodhiattendance.data.local.entity.ArrivalCandidateEntity

@Dao
interface ArrivalCandidateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordFirst(candidate: ArrivalCandidateEntity): Long

    @Query("SELECT * FROM arrival_candidates WHERE userId=:userId AND deviceId=:deviceId AND uploaded=0 ORDER BY observedElapsedMs LIMIT 30")
    suspend fun pending(userId: Int, deviceId: String): List<ArrivalCandidateEntity>

    @Query("UPDATE arrival_candidates SET uploaded=1 WHERE userId=:userId AND deviceId=:deviceId AND id IN (:ids)")
    suspend fun markUploaded(userId: Int, deviceId: String, ids: List<String>)
}

package com.prodigit.bodhiattendance.data.local.dao

import androidx.room.*
import com.prodigit.bodhiattendance.data.local.entity.ProvisionalArrivalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProvisionalArrivalDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordFirst(row: ProvisionalArrivalEntity): Long

    @Query("SELECT * FROM provisional_arrivals WHERE userId=:user AND deviceId=:device AND status='SAVED_LOCALLY' ORDER BY trustedTimeMs LIMIT 20")
    suspend fun pending(user: Int, device: String): List<ProvisionalArrivalEntity>

    @Query("SELECT * FROM provisional_arrivals WHERE userId=:user AND deviceId=:device ORDER BY trustedTimeMs DESC LIMIT 100")
    fun observe(user: Int, device: String): Flow<List<ProvisionalArrivalEntity>>

    @Query("UPDATE provisional_arrivals SET status=:status, requestId=:requestId, message=:message WHERE id=:id AND userId=:user AND deviceId=:device")
    suspend fun acknowledge(id: String, user: Int, device: String, status: String, requestId: String?, message: String?)
}

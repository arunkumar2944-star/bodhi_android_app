package com.prodigit.bodhiattendance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prodigit.bodhiattendance.data.local.entity.RegisteredDeviceEntity

@Dao
interface RegisteredDeviceDao {
    @Query("SELECT * FROM registered_devices WHERE userId = :userId LIMIT 1")
    suspend fun forUser(userId: Int): RegisteredDeviceEntity?

    @Query("DELETE FROM registered_devices WHERE userId = :userId")
    suspend fun deleteForUser(userId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: RegisteredDeviceEntity)
}

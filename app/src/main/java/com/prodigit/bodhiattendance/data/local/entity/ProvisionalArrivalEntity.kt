package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "provisional_arrivals", indices = [
    Index(value = ["userId", "companyId", "deviceId", "workDate", "officeId"], unique = true)
])
data class ProvisionalArrivalEntity(
    @PrimaryKey val id: String,
    val userId: Int, val companyId: Int, val deviceId: String,
    val workDate: String, val officeId: Long, val trustedTimeMs: Long,
    val payloadJson: String,
    val status: String = "SAVED_LOCALLY",
    val requestId: String? = null,
    val message: String? = null
)

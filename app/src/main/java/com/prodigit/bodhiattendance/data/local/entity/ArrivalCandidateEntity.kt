package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** First approved-router observation, not a CHECK_IN or proof of continuous presence. */
@Entity(tableName = "arrival_candidates", indices = [Index(value = ["userId", "deviceId", "uploaded"])])
data class ArrivalCandidateEntity(
    @PrimaryKey val id: String,
    val userId: Int,
    val companyId: Int,
    val deviceId: String,
    val officeId: Long,
    val bootId: String,
    val anchorUuid: String,
    val observedElapsedMs: Long,
    val wifiBssid: String,
    val uploaded: Boolean = false
)

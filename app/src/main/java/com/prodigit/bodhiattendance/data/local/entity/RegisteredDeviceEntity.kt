package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Server-confirmed installation binding, never inferred from a phone model. */
@Entity(tableName = "registered_devices")
data class RegisteredDeviceEntity(
    @PrimaryKey val userId: Int,
    val registrationId: Int,
    val deviceId: String,
    val companyId: Int?,
    val isActive: Boolean,
    val isOnboarded: Boolean
)

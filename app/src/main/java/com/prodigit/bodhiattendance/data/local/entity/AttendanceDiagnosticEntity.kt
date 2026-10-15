package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "attendance_diagnostics", indices = [Index(value = ["userId", "deviceId", "uploaded"])])
data class AttendanceDiagnosticEntity(
    @PrimaryKey val id: String,
    val userId: Int,
    val deviceId: String,
    val bootId: String,
    val observedElapsedMs: Long,
    val deviceTime: String,
    val category: String,
    val message: String,
    val uploaded: Boolean = false
)

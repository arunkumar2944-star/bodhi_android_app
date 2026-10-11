package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


// ============================================================
// ATTENDANCE LOCATION ENTITY
//
// Local Room cache of an attendance location received from
// Express.
//
// Source of truth:
//
// React CRM
//      ↓
// Express / PostgreSQL
//      ↓
// Android Room cache
//
// Android does NOT create its own attendance locations.
//
// Used later by:
//
// - GPS evidence provider
// - BLE evidence provider
// - Wi-Fi evidence provider
// - Attendance Engine
//
// IMPORTANT:
//
// This is configuration cache data.
// It is separate from AttendanceEventEntity, which stores
// actual attendance events waiting for synchronization.
// ============================================================

@Entity(
    tableName = "attendance_locations"
)
data class AttendanceLocationEntity(

    // Backend attendance location ID.
    //
    // We deliberately use the backend ID as the Room primary
    // key so configuration updates replace the correct
    // location instead of creating duplicate local locations.

    @PrimaryKey
    val id: Long,


    // ========================================================
    // LOCATION
    // ========================================================

    val locationName: String,

    val locationCode: String,


    // ========================================================
    // GPS / GEOFENCE
    // ========================================================

    val latitude: Double,

    val longitude: Double,

    val geofenceRadiusMeters: Double,

    val maxAccuracyMeters: Double?,


    // ========================================================
    // LOCAL CACHE METADATA
    //
    // This is NOT attendance event time.
    //
    // It only tells Android when this configuration was
    // written into the local cache.
    // ========================================================

    val cachedAt: Long
)
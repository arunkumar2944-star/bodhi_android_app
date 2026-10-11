package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


// ============================================================
// ATTENDANCE BEACON ENTITY
//
// Local Room cache of BLE beacon configuration received from
// Express.
//
// One attendance location can contain multiple BLE beacons.
//
// Example:
//
// Main Office
//    ├── Entrance Beacon
//    └── Office Room Beacon
//
// Knowledge Centre
//    └── Knowledge Centre Beacon
//
// Android does NOT create or manage this configuration.
// Configuration originates from React CRM.
// ============================================================

@Entity(
    tableName = "attendance_beacons",

    foreignKeys = [

        ForeignKey(
            entity = AttendanceLocationEntity::class,

            parentColumns = ["id"],

            childColumns = ["attendanceLocationId"],

            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [

        // Required/recommended because attendanceLocationId
        // participates in a foreign-key relationship.
        Index(
            value = ["attendanceLocationId"]
        ),

        // Helps when matching a detected BLE identifier against
        // locally cached configuration.
        Index(
            value = ["identifier"]
        )
    ]
)
data class AttendanceBeaconEntity(

    // Backend beacon configuration ID.
    //
    // Backend ID is used directly as the local Room primary key.
    @PrimaryKey
    val id: Long,


    // ========================================================
    // PARENT ATTENDANCE LOCATION
    // ========================================================

    val attendanceLocationId: Long,


    // ========================================================
    // BLE CONFIGURATION
    // ========================================================

    val name: String,

    val identifier: String,

    val expectedMinRssi: Int? = null
)
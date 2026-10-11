package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


// ============================================================
// ATTENDANCE WI-FI ENTITY
//
// Local Room cache of Wi-Fi access-point configuration
// received from Express.
//
// One attendance location can contain multiple Wi-Fi access
// points.
//
// Example:
//
// Main Office
//    ├── Main Router
//    └── Office Access Point
//
// IMPORTANT:
//
// SSID is informational.
//
// BSSID identifies the actual Wi-Fi access point and will be
// used later when producing Wi-Fi attendance evidence.
//
// Android does NOT create or manage this configuration.
// Configuration originates from React CRM.
// ============================================================

@Entity(
    tableName = "attendance_wifi",

    foreignKeys = [

        ForeignKey(
            entity = AttendanceLocationEntity::class,

            parentColumns = ["id"],

            childColumns = ["attendanceLocationId"],

            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [

        // Required/recommended for the foreign-key column.
        Index(
            value = ["attendanceLocationId"]
        ),

        // Helps Android quickly compare the currently connected
        // access point with locally cached configuration.
        Index(
            value = ["bssid"]
        )
    ]
)
data class AttendanceWifiEntity(

    // Backend Wi-Fi configuration ID.
    //
    // Backend ID is used as the Room primary key.
    @PrimaryKey
    val id: Long,


    // ========================================================
    // PARENT ATTENDANCE LOCATION
    // ========================================================

    val attendanceLocationId: Long,


    // ========================================================
    // WI-FI CONFIGURATION
    // ========================================================

    val name: String,

    // Human-readable network name.
    //
    // This is useful for display/debugging but should NOT be
    // treated as the unique physical access-point identity.
    val ssid: String? = null,

    // Physical access-point BSSID.
    //
    // Example:
    //
    // AA:BB:CC:DD:EE:FF
    //
    // Later we will normalize BSSID values before comparing
    // them so case differences don't cause false mismatches.
    val bssid: String
)
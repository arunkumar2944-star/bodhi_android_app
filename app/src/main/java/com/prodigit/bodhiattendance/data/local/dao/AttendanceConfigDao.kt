package com.prodigit.bodhiattendance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

import com.prodigit.bodhiattendance.data.local.entity.AttendanceBeaconEntity
import com.prodigit.bodhiattendance.data.local.entity.AttendanceLocationEntity
import com.prodigit.bodhiattendance.data.local.entity.AttendanceWifiEntity


// ============================================================
// ATTENDANCE CONFIGURATION DAO
//
// Manages the local Room cache for attendance configuration.
//
// Configuration source:
//
// React CRM
//      ↓
// Express / PostgreSQL
//      ↓
// Android
//      ↓
// Room cache
//
// Android treats this configuration as READ ONLY from the
// business perspective.
//
// The DAO only stores the latest configuration received from
// the backend.
// ============================================================

@Dao
interface AttendanceConfigDao {


    // ========================================================
    // INSERT / UPDATE LOCATIONS
    //
    // Backend IDs are used as primary keys.
    //
    // If the same configuration ID already exists, REPLACE
    // updates the cached copy.
// ========================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertLocations(
        locations: List<AttendanceLocationEntity>
    )


    // ========================================================
    // INSERT / UPDATE BEACONS
    // ========================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertBeacons(
        beacons: List<AttendanceBeaconEntity>
    )


    // ========================================================
    // INSERT / UPDATE WI-FI ACCESS POINTS
    // ========================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertWifiAccessPoints(
        wifiAccessPoints: List<AttendanceWifiEntity>
    )


    // ========================================================
    // READ ALL LOCATIONS
    // ========================================================

    @Query(
        """
        SELECT *
        FROM attendance_locations
        ORDER BY locationName ASC
        """
    )
    suspend fun getLocations():
            List<AttendanceLocationEntity>


    // ========================================================
    // GET ONE LOCATION
    // ========================================================

    @Query(
        """
        SELECT *
        FROM attendance_locations
        WHERE id = :locationId
        LIMIT 1
        """
    )
    suspend fun getLocationById(
        locationId: Long
    ): AttendanceLocationEntity?


    // ========================================================
    // GET BEACONS FOR LOCATION
    // ========================================================

    @Query(
        """
        SELECT *
        FROM attendance_beacons
        WHERE attendanceLocationId = :locationId
        ORDER BY id ASC
        """
    )
    suspend fun getBeaconsForLocation(
        locationId: Long
    ): List<AttendanceBeaconEntity>


    // ========================================================
    // GET WI-FI FOR LOCATION
    // ========================================================

    @Query(
        """
        SELECT *
        FROM attendance_wifi
        WHERE attendanceLocationId = :locationId
        ORDER BY id ASC
        """
    )
    suspend fun getWifiForLocation(
        locationId: Long
    ): List<AttendanceWifiEntity>


    // ========================================================
    // FIND BEACON
    //
    // Later used by the BLE evidence provider.
    // ========================================================

    @Query(
        """
        SELECT *
        FROM attendance_beacons
        WHERE identifier = :identifier
        LIMIT 1
        """
    )
    suspend fun findBeaconByIdentifier(
        identifier: String
    ): AttendanceBeaconEntity?


    // ========================================================
    // FIND WI-FI ACCESS POINT
    //
    // COLLATE NOCASE prevents MAC-address case differences:
    //
    // AA:BB:CC...
    // aa:bb:cc...
    //
    // from producing a false mismatch.
    // ========================================================

    @Query(
        """
        SELECT *
        FROM attendance_wifi
        WHERE bssid = :bssid COLLATE NOCASE
        LIMIT 1
        """
    )
    suspend fun findWifiByBssid(
        bssid: String
    ): AttendanceWifiEntity?


    // ========================================================
    // CLEAR CHILD CONFIGURATION
    //
    // These are intentionally separate from locations.
    //
    // Child rows must be removed before parent locations
    // because of foreign-key relationships.
    // ========================================================

    @Query("DELETE FROM attendance_beacons")
    suspend fun clearBeacons()


    @Query("DELETE FROM attendance_wifi")
    suspend fun clearWifi()


    @Query("DELETE FROM attendance_locations")
    suspend fun clearLocations()


    // ========================================================
    // REPLACE COMPLETE CONFIGURATION
    //
    // CRITICAL:
    //
    // This entire operation runs as ONE Room transaction.
    //
    // Therefore Android will not be left with:
    //
    // new GPS configuration
    // +
    // old BLE configuration
    // +
    // old Wi-Fi configuration
    //
    // if something fails halfway through.
    // ========================================================

    @Transaction
    suspend fun replaceConfiguration(

        locations: List<AttendanceLocationEntity>,

        beacons: List<AttendanceBeaconEntity>,

        wifiAccessPoints: List<AttendanceWifiEntity>

    ) {

        // ----------------------------------------------------
        // Delete children first because they reference
        // attendance_locations through foreign keys.
        // ----------------------------------------------------

        clearBeacons()

        clearWifi()

        clearLocations()


        // ----------------------------------------------------
        // Insert parent rows first.
        // ----------------------------------------------------

        if (locations.isNotEmpty()) {

            insertLocations(
                locations
            )
        }


        // ----------------------------------------------------
        // Then BLE child rows.
        // ----------------------------------------------------

        if (beacons.isNotEmpty()) {

            insertBeacons(
                beacons
            )
        }


        // ----------------------------------------------------
        // Then Wi-Fi child rows.
        // ----------------------------------------------------

        if (wifiAccessPoints.isNotEmpty()) {

            insertWifiAccessPoints(
                wifiAccessPoints
            )
        }
    }
}
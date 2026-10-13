package com.prodigit.bodhiattendance.data.local.repository

import com.prodigit.bodhiattendance.data.local.dao.AttendanceConfigDao

import com.prodigit.bodhiattendance.data.local.entity.AttendanceBeaconEntity
import com.prodigit.bodhiattendance.data.local.entity.AttendanceLocationEntity
import com.prodigit.bodhiattendance.data.local.entity.AttendanceWifiEntity

import com.prodigit.bodhiattendance.domain.model.AttendanceBeaconConfig
import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig
import com.prodigit.bodhiattendance.domain.model.AttendanceWifiConfig


// ============================================================
// ATTENDANCE CONFIG CACHE REPOSITORY
//
// Single access layer between the application and the local
// Room attendance-configuration cache.
//
// Configuration flow:
//
// React CRM
//      ↓
// Express
//      ↓
// AttendanceConfigRepositoryImpl
//      ↓
// AttendanceLocationConfig
//      ↓
// AttendanceConfigCacheRepository
//      ↓
// Room
//
// Android does NOT create attendance configuration.
//
// Room provides a local cached copy so GPS / BLE / Wi-Fi can
// continue using the last successfully downloaded
// configuration during temporary network loss.
// ============================================================

class AttendanceConfigCacheRepository(

    private val attendanceConfigDao: AttendanceConfigDao

) {


    // ========================================================
    // REPLACE COMPLETE CONFIGURATION CACHE
    //
    // Called after Android successfully receives fresh
    // configuration from Express.
    //
    // The DAO performs the actual delete + insert operation
    // inside one Room transaction.
    // ========================================================

    suspend fun replaceConfiguration(

        locations: List<AttendanceLocationConfig>

    ) {


        val cachedAt =
            System.currentTimeMillis()


        // ====================================================
        // LOCATION ENTITIES
        // ====================================================

        val locationEntities =

            locations.map { location ->

                AttendanceLocationEntity(

                    id =
                        location.id,

                    locationName =
                        location.locationName,

                    locationCode =
                        location.locationCode,

                    latitude =
                        location.latitude,

                    longitude =
                        location.longitude,

                    geofenceRadiusMeters =
                        location.geofenceRadiusMeters,

                    maxAccuracyMeters =
                        location.maxAccuracyMeters,

                    wifiRequired = location.wifiRequired,
                    bleRequired = location.bleRequired,
                gracePeriodMinutes = location.gracePeriodMinutes,
                maximumAbsenceMinutes = location.maximumAbsenceMinutes,

                    cachedAt =
                        cachedAt
                )
            }


        // ====================================================
        // BEACON ENTITIES
        // ====================================================

        val beaconEntities =

            locations.flatMap { location ->

                location.beacons.map { beacon ->

                    AttendanceBeaconEntity(

                        id =
                            beacon.id,

                        attendanceLocationId =
                            location.id,

                        name =
                            beacon.name,

                        identifier =
                            beacon.identifier,

                        expectedMinRssi =
                            beacon.expectedMinRssi
                    )
                }
            }


        // ====================================================
        // WI-FI ENTITIES
        // ====================================================

        val wifiEntities =

            locations.flatMap { location ->

                location.wifiAccessPoints.map { wifi ->

                    AttendanceWifiEntity(

                        id =
                            wifi.id,

                        attendanceLocationId =
                            location.id,

                        name =
                            wifi.name,

                        ssid =
                            wifi.ssid,

                        bssid =
                            normalizeBssid(
                                wifi.bssid
                            )
                    )
                }
            }


        // ====================================================
        // ATOMIC ROOM REPLACEMENT
        // ====================================================

        attendanceConfigDao
            .replaceConfiguration(

                locations =
                    locationEntities,

                beacons =
                    beaconEntities,

                wifiAccessPoints =
                    wifiEntities
            )
    }


    // ========================================================
    // GET ALL CACHED CONFIGURATION
    //
    // Reconstructs our domain model from the Room tables.
    //
    // Room:
    //
    // Location
    //    ├── Beacons
    //    └── Wi-Fi
    //
    // becomes:
    //
    // AttendanceLocationConfig
    // ========================================================

    suspend fun getCachedConfiguration():
            List<AttendanceLocationConfig> {


        val locations =
            attendanceConfigDao
                .getLocations()


        return locations.map { location ->


            val beacons =
                attendanceConfigDao
                    .getBeaconsForLocation(
                        location.id
                    )


            val wifiAccessPoints =
                attendanceConfigDao
                    .getWifiForLocation(
                        location.id
                    )


            AttendanceLocationConfig(

                id =
                    location.id,

                locationName =
                    location.locationName,

                locationCode =
                    location.locationCode,

                latitude =
                    location.latitude,

                longitude =
                    location.longitude,

                geofenceRadiusMeters =
                    location.geofenceRadiusMeters,

                maxAccuracyMeters =
                    location.maxAccuracyMeters,

                wifiRequired = location.wifiRequired,
                bleRequired = location.bleRequired,
                gracePeriodMinutes = location.gracePeriodMinutes,
                maximumAbsenceMinutes = location.maximumAbsenceMinutes,


                // =============================================
                // ROOM BEACON -> DOMAIN
                // =============================================

                beacons =
                    beacons.map { beacon ->

                        AttendanceBeaconConfig(

                            id =
                                beacon.id,

                            name =
                                beacon.name,

                            identifier =
                                beacon.identifier,

                            expectedMinRssi =
                                beacon.expectedMinRssi
                        )
                    },


                // =============================================
                // ROOM WI-FI -> DOMAIN
                // =============================================

                wifiAccessPoints =
                    wifiAccessPoints.map { wifi ->

                        AttendanceWifiConfig(

                            id =
                                wifi.id,

                            name =
                                wifi.name,

                            ssid =
                                wifi.ssid,

                            bssid =
                                wifi.bssid
                        )
                    }
            )
        }
    }


    // ========================================================
    // FIND CONFIGURED BEACON
    //
    // Later used by BLE evidence collection.
    // ========================================================

    suspend fun findBeaconByIdentifier(

        identifier: String

    ): AttendanceBeaconEntity? {


        return attendanceConfigDao
            .findBeaconByIdentifier(
                identifier
            )
    }


    // ========================================================
    // FIND CONFIGURED WI-FI
    //
    // Later used by Wi-Fi evidence collection.
    // ========================================================

    suspend fun findWifiByBssid(

        bssid: String

    ): AttendanceWifiEntity? {


        return attendanceConfigDao
            .findWifiByBssid(

                normalizeBssid(
                    bssid
                )
            )
    }


    // ========================================================
    // NORMALIZE BSSID
    //
    // Android/network sources may return:
    //
    // aa:bb:cc:dd:ee:ff
    //
    // while CRM may contain:
    //
    // AA:BB:CC:DD:EE:FF
    //
    // Store/compare using uppercase form.
    // ========================================================

    private fun normalizeBssid(

        bssid: String

    ): String {


        return bssid
            .trim()
            .uppercase()
    }
}
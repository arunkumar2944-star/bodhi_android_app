package com.prodigit.bodhiattendance.data.repository

import com.prodigit.bodhiattendance.data.remote.AttendanceApiService

import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceBeaconDto
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceLocationDto
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceWifiDto

import com.prodigit.bodhiattendance.domain.model.AttendanceBeaconConfig
import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig
import com.prodigit.bodhiattendance.domain.model.AttendanceWifiConfig

import com.prodigit.bodhiattendance.domain.repository.AttendanceConfigRepository

import kotlinx.coroutines.CancellationException

import retrofit2.HttpException


// ============================================================
// ATTENDANCE CONFIGURATION REPOSITORY IMPLEMENTATION
//
// Fetches attendance configuration from Express and converts
// remote DTO objects into domain configuration objects.
//
// React CRM
//      ↓
// Express
//      ↓
// AttendanceApiService
//      ↓
// AttendanceConfigRepositoryImpl
//      ↓
// AttendanceLocationConfig
//
// Android receives this configuration as READ ONLY.
// ============================================================

class AttendanceConfigRepositoryImpl(

    private val api: AttendanceApiService

) : AttendanceConfigRepository {


    // ========================================================
    // GET ACTIVE ATTENDANCE CONFIGURATION
    // ========================================================

    override suspend fun getActiveAttendanceConfig(

        token: String

    ): Result<List<AttendanceLocationConfig>> {


        return try {


            // =================================================
            // CALL EXPRESS
            // =================================================

            val response =
                api.getActiveAttendanceConfig(

                    authorization =
                        "Bearer $token"
                )


            // =================================================
            // HTTP SUCCESS
            // =================================================

            if (response.isSuccessful) {


                val body =
                    response.body()


                if (body == null) {

                    return Result.failure(

                        IllegalStateException(
                            "Empty attendance configuration response"
                        )
                    )
                }


                // ---------------------------------------------
                // Backend returned HTTP success but application
                // response says success = false.
                // ---------------------------------------------

                if (!body.success) {

                    return Result.failure(

                        IllegalStateException(
                            body.message
                                ?: "Unable to load attendance configuration"
                        )
                    )
                }


                // =================================================
                // DTO -> DOMAIN
                // =================================================

                val locations =
                    body.data.map { locationDto ->

                        locationDto.toDomain()
                    }


                Result.success(
                    locations
                )


            } else {


                // =================================================
                // HTTP ERROR
                //
                // Examples:
                //
                // 401 Unauthorized
                // 403 Forbidden
                // 404 Endpoint not available
                // 500 Server error
                // =================================================

                Result.failure(
                    HttpException(response)
                )
            }


        } catch (e: CancellationException) {


            // Coroutine cancellation must never be converted into
            // an ordinary network failure.

            throw e


        } catch (e: Exception) {


            Result.failure(e)
        }
    }


    // ========================================================
    // LOCATION DTO -> DOMAIN
    // ========================================================

    private fun AttendanceLocationDto.toDomain():
            AttendanceLocationConfig {


        return AttendanceLocationConfig(

            id =
                id,

            locationName =
                locationName,

            locationCode =
                locationCode,


            // -------------------------------------------------
            // GPS
            // -------------------------------------------------

            latitude =
                latitude,

            longitude =
                longitude,

            geofenceRadiusMeters =
                geofenceRadiusMeters,

            maxAccuracyMeters =
                maxAccuracyMeters,


            // -------------------------------------------------
            // BLE
            // -------------------------------------------------

            beacons =
                beacons.map { beaconDto ->

                    beaconDto.toDomain()
                },


            // -------------------------------------------------
            // WI-FI
            // -------------------------------------------------

            wifiAccessPoints =
                wifi.map { wifiDto ->

                    wifiDto.toDomain()
                }
        )
    }


    // ========================================================
    // BEACON DTO -> DOMAIN
    // ========================================================

    private fun AttendanceBeaconDto.toDomain():
            AttendanceBeaconConfig {


        return AttendanceBeaconConfig(

            id =
                id,

            name =
                name,

            identifier =
                identifier,

            expectedMinRssi =
                expectedMinRssi
        )
    }


    // ========================================================
    // WI-FI DTO -> DOMAIN
    // ========================================================

    private fun AttendanceWifiDto.toDomain():
            AttendanceWifiConfig {


        return AttendanceWifiConfig(

            id =
                id,

            name =
                name,

            ssid =
                ssid,

            bssid =
                bssid
        )
    }
}
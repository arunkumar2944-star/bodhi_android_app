package com.prodigit.bodhiattendance.core.attendance

import com.prodigit.bodhiattendance.data.local.repository.AttendanceConfigCacheRepository
import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig
import com.prodigit.bodhiattendance.domain.repository.AttendanceConfigRepository

import kotlinx.coroutines.CancellationException


// ============================================================
// ATTENDANCE CONFIG MANAGER
//
// Coordinates:
//
// 1. Express attendance configuration
// 2. Local Room configuration cache
//
// Normal flow:
//
// Express
//    ↓
// Fresh configuration
//    ↓
// Save to Room
//    ↓
// Return configuration
//
// Offline / temporary network failure:
//
// Express fails
//    ↓
// Load last successful configuration from Room
//    ↓
// Return cached configuration
//
// GPS / BLE / Wi-Fi modules should use this manager instead
// of directly accessing Retrofit or Room.
// ============================================================

class AttendanceConfigManager(

    private val remoteRepository: AttendanceConfigRepository,

    private val cacheRepository: AttendanceConfigCacheRepository

) {


    // ========================================================
    // LOAD CONFIGURATION
    //
    // Strategy:
    //
    // NETWORK FIRST
    //      ↓
    // success -> cache -> return fresh config
    //
    // failure
    //      ↓
    // Room cache
    //
    // If both network and cache are unavailable, return the
    // original network failure.
    // ========================================================

    suspend fun loadConfiguration(

        token: String

    ): Result<List<AttendanceLocationConfig>> {


        try {


            // =================================================
            // TRY EXPRESS FIRST
            // =================================================

            val remoteResult =
                remoteRepository
                    .getActiveAttendanceConfig(
                        token = token
                    )


            if (remoteResult.isSuccess) {


                val remoteConfiguration =
                    remoteResult.getOrThrow()


                // =============================================
                // SAVE FRESH CONFIGURATION TO ROOM
                // =============================================

                cacheRepository
                    .replaceConfiguration(
                        locations =
                            remoteConfiguration
                    )


                return Result.success(
                    remoteConfiguration
                )
            }


            // =================================================
            // EXPRESS FAILED
            //
            // Try last successfully cached configuration.
            // =================================================

            val cachedConfiguration =
                cacheRepository
                    .getCachedConfiguration()


            if (cachedConfiguration.isNotEmpty()) {


                return Result.success(
                    cachedConfiguration
                )
            }


            // =================================================
            // NO CACHE
            //
            // Preserve the original network/backend error.
            // =================================================

            return Result.failure(

                remoteResult.exceptionOrNull()
                    ?: IllegalStateException(
                        "Attendance configuration unavailable"
                    )
            )


        } catch (e: CancellationException) {


            // Coroutine cancellation must propagate normally.

            throw e


        } catch (e: Exception) {


            // =================================================
            // Unexpected local/cache error
            //
            // At this point we cannot safely provide a valid
            // configuration to the attendance evidence layer.
            // =================================================

            return Result.failure(e)
        }
    }


    // ========================================================
    // GET CACHED CONFIGURATION ONLY
    //
    // IMPORTANT:
    //
    // Does NOT make a network request.
    //
    // Useful later for:
    //
    // - background evidence collection
    // - BLE scanning
    // - Wi-Fi matching
    // - GPS evaluation
    // ========================================================

    suspend fun getCachedConfiguration():
            List<AttendanceLocationConfig> {


        return cacheRepository
            .getCachedConfiguration()
    }


    // ========================================================
    // FIND LOCATION
    //
    // Convenience lookup using the cached configuration.
    // ========================================================

    suspend fun getCachedLocationById(

        locationId: Long

    ): AttendanceLocationConfig? {


        return cacheRepository
            .getCachedConfiguration()
            .firstOrNull { location ->

                location.id == locationId
            }
    }
}
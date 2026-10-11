package com.prodigit.bodhiattendance.data.local.security

import com.prodigit.bodhiattendance.data.local.entity.TimeAnchorEntity
import com.prodigit.bodhiattendance.data.local.repository.TimeAnchorLocalRepository
import com.prodigit.bodhiattendance.domain.repository.AttendanceRepository

import kotlinx.coroutines.CancellationException


// ============================================================
// TIME ANCHOR MANAGER
//
// Coordinates Android trusted-time setup.
//
// Flow:
//
// BootSessionManager
//      ↓
// bootId + elapsedRealtime
//      ↓
// AttendanceRepository
//      ↓
// Express /time-anchor
//      ↓
// trusted server anchor
//      ↓
// Security validation
//      ↓
//      ├── TimeAnchorStorage
//      │       Current active anchor cache
//      │
//      └── TimeAnchorLocalRepository
//              Durable Room persistence
//
// IMPORTANT:
//
// Android does NOT use the employee-controlled phone wall clock
// as authoritative attendance time.
//
// Express remains the final authority.
//
// Offline attendance events reference a persisted server anchor
// together with their own SystemClock.elapsedRealtime() value.
//
// Historical events, such as delayed CHECK_OUT classification,
// may need an older persisted anchor from the SAME boot session.
// ============================================================

class TimeAnchorManager(

    private val attendanceRepository:
    AttendanceRepository,

    private val bootSessionManager:
    BootSessionManager,

    private val timeAnchorStorage:
    TimeAnchorStorage,

    private val timeAnchorLocalRepository:
    TimeAnchorLocalRepository

) {


    // ========================================================
    // CREATE FRESH TIME ANCHOR
    //
    // Requests a new trusted anchor from Express, validates it,
    // stores it in Room and updates the active-anchor cache.
    // ========================================================

    suspend fun createFreshAnchor(

        token: String,

        deviceId: String

    ): Result<StoredTimeAnchor> {


        return try {


            // =================================================
            // CURRENT ANDROID BOOT SESSION
            // =================================================

            val bootId =
                bootSessionManager
                    .getBootId()


            // =================================================
            // CURRENT MONOTONIC DEVICE TIME
            //
            // SystemClock.elapsedRealtime() cannot be changed by
            // manually changing Android date/time.
            // =================================================

            val elapsedRealtimeMs =
                bootSessionManager
                    .getElapsedRealtimeMs()


            // =================================================
            // REQUEST SERVER ANCHOR
            // =================================================

            val result =
                attendanceRepository
                    .createTimeAnchor(

                        token =
                            token,

                        deviceId =
                            deviceId,

                        elapsedRealtimeMs =
                            elapsedRealtimeMs,

                        bootId =
                            bootId
                    )


            // =================================================
            // NETWORK / REPOSITORY FAILURE
            // =================================================

            if (result.isFailure) {

                return Result.failure(

                    result.exceptionOrNull()
                        ?: IllegalStateException(
                            "Unable to create time anchor"
                        )
                )
            }


            val response =
                result.getOrThrow()


            // =================================================
            // BACKEND BUSINESS FAILURE
            //
            // AttendanceRepositoryImpl can return Result.success
            // containing a backend response whose success=false.
            // =================================================

            if (!response.success) {

                return Result.failure(

                    IllegalStateException(
                        response.message
                    )
                )
            }


            // =================================================
            // REQUIRE ANCHOR DATA
            // =================================================

            val anchorData =
                response.data
                    ?: return Result.failure(

                        IllegalStateException(
                            "Time anchor data missing"
                        )
                    )


            // =================================================
            // SECURITY VALIDATION — BOOT SESSION
            //
            // Express should return the same bootId submitted by
            // Android.
            // =================================================

            if (
                anchorData.bootId !=
                bootId
            ) {

                return Result.failure(

                    IllegalStateException(
                        "Time anchor boot session mismatch"
                    )
                )
            }


            // =================================================
            // SECURITY VALIDATION — ELAPSED REALTIME
            //
            // Express should return the monotonic timestamp that
            // Android supplied when requesting the anchor.
            // =================================================

            if (
                anchorData.elapsedRealtimeMs !=
                elapsedRealtimeMs
            ) {

                return Result.failure(

                    IllegalStateException(
                        "Time anchor elapsed realtime mismatch"
                    )
                )
            }


            // =================================================
            // BASIC SERVER DATA VALIDATION
            // =================================================

            if (
                anchorData
                    .anchorUuid
                    .isBlank()
            ) {

                return Result.failure(

                    IllegalStateException(
                        "Time anchor UUID missing"
                    )
                )
            }


            if (
                anchorData
                    .serverTime
                    .isBlank()
            ) {

                return Result.failure(

                    IllegalStateException(
                        "Time anchor server time missing"
                    )
                )
            }


            // =================================================
            // BUILD ACTIVE TRUSTED ANCHOR
            //
            // Lightweight object used by the application/security
            // layer.
            // =================================================

            val storedAnchor =
                StoredTimeAnchor(

                    anchorUuid =
                        anchorData.anchorUuid,

                    serverTime =
                        anchorData.serverTime,

                    elapsedRealtimeMs =
                        anchorData.elapsedRealtimeMs,

                    bootId =
                        anchorData.bootId
                )


            // =================================================
            // BUILD ROOM ENTITY
            //
            // deviceId belongs to the registered installation.
            //
            // storedAtEpochMs is local persistence metadata only.
            //
            // It must NEVER be used as authoritative attendance
            // time.
            // =================================================

            val roomAnchor =
                TimeAnchorEntity(

                    anchorUuid =
                        anchorData.anchorUuid,

                    deviceId =
                        deviceId,

                    serverTime =
                        anchorData.serverTime,

                    elapsedRealtimeMs =
                        anchorData.elapsedRealtimeMs,

                    bootId =
                        anchorData.bootId,

                    storedAtEpochMs =
                        System.currentTimeMillis()
                )


            // =================================================
            // SAVE DURABLE TRUSTED EVIDENCE TO ROOM FIRST
            //
            // If this fails, the fresh anchor must not become the
            // active application anchor.
            // =================================================

            timeAnchorLocalRepository
                .saveTimeAnchor(
                    roomAnchor
                )


            // =================================================
            // SAVE CURRENT ACTIVE ANCHOR CACHE
            //
            // Room remains the durable historical/evidence store.
            // =================================================

            timeAnchorStorage
                .saveAnchor(

                    anchorUuid =
                        storedAnchor.anchorUuid,

                    serverTime =
                        storedAnchor.serverTime,

                    elapsedRealtimeMs =
                        storedAnchor.elapsedRealtimeMs,

                    bootId =
                        storedAnchor.bootId
                )


            // =================================================
            // SUCCESS
            // =================================================

            Result.success(
                storedAnchor
            )


        } catch (
            e: CancellationException
        ) {


            // Coroutine cancellation must propagate normally.

            throw e


        } catch (
            e: Exception
        ) {


            Result.failure(
                e
            )
        }
    }


    // ========================================================
    // GET VALID CURRENT ANCHOR
    //
    // Fast synchronous lookup using TimeAnchorStorage.
    //
    // The anchor must:
    //
    // - belong to the current Android boot
    // - have a monotonic value not newer than current elapsed time
    // ========================================================

    fun getCurrentAnchor():
            StoredTimeAnchor? {


        val storedAnchor =
            timeAnchorStorage
                .getAnchor()
                ?: return null


        val currentBootId =
            bootSessionManager
                .getBootId()


        // =====================================================
        // BOOT SESSION VALIDATION
        // =====================================================

        if (
            storedAnchor.bootId !=
            currentBootId
        ) {

            timeAnchorStorage
                .clearAnchor()

            return null
        }


        // =====================================================
        // MONOTONIC SANITY CHECK
        // =====================================================

        val currentElapsedRealtime =
            bootSessionManager
                .getElapsedRealtimeMs()


        if (
            currentElapsedRealtime <
            storedAnchor.elapsedRealtimeMs
        ) {

            timeAnchorStorage
                .clearAnchor()

            return null
        }


        return storedAnchor
    }


    // ========================================================
    // GET LATEST VALID ANCHOR FROM ROOM
    //
    // Durable recovery path when the lightweight active-anchor
    // cache is unavailable.
    //
    // deviceId must match the registered Android installation.
    // ========================================================

    suspend fun getLatestRoomAnchor(

        deviceId: String

    ): TimeAnchorEntity? {


        if (
            deviceId.isBlank()
        ) {

            return null
        }


        val currentBootId =
            bootSessionManager
                .getBootId()


        val anchor =
            timeAnchorLocalRepository
                .getLatestValidTimeAnchor(

                    deviceId =
                        deviceId,

                    bootId =
                        currentBootId
                )
                ?: return null


        // =====================================================
        // MONOTONIC SANITY CHECK
        // =====================================================

        val currentElapsedRealtime =
            bootSessionManager
                .getElapsedRealtimeMs()


        if (
            currentElapsedRealtime <
            anchor.elapsedRealtimeMs
        ) {

            return null
        }


        return anchor
    }


    // ========================================================
    // GET BEST TRUSTED ANCHOR FOR HISTORICAL EVENT
    //
    // Used when an attendance event is created/classified later
    // than the moment it actually became effective.
    //
    // Main production example:
    //
    // 18:00  Anchor A
    // 18:25  Employee leaves office
    // 19:00  Anchor B
    // 19:55  CHECK_OUT classification
    //
    // Effective CHECK_OUT time remains:
    //
    // 18:25
    //
    // Therefore:
    //
    // Anchor A ✅
    // Anchor B ❌
    //
    // Selection requirements:
    //
    // - same device
    // - same boot
    // - anchor.elapsedRealtimeMs <= eventElapsedRealtimeMs
    // - newest matching anchor
    //
    // IMPORTANT:
    //
    // This method does NOT create a new anchor.
    //
    // It selects already trusted/persisted evidence.
    // ========================================================

    suspend fun getBestAnchorForEvent(

        deviceId: String,

        bootId: String,

        eventElapsedRealtimeMs: Long

    ): TimeAnchorEntity? {


        // =====================================================
        // BASIC INPUT VALIDATION
        // =====================================================

        if (
            deviceId.isBlank()
        ) {

            return null
        }


        if (
            bootId.isBlank()
        ) {

            return null
        }


        if (
            eventElapsedRealtimeMs < 0L
        ) {

            return null
        }


        // =====================================================
        // CURRENT BOOT VALIDATION
        //
        // We do not allow a caller to request arbitrary elapsed
        // time calculations for another boot session.
        // =====================================================

        val currentBootId =
            bootSessionManager
                .getBootId()


        if (
            bootId !=
            currentBootId
        ) {

            return null
        }


        // =====================================================
        // EVENT CANNOT BE IN THE FUTURE
        // =====================================================

        val currentElapsedRealtime =
            bootSessionManager
                .getElapsedRealtimeMs()


        if (
            eventElapsedRealtimeMs >
            currentElapsedRealtime
        ) {

            return null
        }


        // =====================================================
        // FIND NEWEST TRUSTED ANCHOR AT OR BEFORE EVENT
        // =====================================================

        val anchor =
            timeAnchorLocalRepository
                .getBestAnchorForEvent(

                    deviceId =
                        deviceId,

                    bootId =
                        bootId,

                    eventElapsedRealtimeMs =
                        eventElapsedRealtimeMs
                )
                ?: return null


        // =====================================================
        // FINAL SAFETY VALIDATION
        //
        // DAO query already enforces these rules, but security
        // sensitive trusted-time logic should verify again.
        // =====================================================

        if (
            anchor.deviceId !=
            deviceId
        ) {

            return null
        }


        if (
            anchor.bootId !=
            bootId
        ) {

            return null
        }


        if (
            anchor.elapsedRealtimeMs >
            eventElapsedRealtimeMs
        ) {

            return null
        }


        return anchor
    }


    // ========================================================
    // GET ANCHOR BY UUID
    //
    // Required by offline attendance synchronization.
    //
    // AttendanceEventEntity.timeAnchorId
    //              ↓
    // TimeAnchorEntity.anchorUuid
    // ========================================================

    suspend fun getAnchorByUuid(

        anchorUuid: String

    ): TimeAnchorEntity? {


        if (
            anchorUuid.isBlank()
        ) {

            return null
        }


        return timeAnchorLocalRepository
            .getTimeAnchorByUuid(
                anchorUuid =
                    anchorUuid
            )
    }


    // ========================================================
    // CLEAR ACTIVE TRUSTED TIME
    //
    // Clears only the lightweight active-anchor cache.
    //
    // IMPORTANT:
    //
    // Room anchors are intentionally preserved because:
    //
    // - unsynchronized events may reference them
    // - historical checkout may need them
    // - audit/reconciliation may need them
    // ========================================================

    fun clearAnchor() {

        timeAnchorStorage
            .clearAnchor()
    }



    suspend fun ensureCurrentAnchor(
        token: String,
        deviceId: String
    ): Result<StoredTimeAnchor> {

        return try {

            // =====================================================
            // 1. TRY ACTIVE CACHE FIRST
            // =====================================================

            val currentAnchor =
                getCurrentAnchor()

            if (currentAnchor != null) {

                return Result.success(
                    currentAnchor
                )
            }


            // =====================================================
            // 2. TRY ROOM RECOVERY
            // =====================================================

            val roomAnchor =
                getLatestRoomAnchor(
                    deviceId = deviceId
                )

            if (roomAnchor != null) {

                val restoredAnchor =
                    StoredTimeAnchor(

                        anchorUuid =
                            roomAnchor.anchorUuid,

                        serverTime =
                            roomAnchor.serverTime,

                        elapsedRealtimeMs =
                            roomAnchor.elapsedRealtimeMs,

                        bootId =
                            roomAnchor.bootId
                    )


                // Restore lightweight active cache
                timeAnchorStorage
                    .saveAnchor(

                        anchorUuid =
                            restoredAnchor.anchorUuid,

                        serverTime =
                            restoredAnchor.serverTime,

                        elapsedRealtimeMs =
                            restoredAnchor.elapsedRealtimeMs,

                        bootId =
                            restoredAnchor.bootId
                    )


                return Result.success(
                    restoredAnchor
                )
            }


            // =====================================================
            // 3. NO VALID ANCHOR FOUND
            //
            // CREATE FRESH SERVER ANCHOR
            // =====================================================

            createFreshAnchor(
                token = token,
                deviceId = deviceId
            )


        } catch (
            e: CancellationException
        ) {

            throw e

        } catch (
            e: Exception
        ) {

            Result.failure(
                e
            )
        }
    }
}
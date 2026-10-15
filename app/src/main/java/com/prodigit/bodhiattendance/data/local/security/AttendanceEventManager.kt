package com.prodigit.bodhiattendance.data.local.security

import com.prodigit.bodhiattendance.data.local.entity.AttendanceEventEntity
import com.prodigit.bodhiattendance.data.local.repository.OfflineAttendanceRepository

import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventRequest

import com.prodigit.bodhiattendance.domain.repository.AttendanceRepository

import kotlinx.coroutines.CancellationException

import java.util.UUID


// ============================================================
// ATTENDANCE EVENT MANAGER
//
// Creates trusted attendance events using an OFFLINE-FIRST flow.
//
// IMPORTANT:
//
// Event creation and remote synchronization are separate.
//
// Production flow:
//
// Attendance Engine
//      ↓
// valid transition only
//      ↓
// AttendanceEventManager
//      ↓
// trusted event time
//      ↓
// correct trusted anchor
//      ↓
// Room PENDING FIRST
//      ↓
// immediate Express attempt
//
// Possible outcomes:
//
// 1. NOT STORED
//
//    No durable attendance event exists.
//    Engine must NOT advance state.
//
// 2. STORED + SYNCED
//
//    Durable local event exists.
//    Express accepted it.
//    Engine may advance state.
//
// 3. STORED + FAILED_RETRYABLE
//
//    Durable local event exists.
//    Network/server transport failed.
//    Engine may advance state.
//    WorkManager retries SAME event later.
//
// 4. STORED + REJECTED
//
//    Durable local event exists,
//    but Express rejected the event.
//
//    Engine must NOT blindly advance.
//    Server/local state reconciliation is required.
//
// ============================================================
//
// TRUSTED TIME
//
// Phone wall-clock is NOT authoritative.
//
// Express reconstructs trusted event time:
//
// anchor.serverTime
// +
// (eventElapsedRealtimeMs - anchor.elapsedRealtimeMs)
//
// NORMAL EVENT:
//
// eventElapsedRealtimeMsOverride == null
//
//      ↓
//
// event time = current elapsedRealtime()
//
//      ↓
//
// current trusted anchor is used.
//
// If the lightweight current-anchor cache is unavailable,
// the newest valid Room anchor from the same boot is used.
//
// ============================================================
//
// HISTORICAL EVENT:
//
// eventElapsedRealtimeMsOverride != null
//
//      ↓
//
// effective event time = supplied monotonic timestamp
//
//      ↓
//
// select newest Room anchor:
//
// anchor.elapsedRealtimeMs <= eventElapsedRealtimeMs
//
// Example:
//
// 18:00 Anchor A
// 18:25 employee exits
// 19:00 Anchor B
// 19:55 CHECK_OUT classified
//
// Effective CHECK_OUT = 18:25
//
// Correct:
//
// Anchor A ✅
//
// Incorrect:
//
// Anchor B ❌
// ============================================================

class AttendanceEventManager(

    private val attendanceRepository:
    AttendanceRepository,

    private val offlineAttendanceRepository:
    OfflineAttendanceRepository,

    private val bootSessionManager:
    BootSessionManager,

    private val timeAnchorManager:
    TimeAnchorManager,

    private val offlineSession: com.prodigit.bodhiattendance.core.monitoring.OfflineWorkSessionStore? = null

) {


    // ========================================================
    // RECORD ATTENDANCE EVENT
    // ========================================================

    suspend fun recordEvent(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String,

        eventType: String,


        // ====================================================
        // OPTIONAL EFFECTIVE MONOTONIC EVENT TIME
        //
        // null:
        //      normal event occurring now
        //
        // non-null:
        //      event became effective earlier
        //
        // Main use:
        //
        // delayed automatic CHECK_OUT classification.
        // ====================================================

        eventElapsedRealtimeMsOverride: Long? = null,
        arrivalProof: com.prodigit.bodhiattendance.data.remote.dto.attendance.WifiArrivalProof? = null,
        attendanceReason: String? = null,


        // ====================================================
        // GPS EVIDENCE
        // ====================================================

        latitude: Double? = null,

        longitude: Double? = null,

        accuracyMeters: Double? = null,

        insideGeofence: Boolean = false,


        // ====================================================
        // BLE EVIDENCE
        //
        // Supported architecturally.
        //
        // Currently optional / disabled for V1 attendance
        // decision.
        // ====================================================

        beaconDetected: Boolean = false,

        beaconId: String? = null,

        beaconRssi: Int? = null,


        // ====================================================
        // WI-FI EVIDENCE
        // ====================================================

        wifiVerified: Boolean = false,

        wifiBssid: String? = null

    ): AttendanceEventRecordResult {


        // ====================================================
        // VALIDATE BASIC IDENTITY
        //
        // Nothing has been persisted yet.
        //
        // Therefore all failures here are NotStored.
        // ====================================================

        if (userId <= 0) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Valid logged-in user ID is required"
                    )
            )
        }


        if (companyId <= 0) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Valid company ID is required"
                    )
            )
        }


        if (deviceId.isBlank()) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Valid registered device ID is required"
                    )
            )
        }


        // ====================================================
        // CURRENT BOOT SESSION
        //
        // elapsedRealtime values can only be compared inside
        // the same Android boot.
        // ====================================================

        val currentBootId =
            try {

                bootSessionManager
                    .getBootId()

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                return AttendanceEventRecordResult.NotStored(
                    error = e
                )
            }


        if (currentBootId.isBlank()) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Current Android boot session is unavailable"
                    )
            )
        }


        // ====================================================
        // CURRENT MONOTONIC TIME
        // ====================================================

        val currentElapsedRealtimeMs =
            try {

                bootSessionManager
                    .getElapsedRealtimeMs()

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                return AttendanceEventRecordResult.NotStored(
                    error = e
                )
            }


        // ====================================================
        // RESOLVE EFFECTIVE EVENT MONOTONIC TIME
        //
        // Normal:
        //
        //      current elapsedRealtime
        //
        // Historical:
        //
        //      supplied original effective elapsedRealtime
        // ====================================================

        val eventElapsedRealtimeMs =
            eventElapsedRealtimeMsOverride
                ?: currentElapsedRealtimeMs


        // ====================================================
        // EVENT MONOTONIC VALIDATION
        // ====================================================

        if (
            eventElapsedRealtimeMs < 0L
        ) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Attendance event monotonic time is invalid"
                    )
            )
        }


        // ====================================================
        // EVENT CANNOT BE IN FUTURE
        //
        // Historical event may be older than now.
        //
        // It must never be newer than current elapsedRealtime.
        // ====================================================

        if (
            eventElapsedRealtimeMs >
            currentElapsedRealtimeMs
        ) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Attendance event monotonic time is in the future"
                    )
            )
        }


        // ====================================================
        // RESOLVE TRUSTED TIME ANCHOR
        //
        // IMPORTANT:
        //
        // We must choose the anchor AFTER resolving the effective
        // event time.
        //
        // Historical event:
        //
        //      best anchor at/before event time
        //
        // Normal current event:
        //
        //      active current anchor
        //
        //      fallback:
        //
        //      newest valid Room anchor
        // ====================================================

        val selectedAnchorUuid: String

        val selectedAnchorBootId: String

        val selectedAnchorElapsedRealtimeMs: Long


        if (
            eventElapsedRealtimeMsOverride != null
        ) {


            // =================================================
            // HISTORICAL EVENT
            //
            // Example:
            //
            // employee exit occurred earlier but CHECK_OUT is
            // classified after the configured maximum absence.
            //
            // We MUST NOT use a later anchor.
            // =================================================

            val historicalAnchor =
                try {

                    timeAnchorManager
                        .getBestAnchorForEvent(

                            deviceId =
                                deviceId,

                            bootId =
                                currentBootId,

                            eventElapsedRealtimeMs =
                                eventElapsedRealtimeMs
                        )

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    return AttendanceEventRecordResult.NotStored(
                        error = e
                    )
                }


            if (
                historicalAnchor == null
            ) {

                return AttendanceEventRecordResult.NotStored(

                    error =
                        IllegalStateException(
                            "No trusted time anchor exists at or before the effective event time"
                        )
                )
            }


            selectedAnchorUuid =
                historicalAnchor.anchorUuid


            selectedAnchorBootId =
                historicalAnchor.bootId


            selectedAnchorElapsedRealtimeMs =
                historicalAnchor.elapsedRealtimeMs


        } else {


            // =================================================
            // NORMAL CURRENT-TIME EVENT
            //
            // First use lightweight active anchor cache.
            // =================================================

            val currentAnchor =
                try {

                    timeAnchorManager
                        .getCurrentAnchor()

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    return AttendanceEventRecordResult.NotStored(
                        error = e
                    )
                }


            if (
                currentAnchor != null
            ) {


                selectedAnchorUuid =
                    currentAnchor.anchorUuid


                selectedAnchorBootId =
                    currentAnchor.bootId


                selectedAnchorElapsedRealtimeMs =
                    currentAnchor.elapsedRealtimeMs


            } else {


                // =============================================
                // CACHE RECOVERY
                //
                // TimeAnchorStorage may be unavailable/cleared
                // while Room still contains durable valid
                // trusted evidence.
                // =============================================

                val roomAnchor =
                    try {

                        timeAnchorManager
                            .getLatestRoomAnchor(
                                deviceId =
                                    deviceId
                            )

                    } catch (e: CancellationException) {

                        throw e

                    } catch (e: Exception) {

                        return AttendanceEventRecordResult.NotStored(
                            error = e
                        )
                    }


                if (
                    roomAnchor == null
                ) {

                    return AttendanceEventRecordResult.NotStored(

                        error =
                            IllegalStateException(
                                "No valid trusted time anchor available"
                            )
                    )
                }


                selectedAnchorUuid =
                    roomAnchor.anchorUuid


                selectedAnchorBootId =
                    roomAnchor.bootId


                selectedAnchorElapsedRealtimeMs =
                    roomAnchor.elapsedRealtimeMs
            }
        }


        // ====================================================
        // VERIFY SELECTED ANCHOR BOOT SESSION
        // ====================================================

        if (
            selectedAnchorBootId !=
            currentBootId
        ) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Trusted time anchor belongs to a different boot session"
                    )
            )
        }


        // ====================================================
        // VERIFY ANCHOR PRECEDES EVENT
        //
        // Required relationship:
        //
        // anchor.elapsedRealtimeMs
        //          <=
        // eventElapsedRealtimeMs
        //          <=
        // currentElapsedRealtimeMs
        // ====================================================

        if (
            selectedAnchorElapsedRealtimeMs >
            eventElapsedRealtimeMs
        ) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Attendance event monotonic time is before trusted time anchor"
                    )
            )
        }


        // ====================================================
        // GENERATE EVENT UUID ONCE
        //
        // SAME UUID is used for:
        //
        // Room
        // initial Express request
        // future retry
        //
        // This provides idempotency.
        // ====================================================

        val eventUuid =
            UUID.randomUUID()
                .toString()


        // ====================================================
        // LOCAL WALL-CLOCK METADATA
        //
        // NOT authoritative attendance time.
        // ====================================================

        val localWallClockTime =
            System.currentTimeMillis()


        // ====================================================
        // BUILD LOCAL EVENT
        //
        // AttendanceStateMachine / AttendanceEngine must already
        // validate the transition before calling this manager.
        //
        // Invalid normal transitions should never intentionally
        // enter Room.
        // ====================================================

        val storedAnchor = timeAnchorManager.getAnchorByUuid(selectedAnchorUuid)
        val trustedEventTime = storedAnchor?.let { java.time.Instant.parse(it.serverTime)
            .plusMillis(eventElapsedRealtimeMs - it.elapsedRealtimeMs).toEpochMilli() }
        val arrivalAnchor = arrivalProof?.let { timeAnchorManager.getAnchorByUuid(it.anchorUuid) }
        val effectiveArrivalTime = if (arrivalProof != null && arrivalAnchor != null)
            java.time.Instant.parse(arrivalAnchor.serverTime).plusMillis(arrivalProof.observedElapsedMs - arrivalAnchor.elapsedRealtimeMs).toEpochMilli() else null
        val deferred = com.prodigit.bodhiattendance.core.monitoring.OfflineWorkSessionStore.isOffline()
        val offlinePermit = if (deferred && (effectiveArrivalTime ?: trustedEventTime) != null)
            offlineSession?.permitForEvent(userId, companyId, deviceId, currentBootId,
                requireNotNull(effectiveArrivalTime ?: trustedEventTime)) else null
        if (deferred && offlinePermit == null) return AttendanceEventRecordResult.NotStored(
            IllegalStateException("Offline authorization expired or does not cover this event"))
        val localEvent =
            AttendanceEventEntity(

                offlinePermit = offlinePermit,
                arrivalProofJson = arrivalProof?.let { com.google.gson.Gson().toJson(it) },
                effectiveTimeEpochMs = effectiveArrivalTime ?: trustedEventTime,
                reason = if (effectiveArrivalTime != null) "Office Wi-Fi arrival confirmed by GPS" else attendanceReason,
                eventUuid =
                    eventUuid,

                userId =
                    userId,

                companyId =
                    companyId,

                deviceId =
                    deviceId,

                eventType =
                    eventType,


                // --------------------------------------------
                // UNTRUSTED LOCAL WALL-CLOCK METADATA
                // --------------------------------------------

                eventTime =
                    localWallClockTime,


                // --------------------------------------------
                // TRUSTED TIME EVIDENCE
                // --------------------------------------------

                timeAnchorId =
                    selectedAnchorUuid,

                eventElapsedRealtimeMs =
                    eventElapsedRealtimeMs,


                // --------------------------------------------
                // GPS EVIDENCE
                // --------------------------------------------

                latitude =
                    latitude,

                longitude =
                    longitude,

                accuracyMeters =
                    accuracyMeters
                        ?.toFloat(),

                insideGeofence =
                    insideGeofence,


                // --------------------------------------------
                // BLE EVIDENCE
                // --------------------------------------------

                beaconDetected =
                    beaconDetected,

                beaconId =
                    beaconId,

                beaconRssi =
                    beaconRssi,


                // --------------------------------------------
                // WI-FI EVIDENCE
                // --------------------------------------------

                wifiVerified =
                    wifiVerified,

                wifiBssid =
                    wifiBssid,


                // --------------------------------------------
                // SYNC STATE
                // --------------------------------------------

                syncStatus =
                    "PENDING",

                syncAttempts =
                    0,

                lastSyncAttemptAt =
                    null,

                syncError =
                    null,

                serverAttendanceId =
                    null,

                syncedAt =
                    null,


                // --------------------------------------------
                // LOCAL METADATA
                // --------------------------------------------

                createdAt =
                    localWallClockTime
            )


        // ====================================================
        // ROOM-FIRST PERSISTENCE
        //
        // BEFORE:
        //
        // NotStored
        //
        // AFTER successful insertion:
        //
        // real durable attendance event exists.
        // ====================================================

        val localRowId =
            try {

                offlineAttendanceRepository
                    .saveEvent(
                        localEvent
                    )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                return AttendanceEventRecordResult.NotStored(
                    error = e
                )
            }


        // ====================================================
        // DUPLICATE UUID SAFETY
        // ====================================================

        if (
            localRowId == -1L
        ) {

            return AttendanceEventRecordResult.NotStored(

                error =
                    IllegalStateException(
                        "Attendance event UUID already exists locally"
                    )
            )
        }


        // ====================================================
        // FROM THIS POINT FORWARD:
        //
        // EVENT EXISTS DURABLY IN ROOM.
        // ====================================================


        if (deferred) {
            offlineSession?.scheduleSync()
            return AttendanceEventRecordResult.StoredForRetry(eventUuid, "Saved offline; pending CRM validation")
        }

        // ====================================================
        // MARK SYNCING
        // ====================================================

        try {

            offlineAttendanceRepository
                .markSyncing(

                    eventUuid =
                        eventUuid,

                    attemptTime =
                        System.currentTimeMillis()
                )

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {


            val errorMessage =
                e.message
                    ?: "Unable to begin attendance synchronization"


            safeMarkRetryable(

                eventUuid =
                    eventUuid,

                error =
                    errorMessage
            )


            return AttendanceEventRecordResult.StoredForRetry(

                eventUuid =
                    eventUuid,

                error =
                    errorMessage
            )
        }


        // ====================================================
        // BUILD EXPRESS REQUEST
        //
        // Uses EXACTLY the same trusted evidence persisted to
        // Room.
        // ====================================================

        val request =
            AttendanceEventRequest(

                offlinePermit = offlinePermit,
                arrivalProof = arrivalProof,
                eventUuid =
                    eventUuid,

                deviceId =
                    deviceId,

                eventType =
                    eventType,


                // --------------------------------------------
                // PHONE WALL CLOCK
                //
                // Never authoritative.
                // --------------------------------------------

                deviceEventTime =
                    null,


                // --------------------------------------------
                // TRUSTED TIME
                // --------------------------------------------

                timeAnchorUuid =
                    selectedAnchorUuid,

                eventElapsedRealtimeMs =
                    eventElapsedRealtimeMs,

                bootId =
                    currentBootId,


                // --------------------------------------------
                // GPS
                // --------------------------------------------

                latitude =
                    latitude,

                longitude =
                    longitude,

                accuracyMeters =
                    accuracyMeters,

                insideGeofence =
                    insideGeofence,


                // --------------------------------------------
                // BLE
                // --------------------------------------------

                beaconDetected =
                    beaconDetected,

                beaconId =
                    beaconId,

                beaconRssi =
                    beaconRssi,


                // --------------------------------------------
                // WI-FI
                // --------------------------------------------

                wifiVerified =
                    wifiVerified,

                wifiBssid =
                    wifiBssid
            )


        // ====================================================
        // IMMEDIATE EXPRESS ATTEMPT
        // ====================================================

        val remoteResult =
            try {

                attendanceRepository
                    .recordAttendanceEvent(

                        token =
                            token,

                        request =
                            request
                    )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {


                val errorMessage =
                    e.message
                        ?: "Attendance synchronization failed"


                safeMarkRetryable(

                    eventUuid =
                        eventUuid,

                    error =
                        errorMessage
                )


                return AttendanceEventRecordResult.StoredForRetry(

                    eventUuid =
                        eventUuid,

                    error =
                        errorMessage
                )
            }


        // ====================================================
        // TRANSPORT / NETWORK FAILURE
        //
        // Event already exists in Room.
        //
        // Therefore:
        //
        // FAILED_RETRYABLE
        //
        // not:
        //
        // NotStored
        // ====================================================

        if (
            remoteResult.isFailure
        ) {


            val throwable =
                remoteResult
                    .exceptionOrNull()


            val errorMessage =
                throwable?.message
                    ?: "Attendance synchronization failed"


            safeMarkRetryable(

                eventUuid =
                    eventUuid,

                error =
                    errorMessage
            )


            return AttendanceEventRecordResult.StoredForRetry(

                eventUuid =
                    eventUuid,

                error =
                    errorMessage
            )
        }


        // ====================================================
        // VALID EXPRESS RESPONSE
        // ====================================================

        val response =
            try {

                remoteResult
                    .getOrThrow()

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {


                val errorMessage =
                    e.message
                        ?: "Unable to read attendance response"


                safeMarkRetryable(

                    eventUuid =
                        eventUuid,

                    error =
                        errorMessage
                )


                return AttendanceEventRecordResult.StoredForRetry(

                    eventUuid =
                        eventUuid,

                    error =
                        errorMessage
                )
            }


        // ====================================================
        // EXPRESS ACCEPTED EVENT
        // ====================================================

        if (
            response.success
        ) {


            try {

                offlineAttendanceRepository
                    .markSynced(

                        eventUuid =
                            eventUuid,

                        serverAttendanceId =
                            response
                                .attendanceEventId
                                ?.toLongOrNull(),

                        syncedAt =
                            System.currentTimeMillis()
                    )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {


                // Express has already accepted the UUID.
                //
                // Later retry/reconciliation can safely reuse it.

                val errorMessage =
                    e.message
                        ?: "Server accepted attendance but local sync status update failed"


                safeMarkRetryable(

                    eventUuid =
                        eventUuid,

                    error =
                        errorMessage
                )


                return AttendanceEventRecordResult.StoredForRetry(

                    eventUuid =
                        eventUuid,

                    error =
                        errorMessage
                )
            }


            return AttendanceEventRecordResult.StoredAndSynced(

                eventUuid =
                    eventUuid,

                response =
                    response
            )
        }


        // ====================================================
        // EXPRESS BUSINESS REJECTION
        //
        // Express received and evaluated the event.
        //
        // REJECTED is a safety net for:
        //
        // - local/server state disagreement
        // - stale state
        // - races
        // - administrative corrections
        // - invalid/missing evidence
        //
        // Normal invalid transitions should already have been
        // blocked by AttendanceStateMachine before this manager.
        // ====================================================

        val rejectionError =
            buildString {


                if (
                    !response
                        .code
                        .isNullOrBlank()
                ) {

                    append(
                        response.code
                    )
                }


                if (
                    !response
                        .message
                        .isNullOrBlank()
                ) {

                    if (
                        isNotEmpty()
                    ) {

                        append(": ")
                    }


                    append(
                        response.message
                    )
                }


                if (
                    isEmpty()
                ) {

                    append(
                        "Attendance event rejected by backend"
                    )
                }
            }


        safeMarkRejected(

            eventUuid =
                eventUuid,

            error =
                rejectionError
        )


        return AttendanceEventRecordResult.StoredRejected(

            eventUuid =
                eventUuid,

            response =
                response,

            error =
                rejectionError
        )
    }


    // ========================================================
    // SAFE MARK RETRYABLE
    //
    // Event already exists durably in Room.
    //
    // A bookkeeping failure must not cause the system to claim
    // the attendance event never existed.
    // ========================================================

    private suspend fun safeMarkRetryable(

        eventUuid: String,

        error: String

    ) {


        try {

            offlineAttendanceRepository
                .markFailedRetryable(

                    eventUuid =
                        eventUuid,

                    error =
                        error
                )

        } catch (e: CancellationException) {

            throw e

        } catch (_: Exception) {


            // Intentionally preserve original outcome.
            //
            // Event remains durable and later stale-sync recovery
            // or reconciliation can inspect it.
        }
    }


    // ========================================================
    // SAFE MARK REJECTED
    // ========================================================

    private suspend fun safeMarkRejected(

        eventUuid: String,

        error: String

    ) {


        try {

            offlineAttendanceRepository
                .markRejected(

                    eventUuid =
                        eventUuid,

                    error =
                        error
                )

        } catch (e: CancellationException) {

            throw e

        } catch (_: Exception) {


            // Preserve backend rejection outcome even if local
            // bookkeeping temporarily fails.
        }
    }
}
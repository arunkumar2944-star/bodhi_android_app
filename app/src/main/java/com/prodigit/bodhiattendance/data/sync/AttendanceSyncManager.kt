package com.prodigit.bodhiattendance.data.sync

import com.prodigit.bodhiattendance.data.local.entity.AttendanceEventEntity
import com.prodigit.bodhiattendance.data.local.repository.OfflineAttendanceRepository
import com.prodigit.bodhiattendance.data.local.repository.TimeAnchorLocalRepository

import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceEventRequest

import com.prodigit.bodhiattendance.domain.repository.AttendanceRepository

import kotlinx.coroutines.CancellationException


// ============================================================
// ATTENDANCE SYNC MANAGER
//
// Synchronizes attendance events already stored in Room.
//
// IMPORTANT:
//
// This manager NEVER creates a new attendance event.
//
// It MUST reuse:
//
// - existing eventUuid
// - existing timeAnchorId
// - existing eventElapsedRealtimeMs
// - existing GPS evidence
// - existing Wi-Fi evidence
// - existing BLE evidence
//
// This preserves idempotency and trusted offline event time.
//
// Flow:
//
// Room PENDING / FAILED_RETRYABLE
//        ↓
// AttendanceSyncManager
//        ↓
// Validate original trusted-time evidence
//        ↓
// Load referenced TimeAnchor
//        ↓
// Rebuild AttendanceEventRequest
//        ↓
// SAME eventUuid
//        ↓
// Express
//        ↓
// SYNCED / FAILED_RETRYABLE / REJECTED
// ============================================================

class AttendanceSyncManager(

    private val attendanceRepository:
    AttendanceRepository,

    private val offlineAttendanceRepository:
    OfflineAttendanceRepository,

    private val timeAnchorLocalRepository:
    TimeAnchorLocalRepository

) {


    // ========================================================
    // SYNC RESULT
    //
    // failed includes both:
    //
    // - retryable failures
    // - rejected events encountered during this sync pass
    //
    // Later System Health can distinguish them using the
    // individual Room syncStatus values.
    // ========================================================

    data class SyncResult(

        val attempted: Int,

        val synced: Int,

        val failed: Int
    )


    // ========================================================
    // SYNC UNSYNCED EVENTS
    //
    // Processes oldest retryable events first.
    //
    // The DAO should return:
    //
    // PENDING
    // FAILED_RETRYABLE
    //
    // REJECTED events must NOT enter this queue.
    //
    // IMPORTANT:
    //
    // token belongs to the authenticated session.
    //
    // We do NOT generate:
    //
    // - new event UUID
    // - new time anchor
    // - new elapsed realtime
    //
    // The original event evidence is reused.
    // ========================================================

    suspend fun syncUnsyncedEvents(

        token: String,

        batchSize: Int = 20,
        userId: Int? = null,
        deviceId: String? = null

    ): SyncResult {


        var attempted = 0

        var synced = 0

        var failed = 0


        // ====================================================
        // RECOVER INTERRUPTED SYNC OPERATIONS
        //
        // Example:
        //
        // SYNCING
        //    ↓
        // process killed
        //
        // After the stale timeout, DAO converts the event to:
        //
        // FAILED_RETRYABLE
        //
        // because we do not know whether the previous transport
        // operation completed.
        //
        // Idempotency is protected by eventUuid.
        // ====================================================

        val staleBefore =
            System.currentTimeMillis() -
                    STALE_SYNC_TIMEOUT_MS


        offlineAttendanceRepository
            .recoverStuckSyncingEvents(
                staleBefore = staleBefore
            )


        // ====================================================
        // LOAD RETRYABLE UNSYNCED EVENTS
        // ====================================================

        val events = if (userId != null && deviceId != null)
            offlineAttendanceRepository.getScopedUnsyncedBatch(userId, deviceId, batchSize)
        else offlineAttendanceRepository.getUnsyncedBatch(limit = batchSize)


        // ====================================================
        // PROCESS IN ORIGINAL CREATION ORDER
        //
        // DAO returns:
        //
        // ORDER BY createdAt ASC
        // ====================================================

        for (event in events) {


            attempted++


            val eventSynced =
                try {

                    syncSingleEvent(
                        token = token,
                        event = event
                    )

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {


                    // =========================================
                    // UNEXPECTED OPERATIONAL FAILURE
                    //
                    // We cannot prove that the attendance event
                    // itself is invalid.
                    //
                    // Keep it retryable.
                    // =========================================

                    offlineAttendanceRepository
                        .markFailedRetryable(

                            eventUuid =
                                event.eventUuid,

                            error =
                                e.message
                                    ?: "Unexpected synchronization failure"
                        )


                    false
                }


            if (eventSynced) {

                synced++

            } else {

                failed++
            }
        }


        return SyncResult(

            attempted =
                attempted,

            synced =
                synced,

            failed =
                failed
        )
    }


    // ========================================================
    // SYNC SINGLE EXISTING EVENT
    // ========================================================

    private suspend fun syncSingleEvent(

        token: String,

        event: AttendanceEventEntity

    ): Boolean {


        // ====================================================
        // VALIDATE TRUSTED TIME REFERENCES
        // ====================================================

        val timeAnchorId =
            event.timeAnchorId


        if (timeAnchorId.isNullOrBlank()) {


            // ================================================
            // PERMANENT LOCAL EVIDENCE FAILURE
            //
            // Retrying cannot recreate the original trusted
            // time anchor without changing historical evidence.
            // ================================================

            offlineAttendanceRepository
                .markRejected(

                    eventUuid =
                        event.eventUuid,

                    error =
                        "Attendance event has no trusted time anchor"
                )


            return false
        }


        val eventElapsedRealtimeMs =
            event.eventElapsedRealtimeMs


        if (eventElapsedRealtimeMs == null) {


            // ================================================
            // PERMANENT LOCAL EVIDENCE FAILURE
            // ================================================

            offlineAttendanceRepository
                .markRejected(

                    eventUuid =
                        event.eventUuid,

                    error =
                        "Attendance event has no monotonic event time"
                )


            return false
        }


        // ====================================================
        // LOAD ORIGINAL TIME ANCHOR
        //
        // IMPORTANT:
        //
        // We use the anchor referenced by THIS event.
        //
        // We must NOT substitute:
        //
        // getLatestValidTimeAnchor()
        //
        // because a newer anchor represents a different
        // trusted-time reference.
        // ====================================================

        val anchor =
            timeAnchorLocalRepository
                .getTimeAnchorByUuid(
                    anchorUuid = timeAnchorId
                )


        if (anchor == null) {


            // ================================================
            // ORIGINAL TRUSTED EVIDENCE IS MISSING
            //
            // Do not generate or substitute another anchor.
            // Doing so would alter the historical evidence.
            // ================================================

            offlineAttendanceRepository
                .markRejected(

                    eventUuid =
                        event.eventUuid,

                    error =
                        "Referenced trusted time anchor not found"
                )


            return false
        }


        // ====================================================
        // SECURITY CONSISTENCY CHECK
        //
        // The persisted anchor must belong to the same
        // registered installation as the attendance event.
        // ====================================================

        if (anchor.deviceId != event.deviceId) {

            offlineAttendanceRepository
                .markRejected(

                    eventUuid =
                        event.eventUuid,

                    error =
                        "Attendance event device does not match time anchor device"
                )


            return false
        }


        // ====================================================
        // MONOTONIC CONSISTENCY CHECK
        //
        // The attendance event cannot have happened before its
        // referenced trusted anchor in the same monotonic time
        // domain.
        // ====================================================

        if (
            eventElapsedRealtimeMs <
            anchor.elapsedRealtimeMs
        ) {

            offlineAttendanceRepository
                .markRejected(

                    eventUuid =
                        event.eventUuid,

                    error =
                        "Attendance event monotonic time is before trusted anchor"
                )


            return false
        }


        // ====================================================
        // MARK SYNCING
        //
        // This increments syncAttempts.
        // ====================================================

        offlineAttendanceRepository
            .markSyncing(

                eventUuid =
                    event.eventUuid,

                attemptTime =
                    System.currentTimeMillis()
            )


        // ====================================================
        // REBUILD REQUEST FROM ORIGINAL ROOM EVENT
        //
        // NOTHING temporal is regenerated here.
        //
        // The exact historical event evidence is reused.
        // ====================================================

        val request =
            AttendanceEventRequest(


                // --------------------------------------------
                // ORIGINAL EVENT UUID
                // --------------------------------------------

                offlinePermit = event.offlinePermit,
                arrivalProof = event.arrivalProofJson?.let { com.google.gson.Gson().fromJson(it,
                    com.prodigit.bodhiattendance.data.remote.dto.attendance.WifiArrivalProof::class.java) },
                eventUuid =
                    event.eventUuid,


                // --------------------------------------------
                // ORIGINAL DEVICE
                // --------------------------------------------

                deviceId =
                    event.deviceId,


                // --------------------------------------------
                // ORIGINAL EVENT TYPE
                // --------------------------------------------

                eventType =
                    event.eventType,


                // --------------------------------------------
                // PHONE WALL CLOCK
                //
                // Not authoritative.
                //
                // Keep null for server processing.
                // --------------------------------------------

                deviceEventTime =
                    null,


                // --------------------------------------------
                // ORIGINAL TRUSTED TIME EVIDENCE
                // --------------------------------------------

                timeAnchorUuid =
                    anchor.anchorUuid,

                eventElapsedRealtimeMs =
                    eventElapsedRealtimeMs,

                bootId =
                    anchor.bootId,


                // --------------------------------------------
                // ORIGINAL GPS EVIDENCE
                // --------------------------------------------

                latitude =
                    event.latitude,

                longitude =
                    event.longitude,

                accuracyMeters =
                    event.accuracyMeters
                        ?.toDouble(),

                insideGeofence =
                    event.insideGeofence,


                // --------------------------------------------
                // ORIGINAL BLE EVIDENCE
                // --------------------------------------------

                beaconDetected =
                    event.beaconDetected,

                beaconId =
                    event.beaconId,

                beaconRssi =
                    event.beaconRssi,


                // --------------------------------------------
                // ORIGINAL WI-FI EVIDENCE
                // --------------------------------------------

                wifiVerified =
                    event.wifiVerified,

                wifiBssid =
                    event.wifiBssid
            )


        // ====================================================
        // SEND EXISTING EVENT TO EXPRESS
        // ====================================================

        val result =
            attendanceRepository
                .recordAttendanceEvent(

                    token =
                        token,

                    request =
                        request
                )


        // ====================================================
        // NETWORK / SERIALIZATION FAILURE
        //
        // Express may not have received the request.
        //
        // The event itself remains valid, so preserve the same
        // UUID and evidence and retry later.
        // ====================================================

        if (result.isFailure) {

            val error =
                result.exceptionOrNull()


            offlineAttendanceRepository
                .markFailedRetryable(

                    eventUuid =
                        event.eventUuid,

                    error =
                        error?.message
                            ?: "Attendance synchronization failed"
                )


            return false
        }


        val response =
            result.getOrThrow()


        // ====================================================
        // EXPRESS ACCEPTED EVENT
        // ====================================================

        if (response.success) {


            offlineAttendanceRepository
                .markSynced(

                    eventUuid =
                        event.eventUuid,

                    serverAttendanceId =
                        response.attendanceEventId
                            ?.toLongOrNull(),

                    syncedAt =
                        System.currentTimeMillis()
                )


            return true
        }


        // ====================================================
        // EXPRESS BUSINESS REJECTION
        //
        // Express successfully received and evaluated the
        // attendance event but rejected it.
        //
        // Examples:
        //
        // INVALID_ATTENDANCE_TRANSITION
        // INVALID_TIME_ANCHOR
        // DEVICE_DISABLED
        // DEVICE_NOT_REGISTERED
        //
        // Retrying the exact same immutable event normally
        // produces the same result.
        //
        // Therefore:
        //
        // REJECTED
        //
        // and remove it from the automatic retry queue.
        // ====================================================

        val errorMessage =
            buildString {

                if (!response.code.isNullOrBlank()) {

                    append(
                        response.code
                    )
                }


                if (!response.message.isNullOrBlank()) {

                    if (isNotEmpty()) {
                        append(": ")
                    }

                    append(
                        response.message
                    )
                }


                if (isEmpty()) {

                    append(
                        "Attendance event rejected by backend"
                    )
                }
            }


        offlineAttendanceRepository
            .markRejected(

                eventUuid =
                    event.eventUuid,

                error =
                    errorMessage
            )


        return false
    }


    companion object {


        // ====================================================
        // STALE SYNC TIMEOUT
        //
        // Local operational timeout only.
        //
        // NOT attendance time.
        // ====================================================

        private const val STALE_SYNC_TIMEOUT_MS =
            10 * 60 * 1000L
    }
}
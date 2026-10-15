package com.prodigit.bodhiattendance.data.local.repository

import com.prodigit.bodhiattendance.data.local.dao.AttendanceEventDao
import com.prodigit.bodhiattendance.data.local.entity.AttendanceEventEntity

import kotlinx.coroutines.flow.Flow


// ============================================================
// OFFLINE ATTENDANCE REPOSITORY
//
// Single access layer between the application and the local
// attendance Room database.
//
// Used by:
//
// - Attendance Engine
// - Attendance Sync Manager
// - WorkManager
// - System Health
// - Attendance History
//
// UI / ViewModels should NOT directly access AttendanceEventDao.
// ============================================================

class OfflineAttendanceRepository(

    private val attendanceEventDao: AttendanceEventDao,
    private val diagnostics: com.prodigit.bodhiattendance.core.monitoring.AttendanceDiagnostics? = null

) {


    // ========================================================
    // SAVE ATTENDANCE EVENT
    //
    // Returns:
    //
    // > 0 = successfully inserted
    // -1  = duplicate eventUuid ignored
    // ========================================================

    suspend fun saveEvent(
        event: AttendanceEventEntity
    ): Long {

        val inserted = attendanceEventDao.insertEvent(event)
        if (inserted != -1L) diagnostics?.record("EVENT_SAVED", "uuid=${event.eventUuid}; type=${event.eventType}; phoneTime=${event.eventTime}; effectiveTime=${event.effectiveTimeEpochMs}; reason=${event.reason}; elapsedMs=${event.eventElapsedRealtimeMs}; anchor=${event.timeAnchorId}")
        return inserted
    }


    // ========================================================
    // GET EVENT BY UUID
    // ========================================================

    suspend fun getEventByUuid(
        eventUuid: String
    ): AttendanceEventEntity? {

        return attendanceEventDao.getEventByUuid(
            eventUuid
        )
    }


    // ========================================================
    // GET PENDING EVENTS
    //
    // Events waiting for their first synchronization attempt.
    // ========================================================

    suspend fun getPendingEvents():
            List<AttendanceEventEntity> {

        return attendanceEventDao
            .getPendingEvents()
    }


    // ========================================================
    // GET RETRYABLE UNSYNCED EVENTS
    //
    // Returns:
    //
    // PENDING
    // FAILED_RETRYABLE
    //
    // Does NOT return:
    //
    // SYNCING
    // SYNCED
    // REJECTED
    // ========================================================

    suspend fun getUnsyncedEvents():
            List<AttendanceEventEntity> {

        return attendanceEventDao
            .getUnsyncedEvents()
    }


    // ========================================================
    // GET RETRYABLE UNSYNCED BATCH
    //
    // Used by AttendanceSyncManager / WorkManager.
    //
    // REJECTED events are intentionally excluded by the DAO.
    // ========================================================

    suspend fun getScopedUnsyncedBatch(userId: Int, deviceId: String, limit: Int): List<AttendanceEventEntity> =
        attendanceEventDao.getScopedUnsyncedBatch(userId, deviceId, limit)

    suspend fun getUnsyncedBatch(
        limit: Int
    ): List<AttendanceEventEntity> {

        return attendanceEventDao
            .getUnsyncedBatch(
                limit = limit
            )
    }


    // ========================================================
    // MARK SYNCING
    //
    // Called immediately before attempting to send the event
    // to Express.
    //
    // The DAO also:
    //
    // - increments syncAttempts
    // - stores lastSyncAttemptAt
    // - clears previous temporary error
    // ========================================================

    suspend fun markSyncing(
        eventUuid: String,
        attemptTime: Long
    ) {

        attendanceEventDao.markSyncing(
            eventUuid = eventUuid,
            attemptTime = attemptTime
        )
        diagnostics?.record("EVENT_SYNCING", "uuid=$eventUuid")
    }


    // ========================================================
    // MARK SYNCED
    //
    // Called only after Express confirms successful processing.
    //
    // Successfully synchronized events remain in Room according
    // to our local retention policy and are NOT deleted here.
    // ========================================================

    suspend fun markSynced(
        eventUuid: String,
        serverAttendanceId: Long?,
        syncedAt: Long
    ) {

        attendanceEventDao.markSynced(
            eventUuid = eventUuid,
            serverAttendanceId = serverAttendanceId,
            syncedAt = syncedAt
        )
        diagnostics?.record("EVENT_SYNCED", "uuid=$eventUuid")
    }


    // ========================================================
    // MARK RETRYABLE FAILURE
    //
    // Used when the same attendance event should be attempted
    // again later.
    //
    // Examples:
    //
    // - no internet
    // - timeout
    // - connection reset
    // - temporary backend/server failure
    //
    // FAILED_RETRYABLE events remain eligible for automatic
    // synchronization.
    // ========================================================

    suspend fun markFailedRetryable(
        eventUuid: String,
        error: String?
    ) {

        attendanceEventDao.markFailedRetryable(
            eventUuid = eventUuid,
            error = error
        )
        diagnostics?.record("EVENT_RETRY", "uuid=$eventUuid")
    }


    // ========================================================
    // MARK REJECTED
    //
    // Used when Express has rejected the attendance event for
    // a permanent/business-rule reason.
    //
    // Examples:
    //
    // INVALID_ATTENDANCE_TRANSITION
    // INVALID_TIME_ANCHOR
    // DEVICE_DISABLED
    // DEVICE_NOT_REGISTERED
    //
    // REJECTED events are retained for audit/debugging but must
    // NOT be automatically retried.
    // ========================================================

    suspend fun markRejected(
        eventUuid: String,
        error: String?
    ) {

        attendanceEventDao.markRejected(
            eventUuid = eventUuid,
            error = error
        )
        diagnostics?.record("EVENT_REJECTED", "uuid=$eventUuid")
    }


    // ========================================================
    // RECOVER STUCK SYNCING EVENTS
    //
    // Example:
    //
    // SYNCING
    //    ↓
    // API request begins
    //    ↓
    // Android process/app is killed
    //
    // On a later synchronization pass, stale SYNCING events are
    // converted to FAILED_RETRYABLE by the DAO.
    //
    // The same eventUuid will then be reused.
    // ========================================================

    suspend fun recoverStuckSyncingEvents(
        staleBefore: Long
    ): Int {

        return attendanceEventDao
            .recoverStuckSyncingEvents(
                staleBefore = staleBefore
            )
    }


    // ========================================================
    // OBSERVE ACTIVE UNSYNCED COUNT
    //
    // Includes:
    //
    // PENDING
    // FAILED_RETRYABLE
    // SYNCING
    //
    // REJECTED is not considered waiting for synchronization.
    // ========================================================

    fun observeUnsyncedCount():
            Flow<Int> {

        return attendanceEventDao
            .observeUnsyncedCount()
    }


    // ========================================================
    // GET ACTIVE UNSYNCED COUNT ONCE
    //
    // Used later by System Health/manual refresh.
    // ========================================================

    suspend fun getUnsyncedCount():
            Int {

        return attendanceEventDao
            .getUnsyncedCount()
    }


    // ========================================================
    // OBSERVE RETRYABLE FAILURE COUNT
    // ========================================================

    fun observeFailedCount():
            Flow<Int> {

        return attendanceEventDao
            .observeFailedCount()
    }


    // ========================================================
    // OBSERVE REJECTED COUNT
    //
    // Allows System Health/Event Log to distinguish permanent
    // backend rejection from temporary synchronization failure.
    // ========================================================

    fun observeRejectedCount():
            Flow<Int> {

        return attendanceEventDao
            .observeRejectedCount()
    }


    // ========================================================
    // OBSERVE LOCAL ATTENDANCE HISTORY
    //
    // Includes all local attendance records regardless of their
    // synchronization state.
    // ========================================================

    fun observeAllEvents():
            Flow<List<AttendanceEventEntity>> {

        return attendanceEventDao
            .observeAllEvents()
    }

    // ========================================================
// GET LATEST LOCAL EVENT FOR USER + DEVICE
//
// Used by startup consistency/recovery logic.
//
// IMPORTANT:
//
// Ordering is based on Room insertion order:
//
// localId DESC
//
// NOT:
//
// eventTime
//
// because phone wall-clock time is not authoritative.
// ========================================================

    suspend fun getLatestEventForUserDevice(
        userId: Int,
        deviceId: String
    ): AttendanceEventEntity? {

        return attendanceEventDao
            .getLatestEventForUserDevice(
                userId = userId,
                deviceId = deviceId
            )
    }
}
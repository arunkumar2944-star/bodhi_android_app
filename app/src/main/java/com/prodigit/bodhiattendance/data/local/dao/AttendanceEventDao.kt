package com.prodigit.bodhiattendance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import com.prodigit.bodhiattendance.data.local.entity.AttendanceEventEntity

import kotlinx.coroutines.flow.Flow


@Dao
interface AttendanceEventDao {
    /** Only aggregate attendance data for the current registered user/device. */
    @Query("SELECT COUNT(*) FROM attendance_events WHERE userId=:userId AND deviceId=:deviceId AND eventType='BREAK_START'")
    suspend fun countLocalBreakStarts(userId: Int, deviceId: String): Int

    @Query("SELECT COUNT(*) FROM attendance_events WHERE userId=:userId AND deviceId=:deviceId AND eventType='BREAK_END'")
    suspend fun countLocalBreakEnds(userId: Int, deviceId: String): Int

    @Query("SELECT COUNT(*) FROM attendance_events WHERE userId=:userId AND deviceId=:deviceId AND syncStatus IN ('PENDING','FAILED_RETRYABLE','SYNCING')")
    suspend fun countUnsettledEvents(userId: Int, deviceId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM attendance_events WHERE userId=:userId AND deviceId=:deviceId AND syncStatus IN ('PENDING','FAILED_RETRYABLE','SYNCING'))")
    suspend fun hasScopedUnsettledEvents(userId: Int, deviceId: String): Boolean

    @Query("SELECT * FROM attendance_events WHERE userId=:userId AND deviceId=:deviceId AND syncStatus IN ('PENDING','FAILED_RETRYABLE') ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getScopedUnsyncedBatch(userId: Int, deviceId: String, limit: Int): List<AttendanceEventEntity>



    // ============================================================
    // INSERT ATTENDANCE EVENT
    //
    // eventUuid has a UNIQUE index.
    //
    // IGNORE prevents duplicate insertion when the same eventUuid
    // is processed more than once.
    //
    // Return:
    //
    // > 0 = inserted local row ID
    // -1  = duplicate eventUuid ignored
    // ============================================================

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertEvent(
        event: AttendanceEventEntity
    ): Long


    // ============================================================
    // UPDATE COMPLETE EVENT
    // ============================================================

    @Update
    suspend fun updateEvent(
        event: AttendanceEventEntity
    )


    // ============================================================
    // GET EVENT BY UUID
    // ============================================================

    @Query(
        """
        SELECT *
        FROM attendance_events
        WHERE eventUuid = :eventUuid
        LIMIT 1
        """
    )
    suspend fun getEventByUuid(
        eventUuid: String
    ): AttendanceEventEntity?


    // ============================================================
    // GET PENDING EVENTS
    //
    // Event exists locally but has not yet completed a
    // synchronization attempt.
    // ============================================================

    @Query(
        """
        SELECT *
        FROM attendance_events
        WHERE syncStatus = 'PENDING'
        ORDER BY createdAt ASC
        """
    )
    suspend fun getPendingEvents():
            List<AttendanceEventEntity>


    // ============================================================
    // GET RETRYABLE UNSYNCED EVENTS
    //
    // Only events that are allowed to be submitted again:
    //
    // PENDING
    // FAILED_RETRYABLE
    //
    // REJECTED is intentionally excluded.
    //
    // SYNCING is also excluded because another synchronization
    // attempt may currently own that event.
    // ============================================================

    @Query(
        """
        SELECT *
        FROM attendance_events
        WHERE syncStatus IN (
            'PENDING',
            'FAILED_RETRYABLE'
        )
        ORDER BY createdAt ASC
        """
    )
    suspend fun getUnsyncedEvents():
            List<AttendanceEventEntity>


    // ============================================================
    // GET RETRYABLE UNSYNCED BATCH
    //
    // Used by AttendanceSyncManager / WorkManager.
    //
    // Oldest attendance events are processed first.
    //
    // IMPORTANT:
    //
    // REJECTED events must NEVER be automatically submitted again.
    // ============================================================

    @Query(
        """
        SELECT *
        FROM attendance_events
        WHERE syncStatus IN (
            'PENDING',
            'FAILED_RETRYABLE'
        )
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getUnsyncedBatch(
        limit: Int
    ): List<AttendanceEventEntity>


    // ============================================================
    // MARK EVENT AS SYNCING
    //
    // Every synchronization attempt:
    //
    // - changes status to SYNCING
    // - increments attempt count
    // - records attempt time
    // - clears previous temporary error
    // ============================================================

    @Query(
        """
        UPDATE attendance_events
        SET
            syncStatus = 'SYNCING',
            syncAttempts = syncAttempts + 1,
            lastSyncAttemptAt = :attemptTime,
            syncError = NULL
        WHERE eventUuid = :eventUuid
        """
    )
    suspend fun markSyncing(
        eventUuid: String,
        attemptTime: Long
    )


    // ============================================================
    // MARK EVENT AS SYNCED
    //
    // Called only after Express confirms successful processing.
    //
    // The event stays in Room.
    //
    // According to our approved retention policy, successfully
    // synchronized events will later be removed by periodic cleanup
    // only after the retention period has expired.
    // ============================================================

    @Query(
        """
        UPDATE attendance_events
        SET
            syncStatus = 'SYNCED',
            serverAttendanceId = :serverAttendanceId,
            syncedAt = :syncedAt,
            syncError = NULL
        WHERE eventUuid = :eventUuid
        """
    )
    suspend fun markSynced(
        eventUuid: String,
        serverAttendanceId: Long?,
        syncedAt: Long
    )


    // ============================================================
    // MARK EVENT AS RETRYABLE FAILURE
    //
    // Used when synchronization failed because of a temporary
    // condition.
    //
    // Examples:
    //
    // - no network
    // - connection reset
    // - timeout
    // - temporary server failure
    //
    // These events ARE eligible for automatic retry.
    // ============================================================

    @Query(
        """
        UPDATE attendance_events
        SET
            syncStatus = 'FAILED_RETRYABLE',
            syncError = :error
        WHERE eventUuid = :eventUuid
        """
    )
    suspend fun markFailedRetryable(
        eventUuid: String,
        error: String?
    )


    // ============================================================
    // MARK EVENT AS REJECTED
    //
    // Used when Express successfully receives the event but rejects
    // it because retrying the exact same event will not fix it.
    //
    // Examples:
    //
    // INVALID_ATTENDANCE_TRANSITION
    // INVALID_TIME_ANCHOR
    // DEVICE_DISABLED
    // DEVICE_NOT_REGISTERED
    //
    // REJECTED events remain locally for audit/debugging but are
    // excluded from automatic synchronization.
    // ============================================================

    @Query(
        """
        UPDATE attendance_events
        SET
            syncStatus = 'REJECTED',
            syncError = :error
        WHERE eventUuid = :eventUuid
        """
    )
    suspend fun markRejected(
        eventUuid: String,
        error: String?
    )


    // ============================================================
    // RECOVER STUCK SYNCING EVENTS
    //
    // Example:
    //
    // event -> SYNCING
    //       -> API call begins
    //       -> Android process is killed
    //
    // When synchronization later starts again, stale SYNCING
    // records become FAILED_RETRYABLE.
    //
    // This is retryable because we do not know whether the previous
    // request completed.
    //
    // eventUuid idempotency protects against duplicate processing.
    // ============================================================

    @Query(
        """
        UPDATE attendance_events
        SET
            syncStatus = 'FAILED_RETRYABLE',
            syncError = 'Synchronization interrupted'
        WHERE
            syncStatus = 'SYNCING'
            AND (
                lastSyncAttemptAt IS NULL
                OR lastSyncAttemptAt < :staleBefore
            )
        """
    )
    suspend fun recoverStuckSyncingEvents(
        staleBefore: Long
    ): Int


    // ============================================================
    // OBSERVE ACTIVE UNSYNCED COUNT
    //
    // Used by System Health.
    //
    // Includes:
    //
    // PENDING
    // FAILED_RETRYABLE
    // SYNCING
    //
    // REJECTED is intentionally NOT counted as waiting for sync.
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM attendance_events
        WHERE syncStatus IN (
            'PENDING',
            'FAILED_RETRYABLE',
            'SYNCING'
        )
        """
    )
    fun observeUnsyncedCount():
            Flow<Int>


    // ============================================================
    // GET ACTIVE UNSYNCED COUNT ONCE
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM attendance_events
        WHERE syncStatus IN (
            'PENDING',
            'FAILED_RETRYABLE',
            'SYNCING'
        )
        """
    )
    suspend fun getUnsyncedCount():
            Int


    // ============================================================
    // OBSERVE RETRYABLE FAILURE COUNT
    //
    // Used later by System Health.
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM attendance_events
        WHERE syncStatus = 'FAILED_RETRYABLE'
        """
    )
    fun observeFailedCount():
            Flow<Int>


    // ============================================================
    // OBSERVE REJECTED COUNT
    //
    // Separate from retryable failures.
    //
    // System Health / Event Log can later show these as attendance
    // events requiring review rather than events waiting for network
    // synchronization.
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM attendance_events
        WHERE syncStatus = 'REJECTED'
        """
    )
    fun observeRejectedCount():
            Flow<Int>


    // ============================================================
    // OBSERVE LOCAL ATTENDANCE HISTORY
    //
    // Includes all local statuses:
    //
    // PENDING
    // SYNCING
    // FAILED_RETRYABLE
    // REJECTED
    // SYNCED
    //
    // Successfully synchronized records will later follow our
    // approved 30-day local retention policy.
    // ============================================================

    @Query(
        """
        SELECT *
        FROM attendance_events
        ORDER BY createdAt DESC
        """
    )
    fun observeAllEvents():
            Flow<List<AttendanceEventEntity>>


    @Query(
        """
    SELECT *
    FROM attendance_events
    WHERE userId = :userId
      AND deviceId = :deviceId
      AND syncStatus <> 'REJECTED'
    ORDER BY localId DESC
    LIMIT 1
    """
    )
    suspend fun getLatestEventForUserDevice(
        userId: Int,
        deviceId: String
    ): AttendanceEventEntity?
}



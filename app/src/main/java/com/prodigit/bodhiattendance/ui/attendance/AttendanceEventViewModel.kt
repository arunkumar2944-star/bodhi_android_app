package com.prodigit.bodhiattendance.ui.attendance

import android.app.Application
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.SessionManager

import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase

import com.prodigit.bodhiattendance.data.local.repository.OfflineAttendanceRepository
import com.prodigit.bodhiattendance.data.local.repository.TimeAnchorLocalRepository

import com.prodigit.bodhiattendance.data.local.security.AttendanceEventManager
import com.prodigit.bodhiattendance.data.local.security.AttendanceEventRecordResult
import com.prodigit.bodhiattendance.data.local.security.BootSessionManager
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorManager
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorStorage

import com.prodigit.bodhiattendance.data.repository.AttendanceRepositoryImpl

import com.prodigit.bodhiattendance.data.sync.AttendanceSyncManager

import com.prodigit.bodhiattendance.domain.repository.AttendanceRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


// ============================================================
// ATTENDANCE EVENT VIEW MODEL
//
// TEMPORARY development/integration ViewModel.
//
// This currently allows us to manually verify:
//
// - Room-first attendance creation
// - trusted time
// - event UUID idempotency
// - FAILED_RETRYABLE
// - REJECTED
// - manual offline synchronization
//
// IMPORTANT:
//
// Final production attendance events will NOT be generated from
// Dashboard test buttons.
//
// They will be generated automatically by AttendanceEngine.
//
// Current test evidence:
//
// GPS       = not connected yet
// Wi-Fi     = not connected yet
// BLE       = optional / disabled in V1
//
// Therefore test values below deliberately use false/null.
//
// ============================================================

class AttendanceEventViewModel(
    application: Application
) : AndroidViewModel(application) {


    // ========================================================
    // APPLICATION CONTEXT
    // ========================================================

    private val appContext =
        application.applicationContext


    // ========================================================
    // SESSION
    // ========================================================

    private val sessionManager =
        SessionManager(
            appContext
        )


    // ========================================================
    // DEVICE ID
    // ========================================================

    private val deviceIdManager =
        DeviceIdManager(
            appContext
        )


    // ========================================================
    // REMOTE ATTENDANCE REPOSITORY
    // ========================================================

    private val attendanceRepository:
            AttendanceRepository =
        AttendanceRepositoryImpl(
            RetrofitClient.attendanceApi
        )


    // ========================================================
    // ROOM DATABASE
    // ========================================================

    private val attendanceDatabase =
        AttendanceDatabase.getInstance(
            appContext
        )


    // ========================================================
    // OFFLINE ATTENDANCE REPOSITORY
    //
    // Handles:
    //
    // attendance_events
    //
    // including:
    //
    // PENDING
    // SYNCING
    // FAILED_RETRYABLE
    // REJECTED
    // SYNCED
    // ========================================================

    private val offlineAttendanceRepository =
        OfflineAttendanceRepository(

            attendanceEventDao =
                attendanceDatabase
                    .attendanceEventDao()
        )


    // ========================================================
    // LOCAL TIME ANCHOR REPOSITORY
    //
    // Provides durable Room access to:
    //
    // time_anchors
    // ========================================================

    private val timeAnchorLocalRepository =
        TimeAnchorLocalRepository(

            timeAnchorDao =
                attendanceDatabase
                    .timeAnchorDao()
        )


    // ========================================================
    // BOOT SESSION
    // ========================================================

    private val bootSessionManager =
        BootSessionManager(
            appContext
        )


    // ========================================================
    // CURRENT TIME ANCHOR CACHE
    // ========================================================

    private val timeAnchorStorage =
        TimeAnchorStorage(
            appContext
        )


    // ========================================================
    // TIME ANCHOR MANAGER
    //
    // Coordinates:
    //
    // Express
    // +
    // BootSessionManager
    // +
    // active anchor cache
    // +
    // durable Room time_anchors
    // ========================================================

    private val timeAnchorManager =
        TimeAnchorManager(

            attendanceRepository =
                attendanceRepository,

            bootSessionManager =
                bootSessionManager,

            timeAnchorStorage =
                timeAnchorStorage,

            timeAnchorLocalRepository =
                timeAnchorLocalRepository
        )


    // ========================================================
    // ATTENDANCE EVENT MANAGER
    //
    // Creates NEW Room-first attendance events.
    // ========================================================

    private val attendanceEventManager =
        AttendanceEventManager(

            attendanceRepository =
                attendanceRepository,

            offlineAttendanceRepository =
                offlineAttendanceRepository,

            bootSessionManager =
                bootSessionManager,

            timeAnchorManager =
                timeAnchorManager
        )


    // ========================================================
    // ATTENDANCE SYNC MANAGER
    //
    // Retries EXISTING Room events.
    //
    // IMPORTANT:
    //
    // It does NOT create another event UUID.
    //
    // It reuses:
    //
    // - original eventUuid
    // - original timeAnchorId
    // - original eventElapsedRealtimeMs
    // - original evidence
    // ========================================================

    private val attendanceSyncManager =
        AttendanceSyncManager(

            attendanceRepository =
                attendanceRepository,

            offlineAttendanceRepository =
                offlineAttendanceRepository,

            timeAnchorLocalRepository =
                timeAnchorLocalRepository
        )


    // ============================================================
    // TEMPORARY ATTENDANCE EVENT TEST
    //
    // Development only.
    //
    // Final production:
    //
    // AttendancePresenceDecisionEngine
    //          ↓
    // AttendanceEngine
    //          ↓
    // AttendanceEventManager
    //
    // No employee test buttons.
    // ============================================================

    fun testAttendanceEvent(
        eventType: String
    ) {


        // ========================================================
        // TEST EVENT TYPE VALIDATION
        // ========================================================

        val allowedTestEvents =
            setOf(
                "CHECK_IN",
                "BREAK_START",
                "BREAK_END",
                "CHECK_OUT"
            )


        if (
            eventType !in
            allowedTestEvents
        ) {

            Log.e(
                TAG_ATTENDANCE_EVENT,
                "Unsupported test event type: $eventType"
            )

            return
        }


        // ========================================================
        // JWT
        // ========================================================

        val token =
            sessionManager
                .getToken()


        if (
            token.isNullOrBlank()
        ) {

            Log.e(
                TAG_ATTENDANCE_EVENT,
                "JWT session not available"
            )

            return
        }


        // ========================================================
        // REGISTERED INSTALLATION DEVICE ID
        // ========================================================

        val deviceId =
            deviceIdManager
                .getOrCreateDeviceId()


        // ========================================================
        // LOGGED-IN EMPLOYEE
        // ========================================================

        val userId =
            sessionManager
                .getUserId()


        val companyId =
            sessionManager
                .getCompanyId()


        if (
            userId <= 0
        ) {

            Log.e(
                TAG_ATTENDANCE_EVENT,
                "Invalid logged-in user ID"
            )

            return
        }


        if (
            companyId <= 0
        ) {

            Log.e(
                TAG_ATTENDANCE_EVENT,
                "Invalid company ID"
            )

            return
        }


        // ========================================================
        // CREATE TEST EVENT
        // ========================================================

        viewModelScope.launch {


            Log.d(
                TAG_ATTENDANCE_EVENT,
                "Starting $eventType integration test"
            )


            Log.d(
                TAG_ATTENDANCE_EVENT,
                "Device ID: $deviceId"
            )


            val result =
                attendanceEventManager
                    .recordEvent(

                        token =
                            token,

                        userId =
                            userId,

                        companyId =
                            companyId,

                        deviceId =
                            deviceId,

                        eventType =
                            eventType,


                        // ========================================
                        // EFFECTIVE MONOTONIC EVENT TIME
                        //
                        // null for this temporary manual test.
                        //
                        // Production AttendanceEngine may supply
                        // an historical monotonic time for the
                        // delayed CHECK_OUT classification.
                        // ========================================

                        eventElapsedRealtimeMsOverride =
                            null,


                        // ========================================
                        // GPS
                        //
                        // Real evidence collector comes later.
                        // ========================================

                        latitude =
                            null,

                        longitude =
                            null,

                        accuracyMeters =
                            null,

                        insideGeofence =
                            false,


                        // ========================================
                        // BLE
                        //
                        // Architecture supported.
                        // Disabled from V1 attendance decision.
                        // ========================================

                        beaconDetected =
                            false,

                        beaconId =
                            null,

                        beaconRssi =
                            null,


                        // ========================================
                        // WI-FI
                        //
                        // Real BSSID verification comes later.
                        // ========================================

                        wifiVerified =
                            false,

                        wifiBssid =
                            null
                    )


            // ====================================================
            // STRUCTURED EVENT RESULT
            //
            // recordEvent() no longer returns Kotlin Result.
            //
            // We must distinguish:
            //
            // NotStored
            // StoredAndSynced
            // StoredForRetry
            // StoredRejected
            // ====================================================

            when (result) {


                // =============================================
                // EVENT NEVER REACHED DURABLE ROOM STORAGE
                //
                // Production AttendanceEngine must NOT advance
                // attendance state for this outcome.
                // =============================================

                is AttendanceEventRecordResult.NotStored -> {


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "$eventType was NOT stored",
                        result.error
                    )
                }


                // =============================================
                // EVENT STORED + EXPRESS ACCEPTED
                //
                // Production AttendanceEngine may advance the
                // attendance state.
                // =============================================

                is AttendanceEventRecordResult.StoredAndSynced -> {


                    val response =
                        result.response


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "$eventType stored and synchronized successfully"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Local Event UUID: ${result.eventUuid}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Server Event UUID: ${response.eventUuid}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Attendance Event ID: ${response.attendanceEventId}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Attendance ID: ${response.attendanceId}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Previous State: ${response.previousState}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Current State: ${response.currentState}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Effective Event Time: ${response.effectiveEventTime}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Server Received At: ${response.serverReceivedAt}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Time Source: ${response.timeSource}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Time Confidence: ${response.timeConfidence}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Time Drift Seconds: ${response.timeDriftSeconds}"
                    )


                    Log.d(
                        TAG_ATTENDANCE_EVENT,
                        "Message: ${response.message}"
                    )
                }


                // =============================================
                // EVENT EXISTS IN ROOM
                //
                // Remote synchronization temporarily failed.
                //
                // IMPORTANT:
                //
                // Production AttendanceEngine MAY advance its
                // local attendance state because the real event
                // has already been durably created.
                //
                // WorkManager will later retry SAME eventUuid.
                // =============================================

                is AttendanceEventRecordResult.StoredForRetry -> {


                    Log.w(
                        TAG_ATTENDANCE_EVENT,
                        "$eventType stored locally and waiting for synchronization"
                    )


                    Log.w(
                        TAG_ATTENDANCE_EVENT,
                        "Event UUID: ${result.eventUuid}"
                    )


                    Log.w(
                        TAG_ATTENDANCE_EVENT,
                        "Retry reason: ${result.error}"
                    )
                }


                // =============================================
                // EVENT EXISTS IN ROOM BUT EXPRESS REJECTED IT
                //
                // Production AttendanceEngine must NOT blindly
                // advance normal state.
                //
                // Server/local state reconciliation is required.
                // =============================================

                is AttendanceEventRecordResult.StoredRejected -> {


                    val response =
                        result.response


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "$eventType stored locally but rejected by backend"
                    )


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "Event UUID: ${result.eventUuid}"
                    )


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "Code: ${response.code}"
                    )


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "Message: ${response.message}"
                    )


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "Stored rejection error: ${result.error}"
                    )


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "Previous State: ${response.previousState}"
                    )


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "Current State: ${response.currentState}"
                    )


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "Requested Event: ${response.requestedEvent}"
                    )


                    Log.e(
                        TAG_ATTENDANCE_EVENT,
                        "Allowed Next Events: ${response.allowedNextEvents}"
                    )
                }
            }
        }
    }


    // ============================================================
    // TEMPORARY MANUAL OFFLINE SYNC TEST
    //
    // Development only.
    //
    // Final production:
    //
    // WorkManager + network CONNECTED constraint
    //
    // will trigger this automatically.
    // ============================================================

    fun testOfflineAttendanceSync() {


        val token =
            sessionManager
                .getToken()


        if (
            token.isNullOrBlank()
        ) {

            Log.e(
                TAG_ATTENDANCE_SYNC,
                "JWT session not available"
            )

            return
        }


        viewModelScope.launch {


            try {


                Log.d(
                    TAG_ATTENDANCE_SYNC,
                    "Starting manual offline attendance synchronization"
                )


                val result =
                    attendanceSyncManager
                        .syncUnsyncedEvents(
                            token = token
                        )


                Log.d(
                    TAG_ATTENDANCE_SYNC,
                    "Attempted: ${result.attempted}"
                )


                Log.d(
                    TAG_ATTENDANCE_SYNC,
                    "Synced: ${result.synced}"
                )


                Log.d(
                    TAG_ATTENDANCE_SYNC,
                    "Failed: ${result.failed}"
                )


            } catch (
                e: CancellationException
            ) {


                throw e


            } catch (
                e: Exception
            ) {


                Log.e(
                    TAG_ATTENDANCE_SYNC,
                    "Manual attendance synchronization failed",
                    e
                )
            }
        }
    }


    companion object {


        private const val TAG_ATTENDANCE_EVENT =
            "ATTENDANCE_EVENT"


        private const val TAG_ATTENDANCE_SYNC =
            "ATTENDANCE_SYNC"
    }
}
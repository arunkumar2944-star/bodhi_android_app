package com.prodigit.bodhiattendance.domain.attendance

import android.content.Context

import com.prodigit.bodhiattendance.core.network.RetrofitClient

import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase

import com.prodigit.bodhiattendance.data.local.repository.AttendanceEngineStatePersistence
import com.prodigit.bodhiattendance.data.local.repository.AttendanceEngineStateStore
import com.prodigit.bodhiattendance.data.local.repository.OfflineAttendanceRepository
import com.prodigit.bodhiattendance.data.local.repository.RoomAttendanceEngineStatePersistence
import com.prodigit.bodhiattendance.data.local.repository.TimeAnchorLocalRepository

import com.prodigit.bodhiattendance.data.local.security.BootSessionManager
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorManager
import com.prodigit.bodhiattendance.data.local.security.TimeAnchorStorage

import com.prodigit.bodhiattendance.data.repository.AttendanceBootstrapRepositoryImpl
import com.prodigit.bodhiattendance.data.repository.AttendanceRepositoryImpl

import com.prodigit.bodhiattendance.domain.repository.AttendanceRepository
import com.prodigit.bodhiattendance.data.local.security.AttendanceEventManager

// ============================================================
// ATTENDANCE ENGINE RUNTIME PROVIDER
//
// Central dependency provider for the automatic attendance
// runtime.
//
// This class creates and shares:
//
// - AttendanceDatabase
// - AttendanceRepository
// - AttendanceBootstrapRepository
// - BootSessionManager
// - AttendanceEngineStatePersistence
// - AttendanceEngineStateStore
// - TimeAnchorStorage
// - TimeAnchorLocalRepository
// - OfflineAttendanceRepository
// - TimeAnchorManager
//
// Later we will continue adding:
//
// - AttendanceEventManager
// - AttendanceEngineCoordinator
// - AttendanceEngineRuntimeRestorer
// - AttendanceEngineStartupConsistencyGuard
// - AttendanceEngineReconciliationManager
// - AttendanceEngineRuntimeSession
//
// Keeping construction here prevents ViewModels / Services from
// manually creating duplicate runtime dependencies.
// ============================================================

class AttendanceEngineRuntimeProvider(
    context: Context
) {


    // ========================================================
    // APPLICATION CONTEXT
    //
    // Never hold an Activity context here because this provider
    // may live longer than an Activity.
    // ========================================================

    private val appContext =
        context.applicationContext


    // ========================================================
    // ROOM DATABASE
    //
    // Single application database used for:
    //
    // - attendance events
    // - attendance configuration
    // - trusted time anchors
    // - attendance engine state
    // ========================================================

    private val database =
        AttendanceDatabase.getInstance(
            appContext
        )


    // ========================================================
    // REMOTE ATTENDANCE REPOSITORY
    //
    // Handles Express attendance APIs such as:
    //
    // - trusted time anchor
    // - attendance event
    // - active attendance configuration
    // ========================================================

    private val attendanceRepository:
            AttendanceRepository =
        AttendanceRepositoryImpl(
            RetrofitClient.attendanceApi
        )


    // ========================================================
    // ATTENDANCE BOOTSTRAP REPOSITORY
    //
    // Used during authoritative reconciliation.
    //
    // Examples:
    //
    // - no local engine state
    // - phone reboot
    // - stale engine state
    // - server rejection
    //
    // Calls:
    //
    // GET /api/v1/attendance/bootstrap-state
    // ========================================================

    private val attendanceBootstrapRepository =
        AttendanceBootstrapRepositoryImpl(
            RetrofitClient.attendanceApi
        )


    // ========================================================
    // BOOT SESSION MANAGER
    //
    // Supplies:
    //
    // - bootId
    // - elapsedRealtimeMs
    //
    // Android elapsedRealtime resets after a physical reboot.
    //
    // This is required for safe trusted-time and engine-state
    // recovery.
    // ========================================================

    private val bootSessionManager =
        BootSessionManager(
            appContext
        )


    // ========================================================
    // ATTENDANCE ENGINE STATE PERSISTENCE
    //
    // AttendanceEngineStatePersistence is an INTERFACE.
    //
    // Therefore it cannot be instantiated directly.
    //
    // RoomAttendanceEngineStatePersistence is our concrete
    // Room-backed implementation.
    //
    // Flow:
    //
    // AttendanceEngineStateStore
    //          ↓
    // AttendanceEngineStatePersistence
    //          ↓
    // RoomAttendanceEngineStatePersistence
    //          ↓
    // AttendanceEngineStateDao
    //          ↓
    // Room
    // ========================================================

    private val attendanceEngineStatePersistence:
            AttendanceEngineStatePersistence =
        RoomAttendanceEngineStatePersistence(

            attendanceEngineStateDao =
                database.attendanceEngineStateDao()
        )


    // ========================================================
    // ATTENDANCE ENGINE STATE STORE
    //
    // Responsible for safely persisting/restoring:
    //
    // AttendanceState
    // +
    // AttendanceEnginePhase
    //
    // It also validates the Android boot session before restoring
    // monotonic timestamps.
    //
    // It does NOT create attendance events.
    // ========================================================

    private val attendanceEngineStateStore =
        AttendanceEngineStateStore(

            repository =
                attendanceEngineStatePersistence,

            bootSessionManager =
                bootSessionManager
        )


    // ========================================================
    // TIME ANCHOR STORAGE
    //
    // Lightweight local storage for the currently active
    // trusted-time anchor.
    // ========================================================

    private val timeAnchorStorage =
        TimeAnchorStorage(
            appContext
        )


    // ========================================================
    // TIME ANCHOR LOCAL REPOSITORY
    //
    // Durable Room storage for server-issued time anchors.
    //
    // Historical attendance events can reference their original
    // anchor using anchorUuid.
    // ========================================================

    private val timeAnchorLocalRepository =
        TimeAnchorLocalRepository(

            timeAnchorDao =
                database.timeAnchorDao()
        )


    // ========================================================
    // OFFLINE ATTENDANCE REPOSITORY
    //
    // Provides Room-first attendance-event persistence.
    //
    // Attendance events are saved locally before attempting
    // network synchronization.
    // ========================================================

    private val offlineAttendanceRepository =
        OfflineAttendanceRepository(

            attendanceEventDao =
                database.attendanceEventDao()
        )


    // ========================================================
    // TRUSTED TIME MANAGER
    //
    // Coordinates:
    //
    // BootSessionManager
    //          +
    // Express AttendanceRepository
    //          +
    // TimeAnchorStorage
    //          +
    // TimeAnchorLocalRepository
    //
    // Trusted attendance time is based on:
    //
    // serverTime
    // +
    // elapsedRealtime difference
    //
    // Phone wall-clock time is NOT authoritative.
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
// Creates trusted attendance events using Room-first,
// offline-safe persistence.
//
// Flow:
//
// Engine
//   ↓
// AttendanceEventManager
//   ↓
// Save event to Room
//   ↓
// Attempt Express synchronization
//
// Uses the SAME:
//
// - AttendanceRepository
// - OfflineAttendanceRepository
// - BootSessionManager
// - TimeAnchorManager
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
// ATTENDANCE ENGINE COORDINATOR
//
// Connects:
//
// AttendanceEngine
//      ↓
// AttendanceEventManager
//      ↓
// AttendanceEngineStateStore
//
// Responsibilities:
//
// - evaluate engine decisions
// - create/store real attendance events
// - persist resulting engine state
// - keep Room event state and runtime engine state aligned
// ========================================================

    private val attendanceEngineCoordinator =
        AttendanceEngineCoordinator(

            attendanceEventManager =
                attendanceEventManager,

            attendanceEngineStateStore =
                attendanceEngineStateStore
        )

    // ========================================================
// ATTENDANCE ENGINE RUNTIME RESTORER
//
// Restores the persisted automatic-attendance runtime state
// from AttendanceEngineStateStore.
//
// It does NOT create attendance events.
// It only reconstructs:
//
// - AttendanceState
// - AttendanceEnginePhase
//
// and detects cases such as:
//
// - no stored state
// - phone reboot / boot session changed
// - invalid persisted state
// ========================================================

    private val attendanceEngineRuntimeRestorer =
        AttendanceEngineRuntimeRestorer(

            attendanceEngineStateStore =
                attendanceEngineStateStore
        )

    // ========================================================
// ATTENDANCE ENGINE STARTUP CONSISTENCY GUARD
//
// Verifies restored engine state against the latest durable
// local attendance event stored in Room.
//
// Purpose:
//
// - detect mismatch between engine state and latest event
// - prevent automatic attendance from continuing on bad state
// - force reconciliation when local state is inconsistent
//
// Uses:
//
// OfflineAttendanceRepository
// ========================================================

    private val attendanceEngineStartupConsistencyGuard =
        AttendanceEngineStartupConsistencyGuard(

            offlineAttendanceRepository =
                offlineAttendanceRepository
        )

    // ========================================================
// ATTENDANCE ENGINE RUNTIME SESSION
//
// Owns the active in-memory automatic-attendance runtime.
//
// It combines:
//
// - restored persisted engine state
// - startup consistency validation
// - coordinator execution
//
// This is the main runtime object that later ViewModels /
// Services should use instead of constructing engine pieces
// individually.
// ========================================================

    private val attendanceEngineRuntimeSession =
        AttendanceEngineRuntimeSession(

            runtimeRestorer =
                attendanceEngineRuntimeRestorer,

            startupConsistencyGuard =
                attendanceEngineStartupConsistencyGuard,

            coordinator =
                attendanceEngineCoordinator
        )

    // ========================================================
// ATTENDANCE ENGINE RECONCILIATION MANAGER
//
// Used when local runtime cannot safely continue.
//
// Typical cases:
//
// - no persisted engine state
// - phone reboot
// - invalid/corrupted local engine state
// - startup consistency mismatch
// - server rejection
//
// Reconciliation flow:
//
// Express bootstrap state
//        ↓
// fresh trusted-time anchor
//        ↓
// persist reconciled engine state
//        ↓
// apply state back into AttendanceEngineRuntimeSession
// ========================================================

    private val attendanceEngineReconciliationManager =
        AttendanceEngineReconciliationManager(

            bootstrapRepository =
                attendanceBootstrapRepository,

            timeAnchorManager =
                timeAnchorManager,

            attendanceEngineStateStore =
                attendanceEngineStateStore
        )
    // ========================================================
// PUBLIC RUNTIME ACCESS
//
// Only the top-level runtime components are exposed.
//
// Other dependencies remain private so ViewModels / Services
// do not accidentally create parallel attendance flows.
//
// External application code should use:
//
// runtimeSession
// reconciliationManager
// ========================================================

    val runtimeSession:
            AttendanceEngineRuntimeSession
        get() =
            attendanceEngineRuntimeSession


    val reconciliationManager:
            AttendanceEngineReconciliationManager
        get() =
            attendanceEngineReconciliationManager

    val trustedTimeManager:
            TimeAnchorManager
        get() =
            timeAnchorManager

}
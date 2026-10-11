package com.prodigit.bodhiattendance.data.local.repository

import com.prodigit.bodhiattendance.data.local.entity.AttendanceEngineStateEntity
import com.prodigit.bodhiattendance.data.local.security.BootSessionProvider

import com.prodigit.bodhiattendance.domain.attendance.AttendanceEnginePhase
import com.prodigit.bodhiattendance.domain.attendance.AttendanceState

import kotlinx.coroutines.runBlocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


// ============================================================
// ATTENDANCE ENGINE STATE STORE TESTS
//
// These tests verify:
//
// - engine state persistence
// - same-boot process recovery
// - monotonic timestamp preservation
// - reboot protection
// - corrupted state protection
//
// No Android runtime.
// No Room database.
// No Retrofit.
// ============================================================

class AttendanceEngineStateStoreTest {


    // ========================================================
    // TEST 1
    //
    // WORKING + PRESENT
    //
    // Save
    // ↓
    // Restore during same boot
    // ↓
    // WORKING + PRESENT
    // ========================================================

    @Test
    fun workingPresent_sameBoot_restoresSuccessfully() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 1_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            store.saveCurrentState(
                userId = 16,
                companyId = 1,
                deviceId = "DEVICE-1",
                attendanceState = AttendanceState.WORKING,
                phase = AttendanceEnginePhase.Present
            )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is AttendanceEngineRestoreResult.Restored
            )


            result as AttendanceEngineRestoreResult.Restored


            assertEquals(
                AttendanceState.WORKING,
                result.state.attendanceState
            )


            assertEquals(
                AttendanceEnginePhase.Present,
                result.state.phase
            )
        }


    // ========================================================
    // TEST 2
    //
    // ABSENCE_GRACE must preserve the ORIGINAL moment office
    // presence was lost.
    // ========================================================

    @Test
    fun absenceGrace_sameBoot_preservesOriginalAbsenceTime() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 2_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            val absenceStarted =
                1_500_000L


            store.saveCurrentState(
                userId = 16,
                companyId = 1,
                deviceId = "DEVICE-1",
                attendanceState = AttendanceState.WORKING,
                phase =
                    AttendanceEnginePhase.AbsenceGrace(
                        absenceStartedElapsedRealtimeMs =
                            absenceStarted
                    )
            )


            // Simulate some time passing while app/process is gone.

            bootProvider.currentElapsedRealtimeMs =
                1_700_000L


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is AttendanceEngineRestoreResult.Restored
            )


            result as AttendanceEngineRestoreResult.Restored


            assertEquals(
                AttendanceState.WORKING,
                result.state.attendanceState
            )


            assertTrue(
                result.state.phase is
                        AttendanceEnginePhase.AbsenceGrace
            )


            val restoredPhase =
                result.state.phase as
                        AttendanceEnginePhase.AbsenceGrace


            assertEquals(
                absenceStarted,
                restoredPhase.absenceStartedElapsedRealtimeMs
            )
        }


    // ========================================================
    // TEST 3
    //
    // BREAK_ACTIVE must preserve BOTH:
    //
    // - original absence time
    // - break confirmation time
    //
    // This is required for the 90-minute checkout rule.
    // ========================================================

    @Test
    fun breakActive_sameBoot_preservesBothMonotonicTimes() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 3_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            val absenceStarted =
                2_000_000L


            val breakConfirmed =
                2_180_000L


            store.saveCurrentState(
                userId = 16,
                companyId = 1,
                deviceId = "DEVICE-1",
                attendanceState = AttendanceState.ON_BREAK,
                phase =
                    AttendanceEnginePhase.BreakActive(
                        absenceStartedElapsedRealtimeMs =
                            absenceStarted,
                        breakConfirmedElapsedRealtimeMs =
                            breakConfirmed
                    )
            )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is AttendanceEngineRestoreResult.Restored
            )


            result as AttendanceEngineRestoreResult.Restored


            assertEquals(
                AttendanceState.ON_BREAK,
                result.state.attendanceState
            )


            assertTrue(
                result.state.phase is
                        AttendanceEnginePhase.BreakActive
            )


            val restoredPhase =
                result.state.phase as
                        AttendanceEnginePhase.BreakActive


            assertEquals(
                absenceStarted,
                restoredPhase.absenceStartedElapsedRealtimeMs
            )


            assertEquals(
                breakConfirmed,
                restoredPhase.breakConfirmedElapsedRealtimeMs
            )
        }


    // ========================================================
    // TEST 4
    //
    // CHECKOUT_CLASSIFIED preserves:
    //
    // - ORIGINAL exit time
    // - classification/decision time
    //
    // Effective checkout remains original absence time.
    // ========================================================

    @Test
    fun checkoutClassified_preservesOriginalExitAndClassificationTime() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 8_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            val originalExit =
                2_000_000L


            val classifiedAt =
                7_400_000L


            store.saveCurrentState(
                userId = 16,
                companyId = 1,
                deviceId = "DEVICE-1",
                attendanceState = AttendanceState.ON_BREAK,
                phase =
                    AttendanceEnginePhase.CheckoutClassified(
                        absenceStartedElapsedRealtimeMs =
                            originalExit,
                        classifiedAtElapsedRealtimeMs =
                            classifiedAt
                    )
            )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is AttendanceEngineRestoreResult.Restored
            )


            result as AttendanceEngineRestoreResult.Restored


            assertTrue(
                result.state.phase is
                        AttendanceEnginePhase.CheckoutClassified
            )


            val restoredPhase =
                result.state.phase as
                        AttendanceEnginePhase.CheckoutClassified


            assertEquals(
                originalExit,
                restoredPhase.absenceStartedElapsedRealtimeMs
            )


            assertEquals(
                classifiedAt,
                restoredPhase.classifiedAtElapsedRealtimeMs
            )
        }


    // ========================================================
    // TEST 5
    //
    // No stored state.
    //
    // => NotFound
    // ========================================================

    @Test
    fun noPersistedState_returnsNotFound() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 1_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is AttendanceEngineRestoreResult.NotFound
            )
        }


    // ========================================================
    // TEST 6
    //
    // PHONE REBOOT
    //
    // Stored state belongs to BOOT-A.
    //
    // Current device is BOOT-B.
    //
    // Old elapsedRealtime values MUST NOT be restored.
    // ========================================================

    @Test
    fun differentBootSession_blocksMonotonicRestore() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 5_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            store.saveCurrentState(
                userId = 16,
                companyId = 1,
                deviceId = "DEVICE-1",
                attendanceState = AttendanceState.ON_BREAK,
                phase =
                    AttendanceEnginePhase.BreakActive(
                        absenceStartedElapsedRealtimeMs =
                            4_000_000L,
                        breakConfirmedElapsedRealtimeMs =
                            4_180_000L
                    )
            )


            // =================================================
            // SIMULATE DEVICE REBOOT
            //
            // New boot ID.
            //
            // elapsedRealtime also starts from a much smaller
            // value.
            // =================================================

            bootProvider.currentBootId  =
                "BOOT-B"


            bootProvider.currentElapsedRealtimeMs  =
                30_000L


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is
                        AttendanceEngineRestoreResult.BootSessionChanged
            )


            result as
                    AttendanceEngineRestoreResult.BootSessionChanged


            assertEquals(
                AttendanceState.ON_BREAK,
                result.storedAttendanceState
            )


            assertEquals(
                "BOOT-A",
                result.storedBootId
            )


            assertEquals(
                "BOOT-B",
                result.currentBootId
            )
        }


    // ========================================================
    // TEST 7
    //
    // Corrupted / impossible timestamp:
    //
    // persisted absence start is newer than current
    // elapsedRealtime.
    //
    // Same boot but impossible state.
    //
    // => InvalidStoredState
    // ========================================================

    @Test
    fun futureStoredMonotonicTimestamp_returnsInvalidStoredState() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            persistence.state =
                AttendanceEngineStateEntity(
                    userId = 16,
                    companyId = 1,
                    deviceId = "DEVICE-1",
                    attendanceState =
                        AttendanceState.WORKING.name,
                    phaseType =
                        "ABSENCE_GRACE",
                    absenceStartedElapsedRealtimeMs =
                        2_000_000L,
                    breakConfirmedElapsedRealtimeMs =
                        null,
                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,
                    bootId =
                        "BOOT-A",
                    updatedAtEpochMs =
                        123456L
                )


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 1_500_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is
                        AttendanceEngineRestoreResult.InvalidStoredState
            )
        }


    // ========================================================
    // TEST 8
    //
    // BREAK_ACTIVE requires:
    //
    // - absenceStartedElapsedRealtimeMs
    // - breakConfirmedElapsedRealtimeMs
    //
    // Missing break confirmation means persisted state is
    // incomplete/corrupted.
    // ========================================================

    @Test
    fun breakActiveWithoutConfirmationTime_returnsInvalidStoredState() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            persistence.state =
                AttendanceEngineStateEntity(
                    userId = 16,
                    companyId = 1,
                    deviceId = "DEVICE-1",
                    attendanceState =
                        AttendanceState.ON_BREAK.name,
                    phaseType =
                        "BREAK_ACTIVE",
                    absenceStartedElapsedRealtimeMs =
                        1_000_000L,
                    breakConfirmedElapsedRealtimeMs =
                        null,
                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,
                    bootId =
                        "BOOT-A",
                    updatedAtEpochMs =
                        123456L
                )


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 2_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is
                        AttendanceEngineRestoreResult.InvalidStoredState
            )
        }


    // ========================================================
    // TEST 9
    //
    // Timing order corruption:
    //
    // break confirmation cannot occur before original absence.
    // ========================================================

    @Test
    fun breakConfirmationBeforeAbsence_returnsInvalidStoredState() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            persistence.state =
                AttendanceEngineStateEntity(
                    userId = 16,
                    companyId = 1,
                    deviceId = "DEVICE-1",
                    attendanceState =
                        AttendanceState.ON_BREAK.name,
                    phaseType =
                        "BREAK_ACTIVE",
                    absenceStartedElapsedRealtimeMs =
                        2_000_000L,
                    breakConfirmedElapsedRealtimeMs =
                        1_900_000L,
                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,
                    bootId =
                        "BOOT-A",
                    updatedAtEpochMs =
                        123456L
                )


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 3_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is
                        AttendanceEngineRestoreResult.InvalidStoredState
            )
        }


    // ========================================================
    // TEST 10
    //
    // Unknown stored phase.
    //
    // Must never guess or generate attendance.
    // ========================================================

    @Test
    fun unknownPhase_returnsInvalidStoredState() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            persistence.state =
                AttendanceEngineStateEntity(
                    userId = 16,
                    companyId = 1,
                    deviceId = "DEVICE-1",
                    attendanceState =
                        AttendanceState.WORKING.name,
                    phaseType =
                        "SOMETHING_INVALID",
                    absenceStartedElapsedRealtimeMs =
                        null,
                    breakConfirmedElapsedRealtimeMs =
                        null,
                    checkoutClassifiedAtElapsedRealtimeMs =
                        null,
                    bootId =
                        "BOOT-A",
                    updatedAtEpochMs =
                        123456L
                )


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 2_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is
                        AttendanceEngineRestoreResult.InvalidStoredState
            )
        }


    // ========================================================
    // TEST 11
    //
    // CLEAR STATE
    //
    // After logout / controlled reset, current state should no
    // longer restore.
    // ========================================================

    @Test
    fun clearCurrentState_removesPersistedState() =
        runBlocking {


            val persistence =
                FakeAttendanceEngineStatePersistence()


            val bootProvider =
                FakeBootSessionProvider(
                    bootId = "BOOT-A",
                    elapsedRealtimeMs = 1_000_000L
                )


            val store =
                AttendanceEngineStateStore(
                    repository = persistence,
                    bootSessionManager = bootProvider
                )


            store.saveCurrentState(
                userId = 16,
                companyId = 1,
                deviceId = "DEVICE-1",
                attendanceState =
                    AttendanceState.WORKING,
                phase =
                    AttendanceEnginePhase.Present
            )


            store.clearCurrentState(
                userId = 16,
                deviceId = "DEVICE-1"
            )


            val result =
                store.restoreCurrentState(
                    userId = 16,
                    deviceId = "DEVICE-1"
                )


            assertTrue(
                result is AttendanceEngineRestoreResult.NotFound
            )
        }


    // ========================================================
    // FAKE BOOT SESSION PROVIDER
    //
    // Allows tests to simulate:
    //
    // - same boot
    // - passage of monotonic time
    // - device reboot
    // ========================================================

    private class FakeBootSessionProvider(

        bootId: String,

        elapsedRealtimeMs: Long

    ) : BootSessionProvider {


        // ========================================================
        // MUTABLE TEST VALUES
        //
        // Names are intentionally different from interface methods
        // so Kotlin does not generate conflicting JVM getters.
        // ========================================================

        var currentBootId: String =
            bootId


        var currentElapsedRealtimeMs: Long =
            elapsedRealtimeMs


        override fun getBootId(): String {

            return currentBootId
        }


        override fun getElapsedRealtimeMs(): Long {

            return currentElapsedRealtimeMs
        }
    }


    // ========================================================
    // FAKE ENGINE STATE PERSISTENCE
    //
    // Mimics ONE current Room row.
    //
    // No real Room database is needed.
    // ========================================================

    private class FakeAttendanceEngineStatePersistence :
        AttendanceEngineStatePersistence {


        var state:
                AttendanceEngineStateEntity? = null


        override suspend fun saveState(
            state: AttendanceEngineStateEntity
        ) {

            this.state =
                state
        }


        override suspend fun getState(
            userId: Int,
            deviceId: String
        ): AttendanceEngineStateEntity? {


            val current =
                state
                    ?: return null


            return if (
                current.userId == userId &&
                current.deviceId == deviceId
            ) {

                current

            } else {

                null
            }
        }


        override suspend fun deleteState(
            userId: Int,
            deviceId: String
        ) {


            val current =
                state
                    ?: return


            if (
                current.userId == userId &&
                current.deviceId == deviceId
            ) {

                state =
                    null
            }
        }
    }
}
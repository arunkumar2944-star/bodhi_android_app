package com.prodigit.bodhiattendance.domain.attendance

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test

class AttendanceRuntimeStateRefreshTest {
    private val device = "registered-installation"
    private fun snapshot(state: AttendanceState = AttendanceState.WORKING) = AttendanceEngineRuntimeSnapshot(
        AttendanceEngineRuntimeMode.READY, 8, device, state,
        if (state == AttendanceState.NOT_CHECKED_IN) AttendanceEnginePhase.NotMonitoring else AttendanceEnginePhase.Present, null
    )
    private fun ready(state: AttendanceState) = AttendanceEngineBootstrapResult.Ready(state,
        if (state == AttendanceState.NOT_CHECKED_IN) AttendanceEnginePhase.NotMonitoring else AttendanceEnginePhase.Present)
    private fun presence(state: AttendanceState, phase: AttendanceEnginePhase) = AttendancePresenceDecisionEngine.evaluate(
        state, phase, OfficePresenceSnapshot(true, true, capturedAtElapsedRealtimeMs = 1_000_000L),
        AttendanceEnginePolicy(180_000L, 5_400_000L, wifiRequired = true, bleRequired = false)
    )

    @Test fun priorDayRegularizationReleasesStaleWorkingAndFreshPresenceCanCheckInToday() = runBlocking {
        var runtime = snapshot()
        assertTrue(presence(requireNotNull(runtime.attendanceState), requireNotNull(runtime.phase)) is AttendanceEngineDecision.NoAttendanceEvent)
        val order = mutableListOf<String>()
        val result = AttendanceRuntimeStateRefresh(Mutex()).refresh(8, device, AttendanceState.NOT_CHECKED_IN,
            snapshot = { runtime },
            settlePending = { order += "sync original events"; true },
            bootstrap = { order += "read corrected CRM"; ready(AttendanceState.NOT_CHECKED_IN) },
            apply = { order += "apply"; runtime = runtime.copy(attendanceState = it.attendanceState, phase = it.phase) })
        assertTrue(result is AttendanceRuntimeRefreshResult.Updated)
        assertEquals(listOf("sync original events", "read corrected CRM", "apply"), order)
        val decision = presence(requireNotNull(runtime.attendanceState), requireNotNull(runtime.phase)) as AttendanceEngineDecision.CreateAttendanceEvent
        assertEquals(AttendanceEventType.CHECK_IN, decision.eventType)
        assertEquals(1_000_000L, decision.eventElapsedRealtimeMs)
        assertEquals(AttendanceState.WORKING, decision.expectedNextAttendanceState)
        // After that event is accepted, repeating the same observation cannot duplicate check-in.
        assertTrue(presence(decision.expectedNextAttendanceState, decision.nextPhase) is AttendanceEngineDecision.NoAttendanceEvent)
    }

    @Test fun pendingUploadAcceptedByCrmIsNotResetToTheEarlierDailyRead() = runBlocking {
        var runtime = snapshot()
        val result = AttendanceRuntimeStateRefresh(Mutex()).refresh(8, device, AttendanceState.NOT_CHECKED_IN,
            { runtime }, { true }, { ready(AttendanceState.WORKING) },
            { runtime = runtime.copy(attendanceState = it.attendanceState, phase = it.phase) })
        assertTrue(result is AttendanceRuntimeRefreshResult.Updated)
        assertEquals(AttendanceState.WORKING, runtime.attendanceState)
        assertTrue(presence(requireNotNull(runtime.attendanceState), requireNotNull(runtime.phase)) is AttendanceEngineDecision.NoAttendanceEvent)
    }

    @Test fun genuinelyOpenPriorDaySessionStillUsesAuthoritativeBootstrap() = runBlocking {
        var runtime = snapshot()
        val originalGrace = AttendanceEnginePhase.AbsenceGrace(500L)
        runtime = runtime.copy(phase = originalGrace)
        AttendanceRuntimeStateRefresh(Mutex()).refresh(8, device, AttendanceState.NOT_CHECKED_IN,
            { runtime }, { true }, { AttendanceEngineBootstrapResult.Ready(AttendanceState.WORKING, originalGrace) },
            { runtime = runtime.copy(attendanceState = it.attendanceState, phase = it.phase) })
        assertEquals(AttendanceState.WORKING, runtime.attendanceState)
        assertEquals(originalGrace, runtime.phase)
    }

    @Test fun inFlightOrRetryableWriteDefersRefreshWithoutDiscardingLocalState() = runBlocking {
        val original = snapshot()
        var runtime = original
        var reads = 0
        val result = AttendanceRuntimeStateRefresh(Mutex()).refresh(8, device, AttendanceState.NOT_CHECKED_IN,
            { runtime }, { false }, { reads++; ready(AttendanceState.NOT_CHECKED_IN) },
            { runtime = runtime.copy(attendanceState = it.attendanceState) })
        assertTrue(result is AttendanceRuntimeRefreshResult.Deferred)
        assertEquals(0, reads)
        assertEquals(original, runtime)
    }

    @Test fun unchangedStateDoesNotRefreshOrResetDepartureTiming() = runBlocking {
        val runtime = snapshot().copy(phase = AttendanceEnginePhase.AbsenceGrace(123L))
        val result = AttendanceRuntimeStateRefresh(Mutex()).refresh(8, device, AttendanceState.WORKING,
            { runtime }, { error("Should not sync") }, { error("Should not bootstrap") }, { error("Should not apply") })
        assertSame(AttendanceRuntimeRefreshResult.Unchanged, result)
        assertEquals(AttendanceEnginePhase.AbsenceGrace(123L), runtime.phase)
    }

    @Test fun failedBootstrapCannotResetAttendance() = runBlocking {
        for (failure in listOf(AttendanceEngineBootstrapResult.RetryLater("Offline"),
            AttendanceEngineBootstrapResult.Blocked("Conflict"), AttendanceEngineBootstrapResult.Failure(Exception("Server failed")))) {
            val result = AttendanceRuntimeStateRefresh(Mutex()).refresh(8, device, AttendanceState.NOT_CHECKED_IN,
                { snapshot() }, { true }, { failure }, { error("Failure cannot apply") })
            assertTrue(result is AttendanceRuntimeRefreshResult.Deferred)
        }
    }

    @Test fun changedIdentityDoesNotSyncOrApplyAnotherEmployeesState() = runBlocking {
        val result = AttendanceRuntimeStateRefresh(Mutex()).refresh(9, "another-installation", AttendanceState.NOT_CHECKED_IN,
            { snapshot() }, { error("Wrong identity") }, { error("Wrong identity") }, { error("Wrong identity") })
        assertTrue(result is AttendanceRuntimeRefreshResult.Deferred)
    }

    @Test fun cancelledRefreshPreservesStateAndReleasesEvaluationLock() = runBlocking {
        val mutex = Mutex()
        val runtime = snapshot()
        try {
            AttendanceRuntimeStateRefresh(mutex).refresh(8, device, AttendanceState.NOT_CHECKED_IN,
                { runtime }, { true }, { throw CancellationException("Stopped") }, { error("Must not apply") })
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { /* expected */ }
        assertFalse(mutex.isLocked)
        assertEquals(AttendanceState.WORKING, runtime.attendanceState)
    }

    @Test fun sensorEvaluationWaitsUntilCrmStateHasBeenApplied() = runBlocking {
        val mutex = Mutex()
        var runtime = snapshot()
        val requestStarted = CompletableDeferred<Unit>()
        val releaseRequest = CompletableDeferred<Unit>()
        val refresh = launch(start = CoroutineStart.UNDISPATCHED) {
            AttendanceRuntimeStateRefresh(mutex).refresh(8, device, AttendanceState.NOT_CHECKED_IN,
                { runtime }, { true }, { requestStarted.complete(Unit); releaseRequest.await(); ready(AttendanceState.NOT_CHECKED_IN) },
                { runtime = runtime.copy(attendanceState = it.attendanceState, phase = it.phase) })
        }
        requestStarted.await()
        var observed: AttendanceState? = null
        val sensor = launch(start = CoroutineStart.UNDISPATCHED) { mutex.withLock { observed = runtime.attendanceState } }
        assertNull(observed)
        releaseRequest.complete(Unit)
        refresh.join(); sensor.join()
        assertEquals(AttendanceState.NOT_CHECKED_IN, observed)
    }

    @Test fun serverCheckoutStillClosesToday() = runBlocking {
        var runtime = snapshot()
        AttendanceRuntimeStateRefresh(Mutex()).refresh(8, device, AttendanceState.CHECKED_OUT,
            { runtime }, { true }, { AttendanceEngineBootstrapResult.Ready(AttendanceState.CHECKED_OUT, AttendanceEnginePhase.NotMonitoring) },
            { runtime = runtime.copy(attendanceState = it.attendanceState, phase = it.phase) })
        assertEquals(AttendanceState.CHECKED_OUT, runtime.attendanceState)
        assertTrue(presence(requireNotNull(runtime.attendanceState), requireNotNull(runtime.phase)) is AttendanceEngineDecision.NoAttendanceEvent)
    }
}

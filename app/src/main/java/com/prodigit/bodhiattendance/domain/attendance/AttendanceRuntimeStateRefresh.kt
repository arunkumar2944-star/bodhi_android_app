package com.prodigit.bodhiattendance.domain.attendance

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class AttendanceRuntimeRefreshResult {
    object Unchanged : AttendanceRuntimeRefreshResult()
    data class Updated(val bootstrap: AttendanceEngineBootstrapResult.Ready) : AttendanceRuntimeRefreshResult()
    data class Deferred(val reason: String) : AttendanceRuntimeRefreshResult()
}

/** Refreshes manager corrections without racing sensor evaluation or dropping queued events. */
class AttendanceRuntimeStateRefresh(private val evaluationMutex: Mutex) {
    suspend fun refresh(
        userId: Int,
        deviceId: String,
        expectedState: AttendanceState,
        snapshot: () -> AttendanceEngineRuntimeSnapshot,
        settlePending: suspend () -> Boolean,
        bootstrap: suspend (AttendanceEngineRuntimeSnapshot) -> AttendanceEngineBootstrapResult,
        apply: (AttendanceEngineBootstrapResult.Ready) -> Unit
    ): AttendanceRuntimeRefreshResult = evaluationMutex.withLock {
        val before = snapshot()
        if (before.userId != userId || before.deviceId != deviceId) {
            return@withLock AttendanceRuntimeRefreshResult.Deferred("Attendance runtime identity changed")
        }
        if (before.mode == AttendanceEngineRuntimeMode.READY && before.attendanceState == expectedState) {
            return@withLock AttendanceRuntimeRefreshResult.Unchanged
        }
        // A locally saved check-in can legitimately precede CRM's daily read.
        // Settle that original UUID first; never reset past an in-flight write.
        if (!settlePending()) {
            return@withLock AttendanceRuntimeRefreshResult.Deferred("Saved attendance is still awaiting CRM validation")
        }
        when (val result = bootstrap(before)) {
            is AttendanceEngineBootstrapResult.Ready -> {
                // Bootstrap can still report a genuinely open prior-day session.
                // The daily read is a refresh trigger, never permission to reset it.
                apply(result)
                AttendanceRuntimeRefreshResult.Updated(result)
            }
            is AttendanceEngineBootstrapResult.Blocked -> AttendanceRuntimeRefreshResult.Deferred(result.reason)
            is AttendanceEngineBootstrapResult.RetryLater -> AttendanceRuntimeRefreshResult.Deferred(result.reason)
            is AttendanceEngineBootstrapResult.Failure -> AttendanceRuntimeRefreshResult.Deferred(
                result.error.message ?: "Attendance reconciliation failed"
            )
        }
    }
}

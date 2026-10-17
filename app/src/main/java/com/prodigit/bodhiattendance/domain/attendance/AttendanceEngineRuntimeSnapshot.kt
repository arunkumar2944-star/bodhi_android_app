package com.prodigit.bodhiattendance.domain.attendance

// ============================================================
// ATTENDANCE ENGINE RUNTIME MODE
// ============================================================

enum class AttendanceEngineRuntimeMode {

    UNINITIALIZED,

    READY,

    RECONCILIATION_REQUIRED,

    PERSISTENCE_RECOVERY_REQUIRED
}


// ============================================================
// RUNTIME SNAPSHOT
// ============================================================

data class AttendanceEngineRuntimeSnapshot(

    val mode: AttendanceEngineRuntimeMode,

    val userId: Int?,

    val deviceId: String?,

    val attendanceState: AttendanceState?,

    val phase: AttendanceEnginePhase?,

    val reason: String?
)

package com.prodigit.bodhiattendance.domain.attendance


// ============================================================
// ATTENDANCE BOOTSTRAP REPOSITORY
//
// Server-side reconciliation boundary.
//
// Concrete implementation will call Express and return the
// current authoritative automatic attendance state.
//
// IMPORTANT:
//
// We deliberately keep this interface independent from Retrofit
// response DTOs.
//
// Giri's final Express endpoint can return any suitable JSON
// shape and the Android data-layer implementation will normalize
// it into AttendanceBootstrapState.
// ============================================================

interface AttendanceBootstrapRepository {


    // ========================================================
    // GET AUTHORITATIVE CURRENT STATE
    // ========================================================

    suspend fun getCurrentAttendanceState(

        token: String,

        userId: Int,

        companyId: Int,

        deviceId: String

    ): Result<AttendanceBootstrapState>
}
package com.prodigit.bodhiattendance.domain.repository

import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig


// ============================================================
// ATTENDANCE CONFIGURATION REPOSITORY
//
// Domain contract for retrieving attendance configuration.
//
// Configuration is:
//
// React CRM
//      ↓
// Express
//      ↓
// PostgreSQL
//      ↓
// Android
//
// Android has READ-ONLY access to this configuration.
//
// Implemented by:
//
// data/repository/AttendanceConfigRepositoryImpl.kt
// ============================================================

interface AttendanceConfigRepository {


    // ========================================================
    // GET ACTIVE ATTENDANCE CONFIGURATION
    //
    // Returns all active attendance locations available to
    // the authenticated employee/company.
    //
    // Each location can contain:
    //
    // - GPS / geofence configuration
    // - BLE beacon configuration
    // - Wi-Fi access-point configuration
    //
    // Result.failure is used for transport/parsing/system
    // failures.
    // ========================================================

    suspend fun getActiveAttendanceConfig(

        token: String

    ): Result<List<AttendanceLocationConfig>>
}
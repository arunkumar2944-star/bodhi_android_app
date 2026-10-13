package com.prodigit.bodhiattendance.domain.attendance

import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig

/** Converts only CRM-supplied policy. Missing/invalid values cannot authorize events. */
fun AttendanceLocationConfig.toEnginePolicy(): Result<AttendanceEnginePolicy> = runCatching {
    val grace = requireNotNull(gracePeriodMinutes) { "CRM grace period is missing" }
    val absence = requireNotNull(maximumAbsenceMinutes) { "CRM maximum absence is missing" }
    val wifi = requireNotNull(wifiRequired) { "CRM Wi-Fi requirement is missing" }
    val ble = requireNotNull(bleRequired) { "CRM BLE requirement is missing" }
    require(grace >= 0 && grace <= Long.MAX_VALUE / 60_000L) { "Invalid CRM grace period" }
    require(absence > 0 && absence <= Long.MAX_VALUE / 60_000L) { "Invalid CRM maximum absence" }
    require(absence >= grace) { "CRM maximum absence must be at least grace period" }
    AttendanceEnginePolicy(grace * 60_000L, absence * 60_000L, wifi, ble)
}

package com.prodigit.bodhiattendance.ui.systemhealth

import com.prodigit.bodhiattendance.core.monitoring.GpsEvidenceRejection
import com.prodigit.bodhiattendance.core.monitoring.GpsEvidenceStatus

fun gpsIntegrityHealth(gps: GpsEvidenceStatus.Snapshot): HealthItemState = when {
    !gps.observed -> HealthItemState(HealthStatus.CHECKING, "GPS Integrity", "Waiting for a fresh GPS reading")
    gps.rejection == GpsEvidenceRejection.MOCK_LOCATION_DETECTED -> HealthItemState(
        HealthStatus.NOT_READY, "GPS Integrity",
        "Mock location detected. Attendance events are blocked. Stop fake GPS and wait for a fresh real location."
    )
    gps.rejection == GpsEvidenceRejection.GPS_STALE -> HealthItemState(
        HealthStatus.NOT_READY, "GPS Integrity", "GPS reading is stale. Waiting for a fresh location."
    )
    gps.rejection == GpsEvidenceRejection.GPS_INVALID -> HealthItemState(
        HealthStatus.NOT_READY, "GPS Integrity", "GPS reading is invalid. Waiting for a valid location."
    )
    else -> HealthItemState(HealthStatus.READY, "GPS Integrity", "Latest validated GPS reading is not marked as mock")
}

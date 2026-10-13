package com.prodigit.bodhiattendance.domain.attendance

import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig
import org.junit.Assert.*
import org.junit.Test

class AttendanceEnginePolicyMapperTest {
    private val office = AttendanceLocationConfig(
        id = 1, locationName = "CRM office", locationCode = "OFFICE",
        latitude = 0.0, longitude = 0.0, geofenceRadiusMeters = 30.0,
        maxAccuracyMeters = 30.0, wifiRequired = true, bleRequired = false,
        gracePeriodMinutes = 3, maximumAbsenceMinutes = 90
    )

    @Test fun convertsCrmMinutesAndPreservesFlags() {
        val policy = office.toEnginePolicy().getOrThrow()
        assertEquals(180_000L, policy.gracePeriodMs)
        assertEquals(5_400_000L, policy.maximumAbsenceMs)
        assertEquals(true, policy.wifiRequired)
        assertEquals(false, policy.bleRequired)
    }

    @Test fun blocksMissingInvalidAndOverflowingTiming() {
        assertTrue(office.copy(gracePeriodMinutes = null).toEnginePolicy().isFailure)
        assertTrue(office.copy(maximumAbsenceMinutes = 0).toEnginePolicy().isFailure)
        assertTrue(office.copy(maximumAbsenceMinutes = 2).toEnginePolicy().isFailure)
        assertTrue(office.copy(maximumAbsenceMinutes = Long.MAX_VALUE).toEnginePolicy().isFailure)
        assertTrue(office.copy(wifiRequired = null).toEnginePolicy().isFailure)
    }
}

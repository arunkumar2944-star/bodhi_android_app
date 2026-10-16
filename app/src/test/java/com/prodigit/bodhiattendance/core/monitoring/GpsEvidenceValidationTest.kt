package com.prodigit.bodhiattendance.core.monitoring

import org.junit.Assert.*
import org.junit.Test

class GpsEvidenceValidationTest {
    private fun reject(mocked: Boolean = false, accuracy: Float = 10f,
        latitude: Double = 11.0, longitude: Double = 78.0,
        captured: Long = 100_000_000_000L, now: Long = 100_000_000_000L,
        hasAccuracy: Boolean = true) = GpsEvidenceValidation.rejection(
        mocked, hasAccuracy, accuracy, latitude, longitude, captured, now)

    @Test fun mockTakesPriorityEvenWhenStaleOrInvalid() {
        assertEquals(GpsEvidenceRejection.MOCK_LOCATION_DETECTED,
            reject(mocked = true, accuracy = Float.NaN, captured = 1L))
    }
    @Test fun freshRealFixPassesAndSixtySecondBoundaryIsAccepted() {
        assertNull(reject())
        assertNull(reject(captured = 40_000_000_000L))
    }
    @Test fun staleFixHasDistinctReason() {
        assertEquals(GpsEvidenceRejection.GPS_STALE, reject(captured = 39_999_999_999L))
    }
    @Test fun invalidCoordinatesAccuracyAndTimestampsAreRejected() {
        val invalid = GpsEvidenceRejection.GPS_INVALID
        assertEquals(invalid, reject(latitude = Double.NaN))
        assertEquals(invalid, reject(latitude = 91.0))
        assertEquals(invalid, reject(longitude = 181.0))
        assertEquals(invalid, reject(accuracy = Float.POSITIVE_INFINITY))
        assertEquals(invalid, reject(accuracy = -1f))
        assertEquals(invalid, reject(hasAccuracy = false))
        assertEquals(invalid, reject(captured = 0L))
        assertEquals(invalid, reject(captured = 100_000_000_001L))
    }
}

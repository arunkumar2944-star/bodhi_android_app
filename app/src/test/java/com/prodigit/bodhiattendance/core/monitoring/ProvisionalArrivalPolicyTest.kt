package com.prodigit.bodhiattendance.core.monitoring

import org.junit.Assert.*
import org.junit.Test

class ProvisionalArrivalPolicyTest {
    private fun allowed(notCheckedIn: Boolean = true, wifiRequired: Boolean = true,
                        bleRequired: Boolean = false, disconnected: Boolean = true, mock: Boolean = false,
                        distance: Double = 30.0, accuracy: Double = 10.0, radius: Double = 100.0,
                        limit: Double = 25.0, observed: Long = 100_000, captured: Long = 101_000,
                        anchor: Long = 90_000, sameScope: Boolean = true, authorized: Boolean = true) =
        ProvisionalArrivalPolicy.canSave(notCheckedIn, wifiRequired, bleRequired, disconnected, mock,
            distance, accuracy, radius, limit, observed, captured, anchor, sameScope, authorized)

    @Test fun strongGpsWithMissingWifiMaySaveSupportingEvidence() { assertTrue(allowed()) }
    @Test fun existingAttendanceOrOtherOfficePoliciesCannotUseFallback() {
        assertFalse(allowed(notCheckedIn = false)); assertFalse(allowed(wifiRequired = false)); assertFalse(allowed(bleRequired = true))
    }
    @Test fun connectedWrongRouterIsNotWifiUnavailable() { assertFalse(allowed(disconnected = false)) }
    @Test fun mockedAndPoorAccuracyAreRejected() {
        assertFalse(allowed(mock = true)); assertFalse(allowed(accuracy = 39.6)); assertFalse(allowed(accuracy = 0.0))
    }
    @Test fun accuracyCircleMustFitInsideOffice() {
        assertTrue(allowed(distance = 90.0)); assertFalse(allowed(distance = 90.01)); assertFalse(allowed(distance = 101.0))
    }
    @Test fun staleFutureOrPreAnchorObservationIsRejected() {
        assertFalse(allowed(captured = 160_001)); assertFalse(allowed(captured = 99_999)); assertFalse(allowed(anchor = 100_001))
    }
    @Test fun expiredAuthorizationOrAnotherBootIsRejected() {
        assertFalse(allowed(authorized = false)); assertFalse(allowed(sameScope = false))
    }
    @Test fun invalidNumbersCannotCreateEvidence() {
        assertFalse(allowed(distance = Double.NaN)); assertFalse(allowed(accuracy = Double.POSITIVE_INFINITY))
        assertFalse(allowed(radius = 0.0)); assertFalse(allowed(limit = Double.NaN)); assertFalse(allowed(distance = -1.0))
    }
}

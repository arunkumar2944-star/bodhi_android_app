package com.prodigit.bodhiattendance.core.monitoring

import org.junit.Assert.*
import org.junit.Test

class DepartureEvidenceTrackerTest {
    @Test fun firstOutsideObservationRequiresConfirmation() {
        val tracker = DepartureEvidenceTracker()
        assertFalse(tracker.confirmOutside(1000L))
        assertTrue(tracker.confirmOutside(31000L))
    }

    @Test fun gapRequiresNewPairButDoesNotRequireNinetyMinutesOfContinuousGps() {
        val tracker = DepartureEvidenceTracker()
        tracker.confirmOutside(1000L)
        assertTrue(tracker.confirmOutside(31000L))
        assertFalse(tracker.confirmOutside(5500000L))
        assertTrue(tracker.confirmOutside(5530000L))
    }

    @Test fun unknownOrReturnRequiresAnotherPair() {
        val tracker = DepartureEvidenceTracker()
        tracker.confirmOutside(1000L)
        tracker.reset()
        assertFalse(tracker.confirmOutside(31000L))
        assertTrue(tracker.confirmOutside(61000L))
    }

    @Test fun duplicateAndReversedObservationsDoNotConfirmDeparture() {
        val tracker = DepartureEvidenceTracker()
        tracker.confirmOutside(31000L)
        assertFalse(tracker.confirmOutside(31000L))
        assertFalse(tracker.confirmOutside(1000L))
    }
}

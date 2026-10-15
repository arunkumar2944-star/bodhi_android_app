package com.prodigit.bodhiattendance.ui.systemhealth

import org.junit.Assert.assertEquals
import org.junit.Test

class OfficeHealthPolicyTest {
    private val approved = listOf("AA:BB:CC:DD:EE:FF")

    @Test fun matchingRouterIsReady() {
        assertEquals(HealthStatus.READY, officeWifiHealth(true, approved, "aa:bb:cc:dd:ee:ff", true).status)
    }
    @Test fun maskedIdentityIsPendingRatherThanMismatch() {
        assertEquals(HealthStatus.NOT_CONFIGURED, officeWifiHealth(true, approved, "02:00:00:00:00:00", true).status)
    }
    @Test fun wifiOffNeedsAction() {
        assertEquals(HealthStatus.NOT_READY, officeWifiHealth(true, approved, null, false).status)
    }
    @Test fun differentOfficeRouterNeedsAction() {
        assertEquals(HealthStatus.NOT_READY, officeWifiHealth(true, approved, "11:22:33:44:55:66", true).status)
    }
    @Test fun optionalWifiDoesNotRequireConnection() {
        assertEquals(HealthStatus.NOT_REQUIRED, officeWifiHealth(false, emptyList(), null, false).status)
    }
    @Test fun unknownPolicyIsNotAssumedOptional() {
        assertEquals(HealthStatus.NOT_CONFIGURED, officeWifiHealth(null, approved, approved[0], true).status)
    }
}

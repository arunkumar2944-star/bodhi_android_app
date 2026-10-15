package com.prodigit.bodhiattendance.core.monitoring

import org.junit.Assert.*
import org.junit.Test

class EligibilityRecoveryPolicyTest {
    @Test fun unavailableServerAndRateLimitsRemainRetryable() {
        for (status in listOf(408, 429, 500, 502, 503, 504)) assertTrue(EligibilityRecoveryPolicy.shouldRetryHttp(status))
    }
    @Test fun revokedAuthenticationAndDeviceAccessStopMonitoring() {
        assertFalse(EligibilityRecoveryPolicy.shouldRetryHttp(401))
        assertFalse(EligibilityRecoveryPolicy.shouldRetryHttp(403))
    }
    @Test fun retriesBackOffAndRemainBoundedDuringLongOutages() {
        assertEquals(30_000L, EligibilityRecoveryPolicy.retryDelayMillis(1))
        assertEquals(60_000L, EligibilityRecoveryPolicy.retryDelayMillis(2))
        assertEquals(120_000L, EligibilityRecoveryPolicy.retryDelayMillis(3))
        assertEquals(120_000L, EligibilityRecoveryPolicy.retryDelayMillis(1000))
    }
}

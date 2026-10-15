package com.prodigit.bodhiattendance.core.monitoring

import org.junit.Assert.*
import org.junit.Test

class OfflineWorkSessionPolicyTest {
    private val scope = OfflineSessionScope(7, 1, "device", "boot")
    private val lease = OfflineSessionLease(scope, "2026-10-15", 1000000L, 1000000L + 43200000L, 1000L)
    private fun valid(value: OfflineSessionLease = lease, expected: OfflineSessionScope = scope,
        now: Long = 2000L, time: Long = 1001000L, day: String = lease.workDate, paused: Boolean = false) =
        OfflineWorkSessionPolicy.valid(value, expected, now, time, day, paused)

    @Test fun validPreviouslyAuthorizedWorkSessionCanContinueOffline() { assertTrue(valid()) }
    @Test fun logoutCheckoutAndIdentityChangesCannotInheritAuthorization() {
        assertFalse(valid(paused = true))
        assertFalse(valid(expected = scope.copy(userId = 0)))
        assertFalse(valid(expected = scope.copy(userId = 8)))
        assertFalse(valid(expected = scope.copy(companyId = 2)))
        assertFalse(valid(expected = scope.copy(deviceId = "other")))
    }
    @Test fun rebootNewDayAndExpiredGrantBlockOfflineMarking() {
        assertFalse(valid(expected = scope.copy(bootId = "reboot")))
        assertFalse(valid(day = "2026-10-16"))
        assertFalse(valid(time = lease.validUntilMs))
        assertFalse(valid(now = 0))
        assertFalse(valid(time = lease.issuedAtMs - 1))
    }
    @Test fun grantCannotBeExtendedBeyondTwelveHours() {
        assertFalse(valid(value = lease.copy(validUntilMs = lease.validUntilMs + 1)))
        assertFalse(valid(now = lease.receivedElapsedMs + 43200000L))
    }
    @Test fun effectiveEventMustBelongToAuthorizedInterval() {
        assertTrue(OfflineWorkSessionPolicy.permitsEvent(lease, lease.issuedAtMs))
        assertFalse(OfflineWorkSessionPolicy.permitsEvent(lease, lease.issuedAtMs - 1))
        assertFalse(OfflineWorkSessionPolicy.permitsEvent(lease, lease.validUntilMs))
    }
}

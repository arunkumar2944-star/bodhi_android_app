package com.prodigit.bodhiattendance.core.monitoring

data class OfflineSessionScope(val userId: Int, val companyId: Int, val deviceId: String, val bootId: String)
data class OfflineSessionLease(val scope: OfflineSessionScope, val workDate: String,
    val issuedAtMs: Long, val validUntilMs: Long, val receivedElapsedMs: Long)

object OfflineWorkSessionPolicy {
    const val MAX_DURATION_MS = 12 * 60 * 60_000L
    fun valid(lease: OfflineSessionLease, expected: OfflineSessionScope, elapsedMs: Long,
              trustedNowMs: Long, trustedDay: String, paused: Boolean): Boolean =
        !paused && expected.userId > 0 && expected.companyId > 0 && expected.deviceId.isNotBlank() &&
            expected.bootId.isNotBlank() && lease.scope == expected && lease.workDate == trustedDay &&
            lease.issuedAtMs >= 0 && lease.validUntilMs - lease.issuedAtMs in 1..MAX_DURATION_MS &&
            lease.receivedElapsedMs >= 0 && elapsedMs - lease.receivedElapsedMs in 0 until MAX_DURATION_MS &&
            trustedNowMs >= lease.issuedAtMs && trustedNowMs < lease.validUntilMs

    fun permitsEvent(lease: OfflineSessionLease, effectiveMs: Long): Boolean =
        effectiveMs >= lease.issuedAtMs && effectiveMs < lease.validUntilMs
}

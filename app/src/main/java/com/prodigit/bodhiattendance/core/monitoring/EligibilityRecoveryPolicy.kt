package com.prodigit.bodhiattendance.core.monitoring

/** Transport errors never mean leave/absence. Explicit access denial is terminal. */
object EligibilityRecoveryPolicy {
    fun shouldRetryHttp(status: Int): Boolean = status != 401 && status != 403
    fun retryDelayMillis(attempt: Int): Long = when {
        attempt <= 1 -> 30_000L
        attempt == 2 -> 60_000L
        else -> 120_000L
    }
}
class EligibilityHttpFailure(val status: Int) : IllegalStateException("Daily eligibility HTTP $status")

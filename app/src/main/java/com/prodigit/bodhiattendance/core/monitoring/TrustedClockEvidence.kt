package com.prodigit.bodhiattendance.core.monitoring

import java.time.Instant

/** Pure clock calculation. Wall time is comparison metadata, never the reference. */
data class TrustedClockEvidence(
    val trustedTimeMs: Long?,
    val deviceTimeMs: Long,
    val elapsedMs: Long,
    val anchorUuid: String?,
    val anchorServerTime: String?,
    val anchorAgeMs: Long?,
    val clockDifferenceMs: Long?
) {
    val available: Boolean get() = trustedTimeMs != null
    val mismatch: Boolean get() = clockDifferenceMs?.let { it > 60_000L || it < -60_000L } == true
    val status: String get() = when {
        !available -> "UNAVAILABLE"
        mismatch -> "CLOCK_MISMATCH"
        else -> "AVAILABLE"
    }
    val message: String get() = when {
        !available -> "Trusted time unavailable; server verification required"
        mismatch -> "Phone clock differs; server-backed attendance time remains available"
        else -> "Server-backed trusted time available"
    }

    companion object {
        fun calculate(serverTime: String?, anchorElapsedMs: Long?, anchorUuid: String?,
                      sameBootAndDevice: Boolean, nowElapsedMs: Long, deviceTimeMs: Long): TrustedClockEvidence {
            val age = anchorElapsedMs?.takeIf { it >= 0 && it <= nowElapsedMs }?.let { nowElapsedMs - it }
            val trusted = if (sameBootAndDevice && !anchorUuid.isNullOrBlank() && age != null) {
                runCatching { Math.addExact(Instant.parse(serverTime).toEpochMilli(), age) }.getOrNull()
            } else null
            val difference = trusted?.let { runCatching { Math.subtractExact(deviceTimeMs, it) }.getOrNull() }
            return TrustedClockEvidence(trusted, deviceTimeMs, nowElapsedMs,
                anchorUuid.takeIf { trusted != null }, serverTime.takeIf { trusted != null },
                age.takeIf { trusted != null }, difference)
        }
    }
}

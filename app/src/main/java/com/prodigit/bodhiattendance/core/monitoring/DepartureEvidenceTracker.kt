package com.prodigit.bodhiattendance.core.monitoring

/** A gap requires a new pair of fresh outside observations, not a new attendance timer. */
class DepartureEvidenceTracker {
    private var previous: Long? = null

    fun confirmOutside(elapsedMs: Long): Boolean {
        require(elapsedMs > 0)
        val last = previous
        previous = elapsedMs
        return last != null && elapsedMs - last in 1..90_000L
    }

    fun reset() { previous = null }
}

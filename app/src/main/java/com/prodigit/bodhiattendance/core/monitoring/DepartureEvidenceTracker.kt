package com.prodigit.bodhiattendance.core.monitoring

/**
 * Confirm a sustained departure from two independent, sufficiently separated,
 * fresh, accurate outside observations. This is an evidence guard, not the
 * CRM-managed departure grace period or maximum absence duration.
 */
class DepartureEvidenceTracker {
    private var previous: Long? = null

    fun confirmOutside(elapsedMs: Long): Boolean {
        require(elapsedMs > 0L)
        val last = previous
        if (last == null) {
            previous = elapsedMs
            return false
        }

        // Duplicate/out-of-order samples cannot establish continuity or
        // poison the timeline with an older timestamp.
        if (elapsedMs <= last) return false

        val gapMs = elapsedMs - last
        if (gapMs > MAX_CONFIRMATION_GAP_MS) {
            previous = elapsedMs
            return false
        }

        if (gapMs < MIN_CONFIRMATION_GAP_MS) return false

        previous = elapsedMs
        return true
    }

    fun reset() { previous = null }

    companion object {
        const val MIN_CONFIRMATION_GAP_MS = 15_000L
        const val MAX_CONFIRMATION_GAP_MS = 90_000L
    }
}

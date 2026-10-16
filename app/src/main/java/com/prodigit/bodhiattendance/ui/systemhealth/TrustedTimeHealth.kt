package com.prodigit.bodhiattendance.ui.systemhealth

import com.prodigit.bodhiattendance.core.monitoring.TrustedClockEvidence

/** Health reports corrective action separately from the availability of attendance time. */
fun trustedTimeHealth(clock: TrustedClockEvidence): HealthItemState = HealthItemState(
    status = if (clock.available && !clock.mismatch) HealthStatus.READY else HealthStatus.NOT_READY,
    title = "Trusted Time",
    message = if (clock.mismatch) {
        "Phone clock differs. Turn on automatic date & time; server-backed attendance time is still protected."
    } else clock.message
)

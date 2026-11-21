package com.prodigit.bodhiattendance.core.monitoring

/** Only decides whether supporting evidence may be saved. Never confirms attendance. */
object ProvisionalArrivalPolicy {
    fun canSave(notCheckedInOrOnBreak: Boolean, wifiRequired: Boolean, bleRequired: Boolean,
                wifiDisconnected: Boolean, mockDetected: Boolean, distance: Double,
                accuracy: Double, radius: Double, accuracyLimit: Double,
                observedMs: Long, capturedMs: Long, anchorMs: Long,
                sameBootAndDevice: Boolean, authorized: Boolean): Boolean =
        notCheckedInOrOnBreak && wifiRequired && !bleRequired && wifiDisconnected && !mockDetected &&
            sameBootAndDevice && authorized && distance.isFinite() && distance >= 0 &&
            accuracy.isFinite() && accuracy > 0 && accuracyLimit.isFinite() && accuracyLimit > 0 &&
            radius.isFinite() && radius > 0 && accuracy <= accuracyLimit && distance + accuracy <= radius &&
            observedMs >= anchorMs && capturedMs >= observedMs && capturedMs - observedMs <= 60_000L
}

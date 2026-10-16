package com.prodigit.bodhiattendance.core.monitoring

/** Pure validation shared by the sensor boundary and regression tests. */
enum class GpsEvidenceRejection { MOCK_LOCATION_DETECTED, GPS_STALE, GPS_INVALID }

object GpsEvidenceValidation {
    fun rejection(
        mocked: Boolean, hasAccuracy: Boolean, accuracy: Float,
        latitude: Double, longitude: Double, capturedNanos: Long, nowNanos: Long
    ): GpsEvidenceRejection? = when {
        mocked -> GpsEvidenceRejection.MOCK_LOCATION_DETECTED
        !hasAccuracy || !accuracy.isFinite() || accuracy < 0f ||
            !latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0 ||
            capturedNanos <= 0L || capturedNanos > nowNanos -> GpsEvidenceRejection.GPS_INVALID
        nowNanos - capturedNanos > 60_000_000_000L -> GpsEvidenceRejection.GPS_STALE
        else -> null
    }
}

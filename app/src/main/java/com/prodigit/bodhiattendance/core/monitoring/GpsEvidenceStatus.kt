package com.prodigit.bodhiattendance.core.monitoring

import android.location.Location
import android.os.SystemClock
import androidx.core.location.LocationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sensor health only. Never changes the employee's attendance state. */
object GpsEvidenceStatus {
    data class Snapshot(val observed: Boolean = false, val rejection: GpsEvidenceRejection? = null)
    private val mutable = MutableStateFlow(Snapshot())
    val state = mutable.asStateFlow()

    fun rejection(location: Location): GpsEvidenceRejection? = GpsEvidenceValidation.rejection(
        LocationCompat.isMock(location), location.hasAccuracy(), location.accuracy,
        location.latitude, location.longitude, location.elapsedRealtimeNanos, SystemClock.elapsedRealtimeNanos()
    )

    fun report(rejection: GpsEvidenceRejection?) {
        // A stale/invalid reading cannot clear a previously observed mock warning.
        if (mutable.value.rejection == GpsEvidenceRejection.MOCK_LOCATION_DETECTED && rejection != null) return
        mutable.value = Snapshot(observed = true, rejection = rejection)
    }
}

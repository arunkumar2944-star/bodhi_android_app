package com.prodigit.bodhiattendance.core.monitoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-process sensor status for the UI. Not authoritative attendance evidence. */
data class AttendanceMonitorSnapshot(
    val running: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val wifiBssid: String? = null
)

object AttendanceMonitorStatus {
    private val mutable = MutableStateFlow(AttendanceMonitorSnapshot())
    val state = mutable.asStateFlow()

    fun running() { mutable.value = mutable.value.copy(running = true) }
    fun stopped() { mutable.value = mutable.value.copy(running = false) }

    fun observation(latitude: Double, longitude: Double, accuracyMeters: Float, bssid: String?) {
        mutable.value = AttendanceMonitorSnapshot(
            running = true, latitude = latitude, longitude = longitude,
            accuracyMeters = accuracyMeters, wifiBssid = bssid
        )
    }
}

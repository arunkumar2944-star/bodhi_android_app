package com.prodigit.bodhiattendance.core.monitoring

import android.location.Location
import android.util.Log
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceLocationDto

/**
 * Read-only, server-configured office preflight. This is NOT attendance
 * authorization: the final server-side evidence verifier remains authoritative.
 */
object OfficeLocationPreflight {
    private const val TAG = "ATTENDANCE_PREFLIGHT"

    suspend fun check(token: String, latitude: Double, longitude: Double,
                      accuracyMeters: Float, connectedBssid: String?) {
        try {
            val response = RetrofitClient.attendanceApi
                .getActiveAttendanceConfig("Bearer $token")
            val body = response.body()
            if (!response.isSuccessful || body?.success != true) {
                Log.w(TAG, "Active CRM configuration unavailable: HTTP ${response.code()}")
                return
            }
            val offices = body.data?.locations.orEmpty().filter { it.isEnabled }
            Log.i(TAG, "Loaded ${offices.size} enabled CRM offices; version=${body.data?.configurationVersion}")
            val matched = offices.mapNotNull { office ->
                val distance = FloatArray(1)
                Location.distanceBetween(latitude, longitude, office.latitude, office.longitude, distance)
                val configuredAccuracy = office.maxAccuracyMeters
                if (configuredAccuracy <= 0.0) {
                    Log.w(TAG, "Office ${office.id} has no usable accuracy policy; skipping")
                    null
                } else if (distance[0] > office.geofenceRadiusMeters ||
                    accuracyMeters > configuredAccuracy) {
                    null
                } else {
                    office to distance[0]
                }
            }.sortedBy { it.second }

            if (matched.isEmpty()) {
                Log.i(TAG, "No office satisfies CRM geofence and GPS accuracy thresholds")
                return
            }
            val office = matched.first().first
            val approvedBssid = connectedBssid?.let { bssid ->
                office.wifi.any { it.bssid.equals(bssid, ignoreCase = true) }
            } ?: false
            Log.i(TAG, "GPS-preflight officeId=${office.id}, accuracy=${accuracyMeters}m, " +
                "wifiRequired=${office.wifiRequired}, wifiBssidConfigured=$approvedBssid, " +
                "wifiRequirementMet=${!office.wifiRequired || approvedBssid}, bleRequired=${office.bleRequired}; " +
                "event submission disabled pending full office policy verification")
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            Log.w(TAG, "Unable to load/verify CRM office configuration", error)
        }
    }
}

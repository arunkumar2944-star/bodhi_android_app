package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.location.Location
import androidx.core.location.LocationCompat
import com.google.gson.Gson
import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig

/** Immutable sensor/clock snapshot for manager review, never an attendance command. */
class AttendanceReviewEvidence(context: Context) {
    private val app = context.applicationContext
    suspend fun record(location: Location?, bssid: String?, disconnected: Boolean,
                       office: AttendanceLocationConfig?, gpsStatus: String, attendanceState: String) {
        val clock = TrustedTimeMonitor(app).read()
        TrustedTimeMonitor(app).report(clock)
        val readable = bssid?.takeIf { !disconnected &&
            it.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
            it != "02:00:00:00:00:00" && it != "00:00:00:00:00:00" }?.lowercase()
        val matched = readable != null && office?.wifiAccessPoints?.any { it.bssid.equals(readable, true) } == true
        val observed = (location?.elapsedRealtimeNanos ?: 0L) / 1_000_000L
        // Invalid/future sensor timestamps remain diagnostic failures at capture time.
        val captured = observed.takeIf { it > 0 && it <= clock.elapsedMs } ?: clock.elapsedMs
        AttendanceDiagnostics(app).record("ATTENDANCE_EVIDENCE", Gson().toJson(linkedMapOf(
            "version" to 1, "officeId" to office?.id, "attendanceState" to attendanceState,
            "gpsStatus" to gpsStatus, "mockDetected" to location?.let { LocationCompat.isMock(it) },
            "accuracyMeters" to location?.accuracy?.takeIf { location.hasAccuracy() && it.isFinite() && it >= 0 },
            "wifiConnected" to (!disconnected && readable != null), "wifiBssid" to readable,
            "wifiMatched" to matched, "clockStatus" to clock.status,
            "trustedTimeAvailable" to clock.available, "clockMismatch" to clock.mismatch,
            "clockDifferenceMs" to clock.clockDifferenceMs, "anchorUuid" to clock.anchorUuid,
            "anchorAgeMs" to clock.anchorAgeMs,
            "observationTrustedTimeMs" to clock.trustedTimeMs?.takeIf {
                clock.anchorAgeMs != null && clock.elapsedMs - captured <= clock.anchorAgeMs
            }?.minus(clock.elapsedMs - captured)
        )), captured)
    }
}

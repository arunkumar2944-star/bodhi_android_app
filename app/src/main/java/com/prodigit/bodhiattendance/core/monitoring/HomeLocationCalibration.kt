package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.prodigit.bodhiattendance.BuildConfig
import java.io.File

/** Explicit debug-only survey; never alters CRM or authorizes attendance. */
object HomeLocationCalibration {
    private var untilMs = 0L
    private var output: File? = null
    private var count = 0
    val active: Boolean get() = BuildConfig.DEBUG && untilMs > SystemClock.elapsedRealtime()
    val requested: Boolean get() = untilMs > 0L

    fun start(context: Context) {
        if (!BuildConfig.DEBUG) return
        count = 0
        output = File(context.filesDir, "home_location_calibration.csv").also {
            it.writeText("observed_at_utc,elapsed_ms,latitude,longitude,accuracy_m,connected_bssid,is_mock\n")
        }
        untilMs = SystemClock.elapsedRealtime() + 15 * 60_000L
        Log.i("GPS_CALIBRATION", "START: 15-minute survey; attendance evaluation paused")
        AttendanceVerificationSignals.request("home calibration")
    }

    @Synchronized fun record(location: Location?, bssid: String?) {
        val now = java.time.Instant.now().toString()
        @Suppress("DEPRECATION")
        val mock = location?.isFromMockProvider ?: false
        val mac = bssid?.takeIf { it.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
            it != "02:00:00:00:00:00" && it != "00:00:00:00:00:00" } ?: "unavailable"
        val line = listOf(now, SystemClock.elapsedRealtime(), location?.latitude ?: "",
            location?.longitude ?: "", location?.accuracy ?: "", mac, mock).joinToString(",")
        output?.appendText(line + "\n")
        count++
        Log.i("GPS_CALIBRATION", "sample=" + count + "; " + line)
    }

    fun finish() {
        if (!requested) return
        untilMs = 0L
        Log.i("GPS_CALIBRATION", "COMPLETE: samples=" + count + "; home_location_calibration.csv saved; normal monitoring resumes")
    }
}

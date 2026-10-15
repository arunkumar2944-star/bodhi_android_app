package com.prodigit.bodhiattendance.core.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.channels.Channel

internal object AttendanceVerificationSignals {
    val triggers = Channel<String>(Channel.CONFLATED)
    fun request(reason: String) { triggers.trySend(reason) }
}

class OfficeGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w("ATTENDANCE_MONITOR", "Geofence error=" + event.errorCode)
            return
        }
        if (AttendancePrivacyPolicy.isPaused(context)) return
        // Alerts wake an already validated monitoring loop; never start a new location service in the background.
        AttendanceVerificationSignals.request("geofence transition")
    }
}

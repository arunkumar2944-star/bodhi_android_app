package com.prodigit.bodhiattendance.core.monitoring

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Phase 1: Background GPS / connected Wi-Fi observation only.
 *
 * Never writes attendance events. The server configuration and
 * attendance state machine must verify evidence first.
 */
class AttendancePresenceService : Service(), LocationListener {
    private val tag = "ATTENDANCE_MONITOR"
    private lateinit var locationManager: LocationManager
    private var subscribed = false

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Automatic attendance monitoring", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasLocationPermission()) {
            Log.w(tag, "Location permission missing; monitoring not started")
            stopSelf()
            return START_NOT_STICKY
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.prodigit.bodhiattendance.R.mipmap.ic_launcher)
            .setContentTitle("Bodhi attendance")
            .setContentText("Monitoring office presence")
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            if (!subscribed) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 15000L, 3f, this, Looper.getMainLooper()
                )
                subscribed = true
                Log.i(tag, "GPS observer registered; awaiting verified office evidence")
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Location foreground service permission denied", e)
            stopSelf()
            return START_NOT_STICKY
        } catch (e: RuntimeException) {
            Log.e(tag, "Unable to start location foreground monitoring", e)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        @Suppress("DEPRECATION")
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val wifi = wifiManager.connectionInfo
        Log.d(tag, "GPS fix: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m; connectedBssid=${wifi?.bssid ?: "unavailable"}")
        // No automatic event here: next integration stage must match the
        // company office and validate full per-office policy first.
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        if (subscribed) locationManager.removeUpdates(this)
        subscribed = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "attendance_presence"
        private const val NOTIFICATION_ID = 2401
        fun start(context: Context) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
                Log.w("ATTENDANCE_MONITOR", "Cannot start; fine location permission missing")
                return
            }
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, AttendancePresenceService::class.java)
                )
            } catch (e: RuntimeException) {
                Log.e("ATTENDANCE_MONITOR", "Foreground monitoring start blocked", e)
            }
        }
    }
}

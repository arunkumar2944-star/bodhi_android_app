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
import android.os.SystemClock
import com.prodigit.bodhiattendance.core.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Observes GPS / connected Wi-Fi and evaluates CRM-configured attendance.
 * Event creation remains owned by the validated Room-first runtime coordinator.
 */
class AttendancePresenceService : Service(), LocationListener {
    private val tag = "ATTENDANCE_MONITOR"
    private lateinit var locationManager: LocationManager
    private val automaticEvaluator by lazy { AutomaticAttendanceEvaluator(applicationContext) }
    private var subscribed = false
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastConfigCheckElapsedMs = -60000L

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
                AttendanceMonitorStatus.running()
                Log.i(tag, "GPS observer registered; awaiting CRM-policy attendance evaluation")
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
        AttendanceMonitorStatus.observation(location.latitude, location.longitude, location.accuracy, wifi?.bssid)
        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastConfigCheckElapsedMs >= 60000L) {
            lastConfigCheckElapsedMs = elapsed
            val token = SessionManager(applicationContext).getToken()
            if (!token.isNullOrBlank()) {
                monitorScope.launch {
                    automaticEvaluator.observe(Location(location), wifi?.bssid)
                }
            } else {
                Log.w(tag, "No session token; office preflight skipped")
            }
        }
        Log.d(tag, "GPS fix: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m; connectedBssid=${wifi?.bssid ?: "unavailable"}")
        // AutomaticAttendanceEvaluator validates policy and identity before runtime evaluation.
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        if (subscribed) locationManager.removeUpdates(this)
        subscribed = false
        AttendanceMonitorStatus.stopped()
        monitorScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "attendance_presence"
        private const val NOTIFICATION_ID = 2401
        fun start(context: Context): Boolean {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
                Log.w("ATTENDANCE_MONITOR", "Cannot start; fine location permission missing")
                return false
            }
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, AttendancePresenceService::class.java)
                )
                return true
            } catch (e: RuntimeException) {
                Log.e("ATTENDANCE_MONITOR", "Foreground monitoring start blocked", e)
                return false
            }
        }
    }
}

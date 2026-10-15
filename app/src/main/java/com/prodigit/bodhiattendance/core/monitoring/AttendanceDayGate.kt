package com.prodigit.bodhiattendance.core.monitoring

import android.Manifest
import android.app.PendingIntent
import android.app.job.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.*
import com.prodigit.bodhiattendance.data.remote.DailyEligibility
import com.prodigit.bodhiattendance.data.repository.AttendanceConfigRepositoryImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Network checks never request the employee's coordinates. Office alerts use Android geofences. */
object AttendanceDayGate {
    private const val JOB_ID = 25302
    private val lock = Mutex()
    private val statusFlow = kotlinx.coroutines.flow.MutableStateFlow("NOT_VERIFIED")
    val state: kotlinx.coroutines.flow.StateFlow<String> = statusFlow
    private fun scheduler(c: Context) = requireNotNull(c.getSystemService(JobScheduler::class.java))
    private fun prefs(c: Context) = c.getSharedPreferences("attendance_day_gate", Context.MODE_PRIVATE)
    fun schedule(c: Context) {
        if (SessionManager(c).getUserId() <= 0) return
        if (scheduler(c).getPendingJob(JOB_ID) == null) scheduler(c).schedule(
            JobInfo.Builder(JOB_ID, ComponentName(c, AttendanceDayJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
                .setPeriodic(15 * 60_000L).build())
        scheduler(c).schedule(JobInfo.Builder(JOB_ID + 1, ComponentName(c, AttendanceDayJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
    }
    suspend fun fetch(c: Context): DailyEligibility {
        val expectedUser = SessionManager(c).getUserId()
        val device = DeviceIdManager(c).getDeviceId() ?: error("Registered device missing")
        val token = AndroidTokenManager(c).usableAccessToken() ?: error("Session unavailable")
        val started = android.os.SystemClock.elapsedRealtime()
        val response = RetrofitClient.mobileUiApi.dayEligibility("Bearer $token", device)
        val body = response.body()
        val result = body?.data
        check(response.isSuccessful && body?.success == true && result != null) { "Daily eligibility unavailable" }
        check(expectedUser > 0 && result.userId == expectedUser && result.userId == SessionManager(c).getUserId() && result.monitoringMode in setOf("PAUSED", "AWAITING_ARRIVAL", "ACTIVE"))
        check(android.os.SystemClock.elapsedRealtime() - started < 300_000L) { "Daily eligibility response expired" }
        check(result.validUntilMs > result.serverTimeMs && result.validUntilMs - result.serverTimeMs <= 300_000L)
        statusFlow.value = result.status
        prefs(c).edit().putString("status", result.status).putString("date", result.date)
            .putString("mode", result.monitoringMode).apply()
        return result
    }
    fun arrivalAlertsArmed(c: Context): Boolean = prefs(c).getBoolean("arrival_alerts", false)
    fun status(c: Context): String = prefs(c).getString("status", "NOT_VERIFIED") ?: "NOT_VERIFIED"
    private fun arrivalIntent(c: Context): PendingIntent = PendingIntent.getBroadcast(c, 25302,
        Intent(c, AttendanceArrivalReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
    fun clearArrivalAlerts(c: Context): Task<Void> {
        prefs(c).edit().putBoolean("arrival_alerts", false).apply()
        return LocationServices.getGeofencingClient(c).removeGeofences(arrivalIntent(c))
    }
    suspend fun removeArrivalAlerts(c: Context) { clearArrivalAlerts(c).awaitDayResult() }
    suspend fun armArrivalAlerts(c: Context) {
        val expectedUser = SessionManager(c).getUserId()
        check(expectedUser > 0)
        check(AttendancePrivacyPolicy.hasBackgroundLocation(c)) { "Background location is required for office arrival alerts" }
        check(ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        val token = AndroidTokenManager(c).usableAccessToken() ?: error("Session unavailable")
        val offices = AttendanceConfigRepositoryImpl(RetrofitClient.attendanceApi).getActiveAttendanceConfig(token).getOrThrow()
            .filter { it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0 && it.geofenceRadiusMeters.isFinite() && it.geofenceRadiusMeters > 0 }.take(100)
        currentCoroutineContext().ensureActive()
        check(SessionManager(c).getUserId() == expectedUser)
        removeArrivalAlerts(c)
        if (offices.isEmpty()) return
        val request = GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(offices.map { Geofence.Builder().setRequestId("arrival:" + it.id)
                .setCircularRegion(it.latitude, it.longitude, it.geofenceRadiusMeters.toFloat())
                .setExpirationDuration(20 * 60 * 1000L)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setNotificationResponsiveness(60_000).build() }).build()
        // This flag only permits a wake-up; the service must fetch fresh eligibility before sensors.
        prefs(c).edit().putBoolean("arrival_alerts", true).apply()
        try {
            val registration = LocationServices.getGeofencingClient(c).addGeofences(request, arrivalIntent(c))
            registration.addOnSuccessListener { if (SessionManager(c).getUserId() != expectedUser) clearArrivalAlerts(c) }
            registration.awaitDayResult()
        }
        catch (e: Exception) { prefs(c).edit().putBoolean("arrival_alerts", false).apply(); throw e }
    }
    suspend fun refresh(c: Context): Boolean = lock.withLock {
        if (SessionManager(c).getUserId() <= 0) return@withLock true
        if (AttendancePrivacyPolicy.isPaused(c) && !AttendancePrivacyPolicy.needsMorningReconciliation(c)) return@withLock true
        if (!AttendanceBackgroundSync.run(c, diagnostics = false)) return@withLock false
        val policy = fetch(c)
        AttendancePrivacyPolicy.scheduleNextMorning(c)
        if (policy.monitoringMode == "PAUSED") {
            if (policy.status == "CHECKED_OUT") AttendancePrivacyPolicy.pauseAfterCheckout(c)
            requireNotNull(c.getSystemService(android.app.NotificationManager::class.java)).cancel(25301)
            removeArrivalAlerts(c)
            c.stopService(Intent(c, AttendancePresenceService::class.java))
        } else if (!AttendanceMonitorStatus.state.value.running) {
            if (!AttendancePrivacyPolicy.hasBackgroundLocation(c) ||
                ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                removeArrivalAlerts(c)
                AttendancePrivacyPolicy.notifyMorning(c)
                return@withLock true
            }
            // Await office entry instead of starting routine GPS checks from home.
            armArrivalAlerts(c)
        }
        true
    }
    fun cancelScheduled(c: Context) {
        scheduler(c).cancel(JOB_ID); scheduler(c).cancel(JOB_ID + 1)
        prefs(c).edit().clear().apply()
        statusFlow.value = "NOT_VERIFIED"
        LocationServices.getGeofencingClient(c).removeGeofences(arrivalIntent(c))
    }
}

class AttendanceDayJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tasks = mutableMapOf<Int, Job>()
    override fun onStartJob(params: JobParameters): Boolean {
        tasks[params.jobId] = scope.launch {
            val ok = try { AttendanceDayGate.refresh(applicationContext) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("ATTENDANCE_DAY", "Eligibility check deferred; no arrival tracking authorized", e); false }
            jobFinished(params, !ok)
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { tasks.remove(params.jobId)?.cancel(); return true }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

class AttendanceArrivalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        if (SessionManager(context).getUserId() <= 0 || !AttendanceDayGate.arrivalAlertsArmed(context)) return
        // An OS office-entry alert permits service startup; CRM and GPS/Wi-Fi still decide attendance.
        AttendancePresenceService.startFromOfficeEntry(context)
    }
}
private suspend fun <T> Task<T>.awaitDayResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

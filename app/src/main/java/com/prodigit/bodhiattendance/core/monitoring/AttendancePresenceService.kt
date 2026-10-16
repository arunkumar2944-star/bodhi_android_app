package com.prodigit.bodhiattendance.core.monitoring

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.location.Location
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import com.prodigit.bodhiattendance.data.repository.AttendanceConfigRepositoryImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Wi-Fi/geofence triggers plus bounded fixes. No continuous GPS subscription. */
class AttendancePresenceService : Service() {
    private val tag = "ATTENDANCE_MONITOR"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val diagnostics by lazy { AttendanceDiagnostics(applicationContext) }
    private val offlineSession by lazy { OfflineWorkSessionStore(applicationContext) }
    private val evaluator by lazy { AutomaticAttendanceEvaluator(applicationContext, requireNotNull(workDate)) }
    private val arrivals by lazy { ArrivalCandidateRecorder(applicationContext) }
    @Volatile private var arrivalOffices = emptyList<com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig>()
    @Volatile private var wifiSignal: Pair<String, Long>? = null
    private val evidenceJournal by lazy { PresenceEvidenceJournal(applicationContext) }
    private val tokens by lazy { AndroidTokenManager(applicationContext) }
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val fences by lazy { LocationServices.getGeofencingClient(this) }
    private val connectivity by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private var worker: Job? = null
    private var arrivalUploadJob: Job? = null
    private var registered = false
    private var recoveryNetworkRegistered = false
    private val recoveryWakeups = Channel<Unit>(Channel.CONFLATED)
    private val recoveryCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            recoveryWakeups.trySend(Unit)
            AttendanceVerificationSignals.request("Network available — revalidate offline work session")
            AttendanceBackgroundSync.schedule(applicationContext)
        }
    }
    private var workDate: String? = null
    private var lastWifiSignature: String? = null
    private val fenceIntent by lazy {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        PendingIntent.getBroadcast(this, 2402, Intent(this, OfficeGeofenceReceiver::class.java), flags)
    }
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = signalWifi("Wi-Fi available")
        override fun onLost(network: Network) = signalWifi("Wi-Fi lost")
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val signature = network.toString() + ":" + readBssid()
            if (signature != lastWifiSignature) {
                lastWifiSignature = signature
                signalWifi("Wi-Fi capabilities/BSSID changed")
            }
        }
    }
    private fun signalWifi(reason: String) {
        if (AttendancePrivacyPolicy.isPaused(this)) return
        val bssid = readBssid()
        if (reason == "Wi-Fi lost") wifiSignal = null
        else if (bssid != null && !bssid.equals(wifiSignal?.first, true)) {
            wifiSignal = bssid to SystemClock.elapsedRealtime()
        }
        val observed = wifiSignal
        scope.launch {
            diagnostics.record("WIFI_TRIGGER", reason)
            if (reason == "Wi-Fi lost") arrivals.resetContinuity()
            try {
                if (!HomeLocationCalibration.requested && observed != null) arrivals.record(observed.first.takeUnless { isWifiDisconnected() }, observed.second, arrivalOffices)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("ARRIVAL_EVIDENCE", "Arrival observation deferred", e) }
        }
        AttendanceVerificationSignals.request(reason)
    }

    override fun onCreate() {
        super.onCreate()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Automatic attendance monitoring", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (worker?.isActive == true) return START_NOT_STICKY
        if (intent?.action != ACTION_VISIBLE_START || !hasFineLocation() || AttendancePrivacyPolicy.isPaused(this)) {
            stopSelf(); return START_NOT_STICKY
        }
        workDate = intent?.getStringExtra("work_date")
        if (workDate == null) { stopSelf(); return START_NOT_STICKY }
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.prodigit.bodhiattendance.R.mipmap.ic_launcher)
                .setContentTitle("Bodhi attendance")
                .setContentText("Monitoring attendance for the work session you started")
                .setOngoing(true).build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(NOTIFICATION_ID, notification)
            if (!recoveryNetworkRegistered) {
                connectivity.registerDefaultNetworkCallback(recoveryCallback)
                recoveryNetworkRegistered = true
            }
            if (worker?.isActive != true) {
                worker = scope.launch {
                    try {
                    val day = awaitDailyEligibility() ?: return@launch
                    if (day.date != workDate || day.monitoringMode == "PAUSED") {
                        diagnostics.record("POLICY_PAUSED", "CRM daily status=${day.status}; monitoring stopped")
                        stopSelf(); return@launch
                    }
                    if (AttendancePrivacyPolicy.isPaused(this@AttendancePresenceService)) { stopSelf(); return@launch }
                    if (!registered) {
                        connectivity.registerNetworkCallback(NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), callback)
                        registered = true
                    }
                    AttendanceMonitorStatus.running()
                    showVerificationNotification("Monitoring attendance for the work session you started")
                    arrivals.resetContinuity()
                    signalWifi("startup")
                    verificationLoop()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        Log.e(tag, "Monitoring worker stopped", e)
                        stopSelf()
                    }
                }
            }
        } catch (e: RuntimeException) {
            Log.e(tag, "Monitoring start blocked", e)
            stopSelf(); return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun showVerificationNotification(message: String) {
        requireNotNull(getSystemService(NotificationManager::class.java)).notify(NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.prodigit.bodhiattendance.R.mipmap.ic_launcher)
                .setContentTitle("Bodhi attendance")
                .setContentText(message).setOngoing(true).setOnlyAlertOnce(true).build())
    }

    /** Retry online policy or use a bounded, previously server-authorized offline session. */
    private suspend fun awaitDailyEligibility(): com.prodigit.bodhiattendance.data.remote.DailyEligibility? {
        var attempt = 0
        while (currentCoroutineContext().isActive) {
            if (AttendancePrivacyPolicy.isPaused(this) ||
                com.prodigit.bodhiattendance.core.session.SessionManager(this).getUserId() <= 0) {
                stopSelf()
                return null
            }
            // Only network changes during this attempt should accelerate its next retry.
            while (recoveryWakeups.tryReceive().isSuccess) { /* Drain earlier connectivity signals. */ }
            try {
                val activeNetwork = connectivity.activeNetwork
                val networkReady = activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                if (!networkReady) {
                    val wasOffline = OfflineWorkSessionStore.isOffline()
                    val cached = offlineSession.useOffline()
                    if (cached != null && cached.lease.workDate == workDate) {
                        AttendanceMonitorStatus.running()
                        if (!wasOffline) diagnostics.record("OFFLINE_AUTHORIZED", "Connectivity unavailable; using bounded cached work session")
                        return cached.eligibility
                    }
                }
                val wasOffline = OfflineWorkSessionStore.isOffline()
                val day = AttendanceDayGate.fetch(this)
                offlineSession.online()
                if (attempt > 0 || wasOffline) {
                    diagnostics.record("API_RECOVERED", "Daily eligibility recovered after $attempt failed attempts; checking current CRM status")
                    AttendanceBackgroundSync.schedule(this)
                }
                return day
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (e is EligibilityHttpFailure && !EligibilityRecoveryPolicy.shouldRetryHttp(e.status)) {
                    OfflineWorkSessionStore.clear(this, "CRM denied access — monitoring stopped")
                    diagnostics.record("API_BLOCKED", "Daily eligibility denied HTTP ${e.status}; monitoring stopped")
                    AttendanceBackgroundSync.schedule(this)
                    stopSelf()
                    return null
                }
                val temporary = e is java.io.IOException ||
                    (e is EligibilityHttpFailure && (e.status >= 500 || e.status in setOf(408, 429)))
                if (temporary) {
                    val previouslyOffline = OfflineWorkSessionStore.isOffline()
                    val cached = offlineSession.useOffline()
                    if (cached != null && cached.lease.workDate == workDate) {
                        AttendanceMonitorStatus.running()
                        showVerificationNotification("Offline attendance — saved on phone, pending CRM validation")
                        if (!previouslyOffline) diagnostics.record("OFFLINE_AUTHORIZED", "Using bounded work-session authorization; validUntilMs=${cached.lease.validUntilMs}")
                        return cached.eligibility
                    }
                } else {
                    OfflineWorkSessionStore.clear(this, "CRM policy could not be validated — connect to CRM")
                }
                attempt = (attempt + 1).coerceAtMost(1000)
                val delayMs = EligibilityRecoveryPolicy.retryDelayMillis(attempt)
                AttendanceMonitorStatus.waitingForEligibility()
                showVerificationNotification("Verification pending — reconnecting to CRM; GPS paused")
                val cause = if (e is EligibilityHttpFailure) "HTTP ${e.status}" else e.javaClass.simpleName
                diagnostics.record("API_RETRY", "Daily eligibility unavailable ($cause); attempt=$attempt; retryMs=$delayMs; service retained and GPS paused")
                Log.w(tag, "Daily eligibility unavailable ($cause); service retained; retry in ${delayMs}ms")
                withTimeoutOrNull(delayMs) { recoveryWakeups.receive() }
            }
        }
        return null
    }

    private suspend fun verificationLoop() {
        var waitMs = RECOVERY_MS
        var gpsRetries = 0
        while (currentCoroutineContext().isActive) {
            if (com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeProvider.getInstance(this@AttendancePresenceService)
                    .runtimeSession.getSnapshot().mode != com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeMode.READY) {
                stopSelf()
                return
            }
            if (com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeProvider.getInstance(this@AttendancePresenceService)
                    .runtimeSession.getSnapshot().attendanceState == com.prodigit.bodhiattendance.domain.attendance.AttendanceState.CHECKED_OUT) {
                AttendancePrivacyPolicy.pauseAfterCheckout(this@AttendancePresenceService)
                stopSelf()
                return
            }
            val reason = withTimeoutOrNull(waitMs) { AttendanceVerificationSignals.triggers.receive() }
                ?: "recovery/policy deadline"
            try {
                if (AttendancePrivacyPolicy.isPaused(this@AttendancePresenceService) ||
                    com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeProvider.getInstance(this@AttendancePresenceService)
                        .runtimeSession.getSnapshot().attendanceState == com.prodigit.bodhiattendance.domain.attendance.AttendanceState.CHECKED_OUT) {
                    AttendancePrivacyPolicy.pauseAfterCheckout(this@AttendancePresenceService)
                    stopSelf()
                    return
                }
                val day = awaitDailyEligibility() ?: return
                if (day.date != workDate || day.monitoringMode == "PAUSED") {
                    diagnostics.record("POLICY_PAUSED", "CRM daily status=${day.status}; monitoring stopped")
                    stopSelf(); return
                }
                AttendanceMonitorStatus.running()
                showVerificationNotification(if (OfflineWorkSessionStore.isOffline()) "Offline attendance — saved on phone, pending CRM validation"
                    else "Monitoring attendance for the work session you started")
                Log.i(tag, "Verification burst: " + reason)
                diagnostics.record("VERIFICATION", "Verification burst: " + reason)
                TrustedTimeMonitor(applicationContext).report()
                // Refresh office evidence policy; no background geofences are registered.
                val freshConfiguration = if (OfflineWorkSessionStore.isOffline()) false else try {
                    withTimeoutOrNull(10_000L) { refreshOfficeConfiguration() } == true
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { Log.w(tag, "Office configuration refresh deferred", e); false }
                if (freshConfiguration) offlineSession.authorize(day, arrivalOffices)
                if (AttendancePrivacyPolicy.isPaused(this@AttendancePresenceService)) { stopSelf(); return }
                // Persist approved Wi-Fi arrival before waiting for any GPS fix.
                try {
                    if (!HomeLocationCalibration.requested) {
                        val bssid = readBssid()
                        val signal = wifiSignal?.takeIf { it.first.equals(bssid, true) &&
                            SystemClock.elapsedRealtime() - it.second in 0..60_000L }
                        arrivals.record(bssid.takeUnless { isWifiDisconnected() }, signal?.second ?: SystemClock.elapsedRealtime(), arrivalOffices)
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { Log.w("ARRIVAL_EVIDENCE", "Arrival evidence retained for retry", e) }
                if (AttendancePrivacyPolicy.isPaused(this@AttendancePresenceService)) { stopSelf(); return }
                val sensorBudget = if (OfflineWorkSessionStore.isOffline()) offlineSession.sensorBudgetMs() else 35_000L
                if (sensorBudget <= 0L) { waitMs = MIN_RETRY_MS; continue }
                val source = CancellationTokenSource()
                val location = try {
                    withTimeoutOrNull(sensorBudget.coerceAtMost(35_000L)) {
                        fused.getCurrentLocation(CurrentLocationRequest.Builder()
                            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                            .setMaxUpdateAgeMillis(0L).setDurationMillis(sensorBudget.coerceIn(1L, 30_000L)).build(), source.token).awaitResult()
                    }
                } finally { source.cancel() }
                // A one-shot fix may be inaccurate indoors. If so, briefly sample
                // additional Fused Location fixes; CRM accuracy policy remains authoritative.
                val refinedLocation = if (location != null && GpsEvidenceStatus.rejection(location) == null && location.hasAccuracy() &&
                    location.accuracy > REFINE_ABOVE_METERS) {
                    val remaining = if (OfflineWorkSessionStore.isOffline()) offlineSession.sensorBudgetMs() else REFINE_WINDOW_MS
                    if (remaining > 0L) withTimeoutOrNull(remaining) { improvePoorFix(location) } ?: location else location
                } else location
                val calibrating = HomeLocationCalibration.requested
                if (calibrating) HomeLocationCalibration.record(refinedLocation, readBssid())
                if (refinedLocation != null) {
                    val bssid = readBssid()
                    AttendanceMonitorStatus.observation(refinedLocation.latitude, refinedLocation.longitude, refinedLocation.accuracy, bssid)
                    if (!calibrating) {
                        evaluator.observe(refinedLocation, bssid, wifiDisconnected = isWifiDisconnected())
                    }
                } else {
                    Log.w(tag, "No fresh GPS fix; attendance remains unknown")
                    diagnostics.record("GPS", "No fresh GPS fix; attendance remains unknown")
                    val bssid = readBssid()
                    val disconnected = isWifiDisconnected()
                    val reviewOffice = arrivalOffices.filter { office -> !disconnected && bssid != null &&
                        office.wifiAccessPoints.any { it.bssid.equals(bssid, true) } }.singleOrNull()
                    AttendanceReviewEvidence(applicationContext).record(null, bssid, disconnected, reviewOffice,
                        "UNAVAILABLE", com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeProvider
                            .getInstance(applicationContext).runtimeSession.getSnapshot().attendanceState?.name ?: "UNKNOWN")
                }
                if (!calibrating && com.prodigit.bodhiattendance.domain.attendance.AttendanceEngineRuntimeProvider.getInstance(this@AttendancePresenceService)
                        .runtimeSession.getSnapshot().attendanceState == com.prodigit.bodhiattendance.domain.attendance.AttendanceState.CHECKED_OUT) {
                    diagnostics.record("PRIVACY_PAUSED", "Checked out; tracking stopped; next workday requires manual app opening")
                    AttendancePrivacyPolicy.pauseAfterCheckout(this@AttendancePresenceService)
                    stopSelf()
                    return
                }
                if (!calibrating && !OfflineWorkSessionStore.isOffline()) {
                    try { diagnostics.upload() }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { Log.w(tag, "Diagnostics retained for retry") }
                    uploadEvidenceSafely()
                    scheduleArrivalUpload()
                }
                val gpsNeedsRetry = refinedLocation == null || evaluator.needsBetterGps
                val due = if (refinedLocation != null) evaluator.nextVerificationElapsedMs else null
                waitMs = if (evaluator.needsContinuityChecks || arrivals.hasPending()) {
                    gpsRetries = 0
                    // Reconfirm departure promptly; do not let the normal recovery delay invalidate the pair.
                    15_000L
                } else if (gpsNeedsRetry && gpsRetries < MAX_GPS_RETRIES) {
                    gpsRetries++
                    Log.i(tag, "Automatic GPS retry " + gpsRetries + "/" + MAX_GPS_RETRIES)
                    GPS_RETRY_MS
                } else {
                    if (gpsNeedsRetry) Log.w(tag, "GPS retry budget reached; waiting for recovery or sensor trigger")
                    gpsRetries = 0
                    due?.let { (it - SystemClock.elapsedRealtime()).coerceIn(MIN_RETRY_MS, RECOVERY_MS) }
                        ?: RECOVERY_MS
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.w(tag, "Verification unavailable; no absence inferred", e)
                diagnostics.record("VERIFICATION", "Verification failed; no absence inferred")
                waitMs = RECOVERY_MS
            }
            if (HomeLocationCalibration.requested) {
                if (HomeLocationCalibration.active) {
                    // Roughly one minute between bounded survey fixes.
                    waitMs = 45_000L
                } else {
                    HomeLocationCalibration.finish()
                    waitMs = MIN_RETRY_MS
                }
            }
            // Coalesce callbacks generated during a fix and bound rapid Wi-Fi flapping.
            delay(MIN_RETRY_MS)
        }
    }

    /**
     * Only refine a poor one-shot fused fix. This does not lower office accuracy
     * requirements or turn uncertainty into absence. The callback is always removed
     * when the bounded observation finishes or is cancelled.
     */
    private suspend fun improvePoorFix(initial: Location): Location {
        val fixes = Channel<Location>(Channel.CONFLATED)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { fixes.trySend(it) }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(750L)
            .setMaxUpdateDelayMillis(0L)
            .build()
        var subscribed = false
        var best = initial
        try {
            fused.requestLocationUpdates(request, callback, mainLooper).awaitResult()
            subscribed = true
            withTimeoutOrNull(REFINE_WINDOW_MS) {
                while (currentCoroutineContext().isActive) {
                    val fix = fixes.receive()
                    val mocked = androidx.core.location.LocationCompat.isMock(fix)
                    // Surface mock fixes to the evaluator instead of silently keeping an older real fix.
                    if (mocked) {
                        best = fix
                        break
                    }
                    val age = SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos
                    if (!mocked && fix.hasAccuracy() && fix.accuracy.isFinite() &&
                        fix.accuracy >= 0f && age in 0L..15_000_000_000L &&
                        (!best.hasAccuracy() || fix.accuracy < best.accuracy)) {
                        best = fix
                    }
                    if (best.hasAccuracy() && best.accuracy <= REFINE_TARGET_METERS) break
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(tag, "Short fused-location refinement unavailable; retaining original fix", e)
        } finally {
            if (subscribed) fused.removeLocationUpdates(callback)
            fixes.close()
        }
        Log.i(tag, "Fused fix refinement: initial=${initial.accuracy}m, best=${best.accuracy}m")
        return best
    }

    private fun scheduleArrivalUpload() {
        if (arrivalUploadJob?.isActive == true) return
        arrivalUploadJob = scope.launch {
            try {
                arrivals.upload()
                if (!ProvisionalArrivalRecorder(applicationContext).upload()) AttendanceBackgroundSync.schedule(applicationContext)
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("ARRIVAL_EVIDENCE", "Arrival evidence upload deferred", e) }
        }
    }

    private suspend fun uploadEvidenceSafely() {
        // An audit-only upload. Failure never blocks normal attendance processing.
        val token = tokens.usableAccessToken() ?: return
        val deviceId = com.prodigit.bodhiattendance.core.device.DeviceIdManager(applicationContext)
            .getDeviceId() ?: return
        val pending = evidenceJournal.pending()
        if (pending.isEmpty()) return
        try {
            val batch = com.prodigit.bodhiattendance.data.remote.PresenceEvidenceBatch(
                deviceId,
                pending.map {
                    com.prodigit.bodhiattendance.data.remote.PresenceEvidenceItem(
                        it.id, it.officeId, it.elapsedMs, it.latitude, it.longitude,
                        it.accuracyMeters, it.wifiMatched, it.classification
                    )
                }
            )
            val response = com.prodigit.bodhiattendance.core.network.RetrofitClient
                .presenceEvidenceApi.upload("Bearer $token", batch)
            if (response.isSuccessful && response.body()?.success == true) {
                evidenceJournal.markUploaded(response.body()?.accepted.orEmpty())
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Log.w(tag, "Evidence upload deferred; retained for next verification", e) }
    }

    private suspend fun refreshOfficeConfiguration(): Boolean {
        val token = tokens.usableAccessToken() ?: return false
        if (arrivalOffices.isEmpty()) {
            arrivalOffices = com.prodigit.bodhiattendance.data.local.repository.AttendanceConfigCacheRepository(
                com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase.getInstance(applicationContext).attendanceConfigDao()
            ).getCachedConfiguration()
        }
        val remote = AttendanceConfigRepositoryImpl(RetrofitClient.attendanceApi).getActiveAttendanceConfig(token)
        if (remote.isSuccess) {
            arrivalOffices = remote.getOrThrow()
            com.prodigit.bodhiattendance.data.local.repository.AttendanceConfigCacheRepository(
                com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase.getInstance(applicationContext).attendanceConfigDao()
            ).replaceConfiguration(arrivalOffices)
            return true
        } else if (arrivalOffices.isEmpty()) {
            arrivalOffices = com.prodigit.bodhiattendance.data.local.repository.AttendanceConfigCacheRepository(
                com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase.getInstance(applicationContext).attendanceConfigDao()
            ).getCachedConfiguration()
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun isWifiDisconnected(): Boolean = try {
        connectivity.allNetworks.none { network ->
            connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    } catch (_: SecurityException) { false }

    private fun readBssid(): String? = try {
        (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo?.bssid
    } catch (_: SecurityException) { null }

    private fun hasFineLocation() = ContextCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        if (registered) connectivity.unregisterNetworkCallback(callback)
        if (recoveryNetworkRegistered) connectivity.unregisterNetworkCallback(recoveryCallback)
        scope.cancel()
        fences.removeGeofences(fenceIntent)
            .addOnFailureListener { Log.w(tag, "Geofence cleanup failed", it) }
            .addOnCompleteListener {
                AttendanceDayGate.cancelLegacyAutomation(applicationContext).addOnCompleteListener {
                    AttendancePrivacyPolicy.terminateAfterCheckoutCleanup(applicationContext)
                }
            }
        stopForeground(STOP_FOREGROUND_REMOVE)
        AttendanceMonitorStatus.stopped()
        AttendanceMonitorStartGate.onServiceStopped()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_VISIBLE_START = "com.prodigit.bodhiattendance.VISIBLE_WORK_SESSION"
        private const val CHANNEL_ID = "attendance_presence"
        private const val NOTIFICATION_ID = 2401
        private const val RECOVERY_MS = 5 * 60_000L
        private const val MIN_RETRY_MS = 15_000L
        private const val GPS_RETRY_MS = 30_000L
        private const val REFINE_ABOVE_METERS = 20f
        private const val REFINE_TARGET_METERS = 10f
        private const val REFINE_WINDOW_MS = 12_000L
        private const val MAX_GPS_RETRIES = 3
        fun start(context: Context, workDate: String): Boolean {
            if (!AttendanceMonitorStartGate.isVisible()) return false
            if (AttendancePrivacyPolicy.isPaused(context)) return false
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) return false
            return try {
                ContextCompat.startForegroundService(context, Intent(context, AttendancePresenceService::class.java)
                    .setAction(ACTION_VISIBLE_START).putExtra("work_date", workDate))
                true
            } catch (e: RuntimeException) { Log.e("ATTENDANCE_MONITOR", "Monitoring start blocked", e); false }
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

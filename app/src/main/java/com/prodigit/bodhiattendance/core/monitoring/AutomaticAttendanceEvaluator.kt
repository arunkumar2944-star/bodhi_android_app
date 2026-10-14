package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.data.repository.AttendanceConfigRepositoryImpl
import com.prodigit.bodhiattendance.domain.attendance.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Evaluates fresh evidence only; unknown sensors/configuration never mean absence. */
class AutomaticAttendanceEvaluator(context: Context) {
    private val session = SessionManager(context.applicationContext)
    private val tokens = com.prodigit.bodhiattendance.core.session.AndroidTokenManager(context)
    private val devices = DeviceIdManager(context.applicationContext)
    private val provider = AttendanceEngineRuntimeProvider.getInstance(context)
    private val configuration = AttendanceConfigRepositoryImpl(RetrofitClient.attendanceApi)
    private val mutex = Mutex()
    private var boundOfficeId: Long? = null
    var nextVerificationElapsedMs: Long? = null
        private set

    suspend fun observe(location: Location, bssid: String?) = mutex.withLock {
        nextVerificationElapsedMs = null
        val tag = "ATTENDANCE_ENGINE"
        @Suppress("DEPRECATION")
        if (location.isFromMockProvider || !location.hasAccuracy() ||
            !location.accuracy.isFinite() || location.accuracy < 0f ||
            location.elapsedRealtimeNanos <= 0L ||
            SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos !in 0..60_000_000_000L) {
            Log.w(tag, "Ignoring mock, stale or invalid GPS evidence")
            return@withLock
        }
        val token = tokens.usableAccessToken()
        if (token.isNullOrBlank()) {
            Log.w(tag, "Evaluation paused: valid authentication required")
            return@withLock
        }
        val deviceId = devices.getDeviceId() ?: return@withLock
        val snapshot = provider.runtimeSession.getSnapshot()
        if (snapshot.mode != AttendanceEngineRuntimeMode.READY || snapshot.userId != session.getUserId() ||
            snapshot.deviceId != deviceId) {
            Log.w(tag, "Evaluation paused: startup runtime validation required")
            return@withLock
        }
        val offices = configuration.getActiveAttendanceConfig(token).getOrElse {
            Log.w(tag, "Evaluation paused: CRM configuration unavailable", it)
            return@withLock
        }
        fun distance(office: com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig): Float {
            val result = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, office.latitude, office.longitude, result)
            return result[0]
        }
        val office = if (boundOfficeId != null) offices.firstOrNull { it.id == boundOfficeId } else
            offices.filter { it.geofenceRadiusMeters > 0 && distance(it) <= it.geofenceRadiusMeters }
                .singleOrNull()
        if (office == null) {
            Log.i(tag, "Evaluation paused: no unambiguous CRM office; absence is not inferred")
            return@withLock
        }
        val accuracyLimit = office.maxAccuracyMeters
        if (accuracyLimit == null || !accuracyLimit.isFinite() || accuracyLimit <= 0 ||
            location.accuracy > accuracyLimit || !office.geofenceRadiusMeters.isFinite() || office.geofenceRadiusMeters <= 0) {
            Log.i(tag, "Evaluation paused: GPS accuracy does not satisfy CRM policy")
            return@withLock
        }
        val policy = office.toEnginePolicy().getOrElse {
            Log.w(tag, "Evaluation paused: ${it.message}")
            return@withLock
        }
        if (policy.bleRequired == true) {
            Log.w(tag, "Evaluation paused: required BLE evidence provider is not integrated")
            return@withLock
        }
        val readableBssid = bssid?.takeIf { it.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
            it != "02:00:00:00:00:00" && it != "00:00:00:00:00:00" }
        if (policy.wifiRequired == true && readableBssid == null && distance(office) <= office.geofenceRadiusMeters) {
            Log.i(tag, "Evaluation paused: Wi-Fi evidence unavailable; absence is not inferred")
            return@withLock
        }
        val inside = distance(office) <= office.geofenceRadiusMeters
        val wifiVerified = readableBssid != null && office.wifiAccessPoints.any { it.bssid.equals(readableBssid, true) }
        if (boundOfficeId == null && (!inside || (policy.wifiRequired == true && !wifiVerified))) {
            Log.i(tag, "Waiting for CRM office presence verification")
            return@withLock
        }
        boundOfficeId = office.id
        val evidence = AttendanceEngineEvidence(
            latitude = location.latitude, longitude = location.longitude,
            accuracyMeters = location.accuracy.toDouble(), geofenceVerified = inside,
            wifiVerified = wifiVerified, wifiBssid = readableBssid,
            capturedAtElapsedRealtimeMs = location.elapsedRealtimeNanos / 1_000_000L
        )
        val result = provider.runtimeSession.evaluate(token, session.getUserId(), session.getCompanyId(),
            deviceId, evidence, policy)
        val phase = provider.runtimeSession.getSnapshot().phase
        nextVerificationElapsedMs = when (phase) {
            is AttendanceEnginePhase.AbsenceGrace -> phase.absenceStartedElapsedRealtimeMs + policy.gracePeriodMs
            is AttendanceEnginePhase.BreakActive -> phase.absenceStartedElapsedRealtimeMs + policy.maximumAbsenceMs
            else -> null
        }
        Log.i(tag, "CRM officeId=${office.id}; evaluation=$result")
    }
}

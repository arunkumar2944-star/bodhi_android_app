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
    private val journal = PresenceEvidenceJournal(context.applicationContext)
    private val session = SessionManager(context.applicationContext)
    private val tokens = com.prodigit.bodhiattendance.core.session.AndroidTokenManager(context)
    private val devices = DeviceIdManager(context.applicationContext)
    private val provider = AttendanceEngineRuntimeProvider.getInstance(context)
    private val configuration = AttendanceConfigRepositoryImpl(RetrofitClient.attendanceApi)
    private val mutex = Mutex()
    private var boundOfficeId: Long? = null
    private var previousOutsideFixMs: Long? = null
    private var outsideFixCount = 0
    private var continuousOutsideSinceMs: Long? = null
    var needsContinuityChecks: Boolean = false
        private set
    var needsBetterGps: Boolean = false
        private set
    var nextVerificationElapsedMs: Long? = null
        private set

    suspend fun observe(location: Location, bssid: String?, wifiDisconnected: Boolean = false) = mutex.withLock {
        needsBetterGps = false
        needsContinuityChecks = false
        nextVerificationElapsedMs = null
        val tag = "ATTENDANCE_ENGINE"
        @Suppress("DEPRECATION")
        if (location.isFromMockProvider || !location.hasAccuracy() ||
            !location.accuracy.isFinite() || location.accuracy < 0f ||
            location.elapsedRealtimeNanos <= 0L ||
            SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos !in 0..60_000_000_000L) {
            previousOutsideFixMs = null
            continuousOutsideSinceMs = null
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
            previousOutsideFixMs = null
            continuousOutsideSinceMs = null
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
            // Retain uncertainty for review, never invent office presence.
            journal.record(location, null, false, "UNCERTAIN")
            Log.i(tag, "Evaluation paused: no unambiguous CRM office; absence is not inferred")
            return@withLock
        }
        val policy = office.toEnginePolicy().getOrElse {
            Log.w(tag, "Evaluation paused: ${it.message}")
            return@withLock
        }
        suspend fun uncertain(reason: String) {
            outsideFixCount = 0
            previousOutsideFixMs = null
            continuousOutsideSinceMs = null
            needsBetterGps = true
            val evidence = AttendanceEngineEvidence(latitude = location.latitude, longitude = location.longitude,
                accuracyMeters = location.accuracy.toDouble(), capturedAtElapsedRealtimeMs = location.elapsedRealtimeNanos / 1_000_000L,
                observationUncertain = true)
            provider.runtimeSession.evaluate(token, session.getUserId(), session.getCompanyId(), deviceId, evidence, policy)
            journal.record(location, office.id, false, "UNCERTAIN")
            Log.i(tag, "Evidence uncertain: $reason; no attendance event")
        }
        val accuracyLimit = office.maxAccuracyMeters
        if (accuracyLimit == null || !accuracyLimit.isFinite() || accuracyLimit <= 0 ||
            location.accuracy > accuracyLimit || !office.geofenceRadiusMeters.isFinite() || office.geofenceRadiusMeters <= 0) {
            uncertain("GPS accuracy=${location.accuracy}m, CRM limit=${accuracyLimit}m")
            return@withLock
        }
        if (policy.bleRequired == true) {
            uncertain("Required BLE provider is not integrated")
            return@withLock
        }
        val readableBssid = bssid?.takeIf { it.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
            it != "02:00:00:00:00:00" && it != "00:00:00:00:00:00" }
        val distanceMeters = distance(office).toDouble()
        val inside = distanceMeters <= office.geofenceRadiusMeters
        val wifiVerified = readableBssid != null && office.wifiAccessPoints.any { it.bssid.equals(readableBssid, true) }
        val definitelyOutside = distanceMeters - location.accuracy > office.geofenceRadiusMeters
        Log.i(tag, "Evidence officeId=${office.id}; distance=${distanceMeters}m; accuracy=${location.accuracy}m; radius=${office.geofenceRadiusMeters}m; wifiMatched=$wifiVerified; wifiDisconnected=$wifiDisconnected")
        if (inside && policy.wifiRequired == true && !wifiVerified) {
            uncertain("Inside GPS with missing or nonmatching office Wi-Fi")
            return@withLock
        }
        if (!inside) {
            if (!definitelyOutside || (policy.wifiRequired == true && (wifiVerified || (!wifiDisconnected && readableBssid == null)))) {
                uncertain("GPS boundary uncertainty or conflicting/unknown Wi-Fi")
                return@withLock
            }
            val now = location.elapsedRealtimeNanos / 1_000_000L
            // Evidence freshness guard, not a CRM attendance/grace setting.
            if (previousOutsideFixMs == null || now - previousOutsideFixMs!! !in 1..90_000L) {
                uncertain("Departure needs another fresh outside observation")
                previousOutsideFixMs = now
                outsideFixCount = 1
                continuousOutsideSinceMs = now
                return@withLock
            }
            previousOutsideFixMs = now
            outsideFixCount++
            needsContinuityChecks = snapshot.attendanceState in setOf(AttendanceState.WORKING, AttendanceState.ON_BREAK)
            val breakPhase = snapshot.phase as? AttendanceEnginePhase.BreakActive
            if (snapshot.attendanceState == AttendanceState.ON_BREAK && breakPhase != null &&
                now - breakPhase.absenceStartedElapsedRealtimeMs >= policy.maximumAbsenceMs &&
                now - (continuousOutsideSinceMs ?: now) < policy.maximumAbsenceMs) {
                Log.i(tag, "Checkout paused: continuous reliable absence not yet established after evidence gap/restart")
                return@withLock
            }
        } else {
            outsideFixCount = 0
            previousOutsideFixMs = null
            continuousOutsideSinceMs = null
        }
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
        journal.record(location, office.id, wifiVerified, if (inside) "PRESENT" else "ABSENT_CANDIDATE")
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

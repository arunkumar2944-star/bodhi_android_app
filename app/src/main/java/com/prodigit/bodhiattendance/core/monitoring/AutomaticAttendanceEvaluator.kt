package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.location.Location
import android.util.Log
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.data.repository.AttendanceConfigRepositoryImpl
import com.prodigit.bodhiattendance.domain.attendance.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Evaluates fresh evidence only; unknown sensors/configuration never mean absence. */
class AutomaticAttendanceEvaluator(context: Context, private val workDate: String) {
    private val arrivals = ArrivalCandidateRecorder(context.applicationContext)
    private val provisionalArrivals = ProvisionalArrivalRecorder(context.applicationContext)
    private val diagnostics = AttendanceDiagnostics(context.applicationContext)
    private val reviewEvidence = AttendanceReviewEvidence(context.applicationContext)
    private val journal = PresenceEvidenceJournal(context.applicationContext)
    private val session = SessionManager(context.applicationContext)
    private val tokens = com.prodigit.bodhiattendance.core.session.AndroidTokenManager(context)
    private val devices = DeviceIdManager(context.applicationContext)
    private val provider = AttendanceEngineRuntimeProvider.getInstance(context)
    private val configuration = AttendanceConfigRepositoryImpl(RetrofitClient.attendanceApi)
    private val mutex = Mutex()
    private var boundOfficeId: Long? = null
    private val departureEvidence = DepartureEvidenceTracker()
    private val contextForOffline = context.applicationContext
    private val offlineSession = OfflineWorkSessionStore(context.applicationContext)
    private val officeContext = AttendanceOfficeContext(context.applicationContext)
    private val boot = com.prodigit.bodhiattendance.data.local.security.BootSessionManager(context.applicationContext)
    private var workdayDeadlineElapsedMs: Long? = null
    fun setWorkdayDeadline(deadline: Long) { workdayDeadlineElapsedMs = deadline }
    private fun workdayEnded() = WorkdayBoundary.expired(workdayDeadlineElapsedMs, android.os.SystemClock.elapsedRealtime())
    var needsContinuityChecks: Boolean = false
        private set
    var needsBetterGps: Boolean = false
        private set
    var nextVerificationElapsedMs: Long? = null
        private set

    /** Daily eligibility notices a mismatch; authoritative bootstrap decides the replacement. */
    suspend fun refreshServerState(day: com.prodigit.bodhiattendance.data.remote.DailyEligibility): Boolean = mutex.withLock {
        if (OfflineWorkSessionStore.isOffline()) return@withLock true
        val expected = runCatching { AttendanceState.valueOf(day.attendanceState) }.getOrNull()
        if (day.date != workDate || expected == null) {
            diagnostics.record("STATE_RECONCILIATION_PENDING", "CRM workday/state could not be validated")
            return@withLock false
        }
        val device = devices.getDeviceId() ?: return@withLock false
        val token = tokens.usableAccessToken() ?: return@withLock false
        val result = provider.runtimeSession.refreshAuthoritativeState(
            userId = session.getUserId(), deviceId = device, expectedState = expected,
            settlePending = { AttendanceBackgroundSync.run(contextForOffline, diagnostics = false) },
            bootstrap = { before ->
                diagnostics.record("STATE_RECONCILIATION", "workDate=$workDate; local=${before.attendanceState}; CRM daily=$expected")
                val currentBoot = boot.getBootId()
                val sameDayOffice = officeContext.read(session.getUserId(), session.getCompanyId(), device, workDate, currentBoot)
                provider.reconciliationManager.reconcile(token, session.getUserId(), session.getCompanyId(), device,
                    resumePhase = before.phase.takeIf { sameDayOffice != null })
            }
        )
        when (result) {
            AttendanceRuntimeRefreshResult.Unchanged -> true
            is AttendanceRuntimeRefreshResult.Deferred -> {
                diagnostics.record("STATE_RECONCILIATION_PENDING", result.reason)
                AttendanceBackgroundSync.schedule(contextForOffline)
                false
            }
            is AttendanceRuntimeRefreshResult.Updated -> {
                departureEvidence.reset()
                boundOfficeId = result.bootstrap.officeId?.takeIf {
                    result.bootstrap.attendanceState in setOf(AttendanceState.WORKING, AttendanceState.ON_BREAK)
                }
                boundOfficeId?.let { office ->
                    officeContext.save(session.getUserId(), session.getCompanyId(), device, workDate, boot.getBootId(), office)
                }
                diagnostics.record("STATE_RECONCILED", "workDate=$workDate; state=${result.bootstrap.attendanceState}; fresh sensor evidence required before an event")
                true
            }
        }
    }

    suspend fun observe(location: Location, bssid: String?, wifiDisconnected: Boolean = false) = mutex.withLock {
        if (workdayEnded()) {
            diagnostics.record("WORKDAY_ENDED", "Evaluation blocked for expired workDate=$workDate")
            return@withLock
        }
        needsBetterGps = false
        needsContinuityChecks = false
        nextVerificationElapsedMs = null
        val tag = "ATTENDANCE_ENGINE"
        val rejection = GpsEvidenceStatus.rejection(location)
        val wasMocked = GpsEvidenceStatus.state.value.rejection == GpsEvidenceRejection.MOCK_LOCATION_DETECTED
        GpsEvidenceStatus.report(rejection)
        if (rejection != null) {
            reviewEvidence.record(location, bssid, wifiDisconnected, null, rejection.name,
                provider.runtimeSession.getSnapshot().attendanceState?.name ?: "UNKNOWN")
            departureEvidence.reset()
            needsBetterGps = true
            diagnostics.record(rejection.name, when (rejection) {
                GpsEvidenceRejection.MOCK_LOCATION_DETECTED ->
                    "Mock location detected; attendance events blocked. Stop fake GPS and wait for a fresh real location."
                GpsEvidenceRejection.GPS_STALE -> "GPS reading older than 60 seconds; no attendance event"
                GpsEvidenceRejection.GPS_INVALID -> "Invalid GPS coordinates, accuracy or elapsed timestamp; no attendance event"
            })
            return@withLock
        }
        if (wasMocked) diagnostics.record("GPS_RECOVERED", "Fresh valid non-mock GPS received; normal verification resumed")
        val offline = if (OfflineWorkSessionStore.isOffline()) offlineSession.current() else null
        if (OfflineWorkSessionStore.isOffline() && offline == null) {
            diagnostics.record("OFFLINE_EXPIRED", "No valid offline authorization; attendance evaluation paused")
            return@withLock
        }
        val token = if (offline != null) session.getToken() else tokens.usableAccessToken()
        if (token.isNullOrBlank()) {
            diagnostics.record("ENGINE", "Evaluation paused: valid authentication required")
            return@withLock
        }
        val deviceId = devices.getDeviceId() ?: return@withLock
        val snapshot = provider.runtimeSession.getSnapshot()
        if (snapshot.mode != AttendanceEngineRuntimeMode.READY || snapshot.userId != session.getUserId() ||
            snapshot.deviceId != deviceId) {
            diagnostics.record("ENGINE", "Evaluation paused: startup runtime validation required")
            return@withLock
        }
        if (boundOfficeId == null && snapshot.attendanceState in setOf(AttendanceState.WORKING, AttendanceState.ON_BREAK)) {
            boundOfficeId = officeContext.read(session.getUserId(), session.getCompanyId(), deviceId, workDate, boot.getBootId())
        }
        val offices = offline?.offices ?: configuration.getActiveAttendanceConfig(token).getOrElse { error ->
            if (error is retrofit2.HttpException && error.code() in setOf(401, 403)) {
                OfflineWorkSessionStore.clear(contextForOffline, "CRM denied office configuration access")
            }
            val transient = error is java.io.IOException || (error is retrofit2.HttpException && error.code() >= 500)
            val fallback = if (transient) offlineSession.useOffline() else null
            if (fallback == null) {
                departureEvidence.reset()
                needsBetterGps = true
                diagnostics.record("ENGINE", "Evaluation paused: CRM configuration unavailable")
                return@withLock
            }
            fallback.offices
        }
        fun distance(office: com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig): Float {
            val result = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, office.latitude, office.longitude, result)
            return result[0]
        }
        // A return may be to another active office of the same company.
        // Require unambiguous accurate presence plus that office's own Wi-Fi policy.
        val returnOffice = if (snapshot.attendanceState == AttendanceState.ON_BREAK) offices.filter { candidate ->
            candidate.geofenceRadiusMeters > 0 && distance(candidate) <= candidate.geofenceRadiusMeters &&
                candidate.maxAccuracyMeters?.let { location.accuracy <= it } == true &&
                (candidate.wifiRequired == false || (!wifiDisconnected && bssid != null &&
                    candidate.wifiAccessPoints.any { it.bssid.equals(bssid, true) }))
        }.singleOrNull() else null
        val office = returnOffice ?: if (boundOfficeId != null) offices.firstOrNull { it.id == boundOfficeId } else
            offices.filter { it.geofenceRadiusMeters > 0 && distance(it) <= it.geofenceRadiusMeters }
                .singleOrNull()
        if (office == null) {
            // Retain uncertainty for review, never invent office presence.
            val reviewOffice = offices.filter { candidate -> !wifiDisconnected && bssid != null &&
                candidate.wifiAccessPoints.any { it.bssid.equals(bssid, true) } }.singleOrNull()
            reviewEvidence.record(location, bssid, wifiDisconnected, reviewOffice, "UNCERTAIN", snapshot.attendanceState?.name ?: "UNKNOWN")
            journal.record(location, null, false, "UNCERTAIN")
            diagnostics.record("ENGINE", "Evaluation paused: no unambiguous CRM office; absence is not inferred")
            return@withLock
        }
        val policy = office.toEnginePolicy().getOrElse {
            diagnostics.record("ENGINE", "Evaluation paused: ${it.message}")
            return@withLock
        }
        suspend fun uncertain(reason: String) {
            reviewEvidence.record(location, bssid, wifiDisconnected, office, "UNCERTAIN", snapshot.attendanceState?.name ?: "UNKNOWN")
            departureEvidence.reset()
            needsBetterGps = true
            val evidence = AttendanceEngineEvidence(latitude = location.latitude, longitude = location.longitude,
                accuracyMeters = location.accuracy.toDouble(), capturedAtElapsedRealtimeMs = location.elapsedRealtimeNanos / 1_000_000L,
                observationUncertain = true)
            if (workdayEnded()) return
            provider.runtimeSession.evaluate(token, session.getUserId(), session.getCompanyId(), deviceId, evidence, policy)
            val reportedWifiMatch = bssid?.takeUnless { wifiDisconnected }?.takeIf {
                it.matches(Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")) &&
                    it != "02:00:00:00:00:00" && it != "00:00:00:00:00:00"
            }?.let { found -> office.wifiAccessPoints.any { it.bssid.equals(found, true) } } ?: false
            journal.record(location, office.id, reportedWifiMatch, "UNCERTAIN")
            diagnostics.record("ENGINE", "Evidence uncertain: $reason; no attendance event")
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
        val wifiVerified = !wifiDisconnected && readableBssid != null && office.wifiAccessPoints.any { it.bssid.equals(readableBssid, true) }
        val definitelyOutside = distanceMeters - location.accuracy > office.geofenceRadiusMeters
        diagnostics.record("ENGINE", "Evidence officeId=${office.id}; distance=${distanceMeters}m; accuracy=${location.accuracy}m; radius=${office.geofenceRadiusMeters}m; wifiMatched=$wifiVerified; wifiDisconnected=$wifiDisconnected")
        if (inside && policy.wifiRequired == true && !wifiVerified) {
            if (wifiDisconnected) provisionalArrivals.record(location, office, distanceMeters, true)
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
            needsContinuityChecks = snapshot.attendanceState in setOf(AttendanceState.WORKING, AttendanceState.ON_BREAK)
            if (!departureEvidence.confirmOutside(now)) {
                uncertain("Departure needs another fresh outside observation")
                // Uncertainty reset the evidence pair; retain this new first fix for the next burst.
                departureEvidence.confirmOutside(now)
                return@withLock
            }
        } else {
            departureEvidence.reset()
        }
        if (boundOfficeId == null && (!inside || (policy.wifiRequired == true && !wifiVerified))) {
            diagnostics.record("ENGINE", "Waiting for CRM office presence verification")
            return@withLock
        }
        val evidence = AttendanceEngineEvidence(
            latitude = location.latitude, longitude = location.longitude,
            accuracyMeters = location.accuracy.toDouble(), geofenceVerified = inside,
            wifiVerified = wifiVerified, wifiBssid = readableBssid,
            capturedAtElapsedRealtimeMs = location.elapsedRealtimeNanos / 1_000_000L,
            arrivalProof = if (!OfflineWorkSessionStore.isOffline() && snapshot.attendanceState == AttendanceState.NOT_CHECKED_IN && inside && wifiVerified)
                arrivals.confirmation(office.id, readableBssid, location.elapsedRealtimeNanos / 1_000_000L) else null
        )
        evidence.arrivalProof?.let { proof -> diagnostics.record("ARRIVAL_CONFIRMED",
            "officeId=${office.id}; candidate=${proof.id}; arrivalElapsedMs=${proof.observedElapsedMs}; confirmedElapsedMs=${proof.confirmedElapsedMs}") }
        reviewEvidence.record(location, bssid, wifiDisconnected, office,
            if (inside) "INSIDE" else "OUTSIDE", snapshot.attendanceState?.name ?: "UNKNOWN")
        journal.record(location, office.id, wifiVerified, if (inside) "PRESENT" else "ABSENT_CANDIDATE")
        if (workdayEnded()) return@withLock
        val result = provider.runtimeSession.evaluate(token, session.getUserId(), session.getCompanyId(),
            deviceId, evidence, policy)
        val after = provider.runtimeSession.getSnapshot()
        if (inside && after.attendanceState == AttendanceState.WORKING) {
            boundOfficeId = office.id
            officeContext.save(session.getUserId(), session.getCompanyId(), deviceId, workDate, boot.getBootId(), office.id)
        }
        val phase = after.phase
        diagnostics.record("ENGINE_PHASE", "state=${snapshot.attendanceState}; before=${snapshot.phase}; after=$phase; officeId=${office.id}; graceMs=${policy.gracePeriodMs}")
        nextVerificationElapsedMs = when (phase) {
            is AttendanceEnginePhase.AbsenceGrace -> phase.absenceStartedElapsedRealtimeMs + policy.gracePeriodMs
            is AttendanceEnginePhase.BreakActive -> phase.absenceStartedElapsedRealtimeMs + policy.maximumAbsenceMs
            else -> null
        }
        diagnostics.record("ENGINE", "CRM officeId=${office.id}; evaluation=$result")
    }
}

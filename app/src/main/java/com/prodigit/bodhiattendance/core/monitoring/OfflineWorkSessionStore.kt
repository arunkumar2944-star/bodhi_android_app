package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.os.SystemClock
import com.google.gson.Gson
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.security.BootSessionManager
import com.prodigit.bodhiattendance.data.remote.DailyEligibility
import com.prodigit.bodhiattendance.domain.attendance.*
import com.prodigit.bodhiattendance.domain.model.AttendanceLocationConfig
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OfflineWorkSessionView(val offline: Boolean = false, val authorizedUntilMs: Long? = null,
    val message: String = "Connect and open the app to authorize a work session")

data class OfflineWorkAuthorization(val lease: OfflineSessionLease, val permit: String,
    val eligibility: DailyEligibility, val offices: List<AttendanceLocationConfig>)

/** Cache is scoped to one authenticated workday and boot. It never starts a service. */
class OfflineWorkSessionStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("offline_work_session", Context.MODE_PRIVATE)
    private val gson = Gson()
    private fun scope(): OfflineSessionScope = OfflineSessionScope(SessionManager(app).getUserId(),
        SessionManager(app).getCompanyId(), DeviceIdManager(app).getDeviceId().orEmpty(), BootSessionManager(app).getBootId())

    suspend fun current(): OfflineWorkAuthorization? {
        val expected = scope()
        if (expected.userId <= 0 || SessionManager(app).getToken().isNullOrBlank() || AttendancePrivacyPolicy.isPaused(app)) return null
        val cached = runCatching { gson.fromJson(prefs.getString("authorization", null), OfflineWorkAuthorization::class.java) }.getOrNull() ?: return null
        val now = SystemClock.elapsedRealtime()
        val anchor = AttendanceDatabase.getInstance(app).timeAnchorDao().getLatestValidTimeAnchor(expected.deviceId, expected.bootId) ?: return null
        if (anchor.elapsedRealtimeMs > now) return null
        val trusted = runCatching { Math.addExact(Instant.parse(anchor.serverTime).toEpochMilli(), now - anchor.elapsedRealtimeMs) }.getOrNull() ?: return null
        val day = Instant.ofEpochMilli(trusted).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate().toString()
        val valid = runCatching {
            OfflineWorkSessionPolicy.valid(cached.lease, expected, now, trusted, day, false) &&
                cached.permit.isNotBlank() && cached.offices.isNotEmpty() &&
                cached.eligibility.userId == expected.userId && cached.eligibility.companyId == expected.companyId && cached.eligibility.date == day &&
                cached.eligibility.monitoringMode in setOf("ACTIVE", "AWAITING_ARRIVAL")
        }.getOrDefault(false)
        if (!valid) return null
        return cached
    }

    suspend fun authorize(day: DailyEligibility, offices: List<AttendanceLocationConfig>) {
        if (day.monitoringMode == "PAUSED") { clear(app, "CRM paused attendance monitoring"); return }
        if (offices.isEmpty()) { clear(app, "No active office configuration — offline attendance unavailable"); return }
        val expected = scope()
        val runtime = AttendanceEngineRuntimeProvider.getInstance(app).runtimeSession.getSnapshot()
        if (runtime.mode != AttendanceEngineRuntimeMode.READY || runtime.userId != expected.userId ||
            runtime.deviceId != expected.deviceId || day.userId != expected.userId || day.companyId != expected.companyId || offices.isEmpty()) return
        val permit = day.offlinePermit?.takeIf { it.isNotBlank() } ?: run { clear(app, "Offline authorization unavailable for this workday"); return }
        val until = day.offlineValidUntilMs ?: return
        val prior = current()
        // Do not move the grant's lower bound past an already observed departure.
        val lease = prior?.lease ?: OfflineSessionLease(expected, day.date, day.serverTimeMs, until, SystemClock.elapsedRealtime())
        val cached = OfflineWorkAuthorization(lease, prior?.permit ?: permit, day, offices)
        check(prefs.edit().putString("authorization", gson.toJson(cached)).commit()) { "Offline session persistence failed" }
        if (current() != null) mutable.value = OfflineWorkSessionView(false, lease.validUntilMs, "Offline attendance ready for this work session")
    }

    suspend fun useOffline(): OfflineWorkAuthorization? {
        val cached = current() ?: run {
            mutable.value = OfflineWorkSessionView(false, null, "Offline authorization unavailable or expired — connect to CRM")
            return null
        }
        mutable.value = OfflineWorkSessionView(true, cached.lease.validUntilMs, "Offline monitoring — attendance saved on this phone, pending CRM validation")
        return cached
    }

    fun online() { mutable.value = mutable.value.copy(offline = false) }
    suspend fun sensorBudgetMs(): Long {
        val cached = current() ?: return 0L
        val expected = scope()
        val anchor = AttendanceDatabase.getInstance(app).timeAnchorDao().getLatestValidTimeAnchor(expected.deviceId, expected.bootId) ?: return 0L
        val trusted = Instant.parse(anchor.serverTime).toEpochMilli() + SystemClock.elapsedRealtime() - anchor.elapsedRealtimeMs
        return (cached.lease.validUntilMs - trusted).coerceIn(0L, 35_000L)
    }
    suspend fun permitForEvent(user: Int, company: Int, device: String, boot: String, effectiveMs: Long): String? {
        val cached = current() ?: return null
        if (cached.lease.scope != OfflineSessionScope(user, company, device, boot) ||
            !OfflineWorkSessionPolicy.permitsEvent(cached.lease, effectiveMs)) return null
        return cached.permit
    }
    fun scheduleSync() { AttendanceBackgroundSync.schedule(app) }

    companion object {
        private val mutable = MutableStateFlow(OfflineWorkSessionView())
        val state = mutable.asStateFlow()
        fun isOffline(): Boolean = mutable.value.offline
        fun clear(context: Context, reason: String = "Offline work session ended") {
            check(context.applicationContext.getSharedPreferences("offline_work_session", Context.MODE_PRIVATE).edit().clear().commit())
            mutable.value = OfflineWorkSessionView(message = reason)
        }
    }
}

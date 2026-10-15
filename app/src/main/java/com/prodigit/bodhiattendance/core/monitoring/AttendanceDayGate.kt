package com.prodigit.bodhiattendance.core.monitoring

import android.app.PendingIntent
import android.app.job.*
import android.content.*
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.*
import com.prodigit.bodhiattendance.data.remote.DailyEligibility

/** Eligibility is read only when the employee opens the app or monitoring is already running. */
object AttendanceDayGate {
    private val statusFlow = kotlinx.coroutines.flow.MutableStateFlow("NOT_VERIFIED")
    val state: kotlinx.coroutines.flow.StateFlow<String> = statusFlow
    private fun prefs(c: Context) = c.getSharedPreferences("attendance_day_gate", Context.MODE_PRIVATE)
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
    fun cancelLegacyAutomation(c: Context): Task<Void> {
        val scheduler = requireNotNull(c.getSystemService(JobScheduler::class.java))
        scheduler.cancel(25302); scheduler.cancel(25303)
        prefs(c).edit().putBoolean("arrival_alerts", false).apply()
        val oldArrival = PendingIntent.getBroadcast(c, 25302, Intent(c, AttendanceArrivalReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        val client = LocationServices.getGeofencingClient(c)
        runCatching { client.removeGeofences(oldArrival) }
        val oldOffice = PendingIntent.getBroadcast(c, 2402, Intent(c, OfficeGeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        return runCatching { client.removeGeofences(oldOffice) }.getOrElse { com.google.android.gms.tasks.Tasks.forResult<Void>(null) }
    }
    fun clearStatus(c: Context) { prefs(c).edit().clear().apply(); statusFlow.value = "NOT_VERIFIED" }
}

/** Compatibility components can only discard work scheduled by old builds. */
class AttendanceDayJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean { AttendancePrivacyPolicy.removeLegacyAutomation(applicationContext); return false }
    override fun onStopJob(params: JobParameters): Boolean = false
}
class AttendanceArrivalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { AttendancePrivacyPolicy.removeLegacyAutomation(context) }
}

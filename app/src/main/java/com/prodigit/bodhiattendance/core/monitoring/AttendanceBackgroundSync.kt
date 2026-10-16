package com.prodigit.bodhiattendance.core.monitoring

import android.app.job.*
import android.content.*
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.session.*
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.repository.*
import com.prodigit.bodhiattendance.data.repository.AttendanceRepositoryImpl
import com.prodigit.bodhiattendance.data.sync.AttendanceSyncManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Network-only retries: no location, Wi-Fi observations or geofence registration. */
object AttendanceBackgroundSync {
    private const val JOB_ID = 25300
    private val mutex = Mutex()
    fun schedule(context: Context) {
        val scheduler = requireNotNull(context.getSystemService(JobScheduler::class.java))
        if (scheduler.getPendingJob(JOB_ID) != null) return
        scheduler.schedule(JobInfo.Builder(JOB_ID, ComponentName(context, AttendanceUploadJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
            .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
    }
    fun cancel(context: Context) { requireNotNull(context.getSystemService(JobScheduler::class.java)).cancel(JOB_ID) }
    suspend fun run(context: Context, diagnostics: Boolean = true): Boolean = mutex.withLock {
        val user = SessionManager(context).getUserId()
        val device = DeviceIdManager(context).getDeviceId() ?: return@withLock true
        if (user <= 0) return@withLock true
        val token = AndroidTokenManager(context).usableAccessToken() ?: return@withLock false
        // Diagnostic evidence has its own authenticated/device-scoped endpoint. It must
        // not wait for attendance eligibility, GPS or event-sync success.
        val diagnosticsComplete = if (diagnostics) {
            try { AttendanceDiagnostics(context).upload() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { false }
        } else true
        // Historical supporting evidence is independent of today's monitoring eligibility.
        val provisionalComplete = try { ProvisionalArrivalRecorder(context).upload() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { false }
        // Reconnection validates current employee/device eligibility before attempting saved writes.
        // A PAUSED policy revokes the local lease; queued events still reach server review.
        try { AttendanceDayGate.fetch(context) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { return@withLock false }
        val db = AttendanceDatabase.getInstance(context)
        val offline = OfflineAttendanceRepository(db.attendanceEventDao(), AttendanceDiagnostics(context))
        AttendanceSyncManager(AttendanceRepositoryImpl(RetrofitClient.attendanceApi), offline,
            TimeAnchorLocalRepository(db.timeAnchorDao())).syncUnsyncedEvents(token, userId = user, deviceId = device)
        !offline.hasScopedUnsettledEvents(user, device) && diagnosticsComplete && (provisionalComplete || !diagnostics)
    }
}

class AttendanceUploadJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var task: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        task = scope.launch {
            val complete = try { AttendanceBackgroundSync.run(applicationContext) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { false }
            jobFinished(params, !complete)
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { task?.cancel(); return true }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

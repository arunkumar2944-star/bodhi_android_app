package com.prodigit.bodhiattendance.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.entity.AttendanceEventEntity
import com.prodigit.bodhiattendance.data.remote.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Display data is memory-only. Refresh cannot delete durable attendance or credentials. */
data class LiveUiState(
    val loading: Boolean = true,
    val profile: MobileProfile? = null,
    val attendance: AttendanceReadModel? = null,
    val configuration: com.prodigit.bodhiattendance.data.remote.dto.attendance.AttendanceConfigDataDto? = null,
    val requests: List<MobileRequest>? = null,
    val notifications: List<MobileNotification>? = null,
    val notificationUnreadCount: Int = 0,
    val onDuty: List<MobileDuty>? = null,
    val localEvents: List<AttendanceEventEntity>? = null,
    val errors: Map<String, String> = emptyMap(),
    val submitting: Boolean = false,
    val submissionMessage: String? = null
)
class LiveUiViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(LiveUiState())
    val state = _state.asStateFlow()
    private val api = RetrofitClient.mobileUiApi
    private val session = SessionManager(application)
    private val tokens = AndroidTokenManager(application)
    private val lock = Mutex()
    @Volatile private var generation = 0
    private var attendanceRefresh: Job? = null
    private val submissionIds = mutableMapOf<String, String>()
    init {
        viewModelScope.launch {
            AttendanceDatabase.getInstance(application).attendanceEventDao().observeAllEvents().collect { rows ->
                val deviceId = DeviceIdManager(application).getDeviceId()
                _state.update { it.copy(localEvents = rows.filter { row -> row.userId == session.getUserId() && row.deviceId == deviceId }) }
                attendanceRefresh?.cancel()
                attendanceRefresh = viewModelScope.launch { delay(300); refresh() }
            }
        }
    }
    suspend fun refresh() = lock.withLock {
        val currentGeneration = generation
        val token = tokens.usableAccessToken()
        if (token == null) {
            _state.update { if (generation != currentGeneration) it else LiveUiState(loading = false, localEvents = it.localEvents, errors = mapOf("all" to "Sign in or restore connectivity to load CRM data")) }
            return@withLock
        }
        val authorization = "Bearer $token"
        suspend fun <T> load(key: String, call: suspend () -> Response<MobileReply<T>>, save: (LiveUiState, T?) -> LiveUiState) {
            try {
                val response = call()
                val body = response.body()
                if (!response.isSuccessful || body == null || !body.success || body.data == null) {
                    throw IllegalStateException(if (response.code() == 501) "CRM On Duty assignment API is not configured" else "$key unavailable (HTTP ${response.code()}); check CRM deployment")
                }
                _state.update { if (generation != currentGeneration) it else save(it, body.data).copy(errors = it.errors - key - "all") }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.update { if (generation != currentGeneration) it else save(it, null).copy(errors = it.errors + (key to (error.message ?: "$key unavailable"))) } }
        }
        coroutineScope {
            launch {
                try {
                    val response = RetrofitClient.attendanceApi.getActiveAttendanceConfig(authorization)
                    val body = response.body()
                    check(response.isSuccessful && body != null && body.success && body.data != null) { "Attendance configuration unavailable (HTTP ${response.code()})" }
                    _state.update { if (generation != currentGeneration) it else it.copy(configuration = body!!.data, errors = it.errors - "configuration") }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { _state.update { if (generation != currentGeneration) it else it.copy(configuration = null, errors = it.errors + ("configuration" to (error.message ?: "Configuration unavailable"))) } }
            }

            launch { load("attendance", { api.attendance(authorization) }) { s, v -> s.copy(attendance = v) } }
            launch { load("requests", { api.requests(authorization) }) { s, v -> s.copy(requests = v) } }
            launch {
                try {
                    val response = api.notifications(authorization)
                    val body = response.body()
                    check(response.isSuccessful && body?.success == true && body.data != null) { "Notifications unavailable (HTTP ${response.code()})" }
                    val data = requireNotNull(body)
                    val rows = requireNotNull(data.data)
                    _state.update { if (generation != currentGeneration) it else it.copy(notifications = rows,
                        notificationUnreadCount = data.unreadCount ?: rows.count { row -> row.readAt == null }, errors = it.errors - "notifications") }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { _state.update { if (generation != currentGeneration) it else it.copy(errors = it.errors + ("notifications" to (error.message ?: "Notifications unavailable"))) } }
            }
            launch { load("onDuty", { api.onDuty(authorization) }) { s, v -> s.copy(onDuty = v) } }
            launch {
                try {
                    val response = api.profile(authorization)
                    val body = response.body()
                    check(response.isSuccessful && body != null && body.success && body.user != null) { "Profile unavailable (HTTP ${response.code()})" }
                    _state.update { if (generation != currentGeneration) it else it.copy(profile = body!!.user, errors = it.errors - "profile") }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { _state.update { if (generation != currentGeneration) it else it.copy(profile = null, errors = it.errors + ("profile" to (error.message ?: "Profile unavailable"))) } }
            }
        }
        _state.update { if (generation != currentGeneration) it else it.copy(loading = false) }
    }
    fun submit(type: String, date: String, description: String, duration: Int? = null, subtype: String? = null, requestedTime: String? = null, result: (String) -> Unit) {
        if (_state.value.submitting) return
        val key = listOf(type,date,description,duration,subtype,requestedTime).joinToString("\u0000")
        val id = submissionIds.getOrPut(key) { UUID.randomUUID().toString() }
        _state.update { it.copy(submitting = true, submissionMessage = "Submitting request to CRM…") }
        result("Submitting request to CRM…")
        viewModelScope.launch {
            try {
                val token = tokens.usableAccessToken() ?: error("Session or connection unavailable")
                val response = api.submit("Bearer $token", MobileRequestSubmission(id,type,date,description,duration,subtype,requestedTime))
                val body = response.body()
                check(response.isSuccessful && body?.success == true && body.data?.id == id) { "Request was not confirmed by CRM (HTTP ${response.code()})" }
                _state.update { it.copy(submissionMessage = "Request received by CRM; pending review") }
                result("Request received by CRM; pending review")
                refresh()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val message = error.message ?: "Request could not be submitted; please retry"
                _state.update { it.copy(submissionMessage = message) }; result(message)
            }
            finally { _state.update { it.copy(submitting = false) } }
        }
    }
    suspend fun openNotification(id: String): MobileNotification = lock.withLock {
        val currentGeneration = generation
        val token = tokens.usableAccessToken() ?: error("Session unavailable")
        val reply = api.notificationDetail("Bearer $token", id)
        val detail = reply.body()?.data
        check(reply.isSuccessful && reply.body()?.success == true && detail?.id == id) { "Notification details could not be loaded" }
        val notification = requireNotNull(detail)
        val receipt = api.readNotification("Bearer $token", id)
        val read = receipt.body()?.data
        check(receipt.isSuccessful && receipt.body()?.success == true && read?.id == id) { "Unable to mark notification read. Please retry." }
        check(generation == currentGeneration) { "Session changed" }
        val wasUnread = notification.readAt == null
        val updated = notification.copy(readAt = requireNotNull(read).readAt)
        _state.update { it.copy(notifications = it.notifications?.map { row -> if (row.id == id) updated else row },
            notificationUnreadCount = (it.notificationUnreadCount - if (wasUnread) 1 else 0).coerceAtLeast(0)) }
        updated
    }

    fun clearAfterLogout() { generation++; _state.value = LiveUiState(loading = false); submissionIds.clear() }

    fun clearDisplayData() {
        generation++
        _state.update { previous ->
            val userId = session.getUserId()
            val deviceId = DeviceIdManager(getApplication<Application>()).getDeviceId()
            LiveUiState(localEvents = previous.localEvents?.filter { it.userId == userId && it.deviceId == deviceId }, submitting = previous.submitting, submissionMessage = previous.submissionMessage)
        }
        viewModelScope.launch { refresh() }
    }
}
fun displayTime(ms: Long?): String = ms?.let { DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH).withZone(ZoneId.of("Asia/Kolkata")).format(Instant.ofEpochMilli(it)) } ?: "--"
fun displayDate(date: String): String = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH))
fun durationText(minutes: Int?): String = minutes?.let { "${it / 60}h ${it % 60}m" } ?: "--"

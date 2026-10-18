package com.prodigit.bodhiattendance.ui.devicereplacement

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.prodigit.bodhiattendance.ui.common.AppBackground
import com.prodigit.bodhiattendance.ui.common.AppMasterHeader
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class ReplacementRequestBody(
    val device_id: String,
    val device_name: String,
    val reason: String,
    val request_type: String
)

data class ReplacementRequestResult(val status: String? = null, val message: String? = null)

data class ReplacementRequestRecord(
    val id: Long,
    val status: String,
    val request_type: String,
    val reason: String? = null,
    val review_remarks: String? = null,
    val created_at: String? = null,
    val reviewed_at: String? = null,
    val new_device_id: String? = null,
    val new_device_name: String? = null
)

data class ActivationRequest(val device_id: String)
data class ActivationResult(val success: Boolean = false, val status: String? = null)

data class ReplacementHistoryResult(
    val success: Boolean = false,
    val requests: List<ReplacementRequestRecord> = emptyList()
)

interface DeviceReplacementApi {
    @POST("api/v1/device-replacements/requests")
    suspend fun request(
        @Header("Authorization") token: String,
        @Body body: ReplacementRequestBody
    ): Response<ReplacementRequestResult>

    @POST("api/v1/device-replacements/activate")
    suspend fun activate(@Header("Authorization") token: String, @Body body: ActivationRequest): Response<ActivationResult>

    @GET("api/v1/device-replacements/requests/mine")
    suspend fun mine(@Header("Authorization") token: String): Response<ReplacementHistoryResult>
}

@Composable
fun DeviceReplacementScreen(onRecoveryCompleted: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installationId = remember { DeviceIdManager(context).getOrCreateDeviceId() }
    val tokenManager = remember { AndroidTokenManager(context) }

    var reason by remember { mutableStateOf("") }
    var requestType by remember { mutableStateOf("APP_DATA_RESET") }
    var submitting by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var verified by remember { mutableStateOf(false) }
    var refreshSequence by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var currentRequest by remember { mutableStateOf<ReplacementRequestRecord?>(null) }

    // The server, not local UI state, decides whether the employee may submit.
    // Re-check periodically so a manager decision is shown without app restart.
    LaunchedEffect(installationId, refreshSequence) {
        while (true) {
            try {
                val token = tokenManager.usableAccessToken()
                if (token.isNullOrBlank()) {
                    verified = false
                    message = "Session unavailable. Sign in again to check your request."
                } else {
                    val response = RetrofitClient.deviceReplacementApi.mine("Bearer $token")
                    if (response.isSuccessful && response.body()?.success == true) {
                        val requests = response.body()!!.requests
                        // Backend returns only the current employee's requests, newest first.
                        // Never unlock a different installation based on an older request.
                        currentRequest = requests.firstOrNull {
                            it.new_device_id == installationId &&
                                it.status in setOf("PENDING", "APPROVED", "COMPLETED")
                        }
                        verified = true
                        message = null
                    } else {
                        verified = false
                        message = "Could not verify request status (HTTP ${response.code()})."
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                verified = false
                message = "Network unavailable. Your previous request remains saved in CRM."
            } finally {
                loading = false
            }
            delay(20_000)
        }
    }

    var evidenceSubmitted by remember { mutableStateOf(false) }
    var activationRetry by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentRequest?.id, currentRequest?.status, activationRetry) {
        val request = currentRequest ?: return@LaunchedEffect
        if (request.status == "COMPLETED") {
            onRecoveryCompleted()
            return@LaunchedEffect
        }
        if (request.status in setOf("PENDING", "APPROVED")) {
            evidenceSubmitted = com.prodigit.bodhiattendance.core.device.DeviceSecurityEvidence
                .submitEvidence(context, installationId)
            if (request.status == "APPROVED" && evidenceSubmitted) {
                try {
                    val token = tokenManager.usableAccessToken()
                    if (!token.isNullOrBlank()) {
                        val result = RetrofitClient.deviceReplacementApi.activate(
                            "Bearer $token", ActivationRequest(installationId)
                        )
                        if (result.isSuccessful && result.body()?.status == "RECOVERY_COMPLETED") {
                            onRecoveryCompleted()
                        } else {
                            message = "Approved — secure activation pending (HTTP ${result.code()}). Retry after verification."
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = "Approved — waiting for CRM secure activation."
                }
            }
        }
    }

    // Recovery is outside the normal bottom-navigation graph, but it must
    // retain the same application branding and background as other pages.
    // No bottom tabs or notification navigation while registration is blocked.
    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            AppMasterHeader()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
        Text("Secure device recovery", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Only a manager-approved installation can access attendance. Clearing app data or changing phones requires verification.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            loading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Checking your existing request…")
            }
            !verified -> {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Status verification unavailable", fontWeight = FontWeight.SemiBold)
                        Text("For your security, a new request cannot be submitted until CRM confirms whether one already exists.")
                    }
                }
                Button(onClick = { loading = true; refreshSequence++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry status check")
                }
            }
            currentRequest != null -> {
                val request = currentRequest!!
                val title = when (request.status) {
                    "PENDING" -> "Waiting for manager approval"
                    "APPROVED" -> "Manager approved — activation pending"
                    "COMPLETED" -> "Recovery completed"
                    else -> "Recovery status"
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Request #${request.id}")
                        Text(if (evidenceSubmitted) "Device security evidence submitted for CRM verification" else "Device security evidence pending", style = MaterialTheme.typography.bodySmall)
                        Text(if (request.request_type == "APP_DATA_RESET") "Same phone — app data cleared" else "Different phone — replacement")
                        request.created_at?.let { Text("Submitted: $it", style = MaterialTheme.typography.bodySmall) }
                        request.reason?.let { Text("Reason: $it") }
                        request.review_remarks?.takeIf { it.isNotBlank() }?.let { Text("Manager remarks: $it") }
                        Text(
                            when (request.status) {
                                "PENDING" -> "Your request is recorded in CRM. Editing and duplicate submissions are disabled. This screen checks for updates automatically."
                                "APPROVED" -> "Your manager has approved the request. Attendance remains blocked until CRM securely revokes the previous binding and activates this installation. Approval alone does not restore credentials."
                                "COMPLETED" -> "The recovery was completed on the server. Restart the app to pass through registered-device validation and restore attendance state. Do not reuse the old installation identity."
                                else -> "Check with your manager."
                            }
                        )
                    }
                }
                OutlinedButton(onClick = { loading = true; refreshSequence++; activationRetry++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh / retry activation")
                }
            }
            else -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "If you already submitted a request on another installation, contact your manager. Only one pending request per employee is allowed.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Text("What happened?", style = MaterialTheme.typography.titleMedium)
                listOf(
                    "APP_DATA_RESET" to "Same phone — app data cleared or phone reset",
                    "PHONE_REPLACEMENT" to "Different phone — replace registered device"
                ).forEach { (code, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = requestType == code, enabled = !submitting, onClick = { requestType = code })
                        Text(label, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 1000) reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Reason for recovery") },
                    supportingText = { Text("At least 10 characters. Never include your password.") },
                    enabled = !submitting
                )
                Button(
                    enabled = reason.trim().length >= 10 && !submitting,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        submitting = true
                        scope.launch {
                            try {
                                val token = tokenManager.usableAccessToken()
                                if (token.isNullOrBlank()) {
                                    message = "Session unavailable. Sign in again."
                                } else {
                                    val response = RetrofitClient.deviceReplacementApi.request(
                                        "Bearer $token",
                                        ReplacementRequestBody(
                                            installationId,
                                            "${Build.MANUFACTURER} ${Build.MODEL}",
                                            reason.trim(),
                                            requestType
                                        )
                                    )
                                    message = if (response.isSuccessful) {
                                        "Request submitted. Waiting for manager review."
                                    } else if (response.code() == 409) {
                                        "A request may already be pending. Checking CRM status."
                                    } else {
                                        "Request failed (HTTP ${response.code()})."
                                    }
                                    // Refresh immediately after submit, including HTTP 409.
                                    refreshSequence++
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                message = "Network unavailable. Checking CRM before allowing another request."
                                verified = false
                                refreshSequence++
                            } finally {
                                submitting = false
                            }
                        }
                    }
                ) {
                    Text(if (submitting) "Submitting…" else "Submit recovery request")
                }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

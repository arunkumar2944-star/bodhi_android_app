package com.prodigit.bodhiattendance.ui.devicereplacement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import kotlinx.coroutines.launch
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ReplacementRequestBody(
    val device_id: String,
    val device_name: String,
    val reason: String,
    val request_type: String
)
data class ReplacementRequestResult(val status: String? = null, val message: String? = null)
interface DeviceReplacementApi {
    @POST("api/v1/device-replacements/requests")
    suspend fun request(
        @Header("Authorization") token: String,
        @Body body: ReplacementRequestBody
    ): retrofit2.Response<ReplacementRequestResult>
}

@Composable
fun DeviceReplacementScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf("") }
    var requestType by remember { mutableStateOf("APP_DATA_RESET") }
    var submitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val id = remember { DeviceIdManager(context).getOrCreateDeviceId() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Device already registered", style = MaterialTheme.typography.headlineSmall)
        Text("Your account has another active app installation. If this is the same physical phone after reinstalling, switching between release and debug builds, or clearing app data, the stored installation identity may have changed. For security, the old identity cannot be copied automatically.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("Request secure registration recovery or replacement", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("If this is your original phone, explain that in your reason. Your manager or authorized administrator will verify it. The old registration remains active until secure activation is completed.")
            }
        }
        Text("What happened?", style = MaterialTheme.typography.titleMedium)
        listOf(
            "APP_DATA_RESET" to "Same phone — app data cleared or phone reset",
            "PHONE_REPLACEMENT" to "Different phone — replace registered device"
        ).forEach { (code, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = requestType == code, onClick = { if (!submitted) requestType = code })
                Text(label, modifier = Modifier.weight(1f))
            }
        }
        Text(
            if (requestType == "APP_DATA_RESET")
                "We cannot verify a factory-reset phone from its new app ID alone. An authorized administrator must confirm your identity and recover access securely."
            else "A manager or authorized administrator must approve the replacement before the old registration is revoked and this new installation is activated.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = reason,
            onValueChange = { if(it.length <= 1000) reason = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text(if (requestType == "APP_DATA_RESET") "Describe the reset or data loss" else "Reason for changing phone") },
            supportingText = { Text("Please provide at least 10 characters. Do not include passwords.") },
            enabled = !submitting && !submitted
        )
        Button(
            enabled = reason.trim().length >= 10 && !submitting && !submitted,
            onClick = {
                submitting = true
                scope.launch {
                    try {
                        val token = AndroidTokenManager(context).usableAccessToken()
                        if (token.isNullOrBlank()) {
                            message = "Your session is unavailable. Sign in again."
                        } else {
                            val response = RetrofitClient.deviceReplacementApi.request(
                                "Bearer $token",
                                ReplacementRequestBody(
                                    id,
                                    android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL,
                                    reason.trim(),
                                    requestType
                                )
                            )
                            if (response.isSuccessful && response.body()?.status == "REPLACEMENT_PENDING_APPROVAL") {
                                submitted = true
                                message = "Request submitted. Contact your administrator for approval."
                            } else {
                                message = when(response.code()) {
                                    409 -> "A replacement may already be pending, or the device is already assigned. Contact your administrator."
                                    403 -> "Device replacement is not available for this account."
                                    else -> "Could not submit the request (HTTP ${response.code()}). Please retry."
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        message = "Network unavailable. Please retry when connected."
                    } finally { submitting = false }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (submitting) "Submitting…" else if (requestType == "APP_DATA_RESET") "Request access recovery" else "Request device change") }
        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

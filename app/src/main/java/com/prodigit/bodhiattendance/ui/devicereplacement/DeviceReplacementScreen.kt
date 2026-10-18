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
    val reason: String
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
        Text("Your account is linked to another app installation. For your security, your existing device cannot be copied to this phone.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("Request an approved device change", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Your manager or authorized administrator will review the request. The old registration remains active until the replacement is securely activated.")
            }
        }
        OutlinedTextField(
            value = reason,
            onValueChange = { if(it.length <= 1000) reason = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text("Reason for changing device") },
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
                                    reason.trim()
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
        ) { Text(if (submitting) "Submitting…" else "Request device replacement") }
        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

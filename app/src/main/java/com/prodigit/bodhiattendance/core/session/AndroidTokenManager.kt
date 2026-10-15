package com.prodigit.bodhiattendance.core.session

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.data.remote.AndroidDeviceSessionRequest
import com.prodigit.bodhiattendance.data.remote.AndroidRefreshRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One process-wide rotation lock: simultaneous workers must not reuse an
 * already-consumed refresh token, which the server treats as a replay.
 */
class AndroidTokenManager(context: Context) {
    private val contextForOffline = context.applicationContext
    private val sessions = SessionManager(context.applicationContext)
    private val credentials = SecureAndroidRefreshStore(context.applicationContext)
    private val deviceIds = DeviceIdManager(context.applicationContext)

    var lastHttpStatus: Int? = null
        private set

    fun hasRefreshCredential(): Boolean = credentials.get() != null

    suspend fun usableAccessToken(): String? = rotationLock.withLock {
        lastHttpStatus = null
        val current = sessions.getToken() ?: return@withLock null
        if (!JwtUtils.expiresWithin(current)) return@withLock current
        val refresh = credentials.get() ?: return@withLock null
        val deviceId = deviceIds.getDeviceId() ?: return@withLock null
        try {
            val reply = RetrofitClient.androidSessionApi.refresh(AndroidRefreshRequest(deviceId, refresh))
            lastHttpStatus = reply.code()
            val payload = reply.body()
            if (reply.isSuccessful && payload?.success == true && !payload.token.isNullOrBlank() &&
                !payload.refreshToken.isNullOrBlank()) {
                // Store new refresh token first to avoid losing rotation credentials.
                credentials.put(payload.refreshToken)
                sessions.updateAccessToken(payload.token)
                return@withLock payload.token
            }
            Log.w("BODHI_SESSION", "Refresh failed: HTTP ${reply.code()}")
            // Only authoritative invalidation clears the refresh credential.
            if (reply.code() == 401 || reply.code() == 403) {
                credentials.clear()
                com.prodigit.bodhiattendance.core.monitoring.OfflineWorkSessionStore.clear(contextForOffline, "CRM denied access — offline monitoring stopped")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("BODHI_SESSION", "Refresh failed: ${error.javaClass.simpleName}")
            // Offline / temporary failure: keep identity + refresh credential.
        }
        null
    }

    suspend fun activateOnApprovedDevice(): Boolean = rotationLock.withLock {
        lastHttpStatus = null
        if (credentials.get() != null) return@withLock true
        val token = sessions.getToken()?.takeUnless(JwtUtils::isTokenExpired) ?: return@withLock false
        val id = deviceIds.getDeviceId() ?: return@withLock false
        try {
            val response = RetrofitClient.androidSessionApi.activate("Bearer $token", AndroidDeviceSessionRequest(id))
            lastHttpStatus = response.code()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && !body.refreshToken.isNullOrBlank()
                && !body.token.isNullOrBlank()) {
                credentials.put(body.refreshToken)
                sessions.updateAccessToken(body.token)
                return@withLock true
            }
            Log.w("BODHI_SESSION", "Activation failed: HTTP ${response.code()}")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("BODHI_SESSION", "Activation failed: ${error.javaClass.simpleName}")
        }
        false
    }

    /** Explicit sign-out: revoke on server before clearing local credentials.
     * Do not invoke automatically on mere access-token expiry.
     */
    suspend fun revokeOnLogout() = rotationLock.withLock {
        val refreshToken = credentials.get()
        val deviceId = deviceIds.getDeviceId()
        if (refreshToken != null && deviceId != null) {
            try {
                RetrofitClient.androidSessionApi.revoke(
                    "Bearer " + (sessions.getToken() ?: ""),
                    AndroidRefreshRequest(deviceId, refreshToken)
                )
            } catch (_: Exception) {
                // Offline revocation must be retried by server-side administrative
                // controls; clearing locally alone does NOT revoke the remote family.
            }
        }
        sessions.clearSession()
    }

    companion object { private val rotationLock = Mutex() }
}


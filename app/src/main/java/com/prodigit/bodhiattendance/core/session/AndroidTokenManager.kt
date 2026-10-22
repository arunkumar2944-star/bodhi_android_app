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

    /**
     * Authentication for the device-replacement endpoints ONLY.
     *
     * A successful password login creates a short-lived user-authenticated
     * token before this installation is approved. The recovery endpoints
     * must accept that token after server-side authentication and authorize
     * only recovery actions. Never use this method for attendance APIs.
     */
    fun recoveryAccessToken(): String? {
        val token = sessions.getToken() ?: return null
        return token.takeUnless(JwtUtils::isTokenExpired)
    }

    suspend fun usableAccessToken(): String? = rotationLock.withLock {
        lastHttpStatus = null
        val current = sessions.getToken() ?: return@withLock null
        val deviceId = deviceIds.getDeviceId() ?: return@withLock null
        val bound = JwtUtils.isDeviceBound(current, deviceId)
        if (bound && !JwtUtils.expiresWithin(current)) return@withLock current
        val refresh = credentials.get()
        if (refresh == null) {
            // One-time compatibility upgrade for an approved device that still
            // has a valid legacy login JWT but no refresh credential.
            // The server validates registration and issues a bound session.
            if (!bound && !JwtUtils.isTokenExpired(current)) {
                return@withLock activateApprovedSession(current, deviceId)
            }
            return@withLock null
        }
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
        val token = sessions.getToken()?.takeUnless(JwtUtils::isTokenExpired) ?: return@withLock false
        val id = deviceIds.getDeviceId() ?: return@withLock false
        if (credentials.get() != null && JwtUtils.isDeviceBound(token, id)) return@withLock true
        if (credentials.get() != null) return@withLock false // Never bypass an existing refresh family.
        activateApprovedSession(token, id) != null
    }

    // Called only while rotationLock is held. No recursive mutex acquisition.
    // A failed upgrade does not erase the legacy session or create fake evidence.
    private suspend fun activateApprovedSession(token: String, id: String): String? {
        try {
            val response = RetrofitClient.androidSessionApi.activate(
                "Bearer $token", AndroidDeviceSessionRequest(id)
            )
            lastHttpStatus = response.code()
            val body = response.body()
            if (response.isSuccessful && body?.success == true &&
                !body.token.isNullOrBlank() && !body.refreshToken.isNullOrBlank() &&
                JwtUtils.isDeviceBound(body.token, id)) {
                credentials.put(body.refreshToken)
                sessions.updateAccessToken(body.token)
                Log.i("BODHI_SESSION", "Approved installation upgraded to device-bound session")
                return body.token
            }
            Log.w("BODHI_SESSION", "Device session upgrade deferred: HTTP ${response.code()}")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("BODHI_SESSION", "Device session upgrade deferred: ${error.javaClass.simpleName}")
        }
        return null
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


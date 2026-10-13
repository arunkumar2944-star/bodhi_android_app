package com.prodigit.bodhiattendance.core.session

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    /** True for expired, invalid, or nearly-expired access tokens. */
    fun expiresWithin(token: String, seconds: Long = 300L): Boolean {
        return try {
            val payload = token.split(".").getOrNull(1) ?: return true
            val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val exp = JSONObject(String(bytes, Charsets.UTF_8)).getLong("exp")
            exp <= (System.currentTimeMillis() / 1000L + seconds)
        } catch (_: Exception) {
            true
        }
    }

    fun isTokenExpired(
        token: String
    ): Boolean {

        return try {

            val parts = token.split(".")

            if (parts.size != 3) {
                return true
            }

            val payload = parts[1]

            val decodedBytes =
                Base64.decode(
                    payload,
                    Base64.URL_SAFE or
                            Base64.NO_PADDING or
                            Base64.NO_WRAP
                )

            val decodedPayload =
                String(
                    decodedBytes,
                    Charsets.UTF_8
                )

            val json =
                JSONObject(decodedPayload)

            if (!json.has("exp")) {
                return true
            }

            // JWT exp is seconds since Unix epoch
            val expirationTime =
                json.getLong("exp")

            val currentTime =
                System.currentTimeMillis() / 1000

            expirationTime <= currentTime

        } catch (e: Exception) {

            // Invalid/unreadable JWT should not be trusted
            true
        }
    }
}
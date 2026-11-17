package com.prodigit.bodhiattendance.core.session

import org.json.JSONObject

object JwtUtils {

    private fun decodePayload(part: String): String? {
        return try {
            val padLength = (4 - part.length % 4) % 4
            val padded = part + "=".repeat(padLength)
            val bytes = java.util.Base64.getUrlDecoder().decode(padded)
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            try {
                val bytes = android.util.Base64.decode(part, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
                String(bytes, Charsets.UTF_8)
            } catch (_: Exception) { null }
        }
    }

    private fun extractLongClaim(payloadStr: String, key: String): Long? {
        val regex = Regex(""""$key"\s*:\s*(\d+)""")
        val match = regex.find(payloadStr)
        if (match != null) {
            return match.groupValues[1].toLongOrNull()
        }
        return try {
            val json = JSONObject(payloadStr)
            if (json.has(key)) json.getLong(key) else null
        } catch (_: Exception) { null }
    }

    /** Claims are only a local routing hint; the backend verifies JWT signatures. */
    fun isDeviceBound(token: String, installationId: String): Boolean {
        return try {
            val part = token.split(".").getOrNull(1) ?: return false
            val payloadStr = decodePayload(part) ?: return false
            val isDeviceType = payloadStr.contains(""""token_type":"android_device"""") ||
                payloadStr.contains(""""token_type" : "android_device"""")
            val isDeviceMatch = payloadStr.contains(""""android_device_id":"$installationId"""") ||
                payloadStr.contains(""""android_device_id" : "$installationId"""")
            isDeviceType && isDeviceMatch
        } catch (_: Exception) {
            false
        }
    }

    /** True for expired, invalid, or nearly-expired access tokens. */
    fun expiresWithin(token: String, seconds: Long = 300L): Boolean {
        return try {
            val payload = token.split(".").getOrNull(1) ?: return true
            val payloadStr = decodePayload(payload) ?: return true
            val exp = extractLongClaim(payloadStr, "exp") ?: return true
            exp <= (System.currentTimeMillis() / 1000L + seconds)
        } catch (_: Exception) {
            true
        }
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            val payloadStr = decodePayload(parts[1]) ?: return true
            val exp = extractLongClaim(payloadStr, "exp") ?: return true
            val currentTime = System.currentTimeMillis() / 1000
            exp <= currentTime
        } catch (_: Exception) {
            true
        }
    }
}

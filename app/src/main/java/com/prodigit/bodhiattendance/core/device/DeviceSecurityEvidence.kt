package com.prodigit.bodhiattendance.core.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import kotlinx.coroutines.CancellationException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class DeviceChallengeRequest(val device_id: String)
data class DeviceChallengeResponse(
    val success: Boolean = false,
    val challenge_id: String? = null,
    val challenge: String? = null
)
data class DeviceEvidenceRequest(
    val device_id: String,
    val challenge_id: String,
    val challenge: String,
    val public_key_spki: String,
    val signature: String,
    val android_id: String,
    val attestation_cert_chain: List<String>? = null
)
data class DeviceEvidenceResponse(val success: Boolean = false, val status: String? = null)

interface DeviceEvidenceApi {
    @POST("api/v1/device-replacements/security/challenge")
    suspend fun challenge(
        @Header("Authorization") authorization: String,
        @Body body: DeviceChallengeRequest
    ): retrofit2.Response<DeviceChallengeResponse>

    @POST("api/v1/device-replacements/security/proof")
    suspend fun prove(
        @Header("Authorization") authorization: String,
        @Body body: DeviceEvidenceRequest
    ): retrofit2.Response<DeviceEvidenceResponse>
}

object DeviceSecurityEvidence {
    private const val KEYSTORE = "AndroidKeyStore"

    private fun pair(installationId: String, attestationChallenge: ByteArray): KeyPair {
        val alias = "bodhi-device-proof-${installationId}"
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = store.getKey(alias, null)
        if (existing is java.security.PrivateKey) {
            val publicKey = store.getCertificate(alias)?.publicKey
                ?: error("Device public key unavailable")
            return KeyPair(publicKey, existing)
        }
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .setAttestationChallenge(attestationChallenge)
            .build()
        try {
            generator.initialize(spec)
            return generator.generateKeyPair()
        } catch (error: Exception) {
            // Some emulators cannot create attested keys. CRM still requires
            // a valid challenge signature and records attestation as unverified.
            android.util.Log.w("DEVICE_EVIDENCE",
                "Attested key unavailable; using non-attested Android Keystore key: ${error.javaClass.simpleName}")
            val compatible = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
            compatible.initialize(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            return compatible.generateKeyPair()
        }
    }

    // Best-effort enrichment. Failure must never unlock a device or bypass manager approval.
    suspend fun submitEvidence(context: Context, installationId: String): Boolean {
        try {
            val token = AndroidTokenManager(context).recoveryAccessToken() ?: run {
                android.util.Log.w("DEVICE_EVIDENCE", "Recovery proof deferred: login token unavailable")
                return false
            }
            val api = RetrofitClient.deviceEvidenceApi
            val challengeResponse = api.challenge(
                "Bearer $token", DeviceChallengeRequest(installationId)
            )
            val c = challengeResponse.body()
            if (!challengeResponse.isSuccessful || c?.success != true) {
                android.util.Log.w("DEVICE_EVIDENCE", "Challenge refused: HTTP ${challengeResponse.code()}, status=${c?.success}")
                return false
            }
            android.util.Log.i("DEVICE_EVIDENCE", "Challenge accepted; preparing signed key proof")
            val id = c.challenge_id ?: run {
                android.util.Log.w("DEVICE_EVIDENCE", "Challenge response missing challenge_id")
                return false
            }
            val nonce = c.challenge ?: run {
                android.util.Log.w("DEVICE_EVIDENCE", "Challenge response missing challenge")
                return false
            }
            val androidId = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            ) ?: return false
            val userId = com.prodigit.bodhiattendance.core.session.SessionManager(context).getUserId()
            if (userId <= 0) return false
            val payload = "bodhi-device-proof:v1:$id:$nonce:$installationId:$userId"
            android.util.Log.i("DEVICE_EVIDENCE", "Generating or restoring Android Keystore proof key")
            val keyPair = pair(installationId, payload.toByteArray(Charsets.UTF_8))
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            val chain = store.getCertificateChain("bodhi-device-proof-${installationId}")
                ?.map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }
                ?.takeIf { it.size > 1 }
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(keyPair.private)
            signer.update(payload.toByteArray(Charsets.UTF_8))
            val signature = Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
            val publicKey = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
            android.util.Log.i("DEVICE_EVIDENCE", "Signed key proof prepared; submitting to CRM")
            val response = api.prove(
                "Bearer $token",
                DeviceEvidenceRequest(installationId, id, nonce, publicKey, signature, androidId, chain)
            )
            val verified = response.isSuccessful && response.body()?.status == "KEY_PROOF_VERIFIED"
            if (verified) android.util.Log.i("DEVICE_EVIDENCE", "CRM verified recovery key proof")
            if (!verified) android.util.Log.w("DEVICE_EVIDENCE", "Proof refused: HTTP ${response.code()}, status=${response.body()?.status}")
            return verified
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("DEVICE_EVIDENCE", "Evidence deferred: ${e.javaClass.simpleName}")
            return false
        }
    }
}

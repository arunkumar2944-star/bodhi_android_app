package com.prodigit.bodhiattendance.core.network

import com.prodigit.bodhiattendance.BuildConfig
import com.prodigit.bodhiattendance.data.remote.AndroidSessionApi
import com.prodigit.bodhiattendance.data.remote.AuthApiService
import com.prodigit.bodhiattendance.data.remote.DeviceApiService
import com.prodigit.bodhiattendance.data.remote.AttendanceApiService
import android.util.Log
import okhttp3.OkHttpClient
import java.util.UUID
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {


    // ============================================================
    // RETROFIT
    //
    // DEBUG:
    // BuildConfig.BASE_URL -> Local development server
    //
    // RELEASE:
    // BuildConfig.BASE_URL -> Hosted production server
    // ============================================================

    // Log metadata only: never tokens, credentials, bodies, or query strings.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val requestId = UUID.randomUUID().toString()
            val request = chain.request().newBuilder()
                .header("X-Request-ID", requestId).build()
            val route = "${request.method()} ${request.url().host()}${request.url().encodedPath()}"
            val started = System.nanoTime()
            try {
                val response = chain.proceed(request)
                val elapsed = (System.nanoTime() - started) / 1_000_000
                Log.i("BODHI_API", "$route HTTP ${response.code()} ${elapsed}ms requestId=$requestId")
                response
            } catch (error: java.io.IOException) {
                val elapsed = (System.nanoTime() - started) / 1_000_000
                // A call timeout also cancels OkHttp's call. Check it before
                // treating isCanceled() as an ordinary lifecycle cancellation.
                val timedOut = error is java.net.SocketTimeoutException ||
                    (error is java.io.InterruptedIOException && error.message?.contains("timeout", ignoreCase = true) == true)
                val category = when {
                    timedOut -> "TIMEOUT"
                    chain.call().isCanceled() -> "CANCELLED"
                    error is java.net.UnknownHostException -> "DNS_FAILURE"
                    error is javax.net.ssl.SSLException -> "TLS_FAILURE"
                    error is java.net.ConnectException -> "CONNECTION_FAILURE"
                    error is java.net.SocketException -> "SOCKET_FAILURE"
                    else -> "IO_FAILURE"
                }
                val summary = "$route $category ${error.javaClass.simpleName} ${elapsed}ms requestId=$requestId"
                if (category == "CANCELLED") Log.d("BODHI_API", summary)
                else Log.e("BODHI_API", summary)
                throw error
            }
        }
        .build()

    private val retrofit: Retrofit by lazy {

        Retrofit.Builder()
            .client(httpClient)

            .baseUrl(
                BuildConfig.BASE_URL
            )

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()
    }


    // ============================================================
    // AUTH API
    // ============================================================

    val mobileUiApi by lazy { retrofit.create(com.prodigit.bodhiattendance.data.remote.MobileUiApi::class.java) }

    val androidSessionApi: AndroidSessionApi by lazy { retrofit.create(AndroidSessionApi::class.java) }

    val authApi: AuthApiService by lazy {

        retrofit.create(
            AuthApiService::class.java
        )
    }


    // ============================================================
    // DEVICE API
    // ============================================================

    val deviceApi: DeviceApiService by lazy {

        retrofit.create(
            DeviceApiService::class.java
        )
    }

    val deviceReplacementApi by lazy { retrofit.create(com.prodigit.bodhiattendance.ui.devicereplacement.DeviceReplacementApi::class.java) }

    // ============================================================
    // ATTENDANCE API
    // ============================================================

    val attendanceHeartbeatApi by lazy { retrofit.create(com.prodigit.bodhiattendance.data.remote.AttendanceHeartbeatApi::class.java) }

    val presenceEvidenceApi by lazy { retrofit.create(com.prodigit.bodhiattendance.data.remote.PresenceEvidenceApi::class.java) }

    val attendanceApi: AttendanceApiService by lazy {
        retrofit.create(AttendanceApiService::class.java)
    }



}

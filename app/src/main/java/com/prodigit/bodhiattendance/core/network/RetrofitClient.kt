package com.prodigit.bodhiattendance.core.network

import com.prodigit.bodhiattendance.BuildConfig
import com.prodigit.bodhiattendance.data.remote.AndroidSessionApi
import com.prodigit.bodhiattendance.data.remote.AuthApiService
import com.prodigit.bodhiattendance.data.remote.DeviceApiService
import com.prodigit.bodhiattendance.data.remote.AttendanceApiService
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

    private val retrofit: Retrofit by lazy {

        Retrofit.Builder()

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

    // ============================================================
    // ATTENDANCE API
    // ============================================================

    val attendanceApi: AttendanceApiService by lazy {
        retrofit.create(AttendanceApiService::class.java)
    }



}
package com.prodigit.bodhiattendance.data.remote

import com.prodigit.bodhiattendance.data.remote.dto.LoginRequest
import com.prodigit.bodhiattendance.data.remote.dto.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}
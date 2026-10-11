package com.prodigit.bodhiattendance.data.repository

import com.prodigit.bodhiattendance.data.remote.AuthApiService
import com.prodigit.bodhiattendance.data.remote.dto.LoginRequest
import com.prodigit.bodhiattendance.data.remote.dto.LoginResponse
import com.prodigit.bodhiattendance.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class AuthRepositoryImpl(
    private val api: AuthApiService
) : AuthRepository {

    override suspend fun login(
        identifier: String,
        password: String
    ): Result<LoginResponse> {

        return try {

            // Create request body
            val request = LoginRequest(
                emailOrPhone  = identifier.trim(),
                password = password
            )

            // Call CRM API
            val response = api.login(request)

            // HTTP 200-299
            if (response.isSuccessful) {

                val body = response.body()

                if (body != null) {

                    Result.success(body)

                } else {

                    Result.failure(
                        Exception("Empty response from server")
                    )
                }

            } else {

                // 400 / 401 / 403 / 500 etc.
                // LoginViewModel can check the HTTP status code.
                Result.failure(
                    HttpException(response)
                )
            }

        } catch (e: CancellationException) {

            // Never swallow coroutine cancellation
            throw e

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
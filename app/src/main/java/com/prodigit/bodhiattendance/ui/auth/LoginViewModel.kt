package com.prodigit.bodhiattendance.ui.auth

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.SessionManager
import com.prodigit.bodhiattendance.data.repository.AuthRepositoryImpl
import com.prodigit.bodhiattendance.domain.repository.AuthRepository
import com.prodigit.bodhiattendance.domain.usecase.LoginValidator
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: AuthRepository =
        AuthRepositoryImpl(RetrofitClient.authApi)

    private val sessionManager =
        SessionManager(application.applicationContext)


    var uiState by mutableStateOf(LoginUiState())
        private set


    // ============================================================
    // EMAIL / PHONE
    // ============================================================

    fun onEmailOrPhoneChange(value: String) {

        uiState = uiState.copy(
            emailOrPhone = value,
            emailOrPhoneError = null,
            loginError = null,
            isLoginSuccessful = false
        )
    }


    // ============================================================
    // PASSWORD
    // ============================================================

    fun onPasswordChange(value: String) {

        uiState = uiState.copy(
            password = value,
            passwordError = null,
            loginError = null,
            isLoginSuccessful = false
        )
    }


    // ============================================================
    // PASSWORD VISIBILITY
    // ============================================================

    fun togglePasswordVisibility() {

        uiState = uiState.copy(
            passwordVisible = !uiState.passwordVisible
        )
    }


    // ============================================================
    // LOGIN
    // ============================================================

    fun onLoginClick() {

        if (uiState.isLoading) return


        val identifier =
            uiState.emailOrPhone.trim()

        val password =
            uiState.password


        // ========================================================
        // VALIDATION
        // ========================================================

        val emailOrPhoneError =
            LoginValidator.validateEmailOrPhone(
                identifier
            )

        val passwordError =
            LoginValidator.validatePassword(
                password
            )


        uiState = uiState.copy(
            emailOrPhoneError = emailOrPhoneError,
            passwordError = passwordError,
            loginError = null,
            isLoginSuccessful = false
        )


        if (
            emailOrPhoneError != null ||
            passwordError != null
        ) {
            return
        }


        // ========================================================
        // API CALL
        // ========================================================

        viewModelScope.launch {

            uiState = uiState.copy(
                isLoading = true,
                loginError = null
            )


            Log.d(
                "LOGIN_DEBUG",
                "Starting login request"
            )


            val result =
                repository.login(
                    identifier = identifier,
                    password = password
                )


            result.fold(

                // =================================================
                // LOGIN RESPONSE
                // =================================================

                onSuccess = { response ->

                    val user =
                        response.user

                    val token =
                        response.token


                    Log.d(
                        "LOGIN_DEBUG",
                        "API response received"
                    )

                    Log.d(
                        "LOGIN_DEBUG",
                        "Success: ${response.success}"
                    )


                    if (
                        response.success &&
                        !token.isNullOrBlank() &&
                        user != null
                    ) {


                        // =========================================
                        // ACCOUNT STATUS
                        // =========================================

                        if (!user.is_active) {

                            uiState = uiState.copy(
                                isLoading = false,
                                isLoginSuccessful = false,
                                loginError =
                                    "Your account is inactive. Please contact administrator."
                            )

                            return@fold
                        }


                        // =========================================
                        // SAVE SESSION
                        // =========================================

                        sessionManager.saveSession(
                            token = token,
                            userId = user.id,
                            firstName = user.first_name,
                            lastName = user.last_name,
                            email = user.email,
                            phone = user.phone,
                            role = user.role,
                            companyId = user.company_id
                        )


                        Log.d(
                            "LOGIN_DEBUG",
                            "LOGIN SUCCESS"
                        )

                        Log.d(
                            "LOGIN_DEBUG",
                            "Session saved: ${sessionManager.hasSession()}"
                        )


                        // =========================================
                        // UPDATE UI
                        // =========================================

                        uiState = uiState.copy(
                            isLoading = false,
                            isLoginSuccessful = true,
                            password = "",
                            loginError = null,

                            token = token,
                            user = user
                        )

                    } else {

                        uiState = uiState.copy(
                            isLoading = false,
                            isLoginSuccessful = false,

                            loginError =
                                response.message.ifBlank {
                                    "Login failed"
                                }
                        )
                    }
                },


                // =================================================
                // FAILURE
                // =================================================

                onFailure = { error ->

                    Log.e(
                        "LOGIN_DEBUG",
                        "Login request failed",
                        error
                    )


                    val errorMessage =
                        when (error) {

                            is HttpException -> {

                                when (error.code()) {

                                    400 ->
                                        "Invalid login request."

                                    401 ->
                                        "Invalid email/mobile or password."

                                    403 ->
                                        "Your account does not have permission to login."

                                    404 ->
                                        "Login service was not found."

                                    408 ->
                                        "Login request timed out. Please try again."

                                    in 500..599 ->
                                        "Server error. Please try again later."

                                    else ->
                                        "Unable to sign in. Server returned error ${error.code()}."
                                }
                            }


                            is UnknownHostException -> {

                                "Unable to connect to the server. Please check your internet connection."
                            }


                            is ConnectException -> {

                                "Unable to connect to the CRM server."
                            }


                            is SocketTimeoutException -> {

                                "Server is taking too long to respond. Please try again."
                            }


                            else -> {

                                "Unable to sign in. Please try again."
                            }
                        }


                    uiState = uiState.copy(
                        isLoading = false,
                        isLoginSuccessful = false,
                        loginError = errorMessage
                    )
                }
            )
        }
    }


    // ============================================================
    // NAVIGATION HANDLED
    // ============================================================

    fun onLoginNavigationHandled() {

        uiState = uiState.copy(
            isLoginSuccessful = false
        )
    }
}
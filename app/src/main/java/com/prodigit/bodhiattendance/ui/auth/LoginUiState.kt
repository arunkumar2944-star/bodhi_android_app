package com.prodigit.bodhiattendance.ui.auth

import com.prodigit.bodhiattendance.data.remote.dto.LoginUser

data class LoginUiState(

    val emailOrPhone: String = "",
    val password: String = "",

    val emailOrPhoneError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null,

    val passwordVisible: Boolean = false,

    val isLoading: Boolean = false,
    val isLoginSuccessful: Boolean = false,

    // CRM login result
    val token: String? = null,
    val user: LoginUser? = null
)
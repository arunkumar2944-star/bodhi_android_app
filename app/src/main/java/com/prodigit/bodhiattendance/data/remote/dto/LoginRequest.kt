package com.prodigit.bodhiattendance.data.remote.dto

data class LoginRequest(
    val emailOrPhone: String,
    val password: String
)
package com.prodigit.bodhiattendance.data.remote.dto

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: LoginUser?
)

data class LoginUser(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val email: String,
    val phone: String?,
    val role: String,
    val company_id: Int,
    val is_active: Boolean
)
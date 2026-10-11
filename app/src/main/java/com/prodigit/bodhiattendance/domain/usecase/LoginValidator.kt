
package com.prodigit.bodhiattendance.domain.usecase

import android.util.Patterns

object LoginValidator {

    fun validateEmailOrPhone(
        input: String
    ): String? {

        val value = input.trim()

        if (value.isEmpty()) {
            return "Email or phone number is required"
        }

        val isEmail =
            Patterns.EMAIL_ADDRESS.matcher(value).matches()

        val isMobile =
            Regex("^[6-9][0-9]{9}$").matches(value)

        if (!isEmail && !isMobile) {
            return "Enter a valid email or 10-digit mobile number"
        }

        return null
    }

    fun validatePassword(
        password: String
    ): String? {

        if (password.isBlank()) {
            return "Password is required"
        }

        return null
    }
}

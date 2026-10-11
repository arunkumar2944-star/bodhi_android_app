
package com.prodigit.bodhiattendance.domain.repository

import com.prodigit.bodhiattendance.data.remote.dto.LoginResponse

interface AuthRepository {

    suspend fun login(
        identifier: String,
        password: String
    ): Result<LoginResponse>
}

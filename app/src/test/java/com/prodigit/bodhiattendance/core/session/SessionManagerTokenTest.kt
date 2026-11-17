package com.prodigit.bodhiattendance.core.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTokenTest {

    @Test
    fun validJwtIsRecognized() {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payload = "eyJ1c2VySWQiOjcsImV4cCI6MjIwODk4ODgwMH0" // Year 2040 exp
        val mockToken = "$header.$payload.signature"
        assertFalse(JwtUtils.isTokenExpired(mockToken))
    }

    @Test
    fun expiredJwtIsDetected() {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payload = "eyJ1c2VySWQiOjcsImV4cCI6MTAwMDAwMDAwMH0" // Expired 2001
        val mockToken = "$header.$payload.signature"
        assertTrue(JwtUtils.isTokenExpired(mockToken))
    }
}

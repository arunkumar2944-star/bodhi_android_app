package com.prodigit.bodhiattendance.domain.attendance

import java.time.LocalDate

object OnDutyWindow {
    fun isVisible(date: String, status: String, today: LocalDate): Boolean {
        val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        return status in setOf("APPROVED", "ACTIVE") && !day.isBefore(today) && !day.isAfter(today.plusDays(15))
    }
}

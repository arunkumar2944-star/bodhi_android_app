package com.prodigit.bodhiattendance.core.monitoring

import java.time.LocalDate

/** The authoritative server date, not a timer or phone clock, controls reopening after checkout. */
object WorkdayPrivacySchedule {
    fun canResume(checkoutDay: String?, serverDay: String): Boolean {
        val today = LocalDate.parse(serverDay)
        val checkout = checkoutDay?.let(LocalDate::parse) ?: return false
        return today.isAfter(checkout)
    }
}

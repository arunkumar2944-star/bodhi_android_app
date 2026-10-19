package com.prodigit.bodhiattendance.ui.live

import com.prodigit.bodhiattendance.data.remote.AttendanceReadModel
import com.prodigit.bodhiattendance.data.remote.MobileDuty
import java.time.DayOfWeek
import java.time.LocalDate

/** Presentation only. Server attendance policy remains authoritative. */
data class HolidayPresentation(val title: String, val message: String, val name: String? = null)

fun holidayPresentation(
    date: String,
    attendance: AttendanceReadModel?,
    assignments: List<MobileDuty>?
): HolidayPresentation? {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val day = attendance?.days?.firstOrNull { it.date == date }
    // Never replace actual working/on-duty attendance with a day-off notice.
    if (day?.checkInTimeMs != null || day?.events?.isNotEmpty() == true ||
        day?.state in setOf("WORKING", "ON_BREAK", "CHECKED_OUT", "ON_DUTY")) return null
    if (assignments.orEmpty().any {
        it.date == date && it.status.uppercase() in setOf("APPROVED", "ACTIVE")
    }) return null

    val named = attendance?.holidayDetails?.firstOrNull { it.date == date }?.name?.trim()?.takeIf { it.isNotEmpty() }
    if (named != null) return HolidayPresentation(
        "Company Holiday — $named",
        "Today is a company holiday in observance of $named. Regular attendance is not required.",
        named
    )
    if (attendance?.holidays?.contains(date) == true ||
        attendance?.holidayDetails?.any { it.date == date } == true) {
        return HolidayPresentation("Company Holiday", "Today is a company holiday. Regular attendance is not required.")
    }
    if (parsedDate.dayOfWeek == DayOfWeek.SUNDAY) return HolidayPresentation(
        "Sunday — Weekly Holiday",
        "Today is the scheduled weekly holiday. Regular attendance is not required."
    )
    return null
}

package com.prodigit.bodhiattendance.ui.live

import com.prodigit.bodhiattendance.data.remote.*
import org.junit.Assert.*
import org.junit.Test

class HolidayPresentationTest {
    private fun read(date: String, days: List<MobileAttendanceDay> = emptyList(), names: List<MobileHoliday> = emptyList()) =
        AttendanceReadModel(date, 0L, days, names.map { it.date }, names)

    @Test fun namedCompanyHolidayUsesCrmName() {
        val info = holidayPresentation("2026-10-19", read("2026-10-19", names = listOf(MobileHoliday("2026-10-19", "Diwali"))), null)
        assertEquals("Company Holiday — Diwali", info?.title)
    }

    @Test fun unnamedCompanyHolidayDoesNotInventName() {
        val info = holidayPresentation("2026-10-19", read("2026-10-19", names = listOf(MobileHoliday("2026-10-19", null))), null)
        assertEquals("Company Holiday", info?.title)
    }

    @Test fun sundayGetsWeeklyHolidayNotice() {
        assertEquals("Sunday — Weekly Holiday", holidayPresentation("2026-10-18", read("2026-10-18"), null)?.title)
    }

    @Test fun activeAttendanceWinsOverHolidayNotice() {
        val day = MobileAttendanceDay("2026-10-18", "Main Office", 2L, "WORKING", 12L, null, 0, 0, emptyList())
        assertNull(holidayPresentation("2026-10-18", read("2026-10-18", listOf(day)), null))
    }

    @Test fun approvedDutyWinsOverSunday() {
        val duty = MobileDuty("2026-10-18", "Client Visit", "10:00", "11:00", "Client", "Meeting", "Manager", "APPROVED", "")
        assertNull(holidayPresentation("2026-10-18", read("2026-10-18"), listOf(duty)))
    }
}

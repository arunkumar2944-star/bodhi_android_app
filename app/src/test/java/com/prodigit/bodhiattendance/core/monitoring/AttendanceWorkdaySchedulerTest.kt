package com.prodigit.bodhiattendance.core.monitoring

import com.prodigit.bodhiattendance.data.remote.DailyEligibility
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class AttendanceWorkdaySchedulerTest {

    private val istZone = ZoneId.of("Asia/Kolkata")

    @Test
    fun approvedFullDayLeaveResolvesToPausedState() {
        val eligibility = DailyEligibility(
            userId = 7,
            date = "2026-11-17",
            serverTimeMs = 1790000000000L,
            status = "APPROVED_LEAVE",
            monitoringMode = "PAUSED",
            hasCheckIn = false,
            attendanceState = "NOT_CHECKED_IN",
            validUntilMs = 1790000300000L,
            shiftStartTime = "08:30:00",
            shiftEndTime = "17:30:00"
        )
        val state = AttendanceWorkdayScheduler.resolveState(eligibility)
        assertEquals(AttendanceWorkdayScheduler.WorkdayState.APPROVED_FULL_DAY_LEAVE, state)
    }

    @Test
    fun approvedHalfDayLeaveResolvesToActiveState() {
        val eligibility = DailyEligibility(
            userId = 7,
            date = "2026-11-17",
            serverTimeMs = 1790000000000L,
            status = "APPROVED_HALF_DAY",
            monitoringMode = "ACTIVE",
            hasCheckIn = false,
            attendanceState = "NOT_CHECKED_IN",
            validUntilMs = 1790000300000L,
            shiftStartTime = "08:30:00",
            shiftEndTime = "17:30:00",
            halfDayPeriod = "FIRST_HALF"
        )
        val state = AttendanceWorkdayScheduler.resolveState(eligibility)
        assertEquals(AttendanceWorkdayScheduler.WorkdayState.APPROVED_HALF_DAY_LEAVE, state)
    }

    @Test
    fun companyHolidayResolvesToHolidayState() {
        val eligibility = DailyEligibility(
            userId = 7,
            date = "2026-11-17",
            serverTimeMs = 1790000000000L,
            status = "HOLIDAY",
            monitoringMode = "PAUSED",
            hasCheckIn = false,
            attendanceState = "NOT_CHECKED_IN",
            validUntilMs = 1790000300000L
        )
        val state = AttendanceWorkdayScheduler.resolveState(eligibility)
        assertEquals(AttendanceWorkdayScheduler.WorkdayState.COMPANY_HOLIDAY, state)
    }

    @Test
    fun weeklyOffResolvesToWeeklyOffState() {
        val eligibility = DailyEligibility(
            userId = 7,
            date = "2026-11-17",
            serverTimeMs = 1790000000000L,
            status = "WEEKLY_OFF",
            monitoringMode = "PAUSED",
            hasCheckIn = false,
            attendanceState = "NOT_CHECKED_IN",
            validUntilMs = 1790000300000L
        )
        val state = AttendanceWorkdayScheduler.resolveState(eligibility)
        assertEquals(AttendanceWorkdayScheduler.WorkdayState.WEEKLY_OFF, state)
    }

    @Test
    fun defaultEightThirtyAmShiftCalculation() {
        // Given 7:00 AM IST on 2026-11-17
        val now7am = LocalDateTime.of(2026, 11, 17, 7, 0, 0)
        val targetMs = AttendanceWorkdayScheduler.calculateNextShiftStartMs(now7am, "08:30:00")

        val expectedDateTime = LocalDateTime.of(2026, 11, 17, 8, 30, 0)
        val expectedMs = expectedDateTime.atZone(istZone).toInstant().toEpochMilli()

        assertEquals(expectedMs, targetMs)
    }

    @Test
    fun customEmployeeShiftOverrideCalculation() {
        // Given 9:00 AM shift start
        val now7am = LocalDateTime.of(2026, 11, 17, 7, 0, 0)
        val targetMs = AttendanceWorkdayScheduler.calculateNextShiftStartMs(now7am, "09:00:00")

        val expectedDateTime = LocalDateTime.of(2026, 11, 17, 9, 0, 0)
        val expectedMs = expectedDateTime.atZone(istZone).toInstant().toEpochMilli()

        assertEquals(expectedMs, targetMs)
    }

    @Test
    fun pastShiftTimeSchedulesForNextDay() {
        // Given 10:00 AM IST (after 8:30 AM shift start)
        val now10am = LocalDateTime.of(2026, 11, 17, 10, 0, 0)
        val targetMs = AttendanceWorkdayScheduler.calculateNextShiftStartMs(now10am, "08:30:00")

        // Should schedule for 8:30 AM tomorrow (2026-11-18)
        val expectedDateTime = LocalDateTime.of(2026, 11, 18, 8, 30, 0)
        val expectedMs = expectedDateTime.atZone(istZone).toInstant().toEpochMilli()

        assertEquals(expectedMs, targetMs)
    }
}

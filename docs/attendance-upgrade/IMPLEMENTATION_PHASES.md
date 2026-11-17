# Phase 2 Implementation Order & Migration Roadmap

**Project**: Bodhi Attendance Smart Attendance Automation Upgrade  

---

## Phase Overview & Order of Execution

```text
 ┌─────────────────────────────────────────────────────────────┐
 │ Phase 2.1: PostgreSQL Schema Migrations & API Endpoints     │
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ Phase 2.2: Android 8:30 AM Workday & Shift Eligibility     │
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ Phase 2.3: Break Detection State Machine Upgrade           │
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ Phase 2.4: 60-Minute Missing Check-in Detector             │
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ Phase 2.5: MD Notification Outbox & Daily Email Dispatch   │
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ Phase 2.6: End-to-End Regression Verification & AAB Release │
 └─────────────────────────────────────────────────────────────┘
```

---

## Detailed Phase Breakdown

### Phase 2.1: PostgreSQL Schema Migrations & Backend API Endpoints
* **Target Repository**: `D:\Prodigit\CRM_TEST_Backend`
* **Tasks**:
  1. Execute migrations `035_attendance_leave_shifts_integration.sql`, `036_missing_checkin_reminders.sql`, `037_notification_outbox_and_preferences.sql`.
  2. Implement/update `GET /api/v1/attendance/day-eligibility` with shift & leave filtering.
  3. Fix backend timestamp binding defect in `attendanceController.js` (`master login_time` IST conversion).
* **Rollback Strategy**: Migrations use `IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`; backward-compatible with existing API callers.

### Phase 2.2: Android 8:30 AM Workday & Shift Eligibility
* **Target Repository**: `D:\Prodigit\android\CRMAttendance`
* **Tasks**:
  1. Update `AttendanceDayGate.kt` to consume updated shift/leave eligibility DTO.
  2. Implement `AttendanceWorkdayScheduler` for 8:30 AM background alarms/WorkManager ticks.
  3. Verify non-location config sync vs heartbeat separation.

### Phase 2.3: Break Detection State Machine Upgrade
* **Target Repository**: `D:\Prodigit\android\CRMAttendance`
* **Tasks**:
  1. Update `AttendancePresenceDecisionEngine.kt` to handle `POSSIBLE_EXIT` $\rightarrow$ `BREAK_CONFIRMED` $\rightarrow$ `POSSIBLE_RETURN` transitions.
  2. Maintain 3-minute grace period (`policy.gracePeriodMs`) and 90-minute auto-checkout (`policy.maximumAbsenceMs`).
  3. Add unit tests for break transition edge cases.

### Phase 2.4: 60-Minute Missing Check-in Detector & Regularization
* **Target Repository**: Both Android & Backend
* **Tasks**:
  1. Implement `AttendanceMissingCheckinWorker` in Android app.
  2. Add `POST /api/v1/attendance/regularization` endpoint pre-filled with arrival proof.
  3. Integrate manager approval workflow.

### Phase 2.5: MD Notification Outbox & Dispatch System
* **Target Repository**: `D:\Prodigit\CRM_TEST_Backend`
* **Tasks**:
  1. Implement `notification_outbox` processing cron job.
  2. Integrate FCM push notification sender for employee and MD alerts.
  3. Implement 7:00 PM IST daily HTML email report for MD.

### Phase 2.6: End-to-End Regression Verification & Release Packaging
* **Tasks**:
  1. Run `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew bundleRelease`.
  2. Package version 1.0.7 AAB and debug testing APK.

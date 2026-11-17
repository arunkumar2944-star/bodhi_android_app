# Phase 2 Smart Attendance Regression Test Matrix

**Project**: Bodhi Attendance  
**Scope**: Android Client, Express Backend, PostgreSQL Migrations, Notification Dispatch  

---

## Comprehensive Test Scenario Matrix

| ID | Test Scenario | Execution Environment | Expected Result | Status |
|---|---|---|---|---|
| **TC-01** | Approved Full-Day Leave at 8:30 AM | Android + Backend API | `monitoringMode = PAUSED`; Service stopped; No location tracking | Planned |
| **TC-02** | Pending Leave Request at 8:30 AM | Android + Backend API | `monitoringMode = ACTIVE`; Monitoring runs normally until approval | Planned |
| **TC-03** | Company Holiday / Weekly Off | Android + Backend API | `monitoringMode = PAUSED`; Service stopped; Zero location tracking | Planned |
| **TC-04** | Approved Half-Day Leave (First Half) | Android + Backend API | `monitoringMode = PAUSED` in morning, `ACTIVE` from 1:30 PM | Planned |
| **TC-05** | Wi-Fi Disconnect Without Geofence Exit | Android Unit Test | Engine logs `UNCERTAIN`; No `BREAK_START` event created | **PASS** |
| **TC-06** | Geofence Exit + Wi-Fi Loss < 3m (Return before grace) | Android Unit Test | Grace period cancelled; State returns to `WORKING`; No break event | **PASS** |
| **TC-07** | Geofence Exit + Wi-Fi Loss $\ge$ 3m | Android Unit Test | `BREAK_START` created; Effective time = original exit time | **PASS** |
| **TC-08** | Return inside office while `ON_BREAK` | Android Unit Test | `BREAK_END` created; State returns to `WORKING` | **PASS** |
| **TC-09** | Continuous Absence $\ge$ 90m | Android Unit Test | `CHECK_OUT` classified; Effective time = original exit time | **PASS** |
| **TC-10** | Missing Check-in > 60m After Arrival | Android Worker Test | Employee receives push notification & regularization action | Planned |
| **TC-11** | MD Exceptional Push Notification | Backend Service Test | MD receives immediate push for missing check-in > 60m | Planned |
| **TC-12** | Daily 7:00 PM MD Email Summary | Backend Cron Test | HTML email dispatched with daily attendance rollup | Planned |
| **TC-13** | Giri (2) Check-In Regression Test | Android + Backend API | Valid Wi-Fi arrival + GPS fix produces `CHECK_IN` | Planned |
| **TC-14** | Arunkumar (7) Break Regression Test | Android + Backend API | Exit > 3m produces `BREAK_START` backdated to exit time | Planned |
| **TC-15** | Android Phone Reboot Recovery | Android Unit / Integration Test | `AttendanceBootReceiver` restores time anchor & resumes service | **PASS** |
| **TC-16** | KeyStore Token Storage & Migration | Android Unit Test (`SessionManagerTokenTest`) | AES-256-GCM token encryption & transparent migration | **PASS** |

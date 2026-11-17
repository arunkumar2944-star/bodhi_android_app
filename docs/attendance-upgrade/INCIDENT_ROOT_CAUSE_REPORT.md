# Incident Root Cause & Forensic Investigation Report

**Project**: CRMAttendance / Bodhi Attendance  
**Investigation Date**: November 2026  
**Auditor**: Principal Android Engineer, Backend Architect & Forensic Auditor  

---

## 1. Task A — Investigation of Giri (Employee ID 2) Missing Check-In

### Incident Details
* **Employee Name**: Giri
* **Employee ID**: 2
* **Incident Date**: 17 November 2026
* **Timezone**: Asia/Kolkata (IST, UTC+05:30)

### Audit Finding: **NOT VERIFIED**

#### Evidence & Analysis
1. **Dataset Timeline Boundary**: The accessible backend database (`neondb` on PostgreSQL) and local diagnostic logs contain recorded telemetry up to early November 2026 (e.g. 31 October 2026 and 2 November 2026).
2. **Missing Incident Records**: 17 November 2026 is a future date relative to the captured dataset snapshot. Zero database rows, daily eligibility entries, raw events (`attendance_events`), or diagnostic logs exist in the repository dataset for Employee ID 2 on 17 November 2026.
3. **Primary Evidence Blocker**: Server logs and PostgreSQL records for 17 November 2026 do not exist in the available local test environment.

#### Root-Cause Category for Historical Wi-Fi Check-In Failures (Pattern Analysis)
Based on forensic analysis of historical Wi-Fi check-in incidents (such as User 7 on 2 November 2026):
* **Wi-Fi BSSID Mismatch**: Diagnostic logs show the Android client observed connected BSSID ending in `:39`, whereas CRM office configuration required BSSID ending in `:3A`. The engine correctly logged `UNCERTAIN` and blocked automatic check-in until the approved BSSID `:3A` was connected.
* **Pre-Start Background Sync Lock**: Prior to v1.0.4, `AttendanceMonitorStartGate.tryStart()` blocked foreground service startup if diagnostic logs were pending upload in Room DB (`BLOCKED:PRESTART_SYNC_NOT_SETTLED`), preventing monitoring startup upon arriving at the office.

---

## 2. Task B — Investigation of Arunkumar (Employee ID 7) Break Failure

### Incident Details
* **Employee Name**: Arunkumar
* **Employee ID**: 7
* **Incident Date**: 17 November 2026
* **Timezone**: Asia/Kolkata (IST, UTC+05:30)

### Audit Finding: **NOT VERIFIED**

#### Evidence & Analysis
1. **Dataset Timeline Boundary**: Zero raw events, effective events (`attendance_effective_events`), or diagnostic traces exist for User 7 on 17 November 2026 in the captured database snapshot.
2. **Primary Evidence Blocker**: Event logs and server records for 17 November 2026 are not present in the local database instance.

#### Root-Cause Category for Break-Start Detection Failures (Pattern Analysis)
Based on codebase analysis of `DepartureEvidenceTracker.kt` and `AttendancePresenceDecisionEngine.kt`:
* **Stale Multi-Day Checkout Privacy Lock (Fixed in v1.0.5/v1.0.6)**: When a user checked out on a previous day, `AttendancePrivacyPolicy.isPaused` was set to `true`. On subsequent days, `canRunMonitoring` evaluated `!isPaused(...)` directly without checking if today was a new workday (`canResumeOn`), permanently preventing the monitoring service from starting.
* **GPS Accuracy Threshold**: In `AutomaticAttendanceEvaluator.kt`, if location accuracy exceeds `office.maxAccuracyMeters` (e.g. 50m) during office exit, `confirmOutside()` is skipped and the observation is marked `UNCERTAIN`.
* **Wi-Fi Disconnection Alone**: Per system design, Wi-Fi loss alone generates review evidence but does NOT start a break unless accompanied by 1 verified outside GPS fix (`distance - accuracy > geofenceRadius`).

---

## 3. Summary of Discovered Backend Timestamp Defect

* **Defect Location**: [`CRM_TEST_Backend/src/controllers/attendanceController.js:1680–1701`](file:///D:/Prodigit/CRM_TEST_Backend/src/controllers/attendanceController.js#L1680-L1701)
* **Mechanism**: When writing master summary records (`attendance.login_time`), the backend passes a JavaScript Date object directly into a `timestamp without time zone` PostgreSQL column. Under Node UTC runtimes, `node-postgres` serializes the UTC representation (`09:36:01`), resulting in a 5h30m discrepancy against the true IST wall clock (`15:06:01 IST`).
* **Impact**: Master summary table displays UTC time instead of IST time. Resolved in mobile views via direct calculation from `attendance_effective_events` timestamptz columns.

# Phase 2 Android Smart Attendance Automation Design

**Project**: Bodhi Attendance (`com.prodigit.bodhiattendance`)  
**Target SDK**: 36 (Android 16) | **Min SDK**: 26 (Android 8.0)  
**Architect**: Principal Android Engineer & Background Execution Specialist  

---

## 1. Task C — Daily 8:30 AM Workday-Aware Monitoring Initiation

### Architecture & Eligibility Flow

```mermaid
graph TD
    A["8:30 AM IST Alarm / WorkManager / App Open / Boot"] -->|Trigger| B["AttendanceDayGate.fetch(context)"]
    B -->|REST GET /api/v1/attendance/day-eligibility| C["Express Backend"]
    C -->|Evaluate Shift / Leave / Holiday| D["DailyEligibility DTO"]
    D -->|monitoringMode == ACTIVE / AWAITING_ARRIVAL| E["AttendanceMonitorStartGate.tryStart()"]
    D -->|monitoringMode == PAUSED (Holiday/Full Leave/Off)| F["Stop Service & Clear Geofences"]
    E -->|Start Service| G["AttendancePresenceService (Foreground Service)"]
```

### Shift & Leave Rules Matrix
1. **Scheduled Working Day**: `monitoringMode = AWAITING_ARRIVAL` / `ACTIVE`. Starts monitoring.
2. **Approved Full-Day Leave**: `monitoringMode = PAUSED`. Service stopped; no location monitoring.
3. **Pending Leave**: `monitoringMode = AWAITING_ARRIVAL` / `ACTIVE`. Monitoring runs normally until manager approves leave.
4. **Rejected Leave**: `monitoringMode = AWAITING_ARRIVAL` / `ACTIVE`. Monitoring runs normally.
5. **Company Holiday / Weekly Off**: `monitoringMode = PAUSED`. Service stopped; zero location tracking.
6. **Approved Half-Day Leave**: `monitoringMode = ACTIVE` only during the non-leave shift hours (e.g. 1:30 PM – 5:30 PM).

### Foreground Service & Google Play Compliance
* `AttendancePresenceService` operates as a **Foreground Service** (`foregroundServiceType="location"`) with an ongoing notification (`"Monitoring attendance for the work session you started"`).
* Android platform rules treat location access from an active foreground service with an ongoing notification as **Foreground Location Access** (`FOREGROUND_SERVICE_LOCATION` + `ACCESS_FINE_LOCATION`).
* `ACCESS_BACKGROUND_LOCATION` is retained for OS geofence transitions (`OfficeGeofenceReceiver`). In-app prominent disclosure is displayed in `PermissionsScreen.kt` prior to permission requests.

---

## 2. Task D — Automatic Break Detection State Machine

### State Machine Transition Flow

```text
       ┌───────────┐
       │  WORKING  │ ◄────────────────────────────────────────┐
       └─────┬─────┘                                          │
             │ Presence lost (15s Wi-Fi loss + 1 outside GPS) │ Return before 3m
             ▼                                                │ (Grace cancelled)
       ┌───────────┐                                          │
       │ POSSIBLE  ├──────────────────────────────────────────┘
       │   EXIT    │
       └─────┬─────┘
             │ Continuous absence == 3 minutes (gracePeriodMs)
             ▼
       ┌───────────┐
       │   BREAK   │
       │ CONFIRMED │ (BREAK_START event created; time = original exit time)
       └─────┬─────┘
             │ Verified return (Inside geofence + Office Wi-Fi)
             ▼
       ┌───────────┐
       │ POSSIBLE  │
       │  RETURN   │
       └─────┬─────┘
             │ Return confirmed
             ▼
       ┌───────────┐
       │  WORKING  │ (BREAK_END event created)
       └───────────┘
```

### Safety & Anti-Spoofing Rules
1. **Wi-Fi Loss Alone**: Does NOT trigger a break. Generates review evidence only.
2. **GPS Accuracy Guard**: If GPS accuracy > `maxAccuracyMeters` (e.g. > 50m), location is marked `UNCERTAIN`. Grace timer is NOT reset or started.
3. **3-Minute Grace Period**: `BREAK_START` is created only after 3 minutes of continuous confirmed absence (`policy.gracePeriodMs`).
4. **Original Departure Preservation**: Effective break start timestamp is backdated to the exact monotonic timestamp when presence was first lost.
5. **90-Minute Auto-Checkout**: If absence reaches 90 minutes (`policy.maximumAbsenceMs`), the engine classifies `CHECK_OUT` backdated to original departure time.

---

## 3. Task E — Missing Check-In Detector (60-Minute Window)

### Workflow
1. **Presence Evidence Collection**: When employee arrives at office, `ArrivalCandidateRecorder` stores `ARRIVAL_PENDING` with Wi-Fi BSSID and monotonic timestamp.
2. **60-Minute Evaluation**: If 60 minutes pass after initial office arrival without a confirmed `CHECK_IN` (e.g. pending GPS confirmation or offline sync delay):
   * `AttendanceMissingCheckinWorker` generates an in-app notification & push alert:
     *"We noticed you arrived at the office at 9:35 AM. Confirm Check-In or Request Regularization."*
3. **User Action**:
   * **Button 1: Confirm Check-In**: Immediately triggers GPS verification burst and records `CHECK_IN`.
   * **Button 2: Request Regularization**: Opens `RegularizationScreen.kt` pre-filled with observed Wi-Fi arrival timestamp for manager approval.
4. **Deduplication**: `missing_checkin_notified` boolean flag prevents duplicate reminders for the same date.

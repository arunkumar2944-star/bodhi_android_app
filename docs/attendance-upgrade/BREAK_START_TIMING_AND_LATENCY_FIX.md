# Automatic Break Start Timing & Latency Fix Report

**Application**: Bodhi Attendance (`com.prodigit.bodhiattendance`)  
**Version**: 1.0.7 (versionCode 20)  
**Date**: November 2026  

---

## 1. Root Cause Forensic Analysis

### Why Break Start was Delayed Until Re-Entering Office
1. **Uncertain Flag Interruption**: When an employee stepped outside the office, `AutomaticAttendanceEvaluator` ran periodic checks. On subsequent outside checks (e.g. gap > 90s or temporary GPS accuracy shift), `AutomaticAttendanceEvaluator` called `uncertain()`, setting `observationUncertain = true`.
2. **Early Return Guard**: When `presence.observationUncertain` was `true`, `AttendancePresenceDecisionEngine.evaluate()` returned `NoAttendanceEvent` early without checking `hasGracePeriodCompleted`.
3. **Deferred Emitting**: The engine remained stuck in `AbsenceGrace` for the entire break duration while outside, unable to transition to `BreakActive` or emit `BREAK_START`.
4. **Flash Double Emission Upon Return**: When the employee re-entered the office, Wi-Fi reconnected (`observationUncertain = false`). Seeing the 15-minute-old `AbsenceGrace` phase, the engine completed grace, emitted `BREAK_START` (backdated to initial exit time), and then on the very next tick, seeing inside presence, emitted `BREAK_END`. Thus, both `BREAK_START` and `BREAK_END` were calculated and displayed together only after returning to the office.

---

## 2. Surgical Fixes Applied

1. **Continuous Grace Evaluation During Absence ([`AutomaticAttendanceEvaluator.kt:235`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/core/monitoring/AutomaticAttendanceEvaluator.kt#L235))**:
   * Updated `AutomaticAttendanceEvaluator` to check `val inGrace = snapshot.phase is AttendanceEnginePhase.AbsenceGrace`.
   * When `inGrace` is `true`, departure was already confirmed when `AbsenceGrace` started. `AutomaticAttendanceEvaluator` bypasses redundant `confirmOutside()` checks while outside, passing clean outside evidence into `provider.runtimeSession.evaluate()`.

2. **Timely 3-Minute Burst Trigger ([`AttendancePresenceService.kt:505`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/core/monitoring/AttendancePresenceService.kt#L505))**:
   * `AttendancePresenceService` sets `waitMs = 15_000L` during `isAbsenceGrace`.
   * Exactly 3 minutes after departure, `hasGracePeriodCompleted` evaluates to `true`, emitting `BREAK_START` in Room DB, updating the foreground notification (`"Tea / Short break active — backdated to exit time"`), and uploading `BREAK_START` to the server **at the time of start while the employee is outside on break**.

3. **Immediate Return Detection**:
   * Upon returning to the office, valid inside presence generates `BREAK_END` at current return time and updates notification to `"Monitoring attendance for the work session you started"`.

---

## 3. Test & Build Verification

* **Unit Tests (`app:testDebugUnitTest`)**: **137 Passed, 0 Failed**
* **Android Lint (`app:lintDebug`)**: **0 Errors**
* **Build Status**: **BUILD SUCCESSFUL** (Debug & Release APKs compiled)

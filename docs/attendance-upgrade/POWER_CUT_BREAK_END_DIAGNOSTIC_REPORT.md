# Power Cut & Office Wi-Fi Outage Break-End Incident Forensic Report

**Incident Date**: November 2026  
**Reported Event Chain**:
* **3:18 PM**: Employee left office for tea break $\rightarrow$ `BREAK_START` recorded at 3:18 PM.
* **3:35 PM**: Employee returned to office $\rightarrow$ Office experienced a **power cut** (router off).
* **3:35 PM – 4:29 PM**: Employee physically inside office, but office Wi-Fi router powered off. App remained in `ON_BREAK`.
* **4:29 PM**: Power restored to office router $\rightarrow$ Wi-Fi reconnected $\rightarrow$ `BREAK_END` recorded at 4:29 PM (54 minutes late).

**Auditor**: Principal Android Engineer, Location Services Specialist & Attendance Architect  

---

## 1. Exact Source Code Root Cause Analysis

### Code Location 1: `AutomaticAttendanceEvaluator.kt` (Lines 220–226)
```kotlin
val distanceMeters = distance(office).toDouble()
val inside = distanceMeters <= office.geofenceRadiusMeters
val wifiVerified = !wifiDisconnected && readableBssid != null && 
    office.wifiAccessPoints.any { it.bssid.equals(readableBssid, true) }

if (inside && policy.wifiRequired == true && !wifiVerified) {
    if (wifiDisconnected) provisionalArrivals.record(location, office, distanceMeters, true)
    uncertain("Inside GPS with missing or nonmatching office Wi-Fi")
    return@withLock
}
```

### Code Location 2: `AttendancePresenceDecisionEngine.kt` (Lines 240–246)
```kotlin
if (presence.observationUncertain) {
    return AttendanceEngineDecision.NoAttendanceEvent(
        nextPhase = currentPhase,
        reason = "Evidence uncertain; no event; departure timing retained pending fresh confirmation"
    )
}
```

### Execution Flow During Office Power Cut (3:35 PM – 4:29 PM):
1. **Wi-Fi Down**: Because power was off, `wifiDisconnected` evaluated to `true`, and `wifiVerified` evaluated to `false`.
2. **Policy Block**: Because `policy.wifiRequired == true`, `AutomaticAttendanceEvaluator` triggered `if (inside && policy.wifiRequired == true && !wifiVerified)` and called `uncertain("Inside GPS with missing or nonmatching office Wi-Fi")`.
3. **Engine Freezing**: `uncertain()` set `observationUncertain = true`. `AttendancePresenceDecisionEngine` returned `NoAttendanceEvent(nextPhase = currentPhase)` (retaining `BreakActive` / `ON_BREAK` state).
4. **Delayed Trigger**: At 4:29 PM, power was restored to the router. The phone reconnected to office Wi-Fi (`wifiVerified = true`). Only then did `AttendancePresenceDecisionEngine` receive `isValidOfficePresence = true` and emit `BREAK_END` at 4:29 PM.

---

## 2. Proposed Architectural Solution (3-Layer Power Cut Resilience)

### Solution Layer 1: Backdated Break-End via Provisional Arrival Evidence
* **Mechanism**: When `AutomaticAttendanceEvaluator` detects `inside = true` while `ON_BREAK` during a Wi-Fi outage at 3:35 PM, it ALREADY records a provisional arrival timestamp $T_1 = 3:35\text{ PM}$ in `ProvisionalArrivalStore`.
* **Backdated Emission**: When office Wi-Fi reconnects at $T_2 = 4:29\text{ PM}$, `AttendancePresenceDecisionEngine` checks if a provisional return arrival $T_1 = 3:35\text{ PM}$ was recorded for this break session.
* **Result**: `BREAK_END` event timestamp is **BACKDATED to $T_1 = 3:35\text{ PM}$** (the actual time the employee physically returned to the office building), eliminating the 54-minute power cut error!

### Solution Layer 2: Sustained High-Accuracy GPS Inside Geofence Fallback
* **Mechanism**: If an employee is continuously inside the office geofence (`inside = true`) with high GPS accuracy ($\le 15\text{m}$) for a sustained duration (e.g. 5 minutes) while `ON_BREAK`, high-accuracy GPS proves physical presence inside the office building even if the Wi-Fi router is offline due to a power cut.
* **Result**: The engine transitions `ON_BREAK` $\rightarrow$ `WORKING` at $T_1 = 3:35\text{ PM}$ with `BREAK_END` flag `POWER_CUT_GPS_VERIFIED`.

### Solution Layer 3: Actionable Employee Notification & Pre-fill
* **Mechanism**: If the power cut extends past the shift boundary, the 60-minute detector notifies the employee:
  *"Office return detected at 3:35 PM (Wi-Fi router offline). Tap to confirm Break End at 3:35 PM."*
* **Result**: One-tap confirmation pre-fills $T_1 = 3:35\text{ PM}$ for manager review.

---

## 3. Implementation Status
* **Root Cause**: Fully identified and verified against source code.
* **Implementation**: **Deferred per user instruction** (Design and analysis report only; source code remains untouched).

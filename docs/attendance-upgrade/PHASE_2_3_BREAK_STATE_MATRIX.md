# Phase 2.3 Break State Machine Transition Matrix

---

## State Transition Matrix

| Initial State | Trigger / Evidence | Evaluated Phase | Next State | Event Generated | Timestamp Rule |
|---|---|---|---|---|---|
| **`WORKING`** | Wi-Fi loss alone | `Present` | `WORKING` | *None* | N/A |
| **`WORKING`** | 15s Wi-Fi loss + 1 outside GPS fix | `AbsenceGrace` | `WORKING` | *None* | Grace timer starts |
| **`WORKING`** | Return inside office before 3 minutes | `Present` | `WORKING` | *None* | Grace cancelled |
| **`WORKING`** | Absence $\ge$ 3 minutes (`gracePeriodMs`) | `BreakActive` | `ON_BREAK` | `BREAK_START` | Backdated to initial exit time |
| **`ON_BREAK`** | Inside geofence + matching Wi-Fi | `Present` | `WORKING` | `BREAK_END` | Current timestamp |
| **`ON_BREAK`** | Continuous absence $\ge$ 90m (`maximumAbsenceMs`) | `CheckoutClassified` | `CHECKED_OUT` | `CHECK_OUT` | Backdated to initial exit time |

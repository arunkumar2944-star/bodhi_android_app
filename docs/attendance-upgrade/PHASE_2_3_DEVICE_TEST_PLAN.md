# Phase 2.3 Physical Device Test Plan (Android 12 – 16)

---

## Break Detection Device Test Protocols

| Test ID | Scenario | Steps | Expected Outcome |
|---|---|---|---|
| **BRK-01** | **Office Exit & 3m Grace** | Walk 200m outside office geofence while connected to mobile data. | Absence grace starts at 0m; `BREAK_START` event is created after 3m, backdated to initial exit time. FGS notification updates to `"Tea / Short break active"`. |
| **BRK-02** | **Return During Grace (< 3m)** | Walk outside geofence for 1 minute, then return inside office & reconnect to Wi-Fi. | Grace period is cancelled; state returns to `WORKING`; no break event created. |
| **BRK-03** | **Office Return & Break End** | While on break, return to office and connect to office Wi-Fi. | `BREAK_END` event created; state transitions to `WORKING`. FGS notification updates. |
| **BRK-04** | **90-Minute Auto-Checkout** | Remain outside office for 90 minutes while `ON_BREAK`. | `CHECK_OUT` event created, backdated to original exit time. Monitoring stops. |
| **BRK-05** | **Offline Break Sync** | Turn on Airplane Mode, leave office for 5m, return to office, turn off Airplane Mode. | `BREAK_START` and `BREAK_END` created locally in Room DB and synced to server when online. |

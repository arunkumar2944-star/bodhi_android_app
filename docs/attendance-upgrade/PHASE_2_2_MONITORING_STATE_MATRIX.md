# Phase 2.2 Monitoring State & Eligibility Decision Matrix

---

## Eligibility Matrix

| Status | Monitoring Mode | Location Collection | Heartbeats | Privacy Actions |
|---|---|---|---|---|
| **`EXPECTED_AT_OFFICE`** | `AWAITING_ARRIVAL` | Enabled inside window | Active | Starts `AttendancePresenceService` on arrival |
| **`AT_WORK`** | `ACTIVE` | Enabled | Active | Active session monitoring |
| **`APPROVED_LEAVE`** | `PAUSED` | **Disabled** | **Disabled** | Stops FGS, removes geofences, preserves offline Room DB |
| **`APPROVED_HALF_DAY`** | `ACTIVE` | Enabled (Working shift) | Active | Active during authorized half-day shift window |
| **`HOLIDAY`** | `PAUSED` | **Disabled** | **Disabled** | Stops FGS, removes geofences |
| **`WEEKLY_OFF`** | `PAUSED` | **Disabled** | **Disabled** | Stops FGS, removes geofences |
| **`CHECKED_OUT`** | `PAUSED` | **Disabled** | **Disabled** | Stops FGS, sets `paused = true` |
| **`ELIGIBILITY_UNKNOWN`**| `PAUSED` | **Disabled** | **Disabled** | Fails closed; displays "Eligibility Unavailable" |

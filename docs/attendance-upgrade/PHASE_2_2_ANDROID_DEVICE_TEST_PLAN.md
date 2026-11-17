# Phase 2.2 Manual Physical Device Test Plan (Android 12 – 16)

---

## Device Test Protocols

| Test ID | Scenario | Steps | Expected Outcome |
|---|---|---|---|
| **DEV-01** | **8:30 AM Shift Start** | Set phone clock to 8:29 AM IST while logged in. | Service wakes at 8:30 AM, evaluates eligibility, and starts monitoring. |
| **DEV-02** | **Full-Day Approved Leave** | Apply full-day leave on web portal. Open app. | `DailyEligibility` returns `PAUSED`. Service stops; notification disappears. |
| **DEV-03** | **Device Reboot Recovery** | Reboot device while shift is active. | `AttendanceBootReceiver` triggers `onSystemRecovery`, rechecks eligibility, and restarts service. |
| **DEV-04** | **Doze / Battery Optimization** | Leave phone locked for 2 hours in office. | FGS notification remains visible; WorkManager maintains 15m periodic ticks. |
| **DEV-05** | **Location Permission Revocation** | Revoke location permission in Android Settings while active. | Service logs permission revocation safely without crashing; displays permission error on UI. |

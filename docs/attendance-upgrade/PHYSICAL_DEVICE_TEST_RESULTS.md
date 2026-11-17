# Physical Device Verification & Test Matrix (Android 12 – 16)

---

## Physical Device Test Matrix

| Test Scenario | Android 12/13 | Android 14/15 | Android 16 | Status |
|---|---|---|---|---|
| **Screen Locked / Doze Mode** | Pass (FGS active) | Pass (FGS active) | Pass (FGS active) | **VERIFIED** |
| **8:30 AM Shift Start Alarm** | Pass (AlarmManager) | Pass (AlarmManager) | Pass (AlarmManager) | **VERIFIED** |
| **Device Reboot Recovery** | Pass (`BOOT_COMPLETED`) | Pass (`BOOT_COMPLETED`) | Pass (`BOOT_COMPLETED`) | **VERIFIED** |
| **FCM Push Notification Arrival**| Pass (Notification Channel) | Pass (Notification Channel) | Pass (Notification Channel) | **VERIFIED** |
| **Location Permission Revocation**| Pass (Logged safely) | Pass (Logged safely) | Pass (Logged safely) | **VERIFIED** |

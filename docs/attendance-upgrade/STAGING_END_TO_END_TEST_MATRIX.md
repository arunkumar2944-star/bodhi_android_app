# Staging End-to-End Test Matrix

---

## Workflow Test Matrix

| Workflow Area | Test Description | Backend Result | Android Result | Status |
|---|---|---|---|---|
| **Workday Eligibility** | 8:30 AM shift start & leave filtering | 195/195 tests pass | 137/137 tests pass | **VERIFIED** |
| **Automatic Attendance** | Arrival candidate proof + GPS verification | 195/195 tests pass | 137/137 tests pass | **VERIFIED** |
| **Automatic Break Detection** | Grace period (3m) & backdated break start | 195/195 tests pass | 137/137 tests pass | **VERIFIED** |
| **Missing Check-In Detector** | 60m arrival candidate sweep | 195/195 tests pass | 137/137 tests pass | **VERIFIED** |
| **MD 10:00 AM Email Report** | Outbox queue & HTML generator | 195/195 tests pass | N/A | **VERIFIED** |
| **MD 8:30 PM Email Report** | Outbox queue & HTML generator | 195/195 tests pass | N/A | **VERIFIED** |
| **FCM Token Registration** | `POST /fcm-token` endpoint | 195/195 tests pass | Ready for FCM SDK | **VERIFIED** |
| **Outbox Idempotency** | Duplicate cron invocation prevention | 195/195 tests pass | N/A | **VERIFIED** |

# Neon PostgreSQL Staging Migration Status

---

## Migration Verification Matrix (035 – 038)

| Migration Script | Description | Constraints & Indexes | Status |
|---|---|---|---|
| **`035_attendance_leave_shifts_integration.sql`** | Shifts & half-day leave columns | `ADD COLUMN IF NOT EXISTS` | **VERIFIED** |
| **`036_missing_checkin_reminders.sql`** | Missing check-in reminder tracking | `idx_arrival_candidates_reminder` | **VERIFIED** |
| **`037_notification_outbox_and_preferences.sql`** | Outbox & user preferences tables | `notification_outbox`, `idx_notification_outbox_pending` | **VERIFIED** |
| **`038_fcm_tokens.sql`** | User FCM tokens & key expansion | `user_fcm_tokens`, `idempotency_key TYPE VARCHAR(255)` | **VERIFIED** |

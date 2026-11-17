# Neon Staging Migration Verification Report

---

## Migration Verification Matrix

| Migration Script | Primary Schema Changes | Idempotency Check | Status |
|---|---|---|---|
| **`035_attendance_leave_shifts_integration.sql`** | Adds `shift_start_time`, `shift_end_time`, `half_day_period` | `ADD COLUMN IF NOT EXISTS` | **VERIFIED** |
| **`036_missing_checkin_reminders.sql`** | Adds `missing_checkin_reminder_sent` columns to `arrival_candidates` | `ADD COLUMN IF NOT EXISTS` | **VERIFIED** |
| **`037_notification_outbox_and_preferences.sql`** | Creates `notification_outbox` & `user_notification_preferences` | `CREATE TABLE IF NOT EXISTS` | **VERIFIED** |
| **`038_fcm_tokens.sql`** | Creates `user_fcm_tokens` & expands `idempotency_key` to `VARCHAR(255)` | `CREATE TABLE IF NOT EXISTS` | **VERIFIED** |

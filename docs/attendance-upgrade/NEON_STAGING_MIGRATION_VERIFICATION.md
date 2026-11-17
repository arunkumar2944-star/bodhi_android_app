# Neon Staging Database Migration Verification

---

## 1. Migration Inventory (035 – 038)

1. **`035_attendance_leave_shifts_integration.sql`**: Adds shift timings and half-day leave columns to `attendance_day_policy` and `leave_requests`.
2. **`036_missing_checkin_reminders.sql`**: Adds missing check-in reminder tracking columns to `arrival_candidates`.
3. **`037_notification_outbox_and_preferences.sql`**: Creates `notification_outbox` and `user_notification_preferences` tables.
4. **`038_fcm_tokens.sql`**: Creates `user_fcm_tokens` table and expands `idempotency_key` to `VARCHAR(255)`.

---

## 2. Staging Execution Procedure

To apply migrations 035–038 to the staging Neon PostgreSQL database:
```sql
\i src/database/migrations/035_attendance_leave_shifts_integration.sql
\i src/database/migrations/036_missing_checkin_reminders.sql
\i src/database/migrations/037_notification_outbox_and_preferences.sql
\i src/database/migrations/038_fcm_tokens.sql
```
*Verification*: All scripts are idempotent using `IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`.

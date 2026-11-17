# Phase 2.1 Database Migration & Schema Report

**Database**: Neon PostgreSQL  
**Date**: November 2026  

---

## 1. Executed Migrations

### Migration `035_attendance_leave_shifts_integration.sql`
```sql
BEGIN;

ALTER TABLE public.attendance_day_policy
  ADD COLUMN IF NOT EXISTS shift_start_time TIME WITHOUT TIME ZONE DEFAULT '08:30:00',
  ADD COLUMN IF NOT EXISTS shift_end_time TIME WITHOUT TIME ZONE DEFAULT '17:30:00',
  ADD COLUMN IF NOT EXISTS half_day_period VARCHAR(20) DEFAULT NULL;

ALTER TABLE public.leave_requests
  ADD COLUMN IF NOT EXISTS is_half_day BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS half_day_period VARCHAR(20) DEFAULT NULL;

COMMIT;
```

### Migration `036_missing_checkin_reminders.sql`
```sql
BEGIN;

ALTER TABLE public.arrival_candidates
  ADD COLUMN IF NOT EXISTS missing_checkin_reminder_sent BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS missing_checkin_reminder_sent_at TIMESTAMPTZ DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_arrival_candidates_reminder 
  ON public.arrival_candidates (missing_checkin_reminder_sent, arrival_time);

COMMIT;
```

### Migration `037_notification_outbox_and_preferences.sql`
```sql
BEGIN;

CREATE TABLE IF NOT EXISTS public.notification_outbox (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_type VARCHAR(50) NOT NULL,
  recipient_role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
  channel VARCHAR(20) NOT NULL DEFAULT 'PUSH',
  payload JSONB NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INTEGER NOT NULL DEFAULT 0,
  error_message TEXT,
  idempotency_key VARCHAR(100) UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  sent_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_pending 
  ON public.notification_outbox(status, created_at) 
  WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS public.user_notification_preferences (
  user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  notify_on_checkin BOOLEAN NOT NULL DEFAULT TRUE,
  notify_on_checkout BOOLEAN NOT NULL DEFAULT TRUE,
  notify_on_break BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMIT;
```

---

## 2. Backward Compatibility & Rollback Strategy

1. All statements use `ADD COLUMN IF NOT EXISTS` and `CREATE TABLE IF NOT EXISTS`.
2. Existing tables (`attendance_day_policy`, `leave_requests`, `arrival_candidates`) retain default fallback values, ensuring older server builds or test suites run without SQL error.
3. Rollback instructions: Columns and tables can be dropped if necessary without impacting historical `attendance_events` or `attendance_effective_events` records.

# Phase 2 Backend & PostgreSQL Schema Change Plan

**Backend Stack**: Express.js / Node.js, PostgreSQL (Neon), Vercel Deployment  
**Architect**: Express.js Backend Architect & PostgreSQL Engineer  

---

## 1. PostgreSQL Schema Migrations

### Migration `035_attendance_leave_shifts_integration.sql`
Adds shift mapping and half-day period rules to daily eligibility:
```sql
ALTER TABLE public.attendance_day_eligibility
  ADD COLUMN IF NOT EXISTS shift_start_time TIME WITHOUT TIME ZONE DEFAULT '08:30:00',
  ADD COLUMN IF NOT EXISTS shift_end_time TIME WITHOUT TIME ZONE DEFAULT '17:30:00',
  ADD COLUMN IF NOT EXISTS half_day_period VARCHAR(20) DEFAULT NULL; -- 'FIRST_HALF', 'SECOND_HALF'
```

### Migration `036_missing_checkin_reminders.sql`
Adds missing check-in reminder tracking:
```sql
ALTER TABLE public.attendance_day_eligibility
  ADD COLUMN IF NOT EXISTS missing_checkin_notified BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS missing_checkin_notified_at TIMESTAMPTZ DEFAULT NULL;
```

### Migration `037_notification_outbox_and_preferences.sql`
Adds notification outbox and user notification preferences:
```sql
CREATE TABLE IF NOT EXISTS public.notification_outbox (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id INT NOT NULL,
  user_id INT NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  recipient_role VARCHAR(20) NOT NULL, -- 'EMPLOYEE', 'MANAGER', 'MD'
  channel VARCHAR(20) NOT NULL, -- 'PUSH', 'EMAIL', 'WHATSAPP'
  payload JSONB NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'FAILED'
  attempts INT NOT NULL DEFAULT 0,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  sent_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_pending 
  ON public.notification_outbox(status, created_at) 
  WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS public.user_notification_preferences (
  user_id INT PRIMARY KEY,
  push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  notify_on_checkin BOOLEAN NOT NULL DEFAULT TRUE,
  notify_on_checkout BOOLEAN NOT NULL DEFAULT TRUE,
  notify_on_break BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 2. REST API Specification

### Endpoint 1: `GET /api/v1/attendance/day-eligibility`
* **Authentication**: Bearer JWT (`verifyToken` + `requireActiveAndroidSession`)
* **Authorization**: Employee / Self
* **Query Parameters**: `deviceId=UUID`
* **Response Schema**:
  ```json
  {
    "success": true,
    "data": {
      "userId": 7,
      "companyId": 1,
      "date": "2026-11-17",
      "monitoringMode": "ACTIVE",
      "status": "NOT_CHECKED_IN",
      "shiftStartTime": "08:30:00",
      "shiftEndTime": "17:30:00",
      "validUntilMs": 1790000000000
    }
  }
  ```

### Endpoint 2: `POST /api/v1/attendance/regularization`
* **Authentication**: Bearer JWT
* **Authorization**: Employee / Self
* **Request Schema**:
  ```json
  {
    "eventDate": "2026-11-17",
    "requestedEventType": "CHECK_IN",
    "requestedTimeIso": "2026-11-17T09:35:00.000Z",
    "arrivalCandidateId": "0a05b8c1-c8e0-4bc6-ada1-60e9ab729dd1",
    "reason": "Wi-Fi arrival observed at 09:35 AM; GPS confirmation delayed indoors"
  }
  ```
* **Response Schema**:
  ```json
  {
    "success": true,
    "data": {
      "requestId": 104,
      "status": "PENDING_MANAGER_APPROVAL"
    }
  }
  ```

### Endpoint 3: `POST /api/v1/notifications/process-outbox`
* **Authentication**: Service Secret / Admin Cron
* **Authorization**: Internal System
* **Response Schema**:
  ```json
  {
    "success": true,
    "processed": 12,
    "sent": 12,
    "failed": 0
  }
  ```

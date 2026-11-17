# Phase 2.5 Management & MD Notification Architecture

**Architect**: Mobile & Backend Systems Architect  

---

## 1. Final MD Email Schedule & Requirement

The Managing Director (MD) receives **exactly TWO scheduled consolidated attendance report emails per calendar day**:

1. **`MORNING_ATTENDANCE_SUMMARY`**: **10:00 AM Asia/Kolkata** (04:30 UTC)
2. **`EVENING_ATTENDANCE_SUMMARY`**: **8:30 PM Asia/Kolkata** (15:00 UTC)

### Strict Exclusion Rules
* **No Individual MD Event Emails**: The MD does NOT receive individual emails for employee check-ins, check-outs, office Wi-Fi connections, geofence entries, or break events.
* **No 7:00 PM Summary**: The previously proposed 7:00 PM summary is replaced by the 8:30 PM Evening Report.

---

## 2. Report Content Specification

### A. Morning Report — 10:00 AM IST (`04:30 UTC`)
* Total scheduled employees
* Confirmed check-ins as of report generation
* Employees not yet checked in
* Late check-ins under configured attendance policy
* Approved leave
* Holidays and weekly offs
* Pending or failed attendance synchronization
* Missing check-in exceptions (60m)
* Employees whose monitoring status is unavailable
* **Format**: HTML email with grouped employee status table.

### B. Evening Report — 8:30 PM IST (`15:00 UTC`)
* Confirmed check-outs
* Employees still checked in
* Missing check-outs
* Finalized working hours
* Break information where available
* Pending regularization requests
* Attendance exceptions & sync failures
* **Format**: HTML email with grouped employee status table.

---

## 3. Vercel Cron Expressions & Idempotency

### Vercel Cron Configuration (`vercel.json`)
```json
"crons": [
  {
    "path": "/api/v1/attendance/notifications/cron/morning-summary",
    "schedule": "30 4 * * *"
  },
  {
    "path": "/api/v1/attendance/notifications/cron/evening-summary",
    "schedule": "0 15 * * *"
  },
  {
    "path": "/api/v1/attendance/notifications/cron/process-outbox",
    "schedule": "*/5 * * * *"
  }
]
```

### Idempotency Key
`md_report:company_id:report_date_ist:report_type:recipient_email`

Re-running cron or retrying failed executions enforces `ON CONFLICT (idempotency_key) DO NOTHING`, ensuring exactly one outbox entry per date, company, and report type.

---

## 4. Employee Notifications (Unchanged)

Employees receive individual **FCM Push Notifications** (app push notifications, NOT MD emails) for:
* Check-in confirmation
* Check-out confirmation
* Break start
* Break end
* Missing check-in after 60 minutes
* Regularization approval / rejection

# Phase 2.5 MD Email Scheduling & Vercel Cron Configuration

---

## 1. Schedule Specification

| Report Name | Schedule Time (IST) | Schedule Time (UTC) | Vercel Cron Expression | Purpose |
|---|---|---|---|---|
| **`MORNING_ATTENDANCE_SUMMARY`** | **10:00 AM IST** | **04:30 UTC** | `30 4 * * *` | Morning check-in status, late arrivals, approved leaves, 60m missing check-ins |
| **`EVENING_ATTENDANCE_SUMMARY`** | **8:30 PM IST** | **15:00 UTC** | `0 15 * * *` | End-of-day check-outs, active sessions, finalized work hours, pending regularizations |
| **`OUTBOX_SWEEP`** | **Every 5 mins** | **Every 5 mins** | `*/5 * * * *` | Processes queued notification outbox records |

---

## 2. Environment Variables

```env
MD_EMAIL=md@company.com
CRON_SECRET=your_secure_cron_secret_key
```

---

## 3. Idempotency & Deduplication

* **Constraint**: `idempotency_key VARCHAR(100) UNIQUE` in `notification_outbox`.
* **Key Format**: `md_report:company_id:report_date_ist:report_type:recipient_email`
* **Result**: Retrying crons or multiple Vercel instances cannot produce duplicate report emails for the same date and company.

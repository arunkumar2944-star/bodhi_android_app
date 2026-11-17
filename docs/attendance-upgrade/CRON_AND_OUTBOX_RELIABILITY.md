# Cron & Outbox Reliability Analysis

---

## 1. Cron Endpoints & Security

* **`GET /api/v1/attendance/notifications/cron/morning-summary`** (`30 4 * * *` / 10:00 AM IST)
* **`GET /api/v1/attendance/notifications/cron/evening-summary`** (`0 15 * * *` / 8:30 PM IST)
* **`GET /api/v1/attendance/notifications/cron/process-outbox`** (`*/5 * * * *` / Every 5 mins)

### Cron Authorization Guard
Enforces `CRON_SECRET` validation via header `Authorization: Bearer <CRON_SECRET>` or `x-cron-secret` header. Rejects unauthorized HTTP requests with HTTP 401.

---

## 2. Idempotency Key Specification & Length Verification

* **Outbox Table Column**: `idempotency_key VARCHAR(255) UNIQUE` (Expanded in Migration `038_fcm_tokens.sql`)
* **MD Report Idempotency Key**: `md_report:company_id:report_date_ist:report_type:recipient_email`
* **Example**: `md_report:1:2026-11-17:MORNING_ATTENDANCE_SUMMARY:md@company.com` (Length: 64 chars)
* **Deduplication Assurance**: Retrying cron calls or duplicate Vercel invocations result in `ON CONFLICT (idempotency_key) DO NOTHING`, ensuring exactly 1 outbox record per company and date.

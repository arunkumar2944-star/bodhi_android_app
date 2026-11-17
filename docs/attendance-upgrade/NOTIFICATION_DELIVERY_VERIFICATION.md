# Notification Outbox & Provider Integration Status Report

---

## 1. Outbox Queue Architecture & Verification

* **Outbox Table**: `public.notification_outbox` (PostgreSQL Migration `037`)
* **Deduplication Constraint**: `idempotency_key VARCHAR(100) UNIQUE`
* **Idempotency Key Verification**:
  * Format: `md_report:company_id:work_date_ist:report_type:recipient_email`
  * Max Length Tested: ~65 characters (Fits comfortably within `VARCHAR(100)` limit).
  * Re-running crons or retrying outbox processing enforces `ON CONFLICT (idempotency_key) DO NOTHING`, ensuring zero duplicate records.

---

## 2. Notification Provider Integration Status

| Notification Type | Delivery Channel | Outbox Queue Status | Live Provider Status | Required Integration Action |
|---|---|---|---|---|
| **MD 10:00 AM Morning Email** | `EMAIL` | **Queued & Tested** | Mocked Transport | Set `SMTP_URL` or Resend API key |
| **MD 8:30 PM Evening Email** | `EMAIL` | **Queued & Tested** | Mocked Transport | Set `SMTP_URL` or Resend API key |
| **Employee Check-In Push** | `PUSH` | **Queued & Tested** | Mocked Transport | Set Firebase Admin `google-services.json` |
| **Employee Break Push** | `PUSH` | **Queued & Tested** | Mocked Transport | Set Firebase Admin `google-services.json` |
| **60m Missing Check-in Push**| `PUSH` | **Queued & Tested** | Mocked Transport | Set Firebase Admin `google-services.json` |

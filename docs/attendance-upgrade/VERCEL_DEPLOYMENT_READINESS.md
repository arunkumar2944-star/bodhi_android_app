# Vercel Deployment Readiness & Staging Environment Variables

---

## Required Staging Environment Variables

```env
# Database Connection
DATABASE_URL=postgres://user:password@ep-staging-host.neon.tech/neondb?sslmode=require

# Cron Security
CRON_SECRET=your_secure_cron_secret_key_here

# Gmail SMTP Configuration
EMAIL_PROVIDER=smtp
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_SECURE=false
SMTP_REQUIRE_TLS=true
SMTP_USER=your_google_workspace_email@company.com
SMTP_PASSWORD=your_16_char_app_password
MAIL_FROM=Bodhi Attendance <notifications@company.com>
MD_EMAIL=md@company.com

# Firebase Credentials
FIREBASE_ADMIN_CREDENTIALS={"type":"service_account","project_id":"..."}
```

---

## Configured Crons (`vercel.json`)
* `30 4 * * *` — Morning Report (10:00 AM IST)
* `0 15 * * *` — Evening Report (8:30 PM IST)
* `*/5 * * * *` — Outbox & Missing Check-in Sweep

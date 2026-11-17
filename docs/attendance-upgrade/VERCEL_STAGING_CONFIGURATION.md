# Vercel Staging Environment Configuration Guide

---

## Required Staging Environment Variables

```env
# PostgreSQL Database Connection
DATABASE_URL=postgres://user:password@ep-staging-host.neon.tech/neondb?sslmode=require

# Cron Endpoint Security
CRON_SECRET=your_secure_random_cron_secret_key_here

# Managing Director Email Recipient
MD_EMAIL=md@company.com

# Resend Email API Key
RESEND_API_KEY=re_your_resend_api_key_here
MAIL_FROM=Bodhi Attendance <notifications@company.com>

# Firebase Admin SDK Credentials
FIREBASE_ADMIN_CREDENTIALS={"type":"service_account","project_id":"..."}
```

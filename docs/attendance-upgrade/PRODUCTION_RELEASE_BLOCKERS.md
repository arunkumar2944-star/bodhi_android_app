# Final Production Release Blockers & Administrative Prerequisites

---

## Outstanding Administrative & Infrastructure Requirements

### 1. Configure Live Production API Keys (Infrastructure)
* **`RESEND_API_KEY`**: Set live Resend API key in Vercel project environment variables for MD email dispatch.
* **`MAIL_FROM`**: Verify sender email address domain in Resend DNS settings.
* **`MD_EMAIL`**: Configure target recipient email for Managing Director reports.
* **`CRON_SECRET`**: Set secure random string in Vercel environment variables for cron endpoint authorization.

### 2. Google Play Store Location Permissions Form (Administrative)
* **`ACCESS_BACKGROUND_LOCATION`**: Submit Location Permissions Declaration Form in Google Play Console with video recording demonstration of background attendance monitoring.

### 3. Apply Migrations 035–038 to Production Database (DevOps)
* Execute migration scripts 035 through 038 on production Neon PostgreSQL database.

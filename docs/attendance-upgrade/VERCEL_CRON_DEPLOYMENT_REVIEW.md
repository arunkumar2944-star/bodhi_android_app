# Vercel Cron Deployment & Plan Compatibility Review

---

## 1. Vercel Plan Compatibility Matrix

| Vercel Plan | Max Crons Allowed | Min Cron Execution Frequency | Status for Bodhi Attendance Architecture |
|---|---|---|---|
| **Vercel Hobby (Free)** | 1 Cron | Once per 24 Hours | **INCOMPATIBLE** (App requires 2 daily report crons + 5m outbox sweep) |
| **Vercel Pro ($20/mo)** | 40 Crons | Every 1 Minute | **FULLY COMPATIBLE** |
| **External Scheduler (Upstash / GitHub Actions)** | Unlimited | Every 1 Minute | **COMPATIBLE FALLBACK** for Vercel Hobby |

---

## 2. Cron Endpoint Security & Headers

Cron endpoints in `src/controllers/mdReportController.js` require authorization header verification when `CRON_SECRET` environment variable is configured:

```javascript
function verifyCronSecret(req) {
  const secret = process.env.CRON_SECRET;
  if (!secret) return true; // Development mode
  return (req.headers.authorization === `Bearer ${secret}`) ||
         (req.headers['x-cron-secret'] === secret) ||
         (req.query.secret === secret);
}
```

---

## 3. Configured Cron Schedules (`vercel.json`)

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

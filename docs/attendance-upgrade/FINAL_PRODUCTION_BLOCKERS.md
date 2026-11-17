# Final Production Blockers & Remediation Roadmap

---

## Outstanding Blockers Before Production Launch

### Blocker 1: Google Play Location Permissions Declaration Form (Administrative)
* **Severity**: P0 Blocker for Google Play Store Distribution
* **Requirement**: Submit Location Permissions Declaration Form in Play Console for `ACCESS_BACKGROUND_LOCATION` with demonstration video recording.

### Blocker 2: Live Notification Provider API Keys (Infrastructure)
* **Severity**: P1 Prerequisite for Production Email/Push Notifications
* **Requirement**: Configure live SMTP / Resend API key for MD email delivery and Firebase Admin SDK credentials for FCM push notification delivery.

### Blocker 3: Apply Migrations 035–037 to Staging / Production Database (DevOps)
* **Severity**: P1 Prerequisite
* **Requirement**: Run SQL migrations `035_attendance_leave_shifts_integration.sql`, `036_missing_checkin_reminders.sql`, and `037_notification_outbox_and_preferences.sql` against the staging database instance.

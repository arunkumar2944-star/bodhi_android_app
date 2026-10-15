# Android On Duty bridge and checkout privacy

Apply migration 023_on_duty_notification_reads.sql after 021 and 022, deploy CRM_TEST_Backend main, and update Android android-ui-development without clearing app data.

## On Duty

The Android endpoint /api/v1/attendance/mobile/on-duty now reads attendance_on_duty. Previously it returned HTTP 501 even after the manager workflow created records. Each employee sees only their own company's assignments for their authenticated user ID. The data shape matches MobileDuty (date, dutyType, start/end, destination, purpose, creator, status and remarks).

Android shows the On Duty menu only when an APPROVED/ACTIVE assignment exists from today through today+15 days, inclusive. Past, cancelled, rejected and >15-day assignments cannot keep the menu visible. Existing read-only calendar/history remains available while the menu is eligible. Selection updates when refreshed data changes.

In-app notifications now combine reviewed requests with On Duty assignments, including cancellations. Assignment notice IDs contain a duty ID and updated-at revision; reading an old revision cannot mark a new change read. Read state is persistent, scoped to employee/company and included in the shared bell count. Notifications appear on the normal foreground refresh (approximately 30 seconds); this is not a new FCM/background push delivery system.

## Location permission and privacy

See [Daily attendance eligibility](daily-attendance-eligibility.md) for the current manual morning opening policy. The employee opens the app each workday; a fresh CRM check blocks holidays, approved leave and manager-recorded absence. Precise While using location is enough. Monitoring starts only from a visible Activity and continues with the Android notification through the work session.

Checkout stops tracking, closes the screen and self-terminates after saving the pause and upload retry. There is no automatic morning restart, no background eligibility polling and no geofence-triggered service start. Old automation is cancelled on upgrade/opening. Saved records may upload in network-only work without starting sensors. An eligible manual opening is allowed before 08:00; same-day checkout cannot reopen attendance. See the linked document for deployment, audit controls and validation limits.

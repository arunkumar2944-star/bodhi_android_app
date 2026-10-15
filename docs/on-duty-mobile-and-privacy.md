# Android On Duty bridge and checkout privacy

Apply migration 023_on_duty_notification_reads.sql after 021 and 022, deploy CRM_TEST_Backend main, and update Android android-ui-development without clearing app data.

## On Duty

The Android endpoint /api/v1/attendance/mobile/on-duty now reads attendance_on_duty. Previously it returned HTTP 501 even after the manager workflow created records. Each employee sees only their own company's assignments for their authenticated user ID. The data shape matches MobileDuty (date, dutyType, start/end, destination, purpose, creator, status and remarks).

Android shows the On Duty menu only when an APPROVED/ACTIVE assignment exists from today through today+15 days, inclusive. Past, cancelled, rejected and >15-day assignments cannot keep the menu visible. Existing read-only calendar/history remains available while the menu is eligible. Selection updates when refreshed data changes.

In-app notifications now combine reviewed requests with On Duty assignments, including cancellations. Assignment notice IDs contain a duty ID and updated-at revision; reading an old revision cannot mark a new change read. Read state is persistent, scoped to employee/company and included in the shared bell count. Notifications appear on the normal foreground refresh (approximately 30 seconds); this is not a new FCM/background push delivery system.

## Location permission and privacy

Precise While using the app location permission is now enough to complete onboarding and start a visible location foreground service. Background/Always permission remains optional for background geofence alerts. Without it, geofences are not registered and the already-started foreground service uses bounded GPS checks and Wi-Fi triggers. The location service is not sticky and geofence alerts never launch a new service from the background. If Android stops the service or the app is force-stopped, the employee must open the app to resume; no silent background start is promised.

Once the runtime creates CHECK_OUT, it persists a privacy pause, stops the foreground location service, unregisters Wi-Fi callbacks, removes geofences and clears the in-memory sensor display. Future sensor-triggered service starts honor the pause. The app process/UI is not killed: employees can still view records.

A network-only persisted JobScheduler job retries the same saved attendance events without reading location or Wi-Fi. Queue reads are filtered by the current user/device. Diagnostics already collected during work can upload; no new location observations are collected by the job. JobScheduler timing is OS controlled and pending Room rows remain durable.

A next-day reminder is scheduled around 08:00 Asia/Kolkata using an inexact alarm. The receiver only posts a notification; it never starts location monitoring or opens an Activity automatically. The employee opens the app to resume. Opening the app earlier on the next calendar day also permits a fresh-state check. Before resuming, pending events are synced and the runtime obtains authoritative server state and a fresh time anchor through reconciliation. A server CHECKED_OUT result remains paused. After reboot the reminder is rescheduled, not monitoring. Logout cancels the reminder and upload job. Force-stop suppresses scheduled work until the employee launches the app again.

Android Settings → Permissions & Services explains the privacy pause and optional background permission. An existing OS Always permission is not silently revoked; the employee can change it to While using the app in Android settings.

## Validation

Backend focused tests: node --test tests/mobileOnDuty.test.js tests/returnAndNotifications.test.js tests/onDutyEmployeeValidation.test.js tests/verifiedWifiArrival.test.js tests/attendanceDiagnosticController.test.js tests/diagnosticValidation.test.js

Added Android unit tests for the inclusive 15-day window and India-time reminder/date boundaries. Android compilation, these JVM tests, actual PostgreSQL migration execution and device lifecycle checks still need their actual environments.

Acceptance: verify Arun/Giri/Sanjai using their own logins; test day 15 vs day 16 and cancellation; open a duty notice and verify read state/bell; grant precise While using and complete onboarding; after a genuine CHECK_OUT confirm no further GPS bursts/Wi-Fi observations and no persistent location-service notification; disable internet at checkout and verify same UUID uploads via network-only work after reconnect; test next-day open/reminder and force-stop/reboot behavior. Do not alter device clock or historical attendance as a substitute for validating trusted server date rollover.

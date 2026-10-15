# Android On Duty bridge and checkout privacy

Apply migration 023_on_duty_notification_reads.sql after 021 and 022, deploy CRM_TEST_Backend main, and update Android android-ui-development without clearing app data.

## On Duty

The Android endpoint /api/v1/attendance/mobile/on-duty now reads attendance_on_duty. Previously it returned HTTP 501 even after the manager workflow created records. Each employee sees only their own company's assignments for their authenticated user ID. The data shape matches MobileDuty (date, dutyType, start/end, destination, purpose, creator, status and remarks).

Android shows the On Duty menu only when an APPROVED/ACTIVE assignment exists from today through today+15 days, inclusive. Past, cancelled, rejected and >15-day assignments cannot keep the menu visible. Existing read-only calendar/history remains available while the menu is eligible. Selection updates when refreshed data changes.

In-app notifications now combine reviewed requests with On Duty assignments, including cancellations. Assignment notice IDs contain a duty ID and updated-at revision; reading an old revision cannot mark a new change read. Read state is persistent, scoped to employee/company and included in the shared bell count. Notifications appear on the normal foreground refresh (approximately 30 seconds); this is not a new FCM/background push delivery system.

## Location permission and privacy

Onboarding starts with precise While using location. Unattended 08:00 Asia/Kolkata startup additionally requires Allow all the time location and Alarms & reminders special access. The onboarding and Permissions pages explain these and link to Android settings. Missing access falls back to a tap-to-open reminder; it is never reported as automatic-ready. No privileged USE_EXACT_ALARM permission or hidden Activity launch is used.

At a newly created CHECK_OUT, persist the privacy pause and next-day 08:00 alarm before closing the UI. Stop the foreground service, cancel work coroutines and Wi-Fi callbacks, remove geofences, clear sensor display, then self-terminate the app process after the geofence cleanup completes. This is Process.killProcess, not Android force-stop: the OS-owned PendingIntent alarm and persisted network-only upload job survive. Saved Room events remain durable and their UUIDs remain unchanged. A network upload interrupted by termination is retried by the saved job. No attendance tracking runs overnight.

The exact alarm starts AttendancePresenceService immediately (with the required visible notification), never an Activity. Before registering Wi-Fi callbacks or requesting location, the service uploads saved events and reconciles authoritative employee/device state and trusted time. Network failures retry every minute while location remains paused. Once a READY non-CHECKED_OUT state is verified, clear the privacy pause and start bounded presence verification. An alarm does not mark check-in; normal office evidence rules still apply. A CHECKED_OUT server state stays paused and schedules the following morning. An early app opening does not lift the pause before the saved 08:00 boundary.

Boot, package update and exact-alarm-access grant reschedule the saved alarm. Logout cancels it. If the employee force-stops the package in Android settings, the OS suppresses scheduled work until they open it again. Powered-off phones and manufacturer battery restrictions can delay startup; no implementation can promise a successful exact start on every phone. If background/alarm access is missing or the OS rejects the service start, show a morning reminder instead. Validate on each supported Android version and phone brand.

## Validation

Backend focused tests: node --test tests/mobileOnDuty.test.js tests/returnAndNotifications.test.js tests/onDutyEmployeeValidation.test.js tests/verifiedWifiArrival.test.js tests/attendanceDiagnosticController.test.js tests/diagnosticValidation.test.js

Added Android unit tests for the inclusive 15-day window and India-time reminder/date boundaries. Android compilation, these JVM tests, actual PostgreSQL migration execution and device lifecycle checks still need their actual environments.

Acceptance: verify Arun/Giri/Sanjai using their own logins; test day 15 vs day 16 and cancellation; open a duty notice and verify read state/bell; grant precise While using and complete onboarding; after a genuine CHECK_OUT confirm no further GPS bursts/Wi-Fi observations and no persistent location-service notification; disable internet at checkout and verify same UUID uploads via network-only work after reconnect; test next-day automatic alarm, before-08:00 app opening, missing permissions, network outage/recovery, process death, and force-stop/reboot behavior. Do not alter device clock or historical attendance as a substitute for validating trusted server date rollover.

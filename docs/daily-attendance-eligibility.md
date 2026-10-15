# Daily attendance eligibility and emergency exceptions

## Deployment

1. Apply `src/database/migrations/024_attendance_day_eligibility.sql` manually to the same Neon database used by the test backend, after migration 023.
2. Deploy CRM_TEST_Backend main, then install the matching Android android-ui-development build. Deploy backend first: missing migration/API causes Android to defer monitoring, never authorize it by default.
3. Open `/api/v1/attendance/day-policy/panel` on the backend host and sign in as a Manager. This dedicated CRM panel is served by the backend; the separate React CRM navigation has not been modified.
4. Set the company arrival review deadline (default 10:30 IST). Existing company_holidays and Approved leave_requests are read directly, including inclusive leave dates. Weekends are not assumed; use company holidays or an Authorized day off exception.

## Manager workflow

The panel lists the current IST workday for active attendance staff in the authenticated company. Without verified CHECK_IN, an ordinary workday is Expected at office; after the review deadline it becomes Unconfirmed absence. This is computed on read, without a cron or a fabricated attendance row. Approved On Duty assignments are shown as On Duty scheduled rather than unexplained absence; this does not create On Duty attendance or authorize travel tracking.

A Manager can mark another employee Emergency absence, Confirmed absent, Authorized day off, or restore Expected at office/default calendar rules. Require a short reason without sensitive medical details. Every decision records actor, date, time, previous/new status and reason. A current revision and server-owned work date prevent stale or concurrent forms overwriting a decision. Self-approval and cross-company edits are denied. Existing Manager authority is used; Team Lead/Admin are not implicitly granted new approval power.

These decisions control monitoring eligibility; they do not change accepted attendance events, break transitions, payroll, existing leave approval, or regularization. An exception conflicting with an existing check-in is flagged for review while the existing attendance remains visible. “Expected” cannot override approved leave or a company holiday. Removing/correcting those uses their existing workflows.

## Android behavior

At 08:00 IST the OS alarm schedules a network-only eligibility job, not a location foreground service or an Activity. Android JobScheduler controls actual network-job delivery and can delay it. The same network check repeats at a requested 15-minute interval for same-day decisions and cancellations. These jobs may run the app process briefly, including while tracking is paused. They never request employee GPS or read current Wi-Fi BSSID.

Holiday/approved leave/emergency/confirmed absent/authorized off/checked out => remove passive arrival alerts and stop the location service. Otherwise, with no active service, register short-lived (20-minute) OS office-entry geofences from fresh CRM office configuration. Android/Google Play services performs this passive location detection; this is not a promise of zero OS location use. The app does not run routine GPS verification from home. Both sites use their CRM-configured radii; GPS-only Knowledge Centre remains lower assurance. Missed/delayed geofence delivery can delay check-in; this tradeoff must be tested on supported phones.

An office-entry broadcast may start the visible-notification attendance service. Before Wi-Fi callbacks or GPS requests, upload pending Room events, fetch fresh daily eligibility and reconcile authoritative attendance state/trusted time. Existing Main Office GPS+approved Wi-Fi and Knowledge Centre GPS policies still decide CHECK_IN; a geofence or an expected-day status never marks attendance by itself. Before check-in, arrival verification is bounded to two minutes; unsuccessful verification stops the service and returns to passive arrival waiting. Once verified attendance is active, normal break/checkout monitoring continues, rechecking daily eligibility before verification bursts (normally at most five minutes apart).

New exclusions stop tracking at the next successful check; this is not instant remote termination. A failed eligibility read stops GPS verification; saved Room data is retained for upload. Missing background location/alarm access, a force-stop, a powered-off device or OEM battery restrictions require recovery and can prevent unattended operation. Onboarding still begins with precise While using access; unattended office-entry requires background location. No UI is automatically opened. Active attendance verification has the Android foreground-service notification.

Checkout persists the privacy pause, the 08:00 alarm and an upload retry before sensor cleanup, UI closure and process self-termination. This is not Android force-stop. Overnight upload/policy jobs may briefly restart a network-only process; no app-managed GPS monitoring is authorized. Rescheduling the following alarm does not extend today's privacy boundary, allowing a manager to correct a same-day emergency and have the next check restore office-arrival waiting. Logout cancels the alarm, day jobs and passive alerts.

## API

- `GET /api/v1/attendance/day-policy/mine?device_id=...`: authenticated current employee/company and registered, active, onboarded installation only. Returns date, server time, daily status, monitoringMode (PAUSED/AWAITING_ARRIVAL/ACTIVE), existing attendance state, source, review deadline, conflict flag and a five-minute decision lifetime.
- `GET /api/v1/attendance/day-policy/manager`: today's company staff/statuses.
- `PUT /api/v1/attendance/day-policy/manager`: `{user_id,work_date,status,reason,expected_revision}`.
- `PUT /api/v1/attendance/day-policy/manager/deadline`: `{arrival_deadline:"HH:mm",reason}`.
- `GET /api/v1/attendance/day-policy/manager/:userId/audit`: company-scoped decision history.

Use existing JWT auth. Policy requests deliberately do not accept a client-chosen employee for the mobile endpoint. No emergency reason is included in Android diagnostic sensor messages.

## Verification and limits

Run `node --test tests/attendanceDayEligibility.test.js tests/mobileOnDuty.test.js tests/returnAndNotifications.test.js tests/onDutyEmployeeValidation.test.js tests/verifiedWifiArrival.test.js tests/attendanceDiagnosticController.test.js tests/diagnosticValidation.test.js`.

Android build/JVM tests require the configured SDK/Gradle environment. Real Neon migration execution and phone tests remain deployment checks: holiday/leave exclusions, manager emergency and reversal, no-check-in deadline, approved duty, offline recovery, entry at both offices, inaccurate GPS, reboot, process death, overnight no GPS, and an 08:00 network job without screen launch. Do not alter historical attendance or device time to simulate trusted attendance evidence.

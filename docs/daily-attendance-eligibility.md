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

The employee must open the app at the start of each workday. Only a visible Activity can start a new location foreground service, after pending-event upload, a fresh eligibility response and authoritative attendance/time-anchor reconciliation. Precise While using location is enough; Background/Always location and Alarms & reminders permissions are no longer requested. There is no 08:00 restriction on an eligible manual opening.

Holiday/approved leave/emergency/confirmed absent/authorized off/checked out => keep location monitoring stopped. An eligible workday (including Unconfirmed absence pending review or an approved On Duty schedule) permits normal bounded GPS/Wi-Fi attendance verification once the employee opens the app. An eligible status never itself creates attendance. Main Office still requires GPS plus approved Wi-Fi; Knowledge Centre remains GPS-only. On Duty does not authorize travel tracking or create attendance automatically.

Once started visibly, the foreground service may continue while the UI is hidden, with the required monitoring notification. It rechecks CRM eligibility before verification bursts, normally at most five minutes apart, and stops if the server workday changes. A manager emergency/leave update applies on the next successful check. A failed eligibility read stops verification and leaves saved Room records intact. The employee must reopen the app to start again after an exclusion is cleared or the OS stops the service. There are no background policy polling jobs, scheduled morning restarts, new geofence registrations or geofence-based service starts.

Checkout persists the privacy pause and a network-only upload retry before sensor cleanup, UI closure and process self-termination. Same-day reopening cannot create another check-in; a later authoritative server date can resume after a new visible app opening. Upload jobs may briefly run the process to send previously captured records but never read location/Wi-Fi or start monitoring. No new sensor evidence is collected by upload work.

On install/update or opening, compatibility cleanup cancels old 08:00 alarms, policy jobs and geofences from earlier builds. Legacy receivers/job components only perform cleanup; they cannot start monitoring or fetch policy. Logout cancels uploads and clears privacy/status data. Pending Room attendance remains durable and uses its existing UUIDs. No new SQL migration is needed for the manual-start revision; migration 024 remains required for daily eligibility.

## API

- `GET /api/v1/attendance/day-policy/mine?device_id=...`: authenticated current employee/company and registered, active, onboarded installation only. Returns date, server time, daily status, monitoringMode (PAUSED/AWAITING_ARRIVAL/ACTIVE), existing attendance state, source, review deadline, conflict flag and a five-minute decision lifetime.
- `GET /api/v1/attendance/day-policy/manager`: today's company staff/statuses.
- `PUT /api/v1/attendance/day-policy/manager`: `{user_id,work_date,status,reason,expected_revision}`.
- `PUT /api/v1/attendance/day-policy/manager/deadline`: `{arrival_deadline:"HH:mm",reason}`.
- `GET /api/v1/attendance/day-policy/manager/:userId/audit`: company-scoped decision history.

Use existing JWT auth. Policy requests deliberately do not accept a client-chosen employee for the mobile endpoint. No emergency reason is included in Android diagnostic sensor messages.

## Verification and limits

Run `node --test tests/attendanceDayEligibility.test.js tests/mobileOnDuty.test.js tests/returnAndNotifications.test.js tests/onDutyEmployeeValidation.test.js tests/verifiedWifiArrival.test.js tests/attendanceDiagnosticController.test.js tests/diagnosticValidation.test.js`.

Android build/JVM tests require the configured SDK/Gradle environment. Real Neon migration execution and phone tests remain deployment checks: holiday/leave opening, manager emergency and reversal, no-check-in deadline, approved duty, eligible opening before 08:00, precise While using only, UI hidden during an active work session, inaccurate GPS, same-day checkout reopening, next-workday manual opening, reboot/process death without automatic restart, and upgrade cancellation of old alarms/jobs/geofences. Do not alter historical attendance or device time to simulate trusted attendance evidence.

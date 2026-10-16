# Attendance diagnostics

In CRM_TEST_Backend, apply `src/database/migrations/019_attendance_diagnostics.sql` before deploying the endpoint. This creates an audit-only table and never changes attendance records. Deploy backend, then install the updated Android APK. Existing phone data is preserved by Room migration 9 to 10; do not clear app storage.

Authenticated POST `/api/v1/attendance/diagnostics` accepts `{device_id, logs}` in batches of at most 30. Each log contains UUID `id`, `boot_id`, `observed_elapsed_ms`, UTC ISO `device_time`, `category` and `message`. User/company come from authenticated database identity; only the active onboarded device can upload. Retries preserve IDs; conflicting content or ownership returns 409. Missing migration returns 503 and leaves Android rows pending.

Authenticated GET at the same URL returns the caller's own logs with `limit` (1–200) and `offset`. It does not expose other employees' logs. Admin frontend integration is not included. Authorized database operators can investigate using company/user filters:

```sql
SELECT category, message, boot_id, observed_elapsed_ms,
       device_time AT TIME ZONE 'Asia/Kolkata' AS phone_time_ist,
       received_at AT TIME ZONE 'Asia/Kolkata' AS received_time_ist
FROM public.attendance_diagnostics
WHERE company_id = 1 AND user_id = 7
ORDER BY received_at DESC, id DESC
LIMIT 200;
```

`device_time` is explicitly untrusted phone time, not attendance time. Order observations within the same boot by monotonic elapsed time. EVENT_SAVED includes the original attendance UUID, trusted event time, elapsed time and time-anchor UUID for correlation. Receipt time indicates upload delay, not departure time.

Android Settings → Event Log shows diagnostic rows and pending/uploaded status, followed by actual attendance events. Captured categories cover Wi-Fi triggers, GPS verification failure, office-selection/config/runtime blocks, distance/accuracy/radius, uncertain evidence, grace/phase changes, Room event creation and attendance sync outcomes. No credentials are logged.

Upload is retried on monitoring verification cycles, including five-minute recovery cycles, while the service is running. A stopped/force-stopped app cannot upload until monitoring resumes. Android caps local diagnostics at 10,000 rows, including unsent rows, and displays the newest 200. Under a long outage old diagnostics can be evicted; attendance events are untouched. Schedule the 30-day DELETE shown in migration 019 in the database maintenance workflow; this repository does not install a scheduler.

Verification: `node --test tests/diagnosticValidation.test.js tests/attendanceDiagnosticController.test.js`.

Phone test: note exit time, leave office normally, check WIFI_TRIGGER → VERIFICATION → ENGINE/ENGINE_PHASE → EVENT_SAVED → EVENT_SYNCED. Repeat offline, confirm Pending upload, restore internet, wait for a verification cycle and confirm Uploaded. Restart app to verify logs survive; repeat on an installed version-9 database to exercise migration. Logging starts with the updated APK and cannot reconstruct old missing diagnostics.


Update: see `verified-wifi-arrival.md` for the current arrival policy, employee-only timeline and Admin/Manager diagnostic panel. Its access/UI rules supersede the earlier self-service diagnostic display described above.


## GPS integrity diagnostics

GPS rejection now records one explicit category: `MOCK_LOCATION_DETECTED`,
`GPS_STALE` (older than 60 seconds), or `GPS_INVALID` (coordinates, accuracy,
or elapsed timestamp invalid). Mock takes priority when several checks fail.
These are diagnostic rows, never attendance events. Rejection returns before
arrival confirmation, presence journaling, or runtime attendance evaluation.

System Health includes GPS Integrity in its readiness summary. A mock warning
remains through stale/invalid readings until a fresh valid non-mock reading
arrives, which records `GPS_RECOVERED`. This is in-process sensor status;
after process restart the UI waits for another reading rather than claiming readiness.
Selecting a Developer Options mock app alone is not a positive detection.

Device verification: start fake GPS, wait for a verification burst, and confirm
`MOCK_LOCATION_DETECTED` plus a red GPS Integrity warning, with no new attendance
event. Stop fake GPS and wait for a fresh real fix; confirm `GPS_RECOVERED`.
Also test a poor initial fix followed by a mocked refinement fix: the mock must
reach rejection rather than silently retaining the earlier real reading.

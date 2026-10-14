# Live UI integration

Dashboard, attendance history, profile, requests/history/details, notifications and configuration now load CRM data. Offline Data, Sync Status and Event Log observe Room records filtered by logged-in user and this installation. Static labels and the permitted 30/45/60-minute choices remain constants; employee values do not.

## Backend installation

Use the accompanying CRM_Mobile_UI_API.zip on testing-branch. The private CRM repository was not readable by the connected account; these additive handlers were prepared against the supplied local CRM source. Preserve any newer changes in your current attendanceRoutes.js and add this mount before module.exports:

```js
router.use('/mobile', require('./mobileAttendanceRoutes'));
```

Apply Backend/src/database/migrations/015_attendance_requests.sql to your TEST Neon database, then deploy. No SQL in this package deletes attendance, users, devices or time anchors. Keep existing core attendance fixes. The read-model queries require the supplied attendance master fields (attendance_location_id, on_duty) and accepted attendance_events with effective_event_time.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /api/v1/auth/me | Existing CRM profile response: success + user |
| GET | /api/v1/attendance/mobile/attendance | Latest 90 recorded days + accepted child events + trusted event times |
| GET / POST | /api/v1/attendance/mobile/requests | Own requests / idempotent text submission |
| GET | /api/v1/attendance/mobile/requests/team | Same-company Admin/Manager CRM review list |
| PATCH | /api/v1/attendance/mobile/requests/:id/review | Same-company pending request review; body status APPROVED/REJECTED + remarks |
| GET | /api/v1/attendance/mobile/notifications | Own CRM request decisions |
| GET | /api/v1/attendance/mobile/on-duty | Explicit 501 until the real CRM assignment schema is supplied |
| GET | /api/v1/attendance/locations/active | Existing active office configuration |

Authorization is Bearer JWT. User/company identity comes from authentication, not the request body. Request approval does not silently rewrite attendance; regularization processing still needs the CRM workflow. Attachments are not uploaded. Shift hours/department and On Duty assignments are not available from the supplied APIs. UI displays that limitation. App lock and biometric UI are implemented using Android system authentication; see ANDROID_TEST_READINESS.md for device checks.

History shows totals from accepted event sequences, without tea/lunch classification. Incomplete historical sessions show unknown durations. Missing records do not imply absence. Monthly summaries cover the records returned, not a complete absence roster. CRM/API errors show unavailable/retry states, never fake zero counts or fake attendance. Screen reads poll every 30 seconds only while the authenticated UI is visible; this does not change sensor/engine cadence.

## Refresh displayed data on the physical phone

Install the updated debug APK, then Settings > Offline Data > Clear display cache and reload CRM. The authenticated graph also resets its display data on entry. Or use PowerShell:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s 7POVXSTOPFGEAMBE shell am start -n com.prodigit.bodhiattendance/.MainActivity --ez clear_ui_cache true
```

This debug intent reloads UI data; it retains the actual Room attendance queue, trusted-time anchors, installation ID and login. There is no destructive database-reset endpoint. No phone or Neon data has been cleared remotely during this change.

Current debug API URL remains http://127.0.0.1:5000/. For USB/local testing:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s 7POVXSTOPFGEAMBE reverse tcp:5000 tcp:5000
```

When an online testing-branch backend URL is available, pass -PbodhiApiBaseUrl=https://YOUR_TEST_BACKEND/ when building. Release builds require this explicit URL.

## Validation

Backend: node --test Backend/tests/mobileAttendance.test.js (10 passing mocked-handler tests), JavaScript syntax checks and Android git diff whitespace checks. No live PostgreSQL test or deployed API verification was possible. Android compile was attempted but Gradle 9.6.0 could not be downloaded in this environment. Run .\gradlew.bat :app:assembleDebug on the Windows checkout before installing.

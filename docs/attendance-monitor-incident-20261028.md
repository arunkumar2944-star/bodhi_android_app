# Attendance monitoring incident — 2026-10-28

## Status

**Open — root cause not yet confirmed.** This checkpoint records source-level findings and adds diagnostics to the incident branch. It does not establish that attendance monitoring or event recording is repaired. No attendance rows are created, edited, or backfilled by this change.

## Incident evidence supplied by the owner

The CRM Diagnostics page for employee ArunKumar Sakthivel (#7) showed repeated `LOCAL EVIDENCE AUDIT` observations on 28 October 2026 at approximately 2:34 PM, 2:59 PM, 3:29 PM and 3:34 PM IST (plus an observation from 27 October). The UI explanation said that a diagnostic observation had been received and did not represent a confirmed attendance change. The report stated that no expected attendance event or check-in/break start/end result appeared.

Historical device findings in the handover reported that `dumpsys activity services` showed no expected monitoring service, and that the installed build was versionName 1.0.2/versionCode 15/targetSdk 36. These are prior observations and must be repeated on the affected device before treating them as current facts.

## Source-level findings

1. `AttendanceDiagnostics.auditLocalPersistence()` inserts `LOCAL_EVIDENCE_AUDIT` records containing local Room break-start, break-end, unsettled-event, and SQLite candidate counts. Before this incident branch, it did not report foreground-monitor status or the most recent monitor-start result.
2. `AttendanceBackgroundSync.run()` can generate/upload the audit separately from the presence service. Therefore a successful diagnostic upload is not evidence that the foreground monitor or evaluator is running.
3. `AttendanceMonitorStartGate.tryStart()` is called only after startup readiness and Activity visibility. It checks legacy automation cleanup, pre-start synchronization, daily eligibility/pause state, session/device credentials, engine reconciliation, and runtime readiness before requesting the service. Several early-return paths previously had no explicit persistent reason.
4. `AttendancePresenceService.start()` returning true means Android accepted the foreground-service start request; it does not prove that `onStartCommand()` executed, that the worker remained alive, or that the evaluator produced an attendance event.
5. The Android app posts attendance events to `POST /api/v1/attendance/event`. The backend's monitor heartbeat endpoint, `POST /api/v1/attendance/monitor/heartbeat`, updates monitoring-session/liveness records, not attendance itself. Diagnostics are uploaded to `POST /api/v1/attendance/diagnostics`, a separate endpoint.
6. Git history gives a useful comparison window, but not a proven regression: commit `d9dda9a3` (15 October 2026) introduced the requirement for a visible app opening to start each workday; `c6e89286` (13 October 2026) gated foreground-service launch on validated startup and a resumed Activity; commit `57881ad9` (21 October 2026) connected the local Room/SQLite audit to the existing diagnostics upload. Compare these commits with the current source during root-cause review.
7. The actual failure stage remains unknown without a fresh device run, local Room evidence, backend response/log evidence and the matching server-side summary/API result.

## Changes on the incident branch

Branch: `incident/attendance-monitor-observability-20261028`

Base commit: `af127869032e24bbc6f465f553cb9be543255567` on `android-ui-development`.

- `AttendanceMonitorStartGate.kt`: persists the latest startup outcome, including waiting for startup/activity visibility; pre-start sync failure; daily policy decision; missing device/token; reconciliation/runtime readiness; and service-start request outcome.
- `AttendanceDiagnostics.kt`: includes `monitorRunning`, `verificationPending`, and `lastStartGateResult` in the existing local audit payload. It deliberately retains the existing `LOCAL_EVIDENCE_AUDIT` category so it remains visible through the current diagnostics UI/filter.
- `AttendancePresenceService.kt`: records explicit reasons for immediate service rejection (invalid action, missing fine-location permission, paused privacy policy, missing work date), foreground start exception class, and monitoring-worker exception class.

Initial service-instrumentation commit: `5723949e1f4e471174e357ccca0ac221cf3254dd`. Follow-up commits record the positive transition `MONITOR_RUNNING:workDate=YYYY-MM-DD` after the service worker passes daily eligibility: start gate `00f7df41af3aef10ebf96f071aebc24a43297123`, service `28ebf8654a36a6068f3459004bcba686277baa06`. The current PR head also includes this checkpoint document.

These are observability changes only. They do not bypass eligibility/reconciliation, infer attendance from diagnostics, or alter attendance event creation rules.

## Verification status and access limits

- Android source is accessible through GitHub. The original `android-ui-development` branch is unchanged by this incident work.
- Backend source is accessible as `arunkumar2944-star/CRM_TEST_Backend`, default branch `main`.
- The expected backend branch `Android_Attendance` was not present when inspected.
- The frontend repository reference `nunnarivuai/crm` returned HTTP 404 through the connected GitHub account, and no local frontend checkout was available in the execution workspace.
- No local Windows workspace, Android SDK/ADB executable, or connected physical device was accessible in this run. Consequently no Gradle compile, Android tests, backend test suite, current-device check, or end-to-end attendance run has been performed here.
- The incident is **not resolved** and a production deployment/APK publication has not been performed.

## Safe affected-device collection procedure

Run PowerShell from a Windows machine with Android Platform Tools installed. Do not clear app storage, uninstall the app, remove device registration, force-stop the app, or edit production attendance records during evidence collection.

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { throw "ADB not found at $adb; locate your actual Android SDK platform-tools folder." }

& $adb devices -l
& $adb shell getprop ro.build.version.release
& $adb shell getprop ro.build.version.sdk
& $adb shell dumpsys package com.prodigit.bodhiattendance | Select-String -Pattern 'versionName|versionCode|targetSdk|userId='
& $adb shell dumpsys activity services com.prodigit.bodhiattendance
& $adb shell dumpsys package com.prodigit.bodhiattendance | Select-String -Pattern 'ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|FOREGROUND_SERVICE|FOREGROUND_SERVICE_LOCATION'
& $adb shell appops get com.prodigit.bodhiattendance | Select-String -Pattern 'FINE_LOCATION|COARSE_LOCATION|START_FOREGROUND'
```

For a reproduction, first note the local time and keep the app visible until startup completes. Then capture logs without clearing the device log buffer:

```powershell
$log = & $adb logcat -d -v threadtime
$log | Select-String -Pattern 'ATTENDANCE_MONITOR|ATTENDANCE_ENGINE|ATTENDANCE_DIAGNOSTICS|STARTUP_DEBUG|AndroidRuntime|ForegroundServiceStartNotAllowedException|SecurityException' |
  Out-File -Encoding utf8 .\attendance-monitor-incident-logcat.txt
```

Attach the output plus the app's Event Log / diagnostics export and the latest `LOCAL_EVIDENCE_AUDIT` message. Redact access tokens, phone numbers, GPS coordinates and any unrelated personal data before sharing. Avoid repeated retries that could create real attendance events; test only under the established attendance policy.

## Expected interpretation of new fields

- `monitorRunning=false`: the current process's in-memory monitoring status is not running when the audit is assembled; it is not by itself a root cause.
- `verificationPending=true`: the monitor state reports a waiting/pending verification state.
- `lastStartGateResult=BLOCKED:...` / `DEFERRED:...`: identifies the latest known gate outcome. Check the detailed logs and current server response before choosing a code fix.
- `lastStartGateResult=START_REQUEST_ACCEPTED:...`: only a start request was accepted; confirm the service with `dumpsys activity services` and the `ATTENDANCE_MONITOR` logs.
- `lastStartGateResult=SERVICE_REJECTED:...`: the service reported an immediate rejection or worker exception. Correlate the time and reason with system logs.
- `roomUnsettled` above zero: local events are pending, retryable-failed, or syncing; correlate the event UUID and sync response rather than manufacturing another event.

## Gap register

| ID | Severity | Gap | Status | Evidence needed / next action |
|---|---|---|---|---|
| ATT-INC-001 | Critical | Monitoring/attendance failure root cause unconfirmed | Open | Install/test the instrumented build in a controlled eligible session; inspect gate reason and current service state |
| ATT-INC-002 | High | Physical-device service startup/rejection not verified | Blocked | Connected Android device and ADB output required |
| ATT-INC-003 | High | Local Room event and sync result not correlated to backend persistence | Open | Correlate local event UUID, HTTP response, server DB row and summary API |
| ATT-INC-004 | High | Android branch build/regression result unknown | Pending CI/device | Run Android unit tests and debug/release builds; complete physical-device acceptance |
| SEP-001 | High | Standalone backend branch not created | Open | Inspect all migrations/contracts and create `Android_Attendance` only after source review |
| SEP-002 | High | Attendance-only frontend extraction blocked by repository access | Blocked | Grant access to `nunnarivuai/crm` or provide a verified repository/ZIP |
| SEP-003 | Medium | Standalone DB/auth/runtime independence not implemented | Open | Design/version migrations and prove no runtime CRM dependency |

## Acceptance criteria before closure

Do not close ATT-INC-001 until evidence shows: eligible startup → service running → evaluator outcome → valid local event (when criteria are met) → backend accepted/idempotently saved the event → summary API returns it → web UI displays the confirmed attendance record. Also verify that diagnostic-only observations never create attendance events and that ineligible conditions remain blocked safely.

# Backend forensic audit — User 7, 31 October and 2 November 2026

Audit date: 2 November 2026. All human-readable event times are Asia/Kolkata (UTC+05:30). No source, tests, migrations, configuration, services or database records were changed. Only report/evidence documents were written. No attendance-producing API was called.

## Executive finding

**The confirmed Monday event-versus-summary timestamp discrepancy originates at the ingestion path's master-table timestamp binding, not at Wi-Fi arrival reconstruction, effective-event selection or midnight closure.** The source passes a JavaScript Date directly into an IST-convention `timestamp without time zone` field. SELECT-only reproduction under Node UTC gives exactly the stored wrong naive value. The same instant remains correct in the timestamptz event column. The precise deployed process and its historical write cannot be independently authenticated from this workspace, but the defect and matching failure mechanism are reproduced.

Saturday's checkout and break remain unresolved: expanded inspection found no Saturday-dated User 7 rows across 20 attendance relations. Missing environment/deployment provenance is the primary evidence blocker, not a demonstrated calculation error on Saturday's records.

Additional confirmed source defects/behavior divergences were identified: PDF conversion of unknown work duration to zero; different handling of open breaks in normal checkout versus mobile/correction projection; web summaries omitting policy-closure provenance. None is established as the cause of Saturday's reported 17:30 checkout.

## Repository and environment provenance

Backend located at `D:\Prodigit\CRM_TEST_Backend`; initial status clean. Local branch `main`, HEAD and cached `origin/main`: `4ad42afc60b201d9017cf66a5e5df4a38bd2608f`. Read-only `git ls-remote` and authenticated GitHub compare found current main **`5981a8c701710af8edac95dfcd6447e35b109f73`**, **65 commits ahead**, zero behind. No fetch, ref update, checkout or branch change performed.

The comparison is saved as `attendance-backend-audit-main-comparison.json` in the Android docs folder. Relevant differences include bound Android-session middleware/routes, diagnostics filtering, mobile holiday response, PDF/timezone formatting and server startup. The compare file list does **not** change `attendanceController.js`, `verifiedWifiArrival.js`, `attendanceMasterProjection.js`, `attendanceEventSource.js`, `attendanceTimeline.js`, `attendanceRegularizationService.js`, `attendanceDayClosureService.js` or `database/connection.js`. Thus the central audited timestamp/projection code is the same between these two compared commits; deployed revision remains unknown. Tests in this audit apply to the local checkout, not the uninstalled newer main.

Database: the existing local backend `.env` DATABASE_URL points to a **hosted Neon PostgreSQL** database named `neondb`, schema `public`; it is not an embedded local database. Target host is recorded without credentials in `attendance-backend-audit-db-evidence.json`. Non-secret target fingerprint (SHA-256 of hostname + database path):

`9dd1a59da011b996fc1797c97d5f9bb06a2d52fbc2261634a11eaaeab17278fe`

This is the same configured source used in the previous investigation. Whether it is test, staging or production, and whether Vercel/installed Android use it, remain **unverified**. No production access claim is made. Connection was forced read-only; server reported `transaction_read_only=on`, session timezone GMT, search path `"$user", public`. Snapshot clock: `2026-11-02T12:33:09.469Z` = **18:03:09.469 IST**. User 7 exists, company 1, active, role Manager. This verifies identity only in this environment.

Only public-schema attendance relations were discovered in the accessible schema inventory. No accessible attendance archive or migration-ledger relation was returned by the targeted naming scan. This cannot exclude hidden schemas, external archives, backups or another database. DB objects establish migration capabilities, not an authenticated migration execution history.

## Event-versus-summary data flow

```text
Android UUID + anchor + monotonic time + evidence
  -> attendanceController.recordAttendanceEvent
  -> validate identity/device/anchor/boot/time/order/transition/location
  -> verifiedWifiArrival.verifyAndStore (CHECK_IN proof, if supplied)
  -> attendance_events (raw accepted device audit)
  -> attendance_effective_events (device + correction + policy-closure projection)

Within ingestion transaction:
  CHECK_IN -> attendance.login_time (direct binding)
  each new event -> attendanceMasterProjection.updateMasterFromEvent
                  (state/company/office/first and last positive presence)
  CHECK_OUT -> completed-break SQL + gross/net calculation
            -> master login/logout and durations (direct binding)

Manager correction transaction -> correction row + rebuilt master + review audit
Midnight closure transaction -> separate closure row + master logout; durations NULL

Web employee/team/Manager + PDF export -> master fields, not timeline reconstruction
Mobile history -> effective events -> summarize -> policy-review labels
Bootstrap/day eligibility -> accepted event state, plus some master summary fields
```

Raw device evidence authority is `attendance_events`. Accepted operational timeline authority is `attendance_effective_events` when available, filtering `is_active=TRUE` at consumers. `attendanceEventSource.js:2–5` switches by `to_regclass`, not by a migration version number. Migration 025 adds corrections; migration 027 adds `SYSTEM_DAY_END` and supersession rules. The DB view definition was retrieved and contains the closure union, confirming 025/027 functionality exists in this database. If the view is missing, the selector falls back to raw events; if it exists but is an obsolete definition, the selector cannot detect that.

Displayed web/PDF summary authority is the persisted `attendance` master, which may differ from the accepted timeline. Mobile history independently calculates from the timeline. There is no universal automatic “read repairs master” path.

## Summary writers and readers

All application-source matches for the requested fields were searched under `src`. SQL migrations were inspected as definitions only, never executed.

| Path / exact function | Fields and behavior | References (backend-relative) |
|---|---|---|
| `recordAttendanceEvent` | Blank legacy-row reuse and new CHECK_IN write naive login; each event updates presence/state; CHECK_OUT recomputes completed breaks, gross and net, then writes naive login/logout | `controllers/attendanceController.js:1655`, `1680–1701`, `1970–2199` |
| `updateMasterFromEvent` | Sets state/company/office for all transitions; first/last presence only on CHECK_IN or BREAK_END; does not recalculate durations/login/logout | `utils/attendanceMasterProjection.js:2–15` |
| `reviewRequest` | ADD/REPLACE/VOID correction overlays, project full active timeline, explicit IST login/logout casts, all requested state/duration/presence fields; audit and master commit together | `services/attendanceRegularizationService.js:24–76`, especially `68` |
| `settleEndedDays` | Separate policy checkout at next midnight; master state CHECKED_OUT, logout explicitly IST, all three durations NULL; preserves raw events | `services/attendanceDayClosureService.js:21–55` |
| Legacy `recordLogin` / `recordLogout` | DB CURRENT_DATE/CURRENT_TIMESTAMP summary writes; bypassed for active onboarded automatic users or today's active raw event chain | `utils/attendanceRecorder.js:28–102`; auth calls at `controllers/authController.js:219,1250` |
| `myAttendance` | Master login/logout/total minutes/status plus separate software-session totals; no effective-event or closure reason fields | `controllers/attendanceController.js:95–202` |
| `respondTeamAttendance`, Team Lead / Manager callers | Same master columns for selected company users and separate session totals | `controllers/attendanceController.js:285–410,412,444` |
| `teamLeadMonthlyReportExport` | Master login/logout/total minutes, IST clock formatter; `Number(null)` becomes 0 | `controllers/taskReportController.js:1748–1755,1795–1805,1842–1844` |
| Mobile `attendance` / `summarize` | Active effective events, epoch milliseconds, independent accumulating work/break totals; policy closure gets requiresReview and null durations | `controllers/mobileAttendanceController.js:22–71` |
| `bootstrapState` | Calls closure service (therefore not a read-only endpoint); returns master times and null totals via `?? 0` in response; state from latest effective event | `controllers/attendanceController.js:2533,2570–2749,2780–2913` |
| Day/monitoring reads | Eligibility derives state from accepted events, not login_time; monitoring uses that result | `services/attendanceDayService.js:32–55`; `controllers/attendanceMonitorController.js:127` |

No source path was found that arbitrarily deactivates/deletes attendance events and then rebuilds master. Supported corrections use overlays and do rebuild it atomically. Direct operator edits or inactive-flag changes outside that service can leave master stale; no attendance-table trigger existed in the prior schema inspection. This is a risk, not evidence of an actual manual alteration here. First/last presence represent accepted positive transitions, not continuous sensor heartbeats, by implementation.

## Confirmed Monday record and timestamp root cause

| Field | Retrieved value |
|---|---|
| Event / master | ID 96, UUID `580ec82c-9810-4855-8eb6-c2f5018dbb91`, attendance 52 |
| Arrival candidate | `0a05b8c1-c8e0-4bc6-ada1-60e9ab729dd1`, VERIFIED, CONTINUOUS_APPROVED_WIFI_THEN_GPS |
| Boot | `1dd5a0d9-9946-4759-987b-40e2a7828a0b` |
| Anchor | ID 161, UUID `4fd5a8bc-dee2-4cb4-a6fd-3ed006a40bc7`; server `2026-11-02T08:56:26.969Z`, elapsed 384762675 ms |
| Arrival elapsed | 387137026 ms |
| Arrival / effective_check_in_at / effective_event_time | `2026-11-02T09:36:01.320Z` = **15:06:01.320 IST** |
| GPS confirmation elapsed | 387164748 ms |
| presence_verified_at | `2026-11-02T09:36:29.042Z` = **15:06:29.042 IST** |
| Event receipt | `2026-11-02T09:36:30.910Z` = **15:06:30.910 IST** |
| Master login_time (raw naive text) | `2026-11-02 09:36:01.32` |
| Master first/last presence | `2026-11-02T09:36:01.320Z` |
| Master state/durations | WORKING, logout NULL, work NULL, gross 0, break 0 |

Arrival proof preserved **27.722 seconds**. Event receipt followed effective arrival by **29.590 seconds**. Raw/effective-view records agree and origin is DEVICE. No correction, closure or request row for either requested workday was retrieved.

Earlier diagnostics from the previous saved snapshot report GPS inside office with BSSID `0e:36:23:2a:42:39`, `wifiMatched=false`. Approved office 1 BSSID is `0e:36:23:2a:42:3a`. Wi-Fi matching uses normalized BSSID, not SSID (`utils/attendanceLocation.js`, `verifyEventEvidence`, Wi-Fi section). Android blocked creation as uncertain; these are **not demonstrated backend-rejected attendance API requests**. The backend would also reject an entry proof that fails office Wi-Fi. No approved 14:15 candidate or backend rejection for such a candidate was retrieved. The unmatched readings correctly remain ineligible for automatic backdating; verify router ownership/configuration history without bypassing approval.

`verifiedWifiArrival.js:2–42` validates same device/boot/anchor, matching router, monotonic order, same IST date, anchor and device active/onboarded, replay conflicts. `attendanceController.js:1305–1321` separates GPS verification from approved arrival effective time. The linked candidate/event/anchor agree: no evidence of error in this reconstruction.

### Reproduced timestamp defect

`database/connection.js:31–54` states naive timestamp columns hold IST wall time and returns them as raw strings (OID 1114). `recordAttendanceEvent` obtains effective time from a timestamptz query as a JS Date (`attendanceController.js:1220–1232`) and directly binds it to master naive columns at `1694`, `2167–2169`. A naive timestamp ignores the timezone offset in the serialized input; node-postgres Date serialization uses the process timezone by default. This makes summary storage process-zone dependent.

SELECT-only check used the existing installed pg dependency, two **separately typed parameters**, and the actual event instant:

| Node TZ | Direct Date -> timestamp | ISO instant -> timestamptz -> IST timestamp |
|---|---|---|
| UTC | `2026-11-02 09:36:01.32` (matches stored master) | `2026-11-02 15:06:01.32` |
| Asia/Kolkata | `2026-11-02 15:06:01.32` | `2026-11-02 15:06:01.32` |

The correct instant is never shifted in the event table; **the summary write loses the intended wall-clock convention**. Web readers pass the resulting wrong string through; a format change cannot reliably recover its intended zone. Correction and policy-closure writers already use explicit IST conversion. Merely assuming the database session is IST does not fix a Date serialized with UTC and parsed as naive timestamp.

Evidence precedence: use `attendance-backend-audit-final-evidence.json` for this reproduction. The earlier `attendance-backend-audit-db-evidence.json.castReproduction` reused one parameter in two casts; PostgreSQL inferred the parameter as naive timestamp before its later timestamptz cast, producing a misleading double shift in the IST run. That intermediate result is retained as audit history but is **superseded**, not a second finding. The previous Android audit's local-time-only check did not expose this UTC-dependent bug.

This is a reproduced source defect and a strong matching explanation for master 52. A deployed trace/commit/process TZ is still needed to conclusively attribute the historical write to a specific runtime. It does not explain the reported 14:15-to-15:10 difference: 5h30m is distinct from that delay.

## Missing Saturday records

Expanded SELECT-only inventory checked every accessible attendance relation with user_id and a date/timestamp column. It counted User 7 rows where **any date column** matched 31 October (timestamptz converted to IST; naive timestamps compared as stored). All **20 relations** returned zero, including:

- `attendance`, `attendance_events` with no active-only filter, `attendance_effective_events`;
- arrivals, presence evidence, diagnostics, monitor sessions/gaps/reviews;
- regularization events **and** older `attendance_regularization_requests`, current requests/review audit;
- day closures, overrides/day audit, duty assignment/notification tables.

The raw/effective queries additionally checked event effective time, receipt time and relevant master linkage. Therefore this absence is not explained by the normal API's active filter, correction overlay or missing migration 025. No Saturday correction/closure was found that could conceal a raw event. No accessible alternative-schema attendance table was discovered. Inventory and counts are in `attendance-backend-audit-final-evidence.json`.

**What remains unknown:** different database/Neon branch or deployment; historical deletion/restore; employee identity differing across environments; phone-only/never-uploaded events; external archive; screenshot of a different day or source. None can be selected as the actual explanation without deployment settings, phone export, source UUID or database audit/backup history. Do not query other employee identities speculatively. Confirm this User 7/Manager/company 1 is the intended person through authorized admin evidence.

## Checkout, breaks, corrections and closure

### Backend acceptance

`recordAttendanceEvent` validates event UUID/idempotency (`~1025–1064`), anchor/boot/elapsed ordering (`~1120–1240`), chronological latest effective event (`1469–1474`) and allowed transitions (`1488–1494`). BREAK_START accepts after CHECK_IN/BREAK_END; BREAK_END only after BREAK_START; CHECK_OUT after CHECK_IN/BREAK_END/BREAK_START. CHECK_OUT is terminal. Retries return existing accepted event rather than add another UUID occurrence.

Departure validation binds to the check-in office and requires accuracy within CRM limit plus GPS distance outside radius (`1725–1779`). **The backend checks one supplied fix and `distance > radius`; Android's stronger two-fix, 15–90-second confirmation and `distance - accuracy > radius` guard is not independently revalidated by this backend path.** The API payload does not supply the pair. This is a confirmed defense-layer gap/policy question, not proof of a bad Saturday fix. Entry/return validation enforces configured GPS and required Wi-Fi; no recommendation to weaken those checks.

### Duration semantics and actual divergence

Normal CHECK_OUT SQL (`attendanceController.js:1998–2188`) uses accepted effective events, sums **completed BREAK_START -> BREAK_END pairs**, floors total break seconds to minutes; gross is floored effective checkout minus earliest check-in; net is max(0, gross - break). Open terminal BREAK_START is not deducted.

Mobile `summarize` (`mobileAttendanceController.js:22–43`) and correction `projectAttendance` (`attendanceTimeline.js:3–30`) close an open break at checkout, or accrue it to now for a current open session. They floor accumulated work and break independently. Thus they can disagree with normal persisted totals:

- Example (synthetic, not Saturday evidence): CHECK_IN 09:00, BREAK_START 12:00, CHECK_OUT 13:00 -> normal ingestion gross/net **240/240 min**, completed breaks **0**; mobile/correction work **180**, breaks **60**.
- For automatic classification where BREAK_START and CHECK_OUT share the same original departure time, the terminal interval is zero and this specific difference disappears.
- Independent flooring of work/break versus gross-minus-floored-break can also differ by one minute with sub-minute timestamps.

These are confirmed source-level calculation divergences. Whether a nonzero open break should count as break, work or require review is a business decision; do not rewrite existing attendance to force agreement.

Corrections (`attendanceRegularizationService.reviewRequest`) rebuild full effective timeline and master atomically with audit; replay does not double-apply, and invalid chronology rolls back. A correction can intentionally change totals relative to the old ingestion formula. No correction evidence exists for these incident days. Direct inactive flag changes lack an application repair path; stale master is possible if edits bypass the supported correction service.

### Midnight policy checkout

`closureProjection`/`settleEndedDays` (`attendanceDayClosureService.js:10–55`) close only ended days with valid accepted check-in and no real checkout; cutoff is **next 00:00 IST**, not a physical departure claim. Migration 027 keeps closure separately with origin SYSTEM_DAY_END, review confidence and physicalDepartureConfirmed=false; real/approved checkout supersedes it without deleting audit. Master logout gets midnight and all final durations become NULL intentionally.

Mobile history labels this origin and makes work/break null (`mobileAttendanceController.js:65–69`), as required. **Web my/team/Manager summaries and PDF omit closure provenance**, so their raw logout can be rendered as ordinary checkout unless another consumer joins metadata. This is a confirmed API omission; the exact frontend's rendering was not inspected.

PDF `formatMinutes` calls `Number(row.total_work_minutes)` when logout exists; **Number(null)=0**, so unknown policy-closure duration formats as `0h 00m`. Bootstrap also normalizes null work/break totals to zero (`attendanceController.js:2729–2732,2893–2896`), losing unknown-versus-zero information. The policy closure itself is not defective for keeping duration unknown. No closure row for Saturday was found, and midnight does not equal 17:30; closure is not an established incident explanation.

**Saturday conclusion:** no exact BREAK_START/BREAK_END, duration or checkout is retrievable. The reported 20–30-minute break cannot be used as its recorded duration. A missed return plus original-absence classification remains possible based on Android source, but no Saturday evidence proves it.

## Confirmed findings versus hypotheses

| Finding | Status | Incident attribution |
|---|---|---|
| UTC Date binding into master naive timestamps stores wrong IST clock | Confirmed, SELECT-only reproduced | Matches Monday master 52; precise historical runtime unverified |
| Wi-Fi proof preserves original approved arrival / separates GPS and receipt | Confirmed DB + source | Monday verified; earlier unmatched Wi-Fi cannot be backdated |
| Current local main lags GitHub main 65 commits | Confirmed remote comparison | Deployed revision unknown; core event/projection code unchanged across comparison |
| Effective view includes corrections and policy closure | Confirmed DB view definition | Migration-025 absence ruled out for accessed DB |
| Saturday records absent across discovered tables, inactive rows included | Confirmed scoped query result | Cause of absent history unresolved |
| Open-break and rounding semantics differ across ingestion/mobile/corrections | Confirmed source divergence | No Saturday attribution |
| Export turns unknown duration into zero; web lacks policy closure provenance | Confirmed source behavior | No matching closure in incident evidence |
| Backend departure validation weaker than Android guard | Confirmed code gap | No Saturday fix evidence |
| DB/deployment mismatch, deletion/restore, phone-only Saturday history | Hypotheses | Need deployment and phone/DB audit evidence |

## Tests and missing coverage

The previous **14 passing tests** cover `verifiedWifiArrival.test.js` (proof/time separation/conflicts), `attendanceDiagnosticController.test.js` (identity/tenant/role/uploads) and `diagnosticValidation.test.js` (batch/input validation). They do not establish full ingestion/master consistency or production deployment correctness. Their contents were reviewed; no unnecessary repeat run.

This audit ran **26 additional safe mock/pure tests**, all passed:

- `attendanceRegularization.test.js`: effective-source switch before/after migration, timeline/work/break projection, correction/master/audit atomicity, rollback/replay and corrected mobile totals.
- `attendanceMaintenance.test.js`: mocked maintenance authorization/migration guard, mobile policy-review/null duration labeling.
- `attendancePresentation.test.js`: adjacent same-time provisional break/checkout suppression and genuine break preservation.
- `androidLoginAttendance.test.js`: Android login does not record attendance; legacy recorder automatic-user guards.

`attendanceDayClosure.test.js` covers cutoff, actual event-validator scenarios, late uploads, closure supersession and correction migration using isolated PGlite. It was **reviewed but not run**, because it executes SQL migrations in its in-memory fixture and this task explicitly prohibits running migrations. No tests were added/edited; no live handlers or closures invoked. There is no dedicated test found for every normal ingestion master field matching web/mobile/export output. Existing mocks do not catch native pg naive-Date binding.

Required regression coverage before fixes:

1. Real pg binding under both UTC/IST process zones and UTC/IST DB sessions; assert identical effective instant and intended master IST values for CHECK_IN/CHECK_OUT, including verified arrival.
2. Web own/team/Manager/PDF/mobile parity for same day; explicit unknown-vs-zero durations and closure provenance across all endpoints.
3. Completed, open and zero-length terminal breaks; sub-minute rounding; correction and normal ingestion use agreed formulas.
4. Migration 025 absent, present, obsolete view, and 027 applied; raw/inactive/correction/synthetic closure acceptance and latest-state ordering.
5. ADD/REPLACE/VOID and correction-of-correction rebuild all master fields, retain audit, rollback on failure, replay once; operator reconciliation detects master drift without silent repair.
6. Accurate outside pair evidence, boundary accuracy envelope, old/mocked/out-of-order observations and approved-Wi-Fi conflict; specify which evidence is enforced server-side.
7. Policy closure, subsequent late original-day events and real/corrected checkout supersession; no physical departure assertion from midnight cutoff.
8. Verified earlier arrival + delayed GPS + stable proof/UUID, router mismatch, boot change/expired anchor and retry rollback.

## Recommended minimal fixes — ranked, not implemented

| Priority | Minimal proposal | Reason / expected behavior | Risk |
|---|---|---|---|
| P1 | Explicit instant -> IST conversion in normal master login/logout writes (`attendanceController.js:1657,1694,2167–2169`); bind an unambiguous instant, not process-local naive text | Eliminate reproduced process-zone dependency; retain event instant as authority | Legacy consumers/double conversion; test before historical audited repair |
| P1 | Preserve NULL duration and expose closure origin/requiresReview in web/team/PDF/bootstrap summary contracts | Unknown physical departure/final hours must remain unknown; stop PDF displaying 0h | Consumer contract changes; distinguish true zero from missing data |
| P1 | Agree one duration/rounding policy, then share calculation between normal checkout and correction/mobile projection | Eliminate demonstrated open-break/rounding divergence | Payroll/history totals may change; no silent bulk rewrite |
| P2 | Define and enforce server departure evidence contract if two-fix/accuracy-envelope guarantees are required end to end | Current single-fix backend does not independently enforce Android guards | Payload compatibility, delayed offline acceptance; staged rollout |
| P2 | Add read-only master-vs-effective reconciliation diagnostics and view-capability checks | Detect unsupported edits/stale master/obsolete migration definition | Avoid automatic record rewriting or heavy scans |
| P3 | Record non-secret deployment revision and environment/database fingerprint in operator diagnostics | Resolve missing Saturday provenance efficiently | Scope access; never expose credentials |

No attendance correction is recommended solely from this audit. For Monday, correct summary storage only through an approved, audited process after validating deployment/consumers; do not replace the valid 15:06 event with reported 14:15. Saturday needs original records and corroboration first.

## Remaining read-only evidence and exact next steps

1. Operator verifies Vercel deployment SHA, database fingerprint/Neon branch and installed APK base URL/build. Local HEAD is demonstrably stale; do not switch/deploy as part of this investigation.
2. Obtain original 15:10/17:30 screen export with date and UUID, and read-only phone Room event/anchor/state/diagnostic export. Confirm User 7/company 1 is the same person/environment.
3. Execute the attached SELECT-only SQL on the confirmed authoritative target, including raw inactive rows and correction/closure/request history. Preserve original offset/types, then calculate Saturday break seconds from actual accepted pair.
4. If still absent, authorized DBA checks historical backups/restore/deletion audits and alternate Neon branches. No evidence here proves deletion or another target exists.
5. Validate :39 router ownership and CRM/cached-config history. Preserve approved-Wi-Fi policy.
6. Approve a narrow fix proposal with the regression matrix above; only then implement. Existing accepted events and original audits must be preserved.

Evidence artifacts reside in `D:\Prodigit\android\CRMAttendance\docs`: prior investigation snapshots, new `attendance-backend-audit-main-comparison.json`, `attendance-backend-audit-db-evidence.json`, `attendance-backend-audit-final-evidence.json`, and `attendance-backend-audit-readonly.sql`. The SQL contains SELECT statements only; the runner must enforce read-only connection options. The investigation checkpoint was updated with these findings.

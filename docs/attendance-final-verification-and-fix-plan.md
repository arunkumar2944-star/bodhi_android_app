# Attendance final verification and fix plan

## Approved P1 implementation completed — 2 November 2026

This status supersedes the read-only implementation gate in the original plan below. P1 was explicitly approved; P2, production deployment and historical data correction were not.

- Base checkout: `main`, `4ad42afc60b201d9017cf66a5e5df4a38bd2608f`. Existing untracked investigation documents preserved. No pull/merge/rebase/reset, ref change, commit or push.
- Production source change: `src/controllers/attendanceController.js`, function `recordAttendanceEvent`, exactly three write boundaries: placeholder CHECK_IN login at line 1657, new CHECK_IN login at 1694, and CHECK_OUT summary login/logout at 2167–2169. Four assignments now explicitly use `($n::timestamptz AT TIME ZONE 'Asia/Kolkata')`. Event instants, candidate/anchor handling, UUIDs, duration/state calculations and API formats are unchanged.
- Verified actual destination semantics from the saved database schema: login/logout are naive IST timestamps; event effective/confirmation/receipt fields are timestamptz. SQL conversion at the boundary preserves PostgreSQL precision, unlike using a millisecond-formatting JavaScript helper. No new production helper or dependency is needed. NULL remains NULL; invalid input raises existing PostgreSQL invalid-datetime error rather than a fallback time; endpoint validation is unchanged.
- Added `tests/attendanceSummaryTimezone.test.js` (7 tests). It extracts the actual three production query strings and executes them in isolated in-memory PGlite PostgreSQL, using native pg `prepareValue` for input serialization. Four combinations of Node UTC/IST and DB UTC/IST, six instant/offset/midnight/precision inputs and Date/string forms exercise 48 fixture paths through all three write sites. Tests verify persisted values, repeated updates, existing minute totals, NULL/invalid handling and raw-event preservation.
- Test clone before/after: captured User 7 effective time `2026-11-02T09:36:01.320Z` previously persisted naive `09:36:01.32`; fixed code persists `15:06:01.32`. Event effective, GPS confirmation and server receipt values remain unchanged. The deliberately legacy fixture row stays wrong, demonstrating no automatic historical repair. Completed-break checkout fixture retains gross 540, break 30 and net 510 minutes for 09:00–18:00 IST.
- **95 selected tests passed, 0 failed, 0 skipped** across 12 files: `attendanceSummaryTimezone`, `attendanceDayClosure`, `attendanceDayEligibility`, `attendanceRegularization`, `attendanceMaintenance`, `attendancePresentation`, `androidLoginAttendance`, `verifiedWifiArrival`, `attendanceDiagnosticController`, `diagnosticValidation`, `bootstrapOfficeContext`, `returnAndNotifications` (all `.test.js`). Syntax checks passed for modified controller and new test. Production diff review and whitespace check passed.
- Test database verification is **verified in isolated PGlite PostgreSQL**. Existing closure tests execute migrations only in their in-memory fixture; no deployed/hosted DB migration or data access occurred. Initial run could not load missing PGlite; exact lockfile-declared 0.5.10 was installed into a separate test-tools folder and all final tests passed. Backend package manifest, lockfile, dependencies and environment configuration remain unchanged.
- Reproduce locally with the declared dev dependencies installed: `node --test tests/attendanceSummaryTimezone.test.js` plus the 11 existing regression files listed above. In this session NODE_PATH points to `C:/Users/Priya/.codex/visualizations/2026/11/02/01a19c03-e904-7603-8e6d-3674dbffb027/p1-test-deps/node_modules`. Test run log: `D:/Prodigit/android/CRMAttendance/docs/attendance-p1-regression-results.txt`.
- Limits: PostgreSQL persistence/pg serializer were verified, not a remote wire-protocol deployment or deployed endpoint smoke test. One captured incident was cloned; **20 historical mismatch days across five users are not repaired and are not claimed resolved**. No hosted DB was used or connectivity modified in this implementation phase. Production SHA/linkage and P2 evidence remain separate.
- No Android logic, business/shift/break/checkout policy, CRM configuration, production deployment, historical data or P2 behavior changed. Only the controller, new test and requested documentation changed. Ready for review; release/rollback procedure below remains a future separately approved action.


User 7 / company 1; incident dates 31 October and 2 November 2026. Verified 2 November 2026, approximately 19:24–19:26 IST. Read-only investigation: no production code, tests, migrations, database records, deployments, branches or Android logic changed. Existing local documents preserved. **Implementation requires explicit approval.**

## Decision

Prepare **P1: summary timezone correction** as a narrow backend patch to three SQL write sites in `recordAttendanceEvent`. Preserve accepted event effective timestamps and existing break formulas. Prepare **P2: Saturday evidence recovery** as a separate investigation. Saturday's actual checkout and break duration remain unknown.

New evidence refines the earlier backend report: the new CHECK_IN INSERT uses a parameter already typed as timestamptz for the work-date expression, so its assignment to naive login_time depends on the **database session timezone**. The placeholder CHECK_IN UPDATE and checkout UPDATE have naive target parameters and additionally depend on **Node process timezone / Date serialization**. It is inaccurate to describe all three sites as solely Node-UTC dependent. Explicit `timestamptz AT TIME ZONE 'Asia/Kolkata'` removes both ambiguities.

## Phase A — repository and deployment verification

| Repository | Local state | Current GitHub refs | Conclusion |
|---|---|---|---|
| Android `D:\Prodigit\android\CRMAttendance` | branch `android-ui-development`, HEAD `af127869032e24bbc6f465f553cb9be543255567`; untracked investigation documents only | Remote HEAD and `android-ui-development` both `af127869032e24bbc6f465f553cb9be543255567`; no `refs/heads/main` advertised | Local HEAD matches current default branch: no difference at the tip. This repository's advertised default branch is android-ui-development, not main |
| Backend `D:\Prodigit\CRM_TEST_Backend` | branch main, HEAD `4ad42afc60b201d9017cf66a5e5df4a38bd2608f`; untracked previous report and SELECT-only SQL, no tracked modifications | main/remote HEAD `5981a8c701710af8edac95dfcd6447e35b109f73` | Local is 65 commits behind, zero ahead per saved authenticated compare; no merge/pull/fetch/reset/rebase performed |

Prior `attendance-backend-audit-main-comparison.json` remains applicable: both endpoint SHAs are unchanged. Core `attendanceController`, verified arrival, master/effective/timeline projection, closure service and connection module are unchanged between those commits. Diagnostics/session security/routes and export/timezone display helpers do differ. Tests previously run apply to local code. Neither local HEAD nor remote main is automatically the incident deployment.

### Deployment evidence actually available

- Local backend `vercel.json`: `@vercel/node` builds `server.js`, routes all requests there; daily cron `30 18 * * *` calls close-workdays (18:30 UTC = next midnight IST). File defines deployment behavior, not deployed revision or database identity. No local `.vercel` metadata found; VERCEL_TOKEN absent from the command environment.
- Authorized GitHub statuses show successful Vercel status for main SHA `5981a8c...`, updated **22 October 2026 13:13:52 UTC**. Its deployment record ID **7253183669**, environment label **Production**, successful status at 13:13:53 UTC, URL `https://crm-test-backend-3f8g63hpv-sarunkumar2944-5912s-projects.vercel.app`.
- The same record has `production_environment=false` despite environment label Production. Two newer Preview records exist on 28 October at other SHAs. This conflicting flag and alias uncertainty prevent treating the label as proof of current production routing.
- Main's Vercel status links deployment ID `FP9K8E7J4Qv284zUK55uK9gMGEZB` in project `crm-test-backend`. Local SHA also has an older successful Vercel status. A successful build/deployment is not proof the Android-configured alias points to it now or did so on either incident date.
- GET `https://crm-test-backend-phi.vercel.app/api/v1/health/` returned HTTP 200, Vercel server, database Connected and server timestamp. It exposes **no SHA, immutable deployment URL or DB fingerprint**. `x-vercel-id` is a request-routing identifier, not a commit. The GitHub deployment URL did not produce parseable health JSON in the initial probe; its raw response must not be treated as authenticated app/version evidence.
- Android `app/build.gradle.kts:14–23,65–75` allows `bodhiApiBaseUrl`; release fallback is the phi Vercel alias, debug default localhost. `core/network/RetrofitClient.kt:69–76` uses BuildConfig.BASE_URL. Installed APK target/build was not inspected.

**Verified:** GitHub records deployment of a specific SHA. **Unverified:** current alias-to-deployment mapping, incident-time mapping, actual serving commit, production database/Neon branch, installed APK target. Required operator evidence is listed under P2. Saved GitHub and live metadata: `attendance-final-deployment-evidence.json`, `attendance-final-live-metadata.json` in Android docs.

## Phase B — exact timezone defect

### Types, parameters and query sites

Inspected database schema: `attendance.login_time/logout_time` are **timestamp without time zone**; convention is IST wall clock. `attendance_events.effective_event_time/presence_verified_at/server_received_at` and master first/last presence are **timestamptz**. Duration fields are integer minutes. DATE attendance_date is an IST business date.

`recordAttendanceEvent`, `src/controllers/attendanceController.js:1220–1232`, reconstructs effective instant from anchor server_time + elapsed milliseconds. Standard pg timestamptz parsing returns JS Date. Verified arrival can substitute the original trusted arrival instant (`1305–1321`) without changing GPS confirmation time. Example actual values:

- User 7, event UUID `580ec82c-9810-4855-8eb6-c2f5018dbb91`, attendance ID 52.
- Effective check-in `2026-11-02T09:36:01.320Z` = **15:06:01.320 IST**.
- GPS confirmation `2026-11-02T09:36:29.042Z`; receipt `2026-11-02T09:36:30.910Z`.
- Stored master login naive `2026-11-02 09:36:01.32`; expected IST naive `2026-11-02 15:06:01.32`.

The three ambiguous writes are:

1. **Placeholder reuse (`1655–1660`):** `UPDATE attendance SET login_time = $2 ... WHERE id = $1`, parameters `[attendanceId, effectiveEventTime]`. For the actual event shape: `[52, Date('2026-11-02T09:36:01.320Z')]`. This is the parameter shape, not proof that placeholder reuse occurred for row 52. `$2` is inferred as naive timestamp, losing offset.
2. **New master (`1680–1701`):** INSERT attendance_date = `($2::timestamptz AT TIME ZONE 'Asia/Kolkata')::date`, login_time = `$2`; parameters `[authUser.id, effectiveEventTime]`, actual value shape `[7, Date('2026-11-02T09:36:01.320Z')]`. Because `$2` is typed timestamptz, assignment to login_time uses the session-zone timestamptz-to-naive conversion. Under GMT it stores 09:36 despite correct attendance_date.
3. **CHECK_OUT summary (`2163–2189`):** `login_time=$2, logout_time=$3, gross_work_minutes=$4,total_break_minutes=$5,total_work_minutes=$6 WHERE id=$1`; parameters `[attendanceId, checkInTime, checkOutTime, grossWorkMinutes, totalBreakMinutes,totalWorkMinutes]`. Both time parameters originate in effective-event timestamptz fields and are inferred naive here. Saturday checkout parameters are **unavailable**, not invented.

Installed pg code `node_modules/pg/lib/utils.js:55–61,85–117` serializes Date through local date fields/offset when `defaults.parseInputDatesAsUTC=false` (`defaults.js:61`). UTC process serializes the actual Date as `2026-11-02T09:36:01.320+00:00`; Asia/Kolkata serializes 15:06 with +05:30. Parsing either as naive ignores offset. Parsing as timestamptz preserves instant, but an implicit later naive cast uses session zone. Changing only Node TZ or only database TZ cannot fix every site reliably.

### SELECT-only verification performed

Twelve scenarios: Node UTC/Asia-Kolkata × DB GMT/Asia-Kolkata × actual event / just before IST midnight / exact IST midnight. DB timezone was changed only transaction-locally via SELECT set_config and rolled back; no persistent settings or records changed.

| Node TZ | DB TZ | New INSERT-equivalent naive value for actual event | Direct UPDATE-equivalent naive value | Explicit IST result |
|---|---|---|---|---|
| UTC | GMT | 09:36:01.32 | 09:36:01.32 | 15:06:01.32 |
| UTC | Asia/Kolkata | 15:06:01.32 | 09:36:01.32 | 15:06:01.32 |
| Asia/Kolkata | GMT | 09:36:01.32 | 15:06:01.32 | 15:06:01.32 |
| Asia/Kolkata | Asia/Kolkata | 15:06:01.32 | 15:06:01.32 | 15:06:01.32 |

Boundary: `2026-11-01T18:29:59.999Z` is 1 November 23:59:59.999 IST; `18:30:00Z` is 2 November 00:00. With GMT implicit casts the latter can have attendance_date **2 November** but naive login date **1 November**. Explicit IST conversion matches the work date in every checked combination. Evidence: `attendance-final-timezone-evidence.json`. These are SELECT equivalents of binding/cast expressions, not test INSERTs.

### API serialization and Android display

`src/database/connection.js:31–54` returns OID 1114 naive timestamps as raw strings, DATE as date strings. Consequently JSON serialization does not automatically repair the wrong master clock. `utils/timezone.js:~29–54` treats a naive string as wall clock; newer main's formatters also cannot infer that some historical rows contain UTC clocks.

Mobile `controllers/mobileAttendanceController.js:22–71` reads active effective events, exports epoch `timeMs`, `checkInTimeMs/checkOutTimeMs` and independently computes durations. Android `data/remote/MobileUiApi.kt:23` stores epoch times; `ui/live/LiveUiViewModel.kt:169` formats Instant with Asia/Kolkata; `ui/attendance/view/ViewScreen.kt:150–151` uses that helper and labels policy closure. Local `ui/settings/EventLogScreen.kt` prefers saved effective time or saved anchor reconstruction, also IST. **No Android conversion change is needed for P1.** The employee's installed build and exact incident screen remain unverified.

### Shared impact and endpoint inventory

Aggregate SELECT across accessible master + active effective CHECK_IN chains found **24 attendance days / 5 users**, with **20 login-summary mismatch days / 5 users** under the documented IST convention. No individual employee data was exported. This confirms the problem is not isolated to User 7 in this target; it does not prove all mismatches have the same historical cause. Logout drift and full per-day attribution were not enumerated, conserving investigation scope.

Consumers of these shared master writes:

- `attendanceController.myAttendance:95–202`: employee/Manager-own web summary, raw login/logout/total minutes plus separate software-session totals.
- `respondTeamAttendance:~285–410`, `teamLeadAttendance:412`, `managerAttendance:444`: company-scoped team/Manager summaries from master.
- `taskReportController.teamLeadMonthlyReportExport:1748–1755,1795–1805,1842–1844`: master times and minutes into PDF. New main changes display format, not this write defect.
- `attendanceController.bootstrapState:2570–2913`: master login/logout values alongside latest effective state; includes null-to-zero duration defaults. Calling this endpoint can run closure logic, so it was not invoked during read-only verification.
- Mobile event history and day eligibility use effective events and are not fixed by changing master serialization. Master first/last presence are already timestamptz. Normal ingestion duration SQL already uses effective instants, so P1 must not shift or recompute break/gross/net values.

Correction `attendanceRegularizationService.reviewRequest:68` and closure `attendanceDayClosureService.settleEndedDays:49–52` already explicitly cast IST; leave them intact. Legacy auth `attendanceRecorder.recordLogin/recordLogout` uses CURRENT_DATE/CURRENT_TIMESTAMP and DB session timezone, with guards excluding automatic-attendance users; treat its session convention as a separately documented risk, not part of the three-site P1 patch.

## Phase C — Saturday evidence status

Prior SQL reviewed: `attendance-incident-user-7-readonly.sql` and `attendance-backend-audit-readonly.sql`. They distinguish DATE, timestamptz converted to IST, and naive text; include raw inactive records, effective events and correction/closure relations. `attendance_effective_events` exists with migration-025 correction overlays and migration-027 closures in the accessed database. Missing migration 025 or an active-only API filter does not explain Saturday's absence there.

Database already verified is hosted Neon `neondb/public`, using backend local `.env` DATABASE_URL; source code's DB_HOST/etc alternatives are commented out. Target fingerprint:

`9dd1a59da011b996fc1797c97d5f9bb06a2d52fbc2261634a11eaaeab17278fe`

User 7 maps to company 1, active Manager **in this database**. No personally identifying details were printed. No proof connects this target to current/historical production alias or employee APK. This task's aggregate check used the same configured source; no alternate DB was guessed or accessed.

Previous exhaustive accessible-schema scan found zero Saturday-dated User 7 rows across **20 attendance relations**, including raw/inactive/effective events, master, current/legacy regularization requests, review audit, arrivals/presence, diagnostics/monitoring, day overrides/audit/closures and duty tables. Timestamptz filters use IST; raw event receipt date and master linkage were also checked. No accessible alternate attendance schema/archive table was discovered. This does not establish absence from inaccessible schemas, old backups, different Neon branches or phone storage. Today's recheck did not repeat the full historical scan because the unresolved question is target provenance.

Saturday interval to use on the authoritative source is **`2026-10-30T18:30:00Z <= time < 2026-10-31T18:30:00Z`**, or DATE `2026-10-31`; a 31 October terminal policy closure is next midnight **`2026-10-31T18:30:00Z`**, outside that half-open event-time interval. Therefore query closures by **work_date**, and effective events by linked attendance_id as well as timestamp. Existing SQL does so. No Saturday closure was found in the queried target.

**Exact additional evidence needed:** authorized Vercel alias assignment/deployment metadata now and at both incident times; immutable deployment IDs + Git SHA; corresponding environment/Neon branch/endpoint/database fingerprint (without credentials); installed APK build/base URL; authorized employee mapping; original screen/export date, source endpoint and event UUID; phone Room events/anchors/state/diagnostics; backend rejection/receipt/audit logs; historical router, shift and policy configuration. If missing on the verified target, DBA read-only backup/deletion/restore audit and branch inventory are next. Do not assume records never existed or claim deletion.

## Phase D — separate work items for approval

### P1 — Summary timezone correction

**Scope:** backend `src/controllers/attendanceController.js`, function `recordAttendanceEvent`, three SQL sites only, plus focused regression tests after approval. Proposed exact assignment expressions:

```sql
-- Placeholder reuse:
login_time = ($2::timestamptz AT TIME ZONE 'Asia/Kolkata')
-- New master INSERT, keep existing attendance_date expression:
($2::timestamptz AT TIME ZONE 'Asia/Kolkata')  -- login_time value
-- Final checkout summary:
login_time = ($2::timestamptz AT TIME ZONE 'Asia/Kolkata'),
logout_time = ($3::timestamptz AT TIME ZONE 'Asia/Kolkata')
```

Parameters remain validated effective instants/Date objects; explicit timestamptz typing preserves their offsets. Do not change `attendance_events.effective_event_time`, anchor calculations, candidate times, UUIDs, break evidence, state machine or `attendanceMasterProjection`. No schema migration is required. Do not change a global pg timezone/parser setting, which affects unrelated flows.

Impacted read endpoints are the master consumers listed above; API field types stay unchanged. Existing historical bad master rows are **not automatically repaired**. Deploying this code prevents future ambiguous writes; it does not safely repair all past records. Unknown-duration/export/open-break issues remain separate follow-ups, outside this minimal patch.

**Regression acceptance matrix, to implement only after approval:**

1. UTC Z instant and equivalent explicit +05:30 instant produce identical IST naive master values and unchanged timestamptz events; run Node UTC/Asia-Kolkata × DB UTC/Asia-Kolkata.
2. Exercise actual placeholder UPDATE, fresh INSERT and checkout UPDATE against an isolated pre-provisioned test schema. Assert native parameter typing/binding, not mocked SQL substring alone. Fixture writes are test-only in the later implementation phase; none run now.
3. Boundaries 23:59:59.999 / 00:00:00 IST, and post-midnight upload for original-day events: attendance_date, master date and event instant agree; same-day arrival restrictions remain enforced.
4. Verified Wi-Fi arrival at 15:06:01.320, GPS 15:06:29.042, receipt later: preserve all three meanings; summary shows 15:06, not GPS/receipt.
5. Completed break fixture: check-in 09:00, break 12:00–12:30, checkout 18:00 IST -> gross 540, break 30, net 510 minutes. Same results under both input offsets and all TZ pairs; event timestamps unchanged.
6. Preserve existing open-break/same-time BREAK_START+CHECK_OUT, flooring and NULL-duration behavior; P1 must introduce no new change to those semantics. Test current behavior separately from future policy proposals.
7. Own/team/Manager/bootstrap/PDF payload clock values agree with effective events for the fixture; mobile epoch output unchanged; rejected/retried UUIDs do not rewrite event time.
8. Correction and midnight closure continue explicit IST conversion; genuine zero versus unknown durations documented as a separate issue.

Prior evidence: 14 earlier + 26 additional safe mock/pure tests passed. No full suite rerun or tests added in this phase; 12 new SELECT-only binding scenarios were verified. Full migration-executing fixtures remain unrun under current instructions.

**Implementation/deployment plan (future, not executed):** first identify approved target deployment SHA and branch. Create an isolated working branch/worktree from that verified revision, preserving the stale local checkout; do not merge the 65 commits into it. Apply the three SQL changes and tests, review diff, run targeted native-pg and existing safe tests. Build a preview against an isolated test database, verify both TZ axes and endpoint payloads, then obtain deployment approval with exact artifact/commit and target alias. No production attendance-event smoke test that fabricates an employee event.

**Post-deploy read-only verification:** check deployment-to-SHA mapping and aggregate master/effective drift for newly created genuine events only; confirm accepted event instants and work/break values are unchanged. Separate historical drift from post-deploy regressions.

**Rollback:** restore the previously verified immutable deployment/alias through normal release controls if new-write/API regressions occur. No schema rollback or database restore is required. Rollback reintroduces ambiguous future writes, so flag new mismatches for review. Do not mass-shift data or undo accepted events; any historical repair needs a separate audited plan and approval.

### P2 — Saturday attendance investigation

**No code fix selected.** Owner needs authorized deployment/database and phone evidence access.

1. Resolve alias -> deployment SHA -> database/Neon branch for **31 October**, **2 November**, and now; cross-check APK base URL and employee/company mapping. A successful main deployment is insufficient.
2. Run existing SELECT-only SQL on that verified source with read-only connection settings. Include inactive raw events, effective view, correction chain, work-date closure, legacy requests/audits and receipt-time/master-linked searches. No migrations or endpoint calls with closure side effects.
3. Retrieve User 7 phone Room events, saved anchors, boot/state and diagnostics without clearing/restarting monitoring to generate evidence. Correlate original UUIDs/elapsed order, sensor pair, Wi-Fi, gaps, API retries/rejections and received time.
4. Calculate exact accepted BREAK_START/BREAK_END duration in seconds only when both exist. Determine whether 17:30 is original absence, confirmed/classified checkout, receipt, correction, display value or another source. Match historical CRM grace/max absence and corroborate reported 18:30 departure.
5. If still absent, authorized DBA examines backups/branch and deletion/restore audit read-only. Document any inaccessible source explicitly. No speculative events or inferred backdating.
6. Audited correction, only if evidence warrants: retain original raw events and UUIDs, before/after effective projection, reason, approver, actor and corroborating evidence; use supported regularization after separate approval. Do not silently overwrite records or assume reported duration is recorded duration.

## Completion and remaining gate

Original read-only phase is complete. P1 implementation has now been explicitly approved and completed as recorded at the top of this document. Historical runtime attribution and current serving commit/database remain unverified. P2 is unchanged. Production deployment and any historical attendance correction still require separate explicit authorization.

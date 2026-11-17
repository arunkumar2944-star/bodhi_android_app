# P2 resumed implementation planning — Saturday, 31 October 2026

User 7. This checkpoint resumes the completed investigation after P1 implementation. It does not restart evidence collection. No code, configuration, database, tests or deployment changed in this planning turn. Only this plan and the requested handover were written.

## Implementation decision

**No incident-specific P2 code fix can yet be selected reliably.** The completed investigation established scoped record absence and several possible mechanisms, not Saturday's actual checkout timestamp, break pair or causal fault. The minimal safe scope is to recover the missing authoritative incident evidence, select one failing path, then implement only that path after approval. Do not change grace, maximum absence, Wi-Fi approval, break rules or original-exit classification on the basis of employee-reported times alone.

The concrete conditional changes, exact functions, tests and selection criteria below are prepared for review. They are alternatives, not a combined patch proposal. Approval alone cannot substitute for the missing facts.

## Original evidence reviewed and retained

All evidence paths below are under `D:\Prodigit\android\CRMAttendance\docs` unless stated otherwise.

| Original artifact | P2 finding carried forward |
|---|---|
| `attendance-incident-investigation-user-7-2026-10-31-2026-11-02.md`, sections 3–5 and 10 | Saturday 17:30 checkout / 18:30 actual departure and 20–30-minute break are reports, not reconstructed event times. Android path preserves original absence and requires verified return |
| `D:\Prodigit\CRM_TEST_Backend\docs\attendance-backend-forensic-audit-user-7.md`, missing-Saturday and checkout/break sections | Backend transition/idempotency/validation paths; open-break calculation divergence; closure provenance; no attribution to Saturday |
| Backend `docs/attendance-final-verification-and-fix-plan.md`, Phase C/P2 and latest P1 status | Authoritative deployment/DB/phone linkage unresolved; P1 implemented separately |
| `attendance-incident-user-7-evidence.json` | One master/event, dated 2 November; 166 diagnostics, no retrieved Saturday pair; zero overlapping retrieved gaps is not proof monitoring was uninterrupted |
| `attendance-incident-user-7-supplement.json` | Nearby master dates 29 October and 2 November; effective view/current config and diagnostic coverage |
| `attendance-incident-user-7-final-checks.json` | Presence coverage only returned 2 November; no Saturday evidence supplied |
| `attendance-backend-audit-db-evidence.json` | Configured hosted Neon/public target, User 7/company 1/active Manager; no target-day closures/corrections/requests; effective view includes closure behavior |
| `attendance-backend-audit-final-evidence.json` | Twenty accessible attendance relations each have zero User 7 Saturday-dated rows, including inactive raw events, effective projection, audit and older requests |
| `attendance-backend-audit-main-comparison.json` | Local backend base differs from saved remote main by 65 commits; incident deployed code not identified; central audited decision/projection files unchanged in compared revisions |
| `attendance-final-deployment-evidence.json`, `attendance-final-live-metadata.json` | Successful GitHub/Vercel deployment records do not establish incident alias mapping; conflicting Production label/production_environment flag; health exposes no SHA/DB fingerprint |
| Existing two `*-readonly.sql` files | Already inspected raw/inactive, receipt/effective dates, master linkage, corrections and work-date closures; reuse on a newly verified target, not repeatedly on the same unverified source |
| `attendance-p1-regression-results.txt` | 95 passing P1/regression tests, including closure/replay, eligibility, return/projection and correction behavior. These are not a replay of Saturday |

The earlier intermediate mixed-parameter timezone reproduction was superseded by later P1 evidence. It adds no Saturday event evidence. No production API/SQL queries, remote-ref/deployment checks, broad source searches, phone probes or test reruns were repeated in this turn. Source inspection was limited to locating already-identified functions and checking actual P1 overlap.

## Confirmed facts and unresolved hypotheses

**Confirmed from saved records/source:**

- No matching Saturday record was retrieved in the examined database. Counts included raw inactive events and effective/correction/closure/audit relations. Migration 025 missing-view fallback and the normal active filter do not explain that scoped result. The effective view includes migration 027 closure behavior.
- Target fingerprint is `9dd1a59da011b996fc1797c97d5f9bb06a2d52fbc2261634a11eaaeab17278fe`, hosted Neon `neondb/public`. Its role as the incident/production database remains unverified. User identity is verified only in this environment.
- Android requires fresh accurate outside evidence, two increasing fixes 15–90 seconds apart, and no conflicting approved Wi-Fi. Uncertain evidence does not create an event. Same-boot compatible grace/break timing may survive gaps/recovery. Checkout after maximum absence uses original absence start, not classification/upload time.
- A return requires valid GPS and required approved Wi-Fi. Without a verified return, ON_BREAK can persist into original-exit checkout classification. A Monday router mismatch does not establish that this happened Saturday.
- Normal checkout counts completed break pairs; mobile/correction projections can close a nonzero open break at checkout. Server departure validation uses a single submitted fix with distance > radius, while Android uses a stronger accuracy envelope and pair guard. Both are confirmed implementation differences, neither is a confirmed Saturday cause.
- Policy closure is midnight IST with review provenance; no Saturday closure was retrieved. Its existence does not explain a reported 17:30 physical checkout.

**Unknown:** actual accepted CHECK_OUT time, original absence time, BREAK_START/BREAK_END, exact duration, classification/receipt time, return evidence, Saturday office/config/build, gaps/restarts/rejections, and source screen/UUID. A lost return, GPS error, retained timer, configuration mismatch, summary-only issue, alternative deployment/DB, phone-only event or historical restore remain hypotheses. No evidence selects one.

## P1 interaction — actual diff reviewed

P1 remains exactly four assignment changes in backend `src/controllers/attendanceController.js`, `recordAttendanceEvent`:

- placeholder CHECK_IN `login_time`, line 1657;
- new CHECK_IN `login_time`, line 1694;
- CHECK_OUT summary `login_time` / `logout_time`, lines 2167–2169.

They cast validated instants to naive IST wall time. Thus P1 **does intersect the P2 checkout summary processing path**, improving how future summary times are stored. Master consumers such as bootstrap can return the corrected clock strings, while their lastEvent effective time remains unchanged.

P1 does **not** modify sensor fixes, Android `evaluateWorking/evaluateOnBreak`, absence phase persistence, API acceptance/order/idempotency, event effective timestamps, break pair selection, minute calculations or closure/correction policy. It is local and undeployed, and does not rewrite historical Saturday records or retrieve absent evidence. A 5h30m summary convention fault is not proof of the reported one-hour discrepancy.

Consequently no P2 recollection or test rerun is required solely because of P1. Preserve its existing 95-test result. Existing controller/test hashes were recorded before this planning turn and checked afterward; they must remain identical.

## The precise blocking gap and smallest next evidence request

**First obtain the source of the Saturday 17:30 value**, not another scan of the already examined database:

1. An existing incident screen/export or textual record identifying date, employee/company, screen/source endpoint and event UUID/attendance ID. This decides whether 17:30 is an event effective time, summary field, correction, closure, cached/local value or another environment.
2. Existing authorized operator metadata tying that source/installed APK to the incident deployment SHA and database/Neon branch. Compare non-secret target fingerprint to the recorded one; do not print credentials. The saved successful main deployment is insufficient for alias routing on 31 October.
3. The source row/timeline or read-only phone Room export: CHECK_IN, BREAK_START, BREAK_END if present, CHECK_OUT, referenced anchors, boot/elapsed times, sync/rejection fields and pertinent observations. Retrieve one employee/day with its linked audit history, not a new broad inventory.

Why each matters: (1) selects display versus decision layer; (2) establishes that records belong to the incident; (3) distinguishes an invalid early departure from an actual break never verified as ended and permits exact duration calculation.

Those artifacts are absent from all saved evidence. Existing metadata also establishes that direct authenticated deployment inspection was unavailable in the prior session. This turn cannot close the gap from the local archive alone; it did not repeat unavailable access probes or guess another database. Once the operator supplies a new authoritative target/identifier, use the existing SQL only for that newly identified source. The Saturday half-open IST interval is `2026-10-30T18:30:00Z` through before `2026-10-31T18:30:00Z`; query next-midnight closures by work_date and effective events by attendance linkage as already documented.

## Minimal implementation alternatives — select only after evidence

All Android paths below are under `app/src/main/java/com/prodigit/bodhiattendance`. Backend paths are under `D:\Prodigit\CRM_TEST_Backend\src`. These are exact candidate locations, **not a declaration that each needs modification**.

| Evidence that establishes a fault | Minimal proposed change / exact location | Before -> expected after | Component / constraint |
|---|---|---|---|
| Raw/effective checkout correct, specific screen/export wrong | Change only identified reader/formatter: backend `controllers/attendanceController.js` `myAttendance`/`respondTeamAttendance`/`bootstrapState`, or `controllers/mobileAttendanceController.js` `attendance`/`summarize`; Android `ui/live/LiveUiViewModel.kt` `displayTime` / `ui/attendance/view/ViewScreen.kt` only if its payload is already correct | Wrong displayed clock/source -> same accepted effective time displayed accurately, preserving review provenance | Backend or Android depending on proven faulty consumer. Retain P1; no new blanket timezone conversion |
| A genuinely valid office return was observed but a specific return/state transition was dropped | Repair that demonstrated branch in Android `core/monitoring/AutomaticAttendanceEvaluator.kt` `observe` (line 85), or `domain/attendance/AttendancePresenceDecisionEngine.kt` `evaluateOnBreak` (676), or backend `recordAttendanceEvent` only if actual rejection/transition evidence points there | Valid return lost -> one BREAK_END at trusted verified return time; later departure starts its own absence chain | Android, backend, or both only as proven. Missing/nonapproved Wi-Fi is not a valid return to bypass |
| Recovered phase improperly reuses a boot/day-incompatible or contradicted original absence | Repair the demonstrated compatibility/invalidation branch in Android `domain/attendance/DeparturePhaseRecovery.kt` `compatible` (5), `data/local/repository/AttendanceEngineStateStore.kt` `restoreCurrentState` (240), or decision/coordinator path `executeCheckoutClassification` (959) | Incompatible/contradicted timing survives -> reject that timing and retain uncertainty/review without fabricating an alternative departure | Android. **Do not reset every gap or replace original exit with now.** Compatible same-boot retention is intentional and already tested; a gap alone does not prove a bug |
| Exact Saturday pair exists and summary totals disagree with approved policy | Align only proven duration calculation, after deciding incomplete-pair and rounding semantics: backend `controllers/attendanceController.js` `recordAttendanceEvent` completed-break SQL; `controllers/mobileAttendanceController.js` `summarize` (22); `utils/attendanceTimeline.js` `projectAttendance` (3) | Same accepted pair gives inconsistent totals -> agreed formula yields same totals; raw pair unchanged | Backend. Shared calculator is appropriate only if policy is first resolved. This does not inherently change checkout time |
| Router/office/shift historical configuration is demonstrably wrong | Separate audited correction to the specific CRM office/router assignment through its supported administration flow | Valid physical office/radio rejected -> validated approved inventory matches intended office | Configuration, not automatic code bypass; preserve GPS+Wi-Fi requirements and historical audits. Exact config record ID must come from evidence |
| No invalid incident behavior can be demonstrated | No code change; retain investigation/regularization review only | Avoid altering correct original-exit behavior based on an unsupported hypothesis | Neither backend nor Android patch justified |

Backend single-fix/two-fix parity, PDF null-as-zero and policy-closure labels are known separate findings. Do not bundle them into the Saturday fix without causal evidence and separately agreed scope. Enforcing a full outside pair server-side would require an evidence contract and likely Android payload changes; it is not an established minimal P2 incident fix.

## Regression and incident replay plan (not executed this turn)

First derive a replay fixture from the recovered incident UUIDs/boot/elapsed evidence. Preserve original IST/offsets and effective/confirmation/receipt meanings. If no true timestamps are supplied, fixtures must remain explicitly synthetic and cannot verify this incident.

1. **Saturday replay:** assert the recovered actual initial absence, grace completion, return, final departure and checkout classification; evaluate the claimed 17:30/18:30 conflict using evidence, not forced expected values. Assert exact recorded break seconds only for a valid accepted pair; unpaired break is unknown/incomplete.
2. **Valid break/return:** synthetic 20- and 30-minute completed breaks; valid required Wi-Fi/GPS return generates one BREAK_END; later checkout uses the later original departure, not the completed break start. Unverified/mismatched Wi-Fi must not fabricate a return.
3. **GPS guard:** drift near radius, bad accuracy, stale/mock, Wi-Fi conflict, two outside fixes <15s / 15–90s / >90s, duplicate/reversed fixes. No uncertain observation creates attendance.
4. **Recovery:** same-boot compatible absence survives as current approved policy requires; boot/day mismatch rejected; observed valid return cancels old grace/break; an observation gap alone proves neither continuous presence nor absence. Tests must reflect selected, approved continuity policy rather than impose a new one.
5. **Ordering/retry:** offline upload/rejected return/replay/reordered BREAK_END; stable UUID/proof; no duplicate event or accidental checkout reclassification. Preserve raw evidence; invalid chains fail/review rather than invent timestamps.
6. **Cross-layer totals:** completed/open/zero-length terminal breaks, second-level precision/rounding, raw versus effective correction overlay, normal summary/mobile/correction consistency under agreed semantics. Unknown durations stay distinguishable from zero if that change is separately selected.
7. **Closure:** midnight policy origin stays distinct from physical departure, real/corrected checkout supersedes fallback, no fabricated break return; historical work_date query finds closures at next midnight.
8. **P1 protection:** rerun `attendanceSummaryTimezone.test.js` and applicable existing 95-test set after a selected P2 patch; all four assignments, effective instants and IST boundary behavior preserved. Do not rerun the suite now for documentation-only planning.

Existing Android tests to extend only after selection: `DepartureEvidenceTrackerTest`, `AttendancePresenceDecisionEngineTest` (especially `outsideAfterObservationGap_confirmsBreakUsingOriginalDeparture`, `uncertainAtCheckoutDeadline_doesNotCheckout_untilFreshOutsideEvidence`, `verifiedReturnCancelsRetainedGrace`), `DeparturePhaseRecoveryTest`, `AttendanceEngineStateStoreTest`. Existing backend tests: `attendanceRegularization`, `returnAndNotifications`, `attendanceDayClosure`, `attendancePresentation`, `attendanceMaintenance`, `attendanceSummaryTimezone`.

## Risks and implementation gate

- Unconditionally resetting absence on every gap can delay valid checkout and violate the intentional original-exit rule. Classifying elapsed unknown time as absence can instead retain a break too long. Neither assumption is supported by missing Saturday evidence.
- Accepting unmatched Wi-Fi/GPS as a return weakens attendance verification. Tightening server validation without a compatible evidence contract can reject valid offline/backlogged events.
- Changing open-break/rounding semantics affects payroll totals and corrections beyond this employee; scope separately and never silently recalculate historical data.
- Wrong environment or installed build can make a correct patch irrelevant to the incident. P1 remains local/undeployed; do not treat it as deployed evidence.
- Record regularization, if warranted, needs independent arrival/departure corroboration, original UUIDs and before/after timeline, explicit reason and authorized approver. It is a separate audited action, never automatic backdating.

**Gate:** obtain the smallest authoritative incident artifact above, confirm one cause and select its narrow row in the implementation table. Then present the concrete diff scope and replay expectations for explicit approval. Current deliverable is a completed evidence-based P2 implementation-planning checkpoint, not a claimed fix or confirmed Saturday root cause. P1 is preserved; no database/config/code changes, commit, push or deployment occurred.

# User 7 investigation checkpoint — 2 November 2026

## P2 resumed planning — latest checkpoint

- Read original P2 sections in both investigation reports, final plan, referenced JSON snapshots/schema/counts/deployment metadata and SQL; did not restart collection or repeat DB/API/deployment scans. Plan: `attendance-p2-implementation-planning.md` in Android docs (backend docs copy also provided).
- Completed investigation still establishes **no confirmed Saturday root cause**: 17:30 checkout, actual 18:30 departure and reported 20–30-minute break have no retrieved authoritative Saturday event/pair. Saved 20-relation zero results remain scoped to the unverified incident database; migration 025/027/filter checks already completed.
- Actual P1 diff reviewed: checkout summary path does intersect P2 through corrected login/logout representation, but no departure detection, original-absence timer, return transition, break calculation, accepted effective time or historical record changed. P1 controller/new-test hashes checked before/after planning; unchanged. Existing 95 passes retained; no unnecessary rerun.
- Conditional minimal changes are mapped to exact reader/return/phase-recovery/calculation locations in the plan, with selection evidence, before/after expectations and backend/Android/config scope. **No P2 code change selected** without a causal incident artifact. Known open-break totals/server guard/closure-label differences are not proven Saturday causes and are not bundled.
- Blocking artifact: original Saturday screen/export identifier + event UUID/attendance ID/source endpoint, tied to incident deployment/DB/employee mapping, plus raw/effective/phone event+anchor+boot+sync evidence. This chooses display-only error versus genuine early event/missed return/recovery. No alternate DB guessed; repeat existing SQL only when a newly authoritative target is identified.
- Regression plan prepared for actual incident replay, completed break/return then final departure, outside-pair/uncertainty, compatible/incompatible recovery, retries, duration/closure projection and P1 preservation. Existing same-boot gap-retention tests document intentional behavior; do not change it without evidence/policy justification.
- No code/config/DB/Android changes, commits, pushes or deployments in this turn. Next: obtain missing authoritative artifact, select a concrete narrow fix, then seek explicit implementation approval. P1 remains implemented and undeployed; P2 remains planning/evidence-gated.

## P1 implemented — current status (supersedes earlier read-only checkpoints)

- User explicitly approved P1 implementation. Backend local branch remains `main`, base `4ad42afc60b201d9017cf66a5e5df4a38bd2608f`; no branch/ref changes, commits, pushes or deployment.
- Changed only four assignments at three sites in `src/controllers/attendanceController.js`, `recordAttendanceEvent`: placeholder CHECK_IN login (`1657`), new CHECK_IN login (`1694`), CHECK_OUT summary login/logout (`2167–2169`). Each uses `($n::timestamptz AT TIME ZONE 'Asia/Kolkata')` into existing naive IST columns. Existing effective instant parameters, data types, API formats and calculation code preserved; no JS formatting helper/global timezone change.
- New `tests/attendanceSummaryTimezone.test.js`: seven tests execute extracted production SQL against isolated in-memory PGlite PostgreSQL with installed pg parameter serialization. Four Node/DB TZ combinations × six instant/offset/boundary/precision cases × Date/string input = 48 fixture paths through all three writes. Covers NULL/invalid SQL input, repeat updates, raw-event preservation, unchanged gross/break/net and the actual User 7 timestamp as a synthetic test clone.
- Before/after clone: legacy UTC summary `2026-11-02 09:36:01.32`; fixed summary `2026-11-02 15:06:01.32`; raw effective time remains `2026-11-02T09:36:01.320Z`, confirmation/receipt unchanged. Checkout fixture persists 09:00–18:00 IST, gross 540, completed break 30, net 510 minutes.
- **95/95 selected regression/integration tests passed**, zero failed/skipped across 12 files. Includes closure/late replay, eligibility/holiday/leave/Sunday, regularization, arrival proof, bootstrap, return/master projection, auth guards and diagnostics. Log: `attendance-p1-regression-results.txt` in Android docs. Test-only closure fixture migrations ran solely inside PGlite; no migration against a hosted/deployed database.
- PGlite 0.5.10 was declared/locked but missing locally; installed that exact version into the isolated visualization `p1-test-deps` tools folder and used NODE_PATH. Backend package.json/lockfile/node_modules/config unchanged. Database tests did not load the backend .env or use a hosted connection; no production reads/writes in this implementation phase.
- Validation limitations: PGlite exercises PostgreSQL persistence and native pg serialization, not a remote PostgreSQL wire-protocol deployment or deployed API smoke test. Only the captured User 7 example was cloned; **the 20 historical mismatches/five users are not corrected or claimed resolved**. Separate audited repair would need approval.
- P2/Android/CRM shift config/attendance business logic untouched. Existing local documents preserved. Source diff is four removed/four added assignment lines; new test and documentation only. Ready for code review; deployment, production connectivity verification and historical correction remain separately authorized work.

## Final verification and fix preparation (current checkpoint)

- Plan: `attendance-final-verification-and-fix-plan.md`. READ-ONLY; no implementation, deployments, migrations, Git integration or data corrections.
- Both Git tips rechecked: Android local/default android-ui-development `af127869...` matches remote; no main ref advertised. Backend local `4ad42af...`, remote main `5981a8c...`, still 65 behind. Only investigation documents are local changes.
- GitHub records successful deployment of `5981a8c...`, ID 7253183669, environment label Production but production_environment=false. Newer Preview records exist. Actual phi alias/incident serving commit and database linkage remain unverified. Public health 200 gives no SHA/fingerprint; no Vercel token available.
- **Refined exact defect:** new CHECK_IN INSERT already types `$2` timestamptz, so implicit naive assignment depends on DB session timezone; placeholder/checkout UPDATE naive binding also depends on Node Date serialization. Twelve SELECT-only scenarios prove explicit IST conversion stable across both TZ axes and IST midnight. This supersedes blanket Node-only descriptions in earlier reports.
- Aggregate accessed-DB impact: 20 mismatching login-summary days out of 24 event-backed days, spanning 5 users. No per-user details exported; not all historical causes proven.
- **P1 approval proposal:** only three SQL sites in `attendanceController.recordAttendanceEvent` get explicit `::timestamptz AT TIME ZONE 'Asia/Kolkata'` for master login/logout; accepted events, Android and break formulas untouched. Plan includes native pg TZ/boundary/endpoint/break regressions and deployment/rollback. No historical repair bundled.
- **P2:** authoritative incident deployment/Neon branch + employee mapping + screen UUIDs + phone Room/audit evidence needed before Saturday duration/cause/correction. Existing 20-relation absence remains scoped to accessed DB; closure searches must include work_date/master linkage around midnight.
- Evidence: `attendance-final-deployment-evidence.json`, `attendance-final-live-metadata.json`, `attendance-final-timezone-evidence.json`. Next action is explicit user approval of narrow P1 scope and authorized operator evidence for P2; preserve all old documents/raw events.

## Backend audit follow-up (latest checkpoint)

- Full report: `attendance-backend-forensic-audit-user-7.md`; SQL: `attendance-backend-audit-readonly.sql`. New JSON evidence covers GitHub comparison, DB provenance/view definition and final reproduction/counts.
- **Confirmed timestamp defect reproduced:** normal ingestion binds JS Date directly into naive master login/logout. UTC Node stores 09:36 while correct IST is 15:06. Final two-parameter reproduction is authoritative; the intermediate mixed-parameter cast result is superseded. Accepted event/arrival time remains correct. Historical deployed runtime still needs confirmation.
- Local backend `4ad42af...` is **65 commits behind** remote main `5981a8c...`. Core ingestion/projection/closure files are unchanged in that comparison. No fetch/checkout/deploy performed.
- Hosted Neon `neondb`, public schema, User 7/company 1/Manager verified; **production/deployment linkage unverified**. DB fingerprint is recorded in the report.
- Saturday absence expanded to **20 attendance relations**, including inactive raw events, legacy regularization requests and audit/closure tables. All Saturday counts zero. Effective view includes migration 025 and 027 behavior, so normal filtering/missing-view fallback does not explain the absence here.
- Confirmed source divergences: normal checkout ignores nonzero unclosed break while mobile/correction closes it; PDF treats NULL hours as zero; web summary omits policy-closure provenance; backend single-fix departure validation is weaker than Android two-fix/accuracy-envelope guard. No Saturday attribution established.
- **26 additional mock/pure tests passed.** Day-closure migration-executing fixture reviewed, not run under no-migrations rule. No fixes.
- Next: verify deployed SHA/DB/Neon branch and APK target; obtain original screen UUIDs and phone export; run new SELECT-only SQL on authoritative target; retrieve Saturday pair before calculating duration; agree minimal timestamp/unknown-duration/calculation fixes for explicit approval. Do not silently regularize historical records.

Read `attendance-incident-investigation-user-7-2026-10-31-2026-11-02.md` first. No fixes authorized or implemented. All new files are investigation documents/evidence only.

## Completed

- Android and backend Git identity recorded; both initially clean.
- Traced sensors → decision engine → Room → sync → backend validation → events/summary.
- Existing backend database access succeeded in forced READ ONLY transactions; credentials never printed.
- Retrieved User 7/company 1 records for both days; saved three JSON evidence snapshots and executable read-only SQL.
- Monday confirmed: unmatched Wi-Fi :39 blocked observed inside GPS from about 14:26; approved :3A arrival at **15:06:01.320 IST**, GPS **15:06:29.042**, receipt **15:06:30.910**. Arrival preservation worked.
- No Saturday records returned in inspected master/raw/effective events, diagnostics, arrivals or regularization; exact checkout/break remain unknown.
- Separate summary inconsistency: naive login 09:36 versus event 15:06 IST; local cast check did not reproduce it.
- Public diagnostics panel HTTP 200; unauthenticated diagnostics HTTP 401. No account login attempted.
- Focused backend tests: **14 passed**. Android tests inspected, not run.

## Pending / missing evidence

- Actual installed APK/backend/database identity; exact screens behind reported 15:10 and 17:30.
- Saturday phone Room events, anchors, engine state and diagnostics; no phone inspected (ADB absent from PATH).
- Evidence at Monday 14:15; ownership/approval history of :39 and phone configuration cache.
- Historical Saturday policy/shift/eligibility; API rejection/deployment/audit logs.
- Deployed-runtime explanation of naive summary timestamp mismatch.

## Exact next steps

1. Verify installed APK base URL/build and deployment DB identity without exposing environment secrets.
2. Obtain screenshot/export plus a read-only phone database/diagnostic export; preserve app state and original records.
3. Run `attendance-incident-user-7-readonly.sql` on the confirmed authoritative DB; retrieve Saturday UUIDs and break pair first.
4. Correlate Saturday boot/elapsed observations, two outside fixes, original absence start, return, recovery and checkout classification. Calculate duration from actual effective timestamps.
5. Audit :39/:3A router inventory and historical CRM approval/cache; do not approve an unknown router automatically.
6. Reproduce summary writes under deployed UTC/IST process/session settings; then propose narrow fixes and audited regularization for approval.

Do not infer Saturday facts from current source or treat absent records in this database as proof no attendance occurred. Avoid further broad exploration/builds until deployment and phone evidence resolve the gap.

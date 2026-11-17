# Attendance incident investigation — User 7

Investigation date: 2 November 2026. Presentation zone: Asia/Kolkata (IST, UTC+05:30).
Scope: read-only investigation; no application fixes, migrations, configuration changes or attendance corrections performed.

## 1. Executive summary

**Monday has a confirmed Wi-Fi policy blockage in the observed period. Saturday cannot be resolved from the available database.** The reported exact times are not corroborated by the retrieved records.

- **Incident A, high business impact:** the accessible database records CHECK_IN at **15:06:01.320 IST**, not 15:10. GPS confirmation was **15:06:29.042**, receipt **15:06:30.910**. The original approved Wi-Fi arrival was correctly preserved by 27.722 seconds. Earlier diagnostics, beginning about 14:26, repeatedly report inside-office GPS but nonmatching Wi-Fi BSSID ending **:39**; the currently approved BSSID ends **:3A**. This directly explains the observed blocks, but not the unobserved 14:15 arrival or exact 15:10 display.
- **Incident B, high business impact:** no 31 October attendance master, raw/effective events, arrival candidates, diagnostics, regularizations or overlapping monitor gaps were returned for User 7. A 17:30 checkout therefore cannot be authenticated or attributed to a root cause in this data source.
- **Incident C, impact undetermined:** no Saturday BREAK_START/BREAK_END were returned. Exact duration and influence on checkout are unknown. Local diagnostic counters show historical break rows exist on the phone, but counters do not identify their dates or times.
- **Separate confirmed integrity concern:** master row 52 has timezone-free `login_time = 2026-11-02 09:36:01.32`, whereas its anchored effective event is `2026-11-02T09:36:01.320Z` = **15:06:01.320 IST**. Under the backend's documented IST wall-clock convention, these disagree by 5h30m. This is not an explanation for either reported 55/60-minute difference. Runtime-specific serialization/casting is a hypothesis requiring deployed-runtime reproduction.

### Workspace, provenance and access

| Item | Verified result |
|---|---|
| Android path | `D:\Prodigit\android\CRMAttendance` |
| Android branch / HEAD | `android-ui-development`, `af127869032e24bbc6f465f553cb9be543255567` |
| Initial Android status | Clean; no pre-existing tracked/untracked changes reported |
| Android build configuration | versionCode 15, versionName 1.0.2; installed APK version/build not established |
| Backend path | `D:\Prodigit\CRM_TEST_Backend` |
| Backend branch / HEAD | `main`, `4ad42afc60b201d9017cf66a5e5df4a38bd2608f` |
| Backend status | Clean using read-only elevated Git inspection |
| DB access | Successful using existing backend `.env` DATABASE_URL, without exposing credentials; transactions forced READ ONLY |
| DB identity | User 7, company 1 |
| Snapshot | Primary evidence captured `2026-11-02T11:29:30Z` approximately; supplementary DB clock `2026-11-02T11:30:20.618Z` = 17:00:20.618 IST |
| DB session zone | GMT; a session setting, not proof of the deployed application's session zone |
| API base | Release fallback `https://crm-test-backend-phi.vercel.app/`; debug default `http://127.0.0.1:5000/`; build property `bodhiApiBaseUrl` can override |
| Diagnostics deployment | GET `/api/v1/attendance/diagnostics/panel` HTTP 200; unauthenticated GET diagnostics HTTP 401 |

The locally configured database is accessible, but its linkage to the employee's incident deployment and the public Vercel deployment is **not proven**. Do not generalize Saturday's absence here to all systems. No login was attempted, token minted or attendance-producing endpoint called. Initial sandbox DNS failed; the authorized read-only probe succeeded outside the sandbox. No AGENTS.md was found in the initial workspace scan or backend root.

Evidence snapshots, relative to this report:

- `attendance-incident-user-7-evidence.json`: selected event evidence, anchor, arrival, 166 diagnostics, master record, current office policy; no precise event GPS coordinates or credentials.
- `attendance-incident-user-7-supplement.json`: DB clock, nearby-day counts, approved BSSIDs, relevant schema, effective-view results and trigger inspection.
- `attendance-incident-user-7-final-checks.json`: presence-evidence counts and SELECT-only timestamp-cast check. The cast check ran under the local Node runtime and produced the correct IST naive value; it **did not reproduce** the stored master discrepancy. Its `explicit_ist` JSON serialization is not an authoritative instant because the SQL expression returns a timezone-free timestamp.

## 2. Monday check-in investigation

### Reported versus observed timeline

| IST time, 2 November | Evidence / interpretation |
|---|---|
| 14:15 | Employee-reported arrival only; no retrieved observation at this time |
| 14:26:26.969 | Trusted anchor server time `08:56:26.969Z`, anchor ID 161, UUID `4fd5a8bc-dee2-4cb4-a6fd-3ed006a40bc7`, anchor elapsed 384762675 ms |
| About 14:26:31–14:26:40 | Startup/Wi-Fi diagnostics; connected BSSID `0e:36:23:2a:42:39`; inside GPS, accuracy 18.399 m, radius 30 m, wifiMatched=false; engine explicitly blocks as uncertain |
| About 14:27–15:05 | Repeated unmatched :39 observations and inside GPS; occasional accuracy failures (29.671, 58.254 and 25.451 m exceed 25 m limit). Some multi-minute observation gaps exist; continuous monitoring is not established |
| About 15:05:59 | Office selection temporarily ambiguous; no attendance inferred |
| 15:06:01.320 | First persisted approved Wi-Fi arrival, elapsed **387137026 ms**, candidate `0a05b8c1-c8e0-4bc6-ada1-60e9ab729dd1` |
| 15:06:06.781 | Arrival candidate server receipt |
| 15:06:29.042 | GPS confirmation elapsed **387164748 ms**, accuracy 19.004 m; effective arrival remains 15:06:01.320 |
| 15:06:30.910 | CHECK_IN server receipt, event UUID `580ec82c-9810-4855-8eb6-c2f5018dbb91`, event ID 96, attendance ID 52 |
| About 15:06:38 | EVENT_SYNCED diagnostic; no rejection/retry evidence for this UUID in retrieved diagnostics |
| 15:10 | Employee-reported displayed time; no matching event timestamp retrieved |

Approximate diagnostic times above use untrusted `device_time` converted to IST for readability. Within-boot ordering uses elapsed time. Trusted anchor reconstruction is `anchor.server_time + (observation_elapsed - anchor_elapsed) milliseconds`. The first GPS evidence elapsed 384774866 reconstructs **14:26:39.160 IST**; device diagnostic wall time is about one second different. The trusted-time diagnostic reports approximately -1059 ms phone-clock difference, not 55 minutes.

### Confirmed actual path

1. `AttendancePresenceService.kt:428–437` records approved Wi-Fi evidence before waiting for GPS. Callback paths at 117–121 track Wi-Fi changes/loss; the startup path resets continuity at 174.
2. `ArrivalCandidateRecorder.kt:35–57` requires a unique Wi-Fi-required matching CRM office, NOT_CHECKED_IN state, matching identity and a boot-compatible server anchor. BSSID :39 cannot create an approved candidate against the observed :3A policy.
3. `AutomaticAttendanceEvaluator.kt:208–240` rejects excessive accuracy and marks inside GPS with missing/nonmatching office Wi-Fi uncertain. This matches the uploaded ENGINE messages. GPS was available well before check-in; a GPS-only delay is not supported for those observed readings.
4. `ArrivalCandidateRecorder.kt:60–75` selects a same-office/BSSID/boot candidate, checks elapsed ordering and recent continuity, reconstructs trusted time and requires the same IST date.
5. `AutomaticAttendanceEvaluator.kt:255–265` attaches that proof to CHECK_IN. `AttendanceEventManager.kt:673–688` stores proof JSON and effective arrival in Room; `AttendanceSyncManager.kt:327–455` uses the event's saved anchor/proof rather than replacing them during retry.
6. Backend `src/utils/verifiedWifiArrival.js:2–42` checks proof shape, active onboarded device, anchor identity, office/router, same day and replay conflicts. `src/controllers/attendanceController.js:1305–1321` saves GPS confirmation separately and replaces effective check-in with verified arrival. Lines 2193–2196 associate candidate and confirmation with the event. Retrieved DB fields demonstrate that this path worked.
7. Android `ui/settings/EventLogScreen.kt:25–48` prefers saved effective time, otherwise reconstructs from saved anchor, and formats in Asia/Kolkata. The exact screen the employee saw is not known.

**Critical requirement answer:** yes, valid trusted arrival evidence should preserve 14:15 after later GPS confirmation. Current source supports this; the retrieved event proves preservation for 15:06:01.320, not 14:15. No retrieved approved 14:15 candidate exists. Earlier unmatched Wi-Fi must not be silently converted into an approved arrival.

Arrival continuity is invalidated by explicit disconnect, different router/office, reboot, service restart or a gap exceeding 180 seconds (`ArrivalCandidateRecorder.kt:30–54`; `docs/verified-wifi-arrival.md`). A valid old candidate can therefore be conservatively lost. There is no evidence that such invalidation caused this specific Monday incident. Uploaded candidates do not retain every local invalidation reason; Room export is needed to rule out discarded local evidence.

The observed :39-to-:3A change is established by diagnostics and current configuration. Whether :39 is a legitimate office radio omitted from CRM, a roaming band, another access point, or stale client configuration remains unverified. Router ownership/configuration history is required before changing approval.

### Timestamp summary concern

Master login is `09:36:01.32` without timezone; event/first_presence are `09:36:01.320Z`. The backend's `src/database/connection.js:34–54` describes naive columns as IST wall time and returns them as strings. `attendanceController.js:1680–1701` inserts `effectiveEventTime` into naive login_time without explicit IST conversion; checkout updates at 2167–2169 also deserve review. Binding/serialization may depend on the Node runtime timezone. There are no attendance-table triggers in the inspected schema. The local SELECT-only cast test produced correct IST and did not recreate the stored discrepancy; deployed runtime, write path and display must be traced before assigning the exact mechanism. No evidence links this discrepancy to a 15:10 display.

## 3. Saturday checkout investigation

### Timeline and missing observations

| IST time, 31 October | Status |
|---|---|
| Break, unspecified time | Employee reports approximately 20–30 minutes; timestamps unavailable |
| 17:30 | Reported checkout; no underlying event retrieved |
| 18:30 | Reported actual departure; no independent observation retrieved |

Raw events and the `attendance_effective_events` projection both return no Saturday events. There is no Saturday master row, uploaded diagnostics or arrival candidate for User 7. Nearby master dates returned were 29 October and 2 November. No regularization rows for either requested day, and no monitor gaps overlapping the investigated range, were returned. No Saturday presence evidence was returned by receipt-date coverage query. These are scoped negative results, not proof the employee did not attend or the service had no gaps.

### Actual source decision path

- `GpsEvidenceValidation.kt:9–23`: rejects mock, invalid or older-than-60-second GPS before attendance evaluation.
- `AutomaticAttendanceEvaluator.kt:208–245`: requires CRM accuracy, definitely outside (`distance - accuracy > radius`), and no conflicting approved Wi-Fi; unknown Wi-Fi is conservative. Wi-Fi loss alone only creates review evidence, not a break (`198–206`).
- `DepartureEvidenceTracker.kt:11–39`: two increasing, distinct outside fixes separated by **15–90 seconds**; duplicate/reversed/too-rapid fixes cannot confirm, >90 seconds starts a new pair. Grace begins at the first *engine-accepted* outside observation, normally the second fix, not necessarily the first raw outside fix.
- `AttendancePresenceDecisionEngine.kt:~520–710`: starts AbsenceGrace; after CRM grace creates BREAK_START at the **original absence start**, not confirmation/receipt time. Despite a nearby comment saying “created NOW,” the supplied effective elapsed time is the absence start.
- `AttendancePresenceDecisionEngine.kt:~750–900`: verified return creates BREAK_END at the return fix; maximum absence classifies CHECK_OUT at the original absence start. `AttendanceEngineCoordinator.kt:1035–1090` passes that original elapsed timestamp to event creation with current evidence.
- `AttendanceEngineStateStore.kt:~280–390` rejects boot mismatch and stale workday state. `DeparturePhaseRecovery.kt:5–9` retains compatible WORKING+AbsenceGrace or ON_BREAK+BreakActive state. A same-boot observation gap does **not** necessarily clear the original absence timer. Uncertain observations preserve it; a fresh pair can later trigger classification using that old time.

Current office 1 policy is radius 30 m, max accuracy 25 m, grace **3 minutes**, maximum absence **90 minutes**, Wi-Fi required and BLE false. These are current values, not proven historical Saturday values. A genuine 17:30 absence under this policy could be classified around 19:00 or later, still effective 17:30. This illustrates the mechanism; it is not the Saturday timeline.

**Potential causes, all unverified for Saturday:** false outside GPS pair; wrong CRM office/radius; valid break without verified return; retained pre-gap absence phase; service/reboot recovery; alternate/legacy record source; summary/regularization/display divergence. A 60-minute discrepancy does not establish a 60-minute policy or timezone error. Need the exact event UUID, raw evidence and deployment first.

## 4. Saturday break investigation

BREAK_START: **unknown**. BREAK_END: **unknown**. Exact duration: **not calculable from available evidence**. Reported 20–30 minutes must not be substituted for records.

`LOCAL_EVIDENCE_AUDIT` Monday diagnostics include `roomBreakStarts=17; roomBreakEnds=12; roomUnsettled=0; sqliteCandidates=12`. These are aggregate phone counters, not Saturday events. Different counts do not prove five missing returns: some starts may be terminal departures classified as checkout, and dates/sync states are absent from the counters.

Source semantics: break start retains original confirmed absence timing; break end requires valid office presence (including approved Wi-Fi where required). If a return is uncertain or on the wrong BSSID, the engine can remain ON_BREAK and eventually classify the original break departure as checkout. Observation gaps preserve timing but do not themselves create events. This creates a plausible relationship between incidents B and C, not a confirmed one.

Backend `attendanceController.js:1998–2120` pairs active BREAK_START/BREAK_END using effective event ordering and sums completed pair seconds, then floors total minutes. An unclosed terminal break contributes zero to completed-break minutes. Need both raw and effective-view rows, summary and regularization history to establish actual duration, duplicates or rewritten projections. Event UUID deduplication is implemented at `attendanceController.js:~1010–1064`; Android retries preserve UUID/proof. No Saturday retry, rejection or ordering evidence was retrieved.

## 5. Root-cause matrix

| Issue | Finding | Evidence | Classification / confidence | Layer |
|---|---|---|---|---|
| Late check-in | Earlier observed inside GPS blocked by unmatched :39 BSSID | ENGINE + ATTENDANCE_EVIDENCE diagnostics; approved :3A config | Confirmed / high for observed ~14:26–15:06 period | Sensor/config/policy |
| Late check-in | Approved arrival 15:06:01.320 preserved; GPS 27.722s later | Event 96 + linked VERIFIED candidate + anchor | Confirmed / high in accessed DB | Android/backend |
| Late check-in | Exact reported 14:15→15:10 discrepancy | No 14:15 evidence; retrieved effective time differs | Unverified | Deployment/data/display |
| Early checkout | No Saturday record in accessed source | Raw/effective/master/diagnostic queries | Confirmed absence in this source; cause unverified | Evidence coverage |
| Early checkout | Retained absence or unrecognized return could classify break start as checkout | Decision engine, recovery and two-fix guard | Unverified incident hypothesis | Android policy/recovery |
| Break timing | Exact pair/duration and causal link unavailable | No Saturday event pair | Unverified | Evidence coverage |
| Summary integrity | Naive login and anchored event differ under documented IST convention | master 52 vs event 96 | Confirmed mismatch; runtime-cast mechanism probable, not reproduced locally | Backend projection |
| Knowledge Centre policy | Current “Knowledge Centre - Home Test” row is inactive and Wi-Fi-required | office 2 current config | Confirmed current config; relation to intended GPS-only office unverified | CRM |

“Not reproducible” is not used to imply the reported incidents were disproved. Only the timestamp-cast hypothesis was checked locally and failed to reproduce there.

## 6. Recommended solutions — proposals only

| Priority | Module / recommendation | Expected behavior and rationale | Risk / required verification |
|---|---|---|---|
| P1 | CRM office Wi-Fi inventory and Android config cache: verify ownership of :39, approval history and installed cached config before proposing approval | Legitimate office radios recognized; arbitrary routers remain rejected | Approving wrong radio weakens attendance policy; test roaming, multiple bands, stale cache and unapproved APs |
| P1 | Backend `attendanceController.js` naive login/logout writes and summary/report readers: explicitly define IST conversion if runtime reproduction confirms cause | Master and effective event represent same IST time independent of process zone | Legacy timestamp consumers can double-convert; test UTC/IST process and DB sessions; no bulk historical rewrite |
| P1 | `AttendancePresenceDecisionEngine`, evaluator and recovery: review retained absence across long unobserved intervals after Saturday evidence is obtained | Do not classify an unverifiable gap as continuous physical absence; preserve genuine original exits | Policy decision can delay legitimate checkout; test return during gap, valid outside reconfirmation and missed break return; do not change CRM timers to hide issue |
| P2 | `ArrivalCandidateRecorder` / service: retain explicit invalidation/restart reasons as diagnostic evidence | Explain why an early proof became unusable without accepting uncertain proof | Volume/privacy/retention risks; test disconnect, router change, >180s gap and reboot |
| P3 | Diagnostics and deployment provenance | Correlate app version, backend revision, event UUID and effective/GPS/receipt timestamps | Keep detailed diagnostics company-scoped and role-limited; verify permissions |

No incident-specific code fix is justified for Saturday until the missing records are identified. Monday supports configuration verification before code changes. The summary inconsistency warrants a separate focused reproduction. P0 escalation only if further evidence establishes systemic corruption/security impact; none established here.

## 7. Attendance correction recommendation

**Do not auto-correct or backdate either day.** Audited regularization may be appropriate after corroboration of actual arrival/departure and identification of authoritative records. Preserve original UUIDs, effective/confirmation/receipt times, approving actor, reason and before/after projection. Employee testimony, office/router access history, supervisor confirmation and phone Room export should be reconciled first. An unmatched router observation alone is not approved arrival evidence.

## 8. Regression test plan and completed validation

Completed: 14/14 backend mock-based tests passed using `node --test` on `tests/verifiedWifiArrival.test.js`, `tests/attendanceDiagnosticController.test.js`, `tests/diagnosticValidation.test.js`. These test proof/dedup conflict/access handling with mocks; they do not validate live sensor history or the deployed revision. No Android build/install/test was run, preserving the read-only investigation scope and usage budget.

Relevant existing Android tests inspected: `DepartureEvidenceTrackerTest.firstOutsideObservationRequiresConfirmation`, `gapRequiresNewPairButDoesNotRequireNinetyMinutesOfContinuousGps`, `rapidRepeatedOutsideFixesDoNotConfirmDeparture`, `duplicateAndReversedObservationsDoNotConfirmDeparture`; `AttendancePresenceDecisionEngineTest.outsideAfterObservationGap_confirmsBreakUsingOriginalDeparture`, `uncertainAtCheckoutDeadline_doesNotCheckout_untilFreshOutsideEvidence`, `verifiedReturnCancelsRetainedGrace`, `absenceReachesMaximum_classifiesOriginalExitAsCheckout`; `DeparturePhaseRecoveryTest.matchingBreakStateRetainsOriginalCheckoutTiming`, `missingOrBootInvalidatedTimingCannotBeInvented`; instrumented `ArrivalCandidateStorageTest.firstArrivalSurvivesLaterGpsRetryAndRemainsUserScoped`.

Required subsequent cases:

1. Approved Wi-Fi at 14:15, GPS at 15:10, continuous observations: effective 14:15, distinct GPS/receipt timestamps; same UUID through offline retry.
2. Disconnect/router change/restart/reboot/>180s gap before GPS: old arrival invalid, no fabricated early CHECK_IN; preserve reason.
3. Unapproved :39 versus approved :3A, roaming and cached configuration: valid GPS alone cannot bypass Wi-Fi policy.
4. Boundary drift, excessive/stale/mock GPS and approved-Wi-Fi conflict: uncertain, no event.
5. Two accurate outside fixes at <15s, 15–90s, >90s, duplicate/reversed times: only valid pairs accepted.
6. Observation gap, process death and same-boot recovery: verify retained timer behavior against intended policy; return during gap must not be assumed absent.
7. Reboot and next-workday restoration: old monotonic phase rejected; no invented break/checkout timing.
8. Break return during monitoring interruption or on wrong BSSID; recovery uses original trusted start and verified return; duration calculated from effective pair.
9. Duplicate/reordered/rejected events and API retry: stable UUID/proof, no double insertion or silent time substitution.
10. Expired/inactive/mismatched anchor: no event backdated with a replacement anchor; pending/review outcome explicit.
11. Summary versus raw/effective events and regularization projection: test UTC/IST Node process and DB zones; preserve raw timestamp types, completed-break flooring, open terminal break and audited corrections.

## 9. Final conclusion

1. **Why Monday 15:10 rather than 14:15?** Those exact endpoints are not verified. Available evidence confirms nonmatching office Wi-Fi blocked earlier observed inside GPS; approved arrival was saved at 15:06:01.320 and correctly preserved through GPS confirmation. The unobserved 14:15 period and reported 15:10 screen remain unresolved.
2. **Why Saturday 17:30 rather than 18:30?** Unknown: no Saturday authoritative records in the accessed DB. Original-absence checkout classification is a code-supported possibility, not a finding about this incident.
3. **Exact Saturday break duration?** Unknown; no pair retrieved.
4. **Did break cause checkout discrepancy?** Unverified; an unrecognized return can cause classification of original break exit, but evidence is missing.
5. **Which fixes are required?** Verify legitimate router inventory/cached config, reproduce and resolve summary timestamp inconsistency, and obtain Saturday evidence before selecting attendance-decision changes. No fixes implemented.
6. **Evidence before correction?** Exact deployment/DB identity; installed app build; original screen/export and event UUID; Saturday Room events/anchors/state/diagnostics; router approval history; CRM historical policies and shift/eligibility; backend rejection/audit/regularization logs; independent arrival/departure corroboration.

## 10. Handover checkpoint — resume here

**Completed:** Git/workspace inventory; both repositories located and source paths traced; backend/database read access verified; diagnostics panel 200/auth API 401 checked; actual User 7 records and 166 diagnostics saved; Monday mismatch and proof preservation established; Saturday absence checked in raw/effective/master/diagnostic/regularization sources; current policy inspected; 14 tests passed; report/evidence saved. Source and DB unchanged.

**Pending and exact next steps, in order:**

1. Confirm the employee's installed APK base URL/build and the Vercel deployment's database identity against the locally configured backend. Do not dump environment values. Obtain screenshot/export identifying the screen, date and event UUID behind 15:10/17:30.
2. Obtain a read-only phone export through approved support tooling: `attendance_events`, referenced time anchors, arrival candidates, persisted engine state and diagnostics for both days. Do not clear app data, restart monitoring for evidence creation, or trigger attendance. ADB was not on PATH in this session; no device inspected.
3. Re-run the attached schema-based read-only SQL on the confirmed authoritative database. Saturday's exact pair/checkout timestamps take priority; preserve original offsets and types. Check rejection logs and legacy/regularized projections if raw rows remain absent.
4. Compare :39/:3A with the physical office router radio inventory and CRM change history. Establish whether/when :39 was legitimately approved and what the phone cached at 14:15.
5. Reconstruct Saturday by boot/elapsed time: first two valid outside fixes, original absence start, grace completion, any return/Wi-Fi match, gaps/restarts, checkout classification time, upload/rejection and summary projection. Compute break duration only after retrieving a pair.
6. Reproduce naive timestamp insertion under the deployed Node timezone/session configuration using SELECT-only casts first; compare summary endpoint payload with effective event. Local check did not reproduce the stored value.
7. Retrieve historical CRM eligibility/shift/office policy and audit data (actual shift schema was not found in the limited schema-name scan). Reassess fixes and audited regularization; request implementation authorization only after a concrete proposal.

Missing evidence is substantive; do not substitute current-source policy for the app installed on 31 October. Avoid broad repository exploration or further builds until the deployment/phone records resolve the evidence gap.

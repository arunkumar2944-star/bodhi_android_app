## 2.1.5 — 2026-10-19

- Sync Status now distinguishes queued/retryable/rejected and CRM-acknowledged Room records.
- Added network-constrained upload retry request and display refresh; requesting a retry does not mark any attendance as accepted.
- Added production-readiness and consolidated physical E2E checklist at `docs/production-readiness-checklist.md`.
- Automatic event creation and CRM-authoritative attendance rules unchanged.

## 2.1.4 — 2026-10-19

- Phase 1: departure confirmation now requires two separately observed accurate, unambiguous outside GPS fixes at least 15 seconds apart and no more than 90 seconds apart.
- Duplicate, very rapid, and reversed elapsed-time observations cannot establish a departure; interrupted observation gaps require a fresh pair.
- Existing CRM-defined grace/maximum absence, office Wi-Fi verification, Room evidence retention, and actual attendance event rules remain unchanged.
- Regression tests cover rapid fix repeats, time reversal, fresh confirmation and recovery.

## 2.1.3 — 2026-10-19

- Attendance History lists only records from the selected month, not all loaded months.
- Month chooser restricts future months and uses actual loaded record/calendar years rather than an arbitrary 90-calendar-day cutoff.
- Week strip prevents future-week navigation and future-date selection.
- Clarifies that unloaded/no-record months do not mean an employee was absent; monthly absence count remains unavailable until CRM supplies an absence roster.
- No attendance engine, security, or persistence changes.

## 2.1.2 — 2026-10-19

- Professional named CRM company holiday, unnamed company holiday, and Sunday weekly holiday notices in Attendance History and Dashboard.
- Existing attendance and approved On Duty remain authoritative over day-off notices.
- Uses backward-compatible CRM holiday detail metadata; no attendance-writing changes.

# Bodhi Attendance — Android release history

This file is kept in Git alongside the Android Studio project. The **source of truth for the build version** is `app/build.gradle.kts` under `defaultConfig`. Release-history entries reflect repository changes, not proof of a published/signed APK.

| Version | versionCode | Date (IST) | Status | Summary |
| --- | ---: | --- | --- | --- |
| 2.1.1 | 7 | 2026-10-19 | Prepared; E2E pending | Holiday-aware attendance history/dashboard, current-week default, matching On Duty quick access, clearer office policy health state |
| 2.1.0 | 6 | 2026-10-18 | Prepared; E2E pending | Device-bound JWT compatibility upgrade, approved-device activation/refresh handling, recovery security workflow and monitoring improvements from recent development |
| 2.0 | 5 | Before 2026-10-18 | Previous configuration | Historical version found in Gradle before this bump; exact distribution date and earlier feature list not established |

## 2.1.1 — 2026-10-19

- History calendar initially selects the CRM current day and week.
- CRM-confirmed holidays are displayed explicitly in details and Dashboard, including dates without attendance events.
- Dashboard On Duty quick access shares the bottom-navigation visibility rule.
- System Health distinguishes unknown office assignment from failed Wi-Fi/BLE, and marks office-specific checks not required on confirmed holidays.
- Companion backend change: CRM_TEST_Backend PR #19 provides company holiday dates in the mobile attendance read model.
- Tests and device validation remain required before distributing signed APK.

## 2.1.0 — 2026-10-18

**Build metadata:** `versionName = "2.1.0"`, `versionCode = 6`.

**Changes:**
- Detect locally when a stored Android JWT lacks current installation-binding claims.
- For approved installations, upgrade a legacy access JWT through backend-validated Android session activation when no refresh credential exists.
- Use refresh rotation when a stored refresh credential exists, rather than continuing to use an incompatible legacy token.
- Keep revoked-session and replaced-device enforcement on the server; do not clear app data to fix a token mismatch.
- Includes the recent device-replacement recovery UI and associated security-evidence updates already merged on the development branch.

**Verification at entry:** Earlier compatibility PR #15 passed GitHub Android build and departure engine regression tests. This version bump requires its own build check. Real-device E2E, release signing, publication and store distribution are **not yet confirmed**.

**Backend dependency:** Backend device-bound Android session enforcement in `CRM_TEST_Backend/main` and CRM `testing-branch`. Hosted deployment/version and end-to-end authorization must be verified separately.

## Release management policy

1. For every distributable Android build, increase `versionCode` **strictly above all previously distributed builds**. Never reuse a published code.
2. Update `versionName` to represent the release (e.g. `2.1.1`, `2.2.0`). A source-only experiment need not trigger a release.
3. Add a new entry to this file noting IST date, changes, backend compatibility, CI evidence, sign-off status, and migration notes. Preserve older entries.
4. Keep the release tag and Git SHA after publishing; use an annotated tag such as `android-v2.1.0` **only after** signing and release approval.
5. In Android Studio, view version metadata in `app/build.gradle.kts`; view historical release notes here. Git history and tags preserve the exact sources associated with each release.
6. Updating the version does **not** install an APK on devices or guarantee an upgrade: signing certificate and application ID must remain unchanged for an in-place upgrade. Never clear Room data as part of a normal update.

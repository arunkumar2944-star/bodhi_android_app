## 1.0.1 (versionCode 14) — 2026-10-22 — internal testing candidate

**Build metadata:** `versionName = "1.0.1"`, `versionCode = 14`. This is a candidate, not an approved published release. If any employee has already installed versionCode >=14, increment the code before distribution. Confirm signing-certificate continuity to update in place.

**Improvements since 1.0 (13):**
- Appearance setting: Light, Dark, and Follow Device with persisted preference; shared page surfaces, cards, text, navigation and contrast fixes.
- Global icon-only page-data refresh beside notifications. Dashboard and Attendance background display polling; refreshing cannot create, amend, or backdate attendance events.
- Requests screen contrast, Dashboard and Attendance monthly summary tile sizing and theme-aware borders/surfaces.
- Secure device-replacement flow: authenticated recovery-only requests, manager approval, Android Keystore challenge signature, emulator non-attested key compatibility, CRM-side proof verification, and final server-authorized activation. **Do not claim recovery E2E passed** until CRM accepts proof and returns RECOVERY_COMPLETED.
- Sanjai departure regression: preserve outside candidate evidence when GPS accuracy is poor, expedite recheck without inventing departure events.

**Compatibility and outstanding validation:**
- Backend must have a stable `DEVICE_FINGERPRINT_SECRET` (>=32 characters), recovery endpoints, approved manager decision, and fresh post-approval key proof. No automatic bypass of employee/device binding.
- Validate in-place upgrade without uninstall or data reset, keep installation UUID and offline attendance, and test emulator and physical-device recovery, attendance operations, signed APK and version metadata.
- The 2.x entries retained below describe earlier development milestones; they do not imply that 2.x was distributed. The actual Gradle version before this candidate was 1.0 (13).
- GitHub Actions and local/emulator QA for this version candidate must be recorded before production distribution.

---

## 2.1.6 — 2026-10-19

- Attendance History now uses an exact rolling 90-calendar-day display window (today plus 89 preceding days).
- Month picker, weekly navigation, individual date controls, records and monthly presence counts respect the same range.
- Months outside the range remain visible but disabled; valid months across year boundaries remain selectable.
- No CRM attendance event, Room data or backend changes.

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


## Android v1.0.1 (versionCode 14) — official release preparation, 2026-10-21

**Important:** Existing history above includes earlier *development* version naming (2.x). The local user-confirmed version for the next distributed APK is `1.0.1` / code `14`. Do not release until Gradle, prior distributed versionCode, APK metadata and signing certificate are reconciled and verified.

- Feature branch: `feature/android-v1.0.1-appearance-refresh`, PR #31.
- Implemented: Settings Appearance (Light/Dark/System) with persistent device preference; default remains Light on existing installations.
- Implemented: manual Refresh on authenticated screens; automatic 30-second CRM *display-only* refresh on Dashboard/Attendance while Activity is resumed.
- Implemented: retain last CRM display data on temporary fetch failures; do not manufacture attendance events.
- Known issue: Giri and Sanjay missing-break recovery is still unverified; neither refresh nor theme changes fix `BREAK_START` creation.
- Official employee-facing channel planned, but APK build, SHA-256, signing compatibility, employee upgrade and distribution are not yet verified.
- Planned Firebase integration: keep machine-readable release metadata at `docs/releases/firebase-update-manifest.v1.0.1.json`; no Firebase update implementation or forced-update behavior exists yet.
- Before publishing: run Gradle compile, unit and UI checks, sign official APK, confirm code 14 and cert fingerprint, record Git SHA, APK hash and release timestamp.

### Release-note text for future Firebase presentation

**What's new:** Choose your preferred Light, Dark or Follow device theme in Settings. Refresh CRM attendance information without restarting the app; Dashboard and Attendance also check for updated display data while open. Temporary network interruptions now retain the last received information.

**Note:** Refreshing does not create or correct attendance events. Automatic missing-break recovery remains under investigation.


### v1.0.1 dark-theme compatibility follow-up — pending build verification

- Reason: selecting Dark changed Material 3 colors, but shared launch background, bottom navigation and several screens retained hardcoded light backgrounds and text.
- Change in `fix/android-full-dark-theme`: use the dark Material background instead of the fixed launch graphic in Dark; make bottom navigation and multiple attendance, request, health and settings card surfaces theme-aware; improve text/status card contrast.
- Light continues to use the existing Bodhi launch background and established light palette.
- This is a UI-only remediation: it does not modify check-in, break, geofencing, or API confirmation behavior.
- Still required: Kotlin compile, visual audit of **every** screen and component in Light / Dark / Follow device, and employee upgrade verification; no APK or release approval yet.

### v1.0.1 appearance follow-up: centralized UI palette (pending verification)

- Added `ui/theme/BodhiPalette.kt` for reusable Material semantic colors; Light/Dark/System preference remains controlled centrally by `MainActivity.kt` and `AppearancePreference.kt`.
- Updated authenticated app background, shared navigation, status-card surface/gradient, Today/History views, and Dashboard holiday colors to respond to the selected theme.
- Prior light branding remains the target when Light is selected. Dark now uses theme surfaces instead of the fixed light launch image in the authenticated shell.
- **Not a verified full-screen migration yet**: visual audit and replacement of remaining literal light colors across screens are required; compiling and emulator/phone testing are pending.
- The theme switch must not change device authorization, attendance evidence, monitoring, or CRM synchronization.

### v1.0.1 broad theme migration for MD demo — verification pending

- Updated shared authenticated shell, bottom navigation, Dashboard/summary cards, Attendance History/calendar/detail UI, Requests and its child forms, System Health, Settings/device pages, login/onboarding, and app-lock presentation to use Material semantic colors.
- Settings appearance preference remains the single selection source for LIGHT, DARK and SYSTEM; previously installed users retain Light unless changed.
- No attendance event engine, CRM mutation, registration, geofence, or device-security policy changes were made for this appearance update.
- **CI build and end-to-end emulator review must pass before marking the migration complete.** Verify text contrast and status meaning in all three modes. Firebase update publication and official APK signing are not included in this change.

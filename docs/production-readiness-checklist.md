# Bodhi Attendance — implementation and production readiness

The app is automatic attendance; no manual check-in/out. CRM remains the attendance authority. None of these checks should create synthetic events.

## Implemented safeguards reviewed (19 October 2026)
- Main Office requires GPS and CRM-approved office Wi-Fi; Knowledge Centre uses GPS-only. BLE is optional unless CRM explicitly requires it.
- Uncertain or inaccurate GPS evidence cannot create attendance. Outside departure requires a fresh confirmation pair, then the CRM grace policy applies.
- Office identity is resolved from scoped CRM / previously verified attendance context, not nearest-office guessing.
- Local Room events retain UUID, monotonic time and original anchor. Retry reuses the same identity and evidence; only server acknowledgment marks SYNCED. Server rejection is retained for review.
- Offline continuation requires a bounded, server-authorized work session, not an unconditional local fallback.
- Heartbeat absence denotes MONITORING_INTERRUPTED / HEARTBEAT_MISSING, not proof of force stop; preserve diagnostics and reconcile after relaunch.
- Holiday and approved On Duty handling use CRM policy; do not fabricate an employee absence on unloaded dates.

## Implementation checks before physical E2E
- [x] Attendance history month selection, future dates and loaded-data wording
- [x] Named holiday, unnamed holiday and Sunday presentation
- [x] Departure confirmation spacing (15–90 seconds between two outside fixes)
- [x] Network-constrained background synchronization infrastructure
- [x] Sync Status retry UI distinguishes requested upload from confirmed delivery
- [ ] Verify installed APK signing identity matches previous release
- [ ] Verify hosted API deploy commit matches mobile read model
- [ ] Verify privacy policy, foreground service notice and app-store permission disclosures with legal review

## Consolidated physical E2E (required before production claims)
1. Verify a signed update with no uninstall, Room wipe, device replacement or forced login.
2. Confirm GPS + office Wi-Fi arrival, checked-in state, and matching CRM accepted event.
3. Stay inside with noisy GPS; ensure uncertainty never writes BREAK_START.
4. Confirm real exit with two qualifying fixes and configured grace; verify original trusted event time.
5. Return during grace; ensure no break. Leave and return later; verify BREAK_START and BREAK_END.
6. Keep phone offline under a valid bounded permit; verify idempotent eventual delivery and rejected-event retention.
7. Force-stop or simulate lost connectivity; verify only monitoring gap evidence, not inferred departure or confirmed force-stop.
8. Check holiday/leave/Sunday and approved On Duty exceptions; verify System Health and Dashboard wording.
9. Check each UI screen for back navigation, loading/empty/error states, keyboard, accessibility and small-display clipping.
10. Verify Manager Diagnostics timestamps, company/user/device scoping and no duplicate event UUIDs.

No test above is considered passed until it has matching device logs plus CRM evidence. Retain the source logs and their timestamps in the test record.

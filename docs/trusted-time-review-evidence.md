# Trusted time and attendance review evidence

The Trusted Time detail screen previously contained prototype strings that always reported pending verification. Both health screens now read persisted server anchors for the current installation and boot. Attendance time is calculated from anchor server time plus monotonic elapsed time; the phone clock is shown separately. A difference exceeding 60 seconds produces a clock mismatch warning without replacing the trusted reference or changing accepted attendance times. Reboot, missing persistence, malformed time or elapsed regression makes the reference unavailable.

Diagnostic transitions: TRUSTED_TIME_AVAILABLE, CLOCK_MISMATCH, TRUSTED_TIME_UNAVAILABLE. The usual diagnostic upload queue persists and retries these reports. ATTENDANCE_EVIDENCE captures GPS status, per-fix mock status, accuracy, office, connected BSSID, configured-router match and trusted clock status, including unavailable GPS. Uncertain GPS remains uncertain even when office Wi-Fi matches. Mock locations remain blocked by the existing evaluator.

The test backend independently calculates diagnostic observation time from a stored server anchor with matching user, company, device and boot. Device time remains untrusted. Configured BSSID matching is supporting device-reported evidence, not proof of physical indoor presence. Regularization review receives evidence near the proposed time and retains the evidence and review reason in the approval audit. Main CRM frontend integration is separate.

## Validation

Four trusted clock JVM tests and 38 existing engine regression tests passed locally. Core evidence classes were syntax/type checked with dependency stubs; this is not a full Android build. GitHub APK workflow includes the new clock tests. Verify the APK on a physical phone: change wall time while keeping the app running, inspect both health screens and CLOCK_MISMATCH, restore automatic time and confirm recovery, test uncertain GPS with approved BSSID, test mock GPS, then reboot and verify fresh-anchor recovery. Do not clear historical evidence to perform these checks.

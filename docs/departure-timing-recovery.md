# Departure timing and monitoring recovery

An observation gap is uncertainty, not verified office presence. Preserve the existing Room AbsenceGrace/BreakActive timestamp while uncertain; do not emit an event. After a gap, require two fresh, accurate, unambiguous outside fixes no more than 90 seconds apart. Their confirmation permits the engine to use the retained original timestamp. Verified office presence still cancels grace or ends a break. Missing GPS alone never marks absence or checkout.

The service requests another bounded burst promptly after the first outside observation. Checkout no longer requires a separate, in-memory 90-minute uninterrupted GPS sequence in addition to the CRM maximum-absence policy. Current authentication, daily eligibility, CRM configuration and evidence checks still apply.

Office identity is recovered from authenticated bootstrap officeId, or the previously verified office saved for the exact employee/company/device/workday/boot. It chooses which active CRM office to evaluate; it is not presence proof. Removed/inactive configurations cannot authorize departure. No nearest-office guessing is used outside the fence.

At visible startup, a Room-restored departure phase may survive reconciliation only for the same boot/workday context and a compatible authoritative attendance state. Reboot, new workday, missing context, or contradictory server state never inherits an old timer. Every restart requires fresh outside confirmation. Historical timing unavailable after a reboot still needs management review.

## Validation

Regression tests cover retained grace after unknown evidence, original BREAK_START time, verified return cancellation, checkout blocked during uncertainty, fresh outside checkout confirmation, observation gaps/duplicates, and compatible/incompatible recovered phases. The APK workflow runs the focused JVM tests before assembling artifacts.

## Existing missed events

Updating does not manufacture events for an earlier session. Retain diagnostics and request audited attendance regularization through the CRM authority. The 15 October logs contain a first outside observation at 19:09:48 IST and confirmed grace at 19:11:14 IST; these are evidence timestamps, not an exact assertion of physical departure. Review office 2's real-world identity/configuration as well: later observations near office 2 do not establish uninterrupted absence from all approved offices.

## Device acceptance

Install an update with the same signing key without clearing data. Start inside a verified office, leave beyond its accuracy-adjusted fence and disconnect office Wi-Fi. Verify BREAK_START after the configured grace with separate effective and confirmation times. Repeat with a network/GPS gap and a service restart while outside. Confirm the original phase is retained, office is restored, unknown evidence emits no event, and fresh outside confirmation resumes evaluation. Verify a genuine return creates BREAK_END/WORKING and maximum absence creates only one CHECK_OUT. Holidays/leave/manager pause and manual next-workday opening remain unchanged.

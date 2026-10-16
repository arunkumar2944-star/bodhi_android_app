# Trusted time health and anchor lifecycle

## Clock mismatch is an action item

A valid server-backed anchor and a correct phone clock are different checks. The master System Health card previously mapped anchor availability directly to READY, even when the shared evidence already reported CLOCK_MISMATCH. Both screens now use the same health mapping: a difference larger than 60 seconds in either direction requires action. The summary counts this as action required. The user should enable automatic date and time, then return to verify recovery.

The master screen refreshes local evidence every second while STARTED and on resume, without repeating network health checks or requesting anchors. The detail screen also stops its periodic polling when not STARTED. Health refresh cannot replace or erase the anchor. Existing diagnostic transition deduplication remains in place.

Attendance time availability is still reported separately. This health warning does not change the attendance engine's transition rules or make phone time authoritative.

## Anchors are not lifetime device identifiers

The installation device UUID is separate from each time-anchor UUID. `ensureCurrentAnchor` reuses an active anchor only when its matching device/current-boot Room record exists, or restores a valid Room anchor. Reconciliation calls `createFreshAnchor`; normal daily app opening or monitoring reinitialisation can therefore create another server anchor, even during the same boot. A reboot requires a new current-boot reference before new monotonic events are trusted.

No calendar-day key in TimeAnchorManager resets an anchor based on the employee-controlled date. TimeAnchorViewModel also exposes an explicit fresh-anchor request. Historical anchors remain persisted because queued events refer to their original UUID. They must not be deleted merely because a new anchor exists.

The time-anchor API creates a server UUID and returns database-backed server time alongside the submitted boot and elapsed-time values. It does not accept phone wall time as the server reference. Attendance events are reconstructed from the stored anchor plus their monotonic delta, scoped to the registered user/device and boot. A routine phone clock edit therefore cannot become authoritative through a daily server refresh. This is the existing app/server trust model, not a guarantee against compromised devices or forged clients.

## Validation

Regression coverage includes ahead/behind offsets, restoration without replacing the reference, 60-second boundaries, absent/previous-boot references, calendar-day rollover and a server-anchor refresh while the phone date remains wrong. The actual health mapping is covered in the JVM workflow and Android unit-test job.

Physical-device checks after installing the updated APK:

1. Open System Health with automatic time enabled. Confirm the master and detail clocks are ready.
2. Disable automatic time and move the clock two hours forward, then backward. Confirm action required in both screens, a corresponding signed clock difference, protected trusted time continuing normally and CLOCK_MISMATCH diagnostics.
3. Return to automatic time. Confirm recovery without clearing app data or changing the current anchor through Health refresh.
4. Reboot. Confirm the old boot reference is not used for new events; allow startup/server reconciliation to establish a new reference.
5. Confirm queued attendance events retain their original anchor IDs after a later server refresh.

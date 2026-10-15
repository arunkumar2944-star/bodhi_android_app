# Verified Wi-Fi arrival and employee event history

Apply migration `020_verified_wifi_arrival.sql` in Neon after migrations 018 and 019, deploy CRM_TEST_Backend, then update Android on `android-ui-development`. Room upgrades through version 11 without clearing attendance data.

At a Wi-Fi-required office, the registered Android app persists an approved-BSSID arrival attempt against a server time anchor. While waiting for accurate GPS it samples continuity on bounded verification cycles. An explicit Wi-Fi disconnect, router/office change, device reboot, monitoring-service restart or an observation gap over 180 seconds invalidates the attempt. Reconnection starts a new UUID. These conservative rules can discard an early arrival when continuity cannot be established; they never assume presence across an unknown gap. GPS-only offices keep their existing behavior.

When GPS confirms the same office and approved router, CHECK_IN retains its GPS confirmation monotonic time and carries the persisted original arrival proof. Room stores the effective arrival timestamp for employee display. The server recomputes both GPS/router policy and arrival time using the server-issued anchor, checks identity/device/boot, same IST date, ordering and replay protection, then records the arrival and GPS confirmation in the same attendance transaction. The effective event time and attendance summary login time use the approved Wi-Fi arrival time. Example: Wi-Fi at 09:00, GPS confirmation at 09:30 → CHECK_IN effective 09:00; `presence_verified_at` 09:30; `server_received_at` shows upload time. No existing historical attendance is rewritten.

Continuity is registered-client evidence, not independent router attestation. A modified client can fabricate sensor claims, just as with existing GPS/BSSID evidence. This feature does not claim independent physical room verification.

An arrival that never receives GPS confirmation remains review evidence and does not create CHECK_IN. A disconnected attempt cannot be selected by the updated Android code. Pre-upgrade daily arrival records are ineligible for automatic backdating. Event retries preserve the exact proof and UUID in Room; they do not select a new arrival. Candidate verification and attendance insertion roll back together on failure.

Android Settings → Event Log shows only Checked in, Break started, Break ended and Checked out, original trusted effective time in IST, short reason and a simple pending/review indication where needed. UUIDs, upload counters, sensor evidence and diagnostic details are hidden. Older events reconstruct time from their saved anchor rather than showing the untrusted phone wall clock.

The detailed read API now requires Admin or Manager and scopes queries to the caller's company. Employee/Intern/Team Lead cannot read diagnostic details. Registered employees can still upload their own diagnostics. Pagination and an optional `employeeId` filter are supported.

## Diagnostics panel

Open `/api/v1/attendance/diagnostics/panel` on the CRM test backend. Sign in with an Admin/Manager account. This sign-in reuses credential validation and rate limiting but explicitly does not create legacy attendance. The token is memory-only and cleared on sign-out/refresh. Data is never rendered as HTML. The panel shows employee, phone time, server receipt time, category, reason, boot, elapsed time, device and log UUID. Use the employee ID filter and paging controls to trace a specific person. The existing React navigation is not modified: its repository is unavailable to this connection. This backend-hosted panel can be linked from that navigation later.

## Verification

`node --test tests/diagnosticValidation.test.js tests/attendanceDiagnosticController.test.js tests/verifiedWifiArrival.test.js`

Run the Android build and unit tests. Phone acceptance tests:

1. Connect to an approved router while GPS is inaccurate; confirm an arrival-pending diagnostic exists in CRM but no CHECK_IN yet.
2. Keep Wi-Fi connected and let GPS confirm; compare effective check-in against first Wi-Fi time, GPS confirmation and server receipt time.
3. Disconnect before confirmation, reconnect and verify the first attempt is not used.
4. Repeat after a service restart/reboot; old pending attempts must not be reused.
5. Repeat offline and reconnect; the same attendance UUID/proof must sync once.
6. Verify Knowledge Centre remains GPS-only.
7. Confirm employee Event Log has only attendance steps; confirm Employee login cannot open detailed CRM data and Manager queries stay company scoped.

Local checks cover backend validation/access rules and SQLite migration preservation. Android compilation and live PostgreSQL/phone integration still require their respective environments.

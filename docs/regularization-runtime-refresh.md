# Recover a running Android session after CRM regularization

An approved 15 October checkout closes that attendance day. If the running Android service retained `WORKING` and 16 October has no check-in, matching GPS and Wi-Fi alone previously produced `Employee remains present` forever.

Every online verification burst now compares the current CRM daily state with the runtime. A mismatch serializes reconciliation with sensor evaluation, uploads saved events using their original identities, and waits until no PENDING, FAILED_RETRYABLE or SYNCING rows remain for the current user/device. Authoritative bootstrap then determines the actual state. An unresolved prior-day session is not reset just because today's daily read reports NOT_CHECKED_IN.

When bootstrap confirms NOT_CHECKED_IN, the next fresh presence evidence follows the normal office policy and creates today's check-in. Reconciliation itself creates no event and does not invent an earlier check-in time. Existing arrival evidence retains its separate review policy. A server-confirmed checkout still stops tracking. Offline authorization continues using the existing local state until CRM becomes reachable.

Opening the app while monitoring is already running requests an immediate verification burst instead of silently skipping state refresh. No automatic next-morning restart was added.

Diagnostics: `STATE_RECONCILIATION`, `STATE_RECONCILED`, and `STATE_RECONCILIATION_PENDING`. Expect `workDate=2026-10-16; state=NOT_CHECKED_IN`, then normal fresh GPS/Wi-Fi verification and `EVENT_SAVED` for CHECK_IN. Verify acceptance in CRM; state refresh alone is not evidence of an accepted check-in.

Release version remains 1.1, versionCode increases to 3. No database migration is introduced.

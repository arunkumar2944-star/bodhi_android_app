# GPS-only arrivals when Wi-Fi is unavailable

Requires the test backend provisional-arrival endpoint and migration `026_provisional_arrivals.sql` in CRM_TEST_Backend. Room migration 12 → 13 adds an independent evidence outbox and preserves attendance records.

Fresh non-mock GPS must meet CRM accuracy and fit its complete accuracy circle inside one office. Wi-Fi must be disconnected, the office must require Wi-Fi with BLE not required, and attendance must be NOT_CHECKED_IN. A valid already authorized work session and original same-boot server anchor are mandatory. Phone-clock mismatch does not alter trusted arrival time.

The first arrival is retained per employee/company/device/office/trusted IST day. It is not an attendance event. Network retries upload the original evidence to create a pending regularization request. Manager approval uses the original trusted timestamp; rejection leaves attendance unchanged. A later normal check-in prevents a second check-in from provisional approval.

Settings → Offline Data shows saved arrivals. After upload, Requests shows the manager's decision. Validation conflicts keep the local evidence and instruct manual regularization. Network/server failures retry; clearing display cache does not delete the outbox.

Start each workday online. Without prior authorization or after reboot without a new validated anchor, an offline arrival cannot be assigned a trusted time. Wi-Fi loss is recorded as cause unverified, never as proof of a power cut.

Test an upgraded installation, offline capture with the phone clock one hour behind, repeated fixes/restart, reconnect/upload, approval/rejection, and a later normal check-in before approval. Mock, stale, poor-accuracy or boundary-uncertain GPS must not save a provisional arrival. The backend deployment document contains the full acceptance sequence.

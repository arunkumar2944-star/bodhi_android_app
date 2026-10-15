# Office return and notification read state

Apply `022_attendance_notification_reads.sql`, deploy backend main, then update Android from android-ui-development without clearing app data.

An ON_BREAK employee may return at a different active office within the same company. Android requires an unambiguous accurate GPS fix plus the return office's configured Wi-Fi/BLE policy. The backend verifies the return against current company configuration. It inserts BREAK_END and projects the master attendance_state=WORKING and attendance_location_id to the verified return office in the same transaction. Future departure checks use that new office. A WORKING employee cannot create duplicate BREAK_END, and CHECKED_OUT remains closed. Unknown/unconfigured locations and inaccurate evidence do not authorize a return. No historical rows are manually rewritten.

Android refreshes CRM display data after local attendance rows change, including after sync acknowledgement, so the dashboard reflects the accepted master/event state. It does not claim cloud acceptance while an event is pending upload.

Notifications now support authenticated GET /api/v1/attendance/mobile/notifications/:id and PATCH /api/v1/attendance/mobile/notifications/:id/read. Read records are durable, idempotent and scoped by user and company. Only reviewed request decisions are visible. GET /notifications includes per-row readAt and an unreadCount separate from the limited result list. Android cards navigate to a separate detail screen, load details, acknowledge read status, and update shared list/bell state. Returning to Unread hides the viewed item; Read keeps it available as history. Merely opening the notification list does not mark everything read. Failed detail/read requests retain unread state and offer retry.

Tests: node --test tests/returnAndNotifications.test.js (plus existing diagnostic/arrival regression tests).

Phone checks: start ON_BREAK at the home test office; return to the real configured office with approved Wi-Fi and accurate GPS; verify one BREAK_END, master WORKING, new office binding, and dashboard refresh. Open an unread notification; verify full decision details, count reduction, removal from Unread, presence in Read, and persistence after app restart. Repeat notification detail on a different account to verify isolation.

Android compilation and live PostgreSQL/phone acceptance must be checked in their actual environments. GitHub SDK 37 availability previously blocked compilation.

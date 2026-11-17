# Android Firebase Cloud Messaging Integration Report

---

## 1. Implementation Architecture

* **Service Class**: [`BodhiFirebaseMessagingService.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/core/messaging/BodhiFirebaseMessagingService.kt)
* **Notification Channel**: `bodhi_push_notifications` ("Attendance Alerts")
* **Token Registration Endpoint**: `POST /api/v1/attendance/notifications/fcm-token`
* **Security Guard**: Firebase Admin credentials are kept exclusively on the server (`FIREBASE_ADMIN_CREDENTIALS`). The client app never contains server keys.

---

## 2. Notification Flow & Deep Links

When a push message arrives:
1. `onMessageReceived` parses title and body text.
2. Posts a local notification to `bodhi_push_notifications` channel.
3. Tap action launches `MainActivity` with pending intent (`FLAG_ACTIVITY_NEW_TASK` / `FLAG_ACTIVITY_CLEAR_TOP`).

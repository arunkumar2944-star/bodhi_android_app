# Firebase Real Device & Token Verification Protocol

---

## 1. Firebase Messaging Architecture

* **Android Service**: [`BodhiFirebaseMessagingService.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/core/messaging/BodhiFirebaseMessagingService.kt)
* **Backend Token Endpoint**: `POST /api/v1/attendance/notifications/fcm-token`
* **Notification Channel**: `bodhi_push_notifications` ("Attendance Alerts")

---

## 2. Staging Device Verification Steps

1. Install `BodhiAttendance-Debug-1.0.7.apk` on physical Android test device (Android 12–16).
2. Log in with synthetic employee credentials. `BodhiFirebaseMessagingService.onNewToken` registers the device token with the backend.
3. Trigger a test event or missing check-in sweep.
4. Verify notification banner appears on physical device with deep link to attendance screen.

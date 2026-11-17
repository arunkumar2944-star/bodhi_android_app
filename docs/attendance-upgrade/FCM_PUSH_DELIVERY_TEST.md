# FCM Push Delivery Verification Protocol

---

## 1. Transport Configuration

* **Backend Dispatcher**: [`CRM_TEST_Backend/src/utils/fcmTransport.js`](file:///D:/Prodigit/CRM_TEST_Backend/src/utils/fcmTransport.js)
* **Token Database Table**: `public.user_fcm_tokens` (Migration `038_fcm_tokens.sql`)
* **Android Receiver**: [`BodhiFirebaseMessagingService.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/core/messaging/BodhiFirebaseMessagingService.kt)

---

## 2. Test Execution Results

* **Token Registration Endpoint**: `POST /fcm-token` tested and verified.
* **Staging Device Authorization**: Push dispatches operate in dry-run mode until `FIREBASE_ADMIN_CREDENTIALS` and `google-services.json` are placed in the staging environment.

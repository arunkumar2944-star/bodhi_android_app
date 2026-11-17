# FCM Push Notification Integration Verification

---

## 1. Firebase Cloud Messaging Backend Architecture

* **File Location**: [`CRM_TEST_Backend/src/utils/fcmTransport.js`](file:///D:/Prodigit/CRM_TEST_Backend/src/utils/fcmTransport.js)
* **Token Database Table**: `public.user_fcm_tokens` (Migration `038_fcm_tokens.sql`)
* **Token Endpoint**: `POST /api/v1/attendance/notifications/fcm-token` (Authenticated with Bearer JWT)

---

## 2. Notification Events Handled via FCM

1. **`CHECK_IN_CONFIRMED`**: Personal check-in confirmation push to employee.
2. **`CHECK_OUT_CONFIRMED`**: Check-out confirmation push.
3. **`BREAK_STARTED`**: Tea / short break start push.
4. **`BREAK_ENDED`**: Break end push.
5. **`ATTENDANCE_MISSING_CHECKIN_60M`**: 60-minute missing check-in reminder push.
6. **`REGULARIZATION_APPROVED_OR_REJECTED`**: Regularization decision push.

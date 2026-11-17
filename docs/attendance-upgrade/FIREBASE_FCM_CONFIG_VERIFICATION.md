# Firebase Cloud Messaging Configuration & Verification Report

**Application Package**: `com.prodigit.bodhiattendance`  
**Firebase Project ID**: `bodhi-attendance`  
**App Version**: 1.0.7 (versionCode 20)  
**Date**: November 2026  

---

## 1. Verification Checklist

1. **`google-services.json` Package Match**:
   * Verified `app/google-services.json` specifies `"package_name": "com.prodigit.bodhiattendance"`, matching `defaultConfig.applicationId` in `app/build.gradle.kts`.
2. **Google Services Gradle Plugin**:
   * Applied `id("com.google.gms.google-services")` in `app/build.gradle.kts` and `id("com.google.gms.google-services") version "4.4.2" apply false` in root `build.gradle.kts`.
3. **Firebase Messaging Dependencies**:
   * Added Firebase BOM `33.9.0` and `com.google.firebase:firebase-messaging` to `app/build.gradle.kts`.
4. **Service Registration in `AndroidManifest.xml`**:
   * Registered `BodhiFirebaseMessagingService` with intent filter `com.google.firebase.MESSAGING_EVENT`.
5. **FCM Token Registration Flow**:
   * On Token Refresh: `BodhiFirebaseMessagingService.onNewToken(token)` registers the token with backend `POST /api/v1/attendance/notifications/fcm-token`.
   * On User Login: `LoginViewModel` invokes `BodhiFirebaseMessagingService.fetchAndRegisterFcmToken(getApplication())` after `sessionManager.saveSession(...)`.
6. **Notification Permission Handling**:
   * Added `POST_NOTIFICATIONS` runtime permission check to `PermissionsScreen.kt` for Android 13+ (API 33+).
7. **Build & Test Results**:
   * `app:testDebugUnitTest`: 137 Passed, 0 Failed.
   * `app:lintDebug`: 0 Errors.
   * `app:assembleDebug`, `app:assembleRelease`, `app:bundleRelease`: BUILD SUCCESSFUL.

---

## 2. Next Real-Device Testing Steps

1. Install `BodhiAttendance-Debug-1.0.7.apk` on a physical test device running Android 13–16.
2. Grant Location and Notification permissions.
3. Log in with employee credentials. Check `logcat` for `BODHI_FCM: FCM token registered with backend successfully`.
4. Trigger a test notification or outbox sweep. Verify notification banner appears on device and opens `MainActivity` when tapped.

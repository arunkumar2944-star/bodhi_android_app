# Google Play Store Production Blockers & Release Readiness

**Application**: Bodhi Attendance (`com.prodigit.bodhiattendance`)  
**Version**: 1.0.6 (versionCode 19)  
**Date**: November 2026  

---

## 1. Release Status: **CONDITIONAL GO**

There are **0 Critical** and **0 High** technical security vulnerabilities blocking release. The application compiles cleanly, passes 123 unit tests, has 0 Lint errors, and target SDK 36.

---

## 2. Play Console Administrative Blockers (P0)

### Blocker 1: Location Permissions Declaration Form (`ACCESS_BACKGROUND_LOCATION`)
* **Impact**: Google Play rejection during app review if the Background Location declaration is omitted.
* **Required Action**:
  1. Complete the **Location Permissions Form** in Play Console under **App Content > Sensitive App Permissions**.
  2. Upload a short video demonstrating why background monitoring is required for automatic check-in.
  3. *Alternative (Fallback)*: If Play Console rejects background location, remove `<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />` from `AndroidManifest.xml`. `AttendancePresenceService` uses `FOREGROUND_SERVICE_LOCATION` with an ongoing notification, which Android classifies as foreground location access.

### Blocker 2: Play Console Data Safety Declaration
* **Impact**: Mandatory for all Google Play Store apps.
* **Required Action**: Declare collected data types:
  * **Location**: Precise & Approximate (Attendance verification inside office geofence; encrypted in transit).
  * **Device Identifiers**: Device ID (Installation binding; encrypted in transit).
  * **User Account Info**: User ID & Email (Account management; encrypted in transit).
  * **Diagnostics**: Local evidence logs (Troubleshooting & audit trail; encrypted in transit).

### Blocker 3: App Reviewer Test Credentials
* **Impact**: Reviewers cannot evaluate app functionality behind login gates.
* **Required Action**: Provide synthetic reviewer credentials in Play Console under **App Access**:
  * Email: `test.employee@prodigit.com`
  * Password: `Password123!`

---

## 3. Technical Hardening Action Items (P1 / P2)

* **P1**: Encrypt access tokens in `SessionManager` using KeyStore AES-256 GCM.
* **P2**: Configure ProGuard R8 rule `-assumenosideeffects class android.util.Log { ... }` to strip debug logcat output in release builds.

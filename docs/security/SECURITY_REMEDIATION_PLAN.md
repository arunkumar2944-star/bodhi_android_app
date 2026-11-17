# Security Remediation Plan & Prioritized Action Items

**Project**: Bodhi Attendance (`com.prodigit.bodhiattendance`)  
**Date**: November 2026  
**Auditor**: Senior Android Security Engineer & Google Play Compliance Auditor  

---

## 1. Prioritized Action Items

### P0 Blockers (Must resolve before Google Play Production Release)

#### Action Item 1: Complete Google Play Background Location Declaration Form
* **Finding ID**: `FINDING-MASVS-PRIVACY-1`
* **Target Component**: Google Play Console
* **Description**: Google Play policies require an explicit declaration form, video demonstration, and prominent in-app disclosure for `ACCESS_BACKGROUND_LOCATION`.
* **Remediation Steps**:
  1. Record a short video showing background monitoring notification during an active work session.
  2. Complete the **Location Permissions Form** in Play Console under App Content.
  3. Verify that `PermissionsScreen.kt` prominent disclosure is shown prior to requesting runtime background location.
* **Owner**: Mobile Release Lead & QA Lead
* **Target Date**: Pre-launch

---

### P1 High Priority (Implement before Public Distribution)

#### Action Item 2: Encrypt Access Tokens in SharedPreferences
* **Finding ID**: `FINDING-MASVS-STORAGE-1`
* **Target File**: [`app/src/main/java/com/prodigit/bodhiattendance/core/session/SessionManager.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/core/session/SessionManager.kt)
* **Description**: Access tokens are currently saved in standard SharedPreferences (`bodhi_attendance_session`). While short-lived, storing access tokens using encrypted storage hardens local security.
* **Remediation Steps**:
  1. Wrap `SessionManager` access token preferences using `EncryptedSharedPreferences` or KeyStore AES/GCM.
  2. Clear old unencrypted keys on session migration.
* **Owner**: Android Security Engineer
* **Target Date**: Next release cycle

---

### P2 Medium / Low Priority (Hardening & Maintenance)

#### Action Item 3: ProGuard / R8 Log Stripping for Release Builds
* **Finding ID**: `FINDING-MASVS-CODE-2`
* **Target File**: `app/proguard-rules.pro`
* **Description**: Strip non-essential `Log.i` debug logs in release builds.
* **Remediation Steps**:
  Add R8 log stripping rule to `proguard-rules.pro`:
  ```proguard
  -assumenosideeffects class android.util.Log {
      public static int d(...);
      public static int v(...);
  }
  ```
* **Owner**: Android Build Engineer
* **Target Date**: Next release cycle

#### Action Item 4: Dependency Version Catalog Upgrades
* **Finding ID**: `FINDING-DEPENDENCY-1`
* **Target File**: `gradle/libs.versions.toml`
* **Description**: Upgrade libraries (e.g., Room 2.8.4 $\rightarrow$ 2.8.5, Activity Compose 1.11.0 $\rightarrow$ 1.13.0) during routine maintenance cycles.
* **Owner**: Maintenance Engineer
* **Target Date**: Post-launch maintenance

---

## 2. Verification & Validation Protocol

For each remediated item:
1. Apply the code change on a feature branch.
2. Run unit tests (`./gradlew testDebugUnitTest`).
3. Run Android Lint (`./gradlew lintDebug`).
4. Build release variant (`./gradlew assembleRelease`).
5. Perform regression testing on a physical test device.

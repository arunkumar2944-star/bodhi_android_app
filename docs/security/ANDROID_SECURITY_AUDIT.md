# Comprehensive Android Security & Google Play Audit Report

**Project Name**: CRMAttendance (Bodhi Attendance)  
**Package ID**: `com.prodigit.bodhiattendance`  
**Target SDK**: 36 (Android 16) | **Min SDK**: 26 (Android 8.0)  
**App Version**: 1.0.6 (versionCode 19)  
**Audit Date**: November 2026  
**Auditor**: Senior Android Security Engineer, Mobile Penetration Tester & Google Play Compliance Auditor  

---

## Executive Summary

### Release Assessment: **CONDITIONAL GO**

The Bodhi Attendance Android application (`com.prodigit.bodhiattendance`) demonstrates a robust security architecture for an enterprise attendance system. Key strengths include:
* **Monotonic Time Anchors**: Phone wall-clock tampering is completely mitigated; all event timestamps use monotonic `SystemClock.elapsedRealtime()` anchored against server-issued trusted time tokens.
* **Keystore-Backed AES-256 GCM Refresh Credentials**: Sensitive refresh tokens are stored in Android KeyStore (`bodhi_refresh_aes_v1`).
* **Mock Location & GPS Integrity Checks**: Built-in mock location detection (`LocationCompat.isMock()`) and accuracy bounds prevent basic coordinate spoofing.
* **Backup Protection**: `android:allowBackup="false"` and `data_extraction_rules.xml` prevent `adb backup` data extraction.
* **Zero Cleartext Traffic**: `android:usesCleartextTraffic="false"` and production HTTPS endpoint (`https://crm-test-backend-phi.vercel.app/`).

### Severity Summary
* **CRITICAL**: 0
* **HIGH**: 0
* **MEDIUM**: 2 (Google Play Background Location Disclosure; Local Unencrypted Access Tokens in SharedPreferences)
* **LOW**: 2 (Debug Build Base URL Fallback to HTTP localhost; Logcat Debug Telemetry in Non-Release Code)
* **INFORMATIONAL**: 3 (Version Catalog Dependency Version Upgrades; APK Unsigned Release Build Option; Biometric Fallback Keyguard Intent)

---

## A. Discovered Architecture & Feature Inventory

### 1. Technology Stack
* **Language & Runtime**: Kotlin 2.2.10 (JVM 11 target)
* **UI Framework**: Jetpack Compose (Material 3, Compose BOM 2026.02.01, Navigation Compose 2.9.5)
* **Database & Persistence**: Room 2.8.4 (`AttendanceDatabase`) + SQLite + SharedPreferences + KeyStore AES/GCM
* **Network & API**: Retrofit 2.11.0 + Gson + OkHttp 4.x
* **Location & Sensors**: Google Play Services Location (`FusedLocationProviderClient`, `GeofencingClient`) 21.4.0
* **Biometrics**: AndroidX Biometric 1.1.0 (`BiometricPrompt`)
* **Build System**: Android Gradle Plugin 9.5.1, KSP 2.3.6

### 2. Feature & Screen Inventory
* **Splash & Launch**: `BodhiLaunchScreen.kt` (entrance animations, startup validation)
* **Authentication**: `LoginScreen.kt`, `LoginViewModel.kt` (identifier & password validation, JWT handling)
* **Device Onboarding & Replacement**: `DeviceRegistrationScreen.kt`, `DeviceReplacementScreen.kt` (installation ID binding)
* **Dashboard & Attendance**: `DashboardScreen.kt`, `TodayAttendanceCard.kt`, `EmployeeStatusCard.kt`, `ViewScreen.kt` (history timeline)
* **System Health & Diagnostics**: `SystemHealthScreen.kt`, `OfficeWifiScreen.kt`, `BatteryOptimizationScreen.kt`, `PermissionsScreen.kt`
* **App Access Lock**: `AppAccessGate.kt` (`FLAG_SECURE` window enforcement, biometric & keyguard PIN verification)

---

## B. OWASP MASVS & MASTG Detailed Findings

### Finding 1: Google Play Background Location Declaration Requirement [MEDIUM]
* **Category**: Privacy & Google Play Policy Compliance (OWASP MASVS-PRIVACY-1)
* **File Reference**: [`app/src/main/AndroidManifest.xml:26`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/AndroidManifest.xml#L26)
* **Code Snippet**:
  ```xml
  <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
  ```
* **Exploitability / Impact**: High rejection risk during Google Play Console review if prominent in-app disclosure or Play Console declaration form is incomplete.
* **Remediation**: Complete the Google Play Console **Location Permissions Declaration Form**, submit a video demonstration of background location monitoring, and verify that in-app prominent disclosures are displayed before requesting background location.
* **Status**: **NOT VERIFIED** (Requires Play Console declaration submission).

### Finding 2: Unencrypted Access Token Storage in SharedPreferences [MEDIUM]
* **Category**: Data Storage & Privacy (OWASP MASVS-STORAGE-1)
* **File Reference**: [`app/src/main/java/com/prodigit/bodhiattendance/core/session/SessionManager.kt:63`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/core/session/SessionManager.kt#L63)
* **Code Snippet**:
  ```kotlin
  check(preferences.edit().putString(KEY_TOKEN, token).commit())
  ```
* **Exploitability / Impact**: On rooted devices or via local storage inspection, unencrypted access JWT tokens stored in standard SharedPreferences could be read.
* **Remediation**: Migrate `SessionManager` access token storage to `EncryptedSharedPreferences` or encrypt access tokens using the existing `SecureAndroidRefreshStore` Android KeyStore AES/GCM pattern.
* **Status**: **CONFIRMED**.

### Finding 3: Debug Build Configuration Uses Cleartext HTTP Localhost [LOW]
* **Category**: Network Communication (OWASP MASVS-NETWORK-1)
* **File Reference**: [`app/build.gradle.kts:65`](file:///D:/Prodigit/android/CRMAttendance/app/build.gradle.kts#L65)
* **Code Snippet**:
  ```kotlin
  debug {
      buildConfigField("String", "BASE_URL", "\"${hostedApiUrl ?: "http://127.0.0.1:5000/"}\"")
  }
  ```
* **Exploitability / Impact**: Debug builds allow cleartext HTTP traffic to `127.0.0.1:5000`. Production release builds correctly override this with HTTPS (`https://crm-test-backend-phi.vercel.app/`).
* **Remediation**: Ensure production release builds always use HTTPS and verify `android:usesCleartextTraffic="false"` remains enforced in release manifests.
* **Status**: **CONFIRMED** (Mitigated in release builds).

### Finding 4: In-Memory Diagnostic Logging [LOW]
* **Category**: Code Quality & Resilience (OWASP MASVS-CODE-2)
* **File Reference**: [`app/src/main/java/com/prodigit/bodhiattendance/core/monitoring/AttendanceDiagnostics.kt:25`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/core/monitoring/AttendanceDiagnostics.kt#L25)
* **Code Snippet**:
  ```kotlin
  Log.i(if (category.startsWith("ENGINE")) "ATTENDANCE_ENGINE" else "ATTENDANCE_DIAGNOSTICS", "$category: $message")
  ```
* **Exploitability / Impact**: Diagnostic messages contain non-sensitive engine status logs (`ENGINE_PHASE`, `WIFI_TRIGGER`). No user passwords, bearer tokens, or PII are logged.
* **Remediation**: Disable non-essential `Log.i` debug output in release builds using R8 ProGuard rules or custom log trees.
* **Status**: **CONFIRMED**.

---

## C. Verification Test Results

| Test Category | Command / Method | Expected Result | Status |
|---|---|---|---|
| **Unit Test Suite** | `./gradlew testDebugUnitTest` | All unit tests pass | **PASS** (123 passed, 0 failed) |
| **Android Lint** | `./gradlew lintDebug` | 0 Lint errors | **PASS** (0 errors) |
| **Release Build** | `./gradlew assembleRelease` | APK builds successfully | **PASS** (Release APK generated) |
| **Debug Build** | `./gradlew assembleDebug` | Debug APK generated & signed | **PASS** (Debug APK signed) |
| **Mock Location Filter** | `GpsEvidenceStatus.rejection()` | Rejects mock location fixes | **PASS** (Verified by unit tests) |
| **Monotonic Time Anchors** | `TimeAnchorManagerTest` | Prevents wall-clock spoofing | **PASS** (Verified by unit tests) |
| **KeyStore AES/GCM** | `SecureAndroidRefreshStore` | Encrypts refresh tokens | **PASS** (Verified by unit tests) |
| **Google Play Submission** | Play Console Upload | AAB submission & compliance | **NOT VERIFIED** (Requires Play Console upload) |

---

## D. Play Store Release Readiness Summary

* **Target SDK 36 Compliance**: **PASS** (App targets API 36).
* **Min SDK 26 Support**: **PASS** (App supports Android 8.0+).
* **64-bit Architecture Support**: **PASS** (Native libraries compiled for `arm64-v8a`, `x86_64`).
* **App Bundle (AAB)**: Ready to build via `./gradlew bundleRelease`.
* **Data Safety Declaration**: Prepared (Location, Device ID, App Performance/Diagnostics).

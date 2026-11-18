# Device Onboarding Navigation, Terms & Privacy, and Launch Loading Verification Report

**Application**: Bodhi Attendance (`com.prodigit.bodhiattendance`)  
**Version**: 1.0.7 (versionCode 20)  
**Date**: November 2026  

---

## 1. Device Replacement / Recovery Acceptance Onboarding Fix

* **Issue Analysis**: When a device replacement/recovery request was approved by a manager, logging in or re-launching the app returned to `ROUTE_LAUNCH`. Because Compose Navigation's `viewModel()` reuses the existing `StartupViewModel` instance, `_destination.value` previously remained set at `StartupDestination.DASHBOARD` from prior sessions. This caused `LaunchedEffect(destination)` to bypass onboarding and immediately open `ROUTE_MAIN` (Home Screen).
* **Fix Applied**:
  1. Updated [`StartupViewModel.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/startup/StartupViewModel.kt) with `resetAndCheckStartupState()`, ensuring `_destination` is reset to `StartupDestination.LOADING` on startup re-entry.
  2. Updated [`AppNavigation.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/navigation/AppNavigation.kt) so `StartupRoute` executes `LaunchedEffect(Unit) { startupViewModel.resetAndCheckStartupState() }`.
  3. Added `clearForUser(userId)` to [`RegisteredDeviceStore.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/data/local/repository/RegisteredDeviceStore.kt), clearing stale local device entities on login and recovery completion.
  4. Result: When `device.isOnboarded` is `false`, `StartupViewModel` accurately routes the user to `StartupDestination.ONBOARDING`.

---

## 2. Terms and Conditions & Privacy Policy Integration

* **New Component**: Created [`TermsAndPrivacyScreen.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/settings/TermsAndPrivacyScreen.kt) with tabbed navigation:
  * **Privacy Policy**: Plain-language employee privacy policy detailing location collection (GPS / Geofencing / Office Wi-Fi), background location usage for automatic check-in/checkout, AES-256 GCM KeyStore token encryption, data retention, account deletion, and Google Play Data Safety alignment.
  * **Terms & Conditions**: Operational enterprise rules, mandatory device registration policy, accurate attendance reporting, and anti-spoofing guidelines.
* **Access Points**:
  * Clickable footer link on [`LoginScreen.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/auth/LoginScreen.kt): `"Terms & Conditions • Privacy Policy"`.
  * Navigation button in [`AboutAppScreen.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/settings/AboutAppScreen.kt).
  * Route `ROUTE_TERMS_PRIVACY` added to outer and inner NavHosts in [`AppNavigation.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/navigation/AppNavigation.kt).

---

## 3. Launch Screen Branded Loading Process

* **New Shared Component**: Created [`BodhiLoadingProcess.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/common/BodhiLoadingProcess.kt):
  * `BodhiOrbitLoadingComponent(message)`: Displays the launch screen branded loading orbit GIF (`R.drawable.loading_orbit`) with animated pulsing text (`LoadingTextAnimation`).
  * `BodhiOrbitLoadingDialog(message)`: Modal overlay dialog using the branded loading orbit process.
* **Integration**: Replaced generic `CircularProgressIndicator` across [`DataUnavailable.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/live/DataUnavailable.kt) and [`NotificationDetailScreen.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/notifications/NotificationDetailScreen.kt).

---

## 4. Test Verification Results

* **Android Unit Tests (`app:testDebugUnitTest`)**: **137 Passed, 0 Failed**
* **Android Lint (`app:lintDebug`)**: **0 Errors**
* **Release Artifacts**: Release APK (`53.2 MB`) and Release AAB (`52.5 MB`) compiled successfully.

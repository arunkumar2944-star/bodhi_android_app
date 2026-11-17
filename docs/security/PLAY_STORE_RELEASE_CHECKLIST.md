# Google Play Store Release & Compliance Checklist

**Application Name**: Bodhi Attendance  
**Package Name**: `com.prodigit.bodhiattendance`  
**Version**: 1.0.6 (versionCode 19)  
**Target SDK**: 36 (Android 16) | **Min SDK**: 26 (Android 8.0)  
**Audit Date**: November 2026  

---

## 1. Technical & Build Requirements

- [x] **Target SDK Level**: App targets SDK 36 (meets Google Play requirement of target SDK 35/36 for 2026 submissions).
- [x] **64-bit Architecture Support**: Application supports `arm64-v8a` and `x86_64` native ABIs.
- [x] **Android App Bundle (AAB)**: Production releases will be built as AAB (`./gradlew bundleRelease`).
- [x] **Android Lint Clean**: 0 build-blocking errors (`./gradlew lintDebug`).
- [x] **Unit & Regression Testing**: 100% test pass rate (123 unit tests passed).
- [x] **Cleartext Traffic**: `android:usesCleartextTraffic="false"` in `AndroidManifest.xml`.
- [x] **Data Backup Security**: `android:allowBackup="false"` and `data_extraction_rules.xml` configured.

---

## 2. Permissions & Location Policy Checklist

- [x] **`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`**:
  * Used strictly for verifying presence at registered office geofences.
  * Prominent in-app disclosure displayed prior to runtime permission prompt.
- [ ] **`ACCESS_BACKGROUND_LOCATION`** (Requires Play Console Form):
  * **Play Console Action Required**: Complete the **Location Permissions Declaration Form** in Play Console.
  * Submit a video recording demonstrating why background monitoring is necessary for automatic check-in/checkout.
- [x] **`POST_NOTIFICATIONS`**: Requested at runtime on API 33+ for ongoing attendance service notifications.
- [x] **`BLUETOOTH_SCAN`**: Annotated with `android:usesPermissionFlags="neverForLocation"`.

---

## 3. Data Safety Form Inventory

Use the following inventory when completing the **Data Safety** section in Google Play Console:

| Data Type | Collected / Shared | Ephemeral / Stored | Purpose | Encrypted in Transit |
|---|---|---|---|---|
| **Precise Location** | Collected | Stored locally in Room DB & uploaded to CRM server | App functionality (Attendance verification inside office geofence) | Yes (HTTPS) |
| **Approximate Location** | Collected | Stored locally | App functionality | Yes (HTTPS) |
| **Device Identifiers** | Collected | Stored in KeyStore / SharedPreferences | Account management & device registration binding | Yes (HTTPS) |
| **User Identifiers (User ID, Email)** | Collected | Stored in SharedPreferences | Authentication & account management | Yes (HTTPS) |
| **App Performance & Diagnostics** | Collected | Stored locally in Room DB & uploaded | Analytics & troubleshooting | Yes (HTTPS) |

---

## 4. App Access & Reviewer Credentials

For Google Play App Reviewers:
* **Test Account Email / Mobile**: Provide synthetic test user credentials (e.g., `test.employee@prodigit.com` / `Password123!`).
* **Test Office BSSID / Geofence**: Provide mock location bypass instructions or staging office coordinates if required.
* **Biometric & PIN Lock**: The app access gate allows falling back to device PIN/Pattern.

---

## 5. Play Console Pre-Launch Verification Matrix

| Checklist Item | Verification Method | Status |
|---|---|---|
| **AAB Upload** | Upload `.aab` to Play Console internal test track | Pending Play Console Upload |
| **App Signing** | Play App Signing configured with production release key | Pending Release Key Signing |
| **Privacy Policy URL** | Host accessible HTTPS privacy policy URL | Pending URL Verification |
| **Content Rating** | Complete IARC Rating Questionnaire | Pending Questionnaire |
| **Target Audience** | Set age rating to 18+ (Enterprise / Employee app) | Pending Selection |

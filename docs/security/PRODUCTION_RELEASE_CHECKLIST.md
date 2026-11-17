# Production Release Verification Checklist

**App**: Bodhi Attendance (`com.prodigit.bodhiattendance`)  
**Version**: 1.0.6 (versionCode 19)  
**Target SDK**: 36 (Android 16) | **Min SDK**: 26 (Android 8.0)  

---

## 1. Verified Release Build Artifacts

### AAB Release Artifact (Google Play Production Upload)
* **File Location**: `D:\Prodigit\android\CRMAttendance\app\build\outputs\bundle\release\app-release.aab`
* **File Size**: `52,327,074 bytes` (~49.9 MB)
* **SHA-256 Checksum**: `71355ACF4F9358009E4D245F57A493AB844569651CF0EC88F2737AA73D739F59`
* **Build Variant**: `release`
* **Version Name**: `1.0.6` | **Version Code**: `19`

### APK Release Handoff Artifacts
* **Debug Variant APK (Signed for Internal Testing)**:
  * Path: `D:\Prodigit\android\CRMAttendance\release_handoff\BodhiAttendance-Debug-1.0.6.apk`
  * SHA-256: `D09E9AA3373B94FADFB0893C7D3AC7FF64B30FAEDBE0014A3F985B1C1FE356FF`
  * Size: `67,248,323 bytes` (~64.1 MB)
  * Signature: Signed with Android Debug Signer (`E25058B4EBF5499775F64E05F3F3C47333A2885C59C6E635690C2FC89FC22A41`)

* **Release Variant APK (Unsigned)**:
  * Path: `D:\Prodigit\android\CRMAttendance\release_handoff\BodhiAttendance-Release-1.0.6.apk`
  * SHA-256: `DE329E777B4EFB1230465BA9B7E91242B21E70358F4D3042BC3A4BE780C3FB29`
  * Size: `53,002,003 bytes` (~50.5 MB)

---

## 2. Automated Quality Verification Results

* **Unit Test Execution (`./gradlew testDebugUnitTest`)**: **123 passed, 0 failed, 0 skipped**
* **Android Lint (`./gradlew lintDebug`)**: **0 errors**
* **Release Bundle Build (`./gradlew bundleRelease`)**: **BUILD SUCCESSFUL**
* **Release APK Build (`./gradlew assembleRelease`)**: **BUILD SUCCESSFUL**

---

## 3. Order of Remediation

1. **Phase 1 (Play Console Prerequisites)**: Complete Play Console Location Permissions Form & Data Safety Section.
2. **Phase 2 (Access Token Hardening)**: Update `SessionManager.kt` to encrypt access tokens via `SecureAndroidRefreshStore` AES-256 GCM.
3. **Phase 3 (AAB Upload)**: Upload `app-release.aab` to Play Console Internal Testing track.

# Controlled Staging Deployment Checklist

---

## Pre-Deployment Verification Checklist

- [x] **Database Migrations Ready**: SQL scripts 035, 036, 037 tested and ready to apply.
- [x] **Backend Unit Tests**: 195/195 tests passing (`npm test`).
- [x] **Android Unit Tests**: 137/137 tests passing (`./gradlew testDebugUnitTest`).
- [x] **Android Lint**: 0 errors (`./gradlew lintDebug`).
- [x] **Target SDK 36**: App targets API 36 (Android 16).
- [x] **Cleartext Traffic Disabled**: `usesCleartextTraffic="false"` in release manifest.
- [x] **Release Artifacts Generated**: `.apk` and `.aab` compiled and checksummed.
- [ ] **Apply Database Migrations to Staging DB**: Apply 035, 036, 037 to staging PostgreSQL database.
- [ ] **Configure Staging Environment Variables**: Set `MD_EMAIL`, `CRON_SECRET`, `RESEND_API_KEY`, `FIREBASE_ADMIN_KEY` in Vercel staging.
- [ ] **Google Play Console Location Form**: Submit Location Permissions Form for `ACCESS_BACKGROUND_LOCATION`.

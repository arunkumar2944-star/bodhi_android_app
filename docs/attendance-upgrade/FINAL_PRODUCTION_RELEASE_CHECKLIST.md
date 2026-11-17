# Final Production Release Checklist & Pre-Launch Blockers

---

## Final Pre-Launch Checklist

- [x] **Backend Unit Tests**: 195/195 tests pass (`npm test`).
- [x] **Android Unit Tests**: 137/137 tests pass (`./gradlew testDebugUnitTest`).
- [x] **Android Lint**: 0 errors (`./gradlew lintDebug`).
- [x] **Release Artifacts**: `.apk` (`53.2 MB`) and `.aab` (`52.5 MB`) generated for v1.0.7 (versionCode 20).
- [ ] **Apply Migrations 035–038 to Production Database**: Apply SQL scripts 035, 036, 037, 038 to production database.
- [ ] **Configure Live Vercel Environment Variables**: Set `RESEND_API_KEY`, `MD_EMAIL`, `CRON_SECRET`, `FIREBASE_ADMIN_CREDENTIALS`.
- [ ] **Google Play Console Form**: Submit Location Permissions Declaration Form for `ACCESS_BACKGROUND_LOCATION`.

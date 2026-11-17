# Phase 2.6 Integration Test Results Report

**Date**: November 2026  

---

## 1. Automated Test Results Summary

* **Express Backend Unit & Integration Tests (`npm test`)**: **195 Passed, 0 Failed, 0 Skipped** (`100% Pass Rate`)
* **Android Unit Test Suite (`app:testDebugUnitTest`)**: **137 Passed, 0 Failed, 0 Skipped** (`100% Pass Rate`)
* **Android Lint Check (`app:lintDebug`)**: **0 Errors**
* **Release Artifacts**:
  * Debug APK (`67.2 MB`) — Signed & verified
  * Release APK (`53.0 MB`) — Compiled successfully
  * Release AAB (`52.3 MB`) — Compiled successfully

---

## 2. Test Execution Output Logs

```bash
# Express Backend
> backend@1.0.0 test
> node --test tests/*.test.js
ℹ tests 195
ℹ pass 195
ℹ fail 0

# Android Client
> ./gradlew testDebugUnitTest
137 passed, 0 skipped, 0 failed
```

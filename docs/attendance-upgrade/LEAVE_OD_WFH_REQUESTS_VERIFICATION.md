# Leave, On Duty, and Work From Home Application Feature Verification

**Application Package**: `com.prodigit.bodhiattendance`  
**Version**: 1.0.7 (versionCode 20)  
**Date**: November 2026  

---

## 1. Feature Architecture

1. **New Application Form Screen**: Created [`LeaveOnDutyWfhRequestScreen.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/request/LeaveOnDutyWfhRequestScreen.kt):
   * **Leave Request**: Supports Full Day, First Half, and Second Half leave selection.
   * **On Duty Request**: Supports duty location, time window (e.g. 09:00–17:00), and purpose input.
   * **Work From Home Request**: Supports remote work date and reason submission.
2. **Updated Request Center ([`RequestScreen.kt`](file:///D:/Prodigit/android/CRMAttendance/app/src/main/java/com/prodigit/bodhiattendance/ui/request/RequestScreen.kt))**:
   * Added request cards for **Apply for Leave**, **Apply for On Duty**, and **Work From Home**.
   * Enum `EmployeeRequestType` extended to include `LEAVE`, `ON_DUTY`, `WORK_FROM_HOME`.
3. **Request History & Lifecycle Status Display**:
   * **While Pending Approval**: Displays yellow status badge **`Pending Approval`** / **`Requested`**.
   * **After Manager Approval**: Displays green status badge **`Approved`** / **`Leave`** / **`On Duty`** / **`Work From Home`** with reviewer name and timestamp.
   * **If Rejected**: Displays red status badge **`Rejected`** with review remarks.
4. **Backend Integration**: Submits requests via `POST /api/v1/attendance/mobile/requests` and queries via `GET /api/v1/attendance/mobile/requests`.

---

## 2. Test Execution Verification

* **Android Unit Tests (`app:testDebugUnitTest`)**: **137 Passed, 0 Failed**
* **Android Lint (`app:lintDebug`)**: **0 Errors**
* **Build Status**: **BUILD SUCCESSFUL** (Debug APK, Release APK & Release AAB compiled)

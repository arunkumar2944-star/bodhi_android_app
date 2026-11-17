# Phase 2.1 Android API Contract Document

**Endpoint**: `GET /api/v1/attendance/day-policy/mine`  
**Alternative Route**: `GET /api/v1/attendance/day-eligibility`  
**Authentication**: Bearer JWT (`verifyToken` + `requireActiveAndroidSession`)  

---

## Response DTO Schema & Compatibility

```json
{
  "success": true,
  "data": {
    "userId": 7,
    "employeeName": "Arunkumar",
    "date": "2026-11-17",
    "serverTimeMs": 1790000000000,
    "status": "EXPECTED_AT_OFFICE",
    "monitoringMode": "AWAITING_ARRIVAL",
    "source": "SCHEDULE",
    "hasCheckIn": false,
    "attendanceState": "NOT_CHECKED_IN",
    "arrivalDeadline": "10:30",
    "hasOnDuty": false,
    "attendanceConflict": false,
    "overrideStatus": null,
    "overrideReason": null,
    "revision": 0,
    "validUntilMs": 1790000300000,
    "nextCheckAfterSeconds": 900,
    "shiftStartTime": "08:30:00",
    "shiftEndTime": "17:30:00",
    "halfDayPeriod": null,
    "timezone": "Asia/Kolkata",
    "policyVersion": 2,
    "companyId": 1,
    "offlinePermit": "eyJhbGciOiJIUzI1Ni...",
    "offlineValidUntilMs": 1790043200000
  }
}
```

### Response Field Descriptions
* `status` (String): Daily status string (`EXPECTED_AT_OFFICE`, `HOLIDAY`, `APPROVED_LEAVE`, `APPROVED_HALF_DAY`, `WEEKLY_OFF`, `AT_WORK`, `CHECKED_OUT`).
* `monitoringMode` (String): Monitoring control mode (`AWAITING_ARRIVAL`, `ACTIVE`, `PAUSED`).
* `shiftStartTime` (String): Workday shift start time (`"08:30:00"`).
* `shiftEndTime` (String): Workday shift end time (`"17:30:00"`).
* `halfDayPeriod` (String | null): Half-day period if approved half-day leave (`"FIRST_HALF"`, `"SECOND_HALF"`, or `null`).
* `timezone` (String): Timezone identifier (`"Asia/Kolkata"`).
* `policyVersion` (Number): API policy version integer (`2`).
* `offlinePermit` (String | null): JWT offline attendance authorization permit.

# Giri (Employee ID 2) Missing Check-in Incident Diagnostics Report

**Employee Name**: Giri  
**Employee ID**: 2  
**Incident Date**: 17 November 2026  
**Status**: **NOT VERIFIED**  

---

## Diagnostic Audit Summary

1. **Dataset Boundary**: 17 November 2026 records do not exist in the captured local database instance (`neondb`).
2. **Diagnostic Helper Execution**:
   ```javascript
   const { traceUserIncident } = require('./src/utils/incidentDiagnostics');
   const result = await traceUserIncident(2, "2026-11-17");
   ```
   *Result*: `userFound = true` (Giri, User ID 2, Company ID 1), `eventsCount = 0`, `candidatesCount = 0`.
3. **Confirmed Historical Check-In Blockers (Code Analysis)**:
   * Prior to v1.0.4, unsettled diagnostic uploads blocked monitoring start gate execution (`BLOCKED:PRESTART_SYNC_NOT_SETTLED`).
   * Connected office Wi-Fi BSSID mismatch (e.g. connected to `:39` when policy required `:3A`).
   * Location accuracy exceeding CRM office limits (> 50m).

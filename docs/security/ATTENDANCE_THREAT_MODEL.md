# Attendance Automation Threat Model & Risk Analysis

**Application**: Bodhi Attendance (`com.prodigit.bodhiattendance`)  
**Scope**: Android Client, Background Services, Monotonic Time Anchors, Geofence/Wi-Fi Evidence, and REST API Boundaries  
**Framework**: OWASP MASVS / MASTG & STRIDE Threat Modeling  

---

## 1. Asset & Trust Boundary Mapping

```
 [ Untrusted User / Device ]
           │
           │ (Mock Location Apps, System Clock Tampering, Wi-Fi Spoofing)
           ▼
┌─────────────────────────────────────────────────────────────┐
│ Android Client Boundary (com.prodigit.bodhiattendance)      │
│                                                             │
│  [GpsEvidenceStatus] ───► [LocationCompat.isMock()]         │
│  [TimeAnchorManager] ───► Monotonic SystemClock             │
│  [ArrivalCandidate]  ───► Room DB (ArrivalCandidateDao)    │
│  [SecureRefresh]     ───► KeyStore AES-256 GCM             │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               │ HTTPS (TLS 1.2+ / REST API)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ Server Boundary (Express CRM Backend / PostgreSQL)          │
│                                                             │
│  - Time Anchor Generation & Validation                       │
│  - Device Registration & Token Rotation                      │
│  - Idempotent Attendance Event Evaluation (eventUuid)        │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Threat Analysis & Mitigations

### Threat 1: Phone Clock Alteration / Date Tampering
* **Threat Actor**: Malicious employee altering device date/time to backdate check-in or checkout.
* **Attack Vector**: Changing system date/time in Android Settings before opening the app.
* **Existing Mitigation**: **Monotonic Time Anchors (`TimeAnchorManager`)**. The app never trusts `System.currentTimeMillis()`. All attendance timestamps use monotonic `SystemClock.elapsedRealtime()` anchored against server-issued ISO time tokens (`TimeAnchorEntity`).
* **Residual Risk**: Zero (Phone wall-clock changes have zero impact on attendance calculations).

---

### Threat 2: Mock Location / Fake GPS Spoofing
* **Threat Actor**: Employee using fake GPS / mock location apps to simulate office presence while remote.
* **Attack Vector**: Enabling Mock Location in Developer Options and broadcasting spoofed latitude/longitude.
* **Existing Mitigation**:
  1. `GpsEvidenceStatus.rejection()` detects mock location providers via `LocationCompat.isMock(fix)`.
  2. Mock location fixes return `GpsEvidenceRejection.MOCK_LOCATION_DETECTED`, blocking evaluation and logging `UNCERTAIN` review evidence.
  3. Multi-factor presence validation: Mandatory Office Wi-Fi BSSID matching (`wifiRequired = true`) prevents standalone GPS spoofing.
* **Residual Risk**: Low (Requires rooted device with Xposed/Magisk module hiding mock location flags; mitigated by office Wi-Fi verification).

---

### Threat 3: Replay of Unsynced Attendance Events
* **Threat Actor**: Malicious user capturing and replaying network requests or database rows to duplicate check-in events.
* **Attack Vector**: Resending an offline `AttendanceEventRequest` JSON payload via proxy or API client.
* **Existing Mitigation**:
  1. **Event UUID Idempotency**: Each event is assigned a `UUID.randomUUID()` upon local Room creation (`AttendanceEventEntity`).
  2. Express backend enforces unique constraints on `eventUuid` and rejects duplicate submissions (`response.code == "DUPLICATE_EVENT"`).
* **Residual Risk**: Zero.

---

### Threat 4: Refresh Token Theft from Device Storage
* **Threat Actor**: Physical attacker or malicious app attempting to extract long-lived refresh tokens.
* **Attack Vector**: Reading SharedPreferences files from disk or extracting `adb backup`.
* **Existing Mitigation**:
  1. `SecureAndroidRefreshStore` encrypts refresh credentials using **Android KeyStore AES-256 GCM** (`bodhi_refresh_aes_v1`).
  2. `android:allowBackup="false"` prevents ADB backup extraction.
* **Residual Risk**: Low (Requires hardware-level root exploit to extract KeyStore keys).

---

### Threat 5: Process Interruption During Absence Grace Period
* **Threat Actor**: App process death or Low Memory Kill (LMK) during the 3-minute grace period or 90-minute checkout window.
* **Attack Vector**: OS kills app process while employee is on break.
* **Existing Mitigation**:
  1. `AttendanceEngineStateStore` persists `AbsenceGrace(absenceStartedElapsedRealtimeMs)` and `BreakActive` in Room DB (`attendance_engine_state`).
  2. Upon process restart, `DeparturePhaseRecovery` restores the original monotonic departure timestamp, ensuring grace duration is preserved across process death.
* **Residual Risk**: Zero.

---

## 3. Residual Risk Matrix

| Risk ID | Threat Description | Severity | Likelihood | Residual Risk | Status |
|---|---|---|---|---|---|
| **R-01** | Phone Wall-Clock Tampering | HIGH | HIGH | **ZERO** | Mitigated by Monotonic Time Anchors |
| **R-02** | Fake GPS App Spoofing | HIGH | MEDIUM | **LOW** | Mitigated by `LocationCompat.isMock()` & Office Wi-Fi |
| **R-03** | Replayed Offline Punches | MEDIUM | LOW | **ZERO** | Mitigated by Room `eventUuid` Idempotency |
| **R-04** | Refresh Token Data Extraction | HIGH | LOW | **LOW** | Mitigated by KeyStore AES-256 GCM |
| **R-05** | ADB Data Backup Extraction | MEDIUM | MEDIUM | **ZERO** | Mitigated by `allowBackup="false"` |

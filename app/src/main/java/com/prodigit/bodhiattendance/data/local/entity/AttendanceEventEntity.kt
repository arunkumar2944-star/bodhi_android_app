package com.prodigit.bodhiattendance.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "attendance_events",

    indices = [
        Index(
            value = ["eventUuid"],
            unique = true
        )
    ]
)
data class AttendanceEventEntity(

    // ============================================================
    // LOCAL ROOM PRIMARY KEY
    // ============================================================

    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,


    // ============================================================
    // UNIQUE ATTENDANCE EVENT ID
    //
    // Generated on Android using UUID.
    //
    // Used as the idempotency identifier when synchronizing an
    // offline event to the backend.
    //
    // This prevents the same locally-created attendance event from
    // being inserted multiple times when synchronization is retried.
    // ============================================================

    val eventUuid: String,


    // ============================================================
    // USER / COMPANY / DEVICE
    // ============================================================

    val userId: Int,

    val companyId: Int,

    // Application-generated installation/device UUID.
    //
    // Do not use IMEI, hardware serial number or MAC address
    // as the application's primary device identifier.

    val deviceId: String,


    // ============================================================
    // EVENT
    //
    // Expected values later:
    //
    // CHECK_IN
    // BREAK_START
    // BREAK_END
    // CHECK_OUT
    // ============================================================

    val eventType: String,


    // ============================================================
    // EVENT TIME
    //
    // Epoch milliseconds captured when Android creates the event.
    //
    // IMPORTANT:
    //
    // This is device-supplied evidence/local event metadata.
    // The backend should not treat the employee phone clock as
    // authoritative attendance time.
    //
    // Server-side validated/received time remains authoritative.
    // ============================================================

    val eventTime: Long,

    // ============================================================
    // TRUSTED TIME ANCHOR
    //
    // References:
    //
    // time_anchors.anchorUuid
    //
    // The attendance engine uses this anchor together with the
    // event's monotonic elapsedRealtime value to reconstruct and
    // validate trusted attendance time.
    //
    // Nullable because:
    //
    // - Existing local events were created before trusted time
    //   anchor persistence was introduced.
    //
    // - Historical rows must remain valid after Room migration.
    //
    // IMPORTANT:
    //
    // This value is the server-generated anchor UUID.
    // ============================================================

    val timeAnchorId: String? = null,


    // ============================================================
    // EVENT MONOTONIC TIME
    //
    // SystemClock.elapsedRealtime() captured when this attendance
    // event was generated.
    //
    // Combined later with the referenced server time anchor:
    //
    // trustedEventTime =
    //
    //     anchor.serverTime
    //     +
    //     (eventElapsedRealtimeMs - anchor.elapsedRealtimeMs)
    //
    // Nullable for attendance records created before this trusted
    // time implementation was introduced.
    // ============================================================

    val eventElapsedRealtimeMs: Long? = null,

    // ============================================================
    // GPS EVIDENCE
    // ============================================================

    val latitude: Double? = null,

    val longitude: Double? = null,

    val accuracyMeters: Float? = null,

    val insideGeofence: Boolean = false,


    // ============================================================
    // BLE BEACON EVIDENCE
    //
    // Will later be populated by the office BLE beacon detector.
    // ============================================================

    val beaconDetected: Boolean = false,

    val beaconId: String? = null,

    val beaconRssi: Int? = null,


    // ============================================================
    // OFFICE WI-FI EVIDENCE
    //
    // Wi-Fi is an ADDITIONAL office presence confirmation signal.
    //
    // wifiVerified:
    // true  -> connected/detected BSSID matched an approved office
    //          BSSID from office configuration.
    //
    // false -> office Wi-Fi was not verified.
    //
    // wifiBssid:
    // BSSID detected by Android when the evidence was collected.
    //
    // IMPORTANT:
    //
    // Do NOT use:
    // - Public IP address as office identity
    // - SSID alone as office identity
    //
    // Approved BSSID(s) will eventually come from backend / CRM
    // office configuration.
    // ============================================================

    val wifiVerified: Boolean = false,

    val wifiBssid: String? = null,


    // ============================================================
    // LOCAL SYNC STATE
    //
    // Expected values:
    //
    // PENDING
    // SYNCING
    // SYNCED
    // FAILED
    // ============================================================

    val syncStatus: String = "PENDING",


    // ============================================================
    // RETRY INFORMATION
    // ============================================================

    val syncAttempts: Int = 0,

    val lastSyncAttemptAt: Long? = null,

    val syncError: String? = null,


    // ============================================================
    // SERVER RESULT
    //
    // serverAttendanceId:
    // Attendance/event identifier returned by the backend after
    // successful synchronization.
    //
    // syncedAt:
    // Local record of when synchronization completed.
    // ============================================================

    val serverAttendanceId: Long? = null,

    val syncedAt: Long? = null,


    // ============================================================
    // LOCAL CREATED TIME
    //
    // Local Room record creation timestamp.
    // ============================================================

    val arrivalProofJson: String? = null,
    val effectiveTimeEpochMs: Long? = null,
    val reason: String? = null,

    val createdAt: Long
)
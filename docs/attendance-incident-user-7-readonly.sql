-- Inspected PostgreSQL schema; run against the confirmed authoritative backend.
-- No credentials, mutations, migrations or writes. Timestamp-without-zone stays text.
BEGIN READ ONLY;
SET LOCAL statement_timeout = '15s';
SELECT NOW(), current_setting('TimeZone'), current_setting('transaction_read_only');
SELECT id, company_id FROM users WHERE id = 7;

SELECT id, user_id, company_id, attendance_date::text,
       login_time::text, logout_time::text, attendance_state,
       total_break_minutes, gross_work_minutes, total_work_minutes,
       first_presence_at, last_presence_at
FROM attendance WHERE user_id=7
AND attendance_date IN ('2026-10-31','2026-11-02') ORDER BY attendance_date,id;

SELECT e.id,e.event_uuid,e.attendance_id,e.user_id,e.company_id,e.device_id,
       e.event_type,e.effective_event_time,
       e.effective_event_time AT TIME ZONE 'Asia/Kolkata' AS effective_ist,
       e.presence_verified_at,e.server_received_at,e.time_anchor_id,
       e.event_elapsed_realtime_ms,e.accuracy_meters,e.inside_geofence,
       e.wifi_verified,e.wifi_bssid,e.arrival_candidate_id,e.attendance_reason,
       e.is_active,e.time_source,e.time_confidence
FROM attendance_events e WHERE user_id=7 AND
 ((effective_event_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02')
 OR attendance_id IN (SELECT id FROM attendance WHERE user_id=7
 AND attendance_date IN ('2026-10-31','2026-11-02')))
ORDER BY effective_event_time,id;

SELECT id,event_uuid,event_type,effective_event_time,server_received_at,event_origin
FROM attendance_effective_events WHERE user_id=7 AND
 (effective_event_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02')
ORDER BY effective_event_time,id;

SELECT * FROM attendance_arrival_candidates WHERE user_id=7 AND
 (first_presence_evidence_at AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02')
ORDER BY first_presence_evidence_at;

-- Messages must be reviewed/redacted for precise coordinates before redistribution.
SELECT id,category,message,device_id,boot_id,observed_elapsed_ms,device_time,received_at
FROM attendance_diagnostics WHERE user_id=7 AND
 ((device_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02')
 OR (received_at AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02'))
ORDER BY boot_id,observed_elapsed_ms,id;

SELECT a.* FROM device_time_anchors a WHERE a.id IN
 (SELECT time_anchor_id FROM attendance_events WHERE user_id=7 AND
 (effective_event_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02'));

SELECT * FROM attendance_monitor_gaps WHERE user_id=7
AND last_heartbeat_at<'2026-11-03T00:00:00+05:30'
AND COALESCE(recovered_at,NOW())>='2026-10-31T00:00:00+05:30';

SELECT * FROM attendance_regularization_events WHERE user_id=7 AND
 (effective_event_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02');

SELECT id,company_id,location_name,geofence_radius_meters,max_accuracy_meters,
       wifi_required,ble_required,is_active,is_enabled,grace_period_minutes,maximum_absence_minutes
FROM attendance_locations WHERE company_id=(SELECT company_id FROM users WHERE id=7);

-- Exact completed breaks, retaining seconds rather than rounded summary minutes.
WITH b AS (
 SELECT attendance_id,event_uuid,event_type,effective_event_time,
 LEAD(event_type) OVER w AS next_type,LEAD(effective_event_time) OVER w AS next_time
 FROM attendance_effective_events WHERE user_id=7 AND is_active=TRUE
 AND event_type IN ('BREAK_START','BREAK_END') AND
 (effective_event_time AT TIME ZONE 'Asia/Kolkata')::date='2026-10-31'
 WINDOW w AS (PARTITION BY attendance_id ORDER BY effective_event_time,server_received_at,id)
)
SELECT *, EXTRACT(EPOCH FROM next_time-effective_event_time) AS duration_seconds
FROM b WHERE event_type='BREAK_START'; -- NULL/non-BREAK_END next_type is not a completed pair.
ROLLBACK;

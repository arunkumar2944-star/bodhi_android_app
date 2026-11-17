-- SELECT-only; connect with options='-c default_transaction_read_only=on'.
-- Do not call bootstrap/day/maintenance handlers: some have closure side effects.
SELECT current_database(),current_schema(),current_setting('search_path'),
       current_setting('TimeZone'),current_setting('transaction_read_only'),NOW();
SELECT id,company_id,role,is_active FROM users WHERE id=7;
SELECT table_schema,table_name,table_type FROM information_schema.tables
WHERE table_schema NOT IN ('pg_catalog','information_schema')
AND (table_name ILIKE '%attendance%' OR table_name ILIKE '%archive%' OR table_name ILIKE '%migration%');
SELECT pg_get_viewdef('public.attendance_effective_events'::regclass,true);

SELECT a.id,a.attendance_date::text,a.login_time::text,a.logout_time::text,
       a.total_work_minutes,a.gross_work_minutes,a.total_break_minutes,a.attendance_state,
       a.first_presence_at,a.last_presence_at,e.event_uuid,e.event_type,
       e.effective_event_time,
       (e.effective_event_time AT TIME ZONE 'Asia/Kolkata')::text AS expected_ist,
       e.presence_verified_at,e.server_received_at,e.is_active,e.time_anchor_id,
       e.event_elapsed_realtime_ms,e.arrival_candidate_id
FROM attendance a LEFT JOIN attendance_events e ON e.attendance_id=a.id
WHERE a.user_id=7 AND a.company_id=1
AND a.attendance_date IN ('2026-10-31','2026-11-02')
ORDER BY a.attendance_date,e.effective_event_time,e.id;

-- No active filter: detect raw inactive or orphan/unprojected history.
SELECT id,event_uuid,attendance_id,user_id,company_id,event_type,is_active,
 effective_event_time,presence_verified_at,server_received_at
FROM attendance_events WHERE user_id=7 AND
 ((effective_event_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02')
 OR (server_received_at AT TIME ZONE 'Asia/Kolkata')::date='2026-10-31');
SELECT id,event_uuid,attendance_id,event_type,effective_event_time,is_active,event_origin
FROM attendance_effective_events WHERE user_id=7 AND
 ((effective_event_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02')
 OR attendance_id IN (SELECT id FROM attendance WHERE user_id=7 AND attendance_date IN ('2026-10-31','2026-11-02')));
SELECT * FROM attendance_arrival_candidates WHERE user_id=7 AND
 (first_presence_evidence_at AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02');
SELECT a.* FROM device_time_anchors a WHERE a.id IN
 (SELECT time_anchor_id FROM attendance_events WHERE user_id=7 AND
 (effective_event_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02'));
SELECT attendance_id,event_uuid,work_date::text,effective_event_time,recorded_at,reason
FROM attendance_day_closures WHERE user_id=7 AND work_date IN ('2026-10-31','2026-11-02');
SELECT * FROM attendance_regularization_events WHERE user_id=7 AND
 ((effective_event_time AT TIME ZONE 'Asia/Kolkata')::date IN ('2026-10-31','2026-11-02')
 OR attendance_id IN (SELECT id FROM attendance WHERE user_id=7 AND attendance_date IN ('2026-10-31','2026-11-02')));
SELECT id,request_date::text,request_type,subtype,status,requested_time,reviewed_at
FROM attendance_requests WHERE user_id=7 AND request_date IN ('2026-10-31','2026-11-02');
SELECT id,request_id,action,previous_status,next_status,created_at
FROM attendance_request_review_audit WHERE user_id=7 AND request_id IN
 (SELECT id FROM attendance_requests WHERE user_id=7 AND request_date IN ('2026-10-31','2026-11-02'));

WITH b AS (
 SELECT attendance_id,event_uuid,event_type,effective_event_time,
 LEAD(event_type) OVER w AS next_type,LEAD(effective_event_time) OVER w AS next_time
 FROM attendance_effective_events WHERE user_id=7 AND is_active=TRUE
 AND event_type IN ('BREAK_START','BREAK_END')
 AND attendance_id IN (SELECT id FROM attendance WHERE user_id=7 AND attendance_date='2026-10-31')
 WINDOW w AS (PARTITION BY attendance_id ORDER BY effective_event_time,server_received_at,id)
)
SELECT *,CASE WHEN next_type='BREAK_END' AND next_time>=effective_event_time
 THEN EXTRACT(EPOCH FROM next_time-effective_event_time) END AS completed_seconds
FROM b WHERE event_type='BREAK_START';

-- Stable SQL literal reproduction, independent of Node parameter inference:
SELECT '2026-11-02T09:36:01.320Z'::timestamp::text AS naive_utc_input,
 ('2026-11-02T09:36:01.320Z'::timestamptz AT TIME ZONE 'Asia/Kolkata')::text AS intended_ist;

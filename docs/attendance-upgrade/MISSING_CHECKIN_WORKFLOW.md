# Phase 2.4 Missing Check-in & Regularization Workflow Design

---

## Workflow Diagram

```text
[ Office Arrival Detected ] ──► Stores ArrivalCandidate (PROVISIONAL)
            │
            ├─► Check-In Confirmed within 60m? ──► YES ──► Attendance Marked (NORMAL)
            │
            └─► NO (60m elapsed) 
                 │
                 ▼
     [ Missing Check-In Detector ] ──► Enqueues ATTENDANCE_MISSING_CHECKIN_60M
                 │
                 ▼
     [ Employee Notification ] ──► "Arrival detected at 09:35 AM; Check-In unconfirmed."
                 │
                 ▼
     [ Employee Actions ]
         ├──► Action A: Confirm Check-In (Triggers immediate GPS verification burst)
         └──► Action B: Request Regularization (Pre-fills arrival proof for Manager Review)
                 │
                 ▼
     [ Manager Approval ] ──► Approves request ──► Attendance updated & audit logged
```

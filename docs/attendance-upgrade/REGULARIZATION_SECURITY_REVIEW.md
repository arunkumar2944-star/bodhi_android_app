# Regularization Security & Authorization Review

---

## Security & Tenant Isolation Principles

1. **Server-Derived Identity**: The backend derives the employee identity (`req.user.id`) and company identity (`req.user.company_id`) from verified JWT tokens. Client-supplied user IDs in request bodies are ignored.
2. **Self-Approval Prohibition**: Managers cannot approve their own regularization requests (`if (request.user_id === actor.id) throw 403`).
3. **Idempotency & Replay Protection**: Each regularization request uses a unique UUID and enforces single-review state transitions (`PENDING` $\rightarrow$ `APPROVED` / `REJECTED`).
4. **Audit Trail Integrity**: Every regularization review logs a row in `attendance_request_review_audit` with `before_summary`, `after_summary`, actor ID, timestamp, and review remarks.

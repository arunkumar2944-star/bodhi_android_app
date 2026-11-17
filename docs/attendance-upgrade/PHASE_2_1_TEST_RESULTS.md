# Phase 2.1 Backend Test Results Report

**Repository**: `D:\Prodigit\CRM_TEST_Backend`  
**Test Framework**: Node.js Native Test Runner (`node --test`)  
**Date**: November 2026  

---

## 1. Test Suite Execution Summary

```text
> backend@1.0.0 test
> node --test tests/*.test.js

ℹ tests 191
ℹ suites 0
ℹ pass 191
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
ℹ duration_ms 5696.9596
```

---

## 2. Key Phase 2.1 Verified Test Cases

1. `decide returns PAUSED for approved full-day leave`: **PASS**
2. `decide returns ACTIVE for approved half-day leave`: **PASS**
3. `decide returns PAUSED for company holiday`: **PASS**
4. `decide returns AWAITING_ARRIVAL for regular working day`: **PASS**
5. `historical offline eligibility ignores only policy closure`: **PASS**
6. `all three persisted write sites: Node=UTC, DB=Asia/Kolkata`: **PASS**
7. `all three persisted write sites: Node=Asia/Kolkata, DB=Asia/Kolkata`: **PASS**
8. `DISTRIBUTED RATE LIMITING`: **PASS**
9. `notification read acknowledgement is scoped and idempotent`: **PASS**

# Frontend Test Report

This document summarizes the `v-beta` frontend automated test run.

## Test Run Metadata

- **Date:** 2026-04-25
- **Component:** `v-beta/` (Next.js frontend)
- **Commands executed:**
  - `npm test -- --watchAll=false`

## Overall Result

- **Unit tests (`npm test`):** PASS
  - Test suites: 1 passed, 1 total
  - Tests: 3 passed, 3 total
  - Snapshots: 0 total
  - Time: 8.266 s

## Coverage Snapshot (From Jest Output)

- Statements: 76.19%
- Branches: 0%
- Functions: 75%
- Lines: 77.77%

## Evidence to Commit (GitHub)

- Test execution screenshot:
  - `docs/testing/evidence/frontend/npm-test-summary.png`

![Frontend Jest Test Summary](./evidence/frontend/npm-test-summary.png)

## Console Evidence Excerpt

```text
Test Suites: 1 passed, 1 total
Tests:       3 passed, 3 total
...
```

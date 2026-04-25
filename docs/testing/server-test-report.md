# Server Test Report

This document summarizes the backend automated test run and links to committed evidence files.

## Test Run Metadata

- **Date:** 2026-04-25
- **Component:** `server/` (Spring Boot backend)
- **Commands executed:**
  - `./mvnw test`
  - `./mvnw surefire-report:report`

## Overall Result

- **Status:** PASS
- **Total tests:** 60
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Success rate:** 100%
- **Execution time:** 16.18 s

## Package Breakdown

- `edu.ics499.VBeta.Integration_Test` - 53 tests, 0 failures, 0 errors, 0 skipped
- `edu.ics499.VBeta` - 7 tests, 0 failures, 0 errors, 0 skipped

## Committed Evidence (GitHub)

- Surefire HTML report: [`docs/testing/evidence/backend/surefire-report-2026-04-25.html`](./evidence/backend/surefire-report-2026-04-25.html)
- Add terminal screenshot here: `docs/testing/evidence/backend/mvn-test-summary.png`

## Local Generated Artifacts (Not Committed)

These files are regenerated locally whenever tests run:

- `server/target/reports/surefire.html`
- `server/target/surefire-reports/`

## Console Evidence Excerpt

```text
[INFO] Results:
[INFO]
[INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] --- surefire-report:3.5.5:report (default-cli) @ team-satisfaction-server ---
[INFO] BUILD SUCCESS
```

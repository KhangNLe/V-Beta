# Server Test Report

This document summarizes the backend automated test run and links to committed evidence files.

## Test Run Metadata

- **Date:** 2026-05-04
- **Component:** `server/` (Spring Boot backend)
- **Commands executed:**
  - `./mvnw test`
  - `./mvnw surefire-report:report -DoutputDirectory=../docs/testing/evidence/backend`

## Overall Result

- **Status:** PASS
- **Total tests:** 67
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Success rate:** 100%
- **Execution time:** 24.58 s

## Suite Highlights

- `edu.ics499.VBeta.Integration_Test` - 59 tests, 0 failures, 0 errors, 0 skipped
- `edu.ics499.VBeta.performance.WallSectionsPerformanceTest` - 1 test, PASS
- `edu.ics499.VBeta.Integration_Test.DiscussionRootTest` - 6 tests, PASS (includes invalid parent FK rejection)
- Remaining backend suites (`edu.ics499.VBeta.*`) passed with no failures/errors

## Committed Evidence (GitHub)

- Surefire HTML report: [`docs/testing/evidence/backend/surefire-report-2026-05-04.html`](./evidence/backend/surefire-report-2026-05-04.html)
- Maven test run screenshot: [`docs/testing/evidence/backend/maven-test-2026-05-04.png`](./evidence/backend/maven-test-2026-05-04.png)

## Local Generated Artifacts (Not Committed)

These files are regenerated locally whenever tests run:

- `server/target/reports/surefire.html`
- `server/target/surefire-reports/`

## Console Evidence Excerpt

```text
[INFO] Results:
[INFO]
[INFO] Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] --- surefire-report:3.5.5:report (default-cli) @ team-satisfaction-server ---
[INFO] BUILD SUCCESS
```

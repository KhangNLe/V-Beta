# Server Test Report

This document summarizes the backend automated test run and links to committed evidence files.

## Test Run Metadata

- **Date:** 2026-05-12
- **Component:** `server/` (Spring Boot backend)
- **Commands executed:**
  - `./mvnw test`

## Overall Result

- **Status:** PASS
- **Total tests:** 70
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Success rate:** 100%
- **Execution time:** 24.957 s

## Suite Highlights

- Core integration suites passed with 0 failures/errors, including:
  - `edu.ics499.VBeta.Integration_Test.DiscussionRootTest` (FK rejection for invalid `parent_discussion_id` remains covered)
  - `edu.ics499.VBeta.Integration_Test.UserCommentTest`
  - `edu.ics499.VBeta.Integration_Test.SolutionBetaCreationDeletionTest`
  - `edu.ics499.VBeta.Integration_Test.AccountControllerTest`

## Committed Evidence (GitHub)

- Surefire HTML report (existing): [`docs/testing/evidence/backend/surefire-report-2026-05-04.html`](./evidence/backend/surefire-report-2026-05-04.html)
- Maven test run screenshot (existing): [`docs/testing/evidence/backend/maven-test-2026-05-04.png`](./evidence/backend/maven-test-2026-05-04.png)

## Local Generated Artifacts (Not Committed)

These files are regenerated locally whenever tests run:

- `server/target/reports/surefire.html`
- `server/target/surefire-reports/`

## Console Evidence Excerpt

```text
[INFO] Results:
[INFO]
[INFO] Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

# Server Test Report

This document summarizes the backend automated test run and links to committed evidence files.

## Test Run Metadata

- **Date:** 2026-05-14
- **Component:** `server/` (Spring Boot backend)
- **Commands executed:**
  - `./scripts/start-local-test-db.sh`
  - `DB_HOST=127.0.0.1 DB_PORT=55432 DB_NAME=v_beta_test SQL_USERNAME=postgres SQL_PASSWORD=postgres ./mvnw test`
  - `DB_HOST=127.0.0.1 DB_PORT=55432 DB_NAME=v_beta_test SQL_USERNAME=postgres SQL_PASSWORD=postgres ./mvnw test -Dtest=DiscussionRootTest,UserCommentTest,SolutionBetaCreationDeletionTest`
- **Environment notes:**
  - Local Docker PostgreSQL test DB (`vbeta-test-postgres`) on host port `55432`
  - Spring `test` profile with PostgreSQL overrides in integration tests

## Overall Result

- **Status:** PASS
- **Result summary:** Full backend suite and targeted discussion integrity suites completed successfully in local CI-like bootstrap path.

## Discussion Integrity Highlights (Workstream 1)

- Discussion read-path indexes were applied in both runtime and test schema SQL:
  - `idx_discussion_root_problem_created_id`
  - `idx_discussion_root_parent_created_id`
  - `idx_discussion_root_problem_parent_created_id`
- Discussion query ordering was hardened to be database-driven and deterministic using `(createdAt, discussionId)`.
- In-memory timeline sorting dependency was removed from discussion timeline assembly.
- Core integration suites passed, including:
  - `Integration_Test.app.VBeta.DiscussionRootTest`
  - `Integration_Test.app.VBeta.UserCommentTest`
  - `Integration_Test.app.VBeta.SolutionBetaCreationDeletionTest`

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
[INFO] Tests run: <see local generated surefire report>, Failures: 0, Errors: 0
[INFO]
[INFO] BUILD SUCCESS
```

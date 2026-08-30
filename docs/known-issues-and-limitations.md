# Known Issues and Limitations

This document reflects the current repository state on `sprint5/moderation_mvp`.

## Functional Limitations

### 1) Keyword/text search for problems is deferred to a later sprint
- Grade-range filtering and sort (most recent / easiest / hardest) shipped in completed Sprint 4 discovery.
- Text/keyword search was intentionally not in Sprint 4; tracked for a later sprint (see roadmap Sprint 9).
- Impact: users filter by grade/sort only until that later sprint.

### 2) Comment model is problem-level, not beta-level
- Discussion comments are attached to climbing problems (with user-comment linkage), not directly to a specific beta submission.
- Impact: limited threading/granularity when discussing multiple betas for the same problem.

### 3) Account page is read-only for profile fields
- Users can view account info (and delete account) but cannot self-edit profile attributes such as username/email in the app.
- Impact: profile correction/update requires future feature work or admin/back-end support.

## Authorization and API Limitations

### 4) No unified global API error envelope
- Server error responses are not fully standardized via a single global exception handler contract.
- Controllers catch `RuntimeException` and usually return **404** (wall/problem writes **400**; unread notification GET `/short` **401**; all-inbox GET `/all` **404**) with a plain-text message.
- Impact: clients must handle varying error payload shapes. Action-gated permission failures and duplicate reports currently look like 404/400 rather than 403/409. `GET /api/notification/all` also omits `readAt`; the `/notifications` page infers unread from a parallel `/short` poll.

## Testing and Quality Limitations

### 5) Frontend automated test coverage does not cover every page or end-to-end testing
- Current frontend automated testing is limited only to unit testing, and end-to-end testing is not implemented or supported. Unit testing for signup and login pages do not exist yet either.
- Impact: higher regression risk in UI behavior and integration points.

### 6) Backend integration tests still require PostgreSQL runtime availability
- Resolved gap: deterministic bootstrap is now scripted (`server/scripts/reset-test-db.sh`) and used by local/CI flows.
- Remaining limitation: local runs still require Docker or an existing PostgreSQL instance reachable by test env vars.

## Operations and Deployment Limitations

### 7) CI workflow automation is still maturing
- Resolved gap: backend and frontend PR workflows are in-repo via `.github/workflows/backend-ci.yml` and `.github/workflows/frontend-ci.yml`.
- Remaining limitation: full multi-stage quality gates (lint/docs/deploy checks) are not yet fully standardized.

### 8) Generated report artifacts are local by default
- Maven/Jest generated outputs are under build directories and are not automatically retained in Git.
- Impact: evidence can be lost unless copied into `docs/testing/evidence/`.

### 9) Environment and secret configuration is file/env-path dependent
- Firebase/GCP and DB configuration depend on local environment variables and credential file paths.
- Impact: deployment reliability and security depend on strict environment management practices.

## Planned Follow-up Areas

- Delayed GCS purge of soft-deleted beta objects after a retention window (appeal restore is already shipped).
- Reporting wall sections or problems (Sprint 5 covers comments and betas only).
- Email/push notification channels (in-app inbox only).
- Add automated explain/plan sampling for high-traffic discussion queries as dataset size grows.
- Standardize authorization checks for all mutating discussion/beta routes.
- Add global server error handling contract and document response schema.
- Expand frontend automated tests for core role-based and wall/problem flows.

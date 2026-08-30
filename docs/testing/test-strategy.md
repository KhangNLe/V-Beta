# Test Strategy

## Goal

This strategy defines how we validate correctness, authorization behavior, and regression safety for the system across:

- frontend (`v-beta/`)
- backend (`server/`)
- integration points between them

## Current Test Stack

### Backend (`server/`)

- Framework: JUnit 5 + Spring Boot Test
- Test styles in repo:
  - application/context smoke tests
  - `@WebMvcTest` Mockito controller tests (MockMvc, filters off, mocked services) covering `/api/...` routes
  - integration-style tests under `Integration_Test/*`
- Profiles/config:
  - `application-test.yml` uses H2 by default
  - integration tests under `Integration_Test/*` now use PostgreSQL datasource overrides aligned with runtime DB settings

Run command:

```bash
cd server
./scripts/test-with-postgres.sh
```

### Frontend (`v-beta/`)

- Framework configured: Jest + `next/jest` + Testing Library
- Setup files:
  - `jest.setup.js` (`fetch` polyfill)
  - `jest.setupAfterEnv.js` (`@testing-library/jest-dom`)
- Test styles in repo:
  - page-level component tests using React Testing Library
  - ZOMBIE-style coverage (Zero, One, Many, Boundaries, Interfaces, Exceptions)
  - boundary mocking for auth/session, route params, router actions, and API modules
  - async flow validation for loading/error/empty/success states
  - role/ownership gating tests for guest/climber/setter/admin experiences
  - mutation and failure-path tests for account role changes, wall actions, comments, beta upload, reports, notifications, and appeals
  - pending-state and UI disablement checks for long-running actions (`Adding...`, `Deleting...`, `Submitting...`, `Uploading...`)

Run command:

```bash
cd v-beta
npm test
```

## Test Scope by Layer

### 1) Backend Unit/Service-Level Tests

Focus:

- role/action authorization rules
- account role change behavior
- discussion and beta ownership checks
- domain validation and error paths
- report queue, logbook, notification inbox, and appeal create/resolve

Target outcomes:

- deterministic logic checks
- fast feedback on permission regressions

### 2) Backend Integration Tests

Focus:

- end-to-end service/controller behavior with realistic persistence interactions
- wall/problem lifecycle operations
- comment creation/soft-deletion behavior (root stays; timeline hides deleted rows)
- solution beta metadata creation/soft-deletion behavior (GCS object is not deleted on user/admin hide)
- discussion timeline/read ordering and deterministic retrieval semantics

Target outcomes:

- endpoint-to-service-to-repository correctness
- regression protection for critical domain flows

### 3) Frontend Component/Flow Tests

Priority targets:

- auth forms (login/signup/verify/reset) state and validation behavior, with login/signup coverage prioritized next
- role-based UI visibility (guest/climber/setter/admin)
- wall/problem page interaction states (loading, error, empty, success)
- problem-page discussion ⋮ menu (owner/admin delete vs signed-in report, dialog validation)
- account page behaviors (including delete flow and role-management entry points when applicable)

Target outcomes:

- stable user-visible behavior during UI refactors
- confidence in auth/role gating in client UX
- better regression protection for async mutation and error branches in critical pages

### 4) Manual End-to-End Validation

Because the product is still evolving, manual smoke/regression checks remain required each release.

Manual checklist details should live in:

- `docs/testing/manual-test-cases.md`

## Risk-Based Priorities

Highest regression risk areas:

- authentication/session sync (`/api/accounts/session`)
- authorization and role enforcement (`CHANGE_ROLE`, `VIEW_ACCOUNTS`, `DELETE_COMMENT`, etc.)
- wall/problem modification endpoints (`/api/home/...`, including `PATCH` reset and problem delete)
- solution beta upload flow (`GET /api/discussion/solution-beta/upload-url` → GCS PUT → `POST .../save`)

These areas should always be included in release smoke testing.

## Environment Strategy

- Local development test baseline:
  - backend: `./scripts/test-with-postgres.sh`
  - frontend: `npm test`
- PostgreSQL test DB bootstrap is automated via `server/scripts/reset-test-db.sh` (used by local and CI runs).
- Local Docker-backed bootstrap path is `server/scripts/start-local-test-db.sh` (default host port `55432`).
- Integration tests must always run against isolated PostgreSQL test DB (`v_beta_test`), never shared production-like data.
- Cloud dependencies should be mocked or isolated in automated tests when possible.

## Entry and Exit Criteria

### Before merging major feature changes

- Relevant backend tests pass.
- New/changed authorization paths are covered by tests.
- Manual smoke tests pass for affected flows.

### Before release

- Full backend test suite passes.
- Core manual regression scenarios pass:
  - login/signup/verify/reset
  - wall browse and problem detail
  - role-gated actions
  - account management critical paths

## Planned Improvements

- Expand frontend automation from page-level behavior tests into auth-form coverage and end-to-end testing.
- Increase contract-style tests for critical API responses and error behavior.
- Introduce standardized test-data fixtures for repeatable integration tests.

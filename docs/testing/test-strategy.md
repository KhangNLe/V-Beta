# Test Strategy

## Goal

This strategy defines how we validate correctness, authorization behavior, and regression safety for the capstone system across:

- frontend (`v-beta/`)
- backend (`server/`)
- integration points between them

## Current Test Stack

### Backend (`server/`)

- Framework: JUnit 5 + Spring Boot Test
- Test styles in repo:
  - application/context smoke tests
  - controller/service tests with MockMvc and mocked dependencies
  - integration-style tests under `Integration_Test/*`
- Profiles/config:
  - `application-test.yml` uses H2 by default
  - multiple integration tests override datasource to MySQL test DB via `@TestPropertySource`

Run command:

```bash
cd server
./mvnw test
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
  - mutation and failure-path tests for account role changes, wall actions, comments, and beta upload workflows
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

Target outcomes:

- deterministic logic checks
- fast feedback on permission regressions

### 2) Backend Integration Tests

Focus:

- end-to-end service/controller behavior with realistic persistence interactions
- wall/problem lifecycle operations
- comment creation/deletion behavior
- solution beta metadata creation/deletion behavior

Target outcomes:

- endpoint-to-service-to-repository correctness
- regression protection for critical domain flows

### 3) Frontend Component/Flow Tests

Priority targets:

- auth forms (login(to be added)/signup(to be added)/verify/reset) state and validation behavior
- role-based UI visibility (guest/climber/setter/admin)
- wall/problem page interaction states (loading, error, empty, success)
- account page behaviors (including delete flow and role-management entry points when applicable)

Target outcomes:

- stable user-visible behavior during UI refactors
- confidence in auth/role gating in client UX
- better regression protection for async mutation and error branches in critical pages

### 4) Manual End-to-End Validation

Because this is a capstone with active feature evolution, manual smoke/regression checks remain required each release.

Manual checklist details should live in:

- `docs/testing/manual-test-cases.md`

## Risk-Based Priorities

Highest regression risk areas:

- authentication/session sync (`/api/accounts/session`)
- authorization and role enforcement (`CHANGE_ROLE`, `VIEW_ACCOUNTS`, `DELETE_COMMENT`, etc.)
- wall/problem modification endpoints
- solution beta upload flow (signed URL -> upload -> metadata save)

These areas should always be included in release smoke testing.

## Environment Strategy

- Local development test baseline:
  - backend: `./mvnw test`
  - frontend: `npm test`
- Integration tests that require MySQL should use isolated test DB (`MYSQL_TEST_DB`) and never run against shared production-like data.
- Cloud dependencies should be mocked or isolated in automated tests when possible.

## Entry and Exit Criteria

### Before merging major feature changes

- Relevant backend tests pass.
- New/changed authorization paths are covered by tests.
- Manual smoke tests pass for affected flows.

### Before release/demo

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
- Add CI gating so tests run automatically on pull requests.

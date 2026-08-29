# Test Environments

This document defines testing environments and expected configuration differences.

## 1) Local Development Environment

### Purpose

- Day-to-day development and manual regression testing.

### Typical Setup

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Database: Cloud SQL PostgreSQL via proxy (or local PostgreSQL equivalent)
- Firebase: development/shared project credentials

### Required Services

- `v-beta` frontend server
- `server` backend process
- Cloud SQL Auth Proxy (if using Cloud SQL)
- Credentials files for Firebase/GCP as configured in env docs

### Notes

- Most manual test cases in `manual-test-cases.md` are run here first.

## 2) Backend Test Profile Environment

### Purpose

- Automated backend test execution.

### Profile

- Spring profile: `test`
- Config source: `server/src/test/resources/application-test.yml`

### Behavior

- Default test profile uses H2 in-memory DB config.
- Integration tests under `Integration_Test/*` connect to PostgreSQL **`v_beta_test`** via `TEST_DB_*` only (they do not read runtime `DB_NAME`, `DB_PORT`, or `SQL_*` from `.env`). Local default host port is `55432`.
- Standard bootstrap script: `server/scripts/reset-test-db.sh` (recreates `v_beta_test` and applies schema/seed; refuses to drop `v_beta`).
- One-command local backend run: `server/scripts/test-with-postgres.sh`.
- Local Docker bootstrap helper: `server/scripts/start-local-test-db.sh` (default `DB_PORT=55432`).
- GitHub backend CI provisions PostgreSQL service on `127.0.0.1:5432`.

### Notes

- Integration tests are expected to run against isolated PostgreSQL test DB (`v_beta_test`) with scripted reset.

## 3) Frontend Test Environment

### Purpose

- Component/unit-style testing with Jest.

### Stack

- Jest + `next/jest`
- `jsdom` environment
- `cross-fetch` polyfill
- Testing Library matchers

### Notes

- Frontend unit-style testing exists, but could be expanded to every page and include end-to-end testing.

## 4) Environment Comparison (Quick View)

| Dimension | Local Dev | Backend Test Profile | Frontend Test |
|---|---|---|---|
| Target | Manual + integration behavior | Automated backend tests | Automated frontend tests |
| Backend URL | `localhost:8080` | Test runtime context | Mocked/jsdom context |
| DB | Cloud SQL/PostgreSQL (typical local) | H2 by default; Integration_Test overrides to PostgreSQL with scripted `v_beta_test` bootstrap (`55432` local Docker default, `5432` in CI) | N/A |
| Auth | Firebase real tokens (manual flow) | Test setup/mocks/profile-driven | Usually mocked or not full auth e2e |
| Main Runner | Manual + scripts | `./scripts/test-with-postgres.sh` | `npm test` |

## 5) Environment Validation Checklist

- [ ] Correct env files loaded (`server/.env`, `v-beta/.env.local`)
- [ ] Backend health endpoint returns success
- [ ] Frontend can call backend API base URL
- [ ] Firebase config values are valid for selected environment
- [ ] DB/proxy connectivity is confirmed before manual regression

## 6) Related Docs

- [docs/setup/local-development.md](../setup/local-development.md)
- [docs/setup/environment-variables.md](../setup/environment-variables.md)
- [docs/testing/test-strategy.md](./test-strategy.md)
- [docs/testing/manual-test-cases.md](./manual-test-cases.md)

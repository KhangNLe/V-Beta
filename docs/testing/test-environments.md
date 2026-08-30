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

## 4) Hosted Staging Environment

### Purpose

- Public demo / stakeholder walkthrough against live URLs.

### Typical Setup

- Frontend: Vercel ([https://v-beta-mncoop.vercel.app/](https://v-beta-mncoop.vercel.app/))
- Backend: Cloud Run ([https://v-beta-api-6vqd6rspuq-ue.a.run.app](https://v-beta-api-6vqd6rspuq-ue.a.run.app))
- Database: Neon PostgreSQL (`v_beta`, pooler `ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech`, `sslmode=require`)
- Firebase: shared/development project (authorized domain includes the Vercel hostname)
- Storage: existing GCS public bucket (bucket CORS allows the Vercel origin)

### Notes

- First request after idle can be slow (Neon suspend + Cloud Run scale-to-zero).
- Schema must be applied to Neon before the API boots (`ddl-auto=validate`).
- Full provision and deploy steps: [docs/setup/deployment.md](../setup/deployment.md).

## 5) Environment Comparison (Quick View)

| Dimension | Local Dev | Backend Test Profile | Frontend Test | Hosted Staging |
|---|---|---|---|---|
| Target | Manual + integration behavior | Automated backend tests | Automated frontend tests | Public demo / live smoke |
| Backend URL | `localhost:8080` | Test runtime context | Mocked/jsdom context | Cloud Run HTTPS URL |
| DB | Cloud SQL/PostgreSQL (typical local) | H2 by default; Integration_Test overrides to PostgreSQL with scripted `v_beta_test` bootstrap (`55432` local Docker default, `5432` in CI) | N/A | Neon `v_beta` (us-east-2 pooler, `sslmode=require`) |
| Auth | Firebase real tokens (manual flow) | Test setup/mocks/profile-driven | Usually mocked or not full auth e2e | Firebase real tokens; Vercel domain authorized |
| Main Runner | Manual + scripts | `./scripts/test-with-postgres.sh` | `npm test` | Vercel + `gcloud run deploy` |

## 6) Environment Validation Checklist

- [ ] Correct env files loaded (`server/.env`, `v-beta/.env.local`) **or** hosted dashboard env (Vercel / Cloud Run)
- [ ] Backend health endpoint returns success
- [ ] Frontend can call backend API base URL
- [ ] Firebase config values are valid for selected environment
- [ ] DB connectivity is confirmed (local proxy/Postgres, or Neon SSL for staging) before manual regression

## 7) Related Docs

- [docs/setup/local-development.md](../setup/local-development.md)
- [docs/setup/environment-variables.md](../setup/environment-variables.md)
- [docs/setup/deployment.md](../setup/deployment.md)
- [docs/testing/test-strategy.md](./test-strategy.md)
- [docs/testing/manual-test-cases.md](./manual-test-cases.md)

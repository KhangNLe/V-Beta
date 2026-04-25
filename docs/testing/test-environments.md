# Test Environments

This document defines testing environments and expected configuration differences.

## 1) Local Development Environment

### Purpose

- Day-to-day development and manual regression testing.

### Typical Setup

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Database: Cloud SQL via proxy (or local MySQL equivalent)
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
- Some integration tests override datasource properties to target MySQL test DB.

### Notes

- Do not assume all tests are fully isolated from external DB unless validated per test class.

## 3) Frontend Test Environment

### Purpose

- Component/unit-style testing with Jest.

### Stack

- Jest + `next/jest`
- `jsdom` environment
- `cross-fetch` polyfill
- Testing Library matchers

### Notes

- Frontend test infrastructure exists, but current suite coverage is limited and should be expanded.

## 4) Environment Comparison (Quick View)

| Dimension | Local Dev | Backend Test Profile | Frontend Test |
|---|---|---|---|
| Target | Manual + integration behavior | Automated backend tests | Automated frontend tests |
| Backend URL | `localhost:8080` | Test runtime context | Mocked/jsdom context |
| DB | Cloud SQL/MySQL (typical local) | H2 by default; some MySQL overrides | N/A |
| Auth | Firebase real tokens (manual flow) | Test setup/mocks/profile-driven | Usually mocked or not full auth e2e |
| Main Runner | Manual + scripts | `./mvnw test` | `npm test` |

## 5) Environment Validation Checklist

- [ ] Correct env files loaded (`server/.env`, `v-beta/.env.local`)
- [ ] Backend health endpoint returns success
- [ ] Frontend can call backend API base URL
- [ ] Firebase config values are valid for selected environment
- [ ] DB/proxy connectivity is confirmed before manual regression

## 6) Related Docs

- [docs/setup/local-development.md](../setup/local-development.md)
- [docs/setup/enviroment-variables.md](../setup/enviroment-variables.md)
- [docs/testing/test-strategy.md](./test-strategy.md)
- [docs/testing/manual-test-cases.md](./manual-test-cases.md)

# Backend (`server`)

Spring Boot REST API for V-Beta. It pairs with the Next.js app in `../v-beta`.

## Overview

- Framework: Spring Boot + Spring Security + Spring Data JPA
- Database: PostgreSQL — local `127.0.0.1` for day-to-day runs; **Neon** for hosted staging; integration tests on isolated `v_beta_test`
- Integrations: Firebase Admin SDK and Google Cloud Storage

REST surface includes account, wall/problem, discussion, reports, notifications, moderation logbook, and appeals. See [API endpoints](../docs/api/endpoints.md) and [Moderation](../docs/features/moderation.md).

## Prerequisites

- **JDK 17+** (JDK 21 is also supported).
- **PostgreSQL 14+** reachable by the backend when running locally (or Neon if you intentionally point `.env` at staging).
- Optional for storage/auth flows: Firebase and Google Cloud credentials.
- Optional for one-command backend PostgreSQL test runs: **Docker** (used by `scripts/test-with-postgres.sh`).

Use the Maven wrapper (`./mvnw` or `mvnw.cmd`) so no global Maven install is required.

## Runtime Configuration

The backend reads environment variables from `server/.env` via:

- `spring.config.import=optional:file:.env[.properties]`

Key config files:

- `src/main/resources/application.properties` (datasource, JPA, Firebase, GCP settings)
- `src/main/resources/application.yml` (app name, port, CORS)

### Required Environment Variables

Minimum DB variables for local boot:

- `DB_HOST` (default fallback: `127.0.0.1`)
- `DB_PORT` (default fallback in properties: `5432`)
- `DB_NAME` (default fallback: `v_beta`)
- `SQL_USERNAME` (**required**, no fallback)
- `SQL_PASSWORD` (**required**, no fallback)
- `DB_JDBC_PARAMS` (optional JDBC URL suffix; leave empty locally. Neon: `?sslmode=require&channel_binding=require`)

Additional variables used by Firebase/GCP integrations and hosting:

- `FIREBASE_CREDENTIALS_PATH`
- `GCP_PROJECT_ID`
- `GOOGLE_SERVICE_CREDENTIALS_PATH`
- `STORAGE_PUBLIC_BUCKET_NAME`
- `CORS_ALLOWED_ORIGINS` (comma-separated; default `http://localhost:3000`)
- `PORT` (Cloud Run injects this; local default `8080`)

Example `server/.env`:

```env
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=v_beta
SQL_USERNAME=postgres
SQL_PASSWORD=devpassword
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
GCP_PROJECT_ID=your-gcp-project-id
GOOGLE_SERVICE_CREDENTIALS_PATH=./google-account-credential.json
STORAGE_PUBLIC_BUCKET_NAME=your-public-bucket
# CORS_ALLOWED_ORIGINS=http://localhost:3000
# DB_JDBC_PARAMS=
```

### Databases (local vs Neon)

Do **not** use a git branch to switch databases. `server/.env` is gitignored; Cloud Run has its own env. Same code, same branch.

| Environment | Database | Typical `server/.env` / Cloud Run |
|---|---|---|
| Local daily work | Postgres on `127.0.0.1` (or Cloud SQL Auth Proxy) | `DB_HOST=127.0.0.1`, empty `DB_JDBC_PARAMS` |
| Hosted staging | Neon `v_beta` (AWS `us-east-2`, **pooler**) | See Neon values below |
| `./mvnw test` | Docker/CI `v_beta_test` | `TEST_DB_*` only — ignores runtime `DB_*` / `SQL_*` |

**Neon staging** (password lives in Neon console / Secret Manager / local `.env`, never in git):

| Variable | Staging value |
|---|---|
| `DB_HOST` | `ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `v_beta` |
| `SQL_USERNAME` | `neondb_owner` |
| `SQL_PASSWORD` | Neon role password (not committed) |
| `DB_JDBC_PARAMS` | `?sslmode=require&channel_binding=require` |

Host is the **pooler** hostname (needed for Cloud Run’s many short connections). Apply schema with [`src/main/resources/db/pg-v-beta.sql`](src/main/resources/db/pg-v-beta.sql) to database `v_beta` before the API boots. Setup steps: [Database schema](../docs/setup/database-schema.md) and [Deployment](../docs/setup/deployment.md).

To poke Neon from a laptop without replacing local settings, copy a gitignored `server/.env.neon` over `.env` temporarily; restore `127.0.0.1` when done.

## Run the Application

From `server/`:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

Default API base URL: `http://localhost:8080`

Quick checks:

- `GET http://localhost:8080/api/health`
- `GET http://localhost:8080/api/v1/meta`

Main class: `app.VBeta.VBetaApplication`

## Run Tests

The backend test suite includes integration tests that connect to PostgreSQL **`v_beta_test`**, not the runtime database (`v_beta`). They read `TEST_DB_*` / `TEST_SQL_*` only — never `DB_NAME`, `DB_PORT`, or `SQL_*` from `server/.env` — so `./mvnw test` cannot hit your running app DB.

Local default: `127.0.0.1:55432` (Docker from the scripts below), user `postgres`. CI sets `TEST_DB_PORT=5432`.

For consistent local runs (bootstrap + tests):

```bash
./scripts/test-with-postgres.sh
```

What this command does:

- starts/reuses local Docker PostgreSQL (`vbeta-test-postgres` on `127.0.0.1:55432`)
- recreates `v_beta_test` (never `v_beta`)
- applies `src/test/resources/db/v_beta_test_schema.sql` (schema + seed data)
- runs `./mvnw test` with `TEST_DB_*` wiring

After the Docker test DB is already up, `./mvnw test` uses it by default. To reset schema/seed first:

```bash
./scripts/start-local-test-db.sh
./mvnw test
```

`./scripts/reset-test-db.sh` also defaults to `127.0.0.1:55432` as user `postgres` (not `SQL_*` / `DB_PORT` from `.env`). CI overrides `TEST_DB_PORT=5432`.

## Build a Runnable JAR

```bash
./mvnw -DskipTests package
java -jar target/team-satisfaction-server-0.0.1-SNAPSHOT.jar
```

## Notes

- `spring.jpa.hibernate.ddl-auto=validate` is enabled in runtime config, so your PostgreSQL schema must already exist and match entity mappings.
- Local PostgreSQL SSL is commonly omitted in the JDBC URL for development. Neon (hosted or laptop-against-staging) requires `DB_JDBC_PARAMS=?sslmode=require&channel_binding=require`.
- Cloud Run packaging uses `server/Dockerfile`. Do not copy credential JSON into the image; see [Setup: Deployment](../docs/setup/deployment.md).

## Related Docs

- [Project docs index](../docs/README.md)
- [User Manual](../docs/user-manual.md)
- [Moderation](../docs/features/moderation.md)
- [Setup: Environment Variables](../docs/setup/environment-variables.md)
- [Setup: Local Development](../docs/setup/local-development.md)
- [Setup: Database Schema](../docs/setup/database-schema.md)
- [Setup: Firebase](../docs/setup/firebase-setup.md)
- [Setup: Google Cloud](../docs/setup/google-cloud-setup.md)
- [Setup: Deployment](../docs/setup/deployment.md)
- [Architecture: Backend](../docs/architecture/backend-architecture.md)
- [Architecture: Data Model](../docs/architecture/data-model.md)
- [API: Endpoints](../docs/api/endpoints.md)
- [API: Request/Response Examples](../docs/api/request-response-examples.md)
- [API: Permissions Matrix](../docs/api/permissions-matrix.md)
- [API: Error Handling](../docs/api/error-handling.md)
- [Testing: Strategy](../docs/testing/test-strategy.md)
- [Testing: Server Test Report](../docs/testing/server-test-report.md)

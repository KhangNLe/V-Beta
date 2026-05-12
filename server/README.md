# Backend (`server`)

Spring Boot REST API for V-Beta. It pairs with the Next.js app in `../v-beta`.

## Overview

- Framework: Spring Boot + Spring Security + Spring Data JPA
- Database: PostgreSQL (runtime), H2 (tests)
- Integrations: Firebase Admin SDK and Google Cloud Storage

## Prerequisites

- **JDK 17+** (JDK 21 is also supported).
- **PostgreSQL 14+** reachable by the backend when running locally.
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

Additional variables used by Firebase/GCP integrations:

- `FIREBASE_CREDENTIALS_PATH`
- `GCP_PROJECT_ID`
- `GOOGLE_SERVICE_CREDENTIALS_PATH`
- `STORAGE_PUBLIC_BUCKET_NAME`

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
```

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

Main class: `edu.ics499.VBeta.VBetaApplication`

## Run Tests

The backend test suite includes integration tests that use PostgreSQL datasource overrides.

For consistent local runs without manual DB setup, use:

```bash
./scripts/test-with-postgres.sh
```

What this command does:

- starts/reuses local Docker PostgreSQL (`vbeta-test-postgres` on `127.0.0.1:55432`)
- recreates `v_beta_test`
- applies `src/test/resources/db/v_beta_test_schema.sql` (schema + seed data)
- runs `./mvnw test` with PostgreSQL test env wiring

Alternative (if you already have PostgreSQL running and configured):

```bash
./scripts/reset-test-db.sh
./mvnw test
```

## Build a Runnable JAR

```bash
./mvnw -DskipTests package
java -jar target/team-satisfaction-server-0.0.1-SNAPSHOT.jar
```

## Notes

- `spring.jpa.hibernate.ddl-auto=validate` is enabled in runtime config, so your PostgreSQL schema must already exist and match entity mappings.
- Local PostgreSQL SSL is commonly disabled in JDBC URL for development (`sslmode=disable`) unless your environment requires TLS.

## Related Docs

- [Project docs index](../docs/README.md)
- [User Manual](../docs/user-manual.md)
- [Setup: Environment Variables](../docs/setup/environment-variables.md)
- [Setup: Local Development](../docs/setup/local-development.md)
- [Setup: Database Schema](../docs/setup/database-schema.md)
- [Setup: Firebase](../docs/setup/firebase-setup.md)
- [Setup: Google Cloud](../docs/setup/google-cloud-setup.md)
- [Architecture: Backend](../docs/architecture/backend-architecture.md)
- [Architecture: Data Model](../docs/architecture/data-model.md)
- [API: Endpoints](../docs/api/endpoints.md)
- [API: Error Handling](../docs/api/error-handling.md)
- [Testing: Strategy](../docs/testing/test-strategy.md)
- [Testing: Server Test Report](../docs/testing/server-test-report.md)

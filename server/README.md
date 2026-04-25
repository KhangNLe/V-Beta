# Backend (`server`)

Spring Boot REST API for the capstone project. It pairs with the Next.js app in `../v-beta`.

## Overview

- Framework: Spring Boot + Spring Security + Spring Data JPA
- Database: MySQL (runtime), H2 (tests)
- Integrations: Firebase Admin SDK and Google Cloud Storage

## Prerequisites

- **JDK 17+** (JDK 21 is also supported).
- **MySQL 8** reachable by the backend when running locally.
- Optional for storage/auth flows: Firebase and Google Cloud credentials.

Use the Maven wrapper (`./mvnw` or `mvnw.cmd`) so no global Maven install is required.

## Runtime Configuration

The backend reads environment variables from `server/.env` via:

- `spring.config.import=optional:file:.env[.properties]`

Key config files:

- `src/main/resources/application.properties` (datasource, JPA, Firebase, GCP settings)
- `src/main/resources/application.yml` (app name, port, CORS)

### Required Environment Variables

Minimum DB variables for local boot:

- `MYSQL_HOST` (default fallback: `localhost`)
- `MYSQL_PORT` (default fallback in properties: `3307`)
- `MYSQL_DB` (default fallback: `V_Beta`)
- `SQL_USERNAME` (**required**, no fallback)
- `SQL_PASSWORD` (**required**, no fallback)

Additional variables used by Firebase/GCP integrations:

- `FIREBASE_CREDENTIALS_PATH`
- `GCP_PROJECT_ID`
- `GOOGLE_SERVICE_CREDENTIALS_PATH`
- `STORAGE_PUBLIC_BUCKET_NAME`

Example `server/.env`:

```env
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DB=V_Beta
SQL_USERNAME=root
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

Tests use `src/test/resources/application-test.yml` and run against in-memory H2 (not MySQL).

```bash
./mvnw test
```

Windows:

```bash
mvnw.cmd test
```

## Build a Runnable JAR

```bash
./mvnw -DskipTests package
java -jar target/team-satisfaction-server-0.0.1-SNAPSHOT.jar
```

## Notes

- `spring.jpa.hibernate.ddl-auto=validate` is enabled in runtime config, so your MySQL schema must already exist and match entity mappings.
- If you use Cloud SQL Auth Proxy in local setup, point `MYSQL_HOST`/`MYSQL_PORT` to the proxy endpoint (commonly `127.0.0.1:3306`).

## Related Docs

- [Project docs index](../docs/README.md)
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

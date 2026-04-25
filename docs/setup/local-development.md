# Local Development Setup

This guide explains how to run the full project locally:

- `server/` (Spring Boot API)
- `v-beta/` (Next.js frontend)
- Cloud SQL Auth Proxy (for database connectivity)

## Read These First

Before starting, complete:

1. [docs/setup/environment-variables.md](./environment-variables.md)
2. [docs/setup/firebase-setup.md](./firebase-setup.md)
3. [docs/setup/google-cloud-setup.md](./google-cloud-setup.md)
4. [docs/setup/database-schema.md](./database-schema.md)

## Prerequisites

- Node.js + npm (for `v-beta/`)
- JDK 17+ (for `server/`)
- Google Cloud SQL Auth Proxy installed
- Valid Google service-account JSON files for:
  - Firebase Admin (`FIREBASE_CREDENTIALS_PATH`)
  - GCP services/proxy auth (`GOOGLE_SERVICE_CREDENTIALS_PATH`)

## 1) Configure environment files

### Frontend env file

Create `v-beta/.env.local` with Firebase + API URL values.

At minimum:

```bash
NEXT_PUBLIC_FIREBASE_API_KEY=
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=
NEXT_PUBLIC_FIREBASE_PROJECT_ID=
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=
NEXT_PUBLIC_FIREBASE_APP_ID=
NEXT_PUBLIC_FIREBASE_MEASUREMENT_ID=
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_ORIGIN=http://localhost:3000
```

### Backend env file

Create `server/.env` with DB + Firebase Admin + GCP values.

At minimum:

```bash
SQL_USERNAME=
SQL_PASSWORD=
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DB=V_Beta
MYSQL_TEST_DB=V_Beta_Test
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
GCP_PROJECT_ID=
GOOGLE_SERVICE_CREDENTIALS_PATH=./google-account-credential.json
STORAGE_PUBLIC_BUCKET_NAME=
```

Note: `application.properties` defaults to `3307`, so keep `MYSQL_PORT` in `server/.env` synced with your proxy `--port`.

## 2) Start Cloud SQL Auth Proxy

From project root (or any directory with correct credential path):

```bash
cloud-sql-proxy \
  --credentials-file "./server/google-account-credential.json" \
  "PROJECT_ID:REGION:INSTANCE_NAME" \
  --port 3306
```

Keep this terminal running.

If you change proxy port, update `MYSQL_PORT` in `server/.env` to match.

## 3) Start backend (`server/`)

Open a new terminal:

```bash
cd server
./mvnw spring-boot:run
```

Backend should be available at `http://localhost:8080`.

Quick checks:

- `GET http://localhost:8080/api/health`
- `GET http://localhost:8080/api/v1/meta`

## 4) Start frontend (`v-beta/`)

Open another terminal:

```bash
cd v-beta
npm install
npm run dev
```

Frontend should be available at `http://localhost:3000`.

## 5) Smoke test checklist

- App loads at `http://localhost:3000`
- Login page renders without Firebase errors
- Email/password login or signup works
- Google login popup works
- Backend-protected requests return data (not CORS/auth errors)
- Account page loads user info

## Common startup issues

- **Proxy running on different port**
  - Ensure proxy `--port` matches `MYSQL_PORT` in `server/.env`.

- **Backend fails DB connection**
  - Verify Cloud SQL proxy is running and SQL credentials are correct.

- **Frontend cannot reach backend**
  - Check `NEXT_PUBLIC_API_BASE_URL` and confirm backend is on port `8080`.

- **Firebase auth errors**
  - Re-check Firebase web config in `v-beta/.env.local` and authorized domains.

- **Credential file not found**
  - Verify paths in `FIREBASE_CREDENTIALS_PATH` and `GOOGLE_SERVICE_CREDENTIALS_PATH`.

## Optional test commands

Backend tests:

```bash
cd server
./mvnw test
```

Frontend tests:

```bash
cd v-beta
npm test
```

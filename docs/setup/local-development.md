# Local Development Setup

This guide explains how to run the full project locally:

- `server/` (Spring Boot API)
- `v-beta/` (Next.js frontend)
- PostgreSQL via Docker Desktop (recommended) or a local install / Cloud SQL Auth Proxy


## Read These First

Before starting, complete:

1. [docs/setup/environment-variables.md](./environment-variables.md)
2. [docs/setup/firebase-setup.md](./firebase-setup.md)
3. [docs/setup/google-cloud-setup.md](./google-cloud-setup.md)
4. [docs/setup/database-schema.md](./database-schema.md)

Hosted staging (Vercel + Cloud Run + Neon) is a separate path: [docs/setup/deployment.md](./deployment.md).

## Prerequisites

- Node.js + npm (for `v-beta/`)
- JDK 17+ (for `server/`)
- Docker Desktop (recommended for local PostgreSQL) **or** PostgreSQL installed locally **or** Google Cloud SQL Auth Proxy
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
SQL_USERNAME=postgres
SQL_PASSWORD=postgres
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=v_beta
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
GCP_PROJECT_ID=
GOOGLE_SERVICE_CREDENTIALS_PATH=./google-account-credential.json
STORAGE_PUBLIC_BUCKET_NAME=
```

For Docker local DB (Option A below), use `SQL_USERNAME=postgres` / `SQL_PASSWORD=postgres` unless you override those when starting the container.

Note: `application.properties` defaults to PostgreSQL on `5432`, so keep `DB_PORT` in `server/.env` synced with your Docker publish port, local install, or proxy `--port`.

Keep local `.env` on `127.0.0.1`. Neon staging (`ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech`, database `v_beta`) is for Cloud Run, not a second git branch. See [server/README.md](../server/README.md) and [database-schema.md](./database-schema.md).

## 2) Start PostgreSQL connection path

### Option A (recommended local): Docker PostgreSQL

Requires **Docker Desktop** running. From `server/`:

```bash
./scripts/start-local-db.sh
```

```powershell
.\scripts\start-local-db.ps1
```

What this does:

- Starts/reuses container `vbeta-postgres` on host port `5432`
- Persists data in Docker volume `vbeta-postgres-data`
- Creates database `v_beta` if missing
- Applies `src/main/resources/db/pg-v-beta.sql` only when schema is missing (safe to re-run)

Match `server/.env` to the container defaults:

```bash
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=v_beta
SQL_USERNAME=postgres
SQL_PASSWORD=postgres
```

To wipe and re-seed the runtime DB (destructive):

```bash
./scripts/reset-local-db.sh
```

```powershell
.\scripts\reset-local-db.ps1
```

**Port conflict (Windows):** if a native PostgreSQL service is already listening on `5432`, Spring may connect to that instance instead of Docker and fail auth/dialect startup. Either stop the Windows service (`postgresql-x64-*`) or publish Docker on another port and update `.env`:

```powershell
$env:DB_PORT = "5433"
.\scripts\start-local-db.ps1
# then set DB_PORT=5433 in server/.env
```

**Integration tests** use a separate container (`vbeta-test-postgres` on `55432`) via `start-local-test-db.*`. Do not point runtime `DB_*` at the test DB.

#### Change a user's role in Docker (local testing)

Seed roles: `CLIMBER` = 1, `SETTER` = 2, `ADMIN` = 3.

1. Sign in once through the app so your Firebase user exists in `user_account` (or use a seeded row).
2. Open `psql` in the runtime container:

```powershell
docker exec -it vbeta-postgres psql -U postgres -d v_beta
```

```bash
docker exec -it vbeta-postgres psql -U postgres -d v_beta
```

3. Inspect users:

```sql
SELECT u.user_id, u.username, u.email, u.firebase_uid, r.role_type
FROM user_account u
LEFT JOIN gym_role r ON r.role_id = u.gym_role_id;
```

4. Promote/demote by Firebase UID or email:

```sql
UPDATE user_account
SET gym_role_id = (SELECT role_id FROM gym_role WHERE role_type = 'ADMIN')
WHERE firebase_uid = 'YOUR_FIREBASE_UID';

-- or SETTER / CLIMBER
UPDATE user_account
SET gym_role_id = (SELECT role_id FROM gym_role WHERE role_type = 'SETTER')
WHERE email = 'you@example.com';
```

One-liner:

```powershell
docker exec -i vbeta-postgres psql -U postgres -d v_beta -c "UPDATE user_account SET gym_role_id = 3 WHERE firebase_uid = 'YOUR_FIREBASE_UID';"
```

5. Refresh the session (log out/in or reload) so the API returns the new role.

Seeded sample accounts from `pg-v-beta.sql` (fake Firebase UIDs): `testUser` / climber, `testSetter` / setter, `testAdmin` / admin. For real login testing, update the row that matches your Firebase account.

### Option B: local PostgreSQL install

Ensure PostgreSQL is running and `DB_HOST`/`DB_PORT` in `server/.env` points to it. Apply schema per [database-schema.md](./database-schema.md).

### Option C: use Cloud SQL Auth Proxy

From project root (or any directory with correct credential path):

```bash
cloud-sql-proxy \
  --credentials-file "./server/google-account-credential.json" \
  "PROJECT_ID:REGION:INSTANCE_NAME" \
  --port 5432
```

Keep this terminal running.

If you change proxy port, update `DB_PORT` in `server/.env` to match.

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
  - Ensure proxy `--port` matches `DB_PORT` in `server/.env`.

- **Backend fails DB connection / Hibernate dialect error**
  - Confirm Docker Desktop is running and `.\scripts\start-local-db.ps1` (or `.sh`) succeeded.
  - Confirm `server/.env` matches the container (`postgres` / `postgres` on `127.0.0.1:5432` by default).
  - On Windows, check nothing else owns port `5432` (native `postgresql-x64-*` service). Stop that service or move Docker to another port and update `DB_PORT`.
  - Run Spring Boot from `server/` so `.env` is loaded.

- **Frontend cannot reach backend**
  - Check `NEXT_PUBLIC_API_BASE_URL` and confirm backend is on port `8080`.

- **Firebase auth errors**
  - Re-check Firebase web config in `v-beta/.env.local` and authorized domains.

- **Credential file not found**
  - Verify paths in `FIREBASE_CREDENTIALS_PATH` and `GOOGLE_SERVICE_CREDENTIALS_PATH`.

## Optional test commands

Backend tests (CI-like PostgreSQL bootstrap, recommended):

```bash
cd server
./scripts/test-with-postgres.sh
```

On Windows PowerShell (recommended when WSL Docker integration is off):

```powershell
cd server
.\scripts\test-with-postgres.ps1
```

Manual two-step backend test path:

```bash
cd server
./scripts/start-local-test-db.sh
./mvnw test
```

```powershell
cd server
.\scripts\start-local-test-db.ps1
.\mvnw.cmd test
```

Notes:

- `start-local-test-db.sh` / `.ps1` defaults to Docker Postgres on host port `55432`.
- Docker Desktop must be running. For the `.sh` scripts inside WSL, enable that distro under Docker Desktop → Settings → Resources → WSL Integration.
- GitHub backend CI uses service Postgres on `5432`; this local path is functionally equivalent.

Backend tests (without bootstrap, only when DB is already prepared):

```bash
cd server
./mvnw test
```

Frontend tests:

```bash
cd v-beta
npm test
```

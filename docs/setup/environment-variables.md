# Environment Variables Setup

This project uses environment variables in both apps:

- `v-beta/` (Next.js frontend) uses `.env.local`
- `server/` (Spring Boot backend) uses `.env` (loaded by `spring.config.import=optional:file:.env[.properties]`)

Configure both before running the full stack locally.

## Frontend (`v-beta/.env.local`)

### Quick Setup

1. Get access to the Firebase project used by your environment.
2. In the `v-beta/` root, create a file named `.env.local`.
3. Copy the template below into `.env.local`.
4. Replace placeholder values with your Firebase project values.
5. Restart the Next.js dev server after edits.

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

### Frontend Variable Reference

- `NEXT_PUBLIC_FIREBASE_API_KEY`: Firebase web app API key.
- `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN`: Firebase auth domain (example: `your-project.firebaseapp.com`).
- `NEXT_PUBLIC_FIREBASE_PROJECT_ID`: Firebase project ID.
- `NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET`: Firebase storage bucket name.
- `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID`: Firebase messaging sender ID.
- `NEXT_PUBLIC_FIREBASE_APP_ID`: Firebase web app ID.
- `NEXT_PUBLIC_FIREBASE_MEASUREMENT_ID`: Firebase Analytics measurement ID (optional for local dev if analytics is not used).
- `NEXT_PUBLIC_API_BASE_URL`: Base URL for backend API requests.
  - Local default: `http://localhost:8080`
  - Use deployed backend URL in hosted environments.
- `NEXT_PUBLIC_APP_ORIGIN`: Canonical app origin for Firebase email action links.
  - Local default: `http://localhost:3000`
  - Use deployed frontend URL in hosted environments.

### Frontend Usage in Code

- `src/app/envExports.js` exports env values.
- `src/app/firebase.js` initializes Firebase from `NEXT_PUBLIC_FIREBASE_*`.
- `src/lib/authEmailSettings.js` uses `NEXT_PUBLIC_APP_ORIGIN` for verification/reset links.

## Backend (`server/.env`)

### Quick Setup

1. In the `server/` root, create a file named `.env`.
2. Copy the template below into `server/.env`.
3. Fill in database, Firebase admin credential path, and (if used) Google Cloud Storage values.
4. Restart the backend after edits.

```bash
SQL_USERNAME=
SQL_PASSWORD=
MYSQL_HOST=localhost
MYSQL_PORT=3307
MYSQL_DB=V_Beta
MYSQL_TEST_DB=V_Beta_Test

# Optional aliases used by some tests (fallback chain)
MYSQL_USERNAME=
MYSQL_PASSWORD=

FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
GCP_PROJECT_ID=
GOOGLE_SERVICE_CREDENTIALS_PATH=./google-account-credential.json
STORAGE_PUBLIC_BUCKET_NAME=
```

### Backend Variable Reference

- `SQL_USERNAME`: primary DB username used by `spring.datasource.username`.
- `SQL_PASSWORD`: primary DB password used by `spring.datasource.password`.
- `MYSQL_HOST`: DB host for datasource URL (default `localhost`).
- `MYSQL_PORT`: DB port for datasource URL (default `3307` in config).
- `MYSQL_DB`: main database name for app runtime (default `V_Beta`).
- `MYSQL_TEST_DB`: test database name used by integration tests (default fallback `V_Beta_Test`).
- `MYSQL_USERNAME`: optional alias used in test fallback chain.
- `MYSQL_PASSWORD`: optional alias used in test fallback chain.
- `FIREBASE_CREDENTIALS_PATH`: filesystem path to Firebase Admin SDK credentials JSON.
- `GCP_PROJECT_ID`: Google Cloud project ID.
- `GOOGLE_SERVICE_CREDENTIALS_PATH`: path to Google Cloud service account JSON.
- `STORAGE_PUBLIC_BUCKET_NAME`: public bucket used for file storage.

### Backend Usage in Code/Config

- `server/src/main/resources/application.properties` consumes all server env variables listed above.
- `server/env_example.txt` contains a backend env template.
- `server/src/test/java/.../Integration_Test/*` uses `MYSQL_*` and `SQL_*` fallback patterns for DB test setup.

## Validation Checklist (Full Stack)

- Frontend starts without Firebase config errors.
- Backend starts and connects to MySQL.
- Frontend requests hit `NEXT_PUBLIC_API_BASE_URL`.
- Auth flows work (login/signup/verification).
- `GET /api/health` returns healthy status from backend.

## Security Notes

- Do not commit `v-beta/.env.local` or `server/.env`.
- Do not commit service account JSON files.
- Prefer separate Firebase/GCP projects for development and production.
- Rotate keys immediately if secrets are exposed.

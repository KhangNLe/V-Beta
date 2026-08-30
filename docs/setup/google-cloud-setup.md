# Google Cloud Setup

This guide covers the two Google Cloud pieces used by this project:

1. **Google Cloud Storage bucket** for uploaded media
2. **Google Cloud SQL (PostgreSQL)** for **local/dev** persistence via Auth Proxy

Hosted staging uses **Neon** instead of Cloud SQL and runs the API on **Cloud Run**. Follow [`docs/setup/deployment.md`](./deployment.md) for that path. GCS bucket setup in Part A is still required for solution-beta videos.

Use this with:

- `docs/setup/environment-variables.md`
- `docs/setup/firebase-setup.md`
- `docs/setup/deployment.md` (hosted staging)

## Prerequisites

- A Google Cloud project you can access
- Billing enabled on that project
- A service account key JSON with required permissions
- Backend env file at `server/.env`

The backend reads these properties in `server/src/main/resources/application.properties`:

- `spring.cloud.gcp.project-id` <- `GCP_PROJECT_ID`
- `spring.cloud.gcp.credentials.location` <- `GOOGLE_SERVICE_CREDENTIALS_PATH`
- `app.public-bucket-name` <- `STORAGE_PUBLIC_BUCKET_NAME`
- datasource URL/user/password <- `DB_*` and `SQL_*`

## Part A: Google Cloud Storage Bucket Setup

### 1) Create a bucket

1. Open Google Cloud Console -> **Cloud Storage -> Buckets**.
2. Click **Create**.
3. Choose a globally unique bucket name (example: `team-satisfaction-public`).
4. Select region based on your backend deployment region.
5. Keep defaults unless your project has stricter requirements.

Save the bucket name for `STORAGE_PUBLIC_BUCKET_NAME`.

Hosted staging also needs **bucket CORS** so the Vercel origin can PUT signed uploads. Template: `server/gcs-cors.example.json`. Steps: [deployment.md](./deployment.md).

### 2) Create/configure service account

1. Open **IAM & Admin -> Service Accounts**.
2. Create or reuse a service account for backend access.
3. Grant storage permissions (minimum practical set):
   - `Storage Object Admin` on the target bucket (or broader while developing)
4. Create a JSON key and download it.
5. Place the key file in a secure local path, for example:
   - `server/google-account-credential.json`

Set in `server/.env`:

```bash
GCP_PROJECT_ID=your-gcp-project-id
GOOGLE_SERVICE_CREDENTIALS_PATH=./google-account-credential.json
STORAGE_PUBLIC_BUCKET_NAME=your-bucket-name
```

### 3) Verify backend storage wiring

The backend creates a GCS `Storage` client in:

- `server/src/main/java/app/VBeta/config/GcpConfig.java`

Storage operations are implemented in:

- `server/src/main/java/app/VBeta/application/support/discussion/beta/GcpFileStorageAdapter.java`

Expected behavior:

- Backend can generate signed PUT URLs
- Backend can construct public object URLs
- Backend can delete objects by bucket/key

If signed URL generation fails, check project ID, credentials path, and bucket IAM permissions first.

## Part C: Convert iPhone `.mov` betas to MP4

GCS does not transcode or play `.mov`. The HTML5 player only plays **MP4** and **WebM**. iPhone Photos often uploads `.mov`, so a small Cloud Run service converts those objects to `.mp4` and deletes the original.

Source: [`jobs/mov-to-mp4/`](../../jobs/mov-to-mp4/).

1. Build and deploy (same region as the video bucket, e.g. `us-east1`):

```bash
cd jobs/mov-to-mp4
gcloud builds submit --tag REGION-docker.pkg.dev/PROJECT_ID/v-beta/mov-to-mp4:latest
gcloud run deploy v-beta-mov-to-mp4 \
  --image REGION-docker.pkg.dev/PROJECT_ID/v-beta/mov-to-mp4:latest \
  --region REGION \
  --no-allow-unauthenticated \
  --memory 4Gi \
  --cpu 2 \
  --timeout 900 \
  --set-env-vars STORAGE_PUBLIC_BUCKET_NAME=BUCKET
```

2. Grant the Cloud Run runtime service account **Storage Object Admin** on the public video bucket (download `.mov`, upload `.mp4`, delete `.mov`).

3. Create an Eventarc trigger on **object finalized** for that bucket, destination `v-beta-mov-to-mp4`. The service ignores non-`.mov` objects.

The problem page waits for the sibling `.mp4` (HEAD) before saving discussion metadata, so the player never points at `.mov`.

## Part B: Google Cloud SQL (PostgreSQL) Setup

> Backend connects to Cloud SQL through Cloud SQL Auth Proxy using the JDBC host/port/database env vars (`DB_HOST`, `DB_PORT`, `DB_NAME`).

### 1) Create Cloud SQL instance

1. Open Google Cloud Console -> **SQL**.
2. Create a **PostgreSQL** instance.
3. Choose region and machine size appropriate for expected usage.
4. Create database (example: `v_beta`).
5. Create SQL user and password.

### 2) Use Cloud SQL Auth Proxy (required setup)

This project should connect to Cloud SQL through **Cloud SQL Auth Proxy**.

1. Get your instance connection name from Cloud SQL:
   - Format: `PROJECT_ID:REGION:INSTANCE_NAME`
2. Start proxy locally with your service-account JSON:

```bash
cloud-sql-proxy \
  --credentials-file "./google-account-credential.json" \
  "PROJECT_ID:REGION:INSTANCE_NAME" \
  --port 5432
```

Keep this process running while backend is running.

If your environment uses a different port, keep proxy and `DB_PORT` in sync.

Alternative auth method (same result) if you prefer env-based auth:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="./google-account-credential.json"
cloud-sql-proxy "PROJECT_ID:REGION:INSTANCE_NAME" --port 5432
```

### 3) Configure backend env vars

Set in `server/.env` (for proxy on local port `5432`):

```bash
SQL_USERNAME=your_sql_user
SQL_PASSWORD=your_sql_password
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=v_beta
```

### 4) Verify DB connection

1. Start backend from `server/`.
2. Confirm app boots without datasource/authentication errors.
3. Hit backend health endpoint: `GET /api/health`.
4. Perform a frontend flow that reads/writes database-backed data.

If connection fails:

- confirm SQL user/password
- confirm the Cloud SQL Auth Proxy process is running
- confirm proxy port matches `DB_PORT` in `server/.env`
- confirm database exists and user has privileges

## Combined `server/.env` Example (GCP + SQL)

```bash
SQL_USERNAME=your_sql_user
SQL_PASSWORD=your_sql_password
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=v_beta

GCP_PROJECT_ID=your-gcp-project-id
GOOGLE_SERVICE_CREDENTIALS_PATH=./google-account-credential.json
STORAGE_PUBLIC_BUCKET_NAME=your-bucket-name

FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
```

## Troubleshooting

- **`None` project or bucket errors at runtime**
  - `GCP_PROJECT_ID` or `STORAGE_PUBLIC_BUCKET_NAME` is missing in `server/.env`.

- **Cannot read Google credentials file**
  - Fix `GOOGLE_SERVICE_CREDENTIALS_PATH` to a valid local path.

- **`AccessDenied` or permission errors on bucket**
  - Grant storage roles to the service account used by backend credentials.

- **PostgreSQL connection refused / timeout**
  - Check Cloud SQL networking, auth proxy status, and `DB_HOST`/`DB_PORT`.

- **App starts but writes/queries fail**
  - Validate DB schema/state and user privileges for `v_beta`.

## Security notes

- Never commit `server/.env` or service account JSON keys.
- Restrict service account roles to least privilege.
- Avoid exposing Cloud SQL publicly unless necessary; prefer proxy/private networking when possible.

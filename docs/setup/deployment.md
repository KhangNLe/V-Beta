# Hosted Staging Deployment

This guide publishes a **staging/demo** environment:

- Frontend: Vercel (Next.js app in `v-beta/`) — live at [https://v-beta-mncoop.vercel.app/](https://v-beta-mncoop.vercel.app/)
- API: Google Cloud Run (Spring Boot in `server/`)
- Database: Neon PostgreSQL (`v_beta` on AWS us-east-2 pooler)
- Auth: existing Firebase project
- Video files: existing Google Cloud Storage bucket

Current hosted origins (no trailing slash in env/CORS values):

| Service | URL / host |
|---|---|
| Frontend | `https://v-beta-mncoop.vercel.app` |
| API | `https://v-beta-api-6vqd6rspuq-ue.a.run.app` |
| Neon (pooler) | `ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech` |

This is not a hardened production launch. Custom domains, Cloud SQL, VPC connectors, and always-on Cloud Run instances are out of scope here.

Use this with:

- [environment-variables.md](./environment-variables.md)
- [firebase-setup.md](./firebase-setup.md)
- [google-cloud-setup.md](./google-cloud-setup.md) (GCS bucket and service-account JSON)
- [database-schema.md](./database-schema.md)
- [release-readiness-checklist.md](../testing/release-readiness-checklist.md)

## Architecture

1. The browser loads the Next.js app from Vercel.
2. The browser authenticates with Firebase and calls the Cloud Run API (`NEXT_PUBLIC_API_BASE_URL`).
3. Cloud Run validates Firebase ID tokens and reads/writes Neon.
4. Solution-beta uploads use a GCS V4 signed PUT URL; the browser uploads directly to the bucket.

Deploy order: **Neon schema → Cloud Run → Vercel**. After the Vercel hostname is known, update CORS (API + GCS) and redeploy Cloud Run if needed.

## Cold starts and cost

Neon free-tier compute suspends when idle. Cloud Run scales to zero. The first request after idle can take 10–30 seconds. Hobby Vercel, Neon free tier, and Cloud Run’s free allowance are enough for a demo. GCP billing must still be enabled on the project.

## Prerequisites

- GCP project with billing enabled (same project as the video bucket is fine)
- `gcloud` CLI authenticated (`gcloud auth login` and `gcloud config set project PROJECT_ID`)
- Neon account (staging DB already exists: `v_beta` on AWS us-east-2)
- Vercel account with access to this GitHub repo
- Local copies of `server/firebase-credentials.json` and `server/google-account-credential.json` (do not commit them)

Choose and keep these values:

| Placeholder | Example |
|---|---|
| `PROJECT_ID` | your GCP project id |
| `REGION` | `us-central1` |
| `AR_REPO` | `v-beta` |
| `SERVICE` | `v-beta-api` |
| `NEON_HOST` | `ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech` |
| `VERCEL_ORIGIN` | `https://v-beta-mncoop.vercel.app` |

## 1) Neon

Staging database **`v_beta`** is on Neon (AWS `us-east-2`). Use the **pooler** host for the API:

- Host: `ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech`
- Port: `5432`
- User: `neondb_owner`
- `DB_JDBC_PARAMS=?sslmode=require&channel_binding=require`

Password stays in the Neon console and Cloud Run / Secret Manager — not in git.

If schema is not applied yet, run (replace `PASSWORD`):

```bash
psql "postgresql://neondb_owner:PASSWORD@ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech:5432/v_beta?sslmode=require" \
  -f server/src/main/resources/db/pg-v-beta.sql
```

You can also paste the SQL into the Neon SQL Editor with database `v_beta` selected.

Confirm tables exist (`gym_role`, `climbing_grade`, `wall_section`, and Sprint 5 moderation tables). See [database-schema.md](./database-schema.md).

Seeded `User_Account` rows use fake Firebase UIDs. Real hosted users are created by signup / `POST /api/accounts/session`. After the first real login, promote one account to admin (`gym_role_id` for `ADMIN`, or the in-app admin accounts flow).

## 2) GCP: APIs, Artifact Registry, secrets

Enable APIs:

```bash
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  cloudbuild.googleapis.com \
  storage.googleapis.com
```

Create an Artifact Registry Docker repo (once):

```bash
gcloud artifacts repositories create v-beta \
  --repository-format=docker \
  --location=REGION \
  --description="V-Beta API images"
```

Store credential JSON in Secret Manager (once). Paths are relative to `server/` when you run these commands from that directory:

```bash
cd server
gcloud secrets create firebase-credentials --data-file=firebase-credentials.json
gcloud secrets create google-service-credentials --data-file=google-account-credential.json
```

If the secrets already exist, add a new version with `gcloud secrets versions add ... --data-file=...`.

Grant the Cloud Run runtime service account access to those secrets (default Compute Engine SA is `PROJECT_NUMBER-compute@developer.gserviceaccount.com`):

```bash
PROJECT_NUMBER=$(gcloud projects describe PROJECT_ID --format='value(projectNumber)')
for SECRET in firebase-credentials google-service-credentials; do
  gcloud secrets add-iam-policy-binding "$SECRET" \
    --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
done
```

The Google service account JSON used for GCS must still have object-admin (or equivalent) on the public video bucket so signed URL generation and deletes work. See [google-cloud-setup.md](./google-cloud-setup.md).

## 3) GCS bucket CORS

Browser PUT uploads need bucket CORS for the Vercel origin. [server/gcs-cors.example.json](../../server/gcs-cors.example.json) already lists `http://localhost:3000` and `https://v-beta-mncoop.vercel.app`. Apply it with:

```bash
gsutil cors set server/gcs-cors.example.json gs://STORAGE_PUBLIC_BUCKET_NAME
gsutil cors get gs://STORAGE_PUBLIC_BUCKET_NAME
```

Keep the bucket publicly readable for playback URLs (`https://storage.googleapis.com/BUCKET/object`).

If a new Vercel hostname appears (preview deploys), add that origin to the CORS JSON and re-apply.

## 4) Cloud Run

Build and push from `server/` (Cloud Build uses `server/Dockerfile`; secrets stay out of the image via `server/.dockerignore`):

```bash
cd server
gcloud builds submit \
  --tag REGION-docker.pkg.dev/PROJECT_ID/v-beta/server:latest
```

Deploy. Replace placeholders. Mount both JSON secrets as files so existing `FIREBASE_CREDENTIALS_PATH` and `GOOGLE_SERVICE_CREDENTIALS_PATH` keep working (GCS V4 signed URLs need a private key):

```bash
gcloud run deploy v-beta-api \
  --image REGION-docker.pkg.dev/PROJECT_ID/v-beta/server:latest \
  --region REGION \
  --allow-unauthenticated \
  --memory 1Gi \
  --cpu 1 \
  --port 8080 \
  --timeout 60 \
  --set-env-vars "^|^DB_HOST=ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech|DB_PORT=5432|DB_NAME=v_beta|SQL_USERNAME=neondb_owner|SQL_PASSWORD=NEON_PASSWORD|DB_JDBC_PARAMS=?sslmode=require&channel_binding=require|GCP_PROJECT_ID=PROJECT_ID|STORAGE_PUBLIC_BUCKET_NAME=BUCKET|CORS_ALLOWED_ORIGINS=http://localhost:3000,https://v-beta-mncoop.vercel.app|FIREBASE_CREDENTIALS_PATH=/secrets/firebase/firebase-credentials.json|GOOGLE_SERVICE_CREDENTIALS_PATH=/secrets/gcp/google-account-credential.json" \
  --set-secrets="/secrets/firebase/firebase-credentials.json=firebase-credentials:latest,/secrets/gcp/google-account-credential.json=google-service-credentials:latest"
```

The `^|^` delimiter avoids clashes with commas and `=` inside values.

If CORS was deployed before the Vercel hostname was known, set it to the live origin:

```bash
gcloud run services update v-beta-api --region REGION \
  --update-env-vars "CORS_ALLOWED_ORIGINS=http://localhost:3000,https://v-beta-mncoop.vercel.app"
```

### Cloud Run env reference

| Variable | Staging value |
|---|---|
| `DB_HOST` | `ep-autumn-feather-ajy43z9p-pooler.c-3.us-east-2.aws.neon.tech` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `v_beta` |
| `SQL_USERNAME` | `neondb_owner` |
| `SQL_PASSWORD` | Neon role password (Secret Manager / Cloud Run env, not git) |
| `DB_JDBC_PARAMS` | `?sslmode=require&channel_binding=require` |
| `GCP_PROJECT_ID` | GCP project id |
| `STORAGE_PUBLIC_BUCKET_NAME` | GCS bucket name |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,https://v-beta-mncoop.vercel.app` |
| `FIREBASE_CREDENTIALS_PATH` | `/secrets/firebase/firebase-credentials.json` |
| `GOOGLE_SERVICE_CREDENTIALS_PATH` | `/secrets/gcp/google-account-credential.json` |
| `PORT` | injected by Cloud Run |

Smoke the API (first call may be slow):

```bash
curl -sS "https://v-beta-api-6vqd6rspuq-ue.a.run.app/api/health"
curl -sS "https://v-beta-api-6vqd6rspuq-ue.a.run.app/api/v1/meta"
```

## 5) Firebase

In Firebase Console → Authentication → Settings → **Authorized domains**, add:

- `v-beta-mncoop.vercel.app`

If Google sign-in is enabled, add the same host as an authorized JavaScript origin on the Google Cloud OAuth web client (`https://v-beta-mncoop.vercel.app`).

`NEXT_PUBLIC_APP_ORIGIN` on Vercel must be `https://v-beta-mncoop.vercel.app` so verification and password-reset links return to staging, not `localhost`.

## 6) Vercel

The frontend is already live at [https://v-beta-mncoop.vercel.app/](https://v-beta-mncoop.vercel.app/). Root Directory is `v-beta`. Later pushes to the connected branch redeploy that site.

To recreate or adjust the project:

1. Import this GitHub repository in Vercel.
2. Set **Root Directory** to `v-beta`.
3. Framework preset: Next.js. Node 20.
4. Environment variables (Production; add Preview if you want PR deploys to hit the same API):

| Variable | Value |
|---|---|
| `NEXT_PUBLIC_FIREBASE_API_KEY` | same as local Firebase web config |
| `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN` | Firebase auth domain |
| `NEXT_PUBLIC_FIREBASE_PROJECT_ID` | Firebase project id |
| `NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET` | Firebase storage bucket |
| `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID` | messaging sender id |
| `NEXT_PUBLIC_FIREBASE_APP_ID` | web app id |
| `NEXT_PUBLIC_FIREBASE_MEASUREMENT_ID` | optional |
| `NEXT_PUBLIC_API_BASE_URL` | `https://v-beta-api-6vqd6rspuq-ue.a.run.app` |
| `NEXT_PUBLIC_APP_ORIGIN` | `https://v-beta-mncoop.vercel.app` |

5. Deploy. API images are still published with `gcloud builds submit` + `gcloud run deploy` (no GitHub deploy workflow in this slice).

If Root Directory was wrong on the first attempt, fix it and redeploy. `NEXT_PUBLIC_*` values are inlined at build time; change them and trigger a new Vercel build.

### Vercel 404 (NOT_FOUND) after a “Ready” deploy

This repo is a monorepo. If Root Directory is the GitHub repo root, Vercel does not build Next.js. The deployment still finishes, then every URL returns 404.

Fix in Vercel → Project → **Settings → General / Build and Deployment**:

1. **Root Directory:** `v-beta` (not the repo root, not `v-beta/src`)
2. **Framework Preset:** Next.js (not Other)
3. **Output Directory:** leave empty / default (do **not** set `.next`, `out`, or `v-beta`)
4. **Build Command:** leave default (`npm run build` / `next build`)
5. **Redeploy** the latest deployment with **Clear cache** (Deployments → … → Redeploy)

Confirm in the new build log:

- `Detected Next.js version`
- `Running "npm run build"` / `Creating an optimized production build`
- Build time is many seconds, not ~40ms

Then open `/` (the landing page), not a random path. Missing API env vars do not cause this 404; they only break data after the page loads.

## 7) Smoke test (hosted URLs)

Run [release-readiness-checklist.md](../testing/release-readiness-checklist.md) against the live URLs, not localhost:

- [ ] Frontend loads at [https://v-beta-mncoop.vercel.app/](https://v-beta-mncoop.vercel.app/)
- [ ] `GET https://v-beta-api-6vqd6rspuq-ue.a.run.app/api/health` and `/api/v1/meta`
- [ ] Guest browse of walls and problems
- [ ] Email/password login and Google login (`v-beta-mncoop.vercel.app` is in Firebase authorized domains)
- [ ] Comment create
- [ ] Solution-beta upload (confirms GCS CORS)
- [ ] Promote a real user to admin; `/reports` / `/logbook` if in scope

## Troubleshooting

- **API 403 from browser / CORS error:** `CORS_ALLOWED_ORIGINS` must include `https://v-beta-mncoop.vercel.app` (no trailing slash). Redeploy or `gcloud run services update` after changing it.
- **Firebase `auth/unauthorized-domain`:** add `v-beta-mncoop.vercel.app` under Authorized domains (and the OAuth JS origin if using Google sign-in).
- **Backend fails schema validation on boot:** Neon never received `pg-v-beta.sql`, or the SQL was applied to a different database name than `DB_NAME`.
- **Neon connection timeout / SSL error:** `DB_JDBC_PARAMS=?sslmode=require&channel_binding=require` and `DB_HOST` must be the pooler hostname, not a JDBC URL.
- **Signed upload fails in the browser:** GCS bucket CORS origin/method/`Content-Type`; service-account JSON must be able to sign V4 URLs (file-mounted key, not a keyless ADC identity).
- **Vercel 404 NOT_FOUND on every path:** Root Directory is not `v-beta`, Framework Preset is Other, or Output Directory was overridden. See [Vercel 404](#vercel-404-not_found-after-a-ready-deploy).
- **Slow first request:** expected while Neon and Cloud Run are cold.

## Related local docs

- GCS bucket and service-account JSON: [google-cloud-setup.md](./google-cloud-setup.md)
- Cloud SQL via Auth Proxy remains a **local/dev** option; hosted staging uses Neon instead.

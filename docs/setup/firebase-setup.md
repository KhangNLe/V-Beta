# Firebase Setup

This guide configures Firebase for both applications in this repository:

- `v-beta/` (Next.js frontend, Firebase Web SDK)
- `server/` (Spring Boot backend, Firebase Admin SDK)

Use this together with `docs/setup/environment-variables.md`.

## 1) Create or select a Firebase project

1. Open [Firebase Console](https://console.firebase.google.com/).
2. Create a new project (or use an existing shared project).
3. Keep the project ID and project number handy.

## 2) Configure Authentication providers

In Firebase Console, go to **Authentication -> Sign-in method** and enable:

- **Email/Password**
- **Google**

This project uses both:

- Email/password signup/login
- Google popup signup/login
- Email verification and password reset flows

## 3) Add authorized domains

In **Authentication -> Settings -> Authorized domains**, ensure these are present:

- `localhost` (for local frontend dev)
- `v-beta-mncoop.vercel.app` (hosted staging: [https://v-beta-mncoop.vercel.app/](https://v-beta-mncoop.vercel.app/))

If missing, popup login and action links can fail. Hosted staging steps are in [deployment.md](./deployment.md).

## 4) Register the frontend web app (`v-beta/`)

1. In Firebase Console -> **Project settings -> General**.
2. Under **Your apps**, add a **Web app** (if not already added).
3. Copy the Firebase config values and place them in `v-beta/.env.local`.

Required frontend variables:

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

Notes:

- `NEXT_PUBLIC_APP_ORIGIN` is used for email action links (verification/reset callbacks).
- Keep `NEXT_PUBLIC_API_BASE_URL` pointed at your backend URL.

## 5) Create Firebase Admin credentials for backend (`server/`)

1. In Firebase Console -> **Project settings -> Service accounts**.
2. Generate a new private key (JSON).
3. Save it outside version control (example: `server/firebase-credentials.json`).
4. Set backend env var `FIREBASE_CREDENTIALS_PATH` to that file path.

Example in `server/.env`:

```bash
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
```

The backend uses this in `server/src/main/java/app/VBeta/config/FirebaseConfig.java` to initialize Firebase Admin, then validates frontend ID tokens in `FirebaseAuthFilter`.

## 6) Configure backend environment

Create `server/.env` and include at least:

```bash
SQL_USERNAME=
SQL_PASSWORD=
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=v_beta
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
```

For full backend env options, see `docs/setup/environment-variables.md`.

## 7) Run and verify

1. Start backend (`server/`) and frontend (`v-beta/`).
2. Verify these flows:
   - Email/password signup
   - Login with Google
   - Email verification (resend + confirm)
   - Forgot password email flow
3. Confirm API auth works:
   - Authenticated frontend calls succeed
   - Invalid/expired tokens return `401` from backend

## Troubleshooting

- **"Firebase: Error (auth/unauthorized-domain)"**
  - Add your local or deployed domain to authorized domains.

- **Google popup closes or fails**
  - Confirm Google sign-in provider is enabled and domain is authorized.

- **Email verification/reset links point to wrong URL**
  - Check `NEXT_PUBLIC_APP_ORIGIN` and restart frontend.

- **Backend fails on startup reading Firebase credentials**
  - Verify `FIREBASE_CREDENTIALS_PATH` is correct relative to `server/`.

- **Backend rejects valid-looking token**
  - Confirm frontend and backend are using the same Firebase project.

## Security reminders

- Never commit `server/.env`, `v-beta/.env.local`, or service-account JSON keys.
- Rotate service-account keys if exposed.
- Use separate Firebase projects for dev and production when possible.

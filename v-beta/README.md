# Frontend (`v-beta`)

Next.js frontend for V-Beta. This app connects to the Spring Boot backend in `../server` and uses Firebase Authentication on the client side.

## Overview

- Framework: Next.js + React + TypeScript
- Styling/UI: Tailwind CSS + shadcn
- Auth: Firebase client SDK
- API target: Spring Boot backend in `../server`

## Prerequisites

- Node.js 20+ (or current LTS compatible with Next.js 16)
- npm
- Backend API available (local or deployed)
- Firebase project configured for client auth

## Install Dependencies

From `v-beta/`:

```bash
npm install
```

## Environment Variables

Create `v-beta/.env.local`:

```env
NEXT_PUBLIC_FIREBASE_API_KEY=your-firebase-api-key
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your-project-id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your-messaging-sender-id
NEXT_PUBLIC_FIREBASE_APP_ID=your-app-id
NEXT_PUBLIC_FIREBASE_MEASUREMENT_ID=your-measurement-id

NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_ORIGIN=http://localhost:3000
```

Notes:

- `NEXT_PUBLIC_API_BASE_URL` is used by frontend API calls (for example account session sync).
- `NEXT_PUBLIC_APP_ORIGIN` is used for Firebase action-link flows when needed.

## Run in Development

```bash
npm run dev
```

Open `http://localhost:3000`.

## Build and Run Production Locally

```bash
npm run build
npm run start
```

## Lint and Tests

```bash
npm run lint
npm test
```

Watch mode:

```bash
npm run test:watch
```

## Quick Local Verification

1. Start backend (`../server`) on `http://localhost:8080`.
2. Start frontend with `npm run dev`.
3. Open `http://localhost:3000`.
4. Verify login/signup flow reaches Firebase and backend session sync succeeds.
5. Verify role-based navigation renders expected links for the signed-in account.

## Related Docs

- [Project docs index](../docs/README.md)
- [User Manual](../docs/user-manual.md)
- [Setup: Environment Variables](../docs/setup/environment-variables.md)
- [Setup: Local Development](../docs/setup/local-development.md)
- [Setup: Firebase](../docs/setup/firebase-setup.md)
- [Testing: Frontend Test Report](../docs/testing/frontend-test-report.md)

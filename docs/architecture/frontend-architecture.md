# Frontend Architecture

## Stack

- Next.js App Router (`src/app/`)
- React
- Tailwind/shadcn UI components
- Firebase Web SDK (Auth)
- `fetch`-based API modules in `src/api/`

## Directory Structure and Responsibilities

- `src/app/`
  - Route pages and global app layout
  - Firebase singleton/config exports
- `src/components/`
  - Feature-level UI (navbar, guest banner, auth shell)
- `src/components/ui/`
  - Shared UI primitives (button, card, dialog, etc.)
- `src/api/`
  - Backend API calls grouped by domain (`account`, `wallSections`, `comments`, `solutionBeta`)
- `src/hooks/`
  - Auth/session hook (`useRequireAuth`)
- `src/lib/`
  - Session persistence, email verification helpers, formatting utilities

## Routing Model

Primary routes:

- `/`
- `/login`, `/signup`, `/forgot-password`, `/verify-email`
- `/main-page`
- `/account`
- `/wall/[wallSectionID]`
- `/wall/[wallSectionID]/problem/[problemId]`

Routing is handled with Next.js App Router + client-side navigation (`useRouter`/`Link`).

## Auth and Session Flow

1. Firebase authenticates user (email/password or Google).
2. Frontend obtains ID token from Firebase user.
3. Frontend syncs account session with backend (`/api/accounts/session`).
4. Normalized account session (id, roleName, firebaseUid) is stored in local storage.
5. `useRequireAuth` controls redirects and email verification routing behavior.

Supporting auth/session modules:

- `src/hooks/useRequireAuth.js`
- `src/lib/accountSession.js`
- `src/lib/emailVerification.js`
- `src/components/guest-route-guard.jsx`

## State Management

- No global store (Redux/Zustand) is used.
- Most pages manage state via local `useState`/`useEffect`.
- Cross-page identity context comes from Firebase + local account session helper.

## UI and Theming

- Shared visual system is built from shadcn-like primitives and app theme tokens.
- Theme mode (light/dark) is controlled through `data-theme` and persisted preference.
- Toast notifications are globally available through `ToastProvider`.

## API Integration Pattern

- API modules call backend endpoints using `fetch`.
- Protected routes pass `Authorization: Bearer <idToken>`.
- Read-only endpoints may support guest access.
- API surface is split by domain modules:
  - `src/api/account.js`
  - `src/api/wallSections.js`
  - `src/api/comments.js`
  - `src/api/solutionBeta.js`

## Constraints and Considerations

- Route protection is mostly client-side behavior (UX gating), with backend as final authority.
- Some UX controls are role-name based and should be validated against backend permissions.
- API error handling is distributed across pages/modules and can benefit from centralization in future refactors.

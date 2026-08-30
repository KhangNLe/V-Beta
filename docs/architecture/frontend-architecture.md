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
  - Feature-level UI (navbar, guest banner, auth shell, notification bell)
- `src/components/ui/`
  - Shared UI primitives (button, card, dialog, etc.)
- `src/api/`
  - Backend API calls grouped by domain (`account`, `accounts`, `promoteOrDemote`, `wallSections`, `comments`, `solutionBeta`, `reports`, `notifications`)
- `src/hooks/`
  - Auth/session hook (`useRequireAuth`)
- `src/lib/`
  - Session persistence, email verification helpers, formatting utilities, notification click/read helpers

## Routing Model

Primary routes:

- `/`
- `/login`, `/signup`, `/forgot-password`, `/verify-email`
- `/main-page`
- `/account`
- `/accounts`
- `/notifications`
- `/reports`
- `/logbook`
- `/appeals`
- `/appeal-queue`
- `/wall/[wallSectionID]`
- `/wall/[wallSectionID]/problem/[problemId]`

Routing is handled with Next.js App Router + client-side navigation (`useRouter`/`Link`).

## Auth and Session Flow

1. Firebase authenticates user (email/password or Google).
2. Frontend obtains ID token from Firebase user.
3. Frontend syncs account session with backend (`POST /api/accounts/session`).
4. Normalized account session (`id` from `userId`, `roleName` from `role`, `firebaseUid` from the Firebase client) is stored in local storage. The backend `UserAccountDTO` does not include `firebaseUid`.
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

- API modules call backend endpoints using `fetch` under `/api/...`.
- Protected routes pass `Authorization: Bearer <idToken>`.
- Read-only endpoints may support guest access (`/api/home/...` reads and `/api/search/...`).
- HTTP methods follow the controllers (`PATCH` for wall reset and problem delete; `GET` with JSON body for solution-beta upload URL).
- API surface is split by domain modules:
  - `src/api/account.js`
  - `src/api/accounts.js`
  - `src/api/promoteOrDemote.js`
  - `src/api/wallSections.js`
  - `src/api/comments.js`
  - `src/api/solutionBeta.js`
  - `src/api/reports.js`
  - `src/api/moderation.js`
  - `src/api/appeals.js`
  - `src/lib/ownerAppeal.js`
  - `src/lib/appealQueue.js`
  - `src/api/notifications.js`
- Comment/beta delete helpers send a reason (`deletedReason` / `deleteReason`) from `src/lib/discussionDeletion.js`.
- Discussion Report on the problem page calls `createContentReport` (`POST /api/report/create`) with `DISCUSSION` + discussion id.
- Signed-in navbar renders `NotificationBell`: unread poll is `GET /api/notification/short`; mark-read is `PATCH /api/notification/short?notificationId=`.
- `/notifications` pages the full inbox with `GET /api/notification/all?offset=` (1-based, 10 rows). `QuickNotificationDTO` omits `readAt`, so the client overlays unread ids from `/short` before styling. Click paths come from `src/lib/notificationNavigation.js`. Report-queue events go to `/reports?reportId=`. Owner deletion/outcome events go to `/appeals?reportId=`. Admin `APPEAL_SUBMITTED` goes to `/appeal-queue?reportId=`. `/appeals?reportId=` is the owner deletion notice and one-time appeal form.
- `/reports` is admin-only (`VIEW_REPORTS` / role gate). It lists ranked OPEN cases from `GET /api/report/reports` and resolves discussion cases with `POST /api/moderate/report` (required notes, max 255). Non-admins are sent to `/main-page`.
- `/appeal-queue` is admin-only (`VIEW_APPEALS` / role gate). It lists OPEN appeals from `GET /api/moderate/appeal` (`AppealDTO`) and opens detail by `reportId`. Required admin comments (max 255) plus **Approve** / **Deny** call `PATCH /api/moderate/appeal` (`ModerateAppealRequest`: `appealId`, `appealStatus`, `adminReason`). Non-admins are sent to `/main-page`.
- `/logbook` is admin-only (`VIEW_MODERATION_LOGS` / role gate). It pages `GET /api/moderate/logbook` (25 rows, newest first), shows a read-only detail dialog, links to `/reports?reportId=` or `/appeal-queue?reportId=` when a report id exists, and downloads all pages as `.txt`. Non-admins are sent to `/main-page`.
- Account list/session helpers map `UserAccountDTO` (`userId`, `role`) onto the existing client session shape (`id`, `roleName`).

## Constraints and Considerations

- Route protection is mostly client-side behavior (UX gating), with backend as final authority.
- Some UX controls are role-name based and should be validated against backend permissions.
- API error handling is distributed across pages/modules and can benefit from centralization in future refactors.

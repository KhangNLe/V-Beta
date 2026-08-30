# Authentication and Roles

## Feature Overview

This section documents the authentication and authorization features that are currently implemented in the project.

## Implemented Features

- Email/password signup and login using Firebase Authentication
- Google sign-in and signup via popup
- Email verification flow for password-provider users
- Forgot-password email reset flow
- Backend account session sync after successful Firebase auth (`POST /api/accounts/session` → `UserAccountDTO`)
- Role-aware navigation and role-gated UI actions
- Admin account-management capabilities (view accounts, promote/demote roles)
- Signed-in notification bell (unread poll) and `/notifications` all-inbox page (paged `GET /all`)
- Admin report queue at `/reports` (ranked list, detail dialog, dismiss / remove with notes)
- Admin moderation logbook at `/logbook` (paged list, read-only detail, `.txt` download)

## User Flows

### Signup and Login

1. User signs up or logs in from the frontend (`/signup` or `/login`).
2. Firebase authenticates the user.
3. Frontend syncs account session with backend.
4. User is redirected to `main-page` (or `verify-email` when required).

### Email Verification

1. New password-based account receives verification email.
2. User opens verification link.
3. User confirms in-app from `/verify-email`.
4. Session refreshes and user continues to main app.

### Password Reset

1. User requests reset link from `/forgot-password`.
2. Firebase sends reset email.
3. User follows link and signs in again.

## Roles and Access (Current Behavior)

- **Guest**
  - Can browse core wall/problem pages.
  - Cannot perform authenticated actions (comment, beta upload, content report, notifications, account-only actions).
- **Climber**
  - Authenticated features such as comments, beta uploads, grade suggestions, content reports, and one-time appeals after removal.
  - Can poll `GET /api/notification/short` (navbar bell), page the full inbox on `/notifications` with `GET /api/notification/all`, and mark own rows read with `PATCH /api/notification/short?notificationId=`. `REPORT_CREATED` inbox rows are not written for this role; queue-resolve can write `REPORT_DISMISSED`, `REPORT_APPROVED`, or `CONTENT_REMOVED`. Appeal resolve can write `CONTENT_RESTORED` or `APPEAL_DENIED`.
  - Can access own account page and self-service account actions.
- **Setter**
  - Includes climber capabilities.
  - Can create/delete climbing problems and reset wall sections (where role checks are enforced).
- **Admin**
  - Includes climber capabilities, including creating content reports.
  - Receives `REPORT_CREATED` and `APPEAL_SUBMITTED` unread inbox rows (unless they are the reporter/appellant).
  - Can view all accounts and promote/demote account roles.
  - Can view the ranked report queue (`VIEW_REPORTS`), resolve OPEN discussion reports (`MODERATE_REPORT`), read the moderation logbook (`VIEW_MODERATION_LOGS`), read the appeal queue (`VIEW_APPEALS`), and approve or deny appeals (`MODERATE_APPEAL`).
  - Can manage wall sections (create/delete).
  - Can perform moderation-style actions such as deleting comments/betas where admin checks are enforced.
  - Can access admin navigation/account-management workflow.
  - Note: problem creation/deletion and wall reset are currently treated as setter workflow in the UI.

## Admin Role and Access Details

- Account list and role management are backed by:
  - `GET /api/accounts`
  - `PATCH /api/accounts/{userId}/role`
- Report queue and detail are backed by `GET /api/report/reports` (`ActionDefinition.VIEW_REPORTS`).
- Report resolve is backed by `POST /api/moderate/report` (`ActionDefinition.MODERATE_REPORT`).
- Logbook is backed by `GET /api/moderate/logbook` (`ActionDefinition.VIEW_MODERATION_LOGS`).
- Appeal queue and detail are backed by `GET /api/moderate/appeal` (`ActionDefinition.VIEW_APPEALS`).
- Appeal resolve is backed by `PATCH /api/moderate/appeal` (`ActionDefinition.MODERATE_APPEAL`).
- Backend authorization for these actions is enforced through `ActionDefinition.VIEW_ACCOUNTS`, `ActionDefinition.CHANGE_ROLE`, `ActionDefinition.VIEW_REPORTS`, `ActionDefinition.MODERATE_REPORT`, `ActionDefinition.VIEW_MODERATION_LOGS`, `ActionDefinition.VIEW_APPEALS`, and `ActionDefinition.MODERATE_APPEAL`.
- The frontend exposes admin navigation for account-management workflow.
- Admin role changes should be validated end-to-end (UI + API + permission checks) whenever role logic changes.

## Content Reports and Notifications

- Create report: `POST /api/report/create` (authenticated; not action-gated; no `CREATE_REPORT`; success `200`). The problem-page ⋮ menu is the Sprint 5 UI for this (comments and betas; category + reason required, max 250). Owners cannot report their own discussion.
- Admin queue/detail: `GET /api/report/reports` (action-gated `VIEW_REPORTS`; admin only). Optional `reportId` returns one OPEN case. The `/reports` page lists ranked cases and opens detail (wall/problem/content, reporters, required notes).
- Admin resolve: `POST /api/moderate/report` (action-gated `MODERATE_REPORT`; admin only). Dismiss (`REPORT_DISMISSED`) or remove (`CONTENT_REMOVED`) discussion reports; notes required (max 255). Appeals are not accepted here.
- Admin logbook: `GET /api/moderate/logbook` (action-gated `VIEW_MODERATION_LOGS`; admin only). Optional `moderationId` returns one row; `offSetPlace` pages 25 newest-first. The `/logbook` page lists those rows (read-only) and can download all pages as `.txt`.
- Owner appeal create: `POST /api/moderate/appeal` (authenticated; not action-gated). One appeal per `CONTENT_REMOVED` discussion report owned by the caller.
- Owner deletion notice: `GET /api/moderate/appeal/notice?reportId=` (authenticated; owner of the removed discussion). Powers `/appeals?reportId=` (admin reason, content summary, report category/reason without reporter identity, one-time form, status).
- Admin appeal queue/detail: `GET /api/moderate/appeal` (action-gated `VIEW_APPEALS`; admin only). Optional `appealId` or `reportId` returns one row. The `/appeal-queue` page lists OPEN appeals (`AppealDTO`), opens detail, and resolves with required comments.
- Admin appeal resolve: `PATCH /api/moderate/appeal` (action-gated `MODERATE_APPEAL`; admin only). Body is `ModerateAppealRequest` (`appealId`, `appealStatus` `APPROVED`/`DENIED`, `adminReason` max 255). `APPROVED` restores the discussion; `DENIED` keeps it removed. Both write a logbook row and notify the owner. The `/appeal-queue` detail dialog is the UI.
- Unread inbox poll: `GET /api/notification/short` (authenticated; not action-gated). Includes `notificationId` and `click` metadata (`kind` + target ids). Current moderation events use `click.kind = REPORT_QUEUE`. The navbar bell uses this poll.
- All-inbox page: `GET /api/notification/all` (authenticated; not action-gated). 10 rows per page, `offset` 1-based, read and unread. Same DTO as `/short` (`readAt` omitted). `/notifications` calls this with Previous/Next and overlays unread ids from `/short` so rows can still render as read vs unread.
- Mark read: `PATCH /api/notification/short?notificationId=` (authenticated; own row only; already-read is a no-op). Bell and all-inbox both call this before navigating.
- Click routing: `REPORT_CREATED` / `REPORT_DISMISSED` / `REPORT_APPROVED` → `/reports?reportId=` (admin queue). `CONTENT_REMOVED` / `CONTENT_RESTORED` / `APPEAL_DENIED` → `/appeals?reportId=` (owner deletion notice + one-time appeal). `APPEAL_SUBMITTED` → `/appeal-queue?reportId=` (admin appeal queue).
- See `docs/api/endpoints.md`, `docs/api/permissions-matrix.md`, and `docs/api/request-response-examples.md`.

## Key Files

- Frontend auth forms and flows:
  - `v-beta/src/components/ui/login-form.jsx`
  - `v-beta/src/components/ui/signup-form.jsx`
  - `v-beta/src/components/ui/verify-email-form.jsx`
  - `v-beta/src/components/ui/forgot-password-form.jsx`
- Auth/session helpers:
   -`v-beta/src/hooks/useRequireAuth.js`
   - `v-beta/src/lib/accountSession.js`
   - `v-beta/src/lib/emailVerification.js`
- Backend auth filter and authorization:
  - `server/src/main/java/app/VBeta/config/security/FirebaseAuthFilter.java`
  - `server/src/main/java/app/VBeta/application/AuthorizationService.java`
- Content report and notification APIs:
  - `v-beta/src/api/reports.js`
  - `v-beta/src/lib/reportQueue.js`
  - `v-beta/src/app/reports/page.js`
  - `v-beta/src/api/moderation.js`
  - `v-beta/src/lib/moderationLogbook.js`
  - `v-beta/src/app/logbook/page.js`
  - `v-beta/src/api/appeals.js`
  - `v-beta/src/lib/ownerAppeal.js`
  - `v-beta/src/app/appeals/page.js`
  - `v-beta/src/lib/appealQueue.js`
  - `v-beta/src/app/appeal-queue/page.js`
  - `v-beta/src/api/notifications.js`
  - `v-beta/src/lib/notificationNavigation.js`
  - `v-beta/src/components/NotificationBell.js`
  - `v-beta/src/app/notifications/page.js`
  - `v-beta/src/app/wall/[wallSectionID]/problem/[problemId]/page.js`
  - `server/src/main/java/app/VBeta/controller/ContentReportController.java`
  - `server/src/main/java/app/VBeta/controller/ModerationController.java`
  - `server/src/main/java/app/VBeta/controller/EvenNotificationController.java`

## Limitations and Notes

- Some authorization enforcement is endpoint-specific and may differ by controller/service path.
- Some UI gating is role-name based, so frontend behavior and backend authorization should both be validated during regression testing.
- Role permission behavior should be validated during regression testing when adding new actions.
- `GET /api/notification/all` omits `readAt`. The all-inbox UI infers unread from a parallel `/short` poll; if that poll fails, rows render as unread.

## Future Enhancements

Potential improvements are tracked separately in `docs/features/future-features.md`.

# Authentication and Roles

## Feature Overview

This section documents the authentication and authorization features that are currently implemented in the project.

## Implemented Features

- Email/password signup and login using Firebase Authentication
- Google sign-in and signup via popup
- Email verification flow for password-provider users
- Forgot-password email reset flow
- Backend account session sync after successful Firebase auth
- Role-aware navigation and role-gated UI actions
- Admin account-management capabilities (view accounts, promote/demote roles)

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
  - Cannot perform authenticated actions (comment, beta upload, account-only actions).
- **Climber**
  - Authenticated features such as comments, beta uploads, and grade suggestions.
  - Can access own account page and self-service account actions.
- **Setter**
  - Includes climber capabilities.
  - Can create/delete climbing problems and reset wall sections (where role checks are enforced).
- **Admin**
  - Includes climber capabilities.
  - Can view all accounts and promote/demote account roles.
  - Can manage wall sections (create/delete).
  - Can perform moderation-style actions such as deleting comments/betas where admin checks are enforced.
  - Can access admin navigation/account-management workflow.
  - Note: problem creation/deletion and wall reset are currently treated as setter workflow in the UI.

## Admin Role and Access Details

- Account list and role management are backed by:
  - `GET /api/accounts`
  - `PATCH /api/accounts/{userId}/role`
- Backend authorization for these actions is enforced through `ActionDefinition.VIEW_ACCOUNTS` and `ActionDefinition.CHANGE_ROLE`.
- The frontend exposes admin navigation for account-management workflow.
- Admin role changes should be validated end-to-end (UI + API + permission checks) whenever role logic changes.

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
  - `server/src/main/java/edu/ics499/VBeta/config/security/FirebaseAuthFilter.java`
  - `server/src/main/java/edu/ics499/VBeta/application/AuthorizationService.java`

## Limitations and Notes

- Some authorization enforcement is endpoint-specific and may differ by controller/service path.
- Some UI gating is role-name based, so frontend behavior and backend authorization should both be validated during regression testing.
- Role permission behavior should be validated during regression testing when adding new actions.

## Future Enhancements

Potential improvements are tracked separately in `docs/features/future-features.md`.

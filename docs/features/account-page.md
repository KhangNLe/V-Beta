# Account Page

## Feature Overview

This section documents account-related features currently available to authenticated users, including account profile retrieval and account-management capabilities.

## Implemented Features

- Authenticated account page route (`/account`)
- Fetch and display current account information from backend
- Displays core account fields from `UserAccountDTO`:
  - `userId`
  - email
  - username
  - role
- Loading and error states for account data request
- Full account self-deletion support (frontend and backend)
- Full admin role management support for account promotion/demotion (frontend and backend workflow)
- Admin account cards show `userId`, email, username, and role. The API does not return `firebaseUid`.

## User Flow

1. Authenticated user opens `/account`.
2. Frontend checks auth/session readiness.
3. Frontend calls backend account endpoint.
4. UI renders account details card.
5. If request fails, an error card/message is shown.

## Admin Role Promotion/Demotion Flow

1. Admin accesses account-management workflow.
2. Admin selects a target user account.
3. Admin submits role update (promote/demote).
4. Backend validates admin authorization and updates the user role.

## Data and API

- Frontend fetch helper: `v-beta/src/api/account.js`
- Account page route component: `v-beta/src/app/account/page.js`
- Backend endpoint: `GET /api/account` (`UserAccountDTO`)
- Backend self-delete endpoint: `DELETE /api/account/deletion`
- Backend admin role update endpoint: `PATCH /api/accounts/{userId}/role` (`UserAccountDTO`)
- Backend admin account list endpoint: `GET /api/accounts` (array of `UserAccountDTO`)
- Session bootstrap: `POST /api/accounts/session` (`201` + `UserAccountDTO`)
- Frontend session helper maps `userId` → `id` and `role` → `roleName`, and stores the Firebase client UID locally.

## Notes and Limitations

- Account deletion and role-management actions should be regression-tested whenever auth/session logic changes.
- Profile editing is not currently supported from this page.

## Future Enhancements

Potential improvements are tracked separately in `docs/features/future-features.md`.

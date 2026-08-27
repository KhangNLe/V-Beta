# User Manual

This manual explains how to use the application by role.

## 1) Before You Start

- Frontend URL (local): `http://localhost:3000`
- Backend URL (local): `http://localhost:8080`
- For authenticated actions, sign in from `/login` or create an account at `/signup`.

## 2) Guest Flow (Browse Walls and Problems)

Guests can view content but cannot perform authenticated actions.

### Steps
1. Open `/main-page`.
2. Select a wall section.
3. Review the problem list for that section.
4. Open a problem detail page to view:
   - problem details
   - existing discussion
   - perceived grade context

### Expected Guest Limitations
- Cannot post comments
- Cannot upload beta videos
- Cannot submit perceived grades
- Cannot report discussion content
- Cannot open the notification bell or `/notifications`
- Cannot access account-only or admin-only pages

## 3) Climber Flow (Login, Comment, Submit Beta, Suggest Grade)

Climber users can participate in problem discussion and feedback.

### A) Login
1. Open `/login`.
2. Sign in with email/password or supported provider.
3. After successful auth/session sync, continue to `/main-page`.

### B) Post a Comment
1. Open `/main-page` and select a wall section.
2. Open a problem page.
3. Enter comment text and submit.
4. Verify the new comment appears in the discussion.

### B2) Delete a Comment
1. On a problem page, open the actions menu on a comment you own (or, as admin, on another user's comment).
2. Choose Delete Comment.
3. Verify the comment disappears from the discussion after refresh.

### B3) Report a Comment or Beta
1. Sign in and open a problem page.
2. Open the ⋮ menu on another user's comment or beta (not your own).
3. Choose **Report**, pick a category, enter a reason (required, at most 250 characters), and submit.
4. Verify a success toast. Duplicate or invalid reports show an error toast.

### B4) Notifications
1. Sign in. Confirm the navbar bell is visible (guests do not see it).
2. Open the bell to list unread items (`GET /api/notification/short`).
3. Choose **Show all notifications** (or open `/notifications`).
4. Confirm the page lists read and unread history (`GET /api/notification/all`, 10 per page). Use **Next** / **Previous** when there are more than 10 rows.
5. Open an item. It is marked read (`PATCH /api/notification/short?notificationId=`) and you land on `/reports?reportId=` or `/appeals?reportId=` depending on the event type.

### C) Submit a Beta Video
1. On a problem page, choose beta upload flow.
2. Request signed upload URL.
3. Upload video and save beta metadata.
4. Verify your beta appears in discussion.

### C2) Delete a Beta Video
1. On a problem page, open the actions menu on a beta you own (or, as admin, on another user's beta).
2. Choose Delete Solution Beta.
3. Verify the beta disappears from the discussion after refresh.

### D) Suggest a Grade
1. On a problem page, choose a perceived grade.
2. Submit grade suggestion.
3. Verify perceived grade display updates after submission.

## 4) Setter/Admin Flow (Wall/Problem Management + Account Role Management)

Setter and Admin users can perform route-setting and management operations.

### A) Wall and Problem Management (Setter/Admin)
1. Open `/main-page`.
2. Select a wall section.
3. Use available management controls to:
   - create a new problem
   - delete a problem
   - reset/archive wall section problems (setter workflow)
4. Verify the wall/problem list refreshes with updates.

### B) Admin Account Role Management (Admin)
1. Open `/accounts`.
2. Review all user accounts.
3. Click **Change Role** on a target user.
4. Select a new role and submit.
5. Verify role update is reflected in the UI after refresh.

## 5) Account Page Actions

Authenticated users can access `/account` to:
- view email, username, and role
- delete their own account (with confirmation)

## 6) Troubleshooting

- If a page is stuck loading, refresh and confirm backend is running.
- If you receive authorization errors, confirm the signed-in account role.
- If uploads fail, verify backend cloud-storage environment settings.
- If session data appears stale, sign out and sign back in.

## 7) Related References

- Setup: [`docs/setup/local-development.md`](./setup/local-development.md)
- Feature behavior: [`docs/features/authentication-and-roles.md`](./features/authentication-and-roles.md)
- Wall/problem behavior: [`docs/features/wall-and-problems.md`](./features/wall-and-problems.md)
- Account behavior: [`docs/features/account-page.md`](./features/account-page.md)

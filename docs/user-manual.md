# User Manual

This manual explains how to use the application by role.

## 1) Before You Start

- Frontend URL (local): `http://localhost:3000`
- Frontend URL (hosted staging): [https://v-beta-mncoop.vercel.app/](https://v-beta-mncoop.vercel.app/)
- Backend URL (local): `http://localhost:8080`
- Backend URL (hosted staging): [https://v-beta-api-6vqd6rspuq-ue.a.run.app](https://v-beta-api-6vqd6rspuq-ue.a.run.app)
- For authenticated actions, sign in from `/login` or create an account at `/signup`.

Sprint 5 moderation is live: report a comment or beta from the ⋮ menu; admins review **Reports** and **Logbook**; owners appeal from a **Content removed** notification; admins decide on **Appeals**. Details: [`docs/features/moderation.md`](./features/moderation.md).

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

### B) Post to User Discussion
1. Open `/main-page` and select a wall section.
2. Open a problem page.
3. In the floating **Add a comment or solution beta** box under User Discussion:
   - Text only → **Post** (comment).
   - Video only → **Attach video**, then **Post** (signed upload, then beta save).
   - Text + video → enter text, **Attach video**, then **Post** (signed upload, then mixed discussion-content save).
4. Verify the new post appears in the discussion (mixed posts show text and **Watch Beta**).

### B1) Reply to a Discussion Post
1. On a problem page, choose **Reply** on a post that is not a removed placeholder.
2. In the inline composer, post text only, video only, or text + video (same rules as a top-level post).
3. Verify the reply nests under the parent. The parent author (if not you) gets a **DISCUSSION_REPLY** notification.
4. Open that notification from the bell or `/notifications` and confirm the problem page scrolls to and briefly highlights the reply.

### B2) Delete a Discussion Post
1. On a problem page, open the actions menu on a post you own (or, as admin, on another user's post).
2. Choose **Delete Comment**, **Delete Solution Beta**, or **Delete Post** (label matches what the post contains).
3. Verify the post remains in the discussion as a removed placeholder after refresh (for example, “The discussion had been deleted by the author.”). Reply / Report / Delete are not available on that placeholder.

### B3) Report a Comment or Beta
1. Sign in and open a problem page.
2. Open the ⋮ menu on another user's comment, beta, or mixed post (not your own).
3. Choose **Report**, pick a category, enter a reason (required, at most 250 characters), and submit.
4. Verify a success toast. Duplicate or invalid reports show an error toast.

### B4) Notifications
1. Sign in. Confirm the navbar bell is visible (guests do not see it).
2. Open the bell to list unread items (`GET /api/notification/short`).
3. Choose **Show all notifications** (or open `/notifications`).
4. Confirm the page lists read and unread history (`GET /api/notification/all`, 10 per page). Use **Next** / **Previous** when there are more than 10 rows.
5. Open an item. It is marked read (`PATCH /api/notification/short?notificationId=`) and you land on `/reports?reportId=`, `/appeals?reportId=`, `/appeal-queue?reportId=`, or a problem discussion (`?discussionId=`) depending on the event type.

### D) Suggest a Grade
1. On a problem page, use **Suggest Difficulty** next to the climb info card.
2. Choose a perceived grade and submit.
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

### A2) Wall Section Photo (Admin)
1. On `/main-page` or a wall section page, confirm sections without a photo show the default co-op image.
2. Open **Add Wall Section** and confirm the same default photo is shown, with text that it appears until you upload one.
3. Open the section ⋮ menu and choose **Edit wall**.
4. Choose a photo (JPEG, PNG, WebP, or iPhone HEIC). Confirm it stays a preview until **Save changes** or **Add section**. An iPhone HEIC photo shows **Uploading iPhone photo…** while it converts. After save, confirm the thumbnail updates and a success toast appears.
5. Choose **Remove photo**. Confirm the default co-op image returns. Wall thumbnails do not expand.
6. Sign in as a climber or setter and confirm **Edit wall** is not shown.

### A3) Climbing Problem Photo (Setter)
1. On a wall section page, confirm problems without a photo show the default problem-holder image.
2. Open **Add Problem** and confirm the same default photo is shown, with text that it appears until you upload one.
3. Open the problem ⋮ menu and choose **Edit problem**.
4. Choose a photo (JPEG, PNG, WebP, or iPhone HEIC). Confirm it stays a preview until **Save changes** or **Add problem**. An iPhone HEIC photo shows **Uploading iPhone photo…** while it converts. After save, confirm the thumbnail updates and a success toast appears.
5. Choose **Remove photo**. Confirm the default problem-holder image returns.
6. Open the problem page and click the photo. Confirm a larger view opens. Problem cards on the wall section page do not expand.
7. Sign in as a climber or admin and confirm **Edit problem** is not shown.

### B) Admin Account Role Management (Admin)
1. Open `/accounts`.
2. Review all user accounts.
3. Click **Change Role** on a target user.
4. Select a new role and submit.
5. Verify role update is reflected in the UI after refresh.

### C) Admin Report Queue (Admin)
1. Open **Reports** in the navbar (`/reports`). Climbers/setters should be redirected to `/main-page`.
2. Confirm open cases are listed with date, reporter, and category, highest score first.
3. Open a case. Confirm wall section, problem, reported comment or beta, and reporter reason.
4. Leave admin notes empty and confirm **Dismiss** and **Approve deletion** stay disabled.
5. Enter notes and dismiss or approve deletion. Confirm a success toast and that the case leaves the queue.

### D) Admin Moderation Logbook (Admin)
1. Open **Logbook** in the navbar (`/logbook`). Climbers/setters should be redirected to `/main-page`.
2. Confirm newest-first rows with decision, admin, time, and notes.
3. Open a row. Confirm it is read-only and the report link is present when a report id exists.
4. Download `.txt` and confirm the file lists those decisions.

### E) Owner Appeal After Removal
1. After an admin removes your comment or beta, open the **Content removed** notification.
2. On `/appeals?reportId=`, read the admin reason, removed content, report category, and report reason. Reporter names are not shown.
3. Submit one appeal. The form should close. Reloading should not offer a second submit.

### F) Admin Appeal Queue (Admin)
1. After an owner submits an appeal, open the **Appeal submitted** notification.
2. Land on `/appeal-queue?reportId=`. Confirm the owner’s appeal reason and the reported content. Reporter identity may be shown here.
3. Enter admin comments, then **Approve** (restore) or **Deny** (keep removed). The queue should refresh.
4. Open **Appeals** in the navbar. Climbers/setters should be redirected to `/main-page`.

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
- Moderation: [`docs/features/moderation.md`](./features/moderation.md)
- Wall/problem behavior: [`docs/features/wall-and-problems.md`](./features/wall-and-problems.md)
- Account behavior: [`docs/features/account-page.md`](./features/account-page.md)

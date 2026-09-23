# Wall and Problems

## Feature Overview

This section documents wall section and climbing problem features currently available in the application.

## Implemented Features

- View wall sections on the main page
- View active problems within a selected wall section
- View problem details, discussion, and suggested grade context
- Admin wall section creation, edit (name, description, photo), and deletion
- Wall section photos on the main page and wall section page, with `/co-op.png` as the default when `imageURL` is null
- Climbing problem photos on the wall section page and problem page, with `/problem-holder.jpg` as the default when `imageURL` is null
- Setter problem creation, edit (hold color, grade, notes, photo), and deletion
- Problem page photo expand for any viewer
- Setter wall section reset/archive operation
- Authenticated discussion actions (comments, beta upload, grade suggestion, content report)
- iPhone `.mov` beta uploads convert to MP4 (Cloud Run worker) before they are stored for playback; `.mp4` and `.webm` are stored as-is
- Guest browsing mode with read-only wall/problem access and banner messaging
- Owner/admin **soft-delete** for comments and solution betas (`Discussion_Root.deleted_at` / `deleted_by` / `deleted_reason`)
- Signed-in **Report** action on the discussion ⋮ menu (comments and solution betas) via `POST /api/report/create`
- Backend discovery: filter active problems by inclusive grade range within a wall section
- Backend discovery: sort filtered problems by assigned grade ascending or descending
- Wall section Filter UI: grade range (min–max), sort by most recent / easiest / hardest, Apply / Clear
- Add Problem grade picker uses a grade dropdown (`VB`–`V17`)
- Backend image upload/delete APIs for wall sections and climbing problems (`/api/social/image/*`)
- Signed GCS PUT flow for wall, problem, and profile image targets (profile persistence incomplete)

## User Flows

### Browse Walls and Problems

1. User opens `/main-page`.
2. User selects a wall section.
3. App loads problems for that section (default list order from the wall problems endpoint).
4. If a wall section is invalid or missing, user is redirected back to `/main-page`.
5. User opens an individual problem page.

### Problem Discovery by Grade (UI + API)

1. On a wall section page, user opens **Filter**.
2. User selects inclusive min/max grades and a sort mode:
   - **Most Recent** — grade filter via `/api/search` without `sort`, then client sorts by `createdDate` descending
   - **Easiest** — `/api/search?...&sort=asc`
   - **Hardest** — `/api/search?...&sort=desc`
3. **Apply** loads matching active problems; Apply is disabled when min is harder than max.
4. **Clear filters** restores the default wall-section problem list.
5. Guests and signed-in users can use Filter; invalid API ranges still return `400`, missing walls `404`.

Keyword/text search is deferred to a later sprint (completed Sprint 4 delivered grade filter/sort only).

### Setter Management Flow

1. Setter opens a wall section.
2. Setter creates, edits, or deletes problems.
3. Setter can reset/archive active problems for a section.
4. Updated problem list is refreshed after create/edit/delete/reset operations.

### Setter Problem Photo

1. Setter opens a wall section or a problem page.
2. Problems without `imageURL` show `/problem-holder.jpg`. **Add Problem** previews that same default and explains it is what climbers see until a photo is uploaded.
3. Setter opens **Edit problem** from the problem menu.
4. Upload or replace uses a signed URL (`GET /api/social/image/signed-url` with `imageTargetType=CLIMBING_PROBLEM`), a direct GCS `PUT`, then `PATCH /api/social/image/upload`. JPEG, PNG, WebP, and iPhone HEIC/HEIF are accepted; HEIC is converted to JPEG in the browser before upload.
5. **Remove photo** calls `PATCH /api/home/wall-sections/{wallSectionId}/problems/{problemId}/update` with `objectFileName` and `imageURL` set to null. The default photo returns without a full page reload.
6. On the problem page, any viewer can click the photo to expand it.
7. Climbers, admins, and guests do not see Edit problem.

### Admin Wall Photo

1. Admin opens `/main-page` or a wall section page.
2. Sections without `imageURL` show the default co-op photo. **Add Wall Section** previews that same default and explains it is what climbers see until a photo is uploaded.
3. Admin opens **Edit wall** from the section menu.
4. Upload or replace uses a signed URL (`GET /api/social/image/signed-url`), a direct GCS `PUT`, then `PATCH /api/social/image/upload`. JPEG, PNG, WebP, and iPhone HEIC/HEIF are accepted; HEIC is converted to JPEG in the browser before upload.
5. **Remove photo** calls `PATCH /api/home/wall-section/{id}/update` with `objectFileName` and `imageURL` set to null. The default photo returns without a full page reload.
6. Climbers, setters, and guests do not see Edit wall.

### Image Upload Flow (API)

1. Admin requests `GET /api/social/image/signed-url` with `imageTargetType=WALL_SECTION` and `wallSectionId`.
2. Client uploads the image to GCS with the returned signed PUT URL.
3. Client calls `PATCH /api/social/image/upload` with `objectFileName` and `imageUrl`.
4. The card, section header, or problem header updates from the returned public URL.
5. Problem uploads use `imageTargetType=CLIMBING_PROBLEM` and `climbingProblemId` on the metadata save.

See [`docs/features/wall-problem-images.md`](./wall-problem-images.md).

### Discussion Flow

1. Authenticated user opens a problem.
2. User can post a comment.
3. User can upload a beta video and save metadata.
4. User can submit perceived grade.
5. Comment/solution beta **soft-delete** is allowed for owner or admin (when checks pass). Deleted items disappear from the problem timeline after refresh.
6. Signed-in users who do **not** own the discussion can open **Report** from the ⋮ menu, choose a category, enter a reason (required, max 250 characters), and submit. Guests do not see the menu. Owners see Delete only (self-report is rejected by the API).

Current discussion payload contract is unified through `DiscussionRoot` metadata:

- Discussion entries include `discussionId`, `discussionType`, and `discussionContent`.
- Deletion payloads for both comments and solution betas include `discussionId` and a reason string (max 100).
- Comment deletes send `deletedReason`; beta deletes send `deleteReason`.
- The problem page currently sends `"User deleted their own discussion"` for owner deletes and `"Admin forced delete the discussion"` when an admin deletes another user's item.

## Permissions and Visibility

- Guest users can browse wall and problem content.
- Mutating wall/problem management operations require role-qualified users.
- Setter-gated UI controls are used for problem create/edit/delete/reset and problem photos.
- Wall image upload/delete requires admin (`UPLOAD_WALL_IMAGE`). Problem image upload/delete requires setter (`UPLOAD_PROBLEM_IMAGE`).
- Deletion of user-generated discussion content is restricted to owner/admin patterns.
- Reporting discussion content is available to any signed-in role except the discussion owner. Guests have no Report control.

## Key Files

- Main page and wall/problem routes:
    - `v-beta/src/app/main-page/page.js`
    - `v-beta/src/app/wall/[wallSectionID]/page.js`
    - `v-beta/src/app/wall/[wallSectionID]/problem/[problemId]/page.js`
- Frontend API modules: 
    - `v-beta/src/api/wallSections.js`
    - `v-beta/src/api/socialImage.js`
    - `v-beta/src/components/WallSectionAdminMenu.js`
    - `v-beta/src/components/ClimbingProblemSetterMenu.js`
    - `v-beta/src/hooks/useWallSectionImageUpload.js`
    - `v-beta/src/hooks/useClimbingProblemImageUpload.js`
    - `v-beta/src/api/comments.js`
    - `v-beta/src/api/solutionBeta.js`
    - `v-beta/src/api/reports.js`
    - `v-beta/src/lib/discussionDeletion.js`
- Backend controllers/services:
    - `server/src/main/java/app/VBeta/controller/SocialMediaController.java`
    - `server/src/main/java/app/VBeta/application/ImageService.java`
    - `server/src/main/java/app/VBeta/application/support/cloud/CloudStorageManager.java`
    - `server/src/main/java/app/VBeta/controller/WallSectionController.java`
    - `server/src/main/java/app/VBeta/controller/ProblemDiscussionController.java`
    - `server/src/main/java/app/VBeta/controller/ProblemDiscoveryController.java`
    - `server/src/main/java/app/VBeta/application/ClimbingWallService.java`
    - `server/src/main/java/app/VBeta/application/ProblemDiscussionService.java`
    - `server/src/main/java/app/VBeta/application/ProblemFilteringService.java`

## Limitations and Notes

- Comment/beta delete is a soft delete: discussion root metadata is marked deleted; comment text, beta metadata, and GCS objects stay. Appeal **Approve** restores the discussion; delayed GCS purge is still future work.
- Problem delete is `PATCH /api/home/wall-sections/{wallSectionId}/problems/{problemId}/delete`.
- Wall reset is `PATCH /api/home/wall-section/{wallSectionId}/reset`.
- UI gating and backend authorization should both be revalidated when role logic changes.
- Problem-page Report submits `reportTargetType: DISCUSSION` and `targetId` = discussion id. Category is required (`INAPPROPRIATE_CONTENT`, `HARASSMENT_BULLYING`, `SPAM`, `OFF_TOPIC`). Reason is required and capped at 250 characters (API max; not 255).
- Duplicate open reports and self-reports return `404` from create-report; the dialog shows an error toast.
- The rest of the moderation loop (admin queue, logbook, notifications, appeals) is documented in `docs/features/moderation.md`.
- Discovery grade-range endpoints use `/api/search/{wallSectionId}?min=&max=&sort=`.
- CORS allows `/api/**` for the frontend origin (covers `/api/home/**` and `/api/search/**`).
- Keyword/text search is deferred to a later sprint (roadmap Sprint 10); Sprint 4 discovery (grade filter/sort) is complete.
- Wall and problem read DTOs expose nullable `imageURL`. A null wall `imageURL` displays `/co-op.png`. A null problem `imageURL` displays `/problem-holder.jpg`. See [`docs/features/wall-problem-images.md`](./wall-problem-images.md).

## Future Enhancements

Potential improvements are tracked separately in `docs/features/future-features.md`.

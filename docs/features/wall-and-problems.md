# Wall and Problems

## Feature Overview

This section documents wall section and climbing problem features currently available in the application.

## Implemented Features

- View wall sections on the main page
- View active problems within a selected wall section
- View problem details, discussion, and suggested grade context
- Admin wall section creation and deletion
- Setter problem creation and deletion
- Setter wall section reset/archive operation
- Authenticated discussion actions (comments, beta upload, grade suggestion)
- Guest browsing mode with read-only wall/problem access and banner messaging
- Owner/admin **soft-delete** for comments and solution betas (`Discussion_Root.deleted_at` / `deleted_by` / `deleted_reason`)
- Backend discovery: filter active problems by inclusive grade range within a wall section
- Backend discovery: sort filtered problems by assigned grade ascending or descending
- Wall section Filter UI: grade range (min–max), sort by most recent / easiest / hardest, Apply / Clear
- Add Problem grade picker uses a grade dropdown (`VB`–`V17`)

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
2. Setter creates or deletes problems.
3. Setter can reset/archive active problems for a section.
4. Updated problem list is refreshed after create/delete/reset operations.

### Discussion Flow

1. Authenticated user opens a problem.
2. User can post a comment.
3. User can upload a beta video and save metadata.
4. User can submit perceived grade.
5. Comment/solution beta **soft-delete** is allowed for owner or admin (when checks pass). Deleted items disappear from the problem timeline after refresh.

Current discussion payload contract is unified through `DiscussionRoot` metadata:

- Discussion entries include `discussionId`, `discussionType`, and `discussionContent`.
- Deletion payloads for both comments and solution betas include `discussionId` and a reason string (max 100).
- Comment deletes send `deletedReason`; beta deletes send `deleteReason`.
- The problem page currently sends `"User deleted their own discussion"` for owner deletes and `"Admin forced delete the discussion"` when an admin deletes another user's item.

## Permissions and Visibility

- Guest users can browse wall and problem content.
- Mutating wall/problem management operations require role-qualified users.
- Setter-gated UI controls are used for problem create/delete/reset actions.
- Deletion of user-generated discussion content is restricted to owner/admin patterns.

## Key Files

- Main page and wall/problem routes:
    - `v-beta/src/app/main-page/page.js`
    - `v-beta/src/app/wall/[wallSectionID]/page.js`
    - `v-beta/src/app/wall/[wallSectionID]/problem/[problemId]/page.js`
- Frontend API modules: 
    - `v-beta/src/api/wallSections.js`
    - `v-beta/src/api/comments.js`
    - `v-beta/src/api/solutionBeta.js`
    - `v-beta/src/lib/discussionDeletion.js`
- Backend controllers/services:
    - `server/src/main/java/app/VBeta/controller/WallSectionController.java`
    - `server/src/main/java/app/VBeta/controller/ProblemDiscussionController.java`
    - `server/src/main/java/app/VBeta/controller/ProblemDiscoveryController.java`
    - `server/src/main/java/app/VBeta/application/ClimbingWallService.java`
    - `server/src/main/java/app/VBeta/application/ProblemDiscussionService.java`
    - `server/src/main/java/app/VBeta/application/ProblemFilteringService.java`

## Limitations and Notes

- Comment/beta delete is a soft delete: discussion root metadata is marked deleted; comment text, beta metadata, and GCS objects stay until a later purge/restore flow.
- Problem delete is `PATCH /api/home/wall-sections/{wallSectionId}/problems/{problemId}/delete`.
- Wall reset is `PATCH /api/home/wall-section/{wallSectionId}/reset`.
- UI gating and backend authorization should both be revalidated when role logic changes.
- Discovery grade-range endpoints use `/api/search/{wallSectionId}?min=&max=&sort=`.
- CORS allows `/api/**` for the frontend origin (covers `/api/home/**` and `/api/search/**`).
- Keyword/text search is deferred to a later sprint (roadmap Sprint 9); Sprint 4 discovery (grade filter/sort) is complete.

## Future Enhancements

Potential improvements are tracked separately in `docs/features/future-features.md`.

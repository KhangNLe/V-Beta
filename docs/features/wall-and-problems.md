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
- Owner/admin deletion behavior for comments and solution betas
- Backend discovery: filter active problems by inclusive grade range within a wall section
- Backend discovery: sort filtered problems by assigned grade ascending or descending

## User Flows

### Browse Walls and Problems

1. User opens `/main-page`.
2. User selects a wall section.
3. App loads problems for that section.
4. If a wall section is invalid or missing, user is redirected back to `/main-page`.
5. User opens an individual problem page.

### Problem Discovery by Grade (API)

1. Client requests active problems for a wall section within `lowestGrade`–`highestGrade`.
2. Optional sort selects ascending or descending grade order.
3. API returns matching active problems only (archived problems are excluded).
4. Invalid ranges (`lowest > highest`) fail with `400`; unknown wall sections fail with `404`.

Frontend filter/sort controls and text/keyword search are not shipped yet.

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
5. Comment/solution beta deletion is allowed for owner or admin (when checks pass).

Current discussion payload contract is unified through `DiscussionRoot` metadata:

- Discussion entries include `discussionId`, `discussionType`, and `discussionContent`.
- Deletion payloads for both comments and solution betas include `discussionId`.

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
- Backend controllers/services:
    - `server/src/main/java/app/VBeta/controller/WallSectionController.java`
    - `server/src/main/java/app/VBeta/controller/ProblemDiscussionController.java`
    - `server/src/main/java/app/VBeta/controller/ProblemDiscoveryController.java`
    - `server/src/main/java/app/VBeta/application/ClimbingWallService.java`
    - `server/src/main/java/app/VBeta/application/ProblemDiscussionService.java`
    - `server/src/main/java/app/VBeta/application/ProblemFilteringService.java`

## Limitations and Notes

- Some endpoint semantics are legacy (for example, delete problem is currently exposed as a GET route in backend controller).
- UI gating and backend authorization should both be revalidated when role logic changes.
- Discovery grade-range endpoints use query params (`?min=&max=&sort=`).
- Keyword/text search is deferred (frontend-side filtering of loaded results or a future API).

## Future Enhancements

Potential improvements are tracked separately in `docs/features/future-features.md`.

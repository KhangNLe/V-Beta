# API Endpoints

This document is the API contract reference for the backend service in `server/`.

Base URL (local):

- `http://localhost:8080`

## Authentication Model

- **Public:** no bearer token required.
- **Authenticated:** requires `Authorization: Bearer <firebase_id_token>`.
- **Action-gated:** authenticated + role permission check via backend `ActionDefinition`.

If a bearer token is invalid/expired, the backend returns `401` with:

```json
{"error":"Invalid or expired Firebase token"}
```

## Public Endpoints

### Health and Metadata

- `GET /api/health`
  - Purpose: liveness check.
  - Response: `{ "status": "ok" }`.

- `GET /api/v1/meta`
  - Purpose: app metadata.
  - Response: `{ "name": "<spring.application.name>" }`.

### Wall and Problem Read Endpoints

- `GET /home/wall-sections`
  - Purpose: list wall sections.
  - Response: array of wall sections (`wallSectionID`, `wallSectionName`, `wallSectionInfo`).

- `GET /home/wall-sections/{wallSectionId}/problems`
  - Purpose: list active problems for a wall section.
  - Response: array of problems (`problemId`, `holdColor`, `info`, `createdDate`, `assignedGrade`).

- `GET /home/wall-sections/{wallSectionId}/problems/{problemId}`
  - Purpose: problem detail with discussion and perceived grade.
  - Response:
    - `climbingProblem` (problem details),
    - `perceiveGrade` (aggregate/perceived value),
    - `discussion` (ordered `UserCommentData` entries).

### Problem Discovery (Grade Range / Sort)

Public guest-readable endpoints. Returns only **active** problems. Grade bounds are inclusive. Keyword/text search is not provided by these endpoints.

- `GET /search/{wallSectionId}?min={lowestGrade}&max={highestGrade}`
  - Purpose: list active problems in a wall section within an inclusive grade range (unsorted).
  - Path params:
    - `wallSectionId`
  - Query params:
    - `min` / `max` (`GradeDefinition`, e.g. `V0`, `V5`)
    - `sort` (optional): `asc` or `desc`
  - Response: array of `ClimbingProblemResponse` (`problemId`, `holdColor`, `info`, `createdDate`, `assignedGrade`).
  - Errors:
    - `400` when `min` is harder than `max`
    - `404` when the wall section does not exist

- `GET /search/{wallSectionId}?min={lowest}&max={highest}&sort=asc`
  - Purpose: same filter as above, ordered by assigned grade ascending (easier → harder).

- `GET /search/{wallSectionId}?min={lowest}&max={highest}&sort=desc`
  - Purpose: same filter as above, ordered by assigned grade descending (harder → easier).

## Authenticated Endpoints (No Action Gate)

### Account Session and Current Account

- `POST /api/accounts/session`
  - Purpose: bootstrap/sync account record after Firebase auth.
  - Request body:
    - `username`
    - `email` (token claim email may override this value server-side)
  - Response: account session payload (`id`, `username`, `email`, `firebaseUid`, `roleName`).

- `GET /api/account`
  - Purpose: fetch current authenticated account profile.
  - Response: `userId`, `username`, `email`, `role`.

- `DELETE /api/account/deletion`
  - Purpose: delete current authenticated account.
  - Response: `200` with empty body.

### Discussion / Beta (Authenticated)

- `POST /discussion/add-comments`
  - Purpose: add a comment to a problem.
  - Request body:
    - `problemId`
    - `commentInfo`
  - Response: `201` with created `UserCommentData`:
    - `discussionId`
    - `userId`
    - `username`
    - `parentCommentId`
    - `discussionType`
    - `discussionContent`
    - `createdDate`

- `POST /discussion/solution-beta/upload-url`
  - Purpose: generate signed upload URL for solution beta video.
  - Request body:
    - `fileName`
    - `contentType`
    - `problemId`
    - `wallSectionId`
  - Response:
    - `signedURL`
    - `method` (usually `PUT`)
    - `uploadObjectName`
    - `publicURL`

- `POST /discussion/solution-beta/save`
  - Purpose: persist uploaded beta metadata.
  - Request body:
    - `problemId`
    - `objectFileName`
    - `videoURL`
  - Response: `UserCommentData` record.

- `DELETE /discussion/solution-beta`
  - Purpose: delete a solution beta entry.
  - Request body:
    - `userId`
    - `problemId`
    - `discussionId`
    - `publicUrl`
  - Response: `200` (empty body).

## Action-Gated Endpoints

### Account Admin

- `GET /api/accounts`
  - Required action: `VIEW_ACCOUNTS`
  - Purpose: list all accounts.
  - Response: array of account records.

- `PATCH /api/accounts/{userId}/role`
  - Required action: `CHANGE_ROLE`
  - Purpose: promote/demote account role.
  - Request body:
    - `roleType` (`CLIMBER`, `SETTER`, `ADMIN`)
  - Response: updated account record.

### Wall and Problem Management

- `POST /home/wall-section/creation`
  - Required action: `CREATE_WALL`
  - Purpose: create wall section.
  - Request body:
    - `wallSectionName`
    - `wallSectionInfo`
  - Response: created wall section.

- `DELETE /home/wall-section/{wallSectionId}/delete`
  - Required action: `DELETE_WALL`
  - Purpose: delete wall section.
  - Response: `200` (empty body).

- `POST /home/wall-section/{wallSectionId}/reset`
  - Required action: `RESET_WALL`
  - Purpose: reset/archive active problems for a wall section.
  - Response: `200` (empty body).

- `POST /home/wall-sections/{wallSectionId}/problems/create`
  - Required action: `CREATE_PROBLEM`
  - Purpose: create climbing problem.
  - Request body:
    - `holdColor`
    - `info`
    - `assignedGrade`
  - Response: created problem record.

- `GET /home/wall-sections/{wallSectionId}/problems/{problemId}/delete`
  - Required action: `DELETE_PROBLEM`
  - Purpose: delete problem and return updated section problems.
  - Response: array of remaining problems.

### Discussion Authorization

- `POST /discussion/problems/{problemId}/suggest-grade`
  - Required action: `GRADE_PROBLEM`
  - Purpose: submit/replace user perceived grade.
  - Request body:
    - `perceiveGrade`
  - Response: updated problem detail payload.

- `DELETE /discussion/comment/delete`
  - Required action: `DELETE_COMMENT`
  - Purpose: delete comment.
  - Request body:
    - `authorId`
    - `problemId`
    - `discussionId`
    - `commentContent`
  - Additional service rule: requester must be comment owner or admin.
  - Response: `200` (empty body).


## Related Docs

- `docs/api/error-handling.md`
- `docs/features/authentication-and-roles.md`
- `docs/features/wall-and-problems.md`

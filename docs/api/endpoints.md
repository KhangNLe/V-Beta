# API Endpoints

This document is the API contract reference for the backend service in `server/`.

Base URL (local):

- `http://localhost:8080`

All application routes are under `/api`. CORS is configured for `/api/**`.

## Authentication Model

- **Public:** no bearer token required.
- **Authenticated:** requires `Authorization: Bearer <firebase_id_token>`.
- **Action-gated:** authenticated + role permission check via backend `ActionDefinition`.

If a bearer token is invalid/expired, the backend returns `401` with:

```json
{"error":"Invalid or expired Firebase token"}
```

### Controller error mapping

Most controllers catch service failures themselves (plain-text body, not a JSON envelope):

- `RuntimeException` → typically **404**
  - wall/problem write endpoints (`create`/`delete`/`reset`) → **400**
  - `GET /api/notification/short` → **401**
- uncaught `Exception` → **500**
- bean-validation failures (`@Valid`) still return **400** from Spring

Authorization failures thrown as `RuntimeException` therefore follow the controller mapping above (usually 404 or 400), not a dedicated 403.

Account session (`POST /api/accounts/session`) still throws `ResponseStatusException` **401** when the security context is missing.

## Public Endpoints

### Health and Metadata

- `GET /api/health`
  - Purpose: liveness check.
  - Response: `{ "status": "ok" }`.

- `GET /api/v1/meta`
  - Purpose: app metadata.
  - Response: `{ "name": "<spring.application.name>" }`.

### Wall and Problem Read Endpoints

- `GET /api/home/wall-sections`
  - Purpose: list wall sections.
  - Response: array of wall sections (`wallSectionID`, `wallSectionName`, `wallSectionInfo`).

- `GET /api/home/wall-sections/{wallSectionId}/problems`
  - Purpose: list active problems for a wall section.
  - Response: array of problems (`problemId`, `holdColor`, `info`, `createdDate`, `assignedGrade`).

- `GET /api/home/wall-sections/{wallSectionId}/problems/{problemId}`
  - Purpose: problem detail with discussion and perceived grade.
  - Response:
    - `climbingProblem` (problem details),
    - `perceiveGrade` (aggregate/perceived value),
    - `discussion` (ordered `UserDiscussionData` entries).

### Problem Discovery (Grade Range / Sort)

Public guest-readable endpoints. Returns only **active** problems. Grade bounds are inclusive. Keyword/text search is not provided by these endpoints.

- `GET /api/search/{wallSectionId}?min={lowestGrade}&max={highestGrade}`
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

- `GET /api/search/{wallSectionId}?min={lowest}&max={highest}&sort=asc`
  - Purpose: same filter as above, ordered by assigned grade ascending (easier → harder).

- `GET /api/search/{wallSectionId}?min={lowest}&max={highest}&sort=desc`
  - Purpose: same filter as above, ordered by assigned grade descending (harder → easier).

## Authenticated Endpoints (No Action Gate)

### Account Session and Current Account

Account payloads use `UserAccountDTO`: `userId`, `username`, `email`, `role`. The API does **not** return `firebaseUid` or `roleName`.

- `POST /api/accounts/session`
  - Purpose: bootstrap/sync account record after Firebase auth.
  - Request body:
    - `username`
    - `email` (token claim email may override this value server-side)
  - Response: `201` with `UserAccountDTO`.

- `GET /api/account`
  - Purpose: fetch current authenticated account profile.
  - Response: `200` with `UserAccountDTO`.

- `DELETE /api/account/deletion`
  - Purpose: delete current authenticated account.
  - Response: `200` with empty body.

### Discussion / Beta (Authenticated)

- `POST /api/discussion/add-comments`
  - Purpose: add a comment to a problem.
  - Request body:
    - `problemId`
    - `commentInfo`
  - Response: `201` with created `UserDiscussionData`:
    - `discussionId`
    - `userId`
    - `username`
    - `parentCommentId`
    - `discussionType`
    - `discussionContent`
    - `createdDate`

- `GET /api/discussion/solution-beta/upload-url`
  - Purpose: generate signed upload URL for solution beta video.
  - Request: JSON body (not query params):
    - `fileName`
    - `contentType`
    - `problemId`
    - `wallSectionId`
  - Response:
    - `signedURL`
    - `method` (usually `PUT`)
    - `uploadObjectName`
    - `publicURL`

- `POST /api/discussion/solution-beta/save`
  - Purpose: persist uploaded beta metadata.
  - Request body:
    - `problemId`
    - `objectFileName`
    - `videoURL`
  - Response: `201` with `UserDiscussionData` record.

- `DELETE /api/discussion/solution-beta`
  - Purpose: delete a solution beta entry.
  - Request body:
    - `userId`
    - `problemId`
    - `discussionId`
    - `publicUrl`
  - Response: `200` (empty body).

### Content Reports (Authenticated)

Create-report is **authenticated, not action-gated**. There is no `CREATE_REPORT` value in `ActionDefinition`. Guest callers are rejected by Spring Security (`401`). Reporter identity is taken from the Firebase UID, not the request body.

- `POST /api/report/create`
  - Purpose: create an `OPEN` content report and notify admins.
  - Request body (`ReportRequest`):
    - `reportTargetType` (`DISCUSSION`, `WALL_SECTION`, `CLIMBING_PROBLEM`, `USER_ACCOUNT`)
    - `reportReason` (required, max 250 characters)
    - `reportCategoryName` (`INAPPROPRIATE_CONTENT`, `HARASSMENT_BULLYING`, `SPAM`, `OFF_TOPIC`)
    - `targetId` (id of the typed target row)
  - Server-owned fields: reporter, `OPEN` status, timestamps.
  - Response: `200` with empty body.
  - Side effect: writes a `REPORT_CREATED` event (`target_type = REPORT`, actor = reporter) and unread inbox rows for **admins only**. The reporter is skipped if they are an admin. The content owner is not notified on create.
  - Duplicate rules (service throws; controller currently maps `RuntimeException` to **404**):
    1. Same reporter + same target with status `OPEN` (category ignored).
    2. Same reporter + same target + same category, any status (same category cannot be reused after dismiss).
  - Errors:
    - `400` when required fields are missing/invalid (blank reason, missing enums/`targetId`)
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the reporter account is missing, the target is missing/deleted, the reporter owns the discussion / is the reported user, or a duplicate report already exists
  - Product note: Sprint 5 UI scope is discussion comments/betas (`DISCUSSION`). The API also accepts wall, problem, and user targets.

Admin queue and detail are action-gated (`VIEW_REPORTS`). See Action-Gated Endpoints below.

### Notifications (Authenticated)

Unread inbox read is **authenticated, not action-gated**. Any signed-in role may call it; `REPORT_CREATED` rows are currently fanned out to admins only, so climber/setter inboxes are typically empty for this event.

- `GET /api/notification/short`
  - Purpose: poll unread inbox summaries for the authenticated user (`readAt IS NULL`).
  - Response: `200` array of `QuickNotificationDTO`:
    - `event.eventTypeName` (for example `REPORT_CREATED`)
    - `event.description` (seeded catalog text; does **not** include the report reason)
    - `createdAt`
  - Delivery: client polling is sufficient for Sprint 5 (no WebSocket/SSE/FCM).
  - Errors:
    - `401` when unauthenticated, the Firebase token is invalid, or no account matches the UID

## Action-Gated Endpoints

### Account Admin

- `GET /api/accounts`
  - Required action: `VIEW_ACCOUNTS`
  - Purpose: list all accounts.
  - Response: array of `UserAccountDTO` records (`userId`, `username`, `email`, `role`).

- `PATCH /api/accounts/{userId}/role`
  - Required action: `CHANGE_ROLE`
  - Purpose: promote/demote account role.
  - Request body:
    - `roleType` (`CLIMBER`, `SETTER`, `ADMIN`)
  - Response: updated `UserAccountDTO`.

### Wall and Problem Management

- `POST /api/home/wall-section/creation`
  - Required action: `CREATE_WALL`
  - Purpose: create wall section.
  - Request body:
    - `wallSectionName`
    - `wallSectionInfo`
  - Response: created wall section.

- `DELETE /api/home/wall-section/{wallSectionId}/delete`
  - Required action: `DELETE_WALL`
  - Purpose: delete wall section.
  - Response: `200` (empty body).

- `PATCH /api/home/wall-section/{wallSectionId}/reset`
  - Required action: `RESET_WALL`
  - Purpose: reset/archive active problems for a wall section.
  - Response: `200` (empty body).

- `POST /api/home/wall-sections/{wallSectionId}/problems/create`
  - Required action: `CREATE_PROBLEM`
  - Purpose: create climbing problem.
  - Request body:
    - `holdColor`
    - `info`
    - `assignedGrade`
  - Response: created problem record.

- `PATCH /api/home/wall-sections/{wallSectionId}/problems/{problemId}/delete`
  - Required action: `DELETE_PROBLEM`
  - Purpose: delete problem and return updated section problems.
  - Response: array of remaining problems.

### Discussion Authorization

- `POST /api/discussion/problems/{problemId}/suggest-grade`
  - Required action: `GRADE_PROBLEM`
  - Purpose: submit/replace user perceived grade.
  - Request body:
    - `perceiveGrade`
  - Response: `201` with updated problem detail payload.

- `DELETE /api/discussion/comment/delete`
  - Required action: `DELETE_COMMENT`
  - Purpose: delete comment.
  - Request body:
    - `authorId`
    - `problemId`
    - `discussionId`
    - `commentContent`
  - Additional service rule: requester must be comment owner or admin.
  - Response: `200` (empty body).

### Content Reports (Admin Queue)

- `GET /api/report/reports`
  - Required action: `VIEW_REPORTS` (admin)
  - Purpose: list ranked OPEN report **cases** (grouped by target, not one row per reporter).
  - Query params:
    - none — full queue
    - `reportId` (optional) — one case: all OPEN reports on the same target as that id
  - Ranking: `queueScore = Σ (category weight × OPEN count)` on that target, highest first. Weights: `INAPPROPRIATE_CONTENT` 4, `HARASSMENT_BULLYING` 3, `SPAM` 2, `OFF_TOPIC` 1.
  - Visibility: omits discussion cases owned by the viewer and user-account cases targeting the viewer. Dismissed rows are excluded.
  - Response: `200` `ReportsPayload`:
    - `reports[]` (`ReportPriorityDTO`)
      - `report` (`ReportDTO`): `targetType`; exactly one of `discussion` / `climbingProblem` / `wallSection` / `user`; `reporters[]` (`reportId`, `reporter`, `categoryName`, `reportReason`, `createdAt`)
      - `categories[]`: `categoryName`, `reportCount`, `categoryScore` (`weight × reportCount`)
      - `queueScore` (sum of `categoryScore`)
  - Empty `reports` is a valid `200` (nothing visible, or get-by-id hidden / no remaining OPEN siblings).
  - Errors:
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the account is missing, the caller lacks `VIEW_REPORTS`, or `reportId` does not exist
  - Product note: Sprint 5 queue/detail is built for discussion comments and betas. The mapper also serializes problem/wall/user targets if those rows exist.

## Related Docs

- `docs/api/request-response-examples.md`
- `docs/api/permissions-matrix.md`
- `docs/api/error-handling.md`
- `docs/features/authentication-and-roles.md`
- `docs/features/wall-and-problems.md`

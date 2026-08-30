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
  - `GET /api/notification/all` → **404**
  - `PATCH /api/notification/short` → **404**
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
  - Purpose: **soft-delete** a solution beta discussion. The `Discussion_Root` row stays; child `Solution_Beta` metadata and GCS object are kept. Problem timelines omit rows with `deleted_at` set.
  - Request body:
    - `userId` (discussion author)
    - `problemId`
    - `discussionId`
    - `publicUrl`
    - `deleteReason` (required, max 100 characters)
  - Additional service rule: requester must be the beta owner or an admin. A second delete of an already-deleted discussion fails.
  - Frontend currently sends:
    - `"User deleted their own discussion"` for owner deletes
    - `"Admin forced delete the discussion"` when an admin deletes another user's beta
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
  - Product note: Sprint 5 UI on the problem page submits discussion comments/betas (`DISCUSSION`) via `v-beta/src/api/reports.js`. Category and reason are required; reason max is 250. Own discussions are not reportable. The API also accepts wall, problem, and user targets.

Admin queue and detail are action-gated (`VIEW_REPORTS`). Resolve is action-gated (`MODERATE_REPORT`). Logbook is action-gated (`VIEW_MODERATION_LOGS`). Appeal create is authenticated only. Appeal queue/detail are action-gated (`VIEW_APPEALS`). Appeal resolve is action-gated (`MODERATE_APPEAL`). See Action-Gated Endpoints below.

### Notifications (Authenticated)

Inbox APIs are **authenticated, not action-gated**. Any signed-in role may call them. Recipients only see their own rows. `REPORT_CREATED` and `APPEAL_SUBMITTED` are fanned out to admins only. Queue-resolve writes `REPORT_DISMISSED` / `REPORT_APPROVED` to reporters and `CONTENT_REMOVED` to the owner. Appeal resolve writes `CONTENT_RESTORED` or `APPEAL_DENIED` to the owner.

- `GET /api/notification/short`
  - Purpose: poll unread inbox items for the authenticated user (`readAt IS NULL`).
  - Response: `200` array of `QuickNotificationDTO`:
    - `notificationId`
    - `summary.eventTypeName` (`REPORT_CREATED`, `REPORT_DISMISSED`, `REPORT_APPROVED`, `CONTENT_REMOVED`, `APPEAL_SUBMITTED`, `CONTENT_RESTORED`, `APPEAL_DENIED`)
    - `summary.description` (seeded catalog text; does **not** include the report reason or admin notes)
    - `click` (`NotificationClickDTO`): `kind` plus nullable `reportId` / `wallSectionId` / `problemId` / `discussionId` / `userId`
    - `createdAt`
  - Click mapping: derived at read time from the event's `target_type` (not a stored href). Current moderation events use `target_type = REPORT`, so `kind` is `REPORT_QUEUE` and `reportId` is set. Other kinds (`PROBLEM_DISCUSSION`, `PROBLEM`, `WALL_SECTION`, `ACCOUNT`) are reserved for later event targets.
  - Delivery: client polling is sufficient for Sprint 5 (no WebSocket/SSE/FCM). The client builds the frontend path from `click.kind` and the filled ids.
  - Errors:
    - `401` when unauthenticated, the Firebase token is invalid, or no account matches the UID

- `GET /api/notification/all`
  - Purpose: page of the caller's inbox including **read and unread** rows, newest first.
  - Query params:
    - `offset` (optional, 1-based page number, default `1`)
  - Page size is 10 (`offset=1` is rows 1–10, `offset=2` is 11–20).
  - Response: `200` array of `QuickNotificationDTO` (same shape as `/short`: `notificationId`, `summary`, `click`, `createdAt`). Empty array is a valid `200` when the page has no rows.
  - `readAt` is **not** included in the DTO; the page mixes read and unread rows without a read flag.
  - Frontend: `/notifications` requests this with `offset` (Previous/Next, page size 10) and overlays unread ids from `GET /short` so the UI can still mark read vs unread.
  - Errors:
    - `400` when `offset` is not an integer
    - `401` when unauthenticated or the Firebase token is invalid (Spring Security)
    - `404` when auth context is missing or no account matches the UID (controller maps `RuntimeException` to not-found)

- `PATCH /api/notification/short?notificationId={id}`
  - Purpose: mark one of the caller's notifications as read (`readAt` set). Already-read rows succeed as a no-op.
  - Query params:
    - `notificationId` (required)
  - Response: `200` with empty body.
  - Errors:
    - `400` when `notificationId` is missing
    - `401` when unauthenticated or the Firebase token is invalid (Spring Security)
    - `404` when auth context is missing, the account is missing, the id does not exist, or the row belongs to another user

There is no mark-all-read endpoint in this slice.

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
  - Purpose: **soft-delete** a discussion comment. The `Discussion_Root` row stays; the `Discussion_Comment` child row is kept. Problem timelines omit rows with `deleted_at` set.
  - Request body:
    - `authorId`
    - `problemId`
    - `discussionId`
    - `commentContent`
    - `deletedReason` (required, max 100 characters)
  - Additional service rule: requester must be comment owner or admin. A second delete of an already-deleted discussion fails.
  - Frontend currently sends:
    - `"User deleted their own discussion"` for owner deletes
    - `"Admin forced delete the discussion"` when an admin deletes another user's comment
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
      - `report` (`ReportDTO`): `targetType`; `discussion` / `climbingProblem` / `wallSection` / `user` as applicable; `reporters[]` (`reportId`, `reporter`, `categoryName`, `reportReason`, `createdAt`)
      - Discussion cases also include `climbingProblem` and `wallSection` from the discussion's problem (admin wall/problem context). Problem cases also include `wallSection`.
      - `categories[]`: `categoryName`, `reportCount`, `categoryScore` (`weight × reportCount`)
      - `queueScore` (sum of `categoryScore`)
  - Empty `reports` is a valid `200` (nothing visible, or get-by-id hidden / no remaining OPEN siblings).
  - Errors:
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the account is missing, the caller lacks `VIEW_REPORTS`, or `reportId` does not exist
  - Product note: Sprint 5 queue/detail UI is `/reports` (admin-only). It lists ranked OPEN cases and resolves discussion comments/betas with required notes (`REPORT_DISMISSED` or `CONTENT_REMOVED`). The mapper also serializes problem/wall/user targets if those rows exist.

- `POST /api/moderate/report`
  - Required action: `MODERATE_REPORT` (admin)
  - Purpose: close one or more OPEN discussion reports with a dismiss or remove decision. Each `reportIds` value is one reporter row; omitted siblings stay `OPEN`.
  - Request body (`ModerationRequest`):
    - `reportIds` (required list of report ids)
    - `decision` (`REPORT_DISMISSED` or `CONTENT_REMOVED`; appeal types are rejected)
    - `reason` (required admin notes, stored on `Moderation_Action.admin_notes`)
  - Per eligible report: write a logbook row, set status (`DISMISSED` or `CONTENT_REMOVED`), notify that reporter.
  - `CONTENT_REMOVED` shared side effects (once per discussion in the request): soft-delete `Discussion_Root` and notify the owner with `CONTENT_REMOVED`. If the discussion is already deleted, reports still close and the owner is not notified again.
  - Skipped ids (request still `200`): unknown, already-closed, filed by the acting admin, on a discussion the admin owns, or non-`DISCUSSION` targets.
  - Response: `200` with empty body (including when every id was skipped).
  - Side effects (events `target_type = REPORT`, actor = admin):
    - dismiss → reporter `REPORT_DISMISSED`; owner is not notified
    - remove → reporter `REPORT_APPROVED`; owner `CONTENT_REMOVED` once
  - Errors:
    - `400` when `reportIds` / `decision` / `reason` fail bean validation (missing list, missing decision, blank reason)
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the account is missing, the caller lacks `MODERATE_REPORT`, or `decision` is `APPEAL_APPROVED` / `APPEAL_DENIED` (`Appeal decisions are not supported on this endpoint.`)
  - Product note: Sprint 5 resolve is discussion comments and betas only. Appeals are out of this endpoint.

- `GET /api/moderate/logbook`
  - Required action: `VIEW_MODERATION_LOGS` (admin)
  - Purpose: read append-only `Moderation_Action` rows written by report-queue resolve (dismiss/remove) and appeal resolve (approve/deny). Newest first.
  - Query params:
    - none — page 1 (25 newest rows)
    - `offSetPlace` (optional, default `1`) — 1-based page; page `n` skips `25 × (n - 1)` rows
    - `moderationId` (optional) — one logbook row; when set, `offSetPlace` is ignored
  - Response: `200` `ModerationPayload`:
    - `moderationLogs[]` (`ModerationDTO`)
      - `moderationId`
      - `report` (`ReportDTO`): `targetType`, discussion/problem/wall/user snapshot, `reporters[]` for **that decided report only**
      - `resolvedBy` (`UserAccountDTO`)
      - `decision` (`REPORT_DISMISSED`, `CONTENT_REMOVED`, `APPEAL_APPROVED`, or `APPEAL_DENIED`)
      - `adminNote`
      - `createdAt`
  - Empty `moderationLogs` is a valid `200` (no decisions yet, or the page is past the last row).
  - Errors:
    - `400` when `offSetPlace` is `<= 0`
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the account is missing, the caller lacks `VIEW_MODERATION_LOGS`, or `moderationId` does not exist (`Moderation not found`)
  - Product note: no report-id or date filter in this slice. Appeal resolve writes `APPEAL_APPROVED` / `APPEAL_DENIED` rows here. Sprint 5 UI is admin-only `/logbook` (paged list, read-only detail, `.txt` download).

- `POST /api/moderate/appeal`
  - Authenticated only (not action-gated)
  - Purpose: content owner submits a one-time appeal after their discussion was removed (`CONTENT_REMOVED`).
  - Request body (`AppealRequest`):
    - `reportId` (required)
    - `appealReason` (required, max 250 chars)
  - Eligibility: caller owns the reported discussion; report status is `CONTENT_REMOVED`; no appeal exists for that report yet.
  - Side effects: write an `OPEN` appeal, set report status to `APPEAL_PENDING`, notify admins with `APPEAL_SUBMITTED` (skipping the appellant if they are an admin).
  - Response: `201` with empty body.
  - Errors:
    - `400` when `reportId` / `appealReason` fail bean validation
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the account is missing, the report is missing/ineligible (`Appeal is not allowed`), or an appeal already exists (`Appeal already exists`)
  - Product note: Sprint 5 owner UI is `/appeals?reportId=` (notification deep-link).

- `GET /api/moderate/appeal/notice`
  - Authenticated (owner of the removed discussion)
  - Purpose: deletion notice / user-appeal context for one report (admin removal notes, content snapshot, nested `AppealDTO` when submitted, whether a first appeal is still allowed).
  - Query params:
    - `reportId` (required)
  - Eligibility: caller owns the reported discussion; status is `CONTENT_REMOVED`, `APPEAL_PENDING`, `CONTENT_RESTORED`, or `APPEAL_DENIED`.
  - Response: `200` `OwnerDeletionNoticeDTO`:
    - `reportId`
    - `reportStatus`
    - `adminReason` (from the `CONTENT_REMOVED` logbook row)
    - `report` (`ReportDTO` snapshot: content, category, and reason; `reporters[].reporter` omitted)
    - `appealStatus` (`OPEN` / `APPROVED` / `DENIED`, or omitted/`null` when none)
    - `canAppeal` (`true` only for `CONTENT_REMOVED` with no appeal row)
    - `appeal` (`AppealDTO`, or omitted/`null` when none)
  - Errors:
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the account is missing or the report is missing/ineligible (`Appeal is not allowed` / `Report not found`)

- `GET /api/moderate/appeal`
  - Required action: `VIEW_APPEALS` (admin)
  - Purpose: read OPEN appeals newest-first, one appeal by id, or one appeal by report.
  - Query params:
    - none — OPEN appeal queue
    - `appealId` (optional) — one appeal (any status); when set, `reportId` is ignored
    - `reportId` (optional) — the appeal for that report (powers `/appeal-queue?reportId=`)
  - Product note: admin list/detail UI is `/appeal-queue`.
  - Response: `200` `AppealPayload`:
    - `appeals[]` (`AppealDTO`)
      - `appealId`
      - `report` (`ReportDTO`): `targetType`, discussion/problem/wall/user snapshot, `reporters[]` for **that appealed report only**
      - `appealUser` (`UserAccountDTO`)
      - `appealReason`
  - Empty `appeals` is a valid `200` (no OPEN appeals, or every OPEN appeal was filed by the viewing admin).
  - Errors:
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the account is missing, the caller lacks `VIEW_APPEALS`, or `appealId` / `reportId` does not exist / was filed by the viewing admin (`Appeal not found`)

- `PATCH /api/moderate/appeal`
  - Required action: `MODERATE_APPEAL` (admin)
  - Purpose: approve restore or deny one `OPEN` appeal.
  - Request body (`ModerateAppealRequest`):
    - `appealId` (required)
    - `appealStatus` (`APPROVED` or `DENIED`; `OPEN` is rejected)
    - `adminReason` (required admin notes, max 255 chars, stored on `Appeal.admin_note` and `Moderation_Action.admin_notes`)
  - Eligibility: appeal exists, is `OPEN`, and was not filed by the acting admin.
  - `APPROVED` side effects: restore the soft-deleted discussion (clear `deleted_at` / `deleted_by` / `deleted_reason`), set report status to `CONTENT_RESTORED`, write logbook `APPEAL_APPROVED`, notify the owner with `CONTENT_RESTORED`.
  - `DENIED` side effects: discussion stays deleted, set report status to `APPEAL_DENIED`, write logbook `APPEAL_DENIED`, notify the owner with `APPEAL_DENIED`.
  - Response: `200` with empty body.
  - Product note: `/appeal-queue` detail sends this body (required comments + Approve/Deny).
  - Errors:
    - `400` when `appealId` / `appealStatus` / `adminReason` fail bean validation, or `appealStatus` is `OPEN` (`Invalid appeal status`)
    - `401` when unauthenticated or the Firebase token is invalid
    - `404` when the account is missing, the caller lacks `MODERATE_APPEAL`, or the appeal is missing / already decided / filed by the acting admin (`Appeal not found`)

## Related Docs

- `docs/api/request-response-examples.md`
- `docs/api/permissions-matrix.md`
- `docs/api/error-handling.md`
- `docs/features/authentication-and-roles.md`
- `docs/features/wall-and-problems.md`

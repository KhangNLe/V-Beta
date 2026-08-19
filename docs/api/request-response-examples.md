# Request/Response Examples

This file provides practical API examples for common frontend flows.

Base URL (local):

- `http://localhost:8080`

All application routes are under `/api`. CORS is configured for `/api/**`.

Auth header for protected routes:

```http
Authorization: Bearer <firebase_id_token>
```

## 1) Sync Account Session

### Request

```http
POST /api/accounts/session
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "username": "climber01",
  "email": "climber01@example.com"
}
```

### Response (201)

```json
{
  "userId": 7,
  "username": "climber01",
  "email": "climber01@example.com",
  "role": "CLIMBER"
}
```

## 2) Get Current Account

### Request

```http
GET /api/account
Authorization: Bearer <firebase_id_token>
```

### Response (200)

```json
{
  "userId": 7,
  "username": "climber01",
  "email": "climber01@example.com",
  "role": "CLIMBER"
}
```

## 3) List Wall Sections (Public)

### Request

```http
GET /api/home/wall-sections
```

### Response (200)

```json
[
  {
    "wallSectionID": 1,
    "wallSectionName": "Main Wall",
    "wallSectionInfo": "Comp style problems"
  }
]
```

## 4) Create Wall Section (Admin Action)

### Request

```http
POST /api/home/wall-section/creation
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "wallSectionName": "Training Wall",
  "wallSectionInfo": "Endurance circuits"
}
```

### Response (201)

```json
{
  "wallSectionID": 5,
  "wallSectionName": "Training Wall",
  "wallSectionInfo": "Endurance circuits"
}
```

## 5) Get Problem Detail (Public)

### Request

```http
GET /api/home/wall-sections/1/problems/22
```

### Response (200)

```json
{
  "climbingProblem": {
    "problemId": 22,
    "holdColor": "BLUE",
    "info": "Crimpy sequence",
    "createdDate": "2026-04-20T18:10:00",
    "assignedGrade": "V5"
  },
  "perceiveGrade": "V5",
  "discussion": [
    {
      "discussionId": 301,
      "userId": 7,
      "username": "climber01",
      "parentCommentId": null,
      "discussionType": "COMMENT",
      "discussionContent": "Fun movement.",
      "createdDate": "2026-04-20T19:00:00"
    },
    {
      "discussionId": 302,
      "userId": 7,
      "username": "climber01",
      "parentCommentId": null,
      "discussionType": "BETA",
      "discussionContent": "https://storage.googleapis.com/bucket/wallSection-1/problem-22/uuid-beta_22.mp4",
      "createdDate": "2026-04-20T20:02:00"
    }
  ]
}
```

## 6) Add Comment

### Request

```http
POST /api/discussion/add-comments
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "problemId": 22,
  "commentInfo": "Felt like soft V5."
}
```

### Response (201)

```json
{
  "discussionId": 401,
  "userId": 7,
  "username": "climber01",
  "parentCommentId": null,
  "discussionType": "COMMENT",
  "discussionContent": "Felt like soft V5.",
  "createdDate": "2026-04-21T09:11:00"
}
```

## 7) Request Signed Upload URL for Solution Beta

This is a `GET` with a JSON body (`CloudFileStorageRequest`), not query parameters.

### Request

```http
GET /api/discussion/solution-beta/upload-url
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "fileName": "beta_22.mp4",
  "contentType": "video/mp4",
  "problemId": 22,
  "wallSectionId": 1
}
```

### Response (200)

```json
{
  "signedURL": "https://storage.googleapis.com/...",
  "method": "PUT",
  "uploadObjectName": "wallSection-1/problem-22/uuid-beta_22.mp4",
  "publicURL": "https://storage.googleapis.com/bucket/wallSection-1/problem-22/uuid-beta_22.mp4"
}
```

## 8) Save Solution Beta Metadata

### Request

```http
POST /api/discussion/solution-beta/save
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "problemId": 22,
  "objectFileName": "wallSection-1/problem-22/uuid-beta_22.mp4",
  "videoURL": "https://storage.googleapis.com/bucket/wallSection-1/problem-22/uuid-beta_22.mp4"
}
```

### Response (201)

```json
{
  "discussionId": 402,
  "userId": 7,
  "username": "climber01",
  "parentCommentId": null,
  "discussionType": "BETA",
  "discussionContent": "https://storage.googleapis.com/bucket/wallSection-1/problem-22/uuid-beta_22.mp4",
  "createdDate": "2026-04-20T20:02:00"
}
```

## 9) Suggest Perceived Grade

### Request

```http
POST /api/discussion/problems/22/suggest-grade
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "perceiveGrade": "V6"
}
```

### Response (201)

Returns updated problem-detail payload (same shape as problem detail endpoint).

## 9.1) Soft-Delete Comment

Owner or admin. Marks `Discussion_Root.deleted_at` / `deleted_by` / `deleted_reason`. The comment row remains.

### Request

```http
DELETE /api/discussion/comment/delete
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "authorId": 7,
  "problemId": 22,
  "discussionId": 301,
  "commentContent": "Felt like soft V5.",
  "deletedReason": "User deleted their own discussion"
}
```

Admin deleting another user's comment uses `"Admin forced delete the discussion"` for `deletedReason`.

### Response (200)

Empty body. Subsequent problem-detail timelines omit this discussion.

## 9.2) Soft-Delete Solution Beta

Owner or admin. Marks `Discussion_Root` deleted; solution-beta metadata and GCS object are kept.

### Request

```http
DELETE /api/discussion/solution-beta
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "userId": 7,
  "problemId": 22,
  "discussionId": 402,
  "publicUrl": "https://storage.googleapis.com/bucket/wallSection-1/problem-22/uuid-beta_22.mp4",
  "deleteReason": "User deleted their own discussion"
}
```

Admin deleting another user's beta uses `"Admin forced delete the discussion"` for `deleteReason`.

### Response (200)

Empty body. Subsequent problem-detail timelines omit this discussion.

## 10) Promote/Demote User Role (Admin)

### Request

```http
PATCH /api/accounts/7/role
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "roleType": "SETTER"
}
```

### Response (200)

```json
{
  "userId": 7,
  "username": "climber01",
  "email": "climber01@example.com",
  "role": "SETTER"
}
```

## 11) Reset Wall / Delete Problem (Setter Action)

### Reset wall section

```http
PATCH /api/home/wall-section/1/reset
Authorization: Bearer <firebase_id_token>
```

Response: `200` empty body.

### Delete climbing problem

```http
PATCH /api/home/wall-sections/1/problems/22/delete
Authorization: Bearer <firebase_id_token>
```

Response: `200` array of remaining `ClimbingProblemResponse` records for that wall section.

## 12) Filter Problems by Grade Range (Public)

### Request

```http
GET /api/search/1?min=V0&max=V5
```

### Response (200)

```json
[
  {
    "problemId": 2,
    "holdColor": "RED",
    "info": "RED V0-V1",
    "createdDate": "2026-07-21T12:00:00",
    "assignedGrade": "V0"
  }
]
```

## 13) Filter Problems by Grade Range Ascending (Public)

### Request

```http
GET /api/search/1?min=V0&max=V5&sort=asc
```

### Response (200)

Same `ClimbingProblemResponse` array shape as above, ordered easier → harder by `assignedGrade`.

## 14) Filter Problems by Grade Range Descending (Public)

### Request

```http
GET /api/search/1?min=V0&max=V5&sort=desc
```

### Response (200)

Same array shape, ordered harder → easier by `assignedGrade`.

### Error examples

- `400` when lowest grade is harder than highest (`/api/search/1?min=V10&max=V2`)
- `404` when wall section does not exist

## 15) Create Content Report (Authenticated)

Reporter identity is the Firebase UID. Success is `200` with an empty body. There is no `CREATE_REPORT` action gate.

### Request

```http
POST /api/report/create
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "reportTargetType": "DISCUSSION",
  "reportReason": "Spammy comment",
  "reportCategoryName": "SPAM",
  "targetId": 301
}
```

`reportTargetType` values: `DISCUSSION`, `WALL_SECTION`, `CLIMBING_PROBLEM`, `USER_ACCOUNT`.

`reportCategoryName` values: `INAPPROPRIATE_CONTENT`, `HARASSMENT_BULLYING`, `SPAM`, `OFF_TOPIC`.

`reportReason` is required and at most 250 characters.

### Response (200)

Empty body.

### Error examples

- `400` when `reportReason` is blank or required fields/enums are missing
- `401` when the caller is a guest or the Firebase token is invalid
- `404` when the target is missing/deleted, the reporter owns the discussion / is the reported user, or a duplicate report already exists

Example duplicate reason: `Report already exists`.

## 16) Get Admin Report Queue (Action-gated)

Requires `VIEW_REPORTS`. Each element is one **target** (for example one discussion), not one reporter row. Ranked by `queueScore` descending.

### Request — queue

```http
GET /api/report/reports
Authorization: Bearer <firebase_id_token>
```

### Request — case detail

```http
GET /api/report/reports?reportId=11
Authorization: Bearer <firebase_id_token>
```

`reportId` may be any OPEN (or dismissed) report on that target. Detail still returns only remaining **OPEN** siblings. If the viewer owns the discussion, `reports` is `[]`.

### Response (200)

```json
{
  "reports": [
    {
      "report": {
        "targetType": "DISCUSSION",
        "discussion": {
          "discussionId": 40,
          "userId": 8,
          "username": "alex",
          "parentCommentId": null,
          "discussionType": "COMMENT",
          "discussionContent": "hello",
          "createdDate": "2026-08-16T10:00:00"
        },
        "climbingProblem": null,
        "wallSection": null,
        "user": null,
        "reporters": [
          {
            "reportId": 11,
            "reporter": {
              "userId": 2,
              "username": "sam",
              "email": "sam@example.com",
              "role": "CLIMBER"
            },
            "categoryName": "SPAM",
            "reportReason": "Spammy comment",
            "createdAt": "2026-08-16T15:00:00Z"
          }
        ]
      },
      "categories": [
        {
          "categoryName": "SPAM",
          "reportCount": 1,
          "categoryScore": 2
        }
      ],
      "queueScore": 2
    }
  ]
}
```

Two reporters on the same discussion with different categories produce **one** array element, two `reporters`, two `categories`, and `queueScore` as the sum of `categoryScore`.

### Error examples

- `401` when the caller is a guest or the Firebase token is invalid
- `404` when the account is missing, the caller is not allowed `VIEW_REPORTS`, or `reportId` does not exist

Empty queue or a hidden/dismissed-only case is `200` with `"reports": []`, not 404.

## 17) Resolve Report Queue (Action-gated)

Requires `MODERATE_REPORT`. Success is `200` with an empty body. Each `reportIds` value is one reporter row; omitted OPEN siblings stay open.

### Request — dismiss

```http
POST /api/moderate/report
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "reportIds": [11, 12],
  "decision": "REPORT_DISMISSED",
  "reason": "Does not violate gym guidelines."
}
```

### Request — remove content

```json
{
  "reportIds": [11, 12],
  "decision": "CONTENT_REMOVED",
  "reason": "Does not belong on this wall."
}
```

`decision` values accepted here: `REPORT_DISMISSED`, `CONTENT_REMOVED`. `APPEAL_APPROVED` and `APPEAL_DENIED` are rejected.

`reason` is required and stored on `Moderation_Action.admin_notes`.

### Response (200)

Empty body. Unknown, already-closed, admin-filed, and admin-owned-discussion ids are skipped; the call still returns `200`.

Dismiss notifies each reporter (`REPORT_DISMISSED`) and does not notify the owner. Remove closes each report, soft-deletes the discussion once, notifies each reporter (`REPORT_APPROVED`), and notifies the owner once (`CONTENT_REMOVED`).

### Error examples

- `400` when `reportIds` is missing, `decision` is missing, or `reason` is blank
- `401` when the caller is a guest or the Firebase token is invalid
- `404` when the account is missing, the caller is not allowed `MODERATE_REPORT`, or `decision` is an appeal type (`Appeal decisions are not supported on this endpoint.`)

## 18) Get Unread Notifications (Authenticated)

Poll unread inbox rows (`readAt` is null). Each item includes `notificationId`, catalog `summary`, `click` metadata, and `createdAt`. It does not include the report reason or admin notes.

Any authenticated role may call this endpoint. Callers only receive their own rows. `REPORT_CREATED` inbox rows are written for admins only (the reporter is skipped if they are an admin). Queue-resolve writes `REPORT_DISMISSED` / `REPORT_APPROVED` to reporters and `CONTENT_REMOVED` to the owner.

Current moderation events have `target_type = REPORT`, so `click.kind` is `REPORT_QUEUE` and `click.reportId` is set. Unused click ids are `null`.

### Request

```http
GET /api/notification/short
Authorization: Bearer <firebase_id_token>
```

### Response (200) — admin after a new report

```json
[
  {
    "notificationId": 81,
    "summary": {
      "eventTypeName": "REPORT_CREATED",
      "description": "A user submitted a content report"
    },
    "click": {
      "kind": "REPORT_QUEUE",
      "reportId": 11,
      "wallSectionId": null,
      "problemId": null,
      "discussionId": null,
      "userId": null
    },
    "createdAt": "2026-08-14T19:11:00Z"
  }
]
```

### Response (200) — reporter after dismiss

```json
[
  {
    "notificationId": 82,
    "summary": {
      "eventTypeName": "REPORT_DISMISSED",
      "description": "An admin dismissed a report you submitted"
    },
    "click": {
      "kind": "REPORT_QUEUE",
      "reportId": 11,
      "wallSectionId": null,
      "problemId": null,
      "discussionId": null,
      "userId": null
    },
    "createdAt": "2026-08-18T18:00:00Z"
  }
]
```

### Response (200) — owner after content removed

```json
[
  {
    "notificationId": 83,
    "summary": {
      "eventTypeName": "CONTENT_REMOVED",
      "description": "One of your content had been reported and removed."
    },
    "click": {
      "kind": "REPORT_QUEUE",
      "reportId": 11,
      "wallSectionId": null,
      "problemId": null,
      "discussionId": null,
      "userId": null
    },
    "createdAt": "2026-08-18T18:00:00Z"
  }
]
```

### Error examples

- `401` when unauthenticated, the Firebase token is invalid, or no account matches the UID

## 19) Mark Notification Read (Authenticated)

Marks one of the caller's notifications as read. Success is `200` with an empty body. A second call on the same id is a no-op.

### Request

```http
PATCH /api/notification/short?notificationId=81
Authorization: Bearer <firebase_id_token>
```

### Response (200)

Empty body.

### Error examples

- `400` when `notificationId` is missing
- `401` when the caller is a guest or the Firebase token is invalid
- `404` when the account is missing, the id does not exist, or the row belongs to another user

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

## 16) Get Unread Notifications (Authenticated)

Poll unread inbox rows (`readAt` is null). The payload is event type + description + `createdAt`. It does not include the report reason.

Any authenticated role may call this endpoint. `REPORT_CREATED` inbox rows are written for admins only (the reporter is skipped if they are an admin).

### Request

```http
GET /api/notification/short
Authorization: Bearer <firebase_id_token>
```

### Response (200) — admin after a new report

```json
[
  {
    "event": {
      "eventTypeName": "REPORT_CREATED",
      "description": "A user submitted a content report"
    },
    "createdAt": "2026-08-14T19:11:00"
  }
]
```

### Response (200) — climber/setter with no unread admin events

```json
[]
```

### Error examples

- `401` when unauthenticated, the Firebase token is invalid, or no account matches the UID

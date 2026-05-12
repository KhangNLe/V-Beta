# Request/Response Examples

This file provides practical API examples for common frontend flows.

Base URL (local):

- `http://localhost:8080`

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

### Response (200)

```json
{
  "id": 7,
  "username": "climber01",
  "email": "climber01@example.com",
  "firebaseUid": "firebase-uid-123",
  "roleName": "CLIMBER"
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
GET /home/wall-sections
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
POST /home/wall-section/creation
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
GET /home/wall-sections/1/problems/22
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
POST /discussion/add-comments
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

### Request

```http
POST /discussion/solution-beta/upload-url
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
POST /discussion/solution-beta/save
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

### Response (200)

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
POST /discussion/problems/22/suggest-grade
Content-Type: application/json
Authorization: Bearer <firebase_id_token>
```

```json
{
  "perceiveGrade": "V6"
}
```

### Response (200)

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
  "id": 7,
  "username": "climber01",
  "email": "climber01@example.com",
  "firebaseUid": "firebase-uid-123",
  "roleName": "SETTER"
}
```

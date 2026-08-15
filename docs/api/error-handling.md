# API Error Handling

This document describes how errors are produced by the backend in `server/` and how clients should handle them.

## Error Sources

Errors come from three main layers:

- **Auth filter layer** (`FirebaseAuthFilter`) for invalid Firebase bearer tokens
- **Authorization layer** (`AuthorizationService`) for role/permission checks
- **Service/controller layer** (`ResponseStatusException`, validation, and runtime exceptions)

## Common HTTP Status Codes

- **400 Bad Request**
  - Invalid request payloads
  - Invalid role value on role update
  - Invalid perceived grade input
  - Blank or over-length report reason / missing report enums
- **401 Unauthorized**
  - Missing/invalid bearer token for protected routes
  - Authenticated token exists but no matching account in DB
- **403 Forbidden**
  - Authenticated user lacks required role/action permission
  - User has no valid role assigned
- **404 Not Found**
  - Referenced resource not found (wall/problem/comment/beta/account/report targets)
- **409 Conflict** (route-dependent)
  - Duplicate or conflicting state in create/save operations (if thrown by service logic)
  - Duplicate content report: same reporter already has an `OPEN` report on the target, or already used the same category on that target
- **500 Internal Server Error**
  - Unhandled exceptions or infrastructure failures (storage/DB/internal service issues)

## Authentication Error Behavior

### Invalid/Expired Firebase Token

When bearer token verification fails in `FirebaseAuthFilter`, response is explicit JSON:

```json
{"error":"Invalid or expired Firebase token"}
```

Status: `401`

### Missing/Invalid Auth Context

Authorization failures from `AuthorizationService` can return:

- `401 Missing or invalid authentication token`
- `401 Missing Firebase UID in authentication token`
- `401 Authenticated user account does not exist`

## Authorization Error Behavior

When user is authenticated but not allowed:

- `403 User does not have a valid role assigned`
- `403 Role <ROLE> is not allowed to perform action <ACTION>`

This applies to action-gated endpoints (for example account list/role-change, wall/problem management, grade suggestion, and comment delete).

## Validation and Domain Errors

Validation/domain errors are typically returned as `400`, `404`, `409`, or `500` depending on failure point:

- Invalid payload fields/enums: usually `400`
- Missing domain resource: usually `404`
- Duplicate report create: `409` (`Report already exists`)
- Storage/generation/internal failure: usually `500`

Because there is no custom global error envelope documented yet, clients should not hardcode one exact shape for all non-auth errors.

## Content Report and Notification Errors

`POST /api/report/create` and `GET /api/notification/short` are authenticated, not action-gated. Missing bearer tokens are rejected by Spring Security (`401`). Invalid/expired tokens still use the filter payload above.

Create-report domain errors:

- `400` — blank `reportReason`, missing `reportTargetType` / `reportCategoryName` / `targetId`
- `404` — reporter account missing, target missing/deleted, reporter owns the discussion, or reporter is the reported user
- `409` — duplicate `OPEN` report on the same target, or same category already used on that target

Unread notification errors:

- `401` — missing/invalid auth, or no account matches the Firebase UID (current controller maps lookup failure to `401`)

Create-report does not return `403` for climber/setter: any authenticated role may submit a report. Admin inbox fan-out is a side effect, not an access check on these two routes.

## Practical Error Payload Notes

- **Guaranteed stable auth payload** for invalid token:
  - `{"error":"Invalid or expired Firebase token"}`
- **Other errors** usually come from Spring default handling of `ResponseStatusException` / validation exceptions.
  - Treat message/body as informative but not guaranteed stable across framework changes.

## Client Handling Guidance

Frontend/API client should handle by status class first:

- **401**
  - Clear local session
  - Redirect to login or prompt re-authentication
- **403**
  - Show permission-denied message
  - Keep user on current page where possible
- **404**
  - Show not-found message and offer navigation fallback
- **400**
  - Show field-specific or action-specific validation message
- **409**
  - Show conflict message (for example an existing report on that content)
- **500**
  - Show generic retry/support message and log details client-side

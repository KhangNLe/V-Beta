# API Error Handling

This document describes how errors are produced by the backend in `server/` and how clients should handle them.

## Error Sources

Errors come from three main layers:

- **Auth filter layer** (`FirebaseAuthFilter`) for invalid Firebase bearer tokens
- **Authorization layer** (`AuthorizationService`) for role/permission checks
- **Controller catch mapping** plus Spring validation (`@Valid`) and a few remaining `ResponseStatusException` paths (account session)

## Common HTTP Status Codes

- **400 Bad Request**
  - Invalid request payloads (`@Valid`)
  - Invalid perceived grade / role enum values
  - Blank or over-length report reason / missing report enums
  - Wall/problem write `RuntimeException`s (create/delete/reset), including authorization failures on those routes
- **401 Unauthorized**
  - Missing/invalid bearer token for protected routes (Spring Security or `FirebaseAuthFilter`)
  - Missing auth context on `POST /api/accounts/session`
  - `GET /api/notification/short` when auth/account lookup throws `RuntimeException`
- **404 Not Found**
  - Referenced resource not found (wall/problem/comment/beta/account/report targets)
  - Most other controller `RuntimeException`s, including action-gated authorization failures and duplicate report creates
- **500 Internal Server Error**
  - Unhandled exceptions or infrastructure failures (storage/DB/internal service issues)

Controllers return the exception message as a **plain-text** body for caught `RuntimeException` / `Exception`. They do not use a shared JSON error envelope.

## Authentication Error Behavior

### Invalid/Expired Firebase Token

When bearer token verification fails in `FirebaseAuthFilter`, response is explicit JSON:

```json
{"error":"Invalid or expired Firebase token"}
```

Status: `401`

### Missing/Invalid Auth Context

`AuthorizationService` throws `RuntimeException` with messages such as:

- `Missing or invalid authentication token`
- `Missing Firebase UID in authentication token`
- `Authenticated user account does not exist`

How that surfaces depends on the controller:

- most routes → **404** with that message
- wall/problem writes → **400**
- `GET /api/notification/short` → **401**
- `POST /api/accounts/session` with no security context → **401** (`ResponseStatusException`)

## Authorization Error Behavior

When the user is authenticated but not allowed, `AuthorizationService` throws `RuntimeException` (`Role <ROLE> is not allowed to perform action <ACTION>` or no valid role). Controllers currently map those to **404** or **400**, not 403.

This applies to action-gated endpoints (for example account list/role-change, wall/problem management, grade suggestion, comment delete, and `GET /api/report/reports`).

## Validation and Domain Errors

- Invalid payload fields/enums: usually `400` (Spring validation)
- Missing domain resource: usually `404`
- Duplicate report create: currently `404` (`Report already exists`) because the report controller maps all `RuntimeException`s to not-found
- Storage/generation/internal failure: usually `500`

Clients should not hardcode one exact JSON shape for all non-auth errors.

## Content Report and Notification Errors

`POST /api/report/create` and `GET /api/notification/short` are authenticated, not action-gated. Missing bearer tokens are rejected by Spring Security (`401`). Invalid/expired tokens still use the filter payload above.

`GET /api/report/reports` is action-gated (`VIEW_REPORTS`). Guest callers are `401`. Climber/setter and other authorization failures currently map to **404**.

Create-report domain errors:

- `400` — blank `reportReason`, missing `reportTargetType` / `reportCategoryName` / `targetId`
- `404` — reporter account missing, target missing/deleted, reporter owns the discussion, reporter is the reported user, or a duplicate report already exists

Admin queue/detail errors:

- `404` — missing account, missing `VIEW_REPORTS`, or unknown `reportId` (`Report not found`)
- `200` with `"reports": []` — empty queue, viewer owns the reported discussion, or no OPEN siblings remain on that target

Unread notification errors:

- `401` — missing/invalid auth, or no account matches the Firebase UID (controller maps lookup failure to `401`)

Create-report does not return `403` for climber/setter: any authenticated role may submit a report. Admin inbox fan-out is a side effect, not an access check on create/poll. Queue/detail **does** require admin `VIEW_REPORTS`.

## Practical Error Payload Notes

- **Guaranteed stable auth payload** for invalid token:
  - `{"error":"Invalid or expired Firebase token"}`
- **Controller-caught errors** are typically the raw exception message as text.
- **Validation errors** use Spring's default `MethodArgumentNotValidException` JSON.

## Client Handling Guidance

Frontend/API client should handle by status class first:

- **401**
  - Clear local session
  - Redirect to login or prompt re-authentication
- **404**
  - Show not-found or permission-denied message (action-gated failures currently use this status)
  - Offer navigation fallback where appropriate
- **400**
  - Show field-specific or action-specific validation message
- **500**
  - Show generic retry/support message and log details client-side

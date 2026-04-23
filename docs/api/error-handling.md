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
- **401 Unauthorized**
  - Missing/invalid bearer token for protected routes
  - Authenticated token exists but no matching account in DB
- **403 Forbidden**
  - Authenticated user lacks required role/action permission
  - User has no valid role assigned
- **404 Not Found**
  - Referenced resource not found (wall/problem/comment/beta/account targets)
- **409 Conflict** (route-dependent)
  - Duplicate or conflicting state in create/save operations (if thrown by service logic)
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

Validation/domain errors are typically returned as `400`, `404`, or `500` depending on failure point:

- Invalid payload fields/enums: usually `400`
- Missing domain resource: usually `404`
- Storage/generation/internal failure: usually `500`

Because there is no custom global error envelope documented yet, clients should not hardcode one exact shape for all non-auth errors.

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
- **500**
  - Show generic retry/support message and log details client-side

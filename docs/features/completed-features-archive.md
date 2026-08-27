# Completed Features Archive

This document tracks completed implementation items that were moved out of the active planning backlog.

Use this file to preserve historical context while keeping `future-features.md` focused on pending work.

## Completed Foundation Deliveries

### A) PostgreSQL Migration and Test Alignment

Status: Completed

Highlights:

- PostgreSQL became the primary backend runtime database path.
- Runtime and test schema/seed scripts were aligned for PostgreSQL.
- Local and CI test bootstrap flow was standardized for backend tests.

Related docs:

- `docs/setup/database-schema.md`
- `docs/setup/local-development.md`
- `docs/testing/test-environments.md`

### B) Unified Discussion Root Foundation

Status: Completed

Highlights:

- Unified discussion root model introduced for comment/beta lifecycle alignment.
- `discussionId`-based API lifecycle operations adopted.
- Referential integrity checks validated in integration tests.

Related docs:

- `docs/implementation-roadmap.md`
- `docs/testing/server-test-report.md`

### C) Discussion Integrity and Query Hardening

Status: Completed

Highlights:

- Discussion read-path indexes added to runtime and test schema bootstrap SQL.
- Discussion ordering hardened to deterministic DB-driven ordering.
- In-memory timeline sorting dependency removed from discussion assembly path.
- Migration verification and rollback guidance documented.

Related docs:

- `docs/setup/database-schema.md`
- `docs/testing/test-strategy.md`
- `docs/testing/server-test-report.md`

### D) Backend Grade-Range Discovery API

Status: Completed (backend slice)

Highlights:

- Public `/api/search` endpoints filter active problems by inclusive grade range within a wall section.
- Ascending and descending grade sort variants are supported.
- Invalid range and missing-wall error behavior covered by integration tests.
- CORS mapping is `/api/**` (includes `/api/search/**`).

Related docs:

- `docs/api/endpoints.md`
- `docs/features/wall-and-problems.md`
- `docs/implementation-roadmap.md`

### E) Wall Section Filter UI for Grade Range and Sort

Status: Completed (frontend slice)

Highlights:

- Filter button/dialog on the wall section page for guests and signed-in users.
- Inclusive min–max grade range with Apply disabled when min is harder than max.
- Sort modes: Most Recent (client `createdDate` desc after `/api/search`), Easiest (`sort=asc`), Hardest (`sort=desc`).
- Clear restores the default wall problems list; active filter hint shown while applied.
- Add Problem assigned grade uses a `VB`–`V17` dropdown.
- Covered by `wall-page.test.js` filter cases.
- Sprint 4 discovery improvements marked completed on the implementation roadmap.

Related docs:

- `docs/features/wall-and-problems.md`
- `docs/testing/manual-test-cases.md` (DISC-04 / DISC-05)
- `docs/implementation-roadmap.md`

### F) Discussion Root Soft Delete

Status: Completed (delete path; restore/purge still future)

Highlights:

- Comment and solution-beta deletes mark `Discussion_Root` with `deleted_at`, `deleted_by`, and `deleted_reason` instead of removing the row.
- Problem timelines omit soft-deleted discussions; comment/beta child rows and GCS objects stay.
- Delete requests require a reason (`deletedReason` on comments, `deleteReason` on betas). The problem page currently sends `"User deleted their own discussion"` or `"Admin forced delete the discussion"`.

Related docs:

- `docs/api/endpoints.md`
- `docs/api/request-response-examples.md`
- `docs/features/wall-and-problems.md`
- `docs/architecture/data-model.md`

### G) Discussion Report UI (Problem Page)

Status: Completed (frontend slice; admin queue/appeal UIs still future)

Highlights:

- Signed-in users see a ⋮ menu on every discussion row (comment and beta). Guests do not.
- **Report** is shown for other users' content; **Delete** remains owner/admin.
- Report dialog requires a category and a reason (max 250 characters) and calls `POST /api/report/create` with `DISCUSSION` + discussion id.
- Submit is disabled while pending or when category/reason are invalid; success and API errors use toasts.
- Covered by `problem-page.test.js` report cases.

Related docs:

- `docs/features/wall-and-problems.md`
- `docs/testing/manual-test-cases.md` (DISC-06)
- `docs/api/endpoints.md`

### H) Personal Notifications Inbox UI

Status: Completed (frontend slice; `/reports` and `/appeals` landings are stubs)

Highlights:

- Signed-in navbar bell polls unread rows with `GET /api/notification/short`, marks one read with `PATCH /api/notification/short?notificationId=`, and deep-links by event type.
- `/notifications` pages the full inbox with `GET /api/notification/all?offset=` (1-based, 10 rows). Previous/Next appear when a page is full.
- `QuickNotificationDTO` omits `readAt`; the client overlays unread ids from `/short` so the all-inbox list can still show read vs unread.
- Report-queue event types go to `/reports?reportId=`; appeal/deletion types go to `/appeals?reportId=`.
- Covered by `notifications.test.js`, `notifications-page.test.js`, `NotificationBell.test.js`, and `notificationNavigation.test.js`.

Related docs:

- `docs/features/authentication-and-roles.md`
- `docs/architecture/frontend-architecture.md`
- `docs/testing/manual-test-cases.md` (NOTIF-01)
- `docs/api/endpoints.md`

## Moved Out of Future Queue

The following future backlog topics were completed and removed from `future-features.md`:

- Merge `User_Comment` and `User_Beta` into unified discussion flow.
- PostgreSQL migration completion and test alignment baseline.
- Soft-delete fields and hide-from-timeline behavior for `discussion_root` (restore and delayed GCS purge remain in `future-features.md`).

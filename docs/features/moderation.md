# Moderation (Sprint 5)

End-to-end trust-and-safety loop shipped on `sprint5/moderation_mvp`: signed-in users report comments and solution betas; admins review a ranked queue; decisions are logged; reporters and owners are notified in-app; deleted-content owners may submit one appeal for admin restore or deny.

## MVP contract (shipped)

- **Reportable content:** discussion comments and solution beta videos only.
- **Who can report:** any signed-in account except the discussion owner. Guests cannot report.
- **Report flow:** problem-page ⋮ menu → **Report** → category + required reason (max 250) → `POST /api/report/create` → OPEN admin queue → admins receive `REPORT_CREATED`.
- **Notification clicks:** the navbar bell and `/notifications` rows mark the item read, then go **directly** to the target page (`/reports`, `/appeals`, or `/appeal-queue`). **Show all notifications** opens the paged inbox at `/notifications`.
- **Categories (queue weight, high to low):** inappropriate content → harassment/bullying → spam → off-topic.
- **Queue ranking:** category weight first (score = Σ weight × OPEN count), then report date/time.
- **Admin actions:** **Dismiss** or **Approve deletion** with required notes (max 255). Writes the logbook. Notifies each reporter. On deletion, also notifies the content owner with the admin reason.
- **Appeals:** the owner opens `/appeals?reportId=` (deletion reason, category/reason without reporter identity) and may submit **one** appeal. Admins review on a separate `/appeal-queue` page and **Approve** (restore) or **Deny** (keep removed).

## Pages

| Who | Route | Purpose |
|---|---|---|
| Any signed-in user | Problem page ⋮ **Report** | Submit a discussion report |
| Any signed-in user | Navbar bell, `/notifications` | Unread poll + paged inbox |
| Owner | `/appeals?reportId=` | Deletion notice + one-time appeal form |
| Admin | `/reports` | Ranked OPEN report queue + resolve |
| Admin | `/logbook` | Append-only decisions (read-only) |
| Admin | `/appeal-queue` | OPEN appeals + approve/deny |

## Notification routing

| Event | Recipients | Click target |
|---|---|---|
| `REPORT_CREATED` | Admins (reporter skipped if they are an admin) | `/reports?reportId=` |
| `REPORT_DISMISSED` | Reporters | `/reports?reportId=` |
| `REPORT_APPROVED` | Reporters | `/reports?reportId=` |
| `CONTENT_REMOVED` | Content owner | `/appeals?reportId=` |
| `APPEAL_SUBMITTED` | Admins (appellant skipped if they are an admin) | `/appeal-queue?reportId=` |
| `CONTENT_RESTORED` | Content owner | `/appeals?reportId=` |
| `APPEAL_DENIED` | Content owner | `/appeals?reportId=` |

## APIs (summary)

- `POST /api/report/create` — authenticated; discussion comments/betas
- `GET /api/report/reports` — `VIEW_REPORTS`; ranked OPEN queue or `?reportId=`
- `POST /api/moderate/report` — `MODERATE_REPORT`; dismiss or remove
- `GET /api/moderate/logbook` — `VIEW_MODERATION_LOGS`; paged or `?moderationId=`
- `GET /api/notification/short`, `GET /api/notification/all`, `PATCH /api/notification/short` — own inbox
- `GET /api/moderate/appeal/notice` — owner deletion notice
- `POST /api/moderate/appeal` — owner one-time appeal
- `GET /api/moderate/appeal` — `VIEW_APPEALS`; queue or `?appealId=` / `?reportId=`
- `PATCH /api/moderate/appeal` — `MODERATE_APPEAL`; `ModerateAppealRequest` (`appealId`, `APPROVED`/`DENIED`, `adminReason`)

Full contracts: `docs/api/endpoints.md`, `docs/api/permissions-matrix.md`, `docs/api/request-response-examples.md`.

## Explicitly out of scope (still true)

- Automated / ML moderation
- Reporting wall sections or problems
- Email or push delivery (in-app inbox only)
- Delayed GCS purge of soft-deleted beta objects (restore via appeal **Approve** is shipped)

## Original contract vs shipped

The Sprint 5 draft asked for an optional ≤255 character report note and for notification clicks to land on the personal inbox first. Shipped behavior:

- Report **reason is required**, max **250** (API and UI).
- Admin action notes are required, max **255**.
- Inbox clicks go **straight** to `/reports`, `/appeals`, or `/appeal-queue`. `/notifications` is history, not the first hop.
- Owner appeal and admin review are **separate pages**. Admins can **deny** as well as restore.

## Tests and manual cases

- DISC-06, NOTIF-01, REPORT-01, LOGBOOK-01, APPEAL-01, APPEAL-02
- Frontend: `problem-page`, `reports-page`, `logbook-page`, `appeals-page`, `appeal-queue-page`, notification bell/inbox, RoleNavbar
- Backend: report, notification, moderation, and appeal service/controller tests

## Related docs

- `docs/user-manual.md` (B3, B4, C–F)
- `docs/features/authentication-and-roles.md`
- `docs/features/wall-and-problems.md`
- `docs/features/completed-features-archive.md` (G–L)
- `docs/implementation-roadmap.md` (Sprint 5 completed)

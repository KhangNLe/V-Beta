# Implementation Roadmap

This roadmap tracks only active and upcoming work. Completed foundation delivery is documented in release/readiness docs and sprint reports.

## Planning Principles

- Prioritize user-visible value on top of the stabilized platform foundation.
- Keep each sprint reviewable with measurable acceptance criteria.
- Preserve backward compatibility unless a contract change is explicitly planned.
- Update tests and docs in the same sprint as implementation.

## Current Sprint

Sprint 6 (Wall and Problem Images) is in progress. Sprint 5 (Moderation MVP) is complete.

## Upcoming Sprints

### Sprint 6: Wall and Problem Images

Status: In progress

Estimated Duration: 2 weeks

### Summary

Deliver image support for wall sections and climbing problems: admins upload wall photos, setters upload problem photos, and all users (including guests) view thumbnails with click-to-expand. Builds on the existing GCS signed-upload pattern used for solution betas.

### Scope

**Phase 1 — Contract and schema**

- Data/storage contract and lifecycle rules
- Nullable paired image columns on `Wall_Section` and `Climbing_Problem`
- Agreed API response field: `imageUrl: string | null`

**Phase 2 — Backend**

- `UPLOAD_WALL_SECTION_IMAGE` / `UPLOAD_PROBLEM_IMAGE` permissions
- Signed upload URL + metadata save endpoints
- `imageUrl` on wall/problem list, detail, and search DTOs

**Phase 3 — Frontend**

- Admin wall image upload/replace
- Setter problem image upload/replace
- Thumbnails + lightbox on main page, wall page, and problem page

**Phase 4 — Quality**

- Backend/frontend tests, manual cases, API docs

### Acceptance Criteria

- [ ] Schema and contract documented; bootstrap SQL aligned in runtime + test schemas
- [ ] Admin can upload/replace wall section image via authenticated UI
- [ ] Setter can upload/replace problem image via authenticated UI
- [ ] Unauthorized roles cannot upload via API
- [ ] Read APIs return `imageUrl: string | null` on wall and problem payloads
- [ ] UI supports click-to-view; null images show placeholder
- [ ] MIME type and 8 MB size limits enforced server-side
- [ ] Tests and docs updated for Sprint 6

### Explicitly out of scope (Sprint 6 / v1)

- Image galleries, in-app cropping, climber-uploaded photos
- Dedicated delete-image endpoint (replace-only)
- Immediate GCS purge on problem delete
- `image_content_type` / `image_uploaded_at` columns

### Notes

- Contract and plan: `docs/features/wall-problem-images.md`
- Schema: `docs/setup/database-schema.md` (Wall / Problem Image Columns)
- Deferred from `docs/features/future-features.md` item 7

### Sprint 7: API Reliability

Estimated Duration: 2 weeks

Focus:

- Centralized server error handling with `@RestControllerAdvice`
- Standardized error payload contract (`code`, `message`, `status`, `path`, `timestamp`)
- Frontend/API client parsing alignment

### Sprint 8: Discussion Scalability

Estimated Duration: 2 weeks

Focus:

- Cursor pagination for problem discussions
- Continuation-token contract and deterministic feed retrieval
- Performance verification on larger datasets

### Sprint 9: UX Enhancements

Estimated Duration: 2 weeks

Focus:

- Profile images
- Account activity history
- Perceived-grade detail views

### Sprint 10: Problem Text Search (Later)

Estimated Duration: 1–2 weeks

Focus:

- Keyword / free-text search for climbing problems (and optionally wall sections)
- Prefer client-side filtering first if lists stay small; add a backend search query only if needed
- Not part of Sprint 4 discovery (grade filter/sort already covers current discovery needs); scheduled after Sprint 9

## Completed Sprints

### Sprint 5: Moderation MVP

Status: Completed

### Summary

Delivered an end-to-end moderation loop: signed users report comments and solution betas; admins review a ranked queue; decisions are logged; reporters/owners are notified in-app; deleted-content owners may submit one appeal for admin restore or deny.

### Scope delivered

- Report from the discussion ⋮ menu (comments and betas; category + required reason, max 250)
- Ranked admin report queue/detail (`/reports`) with dismiss or approve deletion and required notes (max 255)
- Append-only moderation logbook (`/logbook`)
- In-app notifications (navbar bell + `/notifications`); clicks go directly to `/reports`, `/appeals`, or `/appeal-queue`
- Owner deletion notice + one-time appeal (`/appeals?reportId=`)
- Admin appeal queue + approve/deny (`/appeal-queue`, `PATCH /api/moderate/appeal`)

### Category priority (queue ranking)

1. Inappropriate content
2. Harassment/bullying
3. Spam
4. Off-topic
Then by report date/time.

### Acceptance Criteria

- [x] Signed-in users can report comments and betas from the discussion ⋮ menu
- [x] Reports enter an admin queue ranked by category priority then time
- [x] Admins can view report detail (wall/problem, reason, reported content) and approve deletion or dismiss with required notes
- [x] Moderation decisions are written to a logbook
- [x] In-app notifications notify admins of new reports and notify reporter/owner of outcomes
- [x] Personal notifications page supports redirect into queue/detail flows
- [x] Deleted-content owners can submit one appeal; admin can approve restore or deny
- [x] Schema, APIs, UI, tests, and docs updated for the moderation MVP

### Explicitly out of scope (later)

- Automated/ML moderation
- Reporting wall sections or problems (comments/betas only)
- External email/push channels beyond in-app notifications
- Delayed GCS purge of soft-deleted beta objects

### Notes

- Feature overview: `docs/features/moderation.md`
- API: `docs/api/endpoints.md`, `docs/api/permissions-matrix.md`, `docs/api/request-response-examples.md`
- Manual cases: DISC-06, NOTIF-01, REPORT-01, LOGBOOK-01, APPEAL-01, APPEAL-02

### Sprint 4: Discovery Improvements for Wall Sections and Problems

Status: Completed

### Summary

Delivered discovery improvements for wall sections and climbing problems via grade-range filtering and sort controls.

### Scope delivered

- Grade-range filters (backend API + wall-page UI)
- Sort by most recent, easiest, or hardest (easiest/hardest via `/api/search` sort; most recent client-side by `createdDate`)

### Explicitly out of scope (deferred)

- Keyword / free-text search for problems → Sprint 9

### Acceptance Criteria

- [x] API supports grade-range filter and asc/desc sort queries
- [x] UI exposes filters/sort with stable state handling
- [x] End-to-end discovery flow tested (DISC-04 API + DISC-05 UI; backend/frontend automated coverage)

### Notes

- Public endpoints: `GET /api/search/{wallSectionId}?min=&max=&sort=asc|desc`
- Invalid ranges (`min > max`) return `400`; missing walls return `404`; `/api/search/**` is guest-readable and CORS-enabled via `/api/**`
- Wall Filter dialog: grade range, sort radios, Apply / Clear; Apply dimmed when min is harder than max
- Related issues: #10 (parent), #31 (backend), #32 (frontend)
- Feature docs: `docs/features/wall-and-problems.md`, completed archive D/E

## Definition of Done (Applies to Every Sprint)

- Feature and technical docs are updated in `docs/`.
- API contract docs are updated when request/response behavior changes.
- Testing docs and regression matrix entries are updated.
- Release-readiness implications are documented.
- Known limitations are reviewed and revised as needed.

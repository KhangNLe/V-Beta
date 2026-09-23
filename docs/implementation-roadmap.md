# Implementation Roadmap

This roadmap tracks only active and upcoming work. Completed foundation delivery is documented in release/readiness docs and sprint reports.

## Planning Principles

- Prioritize user-visible value on top of the stabilized platform foundation.
- Keep each sprint reviewable with measurable acceptance criteria.
- Preserve backward compatibility unless a contract change is explicitly planned.
- Update tests and docs in the same sprint as implementation.

## Current Sprint

Sprint 6 (Wall and Problem Images) is complete. Sprint 7 (Unified Discussion) is next.

## Upcoming Sprints

### Sprint 7: Unified Discussion

Status: Next

Estimated Duration: 2 weeks

### Summary

Merge comments and solution betas into one discussion post. A signed-in user can post a written comment and a video beta together. Other users can reply to that discussion. The author receives an in-app notification when someone replies.

### Scope

- One discussion post can include comment text, a beta video, or both
- Replies attach to another discussion (`Discussion_Root.parent_discussion_id` is already nullable for this)
- In-app notification to the discussion author when someone replies
- Problem-page composer and timeline show the combined post and its replies
- Existing separate comment and beta posts stay readable

### Acceptance Criteria

- [ ] A user can publish one discussion that includes a comment and a video beta together
- [ ] A user can publish a comment-only post or a video-only post
- [ ] A user can reply to another discussion
- [ ] The discussion author receives an in-app notification when someone replies
- [ ] Report, soft-delete, and appeal still apply to the unified post and its replies
- [ ] Tests and docs updated for the unified discussion

### Explicitly out of scope

- Email or push notifications (in-app inbox only)
- Cursor pagination and deep thread performance (Sprint 9)
- Reactions, pins, and edit history

### Sprint 8: API Reliability

Estimated Duration: 2 weeks

Focus:

- Centralized server error handling with `@RestControllerAdvice`
- Standardized error payload contract (`code`, `message`, `status`, `path`, `timestamp`)
- Frontend/API client parsing alignment

### Sprint 9: Discussion Scalability

Estimated Duration: 2 weeks

Focus:

- Cursor pagination for problem discussions
- Continuation-token contract and deterministic feed retrieval
- Performance verification on larger datasets

### Sprint 10: UX Enhancements

Estimated Duration: 2 weeks

Focus:

- Profile images (left out of Sprint 6)
- Account activity history
- Perceived-grade detail views

### Sprint 11: Problem Text Search (Later)

Estimated Duration: 1–2 weeks

Focus:

- Keyword / free-text search for climbing problems (and optionally wall sections)
- Prefer client-side filtering first if lists stay small; add a backend search query only if needed
- Not part of Sprint 4 discovery (grade filter/sort already covers current discovery needs); scheduled after Sprint 10

## Completed Sprints

### Sprint 6: Wall and Problem Images

Status: Completed

### Summary

Delivered wall section and climbing problem photos on the existing GCS signed-upload flow. Admins manage wall photos. Setters manage problem photos. Guests and signed-in users see thumbnails. Click-to-expand is only on the climbing problem page.

### Scope delivered

- Nullable paired image columns and read-field `imageURL` on wall and problem payloads
- Admin **Edit wall** / **Add Wall Section** and setter **Edit problem** / **Add Problem**
- Local preview on file pick; bucket upload on **Save changes**, **Add section**, or **Add problem**
- Replace deletes the previous GCS object when the new key differs
- Defaults `/co-op.png` (wall) and `/problem-holder.jpg` (problem)
- HEIC/HEIF converts to JPEG in the browser, with **Uploading iPhone photo…** while that conversion runs
- Problem-page click-to-expand

### Acceptance Criteria

- [x] Schema and contract documented; bootstrap SQL aligned in runtime + test schemas
- [x] Admin can upload/replace a wall section image via authenticated UI
- [x] Setter can upload/replace a problem image via authenticated UI
- [x] Unauthorized roles cannot upload via API
- [x] Read APIs return `imageURL: string | null` on wall and problem payloads
- [x] Thumbnails render; click-to-expand is only on the climbing problem page; null images show a placeholder
- [x] MIME type and 8 MB size limits enforced on metadata save
- [x] Tests and docs updated for Sprint 6

### Explicitly out of scope

- User profile image persistence and UI (Sprint 10)
- Click-to-expand on the main page or wall section page
- Image galleries, in-app cropping, climber-uploaded photos
- `image_content_type` / `image_uploaded_at` columns

### Notes

- Contract: `docs/sprints/wall-problem-images.md`
- Feature summary: `docs/features/wall-problem-images.md`
- Manual cases: WALL-04, WALL-05

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

- Keyword / free-text search for problems → Sprint 11

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

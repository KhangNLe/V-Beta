# Implementation Roadmap

This roadmap tracks only active and upcoming work. Completed foundation delivery is documented in release/readiness docs and sprint reports.

## Planning Principles

- Prioritize user-visible value on top of the stabilized platform foundation.
- Keep each sprint reviewable with measurable acceptance criteria.
- Preserve backward compatibility unless a contract change is explicitly planned.
- Update tests and docs in the same sprint as implementation.

## Current Sprint

## Sprint 5: Moderation MVP

Status: In Progress

### Summary

Deliver an end-to-end moderation loop: signed users report comments and solution betas; admins review a ranked report queue; decisions are logged; reporters/owners are notified; deleted-content owners may submit a one-time appeal for admin-approved restore.

### Estimated Duration

2 weeks

### Scope

- Reporting workflow for comments and solution betas (category + optional ≤255 char reason)
- Admin report queue/detail (ranked by category priority, then datetime)
- Admin resolve actions: approve deletion or dismiss (required admin notes → logbook)
- In-app notifications (new report for admins; outcome notifications for reporter/owner)
- Personal notifications page (click notification lands here, then deep-link to queue/detail)
- Moderation logbook UI/API
- One-time appeal + admin-approved restore of soft-deleted discussion content (comment/beta delete already marks `Discussion_Root`; restore/purge still open)
- Report action in discussion ⋮ menu (alongside owner/admin delete)

### Category priority (queue ranking)

1. Inappropriate content  
2. Harassment/bullying  
3. Spam  
4. Off-topic  
Then by report date/time.

### Acceptance Criteria

- [ ] Signed-in users can report comments and betas from the discussion ⋮ menu
- [ ] Reports enter an admin queue ranked by category priority then time
- [ ] Admins can view report detail (wall/problem, reason, reported content) and approve deletion or dismiss with required notes
- [ ] Moderation decisions are written to a logbook
- [ ] In-app notifications notify admins of new reports and notify reporter/owner of outcomes
- [ ] Personal notifications page supports redirect into queue/detail flows
- [ ] Deleted-content owners can submit one appeal; admin can approve restore or deny
- [ ] Schema, APIs, UI, tests, and docs updated for the moderation MVP

### Explicitly out of scope (later)

- Automated/ML moderation
- Reporting wall sections or problems (comments/betas only)
- External email/push channels beyond in-app notifications (unless already available)

### Planning reference

- Issue breakdown drafts can follow the Sprint 5 moderation MVP contract (reports, notifications, logbook, appeals).
- Related backlog: `docs/features/future-features.md` (#3, #6, #12)
- API contract for the shipped create-report / unread-notification slice: `docs/api/endpoints.md`, `docs/api/permissions-matrix.md`, `docs/api/request-response-examples.md`

## Upcoming Sprints

### Sprint 6: API Reliability

Estimated Duration: 2 weeks

Focus:

- Centralized server error handling with `@RestControllerAdvice`
- Standardized error payload contract (`code`, `message`, `status`, `path`, `timestamp`)
- Frontend/API client parsing alignment

### Sprint 7: Discussion Scalability

Estimated Duration: 2 weeks

Focus:

- Cursor pagination for problem discussions
- Continuation-token contract and deterministic feed retrieval
- Performance verification on larger datasets

### Sprint 8: UX Enhancements

Estimated Duration: 2 weeks

Focus:

- Profile images
- Wall/problem images
- Account activity history
- Perceived-grade detail views

### Sprint 9: Problem Text Search (Later)

Estimated Duration: 1–2 weeks

Focus:

- Keyword / free-text search for climbing problems (and optionally wall sections)
- Prefer client-side filtering first if lists stay small; add a backend search query only if needed
- Not part of Sprint 4 discovery (grade filter/sort already covers current discovery needs)

## Completed Sprints

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

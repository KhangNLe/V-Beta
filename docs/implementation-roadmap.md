# Implementation Roadmap

This roadmap tracks only active and upcoming work. Completed foundation delivery is documented in release/readiness docs and sprint reports.

## Planning Principles

- Prioritize user-visible value on top of the stabilized platform foundation.
- Keep each sprint reviewable with measurable acceptance criteria.
- Preserve backward compatibility unless a contract change is explicitly planned.
- Update tests and docs in the same sprint as implementation.

## Current Sprint

## Sprint 4: Discovery Improvements for Wall Sections and Problems

Status: In Progress

### Summary

Deliver discovery improvements for wall sections and climbing problems.

### Estimated Duration

2 weeks

### Scope

- Grade-range filters (backend API + wall-page UI shipped)
- Sort by most recent, easiest, or hardest (UI; easiest/hardest via `/search` sort; most recent client-side by `createdDate`)

### Explicitly out of scope (later sprint)

- Keyword / free-text search for problems (not required for Sprint 4 discovery)

### Acceptance Criteria

- [x] API supports grade-range filter and asc/desc sort queries
- [x] UI exposes filters/sort with stable state handling
- [ ] End-to-end discovery flow tested

### Backend / Frontend Notes

- Public endpoints: `GET /search/{wallSectionId}?min=&max=&sort=asc|desc` (omit `sort` for unsorted / most-recent client sort).
- Invalid ranges (`min > max`) return `400`; missing wall sections return `404`; `/search/**` is guest-readable and CORS-enabled.
- Wall section page Filter dialog: grade range (min–max), sort radios, Apply / Clear; Apply dimmed when min is harder than max.

## Upcoming Sprints

### Sprint 5: Moderation MVP

Estimated Duration: 2 weeks

Focus:

- Reporting workflow for comments/solution betas
- Admin report queue/history basics
- Initial moderation action audit trail

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

## Definition of Done (Applies to Every Sprint)

- Feature and technical docs are updated in `docs/`.
- API contract docs are updated when request/response behavior changes.
- Testing docs and regression matrix entries are updated.
- Release-readiness implications are documented.
- Known limitations are reviewed and revised as needed.

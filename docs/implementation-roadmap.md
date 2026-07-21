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

- Grade-range filters (backend API shipped)
- Asc/desc sorting by grade (backend API shipped)
- Supporting UI filter/sort state (pending)
- Text search deferred for now (optional later; prefer frontend filter on loaded results or a future backend endpoint)

### Acceptance Criteria

- [x] API supports grade-range filter and asc/desc sort queries
- [ ] UI exposes filters/sort with stable state handling
- [ ] End-to-end discovery flow tested

### Backend Notes

- Public endpoints under `/search/{wallSectionId}/...` return active problems in an inclusive grade range.
- Invalid ranges (`lowest > highest`) return `400`; missing wall sections return `404`.
- Full-text / keyword search is intentionally out of scope for this backend slice.

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

## Definition of Done (Applies to Every Sprint)

- Feature and technical docs are updated in `docs/`.
- API contract docs are updated when request/response behavior changes.
- Testing docs and regression matrix entries are updated.
- Release-readiness implications are documented.
- Known limitations are reviewed and revised as needed.

# Implementation Roadmap

This roadmap prioritizes foundation work first, then product features. It is designed to reduce migration risk and keep feature delivery predictable.

## Planning Principles

- Prioritize platform foundations before major UX expansion.
- Keep each sprint reviewable with measurable acceptance criteria.
- Maintain backward compatibility for existing user flows during internal refactors.
- Update testing and documentation in the same sprint as implementation.

## Sprint 1: PostgreSQL Migration Foundation

### Goal

Make PostgreSQL the primary and validated database path for local development and automated testing.

### Estimated Duration

2-3 weeks

### Scope

- Switch server datasource defaults/configuration to PostgreSQL.
- Convert active schema and seed scripts from MySQL-specific syntax to PostgreSQL-compatible SQL.
- Update test DB setup for PostgreSQL-backed integration tests.
- Resolve dialect and query differences surfaced by backend regression tests.

### Deliverables

- PostgreSQL-ready Spring configuration and environment examples.
- PostgreSQL-compatible schema and seed scripts.
- Updated backend test setup with passing test suite.
- Updated setup docs in server and project docs.

### Acceptance Criteria

- Backend tests pass against PostgreSQL.
- Core backend startup and API health checks succeed with PostgreSQL.
- No MySQL-only SQL remains in active runtime or test paths.

## Sprint 2: Discussion Schema Foundation for Future Subthreads

### Goal

Refactor the discussion data model so it supports future nested replies without requiring immediate UI thread rollout.

### Estimated Duration

2-3 weeks

### Scope

- Introduce a unified discussion root model to anchor both comments and solution betas.
- Add nullable `parent_discussion_id` for future reply chains.
- Bridge existing `User_Comment` and `User_Beta` paths to the new unified discussion structure.
- Preserve current API behavior for existing frontend flows.

### Deliverables

- New schema and migration scripts for discussion root structure.
- Updated backend entities, repositories, and service mappings.
- Data migration/backfill for existing discussion records.
- Integration test coverage for create/read/delete discussion operations.

### Acceptance Criteria

- Existing comment and solution beta flows remain functional.
- New records are persisted in thread-ready discussion schema.
- Referential integrity prevents invalid parent/child discussion links.

## Sprint 3: Hardening and Contract Lock

### Goal

Stabilize and optimize the new database and discussion foundation before additional feature acceleration.

### Estimated Duration

1-2 weeks

### Scope

- Add constraints and indexes for discussion integrity and query performance.
- Add pagination-ready ordering/index strategy for future threaded feed retrieval.
- Update API and testing documentation for schema changes.
- Define migration rollback notes and operational validation checklist.

### Deliverables

- Performance and integrity indexes for discussion queries.
- Updated docs and regression matrix references.
- Migration verification and rollback guidance.

### Acceptance Criteria

- No regressions in core discussion endpoints.
- Discussion queries remain performant on larger datasets.
- Documentation reflects implemented database and schema behavior.

## Next Feature Sprints (After Foundation)

Each feature sprint is estimated at approximately 2 weeks.

1. Discovery UX (2 weeks): search, grade-range filter, and sorting for problems.
2. Moderation MVP (2 weeks): reporting and admin report queue workflow.
3. API reliability (2 weeks): global error handling contract and auth consistency.
4. Discussion scalability (2 weeks): cursor pagination for problem discussions.
5. UX enhancements (2 weeks): profile images, wall/problem images, account activity history, and perceived-grade detail views.

## Definition of Done (Applies to Every Sprint)

- Feature and technical docs are updated in `docs/`.
- API contract docs are updated when request/response behavior changes.
- Testing docs and regression matrix entries are updated.
- Release-readiness implications are documented.
- Known limitations are reviewed and revised as needed.

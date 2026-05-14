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

## Moved Out of Future Queue

The following future backlog topics were completed and removed from `future-features.md`:

- Merge `User_Comment` and `User_Beta` into unified discussion flow.
- PostgreSQL migration completion and test alignment baseline.

# Future and Potential Features

This document tracks ideas that are not fully implemented in the current release.

## How to Use This File

- Keep implemented features in their own feature docs.
- Add only future/potential work here.
- Mark status clearly so readers do not confuse planned work with shipped work.
- For each feature, include both a **Priority** and an **Effort** estimate.
- Move completed backlog items to `docs/features/completed-features-archive.md`.

## Priority Legend

- **High**: high impact and/or urgent; should be scheduled soon
- **Medium**: meaningful improvement; important but not urgent
- **Low**: nice-to-have or exploratory; can be deferred

## Effort Legend

- **Small**: low implementation complexity; typically a focused change
- **Medium**: moderate complexity; usually spans multiple files/components
- **Large**: high complexity; likely needs phased delivery and broader testing

## Candidate Features

### 1) Stronger Authorization Consistency

- **Priority:** Medium
- **Effort:** Medium
- **Area:** Security/Permissions
- **Current Gap:** Authorization style differs across some endpoints/flows.
- **Potential Work:** Standardize permission checks for discussion and beta actions.
- **Dependencies:** Role/action matrix agreement.

### 2) Wall and Problem Quality-of-Life Features

- **Priority:** Medium
- **Effort:** Medium
- **Area:** Wall/Problems
- **Current Gap:** Grade-range filter/sort UI and API exist; broader workflow utilities remain limited.
- **Potential Work:** Bulk/problem lifecycle utilities (text search is tracked separately for a later sprint).
- **Dependencies:** Existing wall/problem APIs and UI patterns.

### 3) Better Content Moderation and Auditability

- **Priority:** High
- **Effort:** Medium
- **Area:** Discussion/Beta
- **Target sprint:** Sprint 5 (in progress)
- **Current Gap:** Deletion and ownership rules exist but limited moderation workflow/audit history.
- **Potential Work:** Add moderation queue, reason codes, notifications, logbook, and appeal/restore.
- **Dependencies:** Backend audit/report model and admin UI.

### 4) Observability and Operational Hardening

- **Priority:** Low
- **Effort:** Medium
- **Area:** Platform
- **Current Gap:** Minimal operational metadata surfaced in-app.
- **Potential Work:** Add structured logs, lightweight admin diagnostics, and usage dashboards.
- **Dependencies:** Logging and monitoring stack choices.

### 5) User Profile Pictures in Community Content

- **Priority:** Medium
- **Effort:** Medium
- **Area:** Account/Discussion UI
- **Current Gap:** User identity in comments and solution beta sections is text-only.
- **Potential Work:** Add profile picture upload/display so each user avatar appears in comments and solution beta entries.
- **Dependencies:** User profile image storage strategy, account schema updates, frontend rendering updates.

### 6) Report Inappropriate Comments and Solution Betas

- **Priority:** High
- **Effort:** Medium
- **Area:** Trust and Safety
- **Target sprint:** Sprint 5 (in progress)
- **Current Gap:** Users cannot flag inappropriate comments or beta content in the current UI. Backend create-report (`POST /api/report/create`), admin queue/detail (`GET /api/report/reports`, `VIEW_REPORTS`), and unread notification poll (`GET /api/notification/short`) are implemented. Resolve/dismiss is not.
- **Potential Work:** Add report actions on comments and solution betas, including reason selection and moderation review workflow.
- **Dependencies:** Report data model, report API endpoints, moderation/admin handling flow.

### 7) Images for Wall Sections and Climbing Problems

- **Priority:** Medium
- **Effort:** Medium
- **Area:** Wall/Problem Experience
- **Current Gap:** Wall sections and climbing problems are currently displayed without dedicated images.
- **Potential Work:** Add image support for each wall section and each climbing problem (upload, storage, display).
- **Dependencies:** Media storage integration, schema/API extensions, frontend image components and fallbacks.

### 8) Text Search for Problems

- **Priority:** Low
- **Effort:** Medium
- **Area:** Wall/Problem Discovery
- **Target sprint:** Later (roadmap Sprint 9) — **not** part of completed Sprint 4
- **Current Gap:** Sprint 4 shipped grade-range filter and most recent / easiest / hardest sort; free-text search is intentionally deferred.
- **Potential Work:** Add client-side text filtering on loaded results first; add a backend text-search endpoint only if lists grow.
- **Dependencies:** UX for search input; optional API contract if server-side search is required.

### 9) Multi-Gym Support with Per-Gym Roles

- **Priority:** Low
- **Effort:** Large
- **Area:** Multi-Tenant / Access Control
- **Current Gap:** Current data model and permissions assume a single gym context.
- **Potential Work:** Expand the platform to support multiple climbing gyms, allow users to search/select gyms, and assign roles per gym (for example, user is ADMIN at one gym and CLIMBER at another).
- **Dependencies:** Gym-level tenancy model, user-gym-role mapping, scoped authorization checks, and gym-aware frontend navigation/filtering.

### 10) Wall Section Reset Notifications

- **Priority:** Medium
- **Effort:** Medium
- **Area:** User Communication / Activity Awareness
- **Current Gap:** Users are not proactively notified when a wall section is reset.
- **Potential Work:** Add a notification system so users are informed when specific wall sections are reset, including in-app notifications and optional email/push channels.
- **Dependencies:** Reset event tracking, notification preference model, delivery mechanism, and frontend notification UI.

### 11) Account Activity History and Quick Navigation

- **Priority:** Medium
- **Effort:** Medium
- **Area:** Account / User Experience
- **Current Gap:** Users cannot view their past comments and solution betas from a centralized account view.
- **Potential Work:** Add an account section where users can see their past comments and solution beta submissions, with direct navigation back to the related currently active problems.
- **Dependencies:** User activity query endpoints, account page UI expansion, active/archived problem status handling, and deep-link routing.

### 12) Admin Reports Log and Developer Event Log

- **Priority:** High
- **Effort:** Large
- **Area:** Moderation / Reliability
- **Target sprint:** Sprint 5 (in progress) for admin reports/logbook; developer event log may follow later
- **Current Gap:** There is no dedicated admin report log for flagged comments/solution betas and no centralized event/error log for developer debugging.
- **Potential Work:** Add an admin-facing reports queue/history for inappropriate content, plus a structured application event log (errors and important events) for developers to diagnose unexpected bugs.
- **Dependencies:** Report/audit data model, admin review UI, event ingestion pipeline, and log retention/access policy.

### 13) Perceived Grade Detail Subpage per Problem

- **Priority:** High
- **Effort:** Medium
- **Area:** Problem Analytics / User Feedback
- **Current Gap:** Problem pages show aggregate perceived difficulty, but not a dedicated breakdown of individual user-submitted perceived grades.
- **Potential Work:** Add a subpage under each climbing problem to display individual user perceived grades, including user attribution and submitted grade values.
- **Dependencies:** Per-user perceived-grade query endpoint, problem-level routing/subpage UI, and privacy/access rules for displaying user-level grade submissions.

### 14) Centralized Server Error Handling with `@RestControllerAdvice`

- **Priority:** Medium
- **Effort:** Small
- **Area:** API Reliability / Developer Experience
- **Current Gap:** Error responses are not fully standardized across all backend failure paths.
- **Potential Work:** Add a centralized global exception handler in the server using Spring `@RestControllerAdvice` and enforce a standardized error message format across all server endpoints (for example: `code`, `message`, `status`, `path`, `timestamp`).
- **Dependencies:** Exception mapping strategy, standardized error payload contract, and frontend/API client error parsing updates.

### 15) Nested Discussion Threads (Replies) on `discussion_root`

- **Priority:** High
- **Effort:** Large
- **Area:** Discussion Architecture / User Experience
- **Current Gap:** Data model foundation exists, but full user-facing threaded reply UX and pagination are not shipped.
- **Potential Work:** Build reply creation/retrieval UX with depth rules and moderation controls on top of existing `discussion_root` parent linkage.
- **Dependencies:** Thread query strategy (recursive CTE or iterative API), frontend threaded rendering, and cursor pagination contract.

### 16) DB-Enforced XOR Integrity for `discussion_root` Children

- **Priority:** High
- **Effort:** Medium
- **Area:** Data Integrity
- **Current Gap:** The application can enforce that a root points to either comment or beta, but the database may still allow drift without stricter constraints.
- **Potential Work:** Add database triggers/check logic so a `discussion_root` of `type='comment'` can only have one `discussion_comment` child and never a `solution_beta` child (and vice versa).
- **Dependencies:** Final table design, trigger migration strategy, and integration tests for invalid insert attempts.

### 17) Discussion Reactions (Like/Helpful) for Comments and Betas

- **Priority:** Medium
- **Effort:** Medium
- **Area:** Community Engagement
- **Current Gap:** Users cannot react to discussion items, making it hard to surface useful betas/comments.
- **Potential Work:** Add a `discussion_reaction` table keyed by `discussion_root_id`, reaction type, and user; support counts and user-specific reaction state in APIs.
- **Dependencies:** Reaction uniqueness constraints, API payload updates, and frontend reaction controls.

### 18) Pinned and Highlighted Discussion Entries per Problem

- **Priority:** Medium
- **Effort:** Medium
- **Area:** Moderation / Content Discovery
- **Current Gap:** Valuable beta videos and comments cannot be elevated within a problem discussion feed.
- **Potential Work:** Allow setters/admins to pin or highlight `discussion_root` entries, with deterministic ordering (pinned first, then chronological).
- **Dependencies:** Moderation permissions, new pin metadata, and timeline sort contract changes.

### 19) Restore and Delayed Purge for Soft-Deleted Discussions

- **Priority:** Medium
- **Effort:** Medium
- **Area:** Moderation / Safety
- **Current Gap:** Comment/beta delete already soft-deletes `discussion_root` (`deleted_at`, `deleted_by`, `deleted_reason`) and hides those rows from problem timelines. There is no admin restore path, and GCS/beta objects are not purged after a retention window.
- **Potential Work:** Admin-approved restore of soft-deleted discussion content, plus a scheduled 30-day purge of GCS objects keyed off `deleted_at` (not upload time).
- **Dependencies:** Restore API/UI, idempotent GCS delete, and appeal/moderation decision wiring.

### 20) Cursor Pagination and Feed Performance for Problem Discussions

- **Priority:** High
- **Effort:** Large
- **Area:** API Performance / Scalability
- **Current Gap:** Cursor pagination API/UX is not implemented yet.
- **Potential Work:** Implement cursor-based pagination on `discussion_root` (for example by `created_at` + `id`) and extend mixed comment/beta feed retrieval for infinite-scroll UX.
- **Dependencies:** Pagination API contract, frontend infinite-scroll support, and continuation-token semantics.

### 21) Edit History for Discussion Content

- **Priority:** Low
- **Effort:** Medium
- **Area:** Trust / Transparency
- **Current Gap:** Comment/beta metadata edits have no history, which reduces transparency during moderation and collaboration.
- **Potential Work:** Add immutable edit history records for `discussion_comment` text changes and `solution_beta` metadata updates, including editor identity and timestamps.
- **Dependencies:** History tables, policy decisions on visible history, and API/UI support for "edited" indicators.

### 22) Server-Side Caching for High-Read Endpoints

- **Priority:** High
- **Effort:** Medium
- **Area:** API Performance / Scalability
- **Current Gap:** Read-heavy endpoints (for example wall sections and per-wall problem lists) can repeatedly hit database/query mapping paths even when data changes infrequently, increasing latency and infrastructure load under concurrent traffic.
- **Potential Work:** Expand Spring cache coverage on read endpoints, use key-scoped cache entries (for example by `wallSectionId`), and pair writes with targeted cache eviction (`@CacheEvict`) to keep data fresh while reducing repeated query work.
- **Dependencies:** Cache provider decision (default in-memory vs Redis), cache TTL/invalidation policy, and benchmark guardrails (p95 latency + error-rate checks in performance tests).

## Change Log

- Keep this section updated when priorities change or items move to implemented docs.

# Data Model

## Overview

The backend data model is relational and centered on climbing content, user roles, discussion artifacts, and Sprint 5 moderation/notification support.

Primary persistence is PostgreSQL. Existing JPA entities live under `server/src/main/java/app/VBeta/domain/model/`, grouped by domain:

- `actions/` — roles, actions, and permissions
- `climb/` — wall sections, problems, grades, lifecycle
- `discussions/` — discussion roots, comments, solution betas
- `user/` — accounts and perceived grades

Moderation tables are defined in bootstrap SQL (`pg-v-beta.sql` / `v_beta_test_schema.sql`). Matching JPA entities can follow in a later Sprint 5 implementation PR.

## Core Entity Groups

### Identity and Access

Located in `domain/model/user/` and `domain/model/actions/`:

- `UserAccount`
  - Core account record, keyed by Firebase UID.
- `GymRole`
  - Role assignment (`CLIMBER`, `SETTER`, `ADMIN`).
- `GymAction`
  - Action definitions mapped to permissions.
- `RolePermission`
  - Join between role and allowed actions.

### Climbing Content

Located in `domain/model/climb/`:

- `WallSection`
  - Logical wall area/group.
- `ClimbingProblem`
  - Problem metadata, assigned grade, lifecycle status, wall association.
- `ClimbingGrade`
  - Grade lookup values (VB..V17 model).

### User Interaction Content

Located in `domain/model/discussions/` and `domain/model/user/`:

- `DiscussionRoot`
  - Unified discussion anchor row for comments and solution betas.
  - Supports future nested discussion via nullable self-reference (`parent_discussion_id`).
  - Stores discussion type (`COMMENT` / `BETA`) and soft-delete metadata (`deleted_by`, `deleted_reason`, `deleted_at`) used for reversible moderation deletes.
- `UserComment`
  - User-to-problem comment anchor.
- `DiscussionComment`
  - Text discussion payload tied to a user comment record.
- `UserBeta`
  - User-to-problem beta relation.
- `SolutionBeta`
  - Uploaded beta metadata (object file/video URL/time).
- `UserPerceiveGrade`
  - User-submitted perceived grade per problem.

### Moderation, Events, and Notifications (Sprint 5 schema)

Defined in runtime/test SQL. Closed workflow values use PostgreSQL enums; extensible labels use lookup tables.

#### Lookup tables

- `Report_Category`
  - Category name + queue `priority` (lower number = higher rank).
  - Seeded: `INAPPROPRIATE_CONTENT` (1), `HARASSMENT_BULLYING` (2), `SPAM` (3), `OFF_TOPIC` (4).
- `Event_Type`
  - Event name + description for notifiable lifecycle events.
  - Seeded: `REPORT_CREATED`, `REPORT_DISMISSED`, `CONTENT_REMOVED`, `APPEAL_SUBMITTED`, `CONTENT_RESTORED`, `APPEAL_DENIED`.

#### Report lifecycle

- `Report`
  - Reporter, optional reason (≤250), category, status, timestamps.
  - Typed target via `report_target_type` plus nullable FKs (`discussion_id`, `problem_id`, `wall_section_id`, `user_id`) with a CHECK that exactly one target is set.
  - Sprint 5 product scope uses `DISCUSSION` only; other targets are reserved for later expansion.
  - Partial unique index `uq_one_open_report_per_user_target` prevents duplicate open reports per user/target.
- `Appeal`
  - One appeal per report (`report_id` unique), appellant user, reason, `appeal_status`, review metadata.
- `Moderation_Action`
  - Append-only admin logbook rows for a report (action type + required notes).
  - Multiple actions per report are allowed (for example remove, then later appeal decision).

#### Events and inbox

- `Events`
  - Happened-fact row: event type, optional `actor_user_id`, `event_target_type`, and typed target FKs with a one-target CHECK.
  - No JSON payload; consumers join related rows for display context.
  - Sprint 5 moderation notifications typically target `REPORT`.
- `Notification`
  - Per-recipient inbox row for an event (`read_at` nullable).
  - Unique on `(event_id, recipient_user_id)`.

#### Key enums

| Enum | Purpose |
|------|---------|
| `report_status` | `OPEN`, `DISMISSED`, `CONTENT_REMOVED`, `APPEAL_PENDING`, `CONTENT_RESTORED`, `APPEAL_DENIED` |
| `report_target_type` | `DISCUSSION`, `WALL_SECTION`, `CLIMBING_PROBLEM`, `USER_ACCOUNT` |
| `moderate_action_type` | `REPORT_DISMISSED`, `CONTENT_REMOVED`, `APPEAL_APPROVED`, `APPEAL_DENIED` |
| `appeal_status` | `OPEN`, `APPROVED`, `DENIED` |
| `event_target_type` | `REPORT`, `DISCUSSION`, `CLIMBING_PROBLEM`, `WALL_SECTION`, `USER_ACCOUNT` |

## Relationship Highlights

- One `GymRole` can belong to many `UserAccount` records.
- One `WallSection` has many `ClimbingProblem` records.
- One `ClimbingProblem` can have many comments and perceived grades.
- One `ClimbingProblem` can have many `DiscussionRoot` entries.
- One `DiscussionRoot` can optionally reference another `DiscussionRoot` as its parent.
- `SolutionBeta` is linked to `UserBeta` for user/problem beta ownership.
- Role permissions are evaluated through role-action mappings (`RolePermission`).
- `UserPerceiveGrade` is keyed per user/problem pair to model one effective grade submission per user per problem.
- One `Report` belongs to one reporter and one category; optionally links to one typed content target.
- One `Report` has at most one `Appeal`.
- One `Report` can have many `Moderation_Action` logbook rows.
- One `Events` row can fan out to many `Notification` rows (one per recipient).

```text
User_Account ──reports──► Report ──category──► Report_Category
                              │
                              ├──► Appeal (0..1)
                              ├──► Moderation_Action (0..n)
                              └──► Events (often target_type = REPORT)
                                        │
                                        └──► Notification (per recipient)
```

## Authorization Data Model

Action-level authorization uses:

- `ActionDefinition` (enum in code)
- role type from `UserAccount -> GymRole`
- permission lookup from `RolePermission`

This model enables action-gated endpoint checks beyond simple authenticated/unauthenticated access.

Dedicated moderation permission seeds (for example `VIEW_REPORTS`, `RESOLVE_REPORT`) are not required for the schema slice; add them when report/admin APIs enforce action checks.

## Schema Management Notes

- Runtime expects an existing schema (`ddl-auto=validate`).
- Runtime schema SQL reference:
  - `server/src/main/resources/db/pg-v-beta.sql` (runtime baseline)
- Test schema SQL reference:
  - `server/src/test/resources/db/v_beta_test_schema.sql`

See setup instructions in:

- `docs/setup/database-schema.md`

## Data Model Evolution Considerations

- Keep entity changes synchronized with SQL schema files and API docs.
- Revalidate role-permission mappings whenever introducing new `ActionDefinition` values.
- For future multi-gym support, add gym-scoped ownership for users/roles/wall content.
- Add new report/event kinds preferentially via `Report_Category` / `Event_Type` inserts; reserve enum alterations for closed workflow/target sets.
- When expanding reportable content beyond discussions, fill the existing typed FK columns and allow the matching `report_target_type` in application code.

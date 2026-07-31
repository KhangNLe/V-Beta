# Database Schema Setup

This project expects an existing PostgreSQL schema at startup.

Important backend behavior:

- `server/src/main/resources/application.properties` sets `spring.jpa.hibernate.ddl-auto=validate`
- That means Spring validates tables/columns but does **not** auto-create schema

If schema is missing or mismatched, backend startup will fail.

## Source of truth for schema

Primary runtime schema SQL:

- `server/src/main/resources/db/pg-v-beta.sql` (targets `v_beta`)

`pg-v-beta.sql` includes:

- core table definitions
- discussion model (`Discussion_Root`, comments, solution betas)
- Sprint 5 moderation/reporting model (reports, appeals, moderation actions, events, notifications)
- baseline seed data for roles/actions/grades/permissions, report categories, and event types
- sample wall/problem/user records for local validation

## Recommended local setup

Use one runtime database:

- Main app DB: `v_beta`

## 1) Create database

```sql
CREATE DATABASE v_beta;
```

## 2) Apply schema to main DB (`v_beta`)

```bash
psql -h 127.0.0.1 -U <SQL_USERNAME> -d v_beta -f server/src/main/resources/db/pg-v-beta.sql
```

## 3) Configure backend env vars

In `server/.env`, confirm:

```bash
SQL_USERNAME=your_sql_user
SQL_PASSWORD=your_sql_password
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=v_beta
```

## 4) Verify schema

From PostgreSQL:

```sql
\dt
SELECT COUNT(*) FROM gym_role;
SELECT COUNT(*) FROM climbing_grade;
```

Expected:

- core tables exist (for example `User_Account`, `Climbing_Problem`, `Wall_Section`, `Discussion_Root`)
- moderation tables exist (`Report`, `Report_Category`, `Appeal`, `Moderation_Action`, `Event_Type`, `Events`, `Notification`)
- seed rows exist in `Gym_Role`, `Climbing_Grade`, `Report_Category`, and `Event_Type`

## 5) Validate with app startup

1. Start PostgreSQL (or Cloud SQL Auth Proxy if using Cloud SQL PostgreSQL).
2. Start backend:

```bash
cd server
./mvnw spring-boot:run
```

3. Confirm no Hibernate schema validation errors.
4. Hit `GET http://localhost:8080/api/health`.

## Discussion Index Verification (Sprint 3)

Discussion timeline/read-path hardening added the following PostgreSQL indexes:

- `idx_discussion_root_problem_created_id`
- `idx_discussion_root_parent_created_id`
- `idx_discussion_root_problem_parent_created_id`

Verify they exist:

```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'discussion_root'
  AND indexname IN (
    'idx_discussion_root_problem_created_id',
    'idx_discussion_root_parent_created_id',
    'idx_discussion_root_problem_parent_created_id'
  )
ORDER BY indexname;
```

Optional plan check (example):

```sql
EXPLAIN ANALYZE
SELECT discussion_id, parent_discussion_id, problem_id, create_at
FROM discussion_root
WHERE problem_id = 1
ORDER BY create_at ASC, discussion_id ASC;
```

## Rollback Guidance (Schema/Index)

If discussion index changes must be rolled back, run:

```sql
DROP INDEX IF EXISTS idx_discussion_root_problem_created_id;
DROP INDEX IF EXISTS idx_discussion_root_parent_created_id;
DROP INDEX IF EXISTS idx_discussion_root_problem_parent_created_id;
```

Notes:

- These rollback statements are idempotent (`IF EXISTS`).
- Keep `server/src/main/resources/db/pg-v-beta.sql` and
  `server/src/test/resources/db/v_beta_test_schema.sql` aligned when reverting.

## Moderation Schema (Sprint 5)

Sprint 5 adds reporting, appeals, moderation audit actions, domain events, and in-app notifications.

Keep `pg-v-beta.sql` and `server/src/test/resources/db/v_beta_test_schema.sql` aligned when changing this model.

### Enums vs lookup tables

Closed workflow / target sets use PostgreSQL enums:

| Enum | Values |
|------|--------|
| `report_status` | `OPEN`, `DISMISSED`, `CONTENT_REMOVED`, `APPEAL_PENDING`, `CONTENT_RESTORED`, `APPEAL_DENIED` |
| `report_target_type` | `DISCUSSION`, `WALL_SECTION`, `CLIMBING_PROBLEM`, `USER_ACCOUNT` |
| `moderate_action_type` | `REPORT_DISMISSED`, `CONTENT_REMOVED`, `APPEAL_APPROVED`, `APPEAL_DENIED` |
| `appeal_status` | `OPEN`, `APPROVED`, `DENIED` |
| `event_target_type` | `REPORT`, `DISCUSSION`, `CLIMBING_PROBLEM`, `WALL_SECTION`, `USER_ACCOUNT` |

Extensible labeled sets use lookup tables:

- `Report_Category` — category name + queue `priority`
- `Event_Type` — event name + description

### Core tables

| Table | Purpose |
|-------|---------|
| `Report` | User-submitted report against one typed target |
| `Report_Category` | Report reason categories ranked for admin queue |
| `Appeal` | One appeal path for content owners after removal |
| `Moderation_Action` | Admin decision / logbook row for a report |
| `Event_Type` | Catalog of notifiable event kinds |
| `Events` | Happened-fact row (who/what/when) |
| `Notification` | Per-recipient inbox row pointing at an event |

### Polymorphic targets (typed FKs)

`Report` and `Events` use `target_type` plus nullable typed FK columns, with a CHECK so exactly one target is set:

- Report targets: `discussion_id`, `problem_id`, `wall_section_id`, `user_id`
- Event targets: `report_id`, `discussion_id`, `problem_id`, `wall_section_id`, `user_id`

Sprint 5 product scope reports discussion content only (`target_type = DISCUSSION`). Wall/problem/user target columns are reserved for later expansion.

`Events.actor_user_id` is nullable so system-generated events do not require a human actor.

There is no JSON `payload` on `Events`; notification UI joins live related rows.

### Indexes

```sql
-- one open report per reporter per concrete target
uq_one_open_report_per_user_target

-- admin queue / inbox reads
idx_report_queue
idx_notification_recipient
```

Verify:

```sql
SELECT indexname
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN (
    'uq_one_open_report_per_user_target',
    'idx_report_queue',
    'idx_notification_recipient'
  )
ORDER BY indexname;
```

### Seed data

Report categories (priority ascending = higher queue rank):

```sql
SELECT category_name, priority
FROM report_category
ORDER BY priority;
```

Expected:

- `INAPPROPRIATE_CONTENT` (1)
- `HARASSMENT_BULLYING` (2)
- `SPAM` (3)
- `OFF_TOPIC` (4)

Event types:

```sql
SELECT event_type_name
FROM event_type
ORDER BY event_type_id;
```

Expected:

- `REPORT_CREATED`
- `REPORT_DISMISSED`
- `CONTENT_REMOVED`
- `APPEAL_SUBMITTED`
- `CONTENT_RESTORED`
- `APPEAL_DENIED`

### Relationship summary

```text
User_Account ──reports──► Report ──category──► Report_Category
                              │
                              ├──► Appeal
                              ├──► Moderation_Action
                              └──► Events (often target_type = REPORT)
                                        │
                                        └──► Notification (recipient)
```

### Fresh DB vs existing DB

- Fresh local/bootstrap: re-run `pg-v-beta.sql` on an empty `v_beta`.
- Existing DB: bare `CREATE TYPE ...` is not idempotent; add enums/tables/indexes carefully (or rebuild from empty schema in local/dev).
- After schema changes, confirm Hibernate validate still passes on startup.

### Rollback guidance (moderation)

Only for local/dev rebuilds. Drop dependents first:

```sql
DROP INDEX IF EXISTS idx_notification_recipient;
DROP INDEX IF EXISTS idx_report_queue;
DROP INDEX IF EXISTS uq_one_open_report_per_user_target;

DROP TABLE IF EXISTS Notification;
DROP TABLE IF EXISTS Events;
DROP TABLE IF EXISTS Event_Type;
DROP TABLE IF EXISTS Moderation_Action;
DROP TABLE IF EXISTS Appeal;
DROP TABLE IF EXISTS Report;
DROP TABLE IF EXISTS Report_Category;

DROP TYPE IF EXISTS event_target_type;
DROP TYPE IF EXISTS appeal_status;
DROP TYPE IF EXISTS moderate_action_type;
DROP TYPE IF EXISTS report_target_type;
DROP TYPE IF EXISTS report_status;
```

Keep test schema SQL aligned if you roll back.

## Common issues

- **Table not found / schema validation failed**
  - Schema not imported into `v_beta`, or imported to wrong DB.

- **Access denied for user**
  - DB user lacks privileges on database and schema.

- **Permission denied for schema public**
  - Grant schema create/usage to your app user in `v_beta`.

- **Wrong DB selected**
  - `DB_NAME` and SQL script target DB are different.

## Review notes for `pg-v-beta.sql`

- `pg-v-beta.sql` is the primary runtime schema bootstrap for `v_beta`.
- Runtime schema aligns with current entity constraints, including:
  - `Gym_Role.role_type` as `NOT NULL`
  - composite primary key on `Role_Permission (role_id, action_id)`
  - `Discussion_Root` with:
    - nullable `parent_discussion_id` (self-FK for future threading)
    - `discussion_type` backed by PostgreSQL enum `discussion_kind`
    - `create_at` timestamp used by runtime entity mapping
    - referential integrity FKs for `problem_id`, `user_id`, and `deleted_by`
  - Sprint 5 moderation tables with:
    - enum-backed statuses / target / action types
    - lookup tables for `Report_Category` and `Event_Type`
    - typed polymorphic FKs + CHECK constraints on `Report` and `Events`
    - partial unique index preventing duplicate open reports per user/target
    - notification uniqueness on `(event_id, recipient_user_id)`

## Notes for future contributors

- Keep schema updates versioned in SQL files and documented in `docs/`.
- If entity models change, update bootstrap SQL and this document together.
- Keep `pg-v-beta.sql` and `v_beta_test_schema.sql` synchronized for moderation changes.
- Prefer lookup-table inserts for new categories/event kinds; prefer enum migrations only for closed workflow states.
- Avoid relying on manual memory for schema changes during team transitions.

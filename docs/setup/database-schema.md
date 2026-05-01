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
- baseline seed data for roles/actions/grades/permissions
- sample wall/problem/user/comment records for local validation

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

- core tables exist (for example `User_Account`, `Climbing_Problem`, `Wall_Section`)
- seed rows exist in `Gym_Role` and `Climbing_Grade`

## 5) Validate with app startup

1. Start PostgreSQL (or Cloud SQL Auth Proxy if using Cloud SQL PostgreSQL).
2. Start backend:

```bash
cd server
./mvnw spring-boot:run
```

3. Confirm no Hibernate schema validation errors.
4. Hit `GET http://localhost:8080/api/health`.

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

## Notes for future contributors

- Keep schema updates versioned in SQL files and documented in `docs/`.
- If entity models change, update bootstrap SQL and this document together.
- Avoid relying on manual memory for schema changes during team transitions.

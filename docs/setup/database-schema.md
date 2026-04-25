# Database Schema Setup

This project expects an existing MySQL schema at startup.

Important backend behavior:

- `server/src/main/resources/application.properties` sets `spring.jpa.hibernate.ddl-auto=validate`
- That means Spring validates tables/columns but does **not** auto-create schema

If schema is missing or mismatched, backend startup will fail.

## Source of truth for schema

Primary runtime schema SQL:

- `server/src/main/resources/db/v-beta.sql` (targets `V_Beta`)

Test schema SQL:

- `server/src/test/resources/db/v_beta_test_schema.sql` (targets `V_Beta_Test`)

`v-beta.sql` includes:

- core table definitions
- baseline seed data for roles/actions/grades/permissions
- sample wall/problem/user/comment records for local validation

## Recommended local setup

Use two databases:

- Main app DB: `V_Beta`
- Test DB: `V_Beta_Test`

## 1) Create databases

```sql
CREATE DATABASE IF NOT EXISTS V_Beta;
CREATE DATABASE IF NOT EXISTS V_Beta_Test;
```

## 2) Apply schema to main DB (`V_Beta`)

Run the runtime schema script directly:

Example command:

```bash
mysql -u <SQL_USERNAME> -p < server/src/main/resources/db/v-beta.sql
```

## 3) Apply schema to test DB (`V_Beta_Test`)

Run the original script as-is (it already targets `V_Beta_Test`):

```bash
mysql -u <SQL_USERNAME> -p < server/src/test/resources/db/v_beta_test_schema.sql
```

## 4) Configure backend env vars

In `server/.env`, confirm:

```bash
SQL_USERNAME=your_sql_user
SQL_PASSWORD=your_sql_password
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DB=V_Beta
MYSQL_TEST_DB=V_Beta_Test
```

## 5) Verify schema

From MySQL:

```sql
USE V_Beta;
SHOW TABLES;
SELECT COUNT(*) FROM Gym_Role;
SELECT COUNT(*) FROM Climbing_Grade;
```

Expected:

- core tables exist (for example `User_Account`, `Climbing_Problem`, `Wall_Section`)
- seed rows exist in `Gym_Role` and `Climbing_Grade`

## 6) Validate with app startup

1. Start Cloud SQL Auth Proxy (if using Cloud SQL).
2. Start backend:

```bash
cd server
./mvnw spring-boot:run
```

3. Confirm no Hibernate schema validation errors.
4. Hit `GET http://localhost:8080/api/health`.

## Common issues

- **Table not found / schema validation failed**
  - Schema not imported into `V_Beta`, or imported to wrong DB.

- **Access denied for user**
  - DB user lacks privileges to selected database.

- **Wrong DB selected**
  - `MYSQL_DB` and SQL script target DB are different.

- **Tests fail on DB schema**
  - Ensure `V_Beta_Test` exists and includes expected tables/data.

## Review notes for `v-beta.sql`

- `v-beta.sql` is valid as the primary runtime schema bootstrap for `V_Beta`.
- Runtime schema, test schema, and entity constraints are now aligned for `Solution_Beta` string lengths (`beta_name` and `video_url` both at 250).
- Compared to test schema, runtime schema intentionally has less seed data (test schema also seeds `User_Beta` and `Solution_Beta` sample rows).

## Notes for future contributors

- Keep schema updates versioned in SQL files and documented in `docs/`.
- If entity models change, update bootstrap SQL and this document together.
- Avoid relying on manual memory for schema changes during handoff.

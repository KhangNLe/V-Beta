# Test Data and Accounts

This document defines recommended test accounts, seed assumptions, and data-reset guidance for manual testing.

## Purpose

- Keep regression testing repeatable
- Avoid confusion about which role/account to use for each test
- Prevent accidental use of production-like data during local testing

## Recommended Test Accounts

Use dedicated accounts for each role:

- **Guest**
  - Not signed in
  - Used for browse-only validation

- **Climber Account**
  - Used for comment posting, beta upload, perceived grade submission

- **Setter Account**
  - Used for problem create/delete and wall reset checks

- **Admin Account**
  - Used for account role changes and wall section admin actions

## Account Inventory Template

Fill this with your team’s actual test users:

| Role | Email | Username | Firebase UID | Notes |
|---|---|---|---|---|
| Climber |  |  |  |  |
| Setter |  |  |  |  |
| Admin |  |  |  |  |

## Required Baseline Data

For manual regression, ensure:

- At least one wall section exists
- At least one active problem exists in a wall section
- At least one account per role exists (`CLIMBER`, `SETTER`, `ADMIN`)
- Grade lookup data is available (VB through V17)

## Data Reset / Rebuild Guidance

- Runtime schema seed file:
  - `server/src/main/resources/db/pg-v-beta.sql`
- Local Docker runtime DB:
  - `server/scripts/start-local-db.sh` / `.ps1` (start)
  - `server/scripts/reset-local-db.sh` / `.ps1` (wipe + re-seed `v_beta`)
- Setup reference:
  - `docs/setup/database-schema.md`
  - `docs/setup/local-development.md`

When data gets inconsistent during testing:

1. Reset the **runtime** Docker DB with `./scripts/reset-local-db.sh` (or `.ps1`), **or**
2. Recreate the **test** DB from schema SQL (`v_beta_test` via `start-local-test-db.*`)
3. Re-run affected manual test cases

## Role Transition Testing

When testing role promotion/demotion:

1. Start from known role state (document it)
2. Perform role change using an admin account **or** update SQL in Docker (below)
3. Re-login or refresh session for target user
4. Validate expected UI and backend behavior for new role

### Promote/demote via Docker (local runtime DB)

With `vbeta-postgres` running:

```powershell
docker exec -it vbeta-postgres psql -U postgres -d v_beta
```

```sql
-- CLIMBER=1, SETTER=2, ADMIN=3
SELECT u.user_id, u.username, u.email, u.firebase_uid, r.role_type
FROM user_account u
LEFT JOIN gym_role r ON r.role_id = u.gym_role_id;

UPDATE user_account
SET gym_role_id = (SELECT role_id FROM gym_role WHERE role_type = 'SETTER')
WHERE firebase_uid = 'YOUR_FIREBASE_UID';
```

Or one-liner:

```powershell
docker exec -i vbeta-postgres psql -U postgres -d v_beta -c "UPDATE user_account SET gym_role_id = 3 WHERE firebase_uid = 'YOUR_FIREBASE_UID';"
```

See also [local-development.md](../setup/local-development.md#change-a-users-role-in-docker-local-testing).

## Safety Notes

- Never test destructive actions against non-test datasets.
- Keep test credentials out of public docs/recordings.
- If using shared Firebase project, coordinate account cleanup after testing cycles.

## Related Docs

- [docs/testing/manual-test-cases.md](./manual-test-cases.md)
- [docs/testing/regression-matrix.md](./regression-matrix.md)
- [docs/setup/database-schema.md](../setup/database-schema.md)

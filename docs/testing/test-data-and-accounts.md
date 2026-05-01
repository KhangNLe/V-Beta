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
- Setup reference:
  - `docs/setup/database-schema.md`

When data gets inconsistent during testing:

1. Recreate test DB from schema SQL (recommended name: `v_beta_test`)
2. Re-seed required baseline records
3. Re-run affected manual test cases

## Role Transition Testing

When testing role promotion/demotion:

1. Start from known role state (document it)
2. Perform role change using admin account
3. Re-login or refresh session for target user
4. Validate expected UI and backend behavior for new role

## Safety Notes

- Never test destructive actions against non-test datasets.
- Keep test credentials out of public docs/recordings.
- If using shared Firebase project, coordinate account cleanup after testing cycles.

## Related Docs

- [docs/testing/manual-test-cases.md](./manual-test-cases.md)
- [docs/testing/regression-matrix.md](./regression-matrix.md)
- [docs/setup/database-schema.md](../setup/database-schema.md)

# Regression Matrix

This matrix maps major feature areas to required regression checks.

Use this together with:

- `docs/testing/manual-test-cases.md`
- `docs/testing/test-strategy.md`

## Coverage Matrix

| Feature Area | Core Flows to Recheck | Related Manual Cases | Priority |
|---|---|---|---|
| Authentication | login, signup, verify-email, reset-password, Google auth | AUTH-01, AUTH-02, AUTH-03, AUTH-04 | High |
| Session and Access Control | guest browse, protected route behavior, role-based UI visibility | NAV-01 + auth/account role checks | High |
| Account Management | profile load, delete account, role promotion/demotion | ACCOUNT-01, ACCOUNT-02, ACCOUNT-03 | High |
| Wall Section Management | list, create/delete section, invalid section navigation | WALL-01, WALL-03 | High |
| Problem Management | create/delete/reset problem flows as setter/admin-allowed | WALL-02 | High |
| Discussion Comments | post comment, delete as owner/admin, block unauthorized delete | DISC-01 | High |
| Solution Beta | signed URL request, upload, save metadata, delete beta | DISC-02 | High |
| Perceived Grade | submit perceived grade and verify reflected result | DISC-03 | Medium |
| Problem Discovery | grade-range filter, asc/desc sort, invalid range/missing wall behavior | DISC-04 | High |
| Problem Discovery UI | wall Filter dialog, most recent/easiest/hardest, clear filters | DISC-05 | High |
| API Reliability | health/meta, unauthorized/invalid token behavior | API-01, API-02 | Medium |
| Theming and UX Stability | critical page render in light/dark mode, key nav interactions | visual sanity pass during smoke | Medium |

## Change-Impact Guide

When code changes in these areas, minimum regression scope should include:

- **Auth/session files changed** (`useRequireAuth`, auth forms, accountSession):
  - Re-run all AUTH + account access checks.
- **Role/permission/backend security changed**:
  - Re-run account role changes + setter/admin/guest restricted actions.
- **Wall/problem controllers or APIs changed**:
  - Re-run WALL-01/02/03 and DISC-03.
  - Confirm `/api/home/...` paths and `PATCH` for wall reset / problem delete.
- **Discovery/filter APIs changed** (`/api/search`, `ProblemFilteringService`):
  - Re-run DISC-04 and related integration tests in `ClimbingProblemFilteringTest`.
- **Wall filter UI changed** (`wall/[wallSectionID]/page.js`, `fetchFilteredWallSectionProblems`):
  - Re-run DISC-05 and `wall-page.test.js` filter cases.
- **Discussion/beta APIs changed**:
  - Re-run DISC-01/02 fully.
- **Routing/navigation/layout changed**:
  - Re-run guest browse + account/wall/problem entry points.

## Sign-off Template

- **Build/Commit:**
- **Tester:**
- **Date:**
- **Cases executed:**
- **Pass count / Fail count:**
- **Known issues accepted for release:**

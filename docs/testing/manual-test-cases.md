# Manual Test Cases

This document defines the manual regression checklist for validating core user flows before release.

## Test Environment Preconditions

- Backend running (`server/`) and reachable at `http://localhost:8080`
- Frontend running (`v-beta/`) and reachable at `http://localhost:3000`
- Firebase and backend env variables configured
- Cloud SQL proxy and storage integrations available (if testing beta upload)
- Test accounts available for:
  - guest (not signed in)
  - climber
  - setter
  - admin

## Smoke Test Suite (Run Every Release)

### AUTH-01: Login (Email/Password)

- Steps:
  1. Open `/login`
  2. Enter valid email/password
  3. Submit
- Expected:
  - user is authenticated
  - redirected to `main-page` or `verify-email` based on account state

### AUTH-02: Signup and Verification Flow

- Steps:
  1. Open `/signup`
  2. Create account with valid values
  3. Confirm verification email flow from `/verify-email`
- Expected:
  - account session sync succeeds
  - verified user can continue to main app

### AUTH-03: Forgot Password

- Steps:
  1. Open `/forgot-password`
  2. Submit registered email
- Expected:
  - success message appears
  - reset email flow can be completed

### AUTH-04: Google Login

- Steps:
  1. Use Google sign-in from login/signup form
- Expected:
  - successful sign-in
  - session synced to backend

### NAV-01: Guest Browse

- Steps:
  1. Access `main-page` while signed out
  2. Open a wall section
  3. Open a problem page
- Expected:
  - guest banner visible
  - browse allowed
  - restricted actions blocked

### WALL-01: Admin Wall Section Management

- Steps:
  1. Sign in as admin
  2. Add a wall section
  3. Delete a wall section
- Expected:
  - add/delete succeed
  - list refreshes correctly

### WALL-02: Setter Problem Management

- Steps:
  1. Sign in as setter
  2. Open a wall section
  3. Create problem
  4. Delete problem
  5. Reset wall section
- Expected:
  - setter actions succeed
  - updated problem list reflects operations

### WALL-03: Invalid Wall Section Routing

- Steps:
  1. Navigate directly to an invalid wall section URL
- Expected:
  - error/toast shown
  - redirected back to `main-page`

### DISC-01: Comment Create/Delete (Owner and Admin)

- Steps:
  1. As climber, post a comment on a problem
  2. Delete own comment
  3. As another non-admin user, attempt deleting other user comment
  4. As admin, delete another user comment
- Expected:
  - owner delete succeeds and the comment disappears from the problem timeline
  - non-owner non-admin delete is blocked
  - admin delete succeeds and the comment disappears from the problem timeline
  - the discussion root remains (soft delete); owner reason is `"User deleted their own discussion"`, admin reason is `"Admin forced delete the discussion"`

### DISC-02: Solution Beta Upload/Delete

- Steps:
  1. Upload a valid beta video on problem page
  2. Confirm metadata appears in discussion
  3. Delete beta as owner
  4. As admin, delete another user's beta
- Expected:
  - signed URL/upload/save flow succeeds
  - beta entry is visible after refresh
  - owner/admin deletion rules are enforced
  - deleted betas disappear from the timeline; video metadata/GCS object is not immediately removed
  - owner reason is `"User deleted their own discussion"`, admin reason is `"Admin forced delete the discussion"`

### DISC-03: Suggest Perceived Grade

- Steps:
  1. Submit perceived grade on problem page
- Expected:
  - request succeeds
  - perceived grade display updates

### DISC-04: Problem Discovery by Grade Range (API)

- Steps:
  1. Call `GET /api/search/{wallSectionId}?min={lowest}&max={highest}` for a known wall
  2. Call the same range with `&sort=asc` and `&sort=desc`
  3. Retry with `min > max`
  4. Retry with a non-existent wall section id
- Expected:
  - valid ranges return only active problems within the inclusive grade bounds
  - ascending/descending responses are ordered by assigned grade
  - invalid range returns `400`
  - missing wall returns `404`
  - keyword/text search is not required for this case

### DISC-05: Wall Section Filter UI

- Steps:
  1. Open a wall section as guest or signed-in user
  2. Open **Filter**, choose min–max grades and Most Recent / Easiest / Hardest
  3. Apply and confirm the problem list and filter hint update
  4. Select min harder than max and confirm Apply is disabled/dimmed
  5. Clear filters and confirm the default list is restored
- Expected:
  - Filter is available without setter role
  - Easiest/Hardest use `/api/search` with `sort=asc|desc`
  - Most Recent uses `/api/search` without sort and orders by newest `createdDate`
  - Clear returns to the default wall problems fetch

### ACCOUNT-01: View Account Profile

- Steps:
  1. Sign in
  2. Open `/account`
- Expected:
  - email, username, role displayed correctly

### ACCOUNT-02: Account Deletion

- Steps:
  1. Trigger account deletion flow for current user
- Expected:
  - account is deleted server-side
  - user can no longer access protected routes with deleted account session

### ACCOUNT-03: Admin Role Promotion/Demotion

- Steps:
  1. As admin, promote climber to setter
  2. Validate setter capabilities
  3. Demote back to climber
- Expected:
  - role updates succeed
  - effective permissions change accordingly

## API and Reliability Checks

### API-01: Health and Meta

- Verify:
  - `GET /api/health` returns healthy response
  - `GET /api/v1/meta` returns app metadata

### API-02: Unauthorized Access

- Steps:
  1. Call protected endpoint without token
  2. Call with invalid/expired token
- Expected:
  - unauthorized responses returned
  - app handles failures gracefully

## Execution Notes

- Record tester, date, and pass/fail for each case.
- Capture screenshot/video evidence for failures.
- Log defects with:
  - test case ID
  - reproduction steps
  - expected vs actual result
  - environment details

## Suggested Result Template

- `Case ID`
- `Status` (Pass/Fail/Blocked)
- `Tester`
- `Date`
- `Notes`

# Known Issues and Limitations

This document reflects the current repository state on `package_sub` and comparison against `administrator_page`.

## Functional Limitations

### 1) Search/filter/sort for climbing problems is not implemented
- Current flows support listing sections and section-specific problems, but there is no shipped search, grade-range filter, or sorting experience.
- Impact: scalability/usability degrades as the number of problems grows.

### 2) Comment model is problem-level, not beta-level
- Discussion comments are attached to climbing problems (with user-comment linkage), not directly to a specific beta submission.
- Impact: limited threading/granularity when discussing multiple betas for the same problem.

### 3) Account page is read-only for profile fields
- Users can view account info (and delete account) but cannot self-edit profile attributes such as username/email in the app.
- Impact: profile correction/update requires future feature work or admin/back-end support.

## Authorization and API Limitations

### 4) No unified global API error envelope
- Server error responses are not fully standardized via a single global exception handler contract.
- Impact: clients must handle varying error payload shapes and message formats.

## Testing and Quality Limitations

### 5) Frontend automated test coverage is still thin
- Current frontend automated evidence is limited to a small Jest suite and does not broadly cover role-based/account/wall workflows.
- Impact: higher regression risk in UI behavior and integration points.

### 6) Backend integration tests depend on MySQL environment setup
- Backend integration test execution requires test DB configuration and environment alignment.
- Impact: onboarding and CI portability are harder than fully self-contained test setups.

## Operations and Deployment Limitations

### 7) CI workflow automation is not fully established in-repo
- No complete PR pipeline is established in the repository for automatic quality gates.
- Impact: test and verification quality relies more heavily on manual process discipline.

### 8) Generated report artifacts are local by default
- Maven/Jest generated outputs are under build directories and are not automatically retained in Git.
- Impact: evidence can be lost unless copied into `docs/testing/evidence/`.

### 9) Environment and secret configuration is file/env-path dependent
- Firebase/GCP and DB configuration depend on local environment variables and credential file paths.
- Impact: deployment reliability and security depend on strict environment management practices.

## Planned Follow-up Areas

- Standardize authorization checks for all mutating discussion/beta routes.
- Add global server error handling contract and document response schema.
- Expand frontend automated tests for core role-based and wall/problem flows.

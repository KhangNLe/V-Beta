# Git Workflow

This workflow defines standards for issues, pull requests, and reporting across the project.

## Branching Standards

- Keep `main` stable and deployable.
- Create one feature/fix branch per issue.
- Use clear branch names:
  - `feature/<short-description>`
  - `fix/<short-description>`
  - `docs/<short-description>`
  - `test/<short-description>`

Examples:

- `feature/account-role-management`
- `fix/problem-delete-authorization`
- `docs/api-endpoints`

## Issue Workflow Standard

### 1) Open or Select an Issue

Each code change should map to a tracked issue.

Issue should include:

- problem statement
- scope and non-scope
- acceptance criteria
- priority/severity

### 2) Assign and Plan

- Assign owner.
- Add labels (feature, bug, docs, testing, priority).
- Confirm dependencies/blockers.

### 3) Definition of Ready (Issue)

Before implementation starts:

- [ ] Repro/problem is clear
- [ ] Acceptance criteria are testable
- [ ] Required environments/data are known
- [ ] Owner and target milestone/sprint are set

## Pull Request Workflow Standard

### 1) Before Opening PR

- Rebase or merge latest `main` into branch as needed.
- Run relevant tests.
- Update docs if behavior, API, setup, or workflow changed.

### 2) PR Title and Scope

- Keep PR focused on one issue/theme.
- Use concise, meaningful title:
  - `feat: add account role change flow`
  - `fix: enforce owner check on comment deletion`
  - `docs: add API permissions matrix`

### 3) PR Description Template

Include:

- **Issue Link:** `Closes #<id>` or `Relates #<id>`
- **Summary:** what changed and why
- **Scope:** key files/features touched
- **Test Evidence:** commands run + manual cases checked
- **Docs Impact:** which docs were updated
- **Risks/Rollback:** known risks and fallback plan

### 4) Definition of Done (PR)

- [ ] Acceptance criteria met
- [ ] Tests pass (automated + required manual checks)
- [ ] Role/auth/permission impacts validated (if relevant)
- [ ] Docs updated (`docs/features`, `docs/api`, `docs/setup`, `docs/testing`, etc.)
- [ ] Reviewer comments addressed

## Review Standards

- At least two reviewers are required for every pull request.
- Review focus order:
  1. correctness and security
  2. authorization/role behavior
  3. data/API contract consistency
  4. maintainability/readability
  5. test coverage and documentation

## Reporting Workflow

### Bug Reporting

Use:

- `docs/testing/bug-report-template.md`

Required report elements:

- reproducible steps
- expected vs actual result
- environment details
- severity and impact
- logs/screenshots/evidence

### Progress Reporting (Issue/PR Updates)

For active work, report:

- current status (`in progress`, `blocked`, `ready for review`)
- what changed since last update
- blockers/risks
- next action

### Test Reporting

For release/demo readiness, include:

- smoke/regression cases executed
- pass/fail counts
- known issues accepted
- links to evidence and bug reports

## Merge and Post-Merge

- Merge only after approval and passing checks.
- Prefer squash merge for small/focused branches unless history detail is needed.
- After merge:
  - verify issue is closed/updated
  - remove stale branch
  - notify team of notable behavior changes

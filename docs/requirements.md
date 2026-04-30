# Requirements Specification (V-Beta)

## 1. Purpose

This document defines the project requirements for V-Beta, a web-based platform for indoor climbers to record, share, and discuss climbing beta for specific gym problems.

This document is self-contained and serves as the authoritative baseline for scope, functional requirements, non-functional requirements, and acceptance criteria.

This specification is intended to be testable, traceable, and aligned with the current repository documentation in `docs/testing/` and `docs/architecture/`.

## 2. Problem Statement and Scope

### 2.1 Problem Statement

Climbing beta is often shared informally (word-of-mouth or temporary recordings) and is rarely preserved in a searchable, structured form. V-Beta addresses this by tying technique knowledge and discussion to specific wall sections and climbing problems over time.

### 2.2 Why This Matters

- Climbers get structured access to diverse strategies.
- Gyms get stronger community engagement.
- Climbing knowledge is preserved even as walls are reset.

### 2.3 In Scope

- Account creation and authentication
- Role-based permissions (climber, setter, administrator)
- Wall section and climbing problem management
- Beta video upload and playback workflow
- Comment/discussion on problems
- Perceived grade suggestion and aggregation
- Search/filter by grade and wall section
- Lifecycle tracking for problems affected by wall resets

### 2.4 Out of Scope

- Native mobile app
- Integration with gym management systems
- Advanced social features (followers, feeds, direct messaging)
- Large-scale automated content moderation

## 3. Stakeholders and Roles

### 3.1 Stakeholders

- Primary users: recreational and competitive indoor climbers
- Secondary stakeholders: route setters and gym administrators

### 3.2 System Roles

- **Climber**: authenticated user who can browse problems, submit beta, comment, and suggest grades
- **Route Setter**: climber with permissions to create/manage climbing problems
- **Administrator**: user with permissions to manage wall sections and assign/change roles

## 4. Core Domain Concepts

- **Climbing Problem**: a route associated with a wall section and lifecycle state
- **Wall Section**: a physical wall area that can be reset
- **Beta Submission**: a user-submitted solution video with metadata
- **Lifecycle State**: status of a problem (for example, active or archived)

## 5. Functional Requirements

### 5.1 Authentication and Roles

- **FR-1**: The system shall allow users to create authenticated accounts.
- **FR-2**: The system shall associate each authenticated user with exactly one role: climber, route setter, or administrator.
- **FR-3**: The system shall restrict role assignment and role changes to administrators.

### 5.2 Climbing Problems and Wall Sections

- **FR-4**: The system shall allow route setters to create climbing problems associated with a specific wall section.
- **FR-5**: The system shall allow administrators to create wall sections.
- **FR-6**: The system shall allow administrators to delete wall sections.
- **FR-7**: The system shall maintain a lifecycle state for each climbing problem.
- **FR-8**: The system shall transition all climbing problems associated with a wall section to an archived state when that wall section is reset.

### 5.3 Beta Submissions

- **FR-9**: The system shall allow climbers to create beta submissions only for active climbing problems.
- **FR-10**: The system shall allow multiple beta submissions for a single climbing problem.
- **FR-11**: The system shall prevent creation or modification of beta submissions for archived climbing problems.
- **FR-12**: The system shall allow beta submissions to include video content.
- **FR-13**: The system shall allow a climber to delete beta submissions that they originally created.
- **FR-14**: The system shall allow administrators to delete any beta submission regardless of creator.
- **FR-15**: The system shall prevent users from deleting beta submissions created by other users unless they hold the administrator role.

### 5.4 Comments and Community Interaction

- **FR-16**: The system shall allow climbers to post comments on beta submissions associated with active climbing problems.
- **FR-17**: The system shall prevent commenting on beta submissions associated with archived climbing problems.
- **FR-18**: The system shall allow a climber to delete comments that they originally created.
- **FR-19**: The system shall allow administrators to delete any comment regardless of creator.
- **FR-20**: The system shall prevent users from deleting comments created by other users unless they hold the administrator role.

### 5.5 Grading and Discovery

- **FR-21**: The system shall allow climbers to submit perceived grade suggestions for climbing problems.
- **FR-22**: The system shall display the aggregated perceived grade alongside the assigned difficulty grade for each climbing problem.
- **FR-23**: The system shall allow signed-out users (guests) to browse wall sections and problem details while restricting authenticated actions.

## 6. Non-Functional Requirements

### 6.1 Performance

- **NFR-1**: Under normal operating conditions, newly submitted beta content shall become visible to authenticated users within 30 seconds of submission.
- **NFR-2**: Newly persisted comments shall appear upon subsequent page reloads without manual synchronization steps.

### 6.2 Availability and Reliability

- **NFR-3**: The system shall provide at least 99% availability for authenticated users, excluding scheduled maintenance.

### 6.3 Security and Authorization

- **NFR-4**: The system shall enforce role-based access control for all operations that modify climbing problems, beta submissions, wall sections, or user roles.

### 6.4 Data Integrity

- **NFR-5**: Archived climbing problems shall remain immutable with respect to beta submissions and comments.
- **NFR-6**: Deletion of beta submissions or comments shall remove the content from user-visible views immediately after successful operation.

## 7. Constraints, Assumptions, and Risks

### 7.1 Constraints

- Time and team capacity constraints can affect scope and implementation depth.
- Cloud services and credentials (Firebase, database, storage) must be available for full workflow validation.

### 7.2 Assumptions

- Users have internet connectivity and modern browsers.
- Test accounts exist for climber, setter, and administrator roles.
- Backend and frontend environment variables are configured per setup documentation.

### 7.3 Risks

- Video upload/playback complexity and storage behavior
- Data-model complexity around wall resets and lifecycle state transitions
- Frontend/backend integration drift
- Uneven workload distribution or delayed collaboration between team members

## 8. Acceptance Criteria (Project Success)

### 8.1 Functional Success

- Users can sign up/login and submit beta videos to problems.
- Multiple beta submissions can exist for the same problem.
- Setters/admins can manage wall sections/problems per role permissions.
- Users can comment and suggest perceived grades.

### 8.2 Technical Success

- Role-based authorization is consistently enforced.
- Wall resets correctly archive associated problems.
- Video upload and retrieval workflows are reliable in supported environments.

### 8.3 Usability Success

- Primary flows (browse, problem detail, submit beta/comment, account actions) are understandable and navigable without special training.

## 9. Requirements Traceability

Requirement verification evidence is tracked through:

- [`docs/testing/test-strategy.md`](./testing/test-strategy.md)
- [`docs/testing/README.md`](./testing/README.md)
- [`docs/testing/manual-test-cases.md`](./testing/manual-test-cases.md)
- [`docs/testing/regression-matrix.md`](./testing/regression-matrix.md)
- [`docs/testing/release-readiness-checklist.md`](./testing/release-readiness-checklist.md)


| Requirement Group                           | Primary Verification Source            | Dated Evidence Artifact                                                                                                 | Last Updated |
| ------------------------------------------- | -------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ------------ |
| FR-1 to FR-3 (auth/roles)                   | AUTH-01..04, ACCOUNT-03, API-02       | [`docs/testing/regression-matrix.md`](./testing/regression-matrix.md), [`docs/testing/manual-test-cases.md`](./testing/manual-test-cases.md) | 2026-04-25   |
| FR-4 to FR-8 (walls/problems/lifecycle)     | WALL-01..03                            | [`docs/testing/regression-matrix.md`](./testing/regression-matrix.md), [`docs/testing/release-readiness-checklist.md`](./testing/release-readiness-checklist.md) | 2026-04-25   |
| FR-9 to FR-15 (beta workflow/authorization) | DISC-02, API-02                        | [`docs/testing/regression-matrix.md`](./testing/regression-matrix.md), [`docs/testing/server-test-report.md`](./testing/server-test-report.md) | 2026-04-25   |
| FR-16 to FR-20 (comments/authorization)     | DISC-01, API-02                        | [`docs/testing/regression-matrix.md`](./testing/regression-matrix.md), [`docs/testing/server-test-report.md`](./testing/server-test-report.md) | 2026-04-25   |
| FR-21 to FR-23 (grades/discovery)           | DISC-03, NAV-01                        | [`docs/testing/regression-matrix.md`](./testing/regression-matrix.md), [`docs/testing/frontend-test-report.md`](./testing/frontend-test-report.md) | 2026-04-25   |
| FR-24 (guest browsing)                      | NAV-01                                 | [`docs/testing/manual-test-cases.md`](./testing/manual-test-cases.md), [`docs/testing/frontend-test-report.md`](./testing/frontend-test-report.md) | 2026-04-25   |
| NFR-1 to NFR-6                              | Smoke/regression + release readiness checks | [`docs/testing/server-test-report.md`](./testing/server-test-report.md), [`docs/testing/frontend-test-report.md`](./testing/frontend-test-report.md), [`docs/testing/evidence/backend/surefire-report-2026-04-25.html`](./testing/evidence/backend/surefire-report-2026-04-25.html) | 2026-04-25   |


## 10. Open Items

- Keep adding release-by-release validation evidence (dated run logs, pass/fail totals, defects) for future iterations.
- Keep this document synchronized with implemented endpoints and role-policy changes.

## 11. Related Documentation

This section provides supporting context documents outside the direct requirement-to-test traceability defined in Section 9.

### 11.1 Documentation Index

- [`docs/README.md`](./README.md)

### 11.2 Setup and Environment

- [`docs/setup/local-development.md`](./setup/local-development.md)
- [`docs/setup/environment-variables.md`](./setup/environment-variables.md)
- [`docs/setup/firebase-setup.md`](./setup/firebase-setup.md)
- [`docs/setup/google-cloud-setup.md`](./setup/google-cloud-setup.md)
- [`docs/setup/database-schema.md`](./setup/database-schema.md)

### 11.3 Architecture and Design

- [`docs/architecture/system-overview.md`](./architecture/system-overview.md)
- [`docs/architecture/frontend-architecture.md`](./architecture/frontend-architecture.md)
- [`docs/architecture/backend-architecture.md`](./architecture/backend-architecture.md)
- [`docs/architecture/data-model.md`](./architecture/data-model.md)
- [`docs/architecture/design-rationale.md`](./architecture/design-rationale.md)

### 11.4 Features and Product Behavior

- [`docs/features/authentication-and-roles.md`](./features/authentication-and-roles.md)
- [`docs/features/wall-and-problems.md`](./features/wall-and-problems.md)
- [`docs/features/account-page.md`](./features/account-page.md)
- [`docs/features/future-features.md`](./features/future-features.md)

### 11.5 API Reference

- [`docs/api/endpoints.md`](./api/endpoints.md)
- [`docs/api/request-response-examples.md`](./api/request-response-examples.md)
- [`docs/api/permissions-matrix.md`](./api/permissions-matrix.md)
- [`docs/api/error-handling.md`](./api/error-handling.md)

### 11.6 Process and Maintenance

- [`docs/contributing/coding-standards.md`](./contributing/coding-standards.md)
- [`docs/contributing/git-workflow.md`](./contributing/git-workflow.md)
- [`docs/known-issues-and-limitations.md`](./known-issues-and-limitations.md)

### 11.7 References

- [`docs/references.md`](./references.md)


# Future and Potential Features

This document tracks ideas that are not fully implemented in the current release.

## How to Use This File

- Keep implemented features in their own feature docs.
- Add only future/potential work here.
- Mark status clearly so readers do not confuse planned work with shipped work.

## Priority Legend

- **High**: strong product or technical impact, low-to-medium effort
- **Medium**: useful improvement, moderate effort
- **Low**: nice-to-have, exploratory, or higher effort

## Candidate Features

### 1) Stronger Authorization Consistency

- **Priority:** Medium
- **Area:** Security/Permissions
- **Current Gap:** Authorization style differs across some endpoints/flows.
- **Potential Work:** Standardize permission checks for discussion and beta actions.
- **Dependencies:** Role/action matrix agreement.

### 2) Wall and Problem Quality-of-Life Features

- **Priority:** Medium
- **Area:** Wall/Problems
- **Current Gap:** Basic CRUD is present, but limited filtering/sorting and workflow support.
- **Potential Work:** Add filtering, search, and bulk/problem lifecycle utilities.
- **Dependencies:** API additions and UI controls.

### 3) Better Content Moderation and Auditability

- **Priority:** Medium
- **Area:** Discussion/Beta
- **Current Gap:** Deletion and ownership rules exist but limited moderation workflow/audit history.
- **Potential Work:** Add moderation queue, reason codes, and event logs.
- **Dependencies:** Backend audit model and admin UI.

### 4) Observability and Operational Hardening

- **Priority:** Low
- **Area:** Platform
- **Current Gap:** Minimal operational metadata surfaced in-app.
- **Potential Work:** Add structured logs, lightweight admin diagnostics, and usage dashboards.
- **Dependencies:** Logging and monitoring stack choices.

### 5) User Profile Pictures in Community Content

- **Priority:** Medium
- **Area:** Account/Discussion UI
- **Current Gap:** User identity in comments and solution beta sections is text-only.
- **Potential Work:** Add profile picture upload/display so each user avatar appears in comments and solution beta entries.
- **Dependencies:** User profile image storage strategy, account schema updates, frontend rendering updates.

### 6) Report Inappropriate Comments and Solution Betas

- **Priority:** High
- **Area:** Trust and Safety
- **Current Gap:** Users cannot flag inappropriate comments or beta content in the current UI.
- **Potential Work:** Add report actions on comments and solution betas, including reason selection and moderation review workflow.
- **Dependencies:** Report data model, report API endpoints, moderation/admin handling flow.

### 7) Images for Wall Sections and Climbing Problems

- **Priority:** Medium
- **Area:** Wall/Problem Experience
- **Current Gap:** Wall sections and climbing problems are currently displayed without dedicated images.
- **Potential Work:** Add image support for each wall section and each climbing problem (upload, storage, display).
- **Dependencies:** Media storage integration, schema/API extensions, frontend image components and fallbacks.

### 8) Search, Grade-Range Filtering, and Sorting for Problems

- **Priority:** High
- **Area:** Wall/Problem Discovery
- **Current Gap:** Users have limited options to quickly find wall sections/problems by search criteria and grade range.
- **Potential Work:** Add search for wall sections and climbing problems, grade-range filters (minimum/maximum grade), and sort controls for ascending or descending problem order.
- **Dependencies:** Query/filter API support, frontend filter UI state, consistent grade ordering logic across backend/frontend.

### 9) Multi-Gym Support with Per-Gym Roles

- **Priority:** Low
- **Area:** Multi-Tenant / Access Control
- **Current Gap:** Current data model and permissions assume a single gym context.
- **Potential Work:** Expand the platform to support multiple climbing gyms, allow users to search/select gyms, and assign roles per gym (for example, user is ADMIN at one gym and CLIMBER at another).
- **Dependencies:** Gym-level tenancy model, user-gym-role mapping, scoped authorization checks, and gym-aware frontend navigation/filtering.

### 10) Wall Section Reset Notifications

- **Priority:** Medium
- **Area:** User Communication / Activity Awareness
- **Current Gap:** Users are not proactively notified when a wall section is reset.
- **Potential Work:** Add a notification system so users are informed when specific wall sections are reset, including in-app notifications and optional email/push channels.
- **Dependencies:** Reset event tracking, notification preference model, delivery mechanism, and frontend notification UI.

### 11) Account Activity History and Quick Navigation

- **Priority:** Medium
- **Area:** Account / User Experience
- **Current Gap:** Users cannot view their past comments and solution betas from a centralized account view.
- **Potential Work:** Add an account section where users can see their past comments and solution beta submissions, with direct navigation back to the related currently active problems.
- **Dependencies:** User activity query endpoints, account page UI expansion, active/archived problem status handling, and deep-link routing.

### 12) Admin Reports Log and Developer Event Log

- **Priority:** High
- **Area:** Moderation / Reliability
- **Current Gap:** There is no dedicated admin report log for flagged comments/solution betas and no centralized event/error log for developer debugging.
- **Potential Work:** Add an admin-facing reports queue/history for inappropriate content, plus a structured application event log (errors and important events) for developers to diagnose unexpected bugs.
- **Dependencies:** Report/audit data model, admin review UI, event ingestion pipeline, and log retention/access policy.

## Change Log

- Keep this section updated when priorities change or items move to implemented docs.

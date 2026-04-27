# Final Handoff

This document is the final project handoff snapshot for Team Satisfaction (ICS-499 capstone).

## Release Snapshot

- **Repository:** `ics499-50-capstone-team-satisfaction`
- **Handoff branch:** `package_sub`
- **Reference commit:** `e427971`
- **Frontend app:** `v-beta/` (Next.js)
- **Backend API:** `server/` (Spring Boot)

## Delivered Scope

- Authentication and role-based access foundations
- Wall section and climbing problem management flows
- Discussion comments and beta submission workflows
- Perceived grade submission workflow
- Account page with account-info retrieval and account deletion flow
- Documentation package for setup, architecture, API, testing, and process

## Key Handoff Artifacts

- Documentation index: [`docs/README.md`](./README.md)
- User manual: [`docs/user-manual.md`](./user-manual.md)
- Requirements: [`docs/requirements.md`](./requirements.md)
- Design rationale mapping: [`docs/architecture/design-rationale.md`](./architecture/design-rationale.md)
- Architecture diagrams index: [`docs/architecture/diagrams/README.md`](./architecture/diagrams/README.md)
- API docs: [`docs/api/endpoints.md`](./api/endpoints.md)
- Known issues and limitations: [`docs/known-issues-and-limitations.md`](./known-issues-and-limitations.md)

## Test Evidence

- Testing index: [`docs/testing/README.md`](./testing/README.md)
- Backend test report: [`docs/testing/server-test-report.md`](./testing/server-test-report.md)
- Frontend test report: [`docs/testing/frontend-test-report.md`](./testing/frontend-test-report.md)
- Evidence index: [`docs/testing/evidence/README.md`](./testing/evidence/README.md)
- Committed backend HTML evidence:
  - [`docs/testing/evidence/backend/surefire-report-2026-04-25.html`](./testing/evidence/backend/surefire-report-2026-04-25.html)
- Committed frontend screenshot evidence:
  - `docs/testing/evidence/frontend/npm-test-summary.png`

## Results, Discussion, and Conclusion

### What Worked Well

- Core end-to-end workflows are functional for authentication, wall/problem browsing, comments, beta submission, and account actions.
- Backend automated testing executed successfully with passing summary evidence.
- Documentation coverage was expanded across setup, architecture, API, testing, and handoff artifacts.

### What Did Not Fully Work / Remaining Gaps

- Frontend automated coverage is limited to Jest unit testing and doesn't cover end-to-end testing.
- Some architecture and API consistency improvements remain in backlog (see known issues and future features docs).
- Operational hardening items (standardized error envelope, deeper observability, full CI gating) are still follow-up work.
- System integration testing is still narrow in multiple places

### Conclusion

The project meets core capstone functional goals with a runnable full-stack implementation and documented verification evidence. Remaining work is primarily quality-hardening and scope expansion rather than foundational functionality.

## SDLC Phase Summary

### Planning and Requirements

- Problem scope, role model, and requirement groups documented in [`docs/requirements.md`](./requirements.md).
- Weekly coordination and risk tracking captured in [`docs/standups/README.md`](./standups/README.md).

### Design

- System and subsystem architecture documented in `docs/architecture/`.
- UML and design artifacts organized in [`docs/architecture/diagrams/README.md`](./architecture/diagrams/README.md).
- Design rationale and code pattern mapping documented in [`docs/architecture/design-rationale.md`](./architecture/design-rationale.md).

### Implementation

- Frontend and backend features were implemented iteratively using branch/PR workflow standards in [`docs/contributing/git-workflow.md`](./contributing/git-workflow.md).

### Testing and Verification

- Test strategy, environments, and regression artifacts are documented in [`docs/testing/README.md`](./testing/README.md).
- Dated backend/frontend evidence is provided in test report files and evidence artifacts.

### Deployment and Handoff

- Environment setup and runtime configuration are documented in `docs/setup/`.
- Final package and reviewer path are consolidated in this handoff document.

## Lessons Learned and Next Iteration Actions

### Lessons Learned

- Commitment to effective early planning helped during implementation.
- Misunderstandings or things not confronted early on ended up cropping up later on in the design lifecycle.
- Good diagrams and documentation made it easier to resolve discrepancies in understanding or clear up confusion and uncertainty.
- There wasn't always a correct decision, but one that had to be decided and justified nonetheless.
- Requirements determined from the problem tie directly into verification and validation that what has been created is what actually solves the problem.
- Chunk goals and feature phases to GitHub issue tickets helped us keep on track to provide a way to centralize work reporting and progress
- Explored different toolings and documentations for the correct use cases outside of traditional classroom learnings
- Followed Sprint workflow protocols towards our development and planning phase helped us understand how each work is delegated on taken on for each group member
- Provided solution possibilities to critical bugs or system implementations during standup meetings or in team chat helped others get unblocked on their issues and which helped with improving efficiency instead of someone stuck and not asking for help last minute

### Next Iteration Actions

- Expand frontend automated tests to include end-to-end coverage and coverage of every page.
- Standardize backend error response shape and improve API consistency.
- Add stronger automation around CI quality gates and release verification.
- Refactor/cleanup files and file structure
- Refactor code structure as some features are all in 1 file and instead should be decoupled
- Add in error page and routing handling for when a user navigates to a unknown url
- Exploratory work on improving and adding in features that would expand beyond documented features
- Deploy app to production
- Integrating Observability tooling to view errors, clicks, logs, metrics, etc when app is deployed
- Improve on dev env, add in stage, and prod
- Iterate on testing in dev env.
- Find alternative to cloudSQL
- Firebase allows for different sign in's, can add those options to our app (e.g Sign in with Apple)
- Provide official links in footer section on landing page
- Improve frontend design on few areas in the app
- Finish out leftover GitHub issues

## Outstanding Limitations

For accepted limitations and follow-up priorities, see:

- [`docs/known-issues-and-limitations.md`](./known-issues-and-limitations.md)
- [`docs/features/future-features.md`](./features/future-features.md)

## Recommended Reviewer Path

1. Read [`docs/requirements.md`](./requirements.md)
2. Review architecture in [`docs/architecture/system-overview.md`](./architecture/system-overview.md)
3. Review API and permissions docs in `docs/api/`
4. Review test reports and evidence in `docs/testing/`
5. Review known limitations for scope context

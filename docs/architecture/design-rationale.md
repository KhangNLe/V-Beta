# Design Rationale and Mapping

This document links core design decisions to concrete project artifacts for rubric traceability.

## 1) Process Model Used

**Model:** Iterative and incremental team development with issue-driven branches and PR-based integration.

**Why this model fit the project:**
- The project evolved feature-by-feature (auth, wall/problem, discussion, account/admin) with regular validation.
- Weekly standups tracked accomplishments, plans, and blockers, which supported short feedback cycles.

**Evidence links:**
- Workflow standard: [`docs/contributing/git-workflow.md`](../contributing/git-workflow.md)
- Iteration cadence and weekly status: [`docs/standups/README.md`](../standups/README.md)
- Release/handoff snapshot: [`docs/final-handoff.md`](../final-handoff.md)

## 2) UML Artifacts Used

The project includes the following UML/design diagrams:

- Domain class diagram
- Use case diagram
- Use case scenarios diagram
- Robustness diagram
- Sequence diagram
- Class diagram
- Database diagram

**Artifact index:**
- [`docs/architecture/diagrams/README.md`](./diagrams/README.md)

## 3) Design Patterns Used and Code Mapping

| Pattern / Approach | Where Used | Code Evidence |
| --- | --- | --- |
| Layered Architecture | Backend request flow from controllers to services, managers, repositories, domain entities | [`docs/architecture/backend-architecture.md`](./backend-architecture.md), `server/src/main/java/edu/ics499/VBeta/controller/`, `server/src/main/java/edu/ics499/VBeta/application/`, `server/src/main/java/edu/ics499/VBeta/application/support/`, `server/src/main/java/edu/ics499/VBeta/repository/`, `server/src/main/java/edu/ics499/VBeta/domain/model/` |
| Repository Pattern | Persistence abstraction through Spring Data JPA repositories | `server/src/main/java/edu/ics499/VBeta/repository/UserAccountRepository.java` (and other repository interfaces) |
| Service Layer / Application Facade | Use-case orchestration in application services, delegated to managers | `server/src/main/java/edu/ics499/VBeta/application/ClimbingWallService.java`, `server/src/main/java/edu/ics499/VBeta/application/ProblemDiscussionService.java`, `server/src/main/java/edu/ics499/VBeta/application/AccountService.java` |
| Ports and Adapters | Storage operations abstracted via interface and implemented with GCP adapter | `server/src/main/java/edu/ics499/VBeta/application/support/VideoStoragePort.java`, `server/src/main/java/edu/ics499/VBeta/application/support/GcpFileStorageAdapter.java` |
| Policy/Authorization Service | Role/action authorization centralized in service + role-permission manager | `server/src/main/java/edu/ics499/VBeta/application/AuthorizationService.java`, `server/src/main/java/edu/ics499/VBeta/application/support/RoleBasedAuthenticationManager.java`, `server/src/main/java/edu/ics499/VBeta/domain/model/ActionDefinition.java` |
| Security Filter Chain | Cross-cutting authentication in servlet filter before endpoint handling | `server/src/main/java/edu/ics499/VBeta/config/security/FirebaseAuthFilter.java`, `server/src/main/java/edu/ics499/VBeta/config/SecurityConfig.java` |

## 4) Notes

- This mapping is intentionally concise for scoring traceability.
- Detailed behavior and constraints are documented in:
  - [`docs/architecture/system-overview.md`](./system-overview.md)
  - [`docs/architecture/backend-architecture.md`](./backend-architecture.md)
  - [`docs/known-issues-and-limitations.md`](../known-issues-and-limitations.md)

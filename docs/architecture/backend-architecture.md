# Backend Architecture

## Stack

- Spring Boot
- Spring Web + Spring Security
- Spring Data JPA (Hibernate)
- PostgreSQL
- Firebase Admin SDK
- Google Cloud Storage client integration

## Layered Design

The backend follows a layered architecture under `server/src/main/java/app/VBeta/`:

1. **Controllers** (`controller/`)
   - Define REST endpoints and request mapping.
2. **Application Services** (`application/`)
   - Coordinate use cases and transaction boundaries.
3. **Support Managers/Adapters** (`application/support/`)
   - Domain-specific orchestration and integration helpers, grouped by concern:
     - `account/`, `discussion/` (`beta/`, `comment/`), `grade/`, `problem/`, `wall/`, `report/`, `events/`, `moderation/`
4. **Repositories** (`repository/`)
   - Data access through Spring Data JPA.
5. **Domain Model** (`domain/model/`)
   - Entities, enums, and role/action concepts, grouped by domain:
     - `actions/` — roles, actions, permissions (`ActionDefinition`, `GymRole`, `RolePermission`, …)
     - `climb/` — walls, problems, grades, lifecycle
     - `discussions/` — discussion roots, comments, solution betas
     - `user/` — accounts and perceived grades
     - `report/`, `notification/`, `appeal/`, `moderation/` — Sprint 5 moderation model
6. **API DTOs** (`api/dto/`)
   - Request/response contracts, grouped by feature area:
     - `account/`, `walls/`, `problems/`, `discussions/` (`comment/`, `video/`), `report/`, `notification/`, `moderation/`

Place new types in the matching domain subpackage rather than the layer root.

## Request Lifecycle

1. Request arrives at Spring Security chain.
2. `FirebaseAuthFilter` validates bearer token when present.
3. Controller endpoint executes and delegates to service layer.
4. `AuthorizationService` enforces role/action permissions where required.
5. Services/managers read/write entities via repositories.
6. Response DTOs are returned to the client.

## Security Architecture

- Token auth: Firebase ID tokens.
- URL-level security: configured in `SecurityConfig`.
- Action-level authorization: `AuthorizationService` + `ActionDefinition` + role-permission mappings from DB.

Access model types:

- public endpoints
- authenticated endpoints
- action-gated endpoints

## Persistence and Domain

- Main entities include user accounts, roles, wall sections, climbing problems, discussion comments, solution betas, perceived grades, reports, events, and notifications.
- Discussion comment/beta user deletes are **soft deletes** on `DiscussionRoot` (`deleted_at`, `deleted_by`, `deleted_reason`). Hard `removeDiscussion` remains for cascading problem/account cleanup.
- JPA schema mode in runtime is `ddl-auto=validate`, so schema must exist and match.
- Role permission evaluation is data-driven from role/action tables.

## External Integrations

- **Firebase Admin**
  - Validates client tokens and provides authenticated principal UID.
- **Google Cloud Storage**
  - Generates signed upload URLs and manages video object operations.
- **PostgreSQL**
  - Stores relational domain data for all core features.

## Configuration and Profiles

- Main runtime configuration: `application.properties` + `application.yml`
- Test profile: `application-test.yml`
- Optional `.env` import for local machine overrides

## Cross-Cutting Concerns

- **Security filter chain**
  - Firebase token verification runs in `FirebaseAuthFilter`.
- **CORS**
  - Configured via `WebConfig` for `/api/**` (GET, POST, PUT, PATCH, DELETE, OPTIONS).
- **Transactions**
  - Service-layer transaction boundaries coordinate multi-step operations.
- **Error handling**
  - Controllers catch `RuntimeException` and return a mapped status with a plain-text message (typically 404; wall writes 400; notification GET 401).
  - `FirebaseAuthFilter` returns JSON `401` for invalid tokens.
  - `POST /api/accounts/session` still uses `ResponseStatusException` for missing auth.

## Request/Permission Patterns

- Endpoint access types:
  - public
  - authenticated
  - action-gated (`ActionDefinition` checks)
- Role permissions are loaded from DB mappings (`RolePermission`) through authorization support services.
- Some discussion endpoints are authenticated without full action-gating; enforceable behavior is partly service-rule based.
- Content report **create** and notification inbox (`GET`/`PATCH /api/notification/short`) are authenticated only (no `CREATE_REPORT` action).
- Admin report **queue/detail** (`GET /api/report/reports`) is action-gated with `VIEW_REPORTS`.
- Admin report **resolve** (`POST /api/moderate/report`) is action-gated with `MODERATE_REPORT`.
- Admin **logbook** (`GET /api/moderate/logbook`) is action-gated with `VIEW_MODERATION_LOGS`.

## Constraints and Technical Debt Notes

- Error payloads are not fully standardized across all failure paths.
- Role-permission cache behavior and refresh strategy should be considered when permission models evolve.

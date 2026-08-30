# Coding Standards

This document defines baseline coding standards for contributors working in:

- frontend: `v-beta/` (Next.js + React)
- backend: `server/` (Spring Boot + Java)

## Core Principles

- Prefer readability over cleverness.
- Keep behavior explicit; avoid hidden side effects.
- Make small, focused changes.
- Keep frontend and backend contracts in sync.
- Document non-obvious decisions in code comments or docs.

## General Standards

- Use descriptive names for variables, functions, classes, and files.
- Avoid dead code and commented-out legacy code.
- Keep functions/methods single-purpose and short where practical.
- Handle error paths deliberately (not just happy path).
- Do not hardcode secrets, credentials, or environment-specific values.
- Avoid broad refactors in feature PRs unless explicitly scoped.

## SRP (Single Responsibility Principle)

Each unit should have one primary reason to change.

- **Functions/methods:** do one clear job. Split validation, transformation, persistence, and presentation concerns when they start to mix.
- **Frontend components:** separate rendering concerns from side-effect/data-fetching concerns when complexity grows.
- **Backend classes:** keep controllers focused on HTTP concerns, services focused on use-case orchestration, and repositories focused on persistence.
- **Refactor trigger:** if one file is repeatedly changed by unrelated tickets, split responsibilities.

## SOLID Principles

Apply SOLID as practical guidance, not rigid ceremony.

- **S — Single Responsibility Principle**
  - Keep each unit focused on one concern (see SRP above).

- **O — Open/Closed Principle**
  - Prefer extending behavior via new modules/classes/helpers rather than rewriting stable core paths.
  - Example: add new endpoint/service methods or UI helpers without breaking existing flows.

- **L — Liskov Substitution Principle**
  - If one implementation replaces another, behavior contracts should remain valid.
  - Example: storage adapters or service abstractions should preserve expected inputs/outputs and error behavior.

- **I — Interface Segregation Principle**
  - Avoid forcing consumers to depend on methods they do not use.
  - Keep APIs and abstractions focused and minimal for each caller context.

- **D — Dependency Inversion Principle**
  - Depend on abstractions where appropriate, not concrete details.
  - Backend example: use service/port abstractions (e.g., storage port/adapters) for integration-heavy logic.
  - Frontend example: keep page components dependent on domain API modules rather than low-level request wiring spread everywhere.

## Applicable Design Patterns

Use the patterns below where they naturally fit existing architecture.

- **Layered Architecture**
  - Controller -> Service/Application -> Support/Manager -> Repository.
  - Keep responsibilities separated by layer.

- **Repository Pattern**
  - Encapsulate persistence in Spring Data repositories.
  - Avoid embedding query/persistence logic in controllers.

- **DTO Pattern**
  - Use request/response DTOs for API contracts.
  - Do not expose internal entity structure directly to API clients.

- **Adapter/Port Pattern**
  - Keep external integrations behind abstraction boundaries.
  - Example in backend: storage port + cloud storage adapter.

- **Filter Pattern (Cross-Cutting Security)**
  - Centralize request pre-processing concerns (for example auth token validation) in filters rather than duplicating checks in every controller.

- **Strategy-Like Authorization Rules**
  - Treat role/action checks as centralized rule evaluation rather than inline condition sprawl.
  - Prefer centralized authorization services over duplicated permission logic.

- **Composition over Inheritance**
  - Favor small collaborating services/components/helpers instead of deep inheritance hierarchies.

When adding a new feature, prefer extending existing project patterns over introducing one-off structures.

## Frontend Standards (`v-beta/`)

### Structure and Patterns

- Keep route-level logic in `src/app/*` pages and shared logic in `src/lib` or `src/hooks`.
- Keep API calls in `src/api/*` modules (avoid scattering direct `fetch` calls across components when possible).
- Reuse existing UI primitives from `src/components/ui/*`.
- Keep side effects (network/storage/toasts) separated from pure rendering logic when possible.

### React and UI

- Use clear state names (`loading`, `error`, `data` patterns).
- Avoid deeply nested component logic; extract helpers when needed.
- Keep role/permission UI gating consistent with backend authorization behavior.
- Preserve accessibility basics (`aria-label`, semantic controls, keyboard-friendly actions).
- Ensure loading, empty, success, and error states are all handled for data-driven screens.

### Styling

- Follow existing Tailwind/shadcn style patterns.
- Prefer shared theme tokens and existing utility conventions over one-off custom styles.
- Keep light/dark mode compatibility in mind for new UI.

## Backend Standards (`server/`)

### Layering

- Keep controllers thin: request mapping + validation + delegation.
- Put use-case logic in services/application layer.
- Keep repository usage in service/support layers (avoid unnecessary direct controller-to-repo calls).

### Package Organization

Place new types in the matching domain subpackage under `app.VBeta` (do not leave them in the layer root):

- **DTOs** (`api/dto/`): `account/`, `walls/`, `problems/`, `discussions/` (`comment/`, `video/`), `report/`, `notification/`, `moderation/`
- **Domain** (`domain/model/`): `actions/`, `climb/`, `discussions/`, `user/`, `report/`, `notification/`, `appeal/`, `moderation/`
- **Support** (`application/support/`): `account/`, `discussion/` (`beta/`, `comment/`), `grade/`, `problem/`, `wall/`, `report/`, `events/`, `moderation/`

See [`docs/architecture/backend-architecture.md`](../architecture/backend-architecture.md) for the full layout.

### API and DTOs

- Use DTOs for request/response contracts.
- Keep endpoint semantics predictable (avoid non-standard method behavior for new endpoints).
- Keep new HTTP routes under `/api` and match existing controller method choices (`PATCH` for wall reset and problem delete; `GET` with JSON body for signed upload URL).
- Return clear, intentional status codes.
- Keep endpoint docs updated in `docs/api/` when contracts change.

### Authorization and Security

- Enforce sensitive actions through existing authorization patterns (`AuthorizationService`, `ActionDefinition`).
- Never rely on frontend checks as the only protection.
- Validate ownership/admin rules for mutation endpoints.
- Favor centralized error/permission handling over ad-hoc per-endpoint logic.

### Persistence and Domain

- Keep entity, SQL schema, and docs synchronized when model changes.
- Use transactional boundaries in service layer for multi-step data changes.

## Testing Expectations

- For backend changes:
  - add/update tests when behavior changes
  - run `./mvnw test`
- For frontend changes:
  - run `npm test` where relevant
  - verify key UI flows manually when automated coverage is missing
- For role/auth/API changes:
  - run high-priority regression cases from `docs/testing/manual-test-cases.md`

Refer to:

- `docs/testing/test-strategy.md`
- `docs/testing/manual-test-cases.md`

## Documentation Expectations

- Update relevant docs for behavior/API/schema/workflow changes.
- Keep docs concise and implementation-accurate.
- If a feature is planned but not implemented, track it in `docs/features/future-features.md` (not in implemented-feature docs).

## Pull Request Quality Checklist

- [ ] Change scope is focused and understandable.
- [ ] Error and edge cases are handled.
- [ ] Authorization behavior is correct.
- [ ] Tests pass and relevant cases were executed.
- [ ] Docs were updated where required.
- [ ] No secrets or environment-specific values were committed.

# Data Model

## Overview

The backend data model is relational and centered on climbing content, user roles, and discussion artifacts.

Primary persistence is PostgreSQL via JPA entities in `server/src/main/java/edu/ics499/VBeta/domain/model/`.

## Core Entity Groups

### Identity and Access

- `UserAccount`
  - Core account record, keyed by Firebase UID.
- `GymRole`
  - Role assignment (`CLIMBER`, `SETTER`, `ADMIN`).
- `GymAction`
  - Action definitions mapped to permissions.
- `RolePermission`
  - Join between role and allowed actions.

### Climbing Content

- `WallSection`
  - Logical wall area/group.
- `ClimbingProblem`
  - Problem metadata, assigned grade, lifecycle status, wall association.
- `ClimbingGrade`
  - Grade lookup values (VB..V17 model).

### User Interaction Content

- `UserComment`
  - User-to-problem comment anchor.
- `DiscussionComment`
  - Text discussion payload tied to a user comment record.
- `UserBeta`
  - User-to-problem beta relation.
- `SolutionBeta`
  - Uploaded beta metadata (object file/video URL/time).
- `UserPerceiveGrade`
  - User-submitted perceived grade per problem.

## Relationship Highlights

- One `GymRole` can belong to many `UserAccount` records.
- One `WallSection` has many `ClimbingProblem` records.
- One `ClimbingProblem` can have many comments and perceived grades.
- `SolutionBeta` is linked to `UserBeta` for user/problem beta ownership.
- Role permissions are evaluated through role-action mappings (`RolePermission`).
- `UserPerceiveGrade` is keyed per user/problem pair to model one effective grade submission per user per problem.

## Authorization Data Model

Action-level authorization uses:

- `ActionDefinition` (enum in code)
- role type from `UserAccount -> GymRole`
- permission lookup from `RolePermission`

This model enables action-gated endpoint checks beyond simple authenticated/unauthenticated access.

## Schema Management Notes

- Runtime expects an existing schema (`ddl-auto=validate`).
- Runtime schema SQL reference:
  - `server/src/main/resources/db/pg-v-beta.sql` (runtime baseline)

See setup instructions in:

- `docs/setup/database-schema.md`

## Data Model Evolution Considerations

- Keep entity changes synchronized with SQL schema files and API docs.
- Revalidate role-permission mappings whenever introducing new `ActionDefinition` values.
- For future multi-gym support, add gym-scoped ownership for users/roles/wall content.
- For moderation/reporting features, add explicit entities for report lifecycle and reviewer actions.
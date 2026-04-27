# Team Satisfaction (ICS-499 Capstone)

Full-stack capstone project for managing climbing gym walls, problems, discussion, and role-based administration.

## Overview

The system includes:

- A Next.js frontend in `v-beta/`
- A Spring Boot backend in `server/`
- Firebase Authentication integration
- MySQL persistence
- Google Cloud Storage for beta video uploads

Core roles:

- Guest (browse public content)
- Climber (authenticated user actions)
- Setter (wall/problem management actions)
- Admin (account/role management actions)

## Repository Structure

- `v-beta/` - frontend app (Next.js, React, Tailwind/shadcn)
- `server/` - backend API (Spring Boot, Spring Security, JPA)
- `docs/` - project documentation (setup, architecture, API, testing, contributing)

## Quick Start (Local)

1. Read setup docs in [`docs/README.md`](./docs/README.md)
2. Configure backend env vars in `server/.env`
3. Configure frontend env vars in `v-beta/.env.local`
4. Start backend from `server/`:
   - `./mvnw spring-boot:run`
5. Start frontend from `v-beta/`:
   - `npm install`
   - `npm run dev`
6. Open `http://localhost:3000`

## Documentation

- [`docs/README.md`](./docs/README.md) - complete documentation index
- [`docs/user-manual.md`](./docs/user-manual.md) - role-based user guide
- [`docs/final-handoff.md`](./docs/final-handoff.md) - final deliverable handoff summary
- [`server/README.md`](./server/README.md) - backend setup and runtime configuration
- [`v-beta/README.md`](./v-beta/README.md) - frontend setup and runtime configuration
- [`docs/architecture/diagrams/README.md`](./docs/architecture/diagrams/README.md) - architecture diagrams index
- [`docs/testing/README.md`](./docs/testing/README.md) - testing documentation and evidence index

## Current Status

This repository is actively used for capstone development and documentation handoff. Some implementation and documentation limitations are tracked in:

- [`docs/known-issues-and-limitations.md`](./docs/known-issues-and-limitations.md)

## Contribution Workflow

Team workflow standards and review expectations are documented in:

- [`docs/contributing/git-workflow.md`](./docs/contributing/git-workflow.md)

# System Overview

## Purpose

This project is a full-stack climbing gym application with:

- a Next.js frontend in `v-beta/`
- a Spring Boot backend in `server/`
- Firebase Authentication for identity
- PostgreSQL for persistence
- Google Cloud Storage for solution beta video uploads

## High-Level Architecture

1. User interacts with frontend pages in `v-beta/src/app/*`.
2. Frontend authenticates user through Firebase Auth.
3. Frontend syncs app account session with backend (`POST /api/accounts/session`).
4. Frontend sends Firebase ID token to protected backend APIs under `/api/**`.
5. Backend validates token, resolves account/role, and authorizes actions.
6. Backend reads/writes domain data in PostgreSQL.
7. For beta videos, backend generates signed upload URLs for Cloud Storage and stores video metadata.

## Main Subsystems

- **Frontend App (`v-beta/`)**
  - App Router pages, role-aware navigation, guest/authorized UX.
- **Backend API (`server/`)**
  - REST controllers under `/api` for account, wall/problem, search, discussion, reports, notifications, and health/meta.
- **Authentication and Authorization**
  - Firebase token verification + role/action checks.
- **Storage and Database**
  - PostgreSQL for application entities.
  - GCS for video file objects.

## Core Functional Areas

- Authentication and email verification
- Account profile + account role administration
- Wall section and climbing problem management
- Problem discussion (comments + solution beta uploads)
- Perceived grade suggestions
- Content reports, admin queue, logbook, in-app notifications, and appeals

## Runtime and Environments

- Local default backend URL: `http://localhost:8080`
- Local default frontend URL: `http://localhost:3000`
- Backend uses environment-driven config (`server/.env` + Spring properties)
- Frontend uses `v-beta/.env.local`

## Architectural Notes

- The system uses a layered backend architecture (controller -> application/service -> support managers -> repositories).
- Frontend state is mostly local component state with Firebase auth listener hooks.
- Role-based behavior is enforced in both frontend UX gating and backend authorization checks.
- Public read APIs support guest browsing for wall/problem discovery.

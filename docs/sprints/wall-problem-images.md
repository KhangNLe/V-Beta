# Wall and Problem Images

## Feature Overview

Sprint 6 delivers image support for wall sections and climbing problems. Admins can upload wall section photos; setters can upload climbing problem photos. All users, including guests, can view thumbnails and click to expand a larger image.

This document is the **source of truth** for the v1 data/storage contract and Sprint 6 implementation plan. API endpoint names may be refined during implementation; rules marked **MUST** are normative.

**Status:** In progress (Sprint 6)  
**Roadmap:** [`docs/implementation-roadmap.md`](../implementation-roadmap.md)  
**Parent epic:** Wall section and climbing problem images

## Sprint 6 Implementation Plan

Estimated duration: 2 weeks

### Phase 1 — Contract and schema (this branch)

| Task | Owner | Status |
|------|-------|--------|
| Document storage contract (this file) | Docs | In progress |
| Add nullable paired image columns to bootstrap SQL | Backend | In progress |
| Update `database-schema.md` and `data-model.md` | Docs | In progress |
| Agree API response shape (`imageUrl`) | Backend + Frontend | Planned |

### Phase 2 — Backend APIs and permissions

| Task | Owner | Status |
|------|-------|--------|
| Add `UPLOAD_WALL_SECTION_IMAGE` and `UPLOAD_PROBLEM_IMAGE` actions | Backend | Planned |
| Signed upload URL endpoints (reuse GCS adapter pattern) | Backend | Planned |
| Confirm/save metadata after client PUT | Backend | Planned |
| Return `imageUrl` in wall/problem read DTOs and `/api/search` | Backend | Planned |
| Update API docs and permissions matrix | Docs | Planned |

### Phase 3 — Frontend upload and display

| Task | Owner | Status |
|------|-------|--------|
| Shared `imageUpload.js` helper (signed PUT flow) | Frontend | Planned |
| Admin wall section upload/replace UI | Frontend | Planned |
| Setter problem upload/replace UI | Frontend | Planned |
| Thumbnails + click-to-expand lightbox on main/wall/problem pages | Frontend | Planned |
| Placeholder when `imageUrl` is null | Frontend | Planned |

### Phase 4 — Quality and release

| Task | Owner | Status |
|------|-------|--------|
| Backend integration tests (auth, validation, DTO shape) | Backend | Planned |
| Frontend tests (role gating, lightbox, upload flow) | Frontend | Planned |
| Manual test cases in `docs/testing/manual-test-cases.md` | QA/Docs | Planned |
| Update `docs/features/wall-and-problems.md` when shipped | Docs | Planned |

### Sprint 6 acceptance criteria

- [ ] Schema and contract documented; bootstrap SQL aligned in runtime + test schemas
- [ ] Admin can upload/replace a wall section image via authenticated UI
- [ ] Setter can upload/replace a problem image via authenticated UI
- [ ] Unauthorized roles cannot upload via API
- [ ] Wall/problem read APIs return `imageUrl: string | null`
- [ ] UI renders thumbnails and supports click-to-view full image
- [ ] Missing images show placeholder without layout break
- [ ] Upload validation enforces allowed MIME types and max file size
- [ ] Core auth and happy-path tests merged
- [ ] API and feature docs updated

### Explicitly out of scope (Sprint 6 / v1)

- Multiple images per wall section or problem (gallery)
- In-app cropping or editing
- Climber-uploaded problem photos
- Automatic image moderation
- Dedicated delete-image endpoint (replace-only in v1)
- Immediate GCS purge on problem delete (deferred; same policy as solution-beta objects)
- `image_content_type` / `image_uploaded_at` columns (deferred to a later sprint if needed)

## Data and Storage Contract (v1)

### Design alignment

Follow the existing solution-beta GCS pattern:

- Signed PUT upload via `GcpFileStorageAdapter`
- Persist both storage key and public URL (like `Solution_Beta.beta_name` + `Solution_Beta.video_url`)
- Client uploads directly to GCS, then confirms metadata with the API

Reference: `SolutionBetaManager.createSignedUrl()`, `CloudFileStorageResponse`

### Database columns

#### `Wall_Section`

| Column | Type | Nullable | Rule |
|--------|------|----------|------|
| `wall_image_url` | `VARCHAR(250)` | Yes | Public GCS URL for display |
| `image_object_name` | `VARCHAR(250)` | Yes | GCS object key |

#### `Climbing_Problem`

| Column | Type | Nullable | Rule |
|--------|------|----------|------|
| `problem_image_url` | `VARCHAR(250)` | Yes | Public GCS URL for display |
| `image_object_name` | `VARCHAR(250)` | Yes | GCS object key |

### Integrity rules

- **R1:** `wall_image_url` and `image_object_name` MUST both be `NULL` or both be non-`NULL` (`chk_wall_img_obj`).
- **R2:** `problem_image_url` and `image_object_name` MUST both be `NULL` or both be non-`NULL` (`chk_img_obj`).
- **R3:** At most one active image per wall section / problem (enforced by columns on the entity row).
- **R4:** `image_object_name` is the canonical storage identifier; `*_image_url` is the client-facing display URL.

### Object key convention

Wall section:

```text
walls/{wallSectionId}/section-image.{ext}
```

Problem:

```text
walls/{wallSectionId}/problems/{problemId}/problem-image.{ext}
```

Key rules:

- **R5:** Extension MUST match validated MIME type.
- **R6:** Keys MUST be deterministic per entity in v1 (replace overwrites the same key).
- **R7:** Keys MUST NOT include user-supplied path segments.
- **R8:** Allowed extensions: `.jpg`, `.jpeg`, `.png`, `.webp`.

### Upload flow

1. Client requests signed upload URL (authenticated, role-gated).
2. Backend generates deterministic `image_object_name` and public URL.
3. Backend returns `signedURL`, `method` (`PUT`), `objectName`, and `publicUrl` (or `imageUrl`).
4. Client uploads bytes directly to GCS.
5. Client calls confirm/save endpoint.
6. Backend persists `image_object_name` + `*_image_url`.

### Read strategy

- **R9:** Read APIs MUST return `imageUrl: string | null`.
- **R10:** Read APIs MUST NOT return `image_object_name` to normal clients.
- **R11:** URLs are public GCS URLs (same bucket strategy as beta videos), not signed read URLs in v1.
- **R12:** If no image exists, APIs return `imageUrl: null`; UI shows a neutral placeholder.

### Validation rules

| Rule | Value |
|------|-------|
| Allowed MIME types | `image/jpeg`, `image/png`, `image/webp` |
| Max file size | 8 MB |
| Reject on mismatch | Content-Type, extension, and magic bytes MUST agree |

### API response shape (agreed contract)

Wall section list/detail:

```json
{
  "wallSectionId": 3,
  "wallSectionName": "Cave",
  "info": "Steep overhang",
  "imageUrl": "https://storage.googleapis.com/<bucket>/walls/3/section-image.webp"
}
```

Problem list/detail/search:

```json
{
  "problemId": 22,
  "holdColor": "RED",
  "info": "Crimp line",
  "assignedGrade": "V5",
  "createdDate": "2026-08-31T12:00:00",
  "imageUrl": "https://storage.googleapis.com/<bucket>/walls/3/problems/22/problem-image.jpg"
}
```

- **R13:** Field name is `imageUrl` everywhere (camelCase in JSON).
- **R14:** Missing image => `imageUrl: null` (field present, not omitted).

### Lifecycle rules

**Upload / replace**

- **R15:** Uploading a new image replaces DB metadata.
- **R16:** Previous GCS object SHOULD be deleted on successful replace.
- **R17:** If DB save fails after upload, orphaned object cleanup is best-effort (known limitation).

**Problem archive (`lifecycle_status = ARCHIVE`)**

- **R18:** Archived problems KEEP image metadata.
- **R19:** New image upload MUST be rejected for archived problems.

**Problem delete**

- **R20:** Problem delete clears `problem_image_url` and `image_object_name`.
- **R21:** GCS object deletion on problem delete is deferred in v1 (same deferred-purge policy as solution-beta objects).

**Wall section delete**

- **R22:** Wall section delete follows existing wall-delete rules; image metadata is removed with the row when delete succeeds.

### Permissions (contract)

| Action | Role |
|--------|------|
| Upload/replace wall image | Admin |
| Upload/replace problem image | Setter, Admin |
| View image | Guest, all roles |

Planned actions: `UPLOAD_WALL_SECTION_IMAGE`, `UPLOAD_PROBLEM_IMAGE` (see `docs/api/permissions-matrix.md` when endpoints ship).

### Error contract

| Case | HTTP | Message intent |
|------|------|----------------|
| Unsupported MIME | 400 | Invalid image type |
| File too large | 400 | Image exceeds 8 MB |
| Unauthorized upload | 403 | Not permitted |
| Missing wall/problem | 404 | Target not found |
| Upload to archived problem | 409 | Problem is archived |

## User Flows (target — post-implementation)

### View images (guest or signed-in)

1. User opens main page, wall section page, or problem page.
2. Thumbnail renders when `imageUrl` is present.
3. User clicks thumbnail to open full-size image in a lightbox/modal.
4. When `imageUrl` is null, UI shows placeholder only.

### Admin: wall section photo

1. Admin opens main page or wall section page.
2. Admin chooses Upload/Replace photo.
3. Client requests signed URL, PUTs file to GCS, confirms metadata.
4. Thumbnail updates after save.

### Setter: problem photo

1. Setter opens problem detail or create/manage flow.
2. Setter chooses Upload/Replace photo.
3. Same signed-upload flow as wall images.
4. Image appears on problem list and detail after save.

## Key Files

### Schema and entities

- `server/src/main/resources/db/pg-v-beta.sql`
- `server/src/test/resources/db/v_beta_test_schema.sql`
- `server/src/main/java/app/VBeta/domain/model/climb/WallSection.java`
- `server/src/main/java/app/VBeta/domain/model/climb/ClimbingProblem.java`

### Storage (existing pattern)

- `server/src/main/java/app/VBeta/application/support/discussion/beta/GcpFileStorageAdapter.java`
- `server/src/main/java/app/VBeta/application/support/discussion/beta/VideoStoragePort.java`

### Frontend (planned)

- `v-beta/src/app/main-page/page.js`
- `v-beta/src/app/wall/[wallSectionID]/page.js`
- `v-beta/src/app/wall/[wallSectionID]/problem/[problemId]/page.js`
- `v-beta/src/api/wallSections.js`
- `v-beta/src/lib/imageUpload.js` (new)

## Related documentation

- Roadmap: [`docs/implementation-roadmap.md`](../implementation-roadmap.md)
- Schema setup: [`docs/setup/database-schema.md`](../setup/database-schema.md)
- Data model: [`docs/architecture/data-model.md`](../architecture/data-model.md)
- Shipped wall flows (update when feature ships): [`docs/features/wall-and-problems.md`](./wall-and-problems.md)

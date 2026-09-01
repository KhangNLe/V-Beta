# Wall and Problem Images

## Feature Overview

Sprint 6 adds backend support for wall section photos, climbing problem photos, and (partially) user profile avatars. Images use the same Google Cloud Storage signed-PUT flow as solution-beta videos: the client requests a signed URL, uploads directly to GCS, then saves metadata through the API.

**Status:** Backend APIs shipped; frontend display/upload UI and `imageUrl` on wall/problem read DTOs are still in progress.

Sprint contract and remaining work: [`docs/sprints/wall-problem-images.md`](../sprints/wall-problem-images.md)

## Implemented (Backend)

- Nullable paired image columns on `Wall_Section` and `Climbing_Problem`
- `UPLOAD_WALL_IMAGE` (admin) and `UPLOAD_PROBLEM_IMAGE` (setter) permissions
- Signed upload URL generation for wall, problem, and profile targets
- Metadata save after client upload
- Wall section and problem image deletion (GCS object + DB metadata)
- MVC and integration tests (`SocialMediaControllerTest`, `ImageServiceTest`)

## API Endpoints

Base path: `/api/social`

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/image/signed-url` | Mint signed GCS PUT URL |
| `PATCH` | `/image/upload` | Persist object key + public URL after upload |
| `DELETE` | `/image/problem?climbingProblemId=` | Remove problem image |
| `DELETE` | `/image/wall?wallSectionId=` | Remove wall section image |

Details: [`docs/api/endpoints.md`](../api/endpoints.md), examples in [`docs/api/request-response-examples.md`](../api/request-response-examples.md).

## Upload Flow

1. Authenticated client calls `GET /api/social/image/signed-url` with query params (`@ModelAttribute`).
2. Backend validates role/target and returns `signedURL`, `method`, `uploadObjectName`, `publicURL`.
3. Client `PUT`s the file bytes to GCS using the signed URL.
4. Client calls `PATCH /api/social/image/upload` with `targetType`, `objectFileName`, `imageUrl`, and the matching entity id.
5. Backend persists `image_object_name` + `*_image_url` on the target row.

## Permissions

| Target | Action | Roles |
|--------|--------|-------|
| Wall section | `UPLOAD_WALL_IMAGE` | Admin |
| Climbing problem | `UPLOAD_PROBLEM_IMAGE` | Setter |
| User profile | Caller must match `userId` | Any authenticated user (persistence not yet complete) |

Guests cannot upload. Authorization failures are thrown as `RuntimeException` and mapped by the controller (typically **404** for signed-url, **400** for upload).

## Storage Layout

Object keys are generated server-side under the `image/` prefix:

```text
image/wallSection-{wallSectionId}/{uuid}-{sanitizedBase}.{ext}
image/problem-{problemId}/{uuid}-{sanitizedBase}.{ext}
image/userProfile-{userId}/{uuid}-{sanitizedBase}.{ext}
```

Allowed extensions: `.jpg`, `.jpeg`, `.png`, `.webp`. Requested `contentType` must match the extension.

## Data Model

| Table | URL column | Object key column |
|-------|------------|-----------------|
| `Wall_Section` | `wall_image_url` | `image_object_name` |
| `Climbing_Problem` | `problem_image_url` | `image_object_name` |

Both columns are nullable but must be set together (CHECK constraints `chk_wall_img_obj`, `chk_img_obj`).

## Key Files

- Controller: `server/src/main/java/app/VBeta/controller/SocialMediaController.java`
- Service: `server/src/main/java/app/VBeta/application/ImageService.java`
- Storage: `server/src/main/java/app/VBeta/application/support/cloud/CloudStorageManager.java`
- DTOs: `server/src/main/java/app/VBeta/api/dto/image/`
- Entities: `WallSection.java`, `ClimbingProblem.java`
- Tests:
  - `server/src/test/java/app/VBeta/mvc/SocialMediaControllerTest.java`
  - `server/src/test/java/app/VBeta/Integration_Test/ImageServiceTest.java`

## Limitations and Notes

- Wall/problem **read** endpoints do not yet expose `imageUrl` on list/detail DTOs (planned).
- Profile image signed-url/save is wired, but `UserAccountManager.updateUserProfile` does not persist avatar columns yet.
- Problem image deletion clears `problem_image_url` but may leave `image_object_name` set until a follow-up fix.
- Replace-on-reupload does not automatically delete the previous GCS object.
- Max file size enforcement is a client/contract target (8 MB); server validates MIME/extension only today.

## Future Enhancements

- Frontend thumbnails, lightbox, and role-gated upload UI
- `imageUrl` on wall/problem read and search responses
- Complete user profile image persistence
- Deferred GCS purge policy alignment with solution-beta objects

Tracked in [`docs/sprints/wall-problem-images.md`](../sprints/wall-problem-images.md) and [`docs/features/future-features.md`](./future-features.md).

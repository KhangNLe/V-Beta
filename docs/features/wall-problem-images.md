# Wall and Problem Images

## Feature Overview

Sprint 6 adds wall section photos and climbing problem photos. Images use the same Google Cloud Storage signed-PUT flow as solution-beta videos: the client requests a signed URL, uploads directly to GCS, then saves metadata through the API. User profile images are not part of this sprint.

**Status:** Complete. Admins manage wall photos (default `/co-op.png`). Setters manage problem photos (default `/problem-holder.jpg`). Choosing a photo prepares a local preview. The bucket upload starts on **Save changes**, **Add section**, or **Add problem**, and that save deletes the previous GCS object when the new key differs. Any viewer can expand a photo on the problem page only. Read DTOs include nullable `imageURL`.

Sprint contract: [`docs/sprints/wall-problem-images.md`](../sprints/wall-problem-images.md)

## Implemented (Frontend)

- Admin **Edit wall** on the main page and wall section page (name, description, upload/replace/remove)
- Setter **Edit problem** on the wall section page and problem page (hold color, grade, notes, upload/replace/remove)
- Optional photo on **Add Wall Section** and **Add Problem**
- File pick previews locally; GCS upload runs on save or add submit
- HEIC/HEIF conversion shows **Uploading iPhone photo…**
- Default `/co-op.png` for wall sections and `/problem-holder.jpg` for problems when `imageURL` is null
- Problem page click-to-expand photo (main page and wall section thumbnails do not expand)
- Component tests in `main-page.test.js`, `wall-page.test.js`, and `problem-page.test.js`

## Implemented (Backend)

- Nullable paired image columns on `Wall_Section` and `Climbing_Problem`
- `UPLOAD_WALL_IMAGE` (admin) and `UPLOAD_PROBLEM_IMAGE` (setter) permissions
- Signed upload URL generation for wall and problem targets (the profile target exists on the API and is unused by this sprint)
- Metadata save after client upload; a different previous object key is deleted
- Wall section and problem image deletion (GCS object + both image columns)
- 8 MB size check when metadata is saved
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
| User profile | Caller must match `userId` | API accepts the target; persistence and UI are Sprint 10, not Sprint 6 |

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

- Click-to-expand is only on the climbing problem page.
- User profile image upload and display are not part of Sprint 6. The signed-url API still lists `USER_ACCOUNT`; `UserAccountManager.updateUserProfile` does not persist avatar columns.
- If the GCS `PUT` succeeds and the metadata save fails, the new object can remain in the bucket while the previous photo stays.
- **Remove photo** in the UI uses the wall or problem update endpoint with both image fields null. `DELETE /api/social/image/wall` and `DELETE /api/social/image/problem` also clear a photo and its object.

## Future Enhancements

- User profile image persistence and display (roadmap Sprint 10)

Tracked in [`docs/features/future-features.md`](./future-features.md).

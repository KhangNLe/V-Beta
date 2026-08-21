# Permissions Matrix

This matrix summarizes who can call which endpoints based on current backend behavior.

Legend:

- `Public` = no bearer token required
- `Authenticated` = valid Firebase bearer token required
- `Action-gated` = authenticated + role/action permission check

All routes below are under `/api`.

## Endpoint Access by Role

| Endpoint | Guest | Climber | Setter | Admin | Access Rule |
|---|---|---|---|---|---|
| `GET /api/health` | Yes | Yes | Yes | Yes | Public |
| `GET /api/v1/meta` | Yes | Yes | Yes | Yes | Public |
| `GET /api/home/wall-sections` | Yes | Yes | Yes | Yes | Public |
| `GET /api/home/wall-sections/{wallSectionId}/problems` | Yes | Yes | Yes | Yes | Public |
| `GET /api/home/wall-sections/{wallSectionId}/problems/{problemId}` | Yes | Yes | Yes | Yes | Public |
| `GET /api/search/{wallSectionId}?min=&max=` | Yes | Yes | Yes | Yes | Public |
| `POST /api/accounts/session` | No | Yes | Yes | Yes | Authenticated |
| `GET /api/account` | No | Yes | Yes | Yes | Authenticated |
| `DELETE /api/account/deletion` | No | Yes | Yes | Yes | Authenticated |
| `GET /api/accounts` | No | No | No | Yes | Action-gated (`VIEW_ACCOUNTS`) |
| `PATCH /api/accounts/{userId}/role` | No | No | No | Yes | Action-gated (`CHANGE_ROLE`) |
| `POST /api/home/wall-section/creation` | No | No | No | Yes | Action-gated (`CREATE_WALL`) |
| `DELETE /api/home/wall-section/{wallSectionId}/delete` | No | No | No | Yes | Action-gated (`DELETE_WALL`) |
| `PATCH /api/home/wall-section/{wallSectionId}/reset` | No | No | Yes | Yes* | Action-gated (`RESET_WALL`) |
| `POST /api/home/wall-sections/{wallSectionId}/problems/create` | No | No | Yes | Yes* | Action-gated (`CREATE_PROBLEM`) |
| `PATCH /api/home/wall-sections/{wallSectionId}/problems/{problemId}/delete` | No | No | Yes | Yes* | Action-gated (`DELETE_PROBLEM`) |
| `POST /api/discussion/add-comments` | No | Yes | Yes | Yes | Authenticated |
| `GET /api/discussion/solution-beta/upload-url` | No | Yes | Yes | Yes | Authenticated |
| `POST /api/discussion/solution-beta/save` | No | Yes | Yes | Yes | Authenticated |
| `POST /api/discussion/problems/{problemId}/suggest-grade` | No | Yes | Yes | Yes | Action-gated (`GRADE_PROBLEM`) |
| `DELETE /api/discussion/comment/delete` | No | Depends | Depends | Yes | Action-gated (`DELETE_COMMENT`) + owner/admin **soft-delete** |
| `DELETE /api/discussion/solution-beta` | No | Depends | Depends | Yes | Authenticated + owner/admin **soft-delete** |
| `POST /api/report/create` | No | Yes | Yes | Yes | Authenticated (not action-gated; no `CREATE_REPORT`) |
| `GET /api/report/reports` | No | No | No | Yes | Action-gated (`VIEW_REPORTS`) |
| `GET /api/report/reports?reportId=` | No | No | No | Yes | Action-gated (`VIEW_REPORTS`) |
| `POST /api/moderate/report` | No | No | No | Yes | Action-gated (`MODERATE_REPORT`) |
| `GET /api/moderate/logbook` | No | No | No | Yes | Action-gated (`VIEW_MODERATION_LOGS`) |
| `GET /api/moderate/logbook?moderationId=` | No | No | No | Yes | Action-gated (`VIEW_MODERATION_LOGS`) |
| `GET /api/notification/short` | No | Yes | Yes | Yes | Authenticated (not action-gated). Own unread rows only |
| `PATCH /api/notification/short?notificationId=` | No | Yes | Yes | Yes | Authenticated (not action-gated). Own row only |

`Yes*` means backend action permission allows it; specific frontend UI exposure may differ by current role-based UI gating.

## Action Definitions Used by Backend

- `CREATE_BETA`
- `DELETE_BETA`
- `CREATE_COMMENT`
- `DELETE_COMMENT`
- `CREATE_PROBLEM`
- `DELETE_PROBLEM`
- `RESET_WALL`
- `CREATE_WALL`
- `DELETE_WALL`
- `CHANGE_ROLE`
- `VIEW_ACCOUNTS`
- `GRADE_PROBLEM`
- `VIEW_REPORTS`
- `MODERATE_REPORT`
- `VIEW_MODERATION_LOGS`

## Notes

- Final permission results are role-permission table driven in the database.
- Some discussion endpoints are authenticated but not action-gated at controller level.
- `POST /api/report/create`, `GET /api/notification/short`, and `PATCH /api/notification/short` are authenticated only. Guest `401` comes from Spring Security. There is no `CREATE_REPORT` action. Queue/detail uses `VIEW_REPORTS` (admin). Resolve uses `MODERATE_REPORT` (admin). Logbook uses `VIEW_MODERATION_LOGS` (admin). Inbox mark-read is own-row only (another user's id is **404**).
- Create-report notifies **admins** of `REPORT_CREATED`. Climber/setter callers still get `200`; they do not receive that inbox event. If the reporter is an admin, they are skipped as a recipient.
- `GET /api/notification/short` returns the caller's unread rows with `notificationId`, catalog `summary`, and `click` metadata. Current moderation events map to `click.kind = REPORT_QUEUE` and `click.reportId`. Report reason and admin notes are omitted.
- `GET /api/report/reports` returns grouped OPEN cases ranked by `Σ (weight × count)`. Climber/setter and missing `VIEW_REPORTS` currently map to **404**. An admin does not see reports on their own discussion (or a user-account report targeting themselves); that is a `200` with an empty `reports` list, not 404.
- `POST /api/moderate/report` requires `MODERATE_REPORT`. Climber/setter and missing permission currently map to **404**. The acting admin cannot close a report they filed, and cannot close reports on their own discussion; those ids are skipped (the request can still `200`). Appeal decisions (`APPEAL_APPROVED` / `APPEAL_DENIED`) are rejected before any report is closed.
- `GET /api/moderate/logbook` requires `VIEW_MODERATION_LOGS`. Climber/setter and missing permission currently map to **404**. Empty pages are **200** with `"moderationLogs": []`. Unknown `moderationId` is **404**. `offSetPlace <= 0` is **400**.
- Queue-resolve notifications: dismiss writes `REPORT_DISMISSED` to each reporter (owner is not notified). Remove writes `REPORT_APPROVED` to each reporter and `CONTENT_REMOVED` once to the owner. Event actor is the admin.
- Action-gated `RuntimeException` failures are currently mapped by controllers to **404** (most reads, including report resolve) or **400** (wall/problem writes), not 403.
- Problem delete is `PATCH /api/home/wall-sections/{wallSectionId}/problems/{problemId}/delete`. Wall reset is `PATCH /api/home/wall-section/{wallSectionId}/reset`. Upload URL is `GET /api/discussion/solution-beta/upload-url` with a JSON body.
- Comment/beta `DELETE` endpoints **soft-delete** `Discussion_Root` (they do not remove comment/beta child rows or GCS objects). Comment requests require `deletedReason`; beta requests require `deleteReason`.

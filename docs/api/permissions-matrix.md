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
| `DELETE /api/discussion/comment/delete` | No | Depends | Depends | Yes | Action-gated (`DELETE_COMMENT`) + owner/admin service check |
| `DELETE /api/discussion/solution-beta` | No | Depends | Depends | Yes | Authenticated + owner/admin service check |
| `POST /api/report/create` | No | Yes | Yes | Yes | Authenticated (not action-gated; no `CREATE_REPORT`) |
| `GET /api/report/reports` | No | No | No | Yes | Action-gated (`VIEW_REPORTS`) |
| `GET /api/report/reports?reportId=` | No | No | No | Yes | Action-gated (`VIEW_REPORTS`) |
| `GET /api/notification/short` | No | Yes | Yes | Yes | Authenticated (not action-gated). `REPORT_CREATED` inbox rows are written for admins only |

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

## Notes

- Final permission results are role-permission table driven in the database.
- Some discussion endpoints are authenticated but not action-gated at controller level.
- `POST /api/report/create` and `GET /api/notification/short` are authenticated only. Guest `401` comes from Spring Security. There is no `CREATE_REPORT` action. Queue/detail uses `VIEW_REPORTS` (admin). There is no `RESOLVE_REPORT` action yet.
- Create-report notifies **admins** of `REPORT_CREATED`. Climber/setter callers still get `200`; they do not receive that inbox event. If the reporter is an admin, they are skipped as a recipient.
- `GET /api/report/reports` returns grouped OPEN cases ranked by `Σ (weight × count)`. Climber/setter and missing `VIEW_REPORTS` currently map to **404**. An admin does not see reports on their own discussion (or a user-account report targeting themselves); that is a `200` with an empty `reports` list, not 404.
- Action-gated `RuntimeException` failures are currently mapped by controllers to **404** (most reads) or **400** (wall/problem writes), not 403.
- Problem delete is `PATCH /api/home/wall-sections/{wallSectionId}/problems/{problemId}/delete`. Wall reset is `PATCH /api/home/wall-section/{wallSectionId}/reset`. Upload URL is `GET /api/discussion/solution-beta/upload-url` with a JSON body.

# Permissions Matrix

This matrix summarizes who can call which endpoints based on current backend behavior.

Legend:

- `Public` = no bearer token required
- `Authenticated` = valid Firebase bearer token required
- `Action-gated` = authenticated + role/action permission check

## Endpoint Access by Role

| Endpoint | Guest | Climber | Setter | Admin | Access Rule |
|---|---|---|---|---|---|
| `GET /api/health` | Yes | Yes | Yes | Yes | Public |
| `GET /api/v1/meta` | Yes | Yes | Yes | Yes | Public |
| `GET /home/wall-sections` | Yes | Yes | Yes | Yes | Public |
| `GET /home/wall-sections/{wallSectionId}/problems` | Yes | Yes | Yes | Yes | Public |
| `GET /home/wall-sections/{wallSectionId}/problems/{problemId}` | Yes | Yes | Yes | Yes | Public |
| `GET /search/{wallSectionId}?min=&max=` | Yes | Yes | Yes | Yes | Public |
| `POST /api/accounts/session` | No | Yes | Yes | Yes | Authenticated |
| `GET /api/account` | No | Yes | Yes | Yes | Authenticated |
| `DELETE /api/account/deletion` | No | Yes | Yes | Yes | Authenticated |
| `GET /api/accounts` | No | No | No | Yes | Action-gated (`VIEW_ACCOUNTS`) |
| `PATCH /api/accounts/{userId}/role` | No | No | No | Yes | Action-gated (`CHANGE_ROLE`) |
| `POST /home/wall-section/creation` | No | No | No | Yes | Action-gated (`CREATE_WALL`) |
| `DELETE /home/wall-section/{wallSectionId}/delete` | No | No | No | Yes | Action-gated (`DELETE_WALL`) |
| `POST /home/wall-section/{wallSectionId}/reset` | No | No | Yes | Yes* | Action-gated (`RESET_WALL`) |
| `POST /home/wall-sections/{wallSectionId}/problems/create` | No | No | Yes | Yes* | Action-gated (`CREATE_PROBLEM`) |
| `GET /home/wall-sections/{wallSectionId}/problems/{problemId}/delete` | No | No | Yes | Yes* | Action-gated (`DELETE_PROBLEM`) |
| `POST /discussion/add-comments` | No | Yes | Yes | Yes | Authenticated |
| `POST /discussion/solution-beta/upload-url` | No | Yes | Yes | Yes | Authenticated |
| `POST /discussion/solution-beta/save` | No | Yes | Yes | Yes | Authenticated |
| `POST /discussion/problems/{problemId}/suggest-grade` | No | Yes | Yes | Yes | Action-gated (`GRADE_PROBLEM`) |
| `DELETE /discussion/comment/delete` | No | Depends | Depends | Yes | Action-gated (`DELETE_COMMENT`) + owner/admin service check |
| `DELETE /discussion/solution-beta` | No | Depends | Depends | Yes | Authenticated + owner/admin service check |
| `POST /api/report/create` | No | Yes | Yes | Yes | Authenticated (not action-gated; no `CREATE_REPORT`) |
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

## Notes

- Final permission results are role-permission table driven in the database.
- Some discussion endpoints are authenticated but not action-gated at controller level.
- `POST /api/report/create` and `GET /api/notification/short` are authenticated only. Guest `401` comes from Spring Security. There is no `CREATE_REPORT`, `VIEW_REPORTS`, or `RESOLVE_REPORT` action in `ActionDefinition`.
- Create-report notifies **admins** of `REPORT_CREATED`. Climber/setter callers still get `201` / `200`; they do not receive that inbox event. If the reporter is an admin, they are skipped as a recipient.
- `GET /home/wall-sections/{wallSectionId}/problems/{problemId}/delete` is a destructive legacy GET route.

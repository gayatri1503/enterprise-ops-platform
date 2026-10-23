# API Contract (source of truth)

All external calls go through the gateway. Change this file in the same PR as the code.

## Standard error body

```json
{ "timestamp": "2026-01-01T10:00:00Z", "requestId": "uuid", "status": 403,
  "code": "FORBIDDEN", "message": "Insufficient permissions",
  "validationErrors": [ { "field": "title", "message": "must not be blank" } ] }
```

Never return stack traces or SQL. `X-Correlation-Id` is created at the gateway and passed on and logged by every service.

## JWT access token (issued by auth-service)

Short-lived (target 10-15 min). Claims: `sub` (userId), `roles` (e.g. `["MANAGER"]`), `perms` (e.g. `["WORK_EDIT","ANALYTICS_VIEW"]`), `deptId`, `jti`, `iat`, `exp`.
Refresh token: opaque, high-entropy, HttpOnly Secure cookie, rotated on every use.

## Roles and permissions

Roles: `ADMIN`, `MANAGER`, `USER`, `AUDITOR`.
Permissions: `USER_READ`, `USER_MANAGE`, `WORK_CREATE`, `WORK_EDIT`, `WORK_ASSIGN`, `ANALYTICS_VIEW`, `ANALYTICS_GLOBAL`, `AUDIT_VIEW`.

| Action | ADMIN | MANAGER | USER | AUDITOR |
| --- | --- | --- | --- | --- |
| Manage users / roles | Y | - | - | - |
| Create work item | Y | Y | Y | - |
| Assign in department | Y | Y (own dept) | - | - |
| Update own/assigned item | Y | Y | Y | - |
| Global analytics | Y | - | - | read-only |
| Department analytics | Y | Y | limited | read-only |
| View audit log | Y | limited | - | Y |

(Exact role -> permission mapping: Student 2 finalises and updates this table in Sprint 1.)

## Endpoints

| Method | Path | Service | Access | Sprint |
| --- | --- | --- | --- | --- |
| POST | /api/auth/login | auth | public | 1 |
| POST | /api/auth/refresh | auth | session cookie | 1 |
| POST | /api/auth/logout | auth | authenticated | 1 |
| GET | /api/users/me | user | authenticated | 1 |
| GET | /api/admin/users | user | ADMIN | 2 |
| POST | /api/work-items | core | WORK_CREATE | 1 |
| GET | /api/work-items?status=&priority=&page=&size=&sort= | core | authenticated (scoped) | 1 |
| GET | /api/work-items/{id} | core | authenticated (scoped) | 1 |
| PATCH | /api/work-items/{id}/status | core | WORK_EDIT | 2 |
| PATCH | /api/work-items/{id}/assignee | core | WORK_ASSIGN | 2 |
| POST | /api/work-items/{id}/comments | core | WORK_EDIT | 2 |
| GET | /api/work-items/{id}/comments | core | authenticated (scoped) | 2 |
| GET | /api/analytics/summary | analytics | ANALYTICS_VIEW | 2 |
| GET | /api/analytics/trends | analytics | ANALYTICS_VIEW | 3 |
| GET | /ws/analytics (or SSE `/api/analytics/stream`) | analytics | ANALYTICS_VIEW | 3 |
| GET | /api/notifications | notification | authenticated | 3 |
| PATCH | /api/notifications/{id}/read | notification | authenticated | 3 |

## Work item

`WorkItem { id, title, description, status, priority, creatorId, assigneeId, departmentId, createdAt, updatedAt, completedAt }`
Status: `NEW -> IN_PROGRESS -> BLOCKED <-> IN_PROGRESS -> COMPLETED`. Priority: `LOW | MEDIUM | HIGH | CRITICAL`.

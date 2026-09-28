# Event Contract

Broker: RabbitMQ. One **topic exchange** `platform.events`. Producers publish only after their DB transaction commits.

## Envelope (every event)

```json
{ "eventId": "uuid", "eventType": "WORK_ITEM_CREATED", "version": 1,
  "occurredAt": "2026-01-01T10:00:00Z", "producer": "core-service",
  "entityId": "123", "actorId": "42", "departmentId": "7",
  "payload": { } }
```

Consumers must be **idempotent**: remember processed `eventId`s so a redelivered event never double-counts.

## Events

| eventType | Routing key | Producer | Consumers | Payload |
| --- | --- | --- | --- | --- |
| WORK_ITEM_CREATED | work-item.created | core | analytics, notification | title, priority, status, assigneeId? |
| WORK_ITEM_ASSIGNED | work-item.assigned | core | analytics, notification | assigneeId, previousAssigneeId? |
| WORK_ITEM_STATUS_CHANGED | work-item.status-changed | core | analytics, notification | fromStatus, toStatus, completedAt? |
| USER_LOGIN_SUCCESS | auth.login-success | auth | audit (analytics) | ip, userAgent |
| USER_LOGIN_FAILED | auth.login-failed | auth | audit (analytics) | loginId, ip |

## Queues

| Queue | Binding | Owner |
| --- | --- | --- |
| analytics.events | `work-item.#`, `auth.#` | analytics |
| notification.events | `work-item.assigned`, `work-item.status-changed` | notification |

Each consumer declares its own queue (with a dead-letter queue for failed messages).

# Phase 0 Findings

## Vikunja Instance

- **URL:** http://localhost:3456
- **Version:** v2.1.0
- **User:** <your-username> (check Vikunja admin panel)
- **Auth:** JWT via POST /api/v1/login

## Critical: Vikunja POST Update Behavior

**Vikunja zeros out fields not included in the POST body when updating tasks.**

When updating a task (POST /api/v1/tasks/{id}), you MUST include ALL fields
you want to preserve, not just the changed ones. Otherwise they reset to
zero/empty values.

Example: sending `{"done": true}` will zero out `repeat_after`, `description`,
`hex_color`, etc.

**Impact on Tsosu sync:** The sync mapper must always send a complete task
object when updating, merging local and server state.

## Repeating Task Lifecycle

1. Create task with `repeat_after: 86400` (seconds) and `due_date`
2. Mark done: `POST /api/v1/tasks/{id}` with `done: true` + ALL other fields
3. Vikunja response:
   - `done` resets to `false`
   - `due_date` advances by `repeat_after` seconds
   - Same task ID (no new task created)
   - `done_at` records when it was completed

**Key insight:** Vikunja doesn't create a NEW task for the next occurrence.
It reuses the same task, resetting `done` and advancing `due_date`.

## Label CRUD

- PUT /api/v1/labels — create label
- PUT /api/v1/tasks/{id}/labels — attach label to task (body: `{"label_id": N}`)
- Labels support emoji in title (e.g., "high", "medium", "low")
- `hex_color` is stored WITHOUT the `#` prefix

## Project CRUD (for Routines)

- PUT /api/v1/projects — create project
- Description supports HTML comments for metadata: `<!-- tsosu-routine:MORNING -->`
- Projects auto-create 4 views: List, Gantt, Table, Kanban

## Task Model Key Fields

| Field | Type | Notes |
|-------|------|-------|
| repeat_after | integer | Seconds between repeats |
| repeat_mode | integer | 0=from due date, 1=from completion, 2=from current |
| hex_color | string | Without `#` prefix |
| labels | array | Read-only on task GET, use separate endpoint to manage |
| description | string | Supports HTML comments for metadata |
| due_date | string | ISO 8601 format |
| done | boolean | Resets to false on repeating tasks |
| done_at | string | Set by server when done=true |
| project_id | integer | Links task to project |
| position | number | For ordering |

## CalDAV (Fastmail)

Deferred to Phase 2. Will need Fastmail app-password.

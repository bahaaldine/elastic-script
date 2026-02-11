# Jira Connector

Connect to Jira to query issues, projects, sprints, and more.

## Setup

### Create Connector

```sql
CREATE CONNECTOR jira_ops
TYPE 'jira'
CONFIG {
    "url": "https://mycompany.atlassian.net",
    "email": "{{secrets.jira_email}}",
    "token": "{{secrets.jira_token}}"
};
```

### Configure Secrets

```bash
moltler secrets set jira_email your.email@company.com
moltler secrets set jira_token your_api_token
```

### Get API Token

1. Go to [Atlassian API Tokens](https://id.atlassian.com/manage-profile/security/api-tokens)
2. Click "Create API token"
3. Copy the token

---

## Available Entities

### Issues (`issues`)

```sql
SELECT key, summary, status, priority, assignee, created
FROM jira_ops.issues
WHERE project = 'OPS'
  AND type = 'Bug'
  AND status NOT IN ('Done', 'Closed')
ORDER BY priority DESC, created DESC;
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | STRING | Issue ID |
| `key` | STRING | Issue key (e.g., OPS-123) |
| `summary` | STRING | Issue summary |
| `description` | STRING | Full description |
| `type` | STRING | Issue type (Bug, Task, etc.) |
| `status` | STRING | Current status |
| `priority` | STRING | Priority level |
| `assignee` | STRING | Assignee username |
| `reporter` | STRING | Reporter username |
| `labels` | ARRAY | Issue labels |
| `components` | ARRAY | Components |
| `created` | TIMESTAMP | Creation time |
| `updated` | TIMESTAMP | Last update |
| `resolved` | TIMESTAMP | Resolution time |
| `project` | STRING | Project key |
| `sprint` | STRING | Current sprint |

### Projects (`projects`)

```sql
SELECT key, name, lead, projectTypeKey
FROM jira_ops.projects
WHERE projectTypeKey = 'software';
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | STRING | Project ID |
| `key` | STRING | Project key |
| `name` | STRING | Project name |
| `lead` | STRING | Project lead |
| `projectTypeKey` | STRING | Type (software, business) |
| `style` | STRING | Style (classic, next-gen) |

### Sprints (`sprints`)

```sql
SELECT id, name, state, startDate, endDate
FROM jira_ops.sprints
WHERE board = 123
  AND state = 'active';
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | NUMBER | Sprint ID |
| `name` | STRING | Sprint name |
| `state` | STRING | `future`, `active`, `closed` |
| `startDate` | TIMESTAMP | Sprint start |
| `endDate` | TIMESTAMP | Sprint end |
| `goal` | STRING | Sprint goal |
| `board` | NUMBER | Board ID |

### Comments (`comments`)

```sql
SELECT author, body, created
FROM jira_ops.comments
WHERE issue = 'OPS-123'
ORDER BY created DESC;
```

### Worklogs (`worklogs`)

```sql
SELECT author, timeSpentSeconds, started
FROM jira_ops.worklogs
WHERE issue = 'OPS-123';
```

---

## JQL Queries

Use native JQL for complex queries:

```sql
SELECT key, summary, status
FROM jira_ops.issues
WHERE JQL = 'project = OPS AND status = "In Progress" AND assignee = currentUser()';
```

### JQL Examples

```sql
-- Issues updated today
WHERE JQL = 'updated >= startOfDay()'

-- High priority bugs
WHERE JQL = 'type = Bug AND priority in (High, Highest)'

-- Issues in current sprint
WHERE JQL = 'sprint in openSprints()'

-- Overdue issues
WHERE JQL = 'duedate < now() AND status != Done'

-- Issues with specific label
WHERE JQL = 'labels = incident'
```

---

## Actions

### Create Issue

```sql
CONNECTOR_EXEC jira_ops.issues.create({
    "project": "OPS",
    "type": "Task",
    "summary": "Automated task from Moltler",
    "description": "This task was created automatically.",
    "priority": "Medium",
    "labels": ["automated"]
});
```

### Update Issue

```sql
CONNECTOR_EXEC jira_ops.issues.update(
    issue_key => 'OPS-123',
    data => {
        "summary": "Updated summary",
        "priority": "High"
    }
);
```

### Transition Issue

```sql
-- Move to "In Progress"
CONNECTOR_EXEC jira_ops.issues.transition(
    issue_key => 'OPS-123',
    transition => 'Start Progress'
);

-- Move to "Done"
CONNECTOR_EXEC jira_ops.issues.transition(
    issue_key => 'OPS-123',
    transition => 'Done',
    resolution => 'Fixed'
);
```

### Add Comment

```sql
CONNECTOR_EXEC jira_ops.issues.comment(
    issue_key => 'OPS-123',
    body => 'Automated comment from Moltler'
);
```

### Assign Issue

```sql
CONNECTOR_EXEC jira_ops.issues.assign(
    issue_key => 'OPS-123',
    assignee => 'johndoe'
);
```

### Add Watcher

```sql
CONNECTOR_EXEC jira_ops.issues.watch(
    issue_key => 'OPS-123',
    watcher => 'janedoe'
);
```

### Log Work

```sql
CONNECTOR_EXEC jira_ops.issues.worklog(
    issue_key => 'OPS-123',
    time_spent => '2h',
    comment => 'Investigating issue'
);
```

---

## Syncing

### Sync All Issues

```sql
SYNC CONNECTOR jira_ops.issues TO jira-issues-*;
```

### Sync Specific Project

```sql
SYNC CONNECTOR jira_ops.issues TO jira-issues-ops
WHERE project = 'OPS'
SCHEDULE '*/15 * * * *';
```

### Incremental Sync

```sql
SYNC CONNECTOR jira_ops.issues TO jira-issues-*
INCREMENTAL ON updated
SCHEDULE '*/5 * * * *';
```

---

## Example Skills

### Skill: Create Incident Ticket

```sql
CREATE SKILL create_incident_ticket
VERSION '1.0.0'
DESCRIPTION 'Creates a Jira incident ticket'
PARAMETERS (
    connector STRING,
    title STRING,
    description STRING,
    severity STRING DEFAULT 'Medium'
)
RETURNS DOCUMENT
BEGIN
    DECLARE priority = CASE severity
        WHEN 'Critical' THEN 'Highest'
        WHEN 'High' THEN 'High'
        WHEN 'Medium' THEN 'Medium'
        ELSE 'Low'
    END;
    
    DECLARE result = CONNECTOR_EXEC connector.issues.create({
        "project": "OPS",
        "type": "Incident",
        "summary": "[INCIDENT] " || title,
        "description": description,
        "priority": priority,
        "labels": ["incident", "automated"]
    });
    
    RETURN {
        "ticket_key": result.key,
        "url": result.self
    };
END SKILL;
```

### Skill: Get Sprint Status

```sql
CREATE SKILL get_sprint_status
VERSION '1.0.0'
DESCRIPTION 'Gets current sprint status with metrics'
PARAMETERS (
    connector STRING,
    project STRING
)
RETURNS DOCUMENT
BEGIN
    -- Get active sprint
    DECLARE sprint_cursor CURSOR FOR
        SELECT id, name, startDate, endDate
        FROM connector.sprints
        WHERE state = 'active'
        LIMIT 1;
    
    OPEN sprint_cursor;
    DECLARE sprint = NULL;
    FETCH sprint_cursor INTO sprint;
    CLOSE sprint_cursor;
    
    IF sprint IS NULL THEN
        RETURN {"error": "No active sprint found"};
    END IF;
    
    -- Get issue counts
    DECLARE issues_cursor CURSOR FOR
        SELECT status, COUNT(*) as count
        FROM connector.issues
        WHERE JQL = 'project = ' || project || ' AND sprint = ' || sprint.id
        GROUP BY status;
    
    DECLARE status_counts = {};
    OPEN issues_cursor;
    FOR issue IN issues_cursor LOOP
        SET status_counts[issue.status] = issue.count;
    END LOOP;
    CLOSE issues_cursor;
    
    -- Calculate progress
    DECLARE total = SUM(DOCUMENT_VALUES(status_counts));
    DECLARE done = status_counts.Done ?? 0;
    DECLARE progress = CASE WHEN total > 0 THEN ROUND(done * 100 / total) ELSE 0 END;
    
    RETURN {
        "sprint": sprint.name,
        "start_date": sprint.startDate,
        "end_date": sprint.endDate,
        "status_counts": status_counts,
        "total_issues": total,
        "done_issues": done,
        "progress_percent": progress
    };
END SKILL;
```

### Skill: Auto-Assign Incidents

```sql
CREATE SKILL auto_assign_incidents
VERSION '1.0.0'
DESCRIPTION 'Automatically assigns unassigned incidents'
PARAMETERS (
    connector STRING,
    project STRING,
    oncall_user STRING
)
RETURNS NUMBER
BEGIN
    DECLARE unassigned CURSOR FOR
        SELECT key
        FROM connector.issues
        WHERE JQL = 'project = ' || project || 
                    ' AND type = Incident AND assignee IS EMPTY';
    
    DECLARE count = 0;
    OPEN unassigned;
    FOR issue IN unassigned LOOP
        CONNECTOR_EXEC connector.issues.assign(
            issue_key => issue.key,
            assignee => oncall_user
        );
        SET count = count + 1;
    END LOOP;
    CLOSE unassigned;
    
    RETURN count;
END SKILL;
```

---

## Configuration Options

```sql
CREATE CONNECTOR jira_ops
TYPE 'jira'
CONFIG {
    "url": "https://mycompany.atlassian.net",
    "email": "{{secrets.jira_email}}",
    "token": "{{secrets.jira_token}}"
}
OPTIONS {
    "rate_limit": 100,       -- Requests per minute
    "timeout_seconds": 30,   -- Request timeout
    "max_results": 1000,     -- Max results per query
    "expand": ["changelog"]  -- Expand fields
};
```

---

## Troubleshooting

### Authentication Errors

```
Error: Basic authentication with password is not supported
```

Solution: Use API token, not password:

1. Generate token at [Atlassian API Tokens](https://id.atlassian.com/manage-profile/security/api-tokens)
2. Update secret: `moltler secrets set jira_token NEW_TOKEN`

### Rate Limiting

```
Error: Rate limit exceeded
```

Solution: Reduce rate limit in connector options.

### Permission Errors

```
Error: You do not have permission to view this issue
```

Solution: Ensure the API token owner has appropriate Jira permissions.

---

## What's Next?

<div class="grid cards" markdown>

-   :material-chart-line:{ .lg .middle } __Datadog Connector__

    Connect to Datadog.

    [:octicons-arrow-right-24: Datadog](datadog.md)

-   :material-bell:{ .lg .middle } __PagerDuty Connector__

    Connect to PagerDuty.

    [:octicons-arrow-right-24: PagerDuty](pagerduty.md)

</div>

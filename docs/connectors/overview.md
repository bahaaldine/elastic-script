# Connectors Overview

Connectors enable Moltler to pull data from external services like GitHub, Jira, Datadog, Salesforce, and more.

## What are Connectors?

A **Connector** is a configured connection to an external data source that allows you to:

1. **Query external data** with familiar SQL-like syntax
2. **Sync data to Elasticsearch** for analysis
3. **Execute actions** on external systems

```sql
-- Create a connector
CREATE CONNECTOR github_myorg
TYPE 'github'
CONFIG {
    "token": "{{secrets.github_token}}",
    "org": "mycompany"
};

-- Query it
SELECT * FROM github_myorg.issues
WHERE state = 'open' AND labels CONTAINS 'bug';

-- Sync to Elasticsearch
SYNC CONNECTOR github_myorg TO github-issues-*;
```

---

## Available Connectors

### DevOps & Development

| Connector | Description | Key Entities |
|-----------|-------------|--------------|
| [GitHub](github.md) | Repositories, issues, PRs, actions | `repos`, `issues`, `pulls`, `actions` |
| [GitLab](gitlab.md) | Projects, issues, MRs, pipelines | `projects`, `issues`, `merges`, `pipelines` |
| [Jira](jira.md) | Issues, projects, sprints | `issues`, `projects`, `sprints` |
| [Confluence](confluence.md) | Pages, spaces, comments | `pages`, `spaces` |

### Monitoring & Observability

| Connector | Description | Key Entities |
|-----------|-------------|--------------|
| [Datadog](datadog.md) | Metrics, monitors, events | `metrics`, `monitors`, `events` |
| [New Relic](newrelic.md) | APM, infrastructure, alerts | `applications`, `hosts`, `alerts` |
| [Prometheus](prometheus.md) | Metrics, alerts | `metrics`, `alerts` |
| [Grafana](grafana.md) | Dashboards, alerts | `dashboards`, `alerts` |

### Incident Management

| Connector | Description | Key Entities |
|-----------|-------------|--------------|
| [PagerDuty](pagerduty.md) | Incidents, services, on-call | `incidents`, `services`, `oncall` |
| [Opsgenie](opsgenie.md) | Alerts, schedules | `alerts`, `schedules` |
| [Statuspage](statuspage.md) | Incidents, components | `incidents`, `components` |

### Communication

| Connector | Description | Key Entities |
|-----------|-------------|--------------|
| [Slack](slack.md) | Messages, channels, users | `messages`, `channels`, `users` |
| [Microsoft Teams](teams.md) | Messages, channels, teams | `messages`, `channels` |

### Cloud Providers

| Connector | Description | Key Entities |
|-----------|-------------|--------------|
| [AWS](aws.md) | EC2, S3, Lambda, CloudWatch | `ec2`, `s3`, `lambda`, `cloudwatch` |
| [GCP](gcp.md) | Compute, Storage, Functions | `compute`, `storage`, `functions` |
| [Azure](azure.md) | VMs, Storage, Functions | `vms`, `storage`, `functions` |

### CRM & Support

| Connector | Description | Key Entities |
|-----------|-------------|--------------|
| [Salesforce](salesforce.md) | Accounts, opportunities, cases | `accounts`, `opportunities`, `cases` |
| [Zendesk](zendesk.md) | Tickets, users, organizations | `tickets`, `users`, `organizations` |
| [HubSpot](hubspot.md) | Contacts, deals, tickets | `contacts`, `deals`, `tickets` |

---

## Creating Connectors

### Basic Syntax

```sql
CREATE CONNECTOR connector_name
TYPE 'connector_type'
CONFIG {
    -- Configuration options
};
```

### With Secrets

Use secret references for sensitive data:

```sql
CREATE CONNECTOR my_connector
TYPE 'github'
CONFIG {
    "token": "{{secrets.github_token}}"
};
```

Configure secrets:

```bash
moltler secrets set github_token ghp_xxxxxxxxxxxx
```

### With Options

```sql
CREATE CONNECTOR my_connector
TYPE 'jira'
CONFIG {
    "url": "https://mycompany.atlassian.net",
    "email": "{{secrets.jira_email}}",
    "token": "{{secrets.jira_token}}"
}
OPTIONS {
    "sync_interval": "5m",
    "batch_size": 100,
    "rate_limit": 10
};
```

---

## Querying Connectors

### SELECT Syntax

```sql
SELECT columns FROM connector.entity
WHERE conditions
ORDER BY column
LIMIT n;
```

### Examples

```sql
-- GitHub: Open bugs assigned to me
SELECT title, created_at, html_url
FROM github_org.issues
WHERE state = 'open' 
  AND assignee = 'myusername'
  AND labels CONTAINS 'bug'
ORDER BY created_at DESC;

-- Jira: High priority incidents
SELECT key, summary, status, priority
FROM jira_project.issues
WHERE project = 'OPS'
  AND type = 'Incident'
  AND priority IN ('High', 'Critical')
  AND status != 'Resolved';

-- Datadog: Recent alerts
SELECT name, status, last_triggered
FROM datadog_prod.monitors
WHERE status = 'Alert'
ORDER BY last_triggered DESC
LIMIT 10;
```

### Joins (Coming Soon)

```sql
-- Correlate GitHub PRs with Jira tickets
SELECT 
    g.title as pr_title,
    j.summary as ticket_summary,
    j.status as ticket_status
FROM github_org.pulls g
JOIN jira_project.issues j ON g.title LIKE '%' || j.key || '%'
WHERE g.state = 'open';
```

---

## Syncing Data

### One-Time Sync

```sql
SYNC CONNECTOR github_org TO github-issues-*;
```

### Scheduled Sync

```sql
SYNC CONNECTOR github_org TO github-issues-*
SCHEDULE '*/15 * * * *';  -- Every 15 minutes
```

### Incremental Sync

```sql
SYNC CONNECTOR github_org TO github-issues-*
INCREMENTAL ON updated_at
SCHEDULE '*/5 * * * *';
```

### Selective Sync

```sql
-- Only sync specific entities
SYNC CONNECTOR github_org.issues TO github-issues-*
WHERE labels CONTAINS 'incident'
SCHEDULE '*/5 * * * *';
```

---

## Connector Actions

Some connectors support write operations:

### Create

```sql
CONNECTOR_EXEC github_org.issues.create({
    "title": "New issue from Moltler",
    "body": "Created automatically",
    "labels": ["automated"]
});
```

### Update

```sql
CONNECTOR_EXEC github_org.issues.update(
    issue_number => 123,
    data => {"state": "closed"}
);
```

### Custom Actions

```sql
CONNECTOR_EXEC pagerduty_prod.incidents.acknowledge(
    incident_id => 'P123ABC'
);
```

---

## Managing Connectors

### List Connectors

```sql
SHOW CONNECTORS;
```

Output:

```
┌─────────────────┬──────────┬────────────┬───────────────┐
│ Name            │ Type     │ Status     │ Last Sync     │
├─────────────────┼──────────┼────────────┼───────────────┤
│ github_myorg    │ github   │ connected  │ 5 minutes ago │
│ jira_ops        │ jira     │ connected  │ 2 minutes ago │
│ datadog_prod    │ datadog  │ error      │ 1 hour ago    │
└─────────────────┴──────────┴────────────┴───────────────┘
```

### View Connector Details

```sql
SHOW CONNECTOR github_myorg;
```

### Test Connection

```sql
TEST CONNECTOR github_myorg;
```

### Delete Connector

```sql
DROP CONNECTOR github_myorg;
```

---

## Connector Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                        Moltler                                │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│   QUERY                    SYNC                   ACTION      │
│   SELECT * FROM            SYNC CONNECTOR        CONNECTOR_   │
│   connector.entity         TO index              EXEC(...)    │
│        │                        │                    │        │
│        └────────────┬───────────┴────────────────────┘        │
│                     │                                          │
│              ┌──────┴──────┐                                  │
│              │  Connector  │                                  │
│              │   Engine    │                                  │
│              └──────┬──────┘                                  │
│                     │                                          │
├─────────────────────┼──────────────────────────────────────────┤
│                     ▼                                          │
│   ┌──────────────────────────────────────────────────────┐    │
│   │                 Elasticsearch                          │    │
│   │   ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │    │
│   │   │  Cache   │  │  Index   │  │ Connector Config │   │    │
│   │   └──────────┘  └──────────┘  └──────────────────┘   │    │
│   └──────────────────────────────────────────────────────┘    │
│                                                               │
└───────────────────────────────────────────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │       External Services       │
              │  GitHub  Jira  Datadog  AWS   │
              └───────────────────────────────┘
```

---

## Best Practices

### 1. Use Secrets for Credentials

```sql
-- Good: Use secret references
CREATE CONNECTOR my_conn TYPE 'github'
CONFIG {"token": "{{secrets.github_token}}"};

-- Bad: Hardcoded credentials
CREATE CONNECTOR my_conn TYPE 'github'
CONFIG {"token": "ghp_actualtoken123"};
```

### 2. Set Rate Limits

```sql
CREATE CONNECTOR my_conn TYPE 'github'
CONFIG {...}
OPTIONS {
    "rate_limit": 10,       -- Requests per second
    "rate_limit_burst": 50  -- Burst allowance
};
```

### 3. Handle Errors Gracefully

```sql
BEGIN
    TRY
        SELECT * FROM my_connector.items;
    CATCH connector_error THEN
        -- Fallback to cached data
        SELECT * FROM my-connector-cache-*;
    END TRY;
END;
```

### 4. Optimize Queries

```sql
-- Good: Specific fields and filters
SELECT id, title, status FROM jira.issues
WHERE project = 'OPS' AND status = 'Open'
LIMIT 100;

-- Bad: Full table scan
SELECT * FROM jira.issues;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-github:{ .lg .middle } __GitHub Connector__

    Connect to GitHub repositories.

    [:octicons-arrow-right-24: GitHub](github.md)

-   :material-jira:{ .lg .middle } __Jira Connector__

    Connect to Jira projects.

    [:octicons-arrow-right-24: Jira](jira.md)

-   :material-chart-line:{ .lg .middle } __Datadog Connector__

    Connect to Datadog monitoring.

    [:octicons-arrow-right-24: Datadog](datadog.md)

-   :material-hammer-wrench:{ .lg .middle } __Build Your Own__

    Create custom connectors.

    [:octicons-arrow-right-24: Building Connectors](building-connectors.md)

</div>

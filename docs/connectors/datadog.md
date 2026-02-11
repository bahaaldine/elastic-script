# Datadog Connector

Connect to Datadog to query metrics, monitors, events, and logs.

## Setup

### Create Connector

```sql
CREATE CONNECTOR datadog_prod
TYPE 'datadog'
CONFIG {
    "api_key": "{{secrets.datadog_api_key}}",
    "app_key": "{{secrets.datadog_app_key}}",
    "site": "datadoghq.com"  -- or datadoghq.eu, us3.datadoghq.com
};
```

### Configure Secrets

```bash
moltler secrets set datadog_api_key your_api_key
moltler secrets set datadog_app_key your_app_key
```

### Get API Keys

1. Go to [Datadog API Keys](https://app.datadoghq.com/organization-settings/api-keys)
2. Create an API key
3. Go to [Datadog Application Keys](https://app.datadoghq.com/organization-settings/application-keys)
4. Create an Application key

---

## Available Entities

### Monitors (`monitors`)

```sql
SELECT id, name, type, query, status, message
FROM datadog_prod.monitors
WHERE status IN ('Alert', 'Warn')
ORDER BY modified DESC;
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | NUMBER | Monitor ID |
| `name` | STRING | Monitor name |
| `type` | STRING | Monitor type |
| `query` | STRING | Monitor query |
| `message` | STRING | Alert message |
| `status` | STRING | `OK`, `Alert`, `Warn`, `No Data` |
| `priority` | NUMBER | Priority (1-5) |
| `tags` | ARRAY | Monitor tags |
| `created` | TIMESTAMP | Creation time |
| `modified` | TIMESTAMP | Last modification |

### Events (`events`)

```sql
SELECT title, text, alert_type, source, date_happened
FROM datadog_prod.events
WHERE alert_type = 'error'
  AND date_happened > NOW() - INTERVAL '24h'
ORDER BY date_happened DESC;
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | NUMBER | Event ID |
| `title` | STRING | Event title |
| `text` | STRING | Event text |
| `alert_type` | STRING | `error`, `warning`, `info`, `success` |
| `source` | STRING | Event source |
| `host` | STRING | Associated host |
| `tags` | ARRAY | Event tags |
| `date_happened` | TIMESTAMP | Event time |

### Metrics (`metrics`)

```sql
SELECT metric, points, scope
FROM datadog_prod.metrics
WHERE metric = 'system.cpu.user'
  AND scope = 'host:web-1'
  AND from = NOW() - INTERVAL '1h'
  AND to = NOW();
```

### Hosts (`hosts`)

```sql
SELECT host_name, up, is_muted, apps, sources
FROM datadog_prod.hosts
WHERE up = true
ORDER BY host_name;
```

Fields:

| Field | Type | Description |
|-------|------|-------------|
| `host_name` | STRING | Host name |
| `up` | BOOLEAN | Is host up |
| `is_muted` | BOOLEAN | Is muted |
| `apps` | ARRAY | Running applications |
| `sources` | ARRAY | Data sources |
| `tags` | ARRAY | Host tags |
| `last_reported_time` | TIMESTAMP | Last report |

### Downtimes (`downtimes`)

```sql
SELECT scope, message, start, end, active
FROM datadog_prod.downtimes
WHERE active = true;
```

### Dashboards (`dashboards`)

```sql
SELECT id, title, description, author_handle
FROM datadog_prod.dashboards
ORDER BY title;
```

---

## Metric Queries

### Query Metrics

```sql
SELECT * FROM datadog_prod.metrics
WHERE query = 'avg:system.cpu.user{*} by {host}'
  AND from = NOW() - INTERVAL '1h'
  AND to = NOW();
```

### Aggregated Metrics

```sql
SELECT 
    AVG(value) as avg_cpu,
    MAX(value) as max_cpu,
    MIN(value) as min_cpu
FROM datadog_prod.metrics
WHERE metric = 'system.cpu.user'
  AND from = NOW() - INTERVAL '24h';
```

---

## Actions

### Create Monitor

```sql
CONNECTOR_EXEC datadog_prod.monitors.create({
    "name": "High CPU Alert",
    "type": "metric alert",
    "query": "avg(last_5m):avg:system.cpu.user{*} > 90",
    "message": "CPU usage is above 90%! @slack-alerts",
    "tags": ["automated", "cpu"],
    "priority": 2
});
```

### Update Monitor

```sql
CONNECTOR_EXEC datadog_prod.monitors.update(
    monitor_id => 12345,
    data => {
        "message": "Updated alert message",
        "priority": 1
    }
);
```

### Mute Monitor

```sql
CONNECTOR_EXEC datadog_prod.monitors.mute(
    monitor_id => 12345,
    scope => 'host:web-1',
    end => NOW() + INTERVAL '1h'
);
```

### Unmute Monitor

```sql
CONNECTOR_EXEC datadog_prod.monitors.unmute(
    monitor_id => 12345
);
```

### Create Event

```sql
CONNECTOR_EXEC datadog_prod.events.create({
    "title": "Deployment completed",
    "text": "Version 1.2.3 deployed to production",
    "alert_type": "info",
    "tags": ["deployment", "production"]
});
```

### Schedule Downtime

```sql
CONNECTOR_EXEC datadog_prod.downtimes.create({
    "scope": "host:web-1",
    "message": "Scheduled maintenance",
    "start": NOW(),
    "end": NOW() + INTERVAL '2h'
});
```

---

## Syncing

### Sync Monitors

```sql
SYNC CONNECTOR datadog_prod.monitors TO datadog-monitors-*
SCHEDULE '*/15 * * * *';
```

### Sync Events

```sql
SYNC CONNECTOR datadog_prod.events TO datadog-events-*
INCREMENTAL ON date_happened
SCHEDULE '*/5 * * * *';
```

### Sync Hosts

```sql
SYNC CONNECTOR datadog_prod.hosts TO datadog-hosts-*
SCHEDULE '*/30 * * * *';
```

---

## Example Skills

### Skill: Check Alert Status

```sql
CREATE SKILL check_datadog_alerts
VERSION '1.0.0'
DESCRIPTION 'Checks current Datadog alert status'
PARAMETERS (
    connector STRING,
    severity STRING DEFAULT 'all'
)
RETURNS DOCUMENT
BEGIN
    DECLARE filter = CASE severity
        WHEN 'critical' THEN "status = 'Alert' AND priority <= 2"
        WHEN 'warning' THEN "status IN ('Alert', 'Warn')"
        ELSE "status IN ('Alert', 'Warn', 'No Data')"
    END;
    
    DECLARE alerts CURSOR FOR
        SELECT id, name, status, priority, message
        FROM connector.monitors
        WHERE filter
        ORDER BY priority, modified DESC;
    
    DECLARE results ARRAY = [];
    OPEN alerts;
    FOR alert IN alerts LOOP
        SET results = ARRAY_APPEND(results, {
            "id": alert.id,
            "name": alert.name,
            "status": alert.status,
            "priority": alert.priority
        });
    END LOOP;
    CLOSE alerts;
    
    RETURN {
        "total_alerts": ARRAY_LENGTH(results),
        "critical": ARRAY_LENGTH(ARRAY_FILTER(results, r => r.priority <= 2)),
        "alerts": results
    };
END SKILL;
```

### Skill: Create Deployment Event

```sql
CREATE SKILL log_deployment_to_datadog
VERSION '1.0.0'
DESCRIPTION 'Logs a deployment event to Datadog'
PARAMETERS (
    connector STRING,
    version STRING,
    environment STRING,
    service STRING
)
BEGIN
    CONNECTOR_EXEC connector.events.create({
        "title": "Deployment: " || service || " " || version,
        "text": "Deployed " || service || " version " || version || 
                " to " || environment,
        "alert_type": "info",
        "source": "moltler",
        "tags": [
            "deployment",
            "env:" || environment,
            "service:" || service,
            "version:" || version
        ]
    });
END SKILL;
```

### Skill: Correlate Alerts with Deployments

```sql
CREATE SKILL correlate_alerts_deployments
VERSION '1.0.0'
DESCRIPTION 'Finds alerts that occurred after recent deployments'
PARAMETERS (
    connector STRING,
    lookback_hours NUMBER DEFAULT 4
)
RETURNS ARRAY
BEGIN
    DECLARE cutoff = NOW() - INTERVAL lookback_hours || 'h';
    
    -- Get recent deployments
    DECLARE deployments CURSOR FOR
        SELECT title, date_happened, tags
        FROM connector.events
        WHERE title LIKE 'Deployment%'
          AND date_happened > cutoff
        ORDER BY date_happened;
    
    -- Get recent alerts
    DECLARE alerts CURSOR FOR
        SELECT name, status, modified, tags
        FROM connector.monitors
        WHERE status = 'Alert'
          AND modified > cutoff
        ORDER BY modified;
    
    DECLARE correlations ARRAY = [];
    
    OPEN deployments;
    FOR deploy IN deployments LOOP
        DECLARE related_alerts ARRAY = [];
        
        OPEN alerts;
        FOR alert IN alerts LOOP
            -- Check if alert happened within 30 min of deployment
            IF alert.modified >= deploy.date_happened 
               AND alert.modified <= deploy.date_happened + INTERVAL '30m' THEN
                SET related_alerts = ARRAY_APPEND(related_alerts, alert.name);
            END IF;
        END LOOP;
        CLOSE alerts;
        
        IF ARRAY_LENGTH(related_alerts) > 0 THEN
            SET correlations = ARRAY_APPEND(correlations, {
                "deployment": deploy.title,
                "time": deploy.date_happened,
                "alerts": related_alerts
            });
        END IF;
    END LOOP;
    CLOSE deployments;
    
    RETURN correlations;
END SKILL;
```

---

## Configuration Options

```sql
CREATE CONNECTOR datadog_prod
TYPE 'datadog'
CONFIG {
    "api_key": "{{secrets.datadog_api_key}}",
    "app_key": "{{secrets.datadog_app_key}}",
    "site": "datadoghq.com"
}
OPTIONS {
    "rate_limit": 60,        -- Requests per minute
    "timeout_seconds": 30,   -- Request timeout
    "max_results": 1000      -- Max results per query
};
```

---

## Troubleshooting

### Authentication Errors

```
Error: Forbidden - Invalid API or Application key
```

Solution:
1. Verify API key at [API Keys](https://app.datadoghq.com/organization-settings/api-keys)
2. Verify App key at [Application Keys](https://app.datadoghq.com/organization-settings/application-keys)
3. Check the site setting matches your Datadog region

### Rate Limiting

```
Error: Too Many Requests
```

Solution: Reduce rate limit in connector options.

### Empty Results

Ensure your query timeframe is correct and data exists.

---

## What's Next?

<div class="grid cards" markdown>

-   :material-bell:{ .lg .middle } __PagerDuty Connector__

    Connect to PagerDuty.

    [:octicons-arrow-right-24: PagerDuty](pagerduty.md)

-   :material-slack:{ .lg .middle } __Slack Connector__

    Connect to Slack.

    [:octicons-arrow-right-24: Slack](slack.md)

</div>

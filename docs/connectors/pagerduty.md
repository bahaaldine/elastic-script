# PagerDuty Connector

Connect to PagerDuty to manage incidents, services, and on-call schedules.

## Setup

### Create Connector

```sql
CREATE CONNECTOR pagerduty_prod
TYPE 'pagerduty'
CONFIG {
    "api_key": "{{secrets.pagerduty_token}}"
};
```

---

## Available Entities

### Incidents (`incidents`)

```sql
SELECT id, title, status, urgency, service, created_at
FROM pagerduty_prod.incidents
WHERE status = 'triggered'
ORDER BY created_at DESC;
```

### Services (`services`)

```sql
SELECT id, name, status, escalation_policy
FROM pagerduty_prod.services;
```

### On-Call (`oncall`)

```sql
SELECT user, schedule, start, end
FROM pagerduty_prod.oncall
WHERE schedule = 'Primary';
```

---

## Actions

### Trigger Incident

```sql
CONNECTOR_EXEC pagerduty_prod.incidents.trigger({
    "service_id": "P1234567",
    "title": "High CPU Alert",
    "body": "CPU usage exceeded 90% on web-1",
    "urgency": "high"
});
```

### Acknowledge Incident

```sql
CONNECTOR_EXEC pagerduty_prod.incidents.acknowledge(
    incident_id => 'P7654321'
);
```

### Resolve Incident

```sql
CONNECTOR_EXEC pagerduty_prod.incidents.resolve(
    incident_id => 'P7654321',
    resolution => 'Scaled up the service'
);
```

### Add Note

```sql
CONNECTOR_EXEC pagerduty_prod.incidents.note(
    incident_id => 'P7654321',
    content => 'Investigating the root cause'
);
```

---

## Example Skills

### Skill: Create Incident with Context

```sql
CREATE SKILL create_pagerduty_incident
VERSION '1.0.0'
DESCRIPTION 'Creates a PagerDuty incident with context'
PARAMETERS (
    connector STRING,
    service_id STRING,
    title STRING,
    details DOCUMENT,
    urgency STRING DEFAULT 'high'
)
RETURNS DOCUMENT
BEGIN
    DECLARE body = 'Alert: ' || title || '\n\n';
    
    FOR key IN DOCUMENT_KEYS(details) LOOP
        SET body = body || key || ': ' || details[key] || '\n';
    END LOOP;
    
    DECLARE result = CONNECTOR_EXEC connector.incidents.trigger({
        "service_id": service_id,
        "title": title,
        "body": body,
        "urgency": urgency
    });
    
    RETURN {
        "incident_id": result.id,
        "html_url": result.html_url
    };
END SKILL;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-hammer-wrench:{ .lg .middle } __Build Your Own__

    Create custom connectors.

    [:octicons-arrow-right-24: Building Connectors](building-connectors.md)

</div>

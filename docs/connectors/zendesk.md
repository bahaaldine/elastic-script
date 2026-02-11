# Zendesk Connector

Connect to Zendesk to query and manage tickets, users, and organizations.

## Setup

### Create Connector

```sql
CREATE CONNECTOR zendesk_support
TYPE 'zendesk'
CONFIG {
    "subdomain": "mycompany",
    "email": "{{secrets.zendesk_email}}",
    "api_token": "{{secrets.zendesk_token}}"
};
```

### Configure Secrets

```bash
moltler secrets set zendesk_email admin@company.com
moltler secrets set zendesk_token your_api_token
```

---

## Available Entities

### Tickets (`tickets`)

```sql
SELECT id, subject, status, priority, assignee_id, created_at
FROM zendesk_support.tickets
WHERE status = 'open'
  AND priority IN ('high', 'urgent')
ORDER BY created_at DESC;
```

### Users (`users`)

```sql
SELECT id, name, email, role, organization_id
FROM zendesk_support.users
WHERE role = 'end-user';
```

### Organizations (`organizations`)

```sql
SELECT id, name, domain_names, tags
FROM zendesk_support.organizations;
```

### Ticket Comments (`comments`)

```sql
SELECT id, body, author_id, created_at
FROM zendesk_support.comments
WHERE ticket_id = 12345;
```

---

## Actions

### Create Ticket

```sql
CONNECTOR_EXEC zendesk_support.tickets.create({
    "subject": "Automated ticket from Moltler",
    "description": "This ticket was created automatically",
    "priority": "normal",
    "tags": ["automated", "moltler"]
});
```

### Update Ticket

```sql
CONNECTOR_EXEC zendesk_support.tickets.update(
    ticket_id => 12345,
    data => {
        "status": "solved",
        "assignee_id": 67890
    }
);
```

### Add Comment

```sql
CONNECTOR_EXEC zendesk_support.tickets.comment(
    ticket_id => 12345,
    body => 'Automated response from Moltler',
    public => false
);
```

---

## Example Skills

### Skill: Auto-Respond to Common Issues

```sql
CREATE SKILL auto_respond_zendesk
VERSION '1.0.0'
DESCRIPTION 'Auto-responds to tickets matching common patterns'
PARAMETERS (
    connector STRING,
    pattern STRING,
    response STRING
)
RETURNS NUMBER
BEGIN
    DECLARE tickets CURSOR FOR
        SELECT id, subject
        FROM connector.tickets
        WHERE status = 'new'
          AND subject LIKE pattern;
    
    DECLARE count = 0;
    OPEN tickets;
    FOR ticket IN tickets LOOP
        CONNECTOR_EXEC connector.tickets.comment(
            ticket_id => ticket.id,
            body => response,
            public => true
        );
        SET count = count + 1;
    END LOOP;
    CLOSE tickets;
    
    RETURN count;
END SKILL;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-slack:{ .lg .middle } __Slack Connector__

    Connect to Slack.

    [:octicons-arrow-right-24: Slack](slack.md)

-   :material-hammer-wrench:{ .lg .middle } __Build Your Own__

    Create custom connectors.

    [:octicons-arrow-right-24: Building Connectors](building-connectors.md)

</div>

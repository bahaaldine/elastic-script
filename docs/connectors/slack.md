# Slack Connector

Connect to Slack to send messages, query channels, and react to events.

## Setup

### Create Connector

```sql
CREATE CONNECTOR slack_workspace
TYPE 'slack'
CONFIG {
    "bot_token": "{{secrets.slack_bot_token}}"
};
```

### Required Scopes

Your Slack bot token needs these scopes:

| Scope | Required For |
|-------|-------------|
| `chat:write` | Send messages |
| `channels:read` | List channels |
| `channels:history` | Read messages |
| `reactions:write` | Add reactions |
| `users:read` | User information |

---

## Available Entities

### Channels (`channels`)

```sql
SELECT id, name, is_private, num_members
FROM slack_workspace.channels
WHERE is_archived = false
ORDER BY num_members DESC;
```

### Messages (`messages`)

```sql
SELECT ts, text, user, reactions
FROM slack_workspace.messages
WHERE channel = 'C1234567890'
  AND ts > NOW() - INTERVAL '24h'
ORDER BY ts DESC;
```

### Users (`users`)

```sql
SELECT id, name, real_name, email
FROM slack_workspace.users
WHERE is_bot = false;
```

---

## Actions

### Send Message

```sql
CONNECTOR_EXEC slack_workspace.messages.send({
    "channel": "#alerts",
    "text": "Hello from Moltler!"
});
```

### Send Rich Message (Blocks)

```sql
CONNECTOR_EXEC slack_workspace.messages.send({
    "channel": "#alerts",
    "blocks": [
        {
            "type": "header",
            "text": {"type": "plain_text", "text": "Alert: High CPU"}
        },
        {
            "type": "section",
            "text": {"type": "mrkdwn", "text": "CPU usage is *above 90%* on host `web-1`"}
        },
        {
            "type": "actions",
            "elements": [
                {
                    "type": "button",
                    "text": {"type": "plain_text", "text": "Acknowledge"},
                    "action_id": "ack_alert"
                }
            ]
        }
    ]
});
```

### Add Reaction

```sql
CONNECTOR_EXEC slack_workspace.reactions.add(
    channel => 'C1234567890',
    timestamp => '1234567890.123456',
    emoji => 'white_check_mark'
);
```

### Update Message

```sql
CONNECTOR_EXEC slack_workspace.messages.update(
    channel => 'C1234567890',
    ts => '1234567890.123456',
    text => 'Updated message text'
);
```

---

## Example Skills

### Skill: Send Alert to Slack

```sql
CREATE SKILL send_slack_alert
VERSION '1.0.0'
DESCRIPTION 'Sends a formatted alert to Slack'
PARAMETERS (
    connector STRING,
    channel STRING DEFAULT '#alerts',
    title STRING,
    message STRING,
    severity STRING DEFAULT 'warning'
)
BEGIN
    DECLARE emoji = CASE severity
        WHEN 'critical' THEN ':rotating_light:'
        WHEN 'error' THEN ':x:'
        WHEN 'warning' THEN ':warning:'
        ELSE ':information_source:'
    END;
    
    DECLARE color = CASE severity
        WHEN 'critical' THEN '#FF0000'
        WHEN 'error' THEN '#FF6B6B'
        WHEN 'warning' THEN '#FFA500'
        ELSE '#36A64F'
    END;
    
    CONNECTOR_EXEC connector.messages.send({
        "channel": channel,
        "attachments": [{
            "color": color,
            "blocks": [
                {
                    "type": "header",
                    "text": {"type": "plain_text", "text": emoji || " " || title}
                },
                {
                    "type": "section",
                    "text": {"type": "mrkdwn", "text": message}
                },
                {
                    "type": "context",
                    "elements": [{
                        "type": "mrkdwn",
                        "text": "Sent by Moltler at " || CURRENT_TIMESTAMP()
                    }]
                }
            ]
        }]
    });
END SKILL;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-bell:{ .lg .middle } __PagerDuty Connector__

    Connect to PagerDuty.

    [:octicons-arrow-right-24: PagerDuty](pagerduty.md)

-   :material-hammer-wrench:{ .lg .middle } __Build Your Own__

    Create custom connectors.

    [:octicons-arrow-right-24: Building Connectors](building-connectors.md)

</div>

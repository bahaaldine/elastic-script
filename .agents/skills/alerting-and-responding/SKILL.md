---
name: alerting-and-responding
description: >
  Manage alerts, notifications, SLOs, and incident response. Use when creating
  alert rules, viewing active alerts, sending Slack or PagerDuty notifications,
  managing SLOs, or automating incident response.
---

# Alerting & Response

## Kibana Alerting

| Function | Example |
|----------|---------|
| `KIBANA_ALERTING_RULES()` | List rules |
| `KIBANA_ALERTING_GET_RULE(id)` | Get rule |
| `KIBANA_ALERTING_CREATE_RULE(config)` | Create rule |
| `KIBANA_ALERTING_UPDATE_RULE(id, config)` | Update rule |
| `KIBANA_ALERTING_DELETE_RULE(id)` | Delete rule |
| `KIBANA_ALERTING_ENABLE_RULE(id)` / `KIBANA_ALERTING_DISABLE_RULE(id)` | Enable/disable |
| `KIBANA_ALERTING_ALERTS(status?)` | `KIBANA_ALERTING_ALERTS('active')` |

## Slack

| Function | Example |
|----------|---------|
| `SLACK_SEND(channel, message)` | `SLACK_SEND('#alerts', 'Alert!')` |
| `SLACK_SEND_BLOCKS(channel, blocks)` | Rich messages |
| `SLACK_WEBHOOK(url, payload)` | Webhook |

```sql
SLACK_SEND('#incidents', 'Critical: Database connection failure');

SLACK_SEND_BLOCKS('#incidents', [
    {'type': 'header', 'text': {'type': 'plain_text', 'text': '🚨 Critical'}},
    {'type': 'section', 'text': {'type': 'mrkdwn', 'text': '*Service:* api\n*Error:* timeout'}}
]);
```

## PagerDuty

| Function | Example |
|----------|---------|
| `PAGERDUTY_TRIGGER(service, title, details)` | Create incident |
| `PAGERDUTY_ACKNOWLEDGE(incident_id)` | Acknowledge |
| `PAGERDUTY_RESOLVE(incident_id)` | Resolve |
| `PAGERDUTY_LIST_INCIDENTS(status?)` | List incidents |

```sql
SET incident = PAGERDUTY_TRIGGER('PSERVICE123', 'DB Failure', {
    'severity': 'critical',
    'custom_details': {'host': 'db-prod-01', 'error_count': 150}
});
```

## SLOs

| Function | Example |
|----------|---------|
| `KIBANA_SLO_LIST()` | List SLOs |
| `KIBANA_SLO_GET(id)` | Get SLO |
| `KIBANA_SLO_CREATE(config)` | Create SLO |
| `KIBANA_SLO_DELETE(id)` | Delete SLO |

## Automated Response

```sql
DECLARE error_count NUMBER;
ESQL FROM logs-* | WHERE @timestamp > NOW() - 5 MINUTES AND level = 'ERROR' | STATS count = COUNT(*) INTO error_count;

IF error_count > 100 THEN
    SLACK_SEND('#incidents', 'Error spike: ' || error_count || ' in 5 min');
    
    IF error_count > 500 THEN
        PAGERDUTY_TRIGGER('PSERVICE123', 'Critical Error Spike', {
            'error_count': error_count
        });
    END IF;
END IF;
```

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL list_alert_rules()` | List rules |
| `RUN SKILL get_active_alerts()` | Active alerts |
| `RUN SKILL send_slack_alert(channel, message)` | Slack |
| `RUN SKILL escalate_to_pagerduty(title, details)` | PagerDuty |

## Best Practices

- Set appropriate thresholds (avoid alert fatigue)
- Include actionable context in alerts
- Use escalation: Slack → PagerDuty
- Link to dashboards in alerts

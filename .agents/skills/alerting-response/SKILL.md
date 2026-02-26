---
name: alerting-response
description: Manage alerts, create alert rules, and respond to incidents with automated actions. Use when the user needs to set up monitoring alerts, respond to incidents, or integrate with notification channels.
---

# Alerting & Incident Response

This skill enables you to manage alerts, create monitoring rules, and automate incident response.

## When to Use

- User wants to **create alert rules** or **monitors**
- User needs to **view active alerts** or **incidents**
- User wants to **send notifications** (Slack, PagerDuty, etc.)
- User needs to **automate incident response**
- User asks about **SLOs** or **error budgets**

## Kibana Alerting Functions

| Function | Description | Example |
|----------|-------------|---------|
| `KIBANA_ALERTING_RULES()` | List all rules | `KIBANA_ALERTING_RULES()` |
| `KIBANA_ALERTING_GET_RULE(id)` | Get rule details | `KIBANA_ALERTING_GET_RULE('rule-id')` |
| `KIBANA_ALERTING_CREATE_RULE(config)` | Create rule | See below |
| `KIBANA_ALERTING_UPDATE_RULE(id, config)` | Update rule | See below |
| `KIBANA_ALERTING_DELETE_RULE(id)` | Delete rule | `KIBANA_ALERTING_DELETE_RULE('id')` |
| `KIBANA_ALERTING_ENABLE_RULE(id)` | Enable rule | `KIBANA_ALERTING_ENABLE_RULE('id')` |
| `KIBANA_ALERTING_DISABLE_RULE(id)` | Disable rule | `KIBANA_ALERTING_DISABLE_RULE('id')` |
| `KIBANA_ALERTING_ALERTS(status?)` | Get alerts | `KIBANA_ALERTING_ALERTS('active')` |

### Create an Alert Rule

```sql
DECLARE rule DOCUMENT;
SET rule = KIBANA_ALERTING_CREATE_RULE({
    'name': 'High Error Rate',
    'rule_type_id': 'logs.alert.document.count',
    'consumer': 'logs',
    'schedule': {'interval': '5m'},
    'params': {
        'index': ['logs-*'],
        'timeField': '@timestamp',
        'timeSize': 5,
        'timeUnit': 'm',
        'thresholdComparator': '>',
        'threshold': [100],
        'filterKuery': 'level: ERROR'
    },
    'actions': [
        {
            'group': 'fired',
            'id': 'slack-connector-id',
            'params': {
                'message': 'High error rate detected: {{context.value}} errors in 5 minutes'
            }
        }
    ]
});

PRINT 'Created rule: ' || rule['id'];
```

## Notification Integrations

### Slack

| Function | Description | Example |
|----------|-------------|---------|
| `SLACK_SEND(channel, message)` | Send message | `SLACK_SEND('#alerts', 'Alert!')` |
| `SLACK_SEND_BLOCKS(channel, blocks)` | Send rich message | See below |
| `SLACK_WEBHOOK(url, payload)` | Webhook message | See below |

```sql
-- Simple message
SLACK_SEND('#incidents', 'Critical: Database connection failure detected');

-- Rich message with blocks
SLACK_SEND_BLOCKS('#incidents', [
    {
        'type': 'header',
        'text': {'type': 'plain_text', 'text': '🚨 Critical Alert'}
    },
    {
        'type': 'section',
        'text': {'type': 'mrkdwn', 'text': '*Service:* payment-api\n*Error:* Connection timeout'}
    },
    {
        'type': 'actions',
        'elements': [
            {'type': 'button', 'text': {'type': 'plain_text', 'text': 'View in Kibana'}, 'url': 'https://...'}
        ]
    }
]);
```

### PagerDuty

| Function | Description | Example |
|----------|-------------|---------|
| `PAGERDUTY_TRIGGER(service, title, details)` | Create incident | See below |
| `PAGERDUTY_ACKNOWLEDGE(incident_id)` | Acknowledge | `PAGERDUTY_ACKNOWLEDGE('P123')` |
| `PAGERDUTY_RESOLVE(incident_id)` | Resolve incident | `PAGERDUTY_RESOLVE('P123')` |
| `PAGERDUTY_LIST_INCIDENTS(status?)` | List incidents | `PAGERDUTY_LIST_INCIDENTS('triggered')` |

```sql
DECLARE incident DOCUMENT;
SET incident = PAGERDUTY_TRIGGER(
    'PSERVICE123',  -- Service ID
    'Database Connection Failure',
    {
        'severity': 'critical',
        'source': 'elasticsearch',
        'custom_details': {
            'host': 'db-prod-01',
            'error_count': 150,
            'dashboard': 'https://kibana/...'
        }
    }
);

PRINT 'Created PagerDuty incident: ' || incident['incident']['id'];
```

## SLO Management

| Function | Description | Example |
|----------|-------------|---------|
| `KIBANA_SLO_LIST()` | List all SLOs | `KIBANA_SLO_LIST()` |
| `KIBANA_SLO_GET(id)` | Get SLO details | `KIBANA_SLO_GET('slo-id')` |
| `KIBANA_SLO_CREATE(config)` | Create SLO | See below |
| `KIBANA_SLO_DELETE(id)` | Delete SLO | `KIBANA_SLO_DELETE('slo-id')` |

### Create an SLO

```sql
DECLARE slo DOCUMENT;
SET slo = KIBANA_SLO_CREATE({
    'name': 'API Availability',
    'description': 'API should be available 99.9% of the time',
    'indicator': {
        'type': 'sli.kql.custom',
        'params': {
            'index': 'traces-apm*',
            'filter': 'service.name: api-gateway',
            'good': 'event.outcome: success',
            'total': '*'
        }
    },
    'timeWindow': {
        'duration': '30d',
        'type': 'rolling'
    },
    'objective': {
        'target': 0.999
    },
    'budgetingMethod': 'occurrences'
});
```

## Automated Incident Response

### Error Spike Response

```sql
-- Check for error spike
DECLARE error_count NUMBER;
ESQL FROM logs-* 
| WHERE @timestamp > NOW() - 5 MINUTES
| WHERE level = 'ERROR'
| STATS count = COUNT(*)
INTO error_count;

IF error_count > 100 THEN
    -- Get error details
    DECLARE errors ARRAY;
    ESQL FROM logs-* 
    | WHERE @timestamp > NOW() - 5 MINUTES
    | WHERE level = 'ERROR'
    | STATS count = COUNT(*) BY service.name, message
    | SORT count DESC
    | LIMIT 5
    INTO errors;
    
    -- Create summary
    DECLARE summary STRING;
    SET summary = 'Error spike detected: ' || error_count || ' errors in 5 min\n\nTop errors:\n';
    
    FOR err IN errors LOOP
        SET summary = summary || '- ' || err['service.name'] || ': ' || err['message'] || ' (' || err['count'] || ')\n';
    END LOOP;
    
    -- Notify
    SLACK_SEND('#incidents', summary);
    
    -- Create PagerDuty incident if critical
    IF error_count > 500 THEN
        PAGERDUTY_TRIGGER('PSERVICE123', 'Critical Error Spike', {
            'error_count': error_count,
            'summary': summary
        });
    END IF;
END IF;
```

## Pre-built Skills (Moltler)

| Skill | Description |
|-------|-------------|
| `RUN SKILL list_alert_rules()` | List all alert rules |
| `RUN SKILL get_active_alerts()` | Get currently firing alerts |
| `RUN SKILL create_error_alert(index, threshold)` | Create error alert |
| `RUN SKILL send_slack_alert(channel, message)` | Send Slack notification |
| `RUN SKILL escalate_to_pagerduty(title, details)` | Create PagerDuty incident |

## Best Practices

1. **Set appropriate thresholds** - Avoid alert fatigue
2. **Include context** - Alert messages should have actionable info
3. **Use escalation** - Start with Slack, escalate to PagerDuty
4. **Link to dashboards** - Include Kibana URLs in alerts
5. **Track SLOs** - Use SLOs to measure reliability objectively

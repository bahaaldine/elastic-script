# Generate Skills with AI

Use AI to generate skills from natural language descriptions.

## Overview

Moltler can generate complete skills from simple descriptions:

```sql
GENERATE SKILL FROM 'Monitor disk usage and alert when above 80%';
```

Output:

```sql
CREATE SKILL monitor_disk_usage
VERSION '1.0.0'
DESCRIPTION 'Monitors disk usage and alerts when above 80%'
PARAMETERS (
    threshold_percent NUMBER DEFAULT 80,
    check_interval STRING DEFAULT '5m'
)
RETURNS DOCUMENT
BEGIN
    DECLARE disk_usage CURSOR FOR
        FROM metrics-system-*
        | WHERE system.filesystem.used.pct IS NOT NULL
        | STATS usage = MAX(system.filesystem.used.pct) BY host.name
        | WHERE usage > threshold_percent / 100;
    
    DECLARE alerts ARRAY = [];
    
    OPEN disk_usage;
    FOR d IN disk_usage LOOP
        CALL send_alert(
            channel => '#ops',
            message => 'Disk usage on ' || d.host.name || ': ' || 
                       ROUND(d.usage * 100) || '%'
        );
        SET alerts = ARRAY_APPEND(alerts, d);
    END LOOP;
    CLOSE disk_usage;
    
    RETURN {
        "checked_at": CURRENT_TIMESTAMP(),
        "alerts_sent": ARRAY_LENGTH(alerts),
        "hosts_alerted": alerts
    };
END SKILL;
```

---

## Basic Generation

### Simple Skills

```sql
-- From description
GENERATE SKILL FROM 'Count errors in logs from the last hour';

-- With name
GENERATE SKILL count_recent_errors
FROM 'Count errors in logs from the last hour';
```

### With Parameters

```sql
GENERATE SKILL 
FROM 'Check if service is healthy, configurable timeout and retries'
WITH PARAMETERS (
    service_name STRING,
    timeout_seconds NUMBER,
    max_retries NUMBER
);
```

### With Examples

```sql
GENERATE SKILL 
FROM 'Analyze log patterns for anomalies'
WITH EXAMPLES (
    -- Example input
    INPUT {"logs": [{"level": "ERROR", "count": 100}]},
    -- Expected output
    OUTPUT {"anomalies": [{"type": "spike", "severity": "high"}]}
);
```

---

## Advanced Generation

### From Template

```sql
GENERATE SKILL
FROM 'Create PagerDuty incident with context'
BASED ON SKILL create_incident  -- Use existing skill as template
WITH MODIFICATIONS (
    'Add Jira ticket creation',
    'Include runbook link'
);
```

### From Runbook

```sql
GENERATE SKILL
FROM RUNBOOK '''
## High CPU Alert Response

1. Check which process is using most CPU
2. If it's a known memory leak, restart the pod
3. If it's unexpected load, scale the service
4. Notify the team with findings
''';
```

### From Existing Code

```sql
GENERATE SKILL
FROM PYTHON '''
def check_health(host):
    response = requests.get(f"{host}/health")
    return response.json()["status"] == "healthy"
''';
```

---

## Generation Options

### Model Selection

```sql
GENERATE SKILL
FROM 'Complex analysis skill'
USING MODEL 'gpt-4';  -- or 'claude-3', 'local'
```

### Skill Complexity

```sql
GENERATE SKILL
FROM 'Monitor service'
WITH COMPLEXITY 'simple';  -- 'simple', 'standard', 'advanced'
```

### Include Tests

```sql
GENERATE SKILL
FROM 'Validate email format'
WITH TESTS;  -- Generate tests automatically
```

Output includes:

```sql
CREATE SKILL validate_email ...

-- Auto-generated tests
TEST SKILL validate_email
NAME 'valid email passes'
WITH email = 'user@example.com'
EXPECT @result = TRUE;

TEST SKILL validate_email
NAME 'invalid email fails'
WITH email = 'not-an-email'
EXPECT @result = FALSE;
```

---

## Interactive Generation

### Refine Generation

```sql
-- Initial generation
GENERATE SKILL FROM 'Check API health';

-- Refine
REFINE SKILL 
ADD 'Include response time tracking'
ADD 'Support multiple endpoints';
```

### Preview Before Saving

```sql
-- Preview only
GENERATE SKILL FROM 'Monitor logs'
PREVIEW;  -- Don't save, just show

-- If satisfied, save
SAVE GENERATED SKILL AS monitor_logs;
```

### Ask Questions

```sql
GENERATE SKILL FROM 'Rotate credentials'
INTERACTIVE;
```

Output:

```
? Which credential types should be supported?
  > AWS IAM keys
  > Database passwords
  > API tokens
  > All of the above

? Should old credentials be archived or deleted?
  > Archive for 30 days
  > Delete immediately

? Notification after rotation?
  > Slack
  > Email
  > Both
```

---

## Context-Aware Generation

### Use Existing Data

```sql
-- Generate based on your actual data
GENERATE SKILL
FROM 'Analyze errors'
USING INDEX 'logs-*'  -- Learn from your data schema
SAMPLE 100;           -- Sample 100 documents
```

### Use Existing Skills

```sql
-- Generate skill that works with existing skills
GENERATE SKILL
FROM 'Comprehensive health check'
COMPOSE WITH [
    check_api_health,
    check_database_health,
    check_cache_health
];
```

### Use Connectors

```sql
-- Generate skill using available connectors
GENERATE SKILL
FROM 'Sync GitHub issues to Jira'
USING CONNECTORS [github_org, jira_project];
```

---

## Quality Controls

### Validation

Generated skills are automatically validated:

```sql
GENERATE SKILL FROM 'Process data';
```

Output includes validation:

```
✓ Syntax valid
✓ All functions exist
✓ Parameter types correct
✓ Return type matches
✓ No security issues detected
⚠ Warning: Consider adding error handling
```

### Security Scan

```sql
GENERATE SKILL FROM 'Execute command'
WITH SECURITY_SCAN;
```

```
Security scan results:
✓ No hardcoded secrets
✓ Input validation present
✓ Error messages don't leak sensitive info
⚠ Warning: Consider rate limiting
```

### Performance Analysis

```sql
GENERATE SKILL FROM 'Process large dataset'
WITH PERFORMANCE_ANALYSIS;
```

```
Performance analysis:
⚠ Potential issue: Query may timeout on large indices
  Recommendation: Add pagination with LIMIT
✓ Memory usage: Efficient streaming with cursor
```

---

## Examples

### Observability Skill

```sql
GENERATE SKILL 
FROM 'Detect anomalies in metrics using statistical analysis, 
      alert if values exceed 3 standard deviations from mean';
```

### Integration Skill

```sql
GENERATE SKILL
FROM 'When a critical alert fires, create a PagerDuty incident,
      post to Slack with context, and create a Jira ticket for tracking';
```

### Data Processing Skill

```sql
GENERATE SKILL
FROM 'Aggregate logs by service and error type,
      generate a daily summary report,
      identify trending issues';
```

### Remediation Skill

```sql
GENERATE SKILL
FROM 'When disk usage exceeds 90%, find and delete old log files,
      rotate large files, and alert if unable to free space';
```

---

## Best Practices

### 1. Be Specific

```sql
-- Good: Specific requirements
GENERATE SKILL FROM 
'Check HTTP endpoints for 200 status, 
 timeout after 5 seconds, 
 retry 3 times on failure,
 return list of unhealthy endpoints';

-- Bad: Too vague
GENERATE SKILL FROM 'Check endpoints';
```

### 2. Include Edge Cases

```sql
GENERATE SKILL FROM
'Parse log messages,
 handle malformed JSON gracefully,
 skip binary content,
 limit processing to 1000 lines';
```

### 3. Specify Error Handling

```sql
GENERATE SKILL FROM
'Query external API,
 handle rate limiting with exponential backoff,
 fallback to cached data on timeout,
 log all errors with context';
```

### 4. Review and Edit

Always review generated skills:

```sql
GENERATE SKILL FROM '...'
PREVIEW;

-- Review, then edit if needed
EDIT GENERATED SKILL
SET parameter_name = 'better_name';

SAVE GENERATED SKILL;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-auto-fix:{ .lg .middle } __Improve Skills__

    Enhance existing skills with AI.

    [:octicons-arrow-right-24: Improve Skills](improve-skills.md)

-   :material-lightbulb:{ .lg .middle } __Recommendations__

    Get AI-powered suggestions.

    [:octicons-arrow-right-24: Recommendations](recommendations.md)

</div>

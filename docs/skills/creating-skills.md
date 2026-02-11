# Creating Skills

This guide covers everything you need to know about creating Skills in Moltler.

## Skill Syntax

The complete syntax for creating a skill:

```sql
CREATE SKILL skill_name
VERSION 'semver'
[DESCRIPTION 'description']
[AUTHOR 'author']
[TAGS ['tag1', 'tag2']]
[REQUIRES [skill1, skill2]]
[PARAMETERS (
    param1 TYPE [DEFAULT value],
    param2 TYPE [DEFAULT value]
)]
[RETURNS return_type]
BEGIN
    -- Implementation
END SKILL;
```

---

## Metadata

### Version (Required)

Every skill must have a semantic version:

```sql
CREATE SKILL my_skill
VERSION '1.0.0'  -- Major.Minor.Patch
```

Version rules:

- **Major** - Breaking changes to parameters or behavior
- **Minor** - New features, backward compatible
- **Patch** - Bug fixes, no behavior change

### Description

Human-readable description of what the skill does:

```sql
DESCRIPTION 'Analyzes log patterns to detect anomalies and alert on-call'
```

Best practices:

- Keep it under 100 characters
- Start with a verb (Analyzes, Detects, Sends, Creates)
- Mention key inputs/outputs

### Author

Who created/maintains the skill:

```sql
AUTHOR 'sre-team'
AUTHOR 'jane.doe@company.com'
```

### Tags

Categorize skills for discovery:

```sql
TAGS ['observability', 'logs', 'alerting']
TAGS ['incident-response', 'pagerduty']
TAGS ['kubernetes', 'scaling', 'devops']
```

---

## Parameters

### Basic Parameters

```sql
PARAMETERS (
    required_param STRING,           -- Required, no default
    optional_param NUMBER DEFAULT 10 -- Optional with default
)
```

### Supported Types

| Type | Description | Example |
|------|-------------|---------|
| `STRING` | Text values | `'hello'` |
| `NUMBER` | Integers and decimals | `42`, `3.14` |
| `BOOLEAN` | True/false | `TRUE`, `FALSE` |
| `DOCUMENT` | JSON object | `{"key": "value"}` |
| `ARRAY` | List of values | `[1, 2, 3]` |
| `TIMESTAMP` | Date/time | `'2026-01-22T10:00:00Z'` |

### Parameter Validation

Add constraints to parameters:

```sql
PARAMETERS (
    -- String with allowed values
    severity STRING CHECK IN ('low', 'medium', 'high', 'critical'),
    
    -- Number with range
    threshold NUMBER CHECK BETWEEN 0 AND 100,
    
    -- String with pattern
    email STRING CHECK MATCHES '^[a-z]+@company\.com$',
    
    -- Required non-empty
    message STRING CHECK NOT EMPTY
)
```

### Default Values

```sql
PARAMETERS (
    index_pattern STRING DEFAULT 'logs-*',
    lookback_hours NUMBER DEFAULT 24,
    include_details BOOLEAN DEFAULT FALSE,
    tags ARRAY DEFAULT ['default'],
    config DOCUMENT DEFAULT {"verbose": false}
)
```

---

## Return Types

### Simple Returns

```sql
RETURNS STRING
BEGIN
    RETURN 'Success';
END SKILL;

RETURNS NUMBER
BEGIN
    RETURN 42;
END SKILL;

RETURNS BOOLEAN
BEGIN
    RETURN TRUE;
END SKILL;
```

### Document Returns

```sql
RETURNS DOCUMENT
BEGIN
    RETURN {
        "status": "success",
        "count": 10,
        "items": [1, 2, 3]
    };
END SKILL;
```

### Array Returns

```sql
RETURNS ARRAY
BEGIN
    DECLARE results ARRAY = [];
    -- Populate array
    RETURN results;
END SKILL;
```

### No Return (Void)

```sql
-- Omit RETURNS clause
BEGIN
    -- Side effects only (e.g., send notification)
    CALL SLACK_SEND('#alerts', 'Alert fired!');
END SKILL;
```

---

## Dependencies

### Requiring Other Skills

```sql
CREATE SKILL complex_analysis
VERSION '1.0.0'
REQUIRES [get_metrics, analyze_patterns, send_notification]
BEGIN
    -- Call required skills
    DECLARE metrics = CALL get_metrics('cpu');
    DECLARE patterns = CALL analyze_patterns(metrics);
    CALL send_notification(patterns);
END SKILL;
```

### Version Constraints

```sql
REQUIRES [
    get_metrics@^1.0.0,      -- Any 1.x.x
    analyze_patterns@~2.1.0, -- Any 2.1.x
    send_notification@2.0.0  -- Exact version
]
```

---

## Implementation Patterns

### Query and Process

```sql
CREATE SKILL find_slow_queries
VERSION '1.0.0'
PARAMETERS (threshold_ms NUMBER DEFAULT 1000)
RETURNS ARRAY
BEGIN
    DECLARE slow_queries ARRAY = [];
    
    DECLARE queries CURSOR FOR
        FROM logs-mysql-*
        | WHERE event.duration > threshold_ms * 1000000
        | SORT event.duration DESC
        | LIMIT 100;
    
    OPEN queries;
    FOR q IN queries LOOP
        SET slow_queries = ARRAY_APPEND(slow_queries, {
            "query": q.mysql.query,
            "duration_ms": q.event.duration / 1000000,
            "timestamp": q.@timestamp
        });
    END LOOP;
    CLOSE queries;
    
    RETURN slow_queries;
END SKILL;
```

### Conditional Logic

```sql
CREATE SKILL determine_severity
VERSION '1.0.0'
PARAMETERS (error_count NUMBER, error_rate NUMBER)
RETURNS STRING
BEGIN
    IF error_rate > 0.5 OR error_count > 1000 THEN
        RETURN 'critical';
    ELSIF error_rate > 0.2 OR error_count > 500 THEN
        RETURN 'high';
    ELSIF error_rate > 0.1 OR error_count > 100 THEN
        RETURN 'medium';
    ELSE
        RETURN 'low';
    END IF;
END SKILL;
```

### External Integration

```sql
CREATE SKILL create_incident
VERSION '1.0.0'
PARAMETERS (
    title STRING,
    description STRING,
    severity STRING DEFAULT 'medium'
)
RETURNS DOCUMENT
BEGIN
    -- Create PagerDuty incident
    DECLARE pd_incident = PAGERDUTY_TRIGGER(
        service_key => ENV('PAGERDUTY_KEY'),
        description => title,
        details => {"description": description}
    );
    
    -- Create Jira ticket
    DECLARE jira_ticket = HTTP_POST(
        ENV('JIRA_URL') || '/rest/api/2/issue',
        {
            "fields": {
                "project": {"key": "OPS"},
                "summary": title,
                "description": description,
                "issuetype": {"name": "Incident"}
            }
        },
        {"Authorization": "Bearer " || ENV('JIRA_TOKEN')}
    );
    
    RETURN {
        "pagerduty_id": pd_incident.incident_id,
        "jira_key": jira_ticket.key
    };
END SKILL;
```

### AI-Powered Analysis

```sql
CREATE SKILL analyze_with_ai
VERSION '1.0.0'
PARAMETERS (data DOCUMENT)
RETURNS DOCUMENT
BEGIN
    -- Generate prompt
    DECLARE prompt STRING = 
        'Analyze the following data and provide insights:\n' ||
        DOCUMENT_TO_JSON(data);
    
    -- Call LLM
    DECLARE analysis = LLM_COMPLETE(
        prompt => prompt,
        model => 'gpt-4',
        max_tokens => 500
    );
    
    -- Structure response
    DECLARE insights = LLM_EXTRACT(
        text => analysis,
        schema => {
            "summary": "string",
            "issues": "array",
            "recommendations": "array"
        }
    );
    
    RETURN insights;
END SKILL;
```

---

## Error Handling

### Basic Try/Catch

```sql
BEGIN
    TRY
        -- Risky operation
        DECLARE result = CALL external_api();
        RETURN result;
    CATCH
        RETURN {
            "status": "error",
            "error": ERROR_MESSAGE()
        };
    END TRY;
END SKILL;
```

### Specific Error Types

```sql
BEGIN
    TRY
        -- Operations
    CATCH connection_error THEN
        -- Handle connection issues
        CALL notify_team('Connection failed');
        RAISE;  -- Re-raise
    CATCH timeout_error THEN
        -- Handle timeouts
        RETURN {"status": "timeout"};
    CATCH OTHERS THEN
        -- Handle everything else
        RETURN {"status": "error", "error": ERROR_MESSAGE()};
    END TRY;
END SKILL;
```

### Finally Block

```sql
BEGIN
    DECLARE connection = NULL;
    
    TRY
        SET connection = open_connection();
        -- Use connection
    CATCH
        RETURN {"status": "error"};
    FINALLY
        -- Always clean up
        IF connection IS NOT NULL THEN
            CALL close_connection(connection);
        END IF;
    END TRY;
END SKILL;
```

---

## Updating Skills

### Create New Version

```sql
-- Original
CREATE SKILL my_skill VERSION '1.0.0' ...

-- Updated (new version)
CREATE SKILL my_skill VERSION '1.1.0' ...
```

### Deprecation

```sql
CREATE SKILL my_skill
VERSION '2.0.0'
DEPRECATES '1.x'  -- Marks all 1.x versions as deprecated
BEGIN
    -- New implementation
END SKILL;
```

### Migration Notes

```sql
CREATE SKILL my_skill
VERSION '2.0.0'
MIGRATION_NOTES 'Parameter "timeout" renamed to "timeout_ms"'
BEGIN
    -- Implementation
END SKILL;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-format-list-bulleted:{ .lg .middle } __Parameters__

    Deep dive into parameter types and validation.

    [:octicons-arrow-right-24: Parameters](parameters.md)

-   :material-test-tube:{ .lg .middle } __Testing__

    Write and run skill tests.

    [:octicons-arrow-right-24: Testing](testing.md)

-   :material-publish:{ .lg .middle } __Publishing__

    Share skills with the community.

    [:octicons-arrow-right-24: Publishing](publishing.md)

</div>

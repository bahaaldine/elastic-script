# Skills Overview

Skills are the core building blocks of Moltler. A **Skill** is a versioned, testable, reusable unit of operational knowledge.

## What is a Skill?

A Skill encapsulates:

- **Operational logic** - The steps to accomplish a task
- **Parameters** - Typed inputs that customize behavior
- **Return value** - Structured output
- **Metadata** - Version, description, author, tags
- **Tests** - Assertions to verify correct behavior

```sql
CREATE SKILL analyze_error_logs
VERSION '1.0.0'
DESCRIPTION 'Analyzes error logs and identifies patterns'
AUTHOR 'ops-team'
TAGS ['observability', 'logs', 'analysis']
PARAMETERS (
    index_pattern STRING DEFAULT 'logs-*',
    time_range STRING DEFAULT '1h',
    min_occurrences NUMBER DEFAULT 5
)
RETURNS DOCUMENT
BEGIN
    -- Query error logs
    DECLARE errors CURSOR FOR
        FROM index_pattern
        | WHERE level == 'ERROR'
        | WHERE @timestamp > NOW() - INTERVAL time_range
        | STATS count = COUNT(*) BY error.type, error.message
        | WHERE count >= min_occurrences
        | SORT count DESC;
    
    -- Build results
    DECLARE patterns ARRAY = [];
    
    OPEN errors;
    FOR err IN errors LOOP
        SET patterns = ARRAY_APPEND(patterns, {
            "type": err.error.type,
            "message": err.error.message,
            "count": err.count
        });
    END LOOP;
    CLOSE errors;
    
    RETURN {
        "analyzed_at": CURRENT_TIMESTAMP(),
        "index": index_pattern,
        "time_range": time_range,
        "patterns": patterns,
        "total_patterns": ARRAY_LENGTH(patterns)
    };
END SKILL;
```

---

## Skills vs. Procedures

You might wonder how Skills differ from Procedures:

| Feature | Procedure | Skill |
|---------|-----------|-------|
| **Versioning** | No | Yes - `VERSION '1.0.0'` |
| **Metadata** | Minimal | Rich - author, tags, description |
| **Testing** | Manual | Built-in - `TEST SKILL` |
| **Dependencies** | Implicit | Explicit - `REQUIRES` |
| **Publishing** | No | Yes - marketplace ready |
| **AI generation** | No | Yes - `GENERATE SKILL` |

**Use Procedures** for simple, one-off automation.  
**Use Skills** for reusable, production-grade operational knowledge.

---

## Skill Lifecycle

### 1. Create

```sql
CREATE SKILL my_skill
VERSION '1.0.0'
DESCRIPTION 'Does something useful'
BEGIN
    -- Implementation
END SKILL;
```

### 2. Test

```sql
TEST SKILL my_skill
WITH param1 = 'value'
EXPECT result.status = 'success';

RUN TESTS FOR SKILL my_skill;
```

### 3. Version

```sql
-- Create new version
CREATE SKILL my_skill
VERSION '1.1.0'
DESCRIPTION 'Does something useful (improved)'
BEGIN
    -- Updated implementation
END SKILL;

-- List versions
SHOW SKILL my_skill VERSIONS;
```

### 4. Deploy

```sql
-- Set default version
SET SKILL my_skill DEFAULT VERSION '1.1.0';

-- Call specific version
CALL my_skill@1.0.0(param1 => 'value');
```

### 5. Publish (Optional)

```sql
PUBLISH SKILL my_skill
TO 'marketplace'
WITH visibility = 'public';
```

---

## Skill Categories

### Observability Skills

Monitor and analyze your infrastructure:

```sql
CREATE SKILL detect_anomalies
VERSION '1.0.0'
DESCRIPTION 'Detects metric anomalies using statistical analysis'
PARAMETERS (
    metric_name STRING,
    threshold_stddev NUMBER DEFAULT 2.0
)
BEGIN
    -- Implementation
END SKILL;
```

### Incident Response Skills

Automate incident handling:

```sql
CREATE SKILL triage_incident
VERSION '1.0.0'
DESCRIPTION 'Triages an incident based on symptoms'
PARAMETERS (
    incident_id STRING,
    auto_escalate BOOLEAN DEFAULT FALSE
)
BEGIN
    -- Implementation
END SKILL;
```

### Integration Skills

Connect to external systems:

```sql
CREATE SKILL sync_jira_issues
VERSION '1.0.0'
DESCRIPTION 'Syncs Jira issues to Elasticsearch'
PARAMETERS (
    project STRING,
    issue_type STRING DEFAULT 'Bug'
)
BEGIN
    -- Implementation
END SKILL;
```

### AI-Powered Skills

Leverage LLMs for intelligent automation:

```sql
CREATE SKILL summarize_incident
VERSION '1.0.0'
DESCRIPTION 'Uses AI to summarize incident timeline'
PARAMETERS (incident_id STRING)
BEGIN
    DECLARE events CURSOR FOR
        FROM incidents-*
        | WHERE incident.id == incident_id
        | SORT @timestamp;
    
    DECLARE timeline STRING = '';
    OPEN events;
    FOR e IN events LOOP
        SET timeline = timeline || e.message || '\n';
    END LOOP;
    CLOSE events;
    
    RETURN LLM_SUMMARIZE(timeline);
END SKILL;
```

---

## Skill Discovery

### List Skills

```sql
-- All skills
SHOW SKILLS;

-- Filter by tag
SHOW SKILLS WHERE tags CONTAINS 'observability';

-- Search by description
SHOW SKILLS WHERE description LIKE '%error%';
```

### Skill Details

```sql
SHOW SKILL analyze_error_logs;
```

Output:

```
┌─────────────────────────────────────────────────────────────┐
│ Skill: analyze_error_logs                                    │
├─────────────────────────────────────────────────────────────┤
│ Version:     1.0.0                                           │
│ Author:      ops-team                                        │
│ Tags:        observability, logs, analysis                   │
│ Description: Analyzes error logs and identifies patterns     │
├─────────────────────────────────────────────────────────────┤
│ Parameters:                                                  │
│   - index_pattern: STRING (default: 'logs-*')               │
│   - time_range: STRING (default: '1h')                      │
│   - min_occurrences: NUMBER (default: 5)                    │
├─────────────────────────────────────────────────────────────┤
│ Returns: DOCUMENT                                            │
├─────────────────────────────────────────────────────────────┤
│ Tests: 3 defined, 3 passing                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## Best Practices

### 1. Use Semantic Versioning

```sql
-- Major: Breaking changes
VERSION '2.0.0'

-- Minor: New features, backward compatible
VERSION '1.1.0'

-- Patch: Bug fixes
VERSION '1.0.1'
```

### 2. Document Parameters

```sql
PARAMETERS (
    -- The Elasticsearch index pattern to search
    index_pattern STRING DEFAULT 'logs-*',
    
    -- How far back to look (e.g., '1h', '24h', '7d')
    time_range STRING DEFAULT '1h'
)
```

### 3. Handle Errors Gracefully

```sql
BEGIN
    TRY
        -- Main logic
        RETURN {"status": "success", ...};
    CATCH
        RETURN {
            "status": "error",
            "error": ERROR_MESSAGE(),
            "skill": "my_skill",
            "version": "1.0.0"
        };
    END TRY;
END SKILL;
```

### 4. Write Comprehensive Tests

```sql
-- Happy path
TEST SKILL my_skill
WITH param = 'valid_value'
EXPECT status = 'success';

-- Edge cases
TEST SKILL my_skill
WITH param = ''
EXPECT status = 'error';

-- Boundary conditions
TEST SKILL my_skill
WITH threshold = 0
EXPECT total >= 0;
```

### 5. Use Meaningful Names

```sql
-- Good
CREATE SKILL detect_memory_leak ...
CREATE SKILL notify_oncall_engineer ...
CREATE SKILL rollback_kubernetes_deployment ...

-- Bad
CREATE SKILL check ...
CREATE SKILL do_thing ...
CREATE SKILL process ...
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-pencil:{ .lg .middle } __Creating Skills__

    Detailed guide to skill creation.

    [:octicons-arrow-right-24: Creating Skills](creating-skills.md)

-   :material-format-list-bulleted:{ .lg .middle } __Parameters__

    Advanced parameter features.

    [:octicons-arrow-right-24: Parameters](parameters.md)

-   :material-test-tube:{ .lg .middle } __Testing__

    Write effective skill tests.

    [:octicons-arrow-right-24: Testing](testing.md)

-   :material-publish:{ .lg .middle } __Publishing__

    Share your skills.

    [:octicons-arrow-right-24: Publishing](publishing.md)

</div>

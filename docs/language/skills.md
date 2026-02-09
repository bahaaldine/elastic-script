# Standalone Skills

**Skills** are first-class objects that wrap procedures with metadata, making them discoverable and invokable by AI agents, natural language interfaces, and REST APIs.

## Overview

A skill provides:
- **Description** - What the skill does
- **Parameters** - Typed inputs with descriptions and defaults
- **Return type** - Expected output type
- **Examples** - Natural language phrases for AI discovery
- **MCP compatibility** - Tool specifications for AI agent frameworks

## Why Skills?

| Without Skills | With Skills |
|----------------|-------------|
| Procedures are code | Skills are capabilities |
| Manual discovery | REST API discovery |
| No parameter docs | Self-documenting |
| Code-only invocation | AI/NL invocation |

## Syntax

### CREATE SKILL

```sql
CREATE SKILL skill_name(param1 TYPE, param2 TYPE DESCRIPTION 'desc' DEFAULT value)
  RETURNS return_type
  DESCRIPTION 'What this skill does'
  EXAMPLES 'example phrase 1', 'example phrase 2'
  PROCEDURE procedure_name(param1, param2)
END SKILL
```

**Components:**
- `skill_name` - Unique identifier for the skill
- Parameters - Typed with optional DESCRIPTION and DEFAULT
- `RETURNS` - Output type (STRING, NUMBER, BOOLEAN, ARRAY, DOCUMENT)
- `DESCRIPTION` - Human/AI readable description
- `EXAMPLES` - Natural language phrases for discovery
- `PROCEDURE` - The underlying procedure to call

### DROP SKILL

```sql
DROP SKILL skill_name
```

### SHOW SKILLS

```sql
-- List all skills
SHOW SKILLS

-- Show details of a specific skill
SHOW SKILL skill_name
```

### ALTER SKILL

```sql
ALTER SKILL skill_name SET DESCRIPTION = 'Updated description'
```

### GENERATE SKILL

Generate a skill template from natural language:

```sql
-- Basic generation
GENERATE SKILL FROM 'Find customers who might leave based on order patterns'

-- With specific model
GENERATE SKILL FROM 'Analyze customer sentiment' WITH MODEL 'gpt-4'

-- Save with a specific name
GENERATE SKILL FROM 'Calculate revenue trends' SAVE AS revenue_analyzer
```

## Examples

### Basic Skill

```sql
-- First, create the underlying procedure
CREATE PROCEDURE find_inactive_users(days IN NUMBER)
BEGIN
    VAR result := ESQL_QUERY('FROM users-sample 
        | WHERE last_login < NOW() - ' || days || ' days
        | LIMIT 100');
    RETURN result;
END PROCEDURE;

-- Then wrap it as a skill
CREATE SKILL inactive_users(days NUMBER)
  RETURNS ARRAY
  DESCRIPTION 'Finds users who have not logged in recently'
  EXAMPLES 'Find inactive users', 'Show dormant accounts', 'Who has not logged in?'
  PROCEDURE find_inactive_users(days)
END SKILL
```

### Skill with Parameter Documentation

```sql
CREATE SKILL analyze_logs(
    severity STRING DESCRIPTION 'Log level: ERROR, WARN, INFO, DEBUG',
    hours NUMBER DESCRIPTION 'Time range in hours' DEFAULT 24,
    limit NUMBER DESCRIPTION 'Maximum results' DEFAULT 100
)
  RETURNS ARRAY
  DESCRIPTION 'Analyzes log entries by severity and time range'
  EXAMPLES 'Show me errors', 'Check warnings from last hour', 'Analyze recent logs'
  PROCEDURE log_analysis(severity, hours, limit)
END SKILL
```

### AI-Generated Skill

```sql
-- Describe what you want in natural language
GENERATE SKILL FROM 'Find products that are running low on inventory'

-- Output includes:
-- {
--   "action": "GENERATE SKILL",
--   "status": "template",
--   "suggested_skill": {
--     "name": "products_running_low_inventory",
--     "create_statement": "CREATE SKILL products_running_low_inventory(input STRING) ..."
--   }
-- }
```

## REST API

Skills are exposed via REST API for programmatic access and AI agent integration.

### List All Skills

```bash
curl -u elastic-admin:elastic-password \
  'http://localhost:9200/_escript/skills'
```

Response:
```json
{
  "skills": [
    {
      "name": "inactive_users",
      "description": "Finds users who have not logged in recently",
      "procedure": "find_inactive_users",
      "parameters": 1,
      "return_type": "ARRAY"
    }
  ],
  "count": 1
}
```

### Get Skill Details

```bash
curl -u elastic-admin:elastic-password \
  'http://localhost:9200/_escript/skills/inactive_users'
```

Response includes MCP tool specification:
```json
{
  "name": "inactive_users",
  "description": "Finds users who have not logged in recently",
  "parameters": [
    {"name": "days", "type": "NUMBER", "required": true}
  ],
  "return_type": "ARRAY",
  "examples": ["Find inactive users", "Show dormant accounts"],
  "mcp_tool_spec": {
    "name": "inactive_users",
    "description": "Finds users who have not logged in recently",
    "inputSchema": {
      "type": "object",
      "properties": {
        "days": {"type": "number", "description": ""}
      },
      "required": ["days"]
    }
  }
}
```

### Invoke a Skill

```bash
curl -u elastic-admin:elastic-password -X POST \
  'http://localhost:9200/_escript/skills/inactive_users/_invoke' \
  -H 'Content-Type: application/json' \
  -d '{"days": 30}'
```

## Storage

Skills are stored in the `.escript_skills` Elasticsearch index with:
- Automatic index creation on first skill
- Local caching for fast lookups
- Immediate refresh for consistency

## MCP Integration

Skills generate MCP-compatible tool specifications for AI agent frameworks:

```json
{
  "name": "inactive_users",
  "description": "Finds users who have not logged in recently",
  "inputSchema": {
    "type": "object",
    "properties": {
      "days": {
        "type": "number",
        "description": "Number of days of inactivity"
      }
    },
    "required": ["days"]
  },
  "examples": ["Find inactive users", "Show dormant accounts"]
}
```

This allows AI agents (Claude, GPT, etc.) to:
1. Discover available skills
2. Understand parameters and types
3. Invoke skills with proper arguments
4. Process structured responses

## Skills vs Application Skills

| Feature | Standalone Skills | Application Skills |
|---------|-------------------|-------------------|
| Storage | `.escript_skills` index | Embedded in application |
| Lifecycle | Independent | Tied to application |
| Reusability | Multiple apps can reference | Single app only |
| Discovery | Global skill search | Application-scoped |
| AI Generation | `GENERATE SKILL FROM` | Manual only |

## Use Cases

### AI Agent Integration

Expose Elasticsearch operations as callable tools:

```sql
CREATE SKILL search_logs(query STRING, hours NUMBER DEFAULT 1)
  RETURNS ARRAY
  DESCRIPTION 'Search logs for specific patterns or errors'
  EXAMPLES 'Find errors in logs', 'Search for timeout issues', 'Check for failed requests'
  PROCEDURE search_logs_impl(query, hours)
END SKILL
```

### Self-Service Analytics

Enable business users to run complex queries:

```sql
CREATE SKILL customer_segments(min_orders NUMBER DEFAULT 5)
  RETURNS DOCUMENT
  DESCRIPTION 'Analyze customer segments by order frequency'
  EXAMPLES 'Show customer segments', 'Analyze buying patterns', 'Who are my best customers?'
  PROCEDURE analyze_segments(min_orders)
END SKILL
```

### Operational Runbooks

Wrap runbook procedures for safe execution:

```sql
CREATE SKILL check_system_health()
  RETURNS DOCUMENT
  DESCRIPTION 'Performs comprehensive system health check'
  EXAMPLES 'Is the system healthy?', 'Check system status', 'Run health check'
  PROCEDURE run_health_check()
END SKILL
```

## Best Practices

1. **Always include DESCRIPTION** - Essential for AI discovery
2. **Add EXAMPLES** - Natural language phrases improve intent matching
3. **Document parameters** - Use DESCRIPTION for each parameter
4. **Provide defaults** - Make skills easier to invoke
5. **Use meaningful names** - `detect_churn` not `proc_001`
6. **Keep skills focused** - One skill = one capability
7. **Test via REST API** - Verify AI agents can invoke properly

## See Also

- [Intents](intents.md) - Natural language pattern matching
- [Procedures](procedures.md) - Underlying implementation

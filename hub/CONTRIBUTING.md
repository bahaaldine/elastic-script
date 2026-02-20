# Contributing to MoltlerHub

Thank you for your interest in contributing skills to MoltlerHub! This guide explains how to create, test, and submit new skills.

## Quick Start

1. **Fork** the repository
2. **Create** your skill directory under `hub/skills/elastic/<category>/<skill-name>/`
3. **Add** the required files: `skill.yaml`, `skill.sql`, `README.md`
4. **Test** your skill locally
5. **Submit** a pull request

## Step-by-Step Guide

### 1. Choose a Category

Place your skill in the appropriate category:

| Category | Use For |
|----------|---------|
| `observability` | Logs, metrics, traces, SLOs |
| `security` | SIEM, threat hunting, risk scores |
| `search` | Document operations, aggregations |
| `apm` | Application performance monitoring |
| `ml` | Machine learning, anomaly detection |
| `alerting` | Alert rules, notifications |
| `cluster` | Cluster health, node management |
| `integrations` | External services (Slack, Jira, etc.) |
| `fleet` | Agent and integration management |
| `meta` | Skills about skills, discovery |

### 2. Create Skill Directory

```bash
mkdir -p hub/skills/elastic/observability/my-new-skill
cd hub/skills/elastic/observability/my-new-skill
```

### 3. Create skill.yaml (Required)

```yaml
name: my-new-skill
version: 1.0.0
description: Short description of what the skill does (< 100 chars)
author: your-github-username
category: observability
tags:
  - logs
  - analysis
  - debugging
license: Elastic-2.0
requirements:
  elasticsearch: ">=8.0.0"
  features:
    - esql
main: skill.sql
```

### 4. Create skill.sql (Required)

```sql
CREATE SKILL my_new_skill
  VERSION '1.0.0'
  DESCRIPTION 'Detailed description for AI discovery. Explain WHEN to use this skill and WHAT it does.'
  AUTHOR 'your-github-username'
  TAGS ['observability', 'logs', 'analysis']
  (
    param1 STRING DESCRIPTION 'First parameter' DEFAULT 'logs-*',
    param2 INT DESCRIPTION 'Second parameter' DEFAULT 10
  )
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  
  -- Your implementation using ES|QL
  SET result = ESQL_QUERY('FROM ' || param1 || ' | LIMIT ' || param2);
  
  RETURN result;
END SKILL;
```

**Important Naming Rules:**
- Directory name: `kebab-case` (e.g., `get-recent-errors`)
- Skill name in SQL: `snake_case` (e.g., `get_recent_errors`)
- They should match (just different case style)

### 5. Create README.md (Required)

```markdown
# skill_name

Brief description of the skill.

## Usage

\`\`\`sql
-- Basic usage
RUN SKILL skill_name();

-- With parameters
RUN SKILL skill_name(param1 => 'value', param2 => 42);
\`\`\`

## Parameters

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `param1` | STRING | logs-* | Description |
| `param2` | INT | 10 | Description |

## Returns

Description of what the skill returns.

## Examples

### Example 1: Basic Usage
\`\`\`sql
RUN SKILL skill_name();
\`\`\`

### Example 2: With Filters
\`\`\`sql
RUN SKILL skill_name(param1 => 'custom-index-*');
\`\`\`
```

### 6. Test Your Skill

```bash
# Start Elasticsearch
cd /path/to/elastic-script
./scripts/quick-start.sh

# Install your skill
./hub/moltler-cli.sh install my-new-skill

# Verify it's installed
./hub/moltler-cli.sh installed

# Test via MCP (optional)
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {"name": "my_new_skill", "arguments": {}},
    "id": 1
  }'
```

### 7. Submit Pull Request

1. Commit your changes:
   ```bash
   git add hub/skills/elastic/observability/my-new-skill/
   git commit -m "Add my-new-skill to observability category"
   ```

2. Push to your fork and create a PR

3. Fill out the PR template with:
   - Skill description
   - Use cases
   - Test results

## Skill Design Guidelines

### 1. AI-Friendly Descriptions

Write descriptions that help AI agents know **when** to use your skill:

```sql
-- Good: Explains when to use it
DESCRIPTION 'Get recent error logs with details. Use this when investigating production issues, debugging errors, or responding to alerts about high error rates.'

-- Bad: Just says what it does
DESCRIPTION 'Gets error logs'
```

### 2. Sensible Defaults

Make skills work out-of-the-box:

```sql
-- Good: Works without arguments
(index_pattern STRING DEFAULT 'logs-*', limit INT DEFAULT 20)

-- Bad: Requires all parameters
(index_pattern STRING, limit INT)  -- Forces user to specify everything
```

### 3. Use ES|QL

Skills should leverage ES|QL for data operations:

```sql
SET result = ESQL_QUERY('FROM ' || index || ' | WHERE level == "ERROR" | LIMIT ' || limit);
```

### 4. Return Structured Data

Return data that's easy to process:

```sql
RETURN {
  'status': 'healthy',
  'metrics': {'cpu': 45, 'memory': 62},
  'timestamp': CURRENT_TIMESTAMP()
};
```

### 5. Handle Edge Cases

Check for empty results:

```sql
IF result IS NULL OR ARRAY_LENGTH(result) == 0 THEN
  RETURN {'status': 'no_data', 'message': 'No matching records found'};
END IF;
```

## Testing Checklist

Before submitting, verify:

- [ ] `skill.yaml` has all required fields
- [ ] `skill.sql` parses without errors
- [ ] Skill installs successfully via CLI
- [ ] Skill appears in `SHOW SKILLS`
- [ ] Skill executes with default parameters
- [ ] Skill appears in MCP `tools/list`
- [ ] README.md has usage examples
- [ ] Tags are relevant and searchable

## Common Issues

### Skill Won't Parse

Check these common issues:
- Tags must be separate strings: `['tag1', 'tag2']` not `['tag1,tag2']`
- Use `END SKILL;` not `END PROCEDURE;`
- Parameter descriptions need quotes: `DESCRIPTION 'text'`

### Skill Not Found After Install

- Directory name must match skill name (kebab-case vs snake_case)
- Check `.escript_skills` index exists
- Verify Elasticsearch is running

### MCP Shows Empty Tools

- Install skills first: `./moltler-cli.sh install --all`
- Check skills are in registry: `SHOW SKILLS;`

## Questions?

Open an issue on GitHub or check existing skills for examples.

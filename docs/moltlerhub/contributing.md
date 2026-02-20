# Contributing Skills to MoltlerHub

This guide explains how to contribute new skills to the MoltlerHub repository.

---

## Overview

MoltlerHub is a GitHub-based skill repository. Contributing a new skill is as simple as:

1. Fork the repository
2. Create your skill directory
3. Add the required files
4. Submit a pull request

---

## Quick Start

### 1. Fork and Clone

```bash
git clone https://github.com/YOUR_USERNAME/elastic-script.git
cd elastic-script/hub
```

### 2. Create Skill Directory

Skills are organized by category:

```bash
mkdir -p skills/elastic/observability/my-new-skill
cd skills/elastic/observability/my-new-skill
```

Available categories:

| Category | Description |
|----------|-------------|
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

### 3. Create Required Files

Every skill needs three files:

=== "skill.yaml"

    ```yaml
    name: my-new-skill
    version: 1.0.0
    description: Short description (< 100 chars)
    author: your-github-username
    category: observability
    tags:
      - logs
      - analysis
    license: Elastic-2.0
    requirements:
      elasticsearch: ">=8.0.0"
      features:
        - esql
    main: skill.sql
    ```

=== "skill.sql"

    ```sql
    CREATE SKILL my_new_skill
      VERSION '1.0.0'
      DESCRIPTION 'Detailed description for AI discovery. Explain WHEN to use this skill.'
      AUTHOR 'your-github-username'
      TAGS ['observability', 'logs', 'analysis']
      (
        index_pattern STRING DESCRIPTION 'Index to search' DEFAULT 'logs-*',
        limit INT DESCRIPTION 'Max results' DEFAULT 20
      )
      RETURNS ARRAY
    BEGIN
      DECLARE result ARRAY;
      SET result = ESQL_QUERY('FROM ' || index_pattern || ' | LIMIT ' || limit);
      RETURN result;
    END SKILL;
    ```

=== "README.md"

    ```markdown
    # my_new_skill

    Brief description of what the skill does.

    ## Usage

    ```sql
    -- Basic usage
    RUN SKILL my_new_skill();

    -- With parameters
    RUN SKILL my_new_skill(index_pattern => 'custom-*', limit => 50);
    ```

    ## Parameters

    | Name | Type | Default | Description |
    |------|------|---------|-------------|
    | `index_pattern` | STRING | logs-* | Index to search |
    | `limit` | INT | 20 | Max results |

    ## Returns

    Array of matching documents.
    ```

### 4. Test Your Skill

```bash
# Start Elasticsearch if not running
cd /path/to/elastic-script
./scripts/quick-start.sh

# Install your skill
./hub/moltler-cli.sh install my-new-skill

# Verify it appears
./hub/moltler-cli.sh installed

# Test via API
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL my_new_skill()"}'
```

### 5. Submit Pull Request

```bash
git add hub/skills/elastic/observability/my-new-skill/
git commit -m "Add my-new-skill to observability category"
git push origin main
```

Then create a PR on GitHub.

---

## Naming Conventions

| Item | Convention | Example |
|------|------------|---------|
| Directory name | `kebab-case` | `get-recent-errors` |
| Skill name (SQL) | `snake_case` | `get_recent_errors` |
| Version | Semantic versioning | `1.0.0` |
| Tags | Lowercase, array | `['logs', 'errors']` |

!!! warning "Important"
    Directory name and skill name must match (just different case style).
    `get-recent-errors/` → `CREATE SKILL get_recent_errors`

---

## Writing Good Descriptions

The description is crucial for AI agent discovery. Write it to explain **when** to use the skill:

=== "Good"

    ```sql
    DESCRIPTION 'Get recent error logs with full details including message, timestamp, and service. Use this when investigating production issues, debugging errors, or responding to alerts about high error rates.'
    ```

=== "Bad"

    ```sql
    DESCRIPTION 'Gets error logs'
    ```

**Tips:**

- Start with what the skill does
- Add context about when to use it
- Mention specific use cases
- Keep it under 200 characters

---

## Parameter Best Practices

### Provide Sensible Defaults

```sql
-- Good: Works without any arguments
(
  index_pattern STRING DEFAULT 'logs-*',
  limit INT DEFAULT 20
)

-- Bad: Forces user to provide everything
(
  index_pattern STRING,
  limit INT
)
```

### Use Descriptive Names

```sql
-- Good
index_pattern STRING DESCRIPTION 'Elasticsearch index pattern to search'

-- Bad  
idx STRING
```

### Handle NULL Values

```sql
BEGIN
  IF service IS NOT NULL THEN
    SET query = query || ' AND service == "' || service || '"';
  END IF;
END SKILL;
```

---

## Return Values

Return structured data that's easy to process:

```sql
-- Good: Structured response
RETURN {
  'status': 'healthy',
  'error_count': 5,
  'error_rate': 2.5,
  'checked_at': CURRENT_TIMESTAMP()
};

-- Also good: Array of results
RETURN ESQL_QUERY('FROM logs-* | WHERE level == "ERROR" | LIMIT 10');
```

### Handle Empty Results

```sql
IF result IS NULL OR ARRAY_LENGTH(result) == 0 THEN
  RETURN {
    'status': 'no_data',
    'message': 'No matching records found'
  };
END IF;
```

---

## Common Issues

### Skill Won't Parse

**Tags must be separate strings:**

```sql
-- Wrong
TAGS ['logs,errors,debugging']

-- Correct
TAGS ['logs', 'errors', 'debugging']
```

**Use END SKILL, not END PROCEDURE:**

```sql
-- Wrong
END PROCEDURE;

-- Correct
END SKILL;
```

### Skill Not Found After Install

- Check directory name matches skill name
- Verify Elasticsearch is running
- Check the `.escript_skills` index exists

### MCP Shows Empty Tools

- Skills must be installed first
- Run `SHOW SKILLS;` to verify registration

---

## Validation Checklist

Before submitting, verify:

- [ ] `skill.yaml` has all required fields
- [ ] `skill.sql` parses without errors
- [ ] Skill installs successfully
- [ ] Skill appears in `SHOW SKILLS`
- [ ] Skill executes with default parameters
- [ ] Skill appears in MCP `tools/list`
- [ ] README.md has usage examples
- [ ] Tags are relevant and searchable
- [ ] Description explains when to use it

---

## Example Skills

Browse existing skills for reference:

| Skill | Category | Description |
|-------|----------|-------------|
| `get-recent-errors` | observability | Error log retrieval |
| `hunt-ioc` | security | Threat hunting |
| `get-trace` | apm | Distributed tracing |
| `cluster-health` | cluster | Health checks |

See the [hub/skills/elastic/](https://github.com/elastic/elastic-script/tree/main/hub/skills/elastic) directory.

---

## What's Next?

<div class="grid cards" markdown>

-   :material-package:{ .lg .middle } __Skill Packs__

    Bundle related skills together.

    [:octicons-arrow-right-24: Skill Packs](../skills/skill-packs.md)

-   :material-api:{ .lg .middle } __MCP Integration__

    Expose skills to AI agents.

    [:octicons-arrow-right-24: MCP](../mcp.md)

</div>

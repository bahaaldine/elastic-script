# Standalone Skills Notebook

This notebook demonstrates the standalone skills system - first-class skill objects that can be created, managed, and invoked independently of applications.

## What You'll Learn

- Creating skills with `CREATE SKILL`
- Viewing skills with `SHOW SKILLS` and `SHOW SKILL`
- Modifying skills with `ALTER SKILL`
- AI-assisted skill generation with `GENERATE SKILL`
- Removing skills with `DROP SKILL`

## Prerequisites

- Elasticsearch running with elastic-script plugin
- Sample data loaded (optional, for more complex examples)

## Running the Notebook

```bash
# Start Elasticsearch
./scripts/quick-start.sh

# Open Jupyter
jupyter notebook notebooks/22-standalone-skills.ipynb
```

## Key Concepts

### Skills vs Procedures

| Procedures | Skills |
|------------|--------|
| Implementation code | Capability wrapper |
| Developer-facing | AI/User-facing |
| No metadata | Rich metadata |
| Direct calls only | REST API callable |

### Skill Components

```sql
CREATE SKILL skill_name(
    param1 TYPE DESCRIPTION 'desc' DEFAULT value
)
  RETURNS return_type
  DESCRIPTION 'What this skill does'
  EXAMPLES 'example phrase 1', 'example phrase 2'
  PROCEDURE underlying_procedure(param1)
END SKILL
```

## Examples from the Notebook

### Creating a Basic Skill

```sql
-- First, create the procedure
CREATE PROCEDURE run_analysis(threshold IN NUMBER)
BEGIN
    VAR result := DOCUMENT { 'threshold' => threshold, 'count' => 42 };
    RETURN result;
END PROCEDURE;

-- Then wrap it as a skill
CREATE SKILL analyze_data(threshold NUMBER)
  RETURNS DOCUMENT
  DESCRIPTION 'Analyzes data with a threshold'
  EXAMPLES 'Analyze my data', 'Run analysis'
  PROCEDURE run_analysis(threshold)
END SKILL
```

### Viewing Skills

```sql
-- List all skills
SHOW SKILLS

-- Get details of a specific skill
SHOW SKILL analyze_data
```

### AI-Generated Skills

```sql
-- Describe what you want
GENERATE SKILL FROM 'Find customers who might leave'

-- Returns a template you can customize and execute
```

## REST API Integration

Skills are exposed via REST API for AI agent integration:

```bash
# List all skills
curl -u elastic-admin:elastic-password \
  http://localhost:9200/_escript/skills

# Invoke a skill
curl -u elastic-admin:elastic-password -X POST \
  http://localhost:9200/_escript/skills/analyze_data/_invoke \
  -H 'Content-Type: application/json' \
  -d '{"threshold": 10}'
```

## See Also

- [Standalone Skills Language Guide](../language/skills.md) - Complete syntax reference
- [Intents](intents.md) - Natural language pattern matching

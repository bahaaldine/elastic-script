# explain_skill

Get detailed explanation of a specific skill.

## Usage

```sql
-- Get details for a specific skill
RUN SKILL explain_skill(skill_name => 'count_logs_by_level');

-- Understand what a skill does
RUN SKILL explain_skill(skill_name => 'search_skills');
```

## Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `skill_name` | STRING | Yes | Name of the skill to explain |

## Returns

Document containing:
- Full description
- All parameters with types and defaults
- Return type
- Tags and metadata

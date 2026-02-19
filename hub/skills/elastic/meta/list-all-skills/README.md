# list_all_skills

List all available skills with their descriptions.

## Usage

```sql
-- List all skills
RUN SKILL list_all_skills();

-- Filter by category
RUN SKILL list_all_skills(category => 'observability');

-- Include parameter details
RUN SKILL list_all_skills(include_details => TRUE);
```

## Parameters

| Name | Type | Default | Description |
|------|------|---------|-------------|
| `category` | STRING | NULL | Filter by category |
| `include_details` | BOOLEAN | FALSE | Include parameter info |

## Returns

Array of skill objects with:
- `name`: Skill name
- `description`: What the skill does
- `tags`: Categorization tags

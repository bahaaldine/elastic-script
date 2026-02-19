# search_skills

Search for skills by keyword or capability.

## Usage

```sql
-- Search for log-related skills
RUN SKILL search_skills(query => 'logs');

-- Search for security skills
RUN SKILL search_skills(query => 'security alerts');

-- Search for error handling
RUN SKILL search_skills(query => 'errors');
```

## Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `query` | STRING | Yes | Search query describing what you want to do |

## Returns

Array of matching skills with name, description, and tags.

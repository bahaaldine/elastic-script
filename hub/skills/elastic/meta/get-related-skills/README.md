# get_related_skills

Find skills related to a given skill.

## Usage

```sql
-- Find skills related to log analysis
RUN SKILL get_related_skills(skill_name => 'count_logs_by_level');

-- Find skills related to security monitoring
RUN SKILL get_related_skills(skill_name => 'get_security_alerts');
```

## Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `skill_name` | STRING | Yes | Name of the skill to find related skills for |

## Returns

Array of related skills with shared tags or complementary functionality.

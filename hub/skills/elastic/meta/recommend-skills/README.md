# recommend_skills

Get skill recommendations based on context or goal.

## Usage

```sql
-- Get recommendations for incident investigation
RUN SKILL recommend_skills(goal => 'investigate production incident');

-- Get recommendations for security analysis
RUN SKILL recommend_skills(goal => 'analyze security threats');

-- Get recommendations for data exploration
RUN SKILL recommend_skills(goal => 'understand user behavior patterns');
```

## Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `goal` | STRING | Yes | Describe what you're trying to achieve |

## Returns

Array of recommended skills prioritized by relevance.

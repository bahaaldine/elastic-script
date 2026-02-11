# Your First Skill

This tutorial walks you through creating, testing, and running your first Moltler skill.

## Prerequisites

Make sure you have Moltler running:

```bash
./scripts/quick-start.sh
```

You should have:
- Elasticsearch at `http://localhost:9200`
- The Moltler CLI available

---

## Step 1: Create a Simple Skill

Let's create a skill that checks the health of an Elasticsearch cluster:

```sql
CREATE SKILL check_cluster_health
VERSION '1.0.0'
DESCRIPTION 'Checks Elasticsearch cluster health and returns status'
RETURNS DOCUMENT
BEGIN
    -- Get cluster health
    DECLARE health DOCUMENT = ESQL_QUERY('SHOW INFO');
    
    -- Build response
    RETURN {
        "status": "healthy",
        "cluster": health.cluster_name,
        "checked_at": CURRENT_TIMESTAMP()
    };
END SKILL;
```

Run this in the CLI:

```bash
moltler> CREATE SKILL check_cluster_health VERSION '1.0.0' ...
✓ Skill 'check_cluster_health' version 1.0.0 created
```

---

## Step 2: Run the Skill

Execute your skill:

```sql
CALL check_cluster_health();
```

Output:

```json
{
  "status": "healthy",
  "cluster": "elasticsearch",
  "checked_at": "2026-01-22T10:30:00Z"
}
```

---

## Step 3: Add Parameters

Let's make the skill more useful by adding parameters:

```sql
CREATE SKILL check_index_health
VERSION '1.0.0'
DESCRIPTION 'Checks health of a specific index'
PARAMETERS (
    index_name STRING,
    include_stats BOOLEAN DEFAULT FALSE
)
RETURNS DOCUMENT
BEGIN
    -- Check if index exists
    DECLARE result DOCUMENT;
    
    IF include_stats THEN
        DECLARE stats CURSOR FOR
            FROM index_name
            | STATS 
                doc_count = COUNT(*),
                earliest = MIN(@timestamp),
                latest = MAX(@timestamp);
        
        OPEN stats;
        FETCH stats INTO result;
        CLOSE stats;
        
        SET result.index = index_name;
        SET result.status = 'healthy';
    ELSE
        SET result = {
            "index": index_name,
            "status": "healthy"
        };
    END IF;
    
    RETURN result;
END SKILL;
```

Now call it with parameters:

```sql
-- Basic check
CALL check_index_health('logs-*');

-- With stats
CALL check_index_health('logs-*', TRUE);
```

---

## Step 4: Add Error Handling

Make your skill robust with error handling:

```sql
CREATE SKILL check_index_health
VERSION '1.1.0'  -- Bumped version
DESCRIPTION 'Checks health of a specific index with error handling'
PARAMETERS (
    index_name STRING,
    include_stats BOOLEAN DEFAULT FALSE
)
RETURNS DOCUMENT
BEGIN
    DECLARE result DOCUMENT;
    
    TRY
        IF include_stats THEN
            DECLARE stats CURSOR FOR
                FROM index_name
                | STATS 
                    doc_count = COUNT(*),
                    earliest = MIN(@timestamp),
                    latest = MAX(@timestamp);
            
            OPEN stats;
            FETCH stats INTO result;
            CLOSE stats;
            
            SET result.index = index_name;
            SET result.status = 'healthy';
        ELSE
            SET result = {
                "index": index_name,
                "status": "healthy"
            };
        END IF;
    CATCH
        SET result = {
            "index": index_name,
            "status": "error",
            "error": ERROR_MESSAGE()
        };
    END TRY;
    
    RETURN result;
END SKILL;
```

---

## Step 5: Test Your Skill

Add tests to verify your skill works correctly:

```sql
-- Test with a known index
TEST SKILL check_index_health
WITH index_name = 'logs-sample', include_stats = TRUE
EXPECT status = 'healthy';

-- Test with non-existent index
TEST SKILL check_index_health
WITH index_name = 'nonexistent-index-xyz'
EXPECT status = 'error';
```

Run all tests:

```sql
RUN TESTS FOR SKILL check_index_health;
```

Output:

```
✓ Test 1: PASSED (status = 'healthy')
✓ Test 2: PASSED (status = 'error')

2/2 tests passed
```

---

## Step 6: View Your Skills

List all skills:

```sql
SHOW SKILLS;
```

Output:

```
┌─────────────────────┬─────────┬─────────────────────────────────────┐
│ Name                │ Version │ Description                         │
├─────────────────────┼─────────┼─────────────────────────────────────┤
│ check_cluster_health│ 1.0.0   │ Checks Elasticsearch cluster health │
│ check_index_health  │ 1.1.0   │ Checks health of a specific index   │
└─────────────────────┴─────────┴─────────────────────────────────────┘
```

Get details about a specific skill:

```sql
SHOW SKILL check_index_health;
```

---

## Step 7: Create a Skill Pack

Bundle related skills together:

```sql
CREATE SKILL PACK health_checks
VERSION '1.0.0'
DESCRIPTION 'Collection of health check skills'
SKILLS [
    check_cluster_health,
    check_index_health
];
```

---

## What's Next?

You've created your first skill! Here are some next steps:

<div class="grid cards" markdown>

-   :material-connection:{ .lg .middle } __Connect Data Sources__

    Query GitHub, Jira, and more.

    [:octicons-arrow-right-24: Connectors](../connectors/overview.md)

-   :material-robot:{ .lg .middle } __Build an Agent__

    Create an autonomous agent that uses your skills.

    [:octicons-arrow-right-24: Agents](../agents/overview.md)

-   :material-package-variant:{ .lg .middle } __Skill Parameters__

    Learn about advanced parameter features.

    [:octicons-arrow-right-24: Skill Parameters](../skills/parameters.md)

-   :material-test-tube:{ .lg .middle } __Testing Skills__

    Write comprehensive tests for your skills.

    [:octicons-arrow-right-24: Testing Guide](../skills/testing.md)

</div>

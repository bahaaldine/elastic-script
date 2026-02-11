# Improve Skills with AI

Use AI to enhance, optimize, and fix existing skills.

## Overview

Moltler can analyze your skills and suggest improvements:

```sql
IMPROVE SKILL my_skill;
```

Output:

```
Analyzing skill: my_skill v1.0.0

Suggestions:
  1. Add error handling for external API calls
  2. Implement retry logic for transient failures
  3. Add logging for debugging
  4. Cache results to reduce API calls

Apply suggestions? [Y/n]
```

---

## Automatic Improvements

### Apply All Suggestions

```sql
IMPROVE SKILL my_skill
APPLY ALL;
```

### Apply Specific Suggestions

```sql
IMPROVE SKILL my_skill
APPLY [1, 3];  -- Apply suggestions 1 and 3
```

### Preview Changes

```sql
IMPROVE SKILL my_skill
PREVIEW;
```

Shows diff:

```diff
  BEGIN
+     TRY
          DECLARE result = HTTP_GET(url);
+         AGENT_LOG('API call succeeded', data => result);
-         RETURN result;
+         RETURN result;
+     CATCH
+         AGENT_LOG('API call failed', level => 'error', error => ERROR_MESSAGE());
+         RAISE;
+     END TRY;
  END SKILL;
```

---

## Improvement Types

### Error Handling

```sql
IMPROVE SKILL my_skill
ADD error_handling;
```

Adds try/catch blocks, graceful degradation, meaningful error messages.

### Performance

```sql
IMPROVE SKILL my_skill
OPTIMIZE performance;
```

Adds caching, pagination, query optimization, parallel execution.

### Logging

```sql
IMPROVE SKILL my_skill
ADD logging;
```

Adds structured logging for debugging and observability.

### Tests

```sql
IMPROVE SKILL my_skill
ADD tests;
```

Generates comprehensive test cases.

### Documentation

```sql
IMPROVE SKILL my_skill
ADD documentation;
```

Adds inline comments, parameter descriptions, usage examples.

### Security

```sql
IMPROVE SKILL my_skill
HARDEN security;
```

Adds input validation, secret handling, rate limiting.

---

## Skill Analysis

### Analyze Without Changing

```sql
ANALYZE SKILL my_skill;
```

Output:

```
Skill Analysis: my_skill v1.0.0

Code Quality:
  ✓ Syntax valid
  ✓ No unused variables
  ⚠ Missing error handling (3 locations)
  ⚠ No logging statements

Performance:
  ✓ Query is efficient
  ⚠ Could benefit from caching
  ⚠ Consider pagination for large results

Security:
  ✓ No hardcoded secrets
  ⚠ Input not validated
  ⚠ Error messages may leak info

Test Coverage:
  ✗ No tests defined
  Recommended: 5 test cases

Documentation:
  ✓ Description provided
  ⚠ Parameters not documented
```

### Compare Versions

```sql
COMPARE SKILL my_skill
VERSION '1.0.0' WITH '2.0.0';
```

---

## Fix Issues

### Auto-Fix

```sql
FIX SKILL my_skill;
```

Automatically fixes:

- Syntax errors
- Type mismatches
- Undefined variables
- Missing returns

### Fix Specific Issue

```sql
FIX SKILL my_skill
ISSUE 'timeout_handling';
```

### Fix Based on Errors

```sql
-- Skill failed with error
FIX SKILL my_skill
BASED ON ERROR 'Connection timeout after 30s';
```

AI suggests:

```sql
-- Add timeout handling
DECLARE result = HTTP_GET(url, timeout => 60);

-- Or add retry logic
DECLARE attempts = 0;
WHILE attempts < 3 LOOP
    TRY
        RETURN HTTP_GET(url);
    CATCH timeout_error THEN
        SET attempts = attempts + 1;
        WAIT INTERVAL '2s';
    END TRY;
END LOOP;
```

---

## Refactoring

### Extract Common Logic

```sql
REFACTOR SKILL my_skill
EXTRACT FUNCTION 'validate_input';
```

### Split Skill

```sql
REFACTOR SKILL large_skill
SPLIT INTO [
    'fetch_data',
    'process_data',
    'store_results'
];
```

### Merge Skills

```sql
REFACTOR SKILLS [skill_a, skill_b]
MERGE INTO 'combined_skill';
```

---

## Learning from Feedback

### Improve Based on Usage

```sql
IMPROVE SKILL my_skill
BASED ON USAGE;
```

Analyzes:

- Common parameter values
- Frequent errors
- Performance patterns
- User feedback

### Improve Based on Outcomes

```sql
IMPROVE SKILL my_skill
BASED ON OUTCOMES
WHERE success_rate < 0.9;
```

---

## Batch Improvements

### Improve All Skills

```sql
IMPROVE ALL SKILLS
WHERE last_updated < NOW() - INTERVAL '90d';
```

### Improve Skill Pack

```sql
IMPROVE SKILL PACK observability_tools;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-lightbulb:{ .lg .middle } __Recommendations__

    Get proactive suggestions.

    [:octicons-arrow-right-24: Recommendations](recommendations.md)

</div>

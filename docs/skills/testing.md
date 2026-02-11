# Testing Skills

Moltler provides a built-in testing framework for skills. Write tests to ensure your skills work correctly and catch regressions.

## Quick Start

```sql
-- Define a test
TEST SKILL my_skill
WITH param1 = 'value', param2 = 10
EXPECT status = 'success';

-- Run tests
RUN TESTS FOR SKILL my_skill;
```

---

## Test Syntax

### Basic Test

```sql
TEST SKILL skill_name
[NAME 'test_name']
WITH parameter1 = value1, parameter2 = value2
EXPECT expression;
```

### Complete Example

```sql
-- Create the skill
CREATE SKILL calculate_discount
VERSION '1.0.0'
PARAMETERS (
    price NUMBER,
    discount_percent NUMBER
)
RETURNS NUMBER
BEGIN
    RETURN price * (1 - discount_percent / 100);
END SKILL;

-- Test: normal case
TEST SKILL calculate_discount
NAME 'applies 10% discount correctly'
WITH price = 100, discount_percent = 10
EXPECT @result = 90;

-- Test: zero discount
TEST SKILL calculate_discount
NAME 'zero discount returns original price'
WITH price = 50, discount_percent = 0
EXPECT @result = 50;

-- Test: 100% discount
TEST SKILL calculate_discount
NAME 'full discount returns zero'
WITH price = 100, discount_percent = 100
EXPECT @result = 0;
```

---

## Assertions

### Equality

```sql
EXPECT @result = 'success';
EXPECT @result.status = 'ok';
EXPECT @result.count = 10;
```

### Comparison

```sql
EXPECT @result.count > 0;
EXPECT @result.percentage >= 0.95;
EXPECT @result.errors < 5;
EXPECT @result.latency_ms <= 1000;
```

### Boolean

```sql
EXPECT @result.enabled = TRUE;
EXPECT @result.has_errors = FALSE;
EXPECT @result IS NOT NULL;
```

### String Patterns

```sql
EXPECT @result.message LIKE '%success%';
EXPECT @result.email MATCHES '^.+@.+$';
EXPECT @result.status IN ('ok', 'success', 'completed');
```

### Array Assertions

```sql
EXPECT ARRAY_LENGTH(@result.items) > 0;
EXPECT ARRAY_CONTAINS(@result.tags, 'important');
EXPECT @result.items[0] = 'first';
```

### Document Assertions

```sql
EXPECT DOCUMENT_CONTAINS(@result, 'status');
EXPECT @result.metadata.version = '1.0.0';
```

### Multiple Assertions

```sql
TEST SKILL my_skill
WITH input = 'test'
EXPECT @result.status = 'success'
   AND @result.count > 0
   AND @result.message LIKE '%completed%';
```

---

## Test Organization

### Test Names

Give tests descriptive names:

```sql
TEST SKILL check_health
NAME 'returns healthy when all services up'
WITH services = ['api', 'db', 'cache']
EXPECT @result.status = 'healthy';

TEST SKILL check_health
NAME 'returns degraded when one service down'
WITH services = ['api', 'db-down', 'cache']
EXPECT @result.status = 'degraded';

TEST SKILL check_health
NAME 'returns critical when all services down'
WITH services = ['api-down', 'db-down', 'cache-down']
EXPECT @result.status = 'critical';
```

### Test Tags

Categorize tests:

```sql
TEST SKILL my_skill
NAME 'basic functionality'
TAGS ['unit', 'fast']
WITH input = 'test'
EXPECT @result = 'TEST';

TEST SKILL my_skill
NAME 'integration with external service'
TAGS ['integration', 'slow']
WITH input = 'test'
EXPECT @result IS NOT NULL;
```

### Test Suites

Group related tests:

```sql
CREATE TEST SUITE health_checks
DESCRIPTION 'Tests for all health check skills'
TESTS [
    test_cluster_health_basic,
    test_cluster_health_degraded,
    test_index_health_basic,
    test_node_health_basic
];
```

---

## Running Tests

### Run All Tests for a Skill

```sql
RUN TESTS FOR SKILL my_skill;
```

Output:

```
Running tests for skill: my_skill

✓ basic functionality (12ms)
✓ handles empty input (8ms)
✓ handles large input (45ms)
✗ handles special characters (15ms)
  Expected: status = 'success'
  Actual:   status = 'error'
  Error: Invalid character in input

3/4 tests passed
1 test failed

Test run completed in 80ms
```

### Run Specific Tests

```sql
-- By name
RUN TEST 'basic functionality' FOR SKILL my_skill;

-- By tag
RUN TESTS FOR SKILL my_skill WHERE tags CONTAINS 'unit';
```

### Run All Tests

```sql
RUN ALL TESTS;
```

### Run Test Suite

```sql
RUN TEST SUITE health_checks;
```

---

## Test Fixtures

### Setup and Teardown

```sql
CREATE TEST FIXTURE test_data
SETUP
BEGIN
    -- Create test data
    INDEX_DOCUMENT('test-index', {
        "id": "test-1",
        "value": 100
    });
    INDEX_DOCUMENT('test-index', {
        "id": "test-2", 
        "value": 200
    });
    REFRESH_INDEX('test-index');
END SETUP
TEARDOWN
BEGIN
    -- Clean up
    DELETE_INDEX('test-index');
END TEARDOWN;

-- Use fixture
TEST SKILL analyze_data
FIXTURE test_data
WITH index = 'test-index'
EXPECT @result.count = 2;
```

### Shared Test Data

```sql
CREATE TEST DATA sample_logs AS
[
    {"level": "ERROR", "message": "Connection failed"},
    {"level": "WARN", "message": "Slow query detected"},
    {"level": "INFO", "message": "Request completed"}
];

TEST SKILL count_errors
WITH logs = TEST_DATA(sample_logs)
EXPECT @result = 1;
```

---

## Mocking

### Mock External Services

```sql
TEST SKILL notify_slack
MOCK SLACK_SEND AS
BEGIN
    -- Record the call instead of sending
    RETURN {"ok": true, "ts": "123.456"};
END MOCK
WITH channel = '#alerts', message = 'Test'
EXPECT @result.ok = TRUE;
```

### Mock Functions

```sql
TEST SKILL analyze_with_ai
MOCK LLM_COMPLETE AS
BEGIN
    RETURN 'Mocked AI response for testing';
END MOCK
WITH data = 'test data'
EXPECT @result LIKE '%Mocked%';
```

### Verify Mock Calls

```sql
TEST SKILL alert_workflow
MOCK SLACK_SEND AS mock_slack
MOCK PAGERDUTY_TRIGGER AS mock_pagerduty
WITH severity = 'critical'
EXPECT MOCK_CALLED(mock_slack, 1)
   AND MOCK_CALLED(mock_pagerduty, 1);
```

---

## Error Testing

### Expect Errors

```sql
TEST SKILL divide
NAME 'throws on division by zero'
WITH numerator = 10, denominator = 0
EXPECT ERROR;

TEST SKILL divide
NAME 'throws specific error message'
WITH numerator = 10, denominator = 0
EXPECT ERROR LIKE '%division by zero%';
```

### Error Types

```sql
TEST SKILL connect_db
NAME 'throws connection error'
WITH host = 'invalid-host'
EXPECT ERROR TYPE connection_error;
```

---

## Test Coverage

### View Coverage

```sql
SHOW COVERAGE FOR SKILL my_skill;
```

Output:

```
Coverage report for skill: my_skill (v1.0.0)

Lines:          45/50 (90%)
Branches:       12/15 (80%)
Parameters:     5/5 (100%)
Error paths:    3/4 (75%)

Uncovered lines: 23, 24, 45, 46, 47
Uncovered branches:
  - Line 15: ELSE branch not tested
  - Line 28: error case not tested
  - Line 35: empty array case not tested
```

### Coverage Requirements

```sql
-- Set minimum coverage
SET SKILL my_skill REQUIRE COVERAGE >= 80%;

-- Check before publish
PUBLISH SKILL my_skill
REQUIRE COVERAGE >= 90%;
```

---

## Continuous Testing

### Watch Mode

```sql
-- Re-run tests on skill changes
WATCH TESTS FOR SKILL my_skill;
```

### Test on Save

Configure in CLI:

```bash
moltler config set test_on_save true
```

---

## Best Practices

### 1. Test Happy Path First

```sql
-- Start with the basic, expected use case
TEST SKILL my_skill
NAME 'basic usage works'
WITH input = 'normal value'
EXPECT @result.status = 'success';
```

### 2. Test Edge Cases

```sql
-- Empty inputs
TEST SKILL my_skill
NAME 'handles empty string'
WITH input = ''
EXPECT @result.status = 'error';

-- Boundary values
TEST SKILL my_skill
NAME 'handles max value'
WITH count = 2147483647
EXPECT @result IS NOT NULL;

-- Null values
TEST SKILL my_skill
NAME 'handles null'
WITH optional_param = NULL
EXPECT @result.status = 'success';
```

### 3. Test Error Conditions

```sql
-- Invalid input
TEST SKILL my_skill
NAME 'rejects invalid input'
WITH input = 'invalid!@#$'
EXPECT ERROR;

-- Missing dependencies
TEST SKILL my_skill
NAME 'handles missing service gracefully'
MOCK EXTERNAL_SERVICE AS RETURN NULL
WITH service = 'unavailable'
EXPECT @result.status = 'degraded';
```

### 4. Keep Tests Fast

```sql
-- Prefer mocks for slow operations
TEST SKILL report_generator
MOCK ESQL_QUERY AS RETURN [{"count": 100}]
TAGS ['unit', 'fast']
WITH index = 'logs-*'
EXPECT @result IS NOT NULL;

-- Mark slow tests
TEST SKILL full_integration
TAGS ['integration', 'slow']
WITH real_data = TRUE
EXPECT @result.success = TRUE;
```

### 5. Use Descriptive Names

```sql
-- Good: describes behavior
TEST SKILL my_skill
NAME 'returns empty array when no matches found'
...

-- Bad: vague
TEST SKILL my_skill
NAME 'test 1'
...
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-publish:{ .lg .middle } __Publishing Skills__

    Share your tested skills.

    [:octicons-arrow-right-24: Publishing](publishing.md)

-   :material-package-variant-closed:{ .lg .middle } __Skill Packs__

    Bundle related skills together.

    [:octicons-arrow-right-24: Skill Packs](skill-packs.md)

</div>

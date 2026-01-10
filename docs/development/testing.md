# Testing

Comprehensive testing guide for elastic-script.

## Test Types

| Type | Location | Purpose |
|------|----------|---------|
| Unit Tests | `src/test/java/` | Test individual components |
| E2E Tests | `tests/e2e/` | Test full user workflows |
| Notebooks | `notebooks/` | Interactive testing |

---

## Unit Tests

### Running Unit Tests

```bash
# Run all unit tests
cd elasticsearch
./gradlew :elastic-script:test

# Run specific test class
./gradlew :elastic-script:test --tests "LanguagePrimitivesTests"

# Run with verbose output
./gradlew :elastic-script:test --info
```

### Test Categories

#### Language Primitives Tests

Tests for basic language features:

```java
// LanguagePrimitivesTests.java

@Test
public void testBooleanCaseInsensitivity() {
    assertParseSuccess("DECLARE x BOOLEAN = TRUE;");
    assertParseSuccess("DECLARE y BOOLEAN = true;");
    assertParseSuccess("DECLARE z BOOLEAN = True;");
}

@Test
public void testArrayLiterals() {
    assertParseSuccess("DECLARE arr ARRAY = [1, 2, 3];");
    assertParseSuccess("DECLARE arr ARRAY = ['a', 'b', 'c'];");
}
```

#### Syntax Parsing Tests

Tests for grammar correctness:

```java
// AsyncExecutionSyntaxTests.java

@Test
public void testPipeDrivenSyntax() {
    assertParseSuccess("""
        CREATE PROCEDURE test()
        BEGIN
            analyze_logs() | ON_DONE process_results(@result);
        END PROCEDURE
        """);
}
```

#### Handler Tests

Tests for statement execution:

```java
// PrintStatementHandlerTests.java

@Test
public void testPrintStatement() {
    ExecutionContext ctx = new ExecutionContext();
    handler.handle(parseTree, ctx, executor);
    
    assertTrue(ctx.getPrintOutput().contains("Hello World"));
}
```

---

## End-to-End Tests

E2E tests execute Jupyter notebooks to validate complete workflows.

### Setup

```bash
cd tests/e2e
pip install -r requirements.txt
```

### Running E2E Tests

```bash
# Run all notebook tests
python run_notebook_tests.py

# Run specific notebook
python run_notebook_tests.py --notebook 01

# Verbose output
python run_notebook_tests.py --verbose

# List available notebooks
python run_notebook_tests.py --list
```

### Test Results

```
📓 Running 6 notebook(s)...
============================================================

▶️  00-complete-reference.ipynb
   ✅ PASSED (4.3s)

▶️  01-getting-started.ipynb
   ✅ PASSED (1.0s)

▶️  02-esql-integration.ipynb
   ✅ PASSED (2.1s)

⏭️  03-ai-observability.ipynb - SKIPPED
   Reason: Requires OpenAI API key

▶️  04-async-execution.ipynb
   ✅ PASSED (1.5s)

▶️  05-runbook-integrations.ipynb
   ✅ PASSED (1.0s)

============================================================
📊 TEST SUMMARY
============================================================
Total: 6 passed, 0 failed (10.1s)
✅ All tests passed!
```

### Skip Configuration

Some tests may need to be skipped due to external dependencies:

```json
// tests/e2e/skip_cells.json
{
  "03-ai-observability.ipynb": {
    "skip_all": true,
    "reason": "Requires OpenAI API key and Elasticsearch license"
  },
  "04-async-execution.ipynb": {
    "skip_cells": [7],
    "reason": "STATUS storage bug - known issue"
  }
}
```

---

## Writing Tests

### Unit Test Template

```java
package org.elasticsearch.xpack.escript.myfeature;

import org.elasticsearch.test.ESTestCase;
import org.junit.Before;
import org.junit.Test;

public class MyFeatureTests extends ESTestCase {
    
    private MyFeature feature;
    
    @Before
    public void setUp() {
        super.setUp();
        feature = new MyFeature();
    }
    
    @Test
    public void testBasicFunctionality() {
        // Arrange
        String input = "test input";
        
        // Act
        String result = feature.process(input);
        
        // Assert
        assertEquals("expected output", result);
    }
    
    @Test
    public void testErrorHandling() {
        assertThrows(IllegalArgumentException.class, () -> {
            feature.process(null);
        });
    }
}
```

### Testing Async Code

```java
@Test
public void testAsyncExecution() throws Exception {
    CompletableFuture<Object> future = executor.executeProcedure(
        "my_procedure", 
        List.of("arg1", "arg2")
    );
    
    Object result = future.get(10, TimeUnit.SECONDS);
    assertEquals("expected", result);
}
```

### Testing with Elasticsearch

For tests that need Elasticsearch:

```java
public class IntegrationTests extends ESIntegTestCase {
    
    @Test
    public void testWithElasticsearch() {
        // Index a document
        client().index(new IndexRequest("test-index")
            .source("field", "value"))
            .actionGet();
        
        // Execute procedure
        Object result = executeProcedure("CALL my_procedure()");
        
        // Verify
        assertNotNull(result);
    }
}
```

---

## Test Coverage

### Checking Coverage

```bash
./gradlew :elastic-script:test jacocoTestReport

# View report
open elastic-script/build/reports/jacoco/test/html/index.html
```

### Coverage Goals

| Component | Target |
|-----------|--------|
| Handlers | 80%+ |
| Functions | 90%+ |
| Parser | 70%+ |
| Executor | 80%+ |

---

## Continuous Integration

Tests run automatically on:

- Pull request creation
- Push to main branch

### GitHub Actions Workflow

```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          submodules: true
          
      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          
      - name: Run tests
        run: ./gradlew :elastic-script:test
```

---

## Debugging Tests

### Verbose Logging

Enable debug logging in tests:

```java
@Before
public void setUp() {
    // Set log level
    System.setProperty(
        "org.elasticsearch.xpack.escript.logging.level", 
        "DEBUG"
    );
}
```

### Running in IDE

1. Open the project in IntelliJ IDEA
2. Navigate to test class
3. Right-click → Run/Debug

### Test Output

```bash
# Save test output to file
./gradlew :elastic-script:test 2>&1 | tee test-output.log

# Filter for failures
grep -A 20 "FAILED" test-output.log
```

---

## Common Issues

### Test Isolation

Ensure tests don't depend on each other:

```java
@Before
public void setUp() {
    // Fresh context for each test
    context = new ExecutionContext();
}

@After
public void tearDown() {
    // Cleanup
    context = null;
}
```

### Flaky Tests

If tests fail intermittently:

1. Check for timing issues
2. Ensure proper async handling
3. Verify test isolation
4. Check for shared state

### External Dependencies

Mock external services:

```java
@Test
public void testSlackIntegration() {
    // Mock Slack client
    SlackClient mockClient = mock(SlackClient.class);
    when(mockClient.send(any())).thenReturn(true);
    
    // Inject mock
    slackHandler.setClient(mockClient);
    
    // Test
    handler.handle("SLACK_SEND('#test', 'message')");
    
    verify(mockClient).send(any());
}
```

---

## Best Practices

1. **Test one thing** - Each test should verify one behavior
2. **Clear naming** - `testFeature_condition_expectedResult()`
3. **Arrange-Act-Assert** - Structure tests clearly
4. **No hardcoded waits** - Use proper async handling
5. **Clean up resources** - Prevent test pollution
6. **Document edge cases** - Comment unusual test scenarios

# Contributing

Guide for contributing to elastic-script development.

## Getting Started

### Prerequisites

- **Java 21+** - Required for Elasticsearch development
- **Git** - Version control
- **Python 3.9+** - For notebooks and testing

### Clone the Repository

```bash
git clone https://github.com/bahaaldine/elastic-script.git
cd elastic-script

# Initialize Elasticsearch submodule
git submodule init
git submodule update
```

### Quick Start

Use the quick-start script to set up everything:

```bash
./scripts/quick-start.sh
```

This will:

1. ✅ Build the elastic-script plugin
2. ✅ Start Elasticsearch with the plugin
3. ✅ Load sample data
4. ✅ Set up Jupyter notebooks
5. ✅ Launch the notebook server

---

## Development Workflow

### 1. Make Your Changes

Edit files in the `elastic-script/` directory:

```
elastic-script/
├── src/main/java/org/elasticsearch/xpack/escript/
│   ├── parser/           # ANTLR grammar
│   ├── handlers/         # Statement handlers
│   ├── functions/        # Built-in functions
│   ├── executors/        # Execution engine
│   └── actions/          # REST API
└── src/test/java/        # Unit tests
```

### 2. Rebuild the Plugin

After making changes:

```bash
cd elasticsearch
./gradlew :elastic-script:build
```

### 3. Restart Elasticsearch

```bash
./scripts/quick-start.sh --stop
./scripts/quick-start.sh
```

### 4. Test Your Changes

```bash
# Run unit tests
./gradlew :elastic-script:test

# Run E2E tests
cd tests/e2e
python run_notebook_tests.py
```

---

## Adding New Functions

### 1. Choose the Right Category

| Category | Location | Example Functions |
|----------|----------|-------------------|
| String | `primitives/StringBuiltInFunctions.java` | LENGTH, UPPER, TRIM |
| Number | `primitives/NumberBuiltInFunctions.java` | ABS, ROUND, SQRT |
| Array | `primitives/ArrayBuiltInFunctions.java` | ARRAY_LENGTH, ARRAY_APPEND |
| Date | `primitives/DateBuiltInFunctions.java` | CURRENT_DATE, DATE_ADD |
| Document | `primitives/DocumentBuiltInFunctions.java` | DOCUMENT_GET, DOCUMENT_KEYS |
| Elasticsearch | `datasources/ESFunctions.java` | ES_GET, ES_INDEX |
| AI | `ai/OpenAIFunctions.java` | LLM_COMPLETE |
| Integration | `thirdparty/` | SLACK_SEND, K8S_GET_PODS |

### 2. Implement the Function

```java
// In the appropriate *Functions.java file:

public static void register(BuiltInFunctionRegistry registry) {
    registry.register("MY_FUNCTION", (args) -> {
        // Validate arguments
        if (args.size() != 2) {
            throw new IllegalArgumentException(
                "MY_FUNCTION expects 2 arguments"
            );
        }
        
        String input = (String) args.get(0);
        int count = ((Number) args.get(1)).intValue();
        
        // Implement logic
        return input.repeat(count);
    });
}
```

### 3. Add Documentation

Update `docs/functions/` with your new function:

```markdown
### MY_FUNCTION

Repeats a string a specified number of times.

\`\`\`sql
DECLARE result STRING = MY_FUNCTION('hello', 3);
-- Returns: 'hellohellohello'
\`\`\`

**Syntax:** `MY_FUNCTION(string, count)`
```

### 4. Add Tests

```java
// In src/test/java/.../MyFunctionTests.java

@Test
public void testMyFunction() {
    String result = executeProcedure("""
        CREATE PROCEDURE test()
        BEGIN
            RETURN MY_FUNCTION('hi', 2);
        END PROCEDURE;
        CALL test()
        """);
    assertEquals("hihi", result);
}
```

---

## Adding New Statement Types

### 1. Update the Grammar

Edit `ElasticScript.g4`:

```antlr
statement
    : declareStatement
    | setStatement
    | myNewStatement    // Add your statement type
    // ...
    ;

myNewStatement
    : MY_KEYWORD expression ';'
    ;

// Add lexer token
MY_KEYWORD: 'MY_KEYWORD';
```

### 2. Regenerate Parser

```bash
cd elastic-script
./gradlew :regen
```

### 3. Create Handler

```java
// handlers/MyNewStatementHandler.java

public class MyNewStatementHandler {
    
    public CompletableFuture<Object> handle(
        MyNewStatementContext ctx,
        ExecutionContext context,
        ProcedureExecutor executor
    ) {
        // Implementation
        return CompletableFuture.completedFuture(result);
    }
}
```

### 4. Register in Executor

```java
// In ProcedureExecutor.java

if (statement instanceof MyNewStatementContext) {
    return myNewStatementHandler.handle(
        (MyNewStatementContext) statement,
        context,
        this
    );
}
```

---

## Code Style

### Java

- Follow Elasticsearch coding conventions
- Use descriptive variable names
- Add Javadoc for public methods
- Handle errors with appropriate exceptions

```java
/**
 * Executes a procedure by name.
 *
 * @param name the procedure name
 * @param args the procedure arguments
 * @return the procedure result
 * @throws ProcedureNotFoundException if procedure doesn't exist
 */
public CompletableFuture<Object> callProcedure(
    String name, 
    List<Object> args
) {
    // Implementation
}
```

### Grammar

- Use UPPER_CASE for keywords
- Use camelCase for rule names
- Add comments for complex rules

---

## Pull Request Process

1. **Fork** the repository
2. **Create a branch** for your feature/fix
3. **Write tests** for your changes
4. **Update documentation** as needed
5. **Submit a PR** with a clear description

### PR Checklist

- [ ] Unit tests pass (`./gradlew :elastic-script:test`)
- [ ] E2E tests pass (`python tests/e2e/run_notebook_tests.py`)
- [ ] Documentation updated
- [ ] Code follows style guidelines
- [ ] Commit messages are clear

---

## Getting Help

- **Issues**: Open a GitHub issue for bugs or features
- **Discussions**: Use GitHub Discussions for questions
- **Documentation**: Check the docs at [elastic-script docs](https://bahaaldine.github.io/elastic-script/)

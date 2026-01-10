# Architecture

Technical overview of elastic-script internals.

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Elasticsearch                             │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    elastic-script Plugin                     ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ ││
│  │  │   Parser    │  │  Executor   │  │   Function Registry │ ││
│  │  │  (ANTLR4)   │──│             │──│   (106 functions)   │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘ ││
│  │         │                │                    │             ││
│  │         ▼                ▼                    ▼             ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ ││
│  │  │    AST      │  │  Handlers   │  │  External Services  │ ││
│  │  │             │  │             │  │  (OpenAI, Slack...) │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘ ││
│  └─────────────────────────────────────────────────────────────┘│
│                              │                                   │
│  ┌───────────────────────────┼───────────────────────────────┐  │
│  │          Elasticsearch Internal APIs                       │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │  │
│  │  │  ES|QL   │  │  Search  │  │  Index   │  │ Inference │  │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Parser (ANTLR4)

The parser converts elastic-script source code into an Abstract Syntax Tree (AST).

**Grammar file:** `elastic-script/src/main/java/org/elasticsearch/xpack/escript/parser/ElasticScript.g4`

```antlr
// Example grammar rules
procedure
    : CREATE PROCEDURE identifier '(' parameterList? ')' 
      BEGIN statementList END PROCEDURE
    ;

statement
    : declareStatement
    | setStatement
    | ifStatement
    | forStatement
    | printStatement
    | returnStatement
    // ... more statement types
    ;
```

**Key classes:**

- `ElasticScriptLexer` - Tokenizes input
- `ElasticScriptParser` - Builds parse tree
- `ElasticScriptVisitor` - Traverses AST

---

### 2. Statement Handlers

Each statement type has a dedicated handler:

| Handler | Responsibility |
|---------|---------------|
| `DeclareStatementHandler` | Variable declarations |
| `SetStatementHandler` | Variable assignments |
| `IfStatementHandler` | Conditional execution |
| `ForStatementHandler` | FOR loops |
| `WhileStatementHandler` | WHILE loops |
| `PrintStatementHandler` | Output statements |
| `ReturnStatementHandler` | Return values |
| `TryCatchHandler` | Exception handling |
| `AsyncProcedureStatementHandler` | Async execution |
| `ParallelStatementHandler` | Parallel execution |

**Location:** `elastic-script/src/main/java/org/elasticsearch/xpack/escript/handlers/`

---

### 3. Function Registry

Built-in functions are organized by category:

```
functions/builtin/
├── primitives/
│   ├── StringBuiltInFunctions.java
│   ├── NumberBuiltInFunctions.java
│   ├── ArrayBuiltInFunctions.java
│   ├── DateBuiltInFunctions.java
│   └── DocumentBuiltInFunctions.java
├── datasources/
│   ├── ESFunctions.java
│   └── EsqlBuiltInFunctions.java
├── ai/
│   ├── OpenAIFunctions.java
│   └── InferenceFunctions.java
└── thirdparty/
    ├── SlackFunctions.java
    ├── PagerDutyFunctions.java
    ├── KubernetesFunctions.java
    └── AWSFunctions.java
```

---

### 4. Execution Context

The `ExecutionContext` maintains state during procedure execution:

```java
public class ExecutionContext {
    private final String executionId;          // Unique execution ID
    private final Map<String, Object> variables;  // Variable storage
    private final List<String> printOutput;    // PRINT output capture
    private final long startTimeMs;            // Execution timing
    
    // Methods
    public void setVariable(String name, Object value);
    public Object getVariable(String name);
    public void addPrintOutput(String message);
}
```

---

### 5. Procedure Executor

The main execution engine that orchestrates statement execution:

```java
public class ProcedureExecutor {
    // Statement handlers
    private final DeclareStatementHandler declareHandler;
    private final SetStatementHandler setHandler;
    private final IfStatementHandler ifHandler;
    // ... more handlers
    
    // Execute a procedure
    public CompletableFuture<Object> executeProcedure(
        String procedureName, 
        List<Object> arguments
    );
    
    // Execute individual statements
    public CompletableFuture<Object> visitStatementAsync(
        ParseTree statement, 
        ExecutionContext context
    );
}
```

---

## Async Execution Model

elastic-script uses Java's `CompletableFuture` for async execution:

```
┌──────────────────────────────────────────────────────────────┐
│                    Async Execution Flow                       │
│                                                               │
│  procedure_a()                                                │
│       │                                                       │
│       ▼                                                       │
│  ┌─────────┐    ON_DONE    ┌─────────┐    FINALLY            │
│  │ Execute │──────────────▶│ handler │──────────────▶ Done   │
│  │         │               │         │                        │
│  └─────────┘               └─────────┘                        │
│       │                                                       │
│       │ ON_FAIL            ┌─────────┐                        │
│       └───────────────────▶│ handler │───────────────▶ Done  │
│                            └─────────┘                        │
└──────────────────────────────────────────────────────────────┘
```

**Key classes:**

- `ExecutionState` - Tracks async execution state
- `ExecutionPipeline` - Manages continuation chain
- `Continuation` - Single step in pipeline
- `ExecutionRegistry` - Persists state to Elasticsearch

---

## REST API

### Endpoint

```
POST /_escript
{
    "query": "CALL my_procedure(arg1, arg2)"
}
```

### Response

```json
{
    "result": "procedure return value",
    "execution_id": "uuid",
    "duration_ms": 150,
    "output": ["PRINT statement 1", "PRINT statement 2"]
}
```

**Handler:** `RestRunEScriptAction.java`

---

## Storage

### Procedure Storage

Procedures are stored in the `.elastic_script_procedures` index:

```json
{
    "name": "my_procedure",
    "source": "CREATE PROCEDURE my_procedure() BEGIN ... END PROCEDURE",
    "parameters": [...],
    "created_at": "2026-01-09T10:00:00Z",
    "updated_at": "2026-01-09T10:00:00Z"
}
```

### Execution State Storage

Async execution state is stored in `.escript_executions`:

```json
{
    "execution_id": "exec-abc123",
    "procedure_name": "my_procedure",
    "status": "RUNNING",
    "progress": {"percentage": 50, "message": "Processing..."},
    "result": null,
    "error": null,
    "created_at": "2026-01-09T10:00:00Z"
}
```

---

## Error Handling

Errors bubble up through the async chain:

```
TRY block
    │
    ├── Success ──▶ Continue execution
    │
    └── Error ──▶ CATCH block
                      │
                      └── FINALLY block (always runs)
```

---

## Thread Safety

- Each execution gets its own `ExecutionContext`
- Variables are scoped to the execution
- No shared mutable state between executions
- Elasticsearch client handles connection pooling

---

## Performance Considerations

1. **Function Registration**: Currently happens on every execution. Optimize to register once at startup.

2. **ESQL Queries**: Use `LIMIT` to avoid processing too much data.

3. **Async Execution**: Long-running procedures should use async execution to avoid timeouts.

4. **Memory**: Large result sets are streamed via cursors to avoid memory issues.

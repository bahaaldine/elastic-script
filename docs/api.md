---
layout: default
title: API Reference
---

# API Reference

## REST Endpoints

### Execute Query

Execute elastic-script queries.

```
POST /_escript
Content-Type: application/json
Authorization: Basic <credentials>
```

**Request:**

```json
{
  "query": "CALL my_procedure()"
}
```

**Response:**

```json
{
  "result": { ... },
  "execution_time_ms": 42,
  "output": ["Line 1 from PRINT", "Line 2 from PRINT"]
}
```

### MCP Endpoint

Model Context Protocol for AI agents.

```
POST /_escript/mcp
Content-Type: application/json
Authorization: Basic <credentials>
```

See [MCP Integration](./mcp.md) for details.

### Get Server Info

```
GET /_escript/mcp
```

**Response:**

```json
{
  "name": "moltler",
  "version": "1.0.0",
  "mcp_version": "2024-11-05",
  "capabilities": ["tools/list", "tools/call"]
}
```

## Query Examples

### List Skills

```sql
SHOW SKILLS;
```

Response:
```json
{
  "result": [
    {"name": "analyze_logs", "version": "1.0", "description": "..."},
    {"name": "check_health", "version": "1.0", "description": "..."}
  ]
}
```

### Get Skill Details

```sql
SHOW SKILL analyze_logs;
```

Response includes full skill definition with parameters, documentation, and source code.

### List Procedures

```sql
SHOW PROCEDURES;
```

### Get Procedure Details

```sql
SHOW PROCEDURE my_procedure;
```

### List Functions

```sql
SHOW FUNCTIONS;
```

### Get Function Details

```sql
SHOW FUNCTION my_function;
```

### Call Procedure

```sql
CALL my_procedure('arg1', 42);
```

### Execute ES|QL

```sql
DECLARE results ARRAY;
SET results = ESQL_QUERY('FROM logs-* | LIMIT 10');
```

## Error Responses

### Syntax Error

```json
{
  "error": {
    "type": "syntax_error",
    "message": "Unexpected token 'FORM' at line 1, column 1. Did you mean 'FROM'?",
    "line": 1,
    "column": 1
  }
}
```

### Execution Error

```json
{
  "error": {
    "type": "execution_error",
    "message": "Procedure 'unknown_proc' not found",
    "procedure": "unknown_proc"
  }
}
```

### Permission Error

```json
{
  "error": {
    "type": "security_exception",
    "message": "User does not have permission to execute procedures"
  }
}
```

## Authentication

All endpoints require HTTP Basic authentication:

```bash
curl -u username:password http://localhost:9200/_escript ...
```

Or via header:

```bash
curl -H "Authorization: Basic $(echo -n 'user:pass' | base64)" http://localhost:9200/_escript ...
```

## Rate Limiting

No built-in rate limiting. Configure via Elasticsearch settings if needed.

## Timeouts

Default query timeout: 30 seconds

Override per-request:

```json
{
  "query": "CALL long_running_procedure()",
  "timeout": "5m"
}
```

## Index Names

elastic-script uses these system indices:

| Index | Purpose |
|-------|---------|
| `.escript_procedures` | Stored procedures |
| `.escript_functions` | User-defined functions |
| `.escript_skills` | Skill definitions |
| `.escript_executions` | Async execution state |
| `.escript_applications` | Application definitions |

These are managed automatically. Direct access is not recommended.

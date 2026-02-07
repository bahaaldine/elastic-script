# Intents Notebook

This notebook demonstrates **INTENT** definitions in elastic-script for encoding SRE expertise with guardrails.

## Topics Covered

1. **Basic Intent Definition** - DEFINE INTENT with DESCRIPTION and ACTIONS
2. **Intent Invocation** - Positional and named arguments
3. **REQUIRES Pre-conditions** - Validation before execution
4. **ON_FAILURE Handling** - Error recovery
5. **Introspection** - ESCRIPT_INTENTS() and ESCRIPT_INTENT()
6. **Real-World Example** - Service health check intent

## Open the Notebook

The notebook is located at `notebooks/18-intents.ipynb`.

### Using Jupyter Lab

```bash
cd elastic-script
jupyter lab notebooks/18-intents.ipynb
```

### Using Classic Jupyter

```bash
cd elastic-script
jupyter notebook notebooks/18-intents.ipynb
```

## Key Concepts

### Intent Definition

```sql
DEFINE INTENT restart_service(service STRING)
DESCRIPTION 'Safely restart a service'
REQUIRES
    LENGTH(service) > 0
ACTIONS
    PRINT 'Restarting: ' || service;
    -- Restart logic here
ON_FAILURE
    PRINT 'Failed: ' || ERROR_MESSAGE;
    -- Cleanup logic here
END INTENT
```

### Intent Invocation

```sql
-- Positional arguments
INTENT restart_service('api-gateway');

-- Named arguments
INTENT restart_service WITH service = 'api-gateway';
```

### Use Cases

- **AI Agent Integration** - Safe interface for LLM-driven operations
- **Runbook Automation** - Encode operational procedures
- **Guardrails** - REQUIRES prevents unsafe operations

## See Also

- [Intents Language Reference](../language/intents.md)
- [Procedures](../language/procedures.md)
- [Error Handling](../language/error-handling.md)

# Skill Parameters

Parameters allow skills to be customized for different use cases. This guide covers all parameter features.

## Basic Syntax

```sql
PARAMETERS (
    param_name TYPE [DEFAULT value] [CHECK constraint]
)
```

---

## Parameter Types

### STRING

Text values of any length:

```sql
PARAMETERS (
    message STRING,
    index_pattern STRING DEFAULT 'logs-*',
    email STRING CHECK MATCHES '^.+@.+\..+$'
)
```

### NUMBER

Integers or decimals:

```sql
PARAMETERS (
    count NUMBER,
    threshold NUMBER DEFAULT 0.95,
    percentage NUMBER CHECK BETWEEN 0 AND 100
)
```

### BOOLEAN

True or false values:

```sql
PARAMETERS (
    enabled BOOLEAN DEFAULT TRUE,
    dry_run BOOLEAN DEFAULT FALSE,
    verbose BOOLEAN
)
```

### DOCUMENT

JSON objects with key-value pairs:

```sql
PARAMETERS (
    config DOCUMENT,
    metadata DOCUMENT DEFAULT {"source": "moltler"},
    filters DOCUMENT DEFAULT {}
)
```

Access document fields:

```sql
BEGIN
    DECLARE timeout = config.timeout ?? 30;
    DECLARE host = config.server?.host ?? 'localhost';
END SKILL;
```

### ARRAY

Lists of values:

```sql
PARAMETERS (
    hosts ARRAY,
    tags ARRAY DEFAULT ['default'],
    thresholds ARRAY DEFAULT [0.5, 0.8, 0.95]
)
```

Access array elements:

```sql
BEGIN
    FOR host IN hosts LOOP
        CALL check_host(host);
    END LOOP;
    
    DECLARE first_threshold = thresholds[0];
END SKILL;
```

### TIMESTAMP

Date/time values:

```sql
PARAMETERS (
    start_time TIMESTAMP,
    end_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    cutoff TIMESTAMP DEFAULT NOW() - INTERVAL '24h'
)
```

---

## Default Values

### Static Defaults

```sql
PARAMETERS (
    limit NUMBER DEFAULT 100,
    format STRING DEFAULT 'json',
    enabled BOOLEAN DEFAULT TRUE
)
```

### Dynamic Defaults

```sql
PARAMETERS (
    -- Current time as default
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    
    -- Environment variable
    api_key STRING DEFAULT ENV('API_KEY'),
    
    -- Expression
    end_time TIMESTAMP DEFAULT NOW(),
    start_time TIMESTAMP DEFAULT NOW() - INTERVAL '1h'
)
```

---

## Validation Constraints

### CHECK IN (Enum)

Limit to specific values:

```sql
PARAMETERS (
    severity STRING CHECK IN ('low', 'medium', 'high', 'critical'),
    format STRING CHECK IN ('json', 'csv', 'yaml'),
    environment STRING CHECK IN ('dev', 'staging', 'prod')
)
```

### CHECK BETWEEN (Range)

Numeric ranges:

```sql
PARAMETERS (
    percentage NUMBER CHECK BETWEEN 0 AND 100,
    port NUMBER CHECK BETWEEN 1 AND 65535,
    timeout_seconds NUMBER CHECK BETWEEN 1 AND 3600
)
```

### CHECK MATCHES (Regex)

Pattern matching:

```sql
PARAMETERS (
    email STRING CHECK MATCHES '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$',
    ip_address STRING CHECK MATCHES '^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$',
    version STRING CHECK MATCHES '^\d+\.\d+\.\d+$'
)
```

### CHECK NOT EMPTY

Require non-empty values:

```sql
PARAMETERS (
    message STRING CHECK NOT EMPTY,
    recipients ARRAY CHECK NOT EMPTY,
    config DOCUMENT CHECK NOT EMPTY
)
```

### CHECK LENGTH

String/array length constraints:

```sql
PARAMETERS (
    name STRING CHECK LENGTH BETWEEN 1 AND 100,
    description STRING CHECK LENGTH <= 500,
    tags ARRAY CHECK LENGTH <= 10
)
```

### Combined Constraints

```sql
PARAMETERS (
    username STRING 
        CHECK NOT EMPTY 
        CHECK LENGTH BETWEEN 3 AND 50
        CHECK MATCHES '^[a-z][a-z0-9_]*$'
)
```

---

## Parameter Modes

### IN (Default)

Read-only input:

```sql
PARAMETERS (
    message STRING  -- Implicitly IN mode
)
```

### OUT

Output parameter:

```sql
PARAMETERS (
    input STRING IN,
    result STRING OUT
)
BEGIN
    SET result = UPPER(input);
END SKILL;

-- Usage
DECLARE output STRING;
CALL my_skill('hello', output);  -- output = 'HELLO'
```

### INOUT

Both input and output:

```sql
PARAMETERS (
    counter NUMBER INOUT
)
BEGIN
    SET counter = counter + 1;
END SKILL;

-- Usage
DECLARE count NUMBER = 5;
CALL my_skill(count);  -- count = 6
```

---

## Optional vs Required

### Required Parameters

Parameters without defaults are required:

```sql
PARAMETERS (
    host STRING,      -- Required
    port NUMBER       -- Required
)
```

### Optional Parameters

Parameters with defaults are optional:

```sql
PARAMETERS (
    host STRING,              -- Required
    port NUMBER DEFAULT 9200, -- Optional
    timeout NUMBER DEFAULT 30 -- Optional
)
```

### Named Arguments

Call with named arguments for clarity:

```sql
-- Positional (order matters)
CALL my_skill('localhost', 9200, 30);

-- Named (order doesn't matter)
CALL my_skill(
    host => 'localhost',
    timeout => 60
);

-- Mixed (positional first, then named)
CALL my_skill('localhost', timeout => 60);
```

---

## Complex Parameter Patterns

### Document Schema

Define expected document structure:

```sql
PARAMETERS (
    config DOCUMENT SCHEMA {
        "host": "STRING",
        "port": "NUMBER",
        "credentials": {
            "username": "STRING",
            "password": "STRING"
        }
    }
)
```

### Array Element Type

Specify array element types:

```sql
PARAMETERS (
    numbers ARRAY OF NUMBER,
    names ARRAY OF STRING,
    records ARRAY OF DOCUMENT
)
```

### Nullable Parameters

Explicitly allow null:

```sql
PARAMETERS (
    optional_value STRING NULLABLE,
    callback STRING NULLABLE DEFAULT NULL
)
```

---

## Best Practices

### 1. Use Descriptive Names

```sql
-- Good
PARAMETERS (
    elasticsearch_host STRING,
    query_timeout_seconds NUMBER,
    include_metadata BOOLEAN
)

-- Bad
PARAMETERS (
    h STRING,
    t NUMBER,
    m BOOLEAN
)
```

### 2. Provide Sensible Defaults

```sql
PARAMETERS (
    -- Good: sensible defaults for common cases
    limit NUMBER DEFAULT 100,
    timeout_seconds NUMBER DEFAULT 30,
    index_pattern STRING DEFAULT 'logs-*',
    
    -- Bad: arbitrary defaults
    limit NUMBER DEFAULT 999999,
    timeout_seconds NUMBER DEFAULT 0
)
```

### 3. Validate Early

```sql
BEGIN
    -- Validate at the start
    IF ARRAY_LENGTH(hosts) = 0 THEN
        RAISE 'hosts cannot be empty';
    END IF;
    
    IF threshold < 0 OR threshold > 1 THEN
        RAISE 'threshold must be between 0 and 1';
    END IF;
    
    -- Main logic after validation
    ...
END SKILL;
```

### 4. Document Parameters

```sql
PARAMETERS (
    -- The Elasticsearch index pattern to query
    -- Examples: 'logs-*', 'metrics-system-*'
    index_pattern STRING DEFAULT 'logs-*',
    
    -- Time range to look back
    -- Format: number + unit (h=hours, d=days, w=weeks)
    lookback STRING DEFAULT '24h',
    
    -- Minimum number of occurrences to report
    min_count NUMBER DEFAULT 5
)
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-test-tube:{ .lg .middle } __Testing Skills__

    Write tests for your parameters.

    [:octicons-arrow-right-24: Testing](testing.md)

-   :material-publish:{ .lg .middle } __Publishing__

    Share your skills.

    [:octicons-arrow-right-24: Publishing](publishing.md)

</div>

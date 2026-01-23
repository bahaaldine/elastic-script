# MAP Functions

MAP functions operate on associative arrays (key-value data structures).

## Overview

The MAP type provides key-value storage with 12 built-in functions for manipulation.

| Function | Description |
|----------|-------------|
| `MAP_GET` | Get value by key |
| `MAP_GET_OR_DEFAULT` | Get value with fallback |
| `MAP_PUT` | Add/update key-value |
| `MAP_REMOVE` | Remove key |
| `MAP_KEYS` | Get all keys |
| `MAP_VALUES` | Get all values |
| `MAP_SIZE` | Count entries |
| `MAP_CONTAINS_KEY` | Check key exists |
| `MAP_CONTAINS_VALUE` | Check value exists |
| `MAP_MERGE` | Combine two maps |
| `MAP_FROM_ARRAYS` | Create from arrays |
| `MAP_ENTRIES` | Convert to array |

## Creating Maps

### MAP Literal

```sql
-- Empty map
DECLARE empty_map MAP = MAP {};

-- Map with initial values
DECLARE config MAP = MAP {
    'host' => 'localhost',
    'port' => 9200,
    'ssl' => true
};

-- Nested maps
DECLARE settings MAP = MAP {
    'database' => MAP { 'host' => 'db.local', 'port' => 5432 },
    'cache' => MAP { 'ttl' => 3600, 'enabled' => true }
};
```

## MAP_GET

Get value by key. Returns `NULL` if key not found.

```sql
MAP_GET(map, key)
```

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `map` | MAP | The map to query |
| `key` | ANY | The key to look up |

**Returns:** Value associated with key, or `NULL`

**Example:**

```sql
DECLARE config MAP = MAP { 'host' => 'localhost', 'port' => 9200 };

DECLARE host STRING = MAP_GET(config, 'host');
PRINT host;  -- 'localhost'

DECLARE missing = MAP_GET(config, 'timeout');
PRINT missing;  -- NULL
```

## MAP_GET_OR_DEFAULT

Get value by key with a default fallback.

```sql
MAP_GET_OR_DEFAULT(map, key, default_value)
```

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `map` | MAP | The map to query |
| `key` | ANY | The key to look up |
| `default_value` | ANY | Value if key not found |

**Returns:** Value for key, or default if not found

**Example:**

```sql
DECLARE config MAP = MAP { 'host' => 'localhost' };

DECLARE timeout NUMBER = MAP_GET_OR_DEFAULT(config, 'timeout', 30);
PRINT timeout;  -- 30 (default)

DECLARE host STRING = MAP_GET_OR_DEFAULT(config, 'host', 'unknown');
PRINT host;  -- 'localhost' (actual value)
```

## MAP_PUT

Return a new map with the key-value pair added or updated.

!!! note "Immutable Operation"
    MAP_PUT returns a **new map** - it does not modify the original.

```sql
MAP_PUT(map, key, value)
```

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `map` | MAP | The base map |
| `key` | ANY | Key to add/update |
| `value` | ANY | Value to set |

**Returns:** New MAP with the key-value pair

**Example:**

```sql
DECLARE original MAP = MAP { 'a' => 1 };

-- Add new key
DECLARE updated MAP = MAP_PUT(original, 'b', 2);
PRINT MAP_SIZE(updated);  -- 2

-- Original unchanged
PRINT MAP_SIZE(original);  -- 1

-- Update existing key
SET updated = MAP_PUT(updated, 'a', 100);
PRINT MAP_GET(updated, 'a');  -- 100
```

## MAP_REMOVE

Return a new map without the specified key.

```sql
MAP_REMOVE(map, key)
```

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `map` | MAP | The base map |
| `key` | ANY | Key to remove |

**Returns:** New MAP without the key

**Example:**

```sql
DECLARE data MAP = MAP { 'keep' => 1, 'remove' => 2, 'also_keep' => 3 };

DECLARE cleaned MAP = MAP_REMOVE(data, 'remove');
PRINT MAP_KEYS(cleaned);  -- ['keep', 'also_keep']
```

## MAP_KEYS

Get all keys as an array.

```sql
MAP_KEYS(map)
```

**Returns:** ARRAY of all keys

**Example:**

```sql
DECLARE scores MAP = MAP { 'alice' => 95, 'bob' => 87, 'charlie' => 92 };

DECLARE names ARRAY = MAP_KEYS(scores);
PRINT ARRAY_JOIN(names, ', ');  -- 'alice, bob, charlie'
```

## MAP_VALUES

Get all values as an array.

```sql
MAP_VALUES(map)
```

**Returns:** ARRAY of all values

**Example:**

```sql
DECLARE scores MAP = MAP { 'alice' => 95, 'bob' => 87, 'charlie' => 92 };

DECLARE all_scores ARRAY = MAP_VALUES(scores);
PRINT ARRAY_JOIN(all_scores, ', ');  -- '95, 87, 92'
```

## MAP_SIZE

Get the number of key-value pairs.

```sql
MAP_SIZE(map)
```

**Returns:** NUMBER count of entries

**Example:**

```sql
DECLARE empty MAP = MAP {};
PRINT MAP_SIZE(empty);  -- 0

DECLARE config MAP = MAP { 'a' => 1, 'b' => 2, 'c' => 3 };
PRINT MAP_SIZE(config);  -- 3
```

## MAP_CONTAINS_KEY

Check if a key exists in the map.

```sql
MAP_CONTAINS_KEY(map, key)
```

**Returns:** BOOLEAN

**Example:**

```sql
DECLARE inventory MAP = MAP { 'apples' => 50, 'oranges' => 30 };

IF MAP_CONTAINS_KEY(inventory, 'apples') THEN
    PRINT 'We have apples!';
END IF;

IF NOT MAP_CONTAINS_KEY(inventory, 'bananas') THEN
    PRINT 'No bananas in stock';
END IF;
```

## MAP_CONTAINS_VALUE

Check if a value exists in the map.

```sql
MAP_CONTAINS_VALUE(map, value)
```

**Returns:** BOOLEAN

**Example:**

```sql
DECLARE inventory MAP = MAP { 'apples' => 50, 'oranges' => 0 };

IF MAP_CONTAINS_VALUE(inventory, 0) THEN
    PRINT 'Warning: Some items are out of stock';
END IF;
```

## MAP_MERGE

Merge two maps. Values from the second map overwrite the first.

```sql
MAP_MERGE(map1, map2)
```

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `map1` | MAP | Base map |
| `map2` | MAP | Map to merge in (takes precedence) |

**Returns:** New merged MAP

**Example:**

```sql
DECLARE defaults MAP = MAP {
    'timeout' => 30,
    'retries' => 3,
    'debug' => false
};

DECLARE overrides MAP = MAP {
    'timeout' => 60,
    'debug' => true
};

DECLARE config MAP = MAP_MERGE(defaults, overrides);

PRINT MAP_GET(config, 'timeout');  -- 60 (overridden)
PRINT MAP_GET(config, 'retries');  -- 3 (from defaults)
PRINT MAP_GET(config, 'debug');    -- true (overridden)
```

## MAP_FROM_ARRAYS

Create a map from two parallel arrays.

```sql
MAP_FROM_ARRAYS(keys, values)
```

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `keys` | ARRAY | Array of keys |
| `values` | ARRAY | Array of values (same length) |

**Returns:** New MAP

**Example:**

```sql
DECLARE names ARRAY = ['alice', 'bob', 'charlie'];
DECLARE ages ARRAY = [25, 30, 35];

DECLARE age_map MAP = MAP_FROM_ARRAYS(names, ages);

PRINT MAP_GET(age_map, 'alice');  -- 25
PRINT MAP_GET(age_map, 'bob');    -- 30
```

!!! warning "Array Length"
    Both arrays must have the same length or an error is thrown.

## MAP_ENTRIES

Convert map to an array of `{key, value}` documents.

```sql
MAP_ENTRIES(map)
```

**Returns:** ARRAY of DOCUMENTs with `key` and `value` fields

**Example:**

```sql
DECLARE data MAP = MAP { 'x' => 10, 'y' => 20 };

DECLARE entries ARRAY = MAP_ENTRIES(data);

FOR i IN 1..ARRAY_LENGTH(entries) LOOP
    DECLARE entry DOCUMENT = entries[i];
    PRINT 'Key: ' || entry['key'] || ', Value: ' || entry['value'];
END LOOP;
-- Output:
-- Key: x, Value: 10
-- Key: y, Value: 20
```

## Common Patterns

### Counting Occurrences

```sql
CREATE PROCEDURE word_count(words ARRAY)
BEGIN
    DECLARE counts MAP = MAP {};
    
    FOR i IN 1..ARRAY_LENGTH(words) LOOP
        DECLARE word STRING = words[i];
        DECLARE current NUMBER = MAP_GET_OR_DEFAULT(counts, word, 0);
        SET counts = MAP_PUT(counts, word, current + 1);
    END LOOP;
    
    RETURN counts;
END PROCEDURE;

-- Usage
DECLARE result MAP = CALL word_count(['a', 'b', 'a', 'c', 'a']);
PRINT MAP_GET(result, 'a');  -- 3
```

### Configuration with Defaults

```sql
CREATE PROCEDURE get_config(overrides MAP)
BEGIN
    DECLARE defaults MAP = MAP {
        'host' => 'localhost',
        'port' => 9200,
        'timeout' => 30,
        'ssl' => false
    };
    
    RETURN MAP_MERGE(defaults, overrides);
END PROCEDURE;

-- Usage
DECLARE config MAP = CALL get_config(MAP { 'ssl' => true });
```

### Iterating Over Map

```sql
DECLARE config MAP = MAP { 'a' => 1, 'b' => 2, 'c' => 3 };

-- Iterate using keys
DECLARE keys ARRAY = MAP_KEYS(config);
FOR i IN 1..ARRAY_LENGTH(keys) LOOP
    DECLARE k STRING = keys[i];
    PRINT k || ' = ' || MAP_GET(config, k);
END LOOP;

-- Or use entries
FOR i IN 1..ARRAY_LENGTH(MAP_ENTRIES(config)) LOOP
    DECLARE entry DOCUMENT = MAP_ENTRIES(config)[i];
    PRINT entry['key'] || ' = ' || entry['value'];
END LOOP;
```

## See Also

- [Array Functions](array.md) - List operations
- [Document Functions](document.md) - JSON object operations
- [Variables & Types](../language/variables-types.md) - Type system

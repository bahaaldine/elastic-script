# Array Functions

Functions for working with arrays and collections.

## Function Reference

### ARRAY_LENGTH

Returns the number of elements in an array.

```sql
DECLARE arr ARRAY = [1, 2, 3, 4, 5];
DECLARE len NUMBER = ARRAY_LENGTH(arr);
-- Returns: 5
```

**Syntax:** `ARRAY_LENGTH(array)`

---

### ARRAY_APPEND

Adds an element to the end of an array.

```sql
DECLARE arr ARRAY = [1, 2, 3];
DECLARE result ARRAY = ARRAY_APPEND(arr, 4);
-- Returns: [1, 2, 3, 4]
```

**Syntax:** `ARRAY_APPEND(array, element)`

---

### ARRAY_PREPEND

Adds an element to the beginning of an array.

```sql
DECLARE arr ARRAY = [2, 3, 4];
DECLARE result ARRAY = ARRAY_PREPEND(arr, 1);
-- Returns: [1, 2, 3, 4]
```

**Syntax:** `ARRAY_PREPEND(array, element)`

---

### ARRAY_REMOVE

Removes all occurrences of an element from an array.

```sql
DECLARE arr ARRAY = [1, 2, 3, 2, 4];
DECLARE result ARRAY = ARRAY_REMOVE(arr, 2);
-- Returns: [1, 3, 4]
```

**Syntax:** `ARRAY_REMOVE(array, element)`

---

### ARRAY_CONTAINS

Checks if an array contains an element.

```sql
DECLARE arr ARRAY = ['apple', 'banana', 'cherry'];
DECLARE has_banana BOOLEAN = ARRAY_CONTAINS(arr, 'banana');
-- Returns: true
```

**Syntax:** `ARRAY_CONTAINS(array, element)`

**Returns:** BOOLEAN

---

### ARRAY_REVERSE

Reverses the order of elements.

```sql
DECLARE arr ARRAY = [1, 2, 3];
DECLARE result ARRAY = ARRAY_REVERSE(arr);
-- Returns: [3, 2, 1]
```

**Syntax:** `ARRAY_REVERSE(array)`

---

### ARRAY_SLICE

Extracts a portion of an array.

```sql
DECLARE arr ARRAY = [1, 2, 3, 4, 5];
DECLARE result ARRAY = ARRAY_SLICE(arr, 1, 3);
-- Returns: [2, 3, 4]
```

**Syntax:** `ARRAY_SLICE(array, start, end)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| array | ARRAY | Input array |
| start | NUMBER | Start index (0-based, inclusive) |
| end | NUMBER | End index (exclusive) |

---

### ARRAY_DISTINCT

Removes duplicate elements.

```sql
DECLARE arr ARRAY = [1, 2, 2, 3, 3, 3];
DECLARE result ARRAY = ARRAY_DISTINCT(arr);
-- Returns: [1, 2, 3]
```

**Syntax:** `ARRAY_DISTINCT(array)`

---

### ARRAY_JOIN

Joins array elements into a string.

```sql
DECLARE words ARRAY = ['hello', 'world'];
DECLARE result STRING = ARRAY_JOIN(words, ', ');
-- Returns: 'hello, world'
```

**Syntax:** `ARRAY_JOIN(array, delimiter)`

---

### ARRAY_SORT

Sorts array elements.

```sql
DECLARE arr ARRAY = [3, 1, 4, 1, 5, 9];
DECLARE result ARRAY = ARRAY_SORT(arr);
-- Returns: [1, 1, 3, 4, 5, 9]
```

**Syntax:** `ARRAY_SORT(array)`

---

### ARRAY_FIRST

Returns the first element.

```sql
DECLARE arr ARRAY = [10, 20, 30];
DECLARE result NUMBER = ARRAY_FIRST(arr);
-- Returns: 10
```

**Syntax:** `ARRAY_FIRST(array)`

---

### ARRAY_LAST

Returns the last element.

```sql
DECLARE arr ARRAY = [10, 20, 30];
DECLARE result NUMBER = ARRAY_LAST(arr);
-- Returns: 30
```

**Syntax:** `ARRAY_LAST(array)`

---

### ARRAY_FILTER

Filters elements based on a condition.

```sql
DECLARE numbers ARRAY = [1, 2, 3, 4, 5, 6];
DECLARE evens ARRAY = ARRAY_FILTER(numbers, 'x => x % 2 == 0');
-- Returns: [2, 4, 6]
```

**Syntax:** `ARRAY_FILTER(array, predicate)`

---

### ARRAY_MAP

Transforms each element.

```sql
DECLARE numbers ARRAY = [1, 2, 3];
DECLARE doubled ARRAY = ARRAY_MAP(numbers, 'x => x * 2');
-- Returns: [2, 4, 6]
```

**Syntax:** `ARRAY_MAP(array, transform)`

---

## Array Access

Access elements using square bracket notation (0-based indexing):

```sql
DECLARE arr ARRAY = ['a', 'b', 'c'];
DECLARE first STRING = arr[0];   -- 'a'
DECLARE second STRING = arr[1];  -- 'b'
```

## Iterating Arrays

Use `FOR` loops to iterate:

```sql
DECLARE items ARRAY = ['apple', 'banana', 'cherry'];
FOR i IN 0..(ARRAY_LENGTH(items)-1) LOOP
    PRINT items[i];
END LOOP;
```

## Example: Process Log Entries

```sql
CREATE PROCEDURE process_logs(logs ARRAY)
BEGIN
    DECLARE errors ARRAY = [];
    
    FOR i IN 0..(ARRAY_LENGTH(logs)-1) LOOP
        DECLARE log DOCUMENT = logs[i];
        IF DOCUMENT_GET(log, 'level') = 'ERROR' THEN
            SET errors = ARRAY_APPEND(errors, log);
        END IF;
    END LOOP;
    
    PRINT 'Found ' || ARRAY_LENGTH(errors) || ' errors';
    RETURN errors;
END PROCEDURE;
```

# Array Functions

## Quick Reference

| Function | Description |
|----------|-------------|
| [`ARRAY_APPEND`](#array-append) | Returns a new array with the specified element appended to t... |
| [`ARRAY_CONTAINS`](#array-contains) | Returns true if the array contains the specified element. |
| [`ARRAY_DISTINCT`](#array-distinct) | Returns a new array with duplicate elements removed, preserv... |
| [`ARRAY_EVERY`](#array-every) | Returns true if all elements have the specified property equ... |
| [`ARRAY_FILTER`](#array-filter) | Filters array elements using a lambda predicate or property ... |
| [`ARRAY_FIND`](#array-find) | Finds the first element where the specified property equals ... |
| [`ARRAY_FIND_INDEX`](#array-find-index) | Finds the index of the first element where the specified pro... |
| [`ARRAY_FLATTEN`](#array-flatten) | Flattens a nested array by one level. |
| [`ARRAY_JOIN`](#array-join) | Joins array elements into a single string with the specified... |
| [`ARRAY_LENGTH`](#array-length) | Returns the number of elements in the given array. |
| [`ARRAY_MAP`](#array-map) | Transforms array elements using a lambda or extracts a prope... |
| [`ARRAY_PREPEND`](#array-prepend) | Returns a new array with the specified element prepended to ... |
| [`ARRAY_REDUCE`](#array-reduce) | Reduces array to a single value by summing all numeric eleme... |
| [`ARRAY_REMOVE`](#array-remove) | Returns a new array with all occurrences of the specified el... |
| [`ARRAY_REVERSE`](#array-reverse) | Returns a new array with elements in reverse order. |
| [`ARRAY_SLICE`](#array-slice) | Returns a portion of the array from start index to end index... |
| [`ARRAY_SOME`](#array-some) | Returns true if at least one element has the specified prope... |
| [`ARRAY_SORT`](#array-sort) | Returns a new array sorted by the specified property (ascend... |

---

## Function Details

### ARRAY_APPEND

```
ARRAY_APPEND(array ARRAY, element ANY) -> ARRAY
```

Returns a new array with the specified element appended to the end.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to append to |
| `element` | ANY | The element to append |

**Returns:** `ARRAY`
 - A new array with the element appended


**Examples:**

```sql
ARRAY_APPEND([1, 2], 3) -> [1, 2, 3]
```

---

### ARRAY_CONTAINS

```
ARRAY_CONTAINS(array ARRAY, element ANY) -> BOOLEAN
```

Returns true if the array contains the specified element.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to search |
| `element` | ANY | The element to search for |

**Returns:** `BOOLEAN`
 - True if the array contains the element, otherwise false


**Examples:**

```sql
ARRAY_CONTAINS([1, 2, 3], 2) -> true
```

---

### ARRAY_DISTINCT

```
ARRAY_DISTINCT(array ARRAY) -> ARRAY
```

Returns a new array with duplicate elements removed, preserving original order.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to deduplicate |

**Returns:** `ARRAY`
 - A new array with duplicates removed


**Examples:**

```sql
ARRAY_DISTINCT([1, 2, 2, 3]) -> [1, 2, 3]
```

---

### ARRAY_EVERY

```
ARRAY_EVERY(array ARRAY, property STRING, value ANY) -> BOOLEAN
```

Returns true if all elements have the specified property equal to the given value.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to check |
| `property` | STRING | The property name to check |
| `value` | ANY | The value to match |

**Returns:** `BOOLEAN`
 - True if all match


---

### ARRAY_FILTER

```
ARRAY_FILTER(array ARRAY, predicateOrProperty ANY, value ANY) -> ARRAY
```

Filters array elements using a lambda predicate or property matching.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to filter |
| `predicateOrProperty` | ANY | A lambda (x) => boolean or property name |
| `value` | ANY | Value to match (only for property mode) |

**Returns:** `ARRAY`
 - Filtered array


**Examples:**

```sql
ARRAY_FILTER(users, (u) => u['age'] >= 18)
ARRAY_FILTER(users, 'status', 'active')
```

---

### ARRAY_FIND

```
ARRAY_FIND(array ARRAY, property STRING, value ANY) -> ANY
```

Finds the first element where the specified property equals the given value.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to search |
| `property` | STRING | The property name to check |
| `value` | ANY | The value to match |

**Returns:** `ANY`
 - The first matching element, or null


---

### ARRAY_FIND_INDEX

```
ARRAY_FIND_INDEX(array ARRAY, property STRING, value ANY) -> NUMBER
```

Finds the index of the first element where the specified property equals the given value.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to search |
| `property` | STRING | The property name to check |
| `value` | ANY | The value to match |

**Returns:** `NUMBER`
 - The index of the first match, or -1


---

### ARRAY_FLATTEN

```
ARRAY_FLATTEN(array ARRAY) -> ARRAY
```

Flattens a nested array by one level.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The nested array to flatten |

**Returns:** `ARRAY`
 - The flattened array


**Examples:**

```sql
ARRAY_FLATTEN([[1, 2], [3, 4]]) -> [1, 2, 3, 4]
```

---

### ARRAY_JOIN

```
ARRAY_JOIN(array ARRAY, delimiter STRING) -> STRING
```

Joins array elements into a single string with the specified delimiter.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to join |
| `delimiter` | STRING | The delimiter to use between elements |

**Returns:** `STRING`
 - The joined string


**Examples:**

```sql
ARRAY_JOIN(['a', 'b', 'c'], ', ') -> 'a, b, c'
```

---

### ARRAY_LENGTH

```
ARRAY_LENGTH(array ARRAY) -> INTEGER
```

Returns the number of elements in the given array.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array whose length is to be determined |

**Returns:** `INTEGER`
 - The length of the array


**Examples:**

```sql
ARRAY_LENGTH([1, 2, 3]) -> 3
```

---

### ARRAY_MAP

```
ARRAY_MAP(array ARRAY, mapperOrProperty ANY) -> ARRAY
```

Transforms array elements using a lambda or extracts a property.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to transform |
| `mapperOrProperty` | ANY | A lambda (x) => expr or property name |

**Returns:** `ARRAY`
 - Array of transformed values


**Examples:**

```sql
ARRAY_MAP(users, (u) => u['name'])
ARRAY_MAP(users, 'name')
```

---

### ARRAY_PREPEND

```
ARRAY_PREPEND(array ARRAY, element ANY) -> ARRAY
```

Returns a new array with the specified element prepended to the beginning.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to prepend to |
| `element` | ANY | The element to prepend |

**Returns:** `ARRAY`
 - A new array with the element prepended


**Examples:**

```sql
ARRAY_PREPEND([2, 3], 1) -> [1, 2, 3]
```

---

### ARRAY_REDUCE

```
ARRAY_REDUCE(array ARRAY, initial ANY) -> ANY
```

Reduces array to a single value by summing all numeric elements (or concatenating strings).

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to reduce |
| `initial` | ANY | The initial accumulator value |

**Returns:** `ANY`
 - The reduced value


**Examples:**

```sql
ARRAY_REDUCE([1, 2, 3], 0) -> 6
```

---

### ARRAY_REMOVE

```
ARRAY_REMOVE(array ARRAY, element ANY) -> ARRAY
```

Returns a new array with all occurrences of the specified element removed.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to remove elements from |
| `element` | ANY | The element to remove |

**Returns:** `ARRAY`
 - A new array with the element removed


**Examples:**

```sql
ARRAY_REMOVE([1, 2, 1, 3], 1) -> [2, 3]
```

---

### ARRAY_REVERSE

```
ARRAY_REVERSE(array ARRAY) -> ARRAY
```

Returns a new array with elements in reverse order.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to reverse |

**Returns:** `ARRAY`
 - The reversed array


**Examples:**

```sql
ARRAY_REVERSE([1, 2, 3]) -> [3, 2, 1]
```

---

### ARRAY_SLICE

```
ARRAY_SLICE(array ARRAY, start NUMBER, end NUMBER) -> ARRAY
```

Returns a portion of the array from start index to end index (exclusive).

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to slice |
| `start` | NUMBER | The start index (inclusive) |
| `end` | NUMBER | The end index (exclusive) |

**Returns:** `ARRAY`
 - The sliced portion of the array


**Examples:**

```sql
ARRAY_SLICE([1, 2, 3, 4, 5], 1, 4) -> [2, 3, 4]
```

---

### ARRAY_SOME

```
ARRAY_SOME(array ARRAY, property STRING, value ANY) -> BOOLEAN
```

Returns true if at least one element has the specified property equal to the given value.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to check |
| `property` | STRING | The property name to check |
| `value` | ANY | The value to match |

**Returns:** `BOOLEAN`
 - True if any match


---

### ARRAY_SORT

```
ARRAY_SORT(array ARRAY, property STRING) -> ARRAY
```

Returns a new array sorted by the specified property (ascending).

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `array` | ARRAY | The array to sort |
| `property` | STRING | The property name to sort by (optional for primitive arrays) |

**Returns:** `ARRAY`
 - The sorted array


---

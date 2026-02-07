# String Functions

## Quick Reference

| Function | Description |
|----------|-------------|
| [`ENV`](#env) | Returns the value of an environment variable. Returns empty ... |
| [`INITCAP`](#initcap) | Converts the first letter of each word to upper case and the... |
| [`INSTR`](#instr) | Returns the position of the first occurrence of a substring ... |
| [`LENGTH`](#length) | Returns the length of a string or the number of elements in ... |
| [`LOWER`](#lower) | Converts a string to lower case. |
| [`LPAD`](#lpad) | Pads the left side of a string with another string to a cert... |
| [`LTRIM`](#ltrim) | Removes leading whitespace from a string. |
| [`REGEXP_REPLACE`](#regexp-replace) | Replaces each substring that matches a regular expression wi... |
| [`REGEXP_SUBSTR`](#regexp-substr) | Returns the first substring that matches the given regular e... |
| [`REPLACE`](#replace) | Replaces all occurrences of a substring with a replacement s... |
| [`REVERSE`](#reverse) | Reverses a string. |
| [`RPAD`](#rpad) | Pads the right side of a string with another string to a cer... |
| [`RTRIM`](#rtrim) | Removes trailing whitespace from a string. |
| [`SPLIT`](#split) | Splits a string by the given delimiter. |
| [`SUBSTR`](#substr) | Returns a substring from a string starting at a given positi... |
| [`TRIM`](#trim) | Removes leading and trailing whitespace from a string. |
| [`UPPER`](#upper) | Converts a string to upper case. |
| [`||`](#||) | Concatenates two strings. |

---

## Function Details

### ENV

```
ENV(name STRING, default_value STRING) -> STRING
```

Returns the value of an environment variable. Returns empty string if not set.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `name` | STRING | The name of the environment variable |
| `default_value` | STRING | Default value if variable is not set (optional) |

**Returns:** `STRING`
 - The environment variable value or default


**Examples:**

```sql
ENV('OPENAI_API_KEY')
ENV('MY_VAR', 'default_value')
```

---

### INITCAP

```
INITCAP(input STRING) -> STRING
```

Converts the first letter of each word to upper case and the rest to lower case.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to convert |

**Returns:** `STRING`
 - The converted string


**Examples:**

```sql
INITCAP('elastic search') -> 'Elastic Search'
```

---

### INSTR

```
INSTR(input STRING, substring STRING) -> INTEGER
```

Returns the position of the first occurrence of a substring in a string (1-based). Returns 0 if not found.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to search |
| `substring` | STRING | The substring to search for |

**Returns:** `INTEGER`
 - The position of the substring, or 0 if not found


**Examples:**

```sql
INSTR('Elastic', 'as') -> 2
INSTR('Elastic', 'xyz') -> 0
```

---

### LENGTH

```
LENGTH(input ANY) -> INTEGER
```

Returns the length of a string or the number of elements in an array.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | ANY | The string or array to measure |

**Returns:** `INTEGER`


**Examples:**

```sql
LENGTH('Elastic') -> 7
LENGTH([1, 2, 3]) -> 3
```

---

### LOWER

```
LOWER(input STRING) -> STRING
```

Converts a string to lower case.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to convert |

**Returns:** `STRING`
 - The lower-cased string


**Examples:**

```sql
LOWER('Elastic') -> 'elastic'
```

---

### LPAD

```
LPAD(input STRING, totalLength NUMBER, padStr STRING) -> STRING
```

Pads the left side of a string with another string to a certain length.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The original string |
| `totalLength` | NUMBER | The desired total length |
| `padStr` | STRING | The string to pad with |

**Returns:** `STRING`
 - The padded string


**Examples:**

```sql
LPAD('Elastic', 10, '*') -> '***Elastic'
```

---

### LTRIM

```
LTRIM(input STRING) -> STRING
```

Removes leading whitespace from a string.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to trim |

**Returns:** `STRING`
 - The left-trimmed string


**Examples:**

```sql
LTRIM('   Elastic') -> 'Elastic'
```

---

### REGEXP_REPLACE

```
REGEXP_REPLACE(input STRING, regex STRING, replacement STRING) -> STRING
```

Replaces each substring that matches a regular expression with a replacement string.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to search |
| `regex` | STRING | The regular expression |
| `replacement` | STRING | The replacement string |

**Returns:** `STRING`
 - The string with regex replacements


**Examples:**

```sql
REGEXP_REPLACE('Elastic', '[aeiou]', '*') -> 'El*st*c'
```

---

### REGEXP_SUBSTR

```
REGEXP_SUBSTR(input STRING, regex STRING) -> STRING
```

Returns the first substring that matches the given regular expression.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to search |
| `regex` | STRING | The regular expression |

**Returns:** `STRING`
 - The first matching substring, or empty if none


**Examples:**

```sql
REGEXP_SUBSTR('Elastic', '[aeiou]') -> 'a'
```

---

### REPLACE

```
REPLACE(input STRING, target STRING, replacement STRING) -> STRING
```

Replaces all occurrences of a substring with a replacement string.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to search |
| `target` | STRING | The substring to replace |
| `replacement` | STRING | The replacement string |

**Returns:** `STRING`
 - The string with replacements


**Examples:**

```sql
REPLACE('Elastic', 'E', 'e') -> 'elastic'
```

---

### REVERSE

```
REVERSE(input STRING) -> STRING
```

Reverses a string.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to reverse |

**Returns:** `STRING`
 - The reversed string


**Examples:**

```sql
REVERSE('Elastic') -> 'citsalE'
```

---

### RPAD

```
RPAD(input STRING, totalLength NUMBER, padStr STRING) -> STRING
```

Pads the right side of a string with another string to a certain length.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The original string |
| `totalLength` | NUMBER | The desired total length |
| `padStr` | STRING | The string to pad with |

**Returns:** `STRING`
 - The padded string


**Examples:**

```sql
RPAD('Elastic', 10, '*') -> 'Elastic***'
```

---

### RTRIM

```
RTRIM(input STRING) -> STRING
```

Removes trailing whitespace from a string.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to trim |

**Returns:** `STRING`
 - The right-trimmed string


**Examples:**

```sql
RTRIM('Elastic   ') -> 'Elastic'
```

---

### SPLIT

```
SPLIT(input STRING, delimiter STRING) -> ARRAY<STRING>
```

Splits a string by the given delimiter.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to split |
| `delimiter` | STRING | The delimiter string |

**Returns:** `ARRAY<STRING>`
 - The list of split parts


**Examples:**

```sql
SPLIT('a,b,c', ',') -> ['a','b','c']
```

---

### SUBSTR

```
SUBSTR(input STRING, start NUMBER, length NUMBER) -> STRING
```

Returns a substring from a string starting at a given position with optional length.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to extract from |
| `start` | NUMBER | The starting position (1-based) |
| `length` | NUMBER | The length of the substring (optional) |

**Returns:** `STRING`
 - The extracted substring


**Examples:**

```sql
SUBSTR('Elastic', 2, 3) -> 'las'
SUBSTR('Elastic', 2) -> 'lastic'
```

---

### TRIM

```
TRIM(input STRING) -> STRING
```

Removes leading and trailing whitespace from a string.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to trim |

**Returns:** `STRING`
 - The trimmed string


**Examples:**

```sql
TRIM('  Elastic  ') -> 'Elastic'
```

---

### UPPER

```
UPPER(input STRING) -> STRING
```

Converts a string to upper case.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | STRING | The string to convert |

**Returns:** `STRING`
 - The upper-cased string


**Examples:**

```sql
UPPER('Elastic') -> 'ELASTIC'
```

---

### ||

```
||(left STRING, right STRING) -> STRING
```

Concatenates two strings.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `left` | STRING | The left string |
| `right` | STRING | The right string |

**Returns:** `STRING`
 - The concatenated string


**Examples:**

```sql
'foo' || 'bar' -> 'foobar'
```

---

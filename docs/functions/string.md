# String Functions

String manipulation functions for text processing.

## Function Reference

### LENGTH

Returns the length of a string.

```sql
DECLARE len NUMBER = LENGTH('Hello World');
-- Returns: 11
```

**Syntax:** `LENGTH(string)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| string | STRING | Input string |

**Returns:** NUMBER - The length of the string

---

### SUBSTR

Extracts a substring from a string.

```sql
DECLARE sub STRING = SUBSTR('Hello World', 1, 5);
-- Returns: 'Hello'
```

**Syntax:** `SUBSTR(string, start, length)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| string | STRING | Input string |
| start | NUMBER | Starting position (1-based) |
| length | NUMBER | Number of characters to extract |

**Returns:** STRING - The extracted substring

---

### UPPER

Converts a string to uppercase.

```sql
DECLARE result STRING = UPPER('hello');
-- Returns: 'HELLO'
```

**Syntax:** `UPPER(string)`

---

### LOWER

Converts a string to lowercase.

```sql
DECLARE result STRING = LOWER('HELLO');
-- Returns: 'hello'
```

**Syntax:** `LOWER(string)`

---

### TRIM

Removes leading and trailing whitespace.

```sql
DECLARE result STRING = TRIM('  hello  ');
-- Returns: 'hello'
```

**Syntax:** `TRIM(string)`

---

### LTRIM

Removes leading whitespace.

```sql
DECLARE result STRING = LTRIM('  hello');
-- Returns: 'hello'
```

**Syntax:** `LTRIM(string)`

---

### RTRIM

Removes trailing whitespace.

```sql
DECLARE result STRING = RTRIM('hello  ');
-- Returns: 'hello'
```

**Syntax:** `RTRIM(string)`

---

### REPLACE

Replaces occurrences of a substring.

```sql
DECLARE result STRING = REPLACE('hello world', 'world', 'everyone');
-- Returns: 'hello everyone'
```

**Syntax:** `REPLACE(string, search, replacement)`

---

### INSTR

Finds the position of a substring.

```sql
DECLARE pos NUMBER = INSTR('hello world', 'world');
-- Returns: 7
```

**Syntax:** `INSTR(string, search)`

**Returns:** NUMBER - Position (1-based), or 0 if not found

---

### LPAD

Left-pads a string to a specified length.

```sql
DECLARE result STRING = LPAD('42', 5, '0');
-- Returns: '00042'
```

**Syntax:** `LPAD(string, length, pad_string)`

---

### RPAD

Right-pads a string to a specified length.

```sql
DECLARE result STRING = RPAD('42', 5, '0');
-- Returns: '42000'
```

**Syntax:** `RPAD(string, length, pad_string)`

---

### SPLIT

Splits a string into an array.

```sql
DECLARE parts ARRAY = SPLIT('a,b,c', ',');
-- Returns: ['a', 'b', 'c']
```

**Syntax:** `SPLIT(string, delimiter)`

**Returns:** ARRAY - Array of substrings

---

### REGEXP_REPLACE

Replaces text matching a regular expression.

```sql
DECLARE result STRING = REGEXP_REPLACE('hello 123 world', '[0-9]+', 'XXX');
-- Returns: 'hello XXX world'
```

**Syntax:** `REGEXP_REPLACE(string, pattern, replacement)`

---

### REGEXP_SUBSTR

Extracts text matching a regular expression.

```sql
DECLARE result STRING = REGEXP_SUBSTR('hello 123 world', '[0-9]+');
-- Returns: '123'
```

**Syntax:** `REGEXP_SUBSTR(string, pattern)`

---

### REVERSE

Reverses a string.

```sql
DECLARE result STRING = REVERSE('hello');
-- Returns: 'olleh'
```

**Syntax:** `REVERSE(string)`

---

### INITCAP

Capitalizes the first letter of each word.

```sql
DECLARE result STRING = INITCAP('hello world');
-- Returns: 'Hello World'
```

**Syntax:** `INITCAP(string)`

---

### ENV

Reads an environment variable.

```sql
DECLARE home STRING = ENV('HOME');
```

**Syntax:** `ENV(variable_name)`

!!! warning "Security Note"
    Environment variable access may be restricted in production environments.

---

## String Concatenation

Use the `||` operator to concatenate strings:

```sql
DECLARE greeting STRING = 'Hello' || ' ' || 'World';
-- Returns: 'Hello World'
```

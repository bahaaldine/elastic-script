# Number Functions

## Quick Reference

| Function | Description |
|----------|-------------|
| [`ABS`](#abs) | Returns the absolute value of a number. |
| [`CEIL`](#ceil) | Returns the smallest integer value greater than or equal to ... |
| [`EXP`](#exp) | Returns the exponential of the number. |
| [`FLOOR`](#floor) | Returns the largest integer value less than or equal to the ... |
| [`LOG`](#log) | Returns the natural logarithm, or with base if provided. |
| [`MOD`](#mod) | Returns the remainder of the division of the first argument ... |
| [`POWER`](#power) | Raises the first argument to the power of the second. |
| [`ROUND`](#round) | Rounds the number. If only one argument is given, then the s... |
| [`SIGN`](#sign) | Returns 1 if positive, -1 if negative, and 0 if zero. |
| [`SQRT`](#sqrt) | Returns the square root of the number. |
| [`TRUNC`](#trunc) | Truncates a number. If only one argument is provided, scale ... |

---

## Function Details

### ABS

```
ABS(input NUMBER) -> NUMBER
```

Returns the absolute value of a number.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |

**Returns:** `NUMBER`
 - Absolute value of the input


**Examples:**

```sql
ABS(-5) => 5
```

---

### CEIL

```
CEIL(input NUMBER) -> NUMBER
```

Returns the smallest integer value greater than or equal to the number.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |

**Returns:** `NUMBER`
 - Ceiling value of the input


**Examples:**

```sql
CEIL(4.3) => 5.0
```

---

### EXP

```
EXP(input NUMBER) -> NUMBER
```

Returns the exponential of the number.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |

**Returns:** `NUMBER`
 - Exponential value of the input


**Examples:**

```sql
EXP(1) => 2.718281828459045
```

---

### FLOOR

```
FLOOR(input NUMBER) -> NUMBER
```

Returns the largest integer value less than or equal to the number.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |

**Returns:** `NUMBER`
 - Floor value of the input


**Examples:**

```sql
FLOOR(4.7) => 4.0
```

---

### LOG

```
LOG(input NUMBER, base NUMBER) -> NUMBER
```

Returns the natural logarithm, or with base if provided.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |
| `base` | NUMBER | Logarithm base (optional) |

**Returns:** `NUMBER`
 - Logarithm value


**Examples:**

```sql
LOG(10) => 2.302585092994046
LOG(8, 2) => 3.0
```

---

### MOD

```
MOD(a NUMBER, b NUMBER) -> NUMBER
```

Returns the remainder of the division of the first argument by the second.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `a` | NUMBER | Dividend |
| `b` | NUMBER | Divisor |

**Returns:** `NUMBER`
 - Remainder after division


**Examples:**

```sql
MOD(10, 3) => 1.0
```

---

### POWER

```
POWER(base NUMBER, exponent NUMBER) -> NUMBER
```

Raises the first argument to the power of the second.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `base` | NUMBER | Base number |
| `exponent` | NUMBER | Exponent |

**Returns:** `NUMBER`
 - Result of base raised to the exponent


**Examples:**

```sql
POWER(2, 3) => 8.0
```

---

### ROUND

```
ROUND(input NUMBER, scale NUMBER) -> NUMBER
```

Rounds the number. If only one argument is given, then the second parameter is ignored.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |
| `scale` | NUMBER | Number of decimal places to round to (optional) |

**Returns:** `NUMBER`
 - Rounded value of the input


**Examples:**

```sql
ROUND(4.567) => 5.0
ROUND(4.567, 2) => 4.57
```

---

### SIGN

```
SIGN(input NUMBER) -> NUMBER
```

Returns 1 if positive, -1 if negative, and 0 if zero.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |

**Returns:** `NUMBER`
 - Sign of the input


**Examples:**

```sql
SIGN(10) => 1
SIGN(-5) => -1
SIGN(0) => 0
```

---

### SQRT

```
SQRT(input NUMBER) -> NUMBER
```

Returns the square root of the number.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |

**Returns:** `NUMBER`
 - Square root of the input


**Examples:**

```sql
SQRT(9) => 3.0
```

---

### TRUNC

```
TRUNC(input NUMBER, scale NUMBER) -> NUMBER
```

Truncates a number. If only one argument is provided, scale is ignored.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `input` | NUMBER | Input number |
| `scale` | NUMBER | Number of decimal places to truncate to (optional) |

**Returns:** `NUMBER`
 - Truncated value of the input


**Examples:**

```sql
TRUNC(4.567) => 4.0
TRUNC(4.567, 2) => 4.56
```

---

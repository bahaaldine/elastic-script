# Number Functions

Mathematical and numeric operations.

## Function Reference

### ABS

Returns the absolute value of a number.

```sql
DECLARE result NUMBER = ABS(-42);
-- Returns: 42
```

**Syntax:** `ABS(number)`

---

### CEIL

Rounds up to the nearest integer.

```sql
DECLARE result NUMBER = CEIL(4.2);
-- Returns: 5
```

**Syntax:** `CEIL(number)`

---

### FLOOR

Rounds down to the nearest integer.

```sql
DECLARE result NUMBER = FLOOR(4.8);
-- Returns: 4
```

**Syntax:** `FLOOR(number)`

---

### ROUND

Rounds to a specified number of decimal places.

```sql
DECLARE result NUMBER = ROUND(3.14159, 2);
-- Returns: 3.14
```

**Syntax:** `ROUND(number, decimals)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| number | NUMBER | Value to round |
| decimals | NUMBER | Number of decimal places (default: 0) |

---

### TRUNC

Truncates to a specified number of decimal places.

```sql
DECLARE result NUMBER = TRUNC(3.14159, 2);
-- Returns: 3.14
```

**Syntax:** `TRUNC(number, decimals)`

!!! note "TRUNC vs ROUND"
    `TRUNC` always removes digits toward zero, while `ROUND` uses standard rounding rules.

---

### MOD

Returns the remainder of division.

```sql
DECLARE result NUMBER = MOD(17, 5);
-- Returns: 2
```

**Syntax:** `MOD(dividend, divisor)`

---

### POWER

Raises a number to a power.

```sql
DECLARE result NUMBER = POWER(2, 10);
-- Returns: 1024
```

**Syntax:** `POWER(base, exponent)`

---

### SQRT

Returns the square root.

```sql
DECLARE result NUMBER = SQRT(144);
-- Returns: 12
```

**Syntax:** `SQRT(number)`

---

### EXP

Returns e raised to a power.

```sql
DECLARE result NUMBER = EXP(1);
-- Returns: 2.718281828...
```

**Syntax:** `EXP(number)`

---

### LOG

Returns the logarithm of a number.

```sql
DECLARE result NUMBER = LOG(100, 10);
-- Returns: 2 (log base 10 of 100)
```

**Syntax:** `LOG(number, base)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| number | NUMBER | Value to take logarithm of |
| base | NUMBER | Logarithm base |

---

### SIGN

Returns the sign of a number (-1, 0, or 1).

```sql
DECLARE result NUMBER = SIGN(-42);
-- Returns: -1
```

**Syntax:** `SIGN(number)`

**Returns:**

| Value | Meaning |
|-------|---------|
| -1 | Negative number |
| 0 | Zero |
| 1 | Positive number |

---

## Arithmetic Operators

Standard arithmetic operators are available:

```sql
DECLARE a NUMBER = 10 + 5;   -- Addition: 15
DECLARE b NUMBER = 10 - 5;   -- Subtraction: 5
DECLARE c NUMBER = 10 * 5;   -- Multiplication: 50
DECLARE d NUMBER = 10 / 5;   -- Division: 2
```

## Example: Calculate Statistics

```sql
CREATE PROCEDURE calculate_stats(values ARRAY)
BEGIN
    DECLARE sum NUMBER = 0;
    DECLARE count NUMBER = ARRAY_LENGTH(values);
    
    FOR i IN 0..(count-1) LOOP
        SET sum = sum + values[i];
    END LOOP;
    
    DECLARE mean NUMBER = sum / count;
    PRINT 'Mean: ' || ROUND(mean, 2);
    
    RETURN mean;
END PROCEDURE;
```

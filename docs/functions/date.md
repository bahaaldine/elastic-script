# Date Functions

## Quick Reference

| Function | Description |
|----------|-------------|
| [`CURRENT_DATE`](#current-date) | Returns the current date with time set to midnight. |
| [`CURRENT_DATE`](#current-date) | Returns the current date with time set to midnight. |
| [`CURRENT_TIMESTAMP`](#current-timestamp) | Returns the current date and time. |
| [`CURRENT_TIMESTAMP`](#current-timestamp) | Returns the current date and time. |
| [`DATEDIFF`](#datediff) | Returns the difference in days between two dates. |
| [`DATEDIFF`](#datediff) | Returns the difference in days between two dates. |
| [`DATE_ADD`](#date-add) | Adds a given number of days to a date. |
| [`DATE_ADD`](#date-add) | Adds a given number of days to a date. |
| [`DATE_SUB`](#date-sub) | Subtracts a given number of days from a date. |
| [`DATE_SUB`](#date-sub) | Subtracts a given number of days from a date. |
| [`EXTRACT_DAY`](#extract-day) | Extracts the day of month from a date. |
| [`EXTRACT_DAY`](#extract-day) | Extracts the day of month from a date. |
| [`EXTRACT_MONTH`](#extract-month) | Extracts the month (1-12) from a date. |
| [`EXTRACT_MONTH`](#extract-month) | Extracts the month (1-12) from a date. |
| [`EXTRACT_YEAR`](#extract-year) | Extracts the year component from a date. |
| [`EXTRACT_YEAR`](#extract-year) | Extracts the year component from a date. |

---

## Function Details

### CURRENT_DATE

```
CURRENT_DATE() -> DATE
```

Returns the current date with time set to midnight.

**Returns:** `DATE`
 - The current date with time zeroed.


**Examples:**

```sql
CURRENT_DATE() -> 2024-05-10
```

---

### CURRENT_DATE

```
CURRENT_DATE() -> DATE
```

Returns the current date with time set to midnight.

**Returns:** `DATE`
 - The current date with time zeroed.


**Examples:**

```sql
CURRENT_DATE() -> 2024-05-10
```

---

### CURRENT_TIMESTAMP

```
CURRENT_TIMESTAMP() -> DATE
```

Returns the current date and time.

**Returns:** `DATE`
 - The current date and time.


**Examples:**

```sql
CURRENT_TIMESTAMP() -> 2024-05-10T12:34:56
```

---

### CURRENT_TIMESTAMP

```
CURRENT_TIMESTAMP() -> DATE
```

Returns the current date and time.

**Returns:** `DATE`
 - The current date and time.


**Examples:**

```sql
CURRENT_TIMESTAMP() -> 2024-05-10T12:34:56
```

---

### DATEDIFF

```
DATEDIFF(date1 DATE, date2 DATE) -> NUMBER
```

Returns the difference in days between two dates.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date1` | DATE | The first date. |
| `date2` | DATE | The second date. |

**Returns:** `NUMBER`
 - Number of days between date1 and date2.


**Examples:**

```sql
DATEDIFF('2024-05-13', '2024-05-10') -> 3
```

---

### DATEDIFF

```
DATEDIFF(date1 DATE, date2 DATE) -> NUMBER
```

Returns the difference in days between two dates.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date1` | DATE | The first date. |
| `date2` | DATE | The second date. |

**Returns:** `NUMBER`
 - Number of days between date1 and date2.


**Examples:**

```sql
DATEDIFF('2024-05-13', '2024-05-10') -> 3
```

---

### DATE_ADD

```
DATE_ADD(date DATE, days NUMBER) -> DATE
```

Adds a given number of days to a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The original date. |
| `days` | NUMBER | Number of days to add. |

**Returns:** `DATE`
 - The resulting date after addition.


**Examples:**

```sql
DATE_ADD('2024-05-10', 3) -> 2024-05-13
```

---

### DATE_ADD

```
DATE_ADD(date DATE, days NUMBER) -> DATE
```

Adds a given number of days to a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The original date. |
| `days` | NUMBER | Number of days to add. |

**Returns:** `DATE`
 - The resulting date after addition.


**Examples:**

```sql
DATE_ADD('2024-05-10', 3) -> 2024-05-13
```

---

### DATE_SUB

```
DATE_SUB(date DATE, days NUMBER) -> DATE
```

Subtracts a given number of days from a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The original date. |
| `days` | NUMBER | Number of days to subtract. |

**Returns:** `DATE`
 - The resulting date after subtraction.


**Examples:**

```sql
DATE_SUB('2024-05-10', 2) -> 2024-05-08
```

---

### DATE_SUB

```
DATE_SUB(date DATE, days NUMBER) -> DATE
```

Subtracts a given number of days from a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The original date. |
| `days` | NUMBER | Number of days to subtract. |

**Returns:** `DATE`
 - The resulting date after subtraction.


**Examples:**

```sql
DATE_SUB('2024-05-10', 2) -> 2024-05-08
```

---

### EXTRACT_DAY

```
EXTRACT_DAY(date DATE) -> NUMBER
```

Extracts the day of month from a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The date to extract from. |

**Returns:** `NUMBER`
 - The day of the month.


**Examples:**

```sql
EXTRACT_DAY('2024-05-10') -> 10
```

---

### EXTRACT_DAY

```
EXTRACT_DAY(date DATE) -> NUMBER
```

Extracts the day of month from a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The date to extract from. |

**Returns:** `NUMBER`
 - The day of the month.


**Examples:**

```sql
EXTRACT_DAY('2024-05-10') -> 10
```

---

### EXTRACT_MONTH

```
EXTRACT_MONTH(date DATE) -> NUMBER
```

Extracts the month (1-12) from a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The date to extract from. |

**Returns:** `NUMBER`


**Examples:**

```sql
EXTRACT_MONTH('2024-05-10') -> 5
```

---

### EXTRACT_MONTH

```
EXTRACT_MONTH(date DATE) -> NUMBER
```

Extracts the month (1-12) from a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The date to extract from. |

**Returns:** `NUMBER`


**Examples:**

```sql
EXTRACT_MONTH('2024-05-10') -> 5
```

---

### EXTRACT_YEAR

```
EXTRACT_YEAR(date DATE) -> NUMBER
```

Extracts the year component from a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The date to extract from. |

**Returns:** `NUMBER`
 - The year component.


**Examples:**

```sql
EXTRACT_YEAR('2024-05-10') -> 2024
```

---

### EXTRACT_YEAR

```
EXTRACT_YEAR(date DATE) -> NUMBER
```

Extracts the year component from a date.

**Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `date` | DATE | The date to extract from. |

**Returns:** `NUMBER`
 - The year component.


**Examples:**

```sql
EXTRACT_YEAR('2024-05-10') -> 2024
```

---

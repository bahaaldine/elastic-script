# Date Functions

Functions for working with dates and timestamps.

## Function Reference

### CURRENT_DATE

Returns the current date.

```sql
DECLARE today DATE = CURRENT_DATE();
-- Returns: 2026-01-09
```

**Syntax:** `CURRENT_DATE()`

---

### CURRENT_TIMESTAMP

Returns the current date and time.

```sql
DECLARE now DATE = CURRENT_TIMESTAMP();
-- Returns: 2026-01-09T15:30:00Z
```

**Syntax:** `CURRENT_TIMESTAMP()`

---

### DATE_ADD

Adds days to a date.

```sql
DECLARE today DATE = CURRENT_DATE();
DECLARE next_week DATE = DATE_ADD(today, 7);
-- Adds 7 days
```

**Syntax:** `DATE_ADD(date, days)`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| date | DATE | Starting date |
| days | NUMBER | Number of days to add |

---

### DATE_SUB

Subtracts days from a date.

```sql
DECLARE today DATE = CURRENT_DATE();
DECLARE last_week DATE = DATE_SUB(today, 7);
-- Subtracts 7 days
```

**Syntax:** `DATE_SUB(date, days)`

---

### EXTRACT_YEAR

Extracts the year from a date.

```sql
DECLARE now DATE = CURRENT_TIMESTAMP();
DECLARE year NUMBER = EXTRACT_YEAR(now);
-- Returns: 2026
```

**Syntax:** `EXTRACT_YEAR(date)`

---

### EXTRACT_MONTH

Extracts the month from a date (1-12).

```sql
DECLARE now DATE = CURRENT_TIMESTAMP();
DECLARE month NUMBER = EXTRACT_MONTH(now);
-- Returns: 1 (January)
```

**Syntax:** `EXTRACT_MONTH(date)`

---

### EXTRACT_DAY

Extracts the day of month from a date (1-31).

```sql
DECLARE now DATE = CURRENT_TIMESTAMP();
DECLARE day NUMBER = EXTRACT_DAY(now);
-- Returns: 9
```

**Syntax:** `EXTRACT_DAY(date)`

---

### DATE_DIFF

Calculates the difference between two dates in days.

```sql
DECLARE start_date DATE = DATE_SUB(CURRENT_DATE(), 30);
DECLARE end_date DATE = CURRENT_DATE();
DECLARE days_between NUMBER = DATE_DIFF(end_date, start_date);
-- Returns: 30
```

**Syntax:** `DATE_DIFF(date1, date2)`

**Returns:** NUMBER - Days between dates (date1 - date2)

---

## Example: Date-Based Filtering

```sql
CREATE PROCEDURE get_recent_logs(days_back NUMBER)
BEGIN
    DECLARE cutoff DATE = DATE_SUB(CURRENT_DATE(), days_back);
    
    DECLARE logs CURSOR FOR ESQL_QUERY('
        FROM logs-* 
        | WHERE @timestamp >= "' || cutoff || '"
        | LIMIT 100
    ');
    
    FOR log IN logs LOOP
        PRINT DOCUMENT_GET(log, 'message');
    END LOOP;
END PROCEDURE;
```

## Example: Generate Date Range

```sql
CREATE PROCEDURE date_range(start_date DATE, num_days NUMBER)
BEGIN
    DECLARE dates ARRAY = [];
    
    FOR i IN 0..(num_days-1) LOOP
        DECLARE d DATE = DATE_ADD(start_date, i);
        SET dates = ARRAY_APPEND(dates, d);
    END LOOP;
    
    RETURN dates;
END PROCEDURE;
```

## Working with Timestamps

Dates and timestamps are interchangeable in most contexts:

```sql
DECLARE event_time DATE = CURRENT_TIMESTAMP();

-- Extract components
PRINT 'Year: ' || EXTRACT_YEAR(event_time);
PRINT 'Month: ' || EXTRACT_MONTH(event_time);
PRINT 'Day: ' || EXTRACT_DAY(event_time);
```

!!! note "Timezone Handling"
    All dates and timestamps are in UTC by default. Timezone conversion 
    should be handled at the application layer if needed.

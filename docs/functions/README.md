# Built-in Functions Reference

elastic-script provides a comprehensive library of built-in functions 
organized by category.

## Categories

Total: **100 functions** across **8 categories**

| Category | Functions | Description |
|----------|-----------|-------------|
| [ARRAY](array.md) | 18 | Array/list operations and transformations |
| [DATASOURCE](datasource.md) | 16 |  |
| [DATE](date.md) | 16 | Date and time operations |
| [DOCUMENT](document.md) | 6 | Document/object manipulation |
| [NETWORK](network.md) | 5 |  |
| [NLP](nlp.md) | 10 |  |
| [NUMBER](number.md) | 11 | Mathematical and numeric operations |
| [STRING](string.md) | 18 | String manipulation and formatting |

## Usage

Functions can be called from anywhere in elastic-script:

```sql
-- In expressions
DECLARE result STRING = UPPER('hello');

-- In SET statements
SET total = ARRAY_LENGTH(items);

-- In conditions
IF LENGTH(name) > 10 THEN
    PRINT 'Long name';
END IF;
```

## See Also

- [Variables & Types](../language/variables-types.md)

- [Control Flow](../language/control-flow.md)

- [User-Defined Functions](../language/functions.md)

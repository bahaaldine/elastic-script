# User-Defined Types

User-defined types allow you to create custom record structures with named fields and their data types.

## Overview

Types are useful for:

- **Structured data** - Define consistent record formats
- **Documentation** - Self-documenting data structures
- **Validation** - Ensure data has the expected shape
- **Reusability** - Share type definitions across procedures

## Creating Types

Use `CREATE TYPE` to define a new record type:

```sql
CREATE TYPE type_name AS RECORD (
    field1 TYPE,
    field2 TYPE,
    ...
) END TYPE
```

### Example: Address Type

```sql
CREATE TYPE address_type AS RECORD (
    street STRING,
    city STRING,
    state STRING,
    zip STRING,
    country STRING
) END TYPE
```

### Example: Customer Type

```sql
CREATE TYPE customer_type AS RECORD (
    id STRING,
    name STRING,
    email STRING,
    age NUMBER,
    verified BOOLEAN,
    created_at DATE
) END TYPE
```

### Example: Order Type

```sql
CREATE TYPE order_type AS RECORD (
    order_id STRING,
    customer_id STRING,
    items ARRAY,
    total NUMBER,
    status STRING,
    metadata DOCUMENT
) END TYPE
```

## Supported Field Types

| Type | Description | Default Value |
|------|-------------|---------------|
| `STRING` | Text values | `""` |
| `NUMBER` | Numeric values (int/float) | `0` |
| `BOOLEAN` | True/false | `false` |
| `DATE` | Date/time values | `null` |
| `ARRAY` | List of values | `[]` |
| `DOCUMENT` | Nested object/map | `{}` |
| `MAP` | Key-value pairs | `{}` |

## Viewing Types

### Show All Types

```sql
SHOW TYPES
```

Lists all defined types with their fields.

### Show Type Details

```sql
SHOW TYPE type_name
```

Shows detailed information about a specific type.

## Dropping Types

```sql
DROP TYPE type_name
```

Removes the type definition.

## Storage

Types are stored in the `.escript_types` index with:

- Type name (unique identifier)
- Field definitions (name and type)
- Creation timestamp

## Best Practices

1. **Use descriptive names** - End type names with `_type` for clarity
2. **Keep types focused** - One type per concept
3. **Document fields** - Use meaningful field names
4. **Consider nesting** - Use DOCUMENT for complex nested structures

## Example: Complete Workflow

```sql
-- Define types for an e-commerce system
CREATE TYPE product_type AS RECORD (
    sku STRING,
    name STRING,
    price NUMBER,
    in_stock BOOLEAN,
    categories ARRAY
) END TYPE

CREATE TYPE line_item_type AS RECORD (
    product_sku STRING,
    quantity NUMBER,
    unit_price NUMBER,
    total NUMBER
) END TYPE

CREATE TYPE invoice_type AS RECORD (
    invoice_id STRING,
    customer_id STRING,
    items ARRAY,
    subtotal NUMBER,
    tax NUMBER,
    total NUMBER,
    issued_at DATE
) END TYPE

-- View all types
SHOW TYPES

-- View specific type
SHOW TYPE invoice_type

-- Clean up
DROP TYPE product_type
DROP TYPE line_item_type
DROP TYPE invoice_type
```

## See Also

- [Variables & Types](variables-types.md) - Built-in data types
- [Packages](packages.md) - Organizing types in packages
- [Procedures](procedures.md) - Using types in procedures

# Type-Namespaced Functions

This guide demonstrates the **namespaced function syntax** introduced in elastic-script for cleaner, more organized function calls.

## Syntax: `NAMESPACE.METHOD(args)`

Instead of `ARRAY_MAP(items, fn)`, you can now write `ARRAY.MAP(items, fn)`.

### Supported Namespaces

| Namespace | Description | Examples |
|-----------|-------------|----------|
| `ARRAY` | Array operations | `ARRAY.MAP()`, `ARRAY.FILTER()`, `ARRAY.LENGTH()` |
| `STRING` | String manipulation | `STRING.UPPER()`, `STRING.LOWER()`, `STRING.REPLACE()` |
| `NUMBER` | Numeric operations | `NUMBER.FORMAT()`, `NUMBER.ROUND()` |
| `DATE` | Date/time operations | `DATE.ADD()`, `DATE.FORMAT()`, `DATE.PARSE()` |
| `DOCUMENT` | Document operations | `DOCUMENT.KEYS()`, `DOCUMENT.VALUES()`, `DOCUMENT.MERGE()` |
| `HTTP` | HTTP requests | `HTTP.GET()`, `HTTP.POST()` |
| `K8S` | Kubernetes | `K8S.GET_PODS()`, `K8S.GET_NODES()` |
| `AWS` | AWS services | `AWS.S3_GET()`, `AWS.LAMBDA_INVOKE()` |

## Array Operations with `ARRAY.*`

```sql
CREATE PROCEDURE array_demo()
BEGIN
    DECLARE numbers ARRAY = [1, 2, 3, 4, 5];
    
    -- Get array length
    DECLARE len NUMBER;
    SET len = ARRAY.LENGTH(numbers);
    
    -- Double each number
    DECLARE doubled ARRAY;
    SET doubled = ARRAY.MAP(numbers, x => x * 2);
    
    -- Filter even numbers
    DECLARE evens ARRAY;
    SET evens = ARRAY.FILTER(doubled, x => x % 4 == 0);
    
    PRINT('Results: ' || evens);
END PROCEDURE

CALL array_demo();
```

## String Operations with `STRING.*`

```sql
CREATE PROCEDURE string_demo()
BEGIN
    DECLARE text STRING = 'Hello, elastic-script!';
    
    -- Convert to uppercase
    DECLARE upper STRING;
    SET upper = STRING.UPPER(text);
    
    -- Replace text
    DECLARE replaced STRING;
    SET replaced = STRING.REPLACE(text, 'script', 'SCRIPT');
    
    PRINT('Processed: ' || replaced);
END PROCEDURE

CALL string_demo();
```

## Combined with First-Class Commands

Namespaced functions work seamlessly with first-class commands:

```sql
CREATE PROCEDURE data_processing()
BEGIN
    DECLARE users ARRAY = [
        {'name': 'john doe', 'active': true},
        {'name': 'jane smith', 'active': false}
    ];
    
    -- Transform using ARRAY.MAP
    DECLARE processed ARRAY;
    SET processed = ARRAY.MAP(users, user => {
        'name': STRING.UPPER(user['name']),
        'active': user['active']
    });
    
    -- Filter with ARRAY.FILTER
    DECLARE active ARRAY;
    SET active = ARRAY.FILTER(processed, u => u['active'] == true);
    
    -- Index using first-class command
    FOR user IN active LOOP
        INDEX user INTO 'users-index';
    END LOOP;
    
    REFRESH 'users-index';
END PROCEDURE
```

## Syntax Comparison

| Old Syntax | New Namespaced Syntax |
|------------|----------------------|
| `ARRAY_MAP(arr, fn)` | `ARRAY.MAP(arr, fn)` |
| `ARRAY_FILTER(arr, fn)` | `ARRAY.FILTER(arr, fn)` |
| `STRING_UPPER(str)` | `STRING.UPPER(str)` |
| `DOCUMENT_KEYS(doc)` | `DOCUMENT.KEYS(doc)` |
| `DATE_ADD(date, interval)` | `DATE.ADD(date, interval)` |

Both syntaxes are supported for backward compatibility!

## Try It

Download the [11-namespaced-functions.ipynb](https://github.com/bahaaldine/elastic-script/blob/main/notebooks/11-namespaced-functions.ipynb) notebook.

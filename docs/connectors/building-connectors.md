# Building Connectors

Create custom connectors to integrate any data source with Moltler.

## Connector Architecture

A connector consists of:

1. **Configuration** - Connection settings and credentials
2. **Entities** - Data types exposed (issues, users, metrics)
3. **Query Handler** - Translates queries to API calls
4. **Actions** - Write operations (create, update, delete)
5. **Sync Engine** - Incremental data synchronization

---

## Creating a Custom Connector

### 1. Define Connector Type

```sql
CREATE CONNECTOR TYPE 'myservice'
DESCRIPTION 'Custom connector for MyService API'
CONFIG_SCHEMA {
    "api_url": {"type": "string", "required": true},
    "api_key": {"type": "string", "required": true, "secret": true}
};
```

### 2. Define Entities

```sql
CREATE CONNECTOR ENTITY 'myservice.items'
FIELDS {
    "id": {"type": "string", "primary_key": true},
    "name": {"type": "string"},
    "status": {"type": "string"},
    "created_at": {"type": "timestamp"},
    "metadata": {"type": "document"}
}
QUERY_HANDLER '''
async function query(config, filters, options) {
    const response = await fetch(config.api_url + '/items', {
        headers: {'Authorization': 'Bearer ' + config.api_key}
    });
    return await response.json();
}
''';
```

### 3. Implement Query Handler

```javascript
// Query handler receives:
// - config: connector configuration
// - filters: WHERE clause conditions
// - options: LIMIT, ORDER BY, etc.

async function query(config, filters, options) {
    const params = new URLSearchParams();
    
    // Apply filters
    if (filters.status) {
        params.append('status', filters.status);
    }
    
    // Apply pagination
    if (options.limit) {
        params.append('limit', options.limit);
    }
    
    const url = `${config.api_url}/items?${params}`;
    const response = await fetch(url, {
        headers: {
            'Authorization': `Bearer ${config.api_key}`,
            'Content-Type': 'application/json'
        }
    });
    
    if (!response.ok) {
        throw new Error(`API error: ${response.status}`);
    }
    
    const data = await response.json();
    
    // Transform to expected format
    return data.items.map(item => ({
        id: item.id,
        name: item.name,
        status: item.status,
        created_at: new Date(item.created_at),
        metadata: item.extra || {}
    }));
}
```

### 4. Implement Actions

```sql
CREATE CONNECTOR ACTION 'myservice.items.create'
PARAMETERS {
    "name": {"type": "string", "required": true},
    "status": {"type": "string", "default": "active"}
}
HANDLER '''
async function execute(config, params) {
    const response = await fetch(config.api_url + '/items', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + config.api_key,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(params)
    });
    return await response.json();
}
''';
```

### 5. Register Connector

```sql
REGISTER CONNECTOR TYPE 'myservice';
```

---

## Using Your Connector

### Create Instance

```sql
CREATE CONNECTOR my_service_prod
TYPE 'myservice'
CONFIG {
    "api_url": "https://api.myservice.com",
    "api_key": "{{secrets.myservice_key}}"
};
```

### Query

```sql
SELECT id, name, status
FROM my_service_prod.items
WHERE status = 'active';
```

### Execute Actions

```sql
CONNECTOR_EXEC my_service_prod.items.create({
    "name": "New Item",
    "status": "pending"
});
```

---

## Advanced Features

### Pagination

```javascript
async function query(config, filters, options) {
    let allResults = [];
    let cursor = null;
    
    do {
        const params = new URLSearchParams();
        if (cursor) params.append('cursor', cursor);
        params.append('limit', 100);
        
        const response = await fetch(
            `${config.api_url}/items?${params}`,
            {headers: {'Authorization': `Bearer ${config.api_key}`}}
        );
        const data = await response.json();
        
        allResults = allResults.concat(data.items);
        cursor = data.next_cursor;
        
        if (options.limit && allResults.length >= options.limit) {
            break;
        }
    } while (cursor);
    
    return allResults.slice(0, options.limit);
}
```

### Rate Limiting

```javascript
const rateLimiter = {
    lastCall: 0,
    minInterval: 100, // 10 requests per second
    
    async wait() {
        const now = Date.now();
        const elapsed = now - this.lastCall;
        if (elapsed < this.minInterval) {
            await new Promise(r => setTimeout(r, this.minInterval - elapsed));
        }
        this.lastCall = Date.now();
    }
};

async function query(config, filters, options) {
    await rateLimiter.wait();
    // ... make API call
}
```

### Caching

```javascript
const cache = new Map();
const CACHE_TTL = 60000; // 1 minute

async function query(config, filters, options) {
    const cacheKey = JSON.stringify({config, filters, options});
    const cached = cache.get(cacheKey);
    
    if (cached && Date.now() - cached.time < CACHE_TTL) {
        return cached.data;
    }
    
    const data = await fetchFromAPI(config, filters, options);
    cache.set(cacheKey, {data, time: Date.now()});
    return data;
}
```

### Error Handling

```javascript
async function query(config, filters, options) {
    try {
        const response = await fetch(url, {headers});
        
        if (response.status === 401) {
            throw new ConnectorAuthError('Invalid API key');
        }
        
        if (response.status === 429) {
            const retryAfter = response.headers.get('Retry-After') || 60;
            throw new ConnectorRateLimitError(
                `Rate limited. Retry after ${retryAfter}s`
            );
        }
        
        if (!response.ok) {
            throw new ConnectorError(
                `API error: ${response.status} ${response.statusText}`
            );
        }
        
        return await response.json();
    } catch (error) {
        if (error.name === 'FetchError') {
            throw new ConnectorConnectionError(
                `Cannot connect to ${config.api_url}`
            );
        }
        throw error;
    }
}
```

---

## Testing Connectors

### Unit Tests

```sql
TEST CONNECTOR TYPE 'myservice'
NAME 'query returns items'
MOCK_API {
    "GET /items": {
        "status": 200,
        "body": {"items": [{"id": "1", "name": "Test"}]}
    }
}
BEGIN
    CREATE CONNECTOR test_conn TYPE 'myservice'
    CONFIG {"api_url": "mock://", "api_key": "test"};
    
    DECLARE results = SELECT * FROM test_conn.items;
    ASSERT ARRAY_LENGTH(results) = 1;
    ASSERT results[0].name = 'Test';
END TEST;
```

### Integration Tests

```sql
TEST CONNECTOR TYPE 'myservice'
NAME 'integration with real API'
TAGS ['integration', 'slow']
BEGIN
    CREATE CONNECTOR test_conn TYPE 'myservice'
    CONFIG {
        "api_url": ENV('MYSERVICE_TEST_URL'),
        "api_key": ENV('MYSERVICE_TEST_KEY')
    };
    
    -- Create item
    DECLARE created = CONNECTOR_EXEC test_conn.items.create({
        "name": "Integration Test " || CURRENT_TIMESTAMP()
    });
    ASSERT created.id IS NOT NULL;
    
    -- Query item
    DECLARE items = SELECT * FROM test_conn.items
                    WHERE id = created.id;
    ASSERT ARRAY_LENGTH(items) = 1;
    
    -- Cleanup
    CONNECTOR_EXEC test_conn.items.delete(id => created.id);
END TEST;
```

---

## Publishing Connectors

### Package Connector

```sql
PACKAGE CONNECTOR TYPE 'myservice'
VERSION '1.0.0'
DESCRIPTION 'Connector for MyService API'
AUTHOR 'Your Name'
LICENSE 'MIT';
```

### Publish

```sql
PUBLISH CONNECTOR TYPE 'myservice'
TO 'marketplace';
```

---

## Best Practices

1. **Use secrets for credentials** - Never hardcode API keys
2. **Implement rate limiting** - Respect API limits
3. **Cache when appropriate** - Reduce API calls
4. **Handle errors gracefully** - Provide useful error messages
5. **Document thoroughly** - Include examples and field descriptions
6. **Test extensively** - Cover edge cases and error conditions
7. **Version your connector** - Use semantic versioning

---

## What's Next?

<div class="grid cards" markdown>

-   :material-robot:{ .lg .middle } __Agents__

    Create agents that use connectors.

    [:octicons-arrow-right-24: Agents](../agents/overview.md)

-   :material-package-variant:{ .lg .middle } __Skills__

    Build skills with connector data.

    [:octicons-arrow-right-24: Skills](../skills/overview.md)

</div>

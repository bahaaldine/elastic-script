---
name: security-ops
description: Manage Elasticsearch security including users, roles, API keys, and security features. Use when the user needs to manage access control, investigate security events, or configure authentication.
---

# Security Operations

This skill enables you to manage Elasticsearch security and investigate security events.

## When to Use

- User needs to **manage users** or **roles**
- User wants to **create or revoke API keys**
- User asks about **authentication** or **authorization**
- User needs to investigate **security events**
- User wants to check **user permissions**

## User Management

| Function | Description | Example |
|----------|-------------|---------|
| `ES_GET_USERS(username?)` | List users | `ES_GET_USERS()` |
| `ES_CREATE_USER(username, config)` | Create/update user | See below |
| `ES_DELETE_USER(username)` | Delete a user | `ES_DELETE_USER('olduser')` |
| `ES_AUTHENTICATE()` | Get current user | `ES_AUTHENTICATE()` |

### Create a User

```sql
DECLARE result DOCUMENT;
SET result = ES_CREATE_USER('analyst', {
    'password': 'secure-password-123',
    'roles': ['viewer', 'logs_reader'],
    'full_name': 'Security Analyst',
    'email': 'analyst@example.com',
    'metadata': {
        'team': 'security',
        'department': 'IT'
    }
});
```

## Role Management

| Function | Description | Example |
|----------|-------------|---------|
| `ES_GET_ROLES(role?)` | List roles | `ES_GET_ROLES()` |
| `ES_CREATE_ROLE(name, config)` | Create/update role | See below |
| `ES_DELETE_ROLE(name)` | Delete a role | `ES_DELETE_ROLE('custom-role')` |

### Create a Custom Role

```sql
DECLARE result DOCUMENT;
SET result = ES_CREATE_ROLE('logs_reader', {
    'cluster': ['monitor'],
    'indices': [
        {
            'names': ['logs-*', 'metrics-*'],
            'privileges': ['read', 'view_index_metadata']
        }
    ],
    'applications': [
        {
            'application': 'kibana-.kibana',
            'privileges': ['read'],
            'resources': ['*']
        }
    ]
});
```

## API Key Management

| Function | Description | Example |
|----------|-------------|---------|
| `ES_CREATE_API_KEY(name, roles?, expiration?)` | Create API key | See below |
| `ES_GET_API_KEY(id?, name?)` | Get API key info | `ES_GET_API_KEY('key-id', NULL)` |
| `ES_INVALIDATE_API_KEY(id)` | Revoke API key | `ES_INVALIDATE_API_KEY('key-id')` |

### Create an API Key

```sql
DECLARE api_key DOCUMENT;
SET api_key = ES_CREATE_API_KEY(
    'automation-key',
    {
        'automation_role': {
            'cluster': ['monitor'],
            'indices': [
                {
                    'names': ['logs-*'],
                    'privileges': ['read']
                }
            ]
        }
    },
    '30d'  -- Expires in 30 days
);

PRINT 'API Key ID: ' || api_key['id'];
PRINT 'API Key (save this!): ' || api_key['encoded'];
```

## Privilege Checking

| Function | Description | Example |
|----------|-------------|---------|
| `ES_HAS_PRIVILEGES(privileges)` | Check privileges | See below |
| `ES_GET_PRIVILEGES()` | Get all privileges | `ES_GET_PRIVILEGES()` |

### Check User Permissions

```sql
DECLARE can_access DOCUMENT;
SET can_access = ES_HAS_PRIVILEGES({
    'cluster': ['monitor'],
    'index': [
        {
            'names': ['logs-*'],
            'privileges': ['read', 'write']
        }
    ]
});

IF can_access['has_all_requested'] THEN
    PRINT 'User has all required permissions';
ELSE
    PRINT 'Missing permissions detected';
END IF;
```

## Security Event Investigation

### Find Failed Logins

```sql
ESQL FROM .security-* 
| WHERE @timestamp > NOW() - 24 HOURS
| WHERE event.action = 'authentication_failed'
| STATS count = COUNT(*) BY user.name, source.ip
| SORT count DESC;
```

### API Key Usage

```sql
ESQL FROM .security-* 
| WHERE @timestamp > NOW() - 7 DAYS
| WHERE authentication.api_key.id IS NOT NULL
| STATS count = COUNT(*) BY authentication.api_key.name
| SORT count DESC;
```

### Privilege Escalation Attempts

```sql
ESQL FROM .security-* 
| WHERE @timestamp > NOW() - 24 HOURS
| WHERE event.action IN ('access_denied', 'tampered_request')
| KEEP @timestamp, user.name, source.ip, event.action, message
| SORT @timestamp DESC;
```

## Kibana Detection Functions

| Function | Description |
|----------|-------------|
| `KIBANA_DETECTION_RULES()` | List detection rules |
| `KIBANA_DETECTION_ALERTS(status?)` | Get security alerts |
| `KIBANA_DETECTION_CREATE_RULE(config)` | Create detection rule |

## Pre-built Skills (Moltler)

| Skill | Description |
|-------|-------------|
| `RUN SKILL list_users()` | List all users |
| `RUN SKILL create_readonly_user(username, indices)` | Create read-only user |
| `RUN SKILL rotate_api_key(key_name)` | Rotate an API key |
| `RUN SKILL audit_user_permissions(username)` | Audit user access |

## Security Best Practices

1. **Use least privilege** - Grant minimum required permissions
2. **Set API key expiration** - Always set expiration for API keys
3. **Use roles, not direct privileges** - Easier to manage and audit
4. **Audit regularly** - Review user access and API key usage
5. **Enable audit logging** - Track security events

## Common Security Tasks

### Audit All API Keys

```sql
DECLARE keys DOCUMENT;
SET keys = ES_GET_API_KEY(NULL, NULL);

FOR key IN keys['api_keys'] LOOP
    PRINT 'Key: ' || key['name'];
    PRINT '  Created: ' || key['creation'];
    PRINT '  Expires: ' || COALESCE(key['expiration'], 'Never');
    PRINT '  Invalidated: ' || key['invalidated'];
END LOOP;
```

### Revoke All Keys for a User

```sql
DECLARE keys DOCUMENT;
SET keys = ES_GET_API_KEY(NULL, NULL);

FOR key IN keys['api_keys'] LOOP
    IF key['username'] = 'compromised-user' AND key['invalidated'] = FALSE THEN
        ES_INVALIDATE_API_KEY(key['id']);
        PRINT 'Revoked: ' || key['name'];
    END IF;
END LOOP;
```

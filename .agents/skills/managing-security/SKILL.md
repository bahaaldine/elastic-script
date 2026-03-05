---
name: managing-security
description: >
  Manage users, roles, API keys, and investigate security events. Use when
  managing access control, creating or revoking API keys, checking permissions,
  or investigating failed logins and security incidents.
---

# Security Operations

## User Management

| Function | Example |
|----------|---------|
| `ES_GET_USERS(username?)` | `ES_GET_USERS()` |
| `ES_CREATE_USER(username, config)` | See below |
| `ES_DELETE_USER(username)` | `ES_DELETE_USER('olduser')` |
| `ES_AUTHENTICATE()` | Current user |

```sql
SET result = ES_CREATE_USER('analyst', {
    'password': 'secure-password',
    'roles': ['viewer', 'logs_reader'],
    'full_name': 'Security Analyst'
});
```

## Role Management

| Function | Example |
|----------|---------|
| `ES_GET_ROLES(role?)` | `ES_GET_ROLES()` |
| `ES_CREATE_ROLE(name, config)` | See below |
| `ES_DELETE_ROLE(name)` | `ES_DELETE_ROLE('custom-role')` |

```sql
SET result = ES_CREATE_ROLE('logs_reader', {
    'cluster': ['monitor'],
    'indices': [{'names': ['logs-*'], 'privileges': ['read']}]
});
```

## API Key Management

| Function | Example |
|----------|---------|
| `ES_CREATE_API_KEY(name, roles?, expiration?)` | See below |
| `ES_GET_API_KEY(id?, name?)` | `ES_GET_API_KEY('key-id', NULL)` |
| `ES_INVALIDATE_API_KEY(id)` | `ES_INVALIDATE_API_KEY('key-id')` |

```sql
SET api_key = ES_CREATE_API_KEY('automation-key', 
    {'role': {'cluster': ['monitor'], 'indices': [{'names': ['logs-*'], 'privileges': ['read']}]}},
    '30d'
);
PRINT 'Key: ' || api_key['encoded'];
```

## Privilege Checking

| Function | Example |
|----------|---------|
| `ES_HAS_PRIVILEGES(privileges)` | Check permissions |
| `ES_GET_PRIVILEGES()` | All privileges |

## Security Event Investigation

```sql
-- Failed logins
ESQL FROM .security-* 
| WHERE @timestamp > NOW() - 24 HOURS AND event.action = 'authentication_failed'
| STATS count = COUNT(*) BY user.name, source.ip
| SORT count DESC;

-- API key usage
ESQL FROM .security-* 
| WHERE @timestamp > NOW() - 7 DAYS AND authentication.api_key.id IS NOT NULL
| STATS count = COUNT(*) BY authentication.api_key.name;

-- Access denied
ESQL FROM .security-* 
| WHERE @timestamp > NOW() - 24 HOURS AND event.action IN ('access_denied', 'tampered_request')
| KEEP @timestamp, user.name, source.ip, event.action;
```

## Pre-built Skills

| Skill | Description |
|-------|-------------|
| `RUN SKILL list_users()` | List users |
| `RUN SKILL create_readonly_user(username, indices)` | Read-only user |
| `RUN SKILL rotate_api_key(key_name)` | Rotate key |
| `RUN SKILL audit_user_permissions(username)` | Audit access |

## Best Practices

- Use least privilege
- Set API key expiration
- Use roles, not direct privileges
- Enable audit logging

# Security & Permissions

elastic-script provides a role-based permission system to control access to procedures, functions, packages, jobs, and triggers.

## Overview

The permission system allows you to:

- **Create roles** - Named groups for organizing access control
- **Grant permissions** - Allow roles/users to execute objects
- **Revoke permissions** - Remove previously granted access
- **Audit permissions** - View current permission grants

## Creating Roles

Roles are named groups that permissions can be granted to. Create roles to organize access control by team, function, or responsibility.

```sql
-- Basic role creation
CREATE ROLE data_analysts

-- Role with description
CREATE ROLE operators DESCRIPTION 'Operations team for runbook execution'

-- More examples
CREATE ROLE developers DESCRIPTION 'Development team access'
CREATE ROLE admin DESCRIPTION 'Administrative access to all resources'
```

## Granting Permissions

Use GRANT to give permissions to roles or users.

### Syntax

```sql
GRANT privilege_list ON object_type object_name TO principal
```

### Examples

```sql
-- Grant execute on a procedure to a role
GRANT EXECUTE ON PROCEDURE restart_service TO ROLE operators

-- Grant execute on a function
GRANT EXECUTE ON FUNCTION calculate_metrics TO ROLE analysts

-- Grant to a specific user
GRANT EXECUTE ON PROCEDURE backup_data TO USER 'john.doe@example.com'

-- Grant all privileges on a package
GRANT ALL PRIVILEGES ON PACKAGE admin_tools TO ROLE admin

-- Grant on jobs and triggers
GRANT EXECUTE ON JOB daily_cleanup TO ROLE scheduler
GRANT EXECUTE ON TRIGGER alert_handler TO ROLE monitoring
```

## Revoking Permissions

Use REVOKE to remove previously granted permissions.

### Syntax

```sql
REVOKE privilege_list ON object_type object_name FROM principal
```

### Examples

```sql
-- Revoke from a role
REVOKE EXECUTE ON PROCEDURE sensitive_proc FROM ROLE guest

-- Revoke from a user
REVOKE EXECUTE ON FUNCTION calc FROM USER 'former.employee@example.com'

-- Revoke all privileges
REVOKE ALL PRIVILEGES ON PACKAGE admin_tools FROM ROLE deprecated_role
```

## Viewing Permissions

### Show All Permissions

```sql
SHOW PERMISSIONS
```

Returns all permission grants in the system.

### Show Permissions for a Principal

```sql
-- For a role
SHOW PERMISSIONS FOR ROLE operators

-- For a user
SHOW PERMISSIONS FOR USER 'john@example.com'
```

### Show Role Details

```sql
SHOW ROLE admin
```

## Managing Roles

### Drop a Role

```sql
DROP ROLE obsolete_role
```

Note: Dropping a role does not automatically revoke its permissions. Revoke permissions before dropping the role.

## Object Types

| Object Type | Description |
|-------------|-------------|
| `PROCEDURE` | Stored procedures created with CREATE PROCEDURE |
| `FUNCTION` | User-defined functions created with CREATE FUNCTION |
| `PACKAGE` | Packages (includes all procedures and functions within) |
| `JOB` | Scheduled jobs created with CREATE JOB |
| `TRIGGER` | Event triggers created with CREATE TRIGGER |

## Privileges

| Privilege | Description |
|-----------|-------------|
| `EXECUTE` | Permission to execute (call) the object |
| `ALL PRIVILEGES` | All available permissions for the object type |

## Principal Types

| Principal | Description | Example |
|-----------|-------------|---------|
| `ROLE rolename` | Named role | `ROLE operators` |
| `USER 'email'` | Specific user by email/username | `USER 'john@example.com'` |

## Storage

Permissions are stored in the `.escript_permissions` index with the following structure:

- **id**: Unique identifier (principal:object)
- **principal_type**: ROLE or USER
- **principal_name**: Role name or username
- **object_type**: PROCEDURE, FUNCTION, PACKAGE, JOB, or TRIGGER
- **object_name**: Name of the protected object
- **privileges**: Array of granted privileges
- **granted_at**: Timestamp of grant
- **granted_by**: User who granted the permission

Roles are stored in the `.escript_roles` index.

## Best Practices

1. **Use roles, not individual users** - Assign permissions to roles, then assign users to roles
2. **Principle of least privilege** - Grant only the minimum permissions needed
3. **Document role purposes** - Use descriptions when creating roles
4. **Audit regularly** - Use SHOW PERMISSIONS to review access
5. **Clean up unused roles** - Drop roles that are no longer needed

## Example: Setting Up Team Access

```sql
-- Create roles for different teams
CREATE ROLE ops_team DESCRIPTION 'Operations team'
CREATE ROLE dev_team DESCRIPTION 'Development team'
CREATE ROLE readonly DESCRIPTION 'Read-only access'

-- Grant appropriate access
GRANT EXECUTE ON PACKAGE runbooks TO ROLE ops_team
GRANT EXECUTE ON PACKAGE dev_tools TO ROLE dev_team
GRANT EXECUTE ON FUNCTION get_metrics TO ROLE readonly
GRANT EXECUTE ON FUNCTION get_logs TO ROLE readonly

-- View permissions
SHOW PERMISSIONS FOR ROLE ops_team
```

## See Also

- [Packages](packages.md) - Grouping procedures and functions
- [Jobs & Triggers](jobs-triggers.md) - Scheduled and event-driven execution
- [Procedures](procedures.md) - Creating stored procedures

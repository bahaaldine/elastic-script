# Skill Packs

Skill Packs bundle related skills together for easy distribution and installation.

## What is a Skill Pack?

A Skill Pack is a collection of skills that work together to solve a category of problems:

```sql
CREATE SKILL PACK incident_response
VERSION '1.0.0'
DESCRIPTION 'Complete incident response automation'
AUTHOR 'sre-team'
SKILLS [
    detect_incident@1.0.0,
    triage_incident@1.0.0,
    notify_oncall@2.0.0,
    create_postmortem@1.0.0,
    track_resolution@1.0.0
];
```

---

## Creating a Skill Pack

### Basic Pack

```sql
CREATE SKILL PACK pack_name
VERSION 'semver'
DESCRIPTION 'description'
SKILLS [skill1, skill2, skill3];
```

### Complete Pack

```sql
CREATE SKILL PACK kubernetes_ops
VERSION '1.0.0'
DESCRIPTION 'Kubernetes operations automation'
AUTHOR 'platform-team'
TAGS ['kubernetes', 'devops', 'automation']
LICENSE 'Apache-2.0'
HOMEPAGE 'https://github.com/myorg/k8s-skills'

-- Skills with version constraints
SKILLS [
    check_pod_health@^1.0.0,      -- Any 1.x version
    scale_deployment@~1.2.0,      -- Any 1.2.x version
    rollback_deployment@1.0.0,    -- Exact version
    restart_pods@1.0.0,
    get_pod_logs@1.0.0
]

-- Pack-level configuration
CONFIG {
    "default_namespace": "default",
    "timeout_seconds": 300
}

-- Pack documentation
DOCUMENTATION '''
# Kubernetes Ops Pack

This pack provides skills for common Kubernetes operations.

## Installation

```sql
INSTALL SKILL PACK kubernetes_ops FROM 'marketplace';
```

## Quick Start

```sql
-- Check all pods in a namespace
CALL check_pod_health(namespace => 'production');

-- Scale a deployment
CALL scale_deployment(
    deployment => 'api-server',
    replicas => 5
);
```
''';
```

---

## Version Constraints

Specify version requirements for included skills:

| Constraint | Meaning | Example |
|------------|---------|---------|
| `1.0.0` | Exact version | Only 1.0.0 |
| `^1.0.0` | Compatible | 1.0.0, 1.1.0, 1.9.9 (not 2.0.0) |
| `~1.2.0` | Approximate | 1.2.0, 1.2.1, 1.2.9 (not 1.3.0) |
| `>=1.0.0` | Minimum | 1.0.0 or higher |
| `>=1.0.0 <2.0.0` | Range | Between 1.0.0 and 2.0.0 |

```sql
CREATE SKILL PACK my_pack
VERSION '1.0.0'
SKILLS [
    core_skill@^1.0.0,           -- Any compatible 1.x
    stable_skill@1.0.0,          -- Exact version
    flexible_skill@>=2.0.0       -- Any 2.0+ version
];
```

---

## Installing Packs

### From Marketplace

```sql
INSTALL SKILL PACK kubernetes_ops FROM 'marketplace';
```

### From Team Registry

```sql
INSTALL SKILL PACK internal_tools FROM 'team';
```

### Specific Version

```sql
INSTALL SKILL PACK kubernetes_ops@1.2.0 FROM 'marketplace';
```

### With Dependencies

```sql
INSTALL SKILL PACK kubernetes_ops
FROM 'marketplace'
WITH DEPENDENCIES;  -- Also installs required skills
```

---

## Managing Packs

### List Installed Packs

```sql
SHOW SKILL PACKS;
```

Output:

```
┌───────────────────┬─────────┬──────────────────────────────────┐
│ Name              │ Version │ Description                      │
├───────────────────┼─────────┼──────────────────────────────────┤
│ kubernetes_ops    │ 1.0.0   │ Kubernetes operations automation │
│ incident_response │ 2.1.0   │ Complete incident response       │
│ log_analysis      │ 1.5.0   │ Log analysis and alerting        │
└───────────────────┴─────────┴──────────────────────────────────┘
```

### View Pack Details

```sql
SHOW SKILL PACK kubernetes_ops;
```

Output:

```
┌─────────────────────────────────────────────────────────────┐
│ Skill Pack: kubernetes_ops                                   │
├─────────────────────────────────────────────────────────────┤
│ Version:     1.0.0                                           │
│ Author:      platform-team                                   │
│ License:     Apache-2.0                                      │
│ Description: Kubernetes operations automation                │
├─────────────────────────────────────────────────────────────┤
│ Included Skills:                                             │
│   • check_pod_health@1.2.0                                  │
│   • scale_deployment@1.2.3                                  │
│   • rollback_deployment@1.0.0                               │
│   • restart_pods@1.0.0                                      │
│   • get_pod_logs@1.0.0                                      │
├─────────────────────────────────────────────────────────────┤
│ Configuration:                                               │
│   • default_namespace: default                               │
│   • timeout_seconds: 300                                     │
└─────────────────────────────────────────────────────────────┘
```

### Update Pack

```sql
UPDATE SKILL PACK kubernetes_ops TO VERSION '1.1.0';
```

### Uninstall Pack

```sql
UNINSTALL SKILL PACK kubernetes_ops;
```

---

## Pack Configuration

### Default Configuration

```sql
CREATE SKILL PACK my_pack
VERSION '1.0.0'
SKILLS [...]
CONFIG {
    "api_endpoint": "https://api.example.com",
    "timeout_seconds": 30,
    "retry_count": 3
};
```

### Override Configuration

```sql
-- Set pack-wide config
SET SKILL PACK kubernetes_ops CONFIG {
    "default_namespace": "production",
    "timeout_seconds": 600
};

-- Individual skill config
SET SKILL check_pod_health FROM PACK kubernetes_ops CONFIG {
    "verbose": true
};
```

---

## Pack Workflows

Packs can define workflows that orchestrate multiple skills:

```sql
CREATE SKILL PACK incident_response
VERSION '1.0.0'
SKILLS [
    detect_incident,
    triage_incident,
    notify_oncall,
    create_postmortem
]

-- Define a workflow
WORKFLOW respond_to_incident(incident_id STRING)
BEGIN
    -- Step 1: Detect and gather info
    DECLARE info = CALL detect_incident(incident_id);
    
    -- Step 2: Triage
    DECLARE severity = CALL triage_incident(info);
    
    -- Step 3: Notify if severe
    IF severity IN ('high', 'critical') THEN
        CALL notify_oncall(incident_id, severity);
    END IF;
    
    -- Step 4: Create postmortem template
    CALL create_postmortem(incident_id, info, severity);
END WORKFLOW;
```

Use the workflow:

```sql
CALL WORKFLOW respond_to_incident('INC-123');
```

---

## Publishing Packs

### Validate

```sql
VALIDATE SKILL PACK kubernetes_ops FOR PUBLISHING;
```

### Publish

```sql
PUBLISH SKILL PACK kubernetes_ops
TO 'marketplace'
WITH visibility = 'public';
```

### Update Published Pack

```sql
-- Create new version
CREATE SKILL PACK kubernetes_ops
VERSION '1.1.0'
SKILLS [
    check_pod_health@^1.0.0,
    scale_deployment@~1.2.0,
    rollback_deployment@1.0.0,
    restart_pods@1.0.0,
    get_pod_logs@1.0.0,
    drain_node@1.0.0  -- New skill added
];

-- Publish update
PUBLISH SKILL PACK kubernetes_ops VERSION '1.1.0'
TO 'marketplace';
```

---

## Example Packs

### Observability Pack

```sql
CREATE SKILL PACK observability_essentials
VERSION '1.0.0'
DESCRIPTION 'Essential observability skills'
SKILLS [
    analyze_logs@1.0.0,
    detect_anomalies@1.0.0,
    create_dashboard@1.0.0,
    setup_alerts@1.0.0,
    generate_report@1.0.0
]
TAGS ['observability', 'monitoring', 'alerting'];
```

### Security Pack

```sql
CREATE SKILL PACK security_automation
VERSION '1.0.0'
DESCRIPTION 'Security monitoring and response'
SKILLS [
    detect_threats@1.0.0,
    analyze_vulnerabilities@1.0.0,
    quarantine_host@1.0.0,
    rotate_credentials@1.0.0,
    generate_compliance_report@1.0.0
]
TAGS ['security', 'compliance', 'automation'];
```

### DevOps Pack

```sql
CREATE SKILL PACK devops_toolkit
VERSION '1.0.0'
DESCRIPTION 'DevOps automation toolkit'
SKILLS [
    deploy_application@1.0.0,
    run_tests@1.0.0,
    rollback_deployment@1.0.0,
    scale_infrastructure@1.0.0,
    backup_database@1.0.0
]
TAGS ['devops', 'ci-cd', 'automation'];
```

---

## Best Practices

### 1. Cohesive Skills

Include skills that work together:

```sql
-- Good: related skills
CREATE SKILL PACK database_ops
SKILLS [
    backup_database,
    restore_database,
    analyze_slow_queries,
    optimize_indexes
];

-- Bad: unrelated skills
CREATE SKILL PACK random_stuff
SKILLS [
    send_email,
    backup_database,
    analyze_logs,
    scale_kubernetes
];
```

### 2. Pin Critical Versions

```sql
SKILLS [
    critical_skill@1.0.0,    -- Exact for stability
    flexible_skill@^2.0.0    -- Range for updates
];
```

### 3. Document Dependencies

```sql
CREATE SKILL PACK my_pack
VERSION '1.0.0'
DOCUMENTATION '''
## Prerequisites

- Elasticsearch 8.x
- OpenAI API key configured
- Kubernetes cluster access

## Required Connectors

This pack requires the following connectors:
- `kubernetes` connector configured
- `slack` connector for notifications
'''
SKILLS [...];
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-connection:{ .lg .middle } __Connectors__

    Add external data sources.

    [:octicons-arrow-right-24: Connectors](../connectors/overview.md)

-   :material-robot:{ .lg .middle } __Agents__

    Create autonomous agents using skills.

    [:octicons-arrow-right-24: Agents](../agents/overview.md)

</div>

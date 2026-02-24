# Elastic Workflows Integration

Moltler integrates with [Elastic Workflows](https://www.elastic.co/docs/explore-analyze/workflows), enabling you to trigger, manage, and install workflows directly from skills.

## Overview

Elastic Workflows is a Kibana platform feature for automating operations across the Elastic Stack using declarative YAML definitions. Moltler provides:

1. **Built-in Functions** - `WORKFLOW_TRIGGER()`, `WORKFLOW_LIST()`, `WORKFLOW_CREATE()`, etc.
2. **Management Skills** - `list_workflows`, `trigger_workflow`, `install_workflow`
3. **Pre-built Workflow Templates** - Install common workflows with one command

## Quick Start

### List Available Workflows

```sql
RUN SKILL list_workflows()
```

### Trigger a Workflow

```sql
RUN SKILL trigger_workflow(
  workflow_id => 'ip-reputation-check',
  inputs => {'ip_address': '8.8.8.8'}
)
```

### Install a Pre-built Workflow

```sql
-- Install IP Reputation workflow (requires AbuseIPDB API key)
RUN SKILL install_ip_reputation_workflow(
  abuseipdb_api_key => 'your-api-key'
)

-- Install Slack Alert workflow
RUN SKILL install_slack_alert_workflow(
  slack_webhook_url => 'https://hooks.slack.com/services/xxx/yyy/zzz'
)

-- Install ES|QL Report workflow (scheduled)
RUN SKILL install_esql_report_workflow(
  workflow_name => 'Daily Error Summary',
  esql_query => 'FROM logs-* | WHERE log.level == "ERROR" | STATS count = COUNT(*) BY service.name',
  schedule => '1d'
)
```

## Built-in Functions

### WORKFLOW_TRIGGER

Trigger a workflow by ID or name with input parameters.

```sql
SET result = WORKFLOW_TRIGGER(
  'workflow-id',
  {'param1': 'value1', 'param2': 'value2'},
  'http://kibana:5601'  -- optional
);
```

**Returns:** `{execution_id, status, workflow_id, success}`

### WORKFLOW_LIST

List all available workflows in Kibana.

```sql
SET workflows = WORKFLOW_LIST();
SET workflows = WORKFLOW_LIST('http://kibana:5601');
```

**Returns:** Array of workflow objects with `id`, `name`, `description`, `enabled`, `tags`

### WORKFLOW_GET

Get details of a specific workflow.

```sql
SET workflow = WORKFLOW_GET('workflow-id');
```

**Returns:** Complete workflow definition including steps, triggers, inputs

### WORKFLOW_STATUS

Get the status of a workflow execution.

```sql
SET status = WORKFLOW_STATUS('execution-id');
```

**Returns:** `{execution_id, status, started_at, completed_at, duration_ms, steps, error}`

### WORKFLOW_CREATE

Create a new workflow from YAML definition.

```sql
SET result = WORKFLOW_CREATE('
name: My Workflow
steps:
  - name: log
    type: console
    with:
      message: "Hello from workflow!"
');
```

**Returns:** `{id, name, success}`

### WORKFLOW_DELETE

Delete a workflow by ID.

```sql
SET result = WORKFLOW_DELETE('workflow-id');
```

**Returns:** `{deleted, success, workflow_id}`

## Workflow Skills

| Skill | Description |
|-------|-------------|
| `list_workflows` | List all available workflows |
| `trigger_workflow` | Trigger a workflow with inputs |
| `install_workflow` | Install workflow from YAML |
| `workflow_status` | Check execution status |
| `install_ip_reputation_workflow` | Install AbuseIPDB IP check workflow |
| `install_slack_alert_workflow` | Install Slack notification workflow |
| `install_esql_report_workflow` | Install scheduled ES\|QL report |

## Workflow YAML Syntax

Elastic Workflows use YAML with these key sections:

```yaml
name: "Workflow Name"           # Required
description: "What it does"     # Optional
enabled: true                   # Optional
tags: ["category", "type"]      # Optional

# Constants - reusable values
consts:
  api_url: "https://api.example.com"
  api_key: "your-key"

# Inputs - runtime parameters
inputs:
  - name: target_ip
    type: string
    required: true
    default: "8.8.8.8"

# Triggers - when workflow runs
triggers:
  - type: manual              # On-demand
  - type: scheduled           # Recurring
    with:
      every: "6h"             # Every 6 hours
  - type: alert               # On security alert

# Steps - the actions
steps:
  - name: step_name
    type: action_type
    with:
      param: "{{ inputs.target_ip }}"
      const: "{{ consts.api_url }}"
    on-failure:
      retry:
        max-attempts: 3
        delay: 5s
      continue: true
```

### Step Types

| Type | Description |
|------|-------------|
| `console` | Log a message |
| `http` | Make HTTP requests |
| `elasticsearch.search` | Search Elasticsearch |
| `elasticsearch.esql.query` | Run ES\|QL query |
| `elasticsearch.index` | Index documents |
| `kibana.cases` | Create/update cases |
| `foreach` | Loop over arrays |
| `if` | Conditional logic |
| `wait` | Pause execution |
| `data.set` | Set variables |

### Liquid Templating

Workflows support Liquid templating:

```yaml
# Reference values
url: "{{ consts.api_url }}/{{ inputs.id }}"
message: "Found {{ steps.search.output.hits.total }} results"

# Conditionals
{% if steps.count.output.value > 100 %}
  High volume detected
{% endif %}

# Loops
{% for item in steps.search.output.hits.hits %}
  - {{ item._source.name }}
{% endfor %}

# Filters
{{ inputs.name | upcase }}
{{ data | json }}
{{ items | size }}
{{ text | default: "fallback" }}
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KIBANA_URL` | Kibana base URL | `http://localhost:5601` |
| `KIBANA_API_KEY` | API key for authentication | - |
| `ELASTIC_USER` | Basic auth username | - |
| `ELASTIC_PASSWORD` | Basic auth password | - |

### Authentication

Workflows API requires authentication. Configure one of:

1. **API Key** (recommended)
   ```bash
   export KIBANA_API_KEY="your-api-key"
   ```

2. **Basic Auth**
   ```bash
   export ELASTIC_USER="elastic"
   export ELASTIC_PASSWORD="password"
   ```

## Examples

### Example 1: Security Alert Enrichment

```sql
-- Install IP reputation workflow
RUN SKILL install_ip_reputation_workflow(
  abuseipdb_api_key => 'your-key'
);

-- When an alert fires, trigger enrichment
DECLARE alert_ip STRING;
SET alert_ip = '192.168.1.100';

RUN SKILL trigger_workflow(
  workflow_id => 'IP Reputation Check',
  inputs => {'ip_address': alert_ip}
);
```

### Example 2: Scheduled Reporting

```sql
-- Create a daily error summary report
RUN SKILL install_esql_report_workflow(
  workflow_name => 'Daily Error Summary',
  esql_query => '
    FROM logs-*
    | WHERE log.level == "ERROR" AND @timestamp > NOW() - 24 hours
    | STATS error_count = COUNT(*) BY service.name
    | SORT error_count DESC
    | LIMIT 10
  ',
  schedule => '1d'
);
```

### Example 3: Incident Response

```sql
-- List workflows to find incident response
DECLARE workflows ARRAY;
SET workflows = RUN SKILL list_workflows();

-- Trigger incident workflow
RUN SKILL trigger_workflow(
  workflow_id => 'incident-triage',
  inputs => {
    'alert_id': 'alert-123',
    'severity': 'high',
    'assign_to': 'sre-team'
  }
);

-- Check execution status
DECLARE exec_result DOCUMENT;
SET exec_result = RUN SKILL workflow_status(execution_id => 'exec-abc');

IF exec_result.status = 'completed' THEN
  PRINT 'Incident workflow completed successfully';
END IF;
```

## Resources

- [Elastic Workflows Documentation](https://www.elastic.co/docs/explore-analyze/workflows)
- [Workflow Library (GitHub)](https://github.com/elastic/workflows)
- [Workflow Schema Reference](https://github.com/elastic/workflows/blob/main/docs/schema.md)

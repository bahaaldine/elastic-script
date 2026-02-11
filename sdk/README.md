# Moltler Python SDK

The official Python SDK for the **Moltler AI Skills Creation Framework**.

Moltler enables you to create, manage, and orchestrate AI-powered automation skills that integrate with your existing tools and services.

## Installation

```bash
pip install moltler
```

## Quick Start

```python
from moltler import Moltler

# Connect to Moltler
client = Moltler(
    host="localhost",
    port=9200,
    username="elastic-admin",
    password="elastic-password"
)

# Test connection
client.connect()
print(f"Connected to {client.cluster_name}")

# List available skills
for skill in client.skills.list():
    print(f"  - {skill.name} v{skill.version}")
```

## Managing Skills

Skills are reusable automation components that encapsulate complex operations.

### List Skills

```python
skills = client.skills.list()
for skill in skills:
    print(f"{skill.name}: {skill.description}")
```

### Create a Skill

```python
from moltler import SkillBuilder

# Use the builder pattern
builder = SkillBuilder("analyze_logs")
builder.version("1.0.0")
builder.description("Analyze logs for anomalies")
builder.author("DevOps Team")
builder.tag("observability", "logs")
builder.parameter("index", "STRING", description="Index pattern to analyze")
builder.parameter("threshold", "NUMBER", default=0.9)
builder.returns("DOCUMENT")
builder.body('''
    DECLARE results DOCUMENT;
    SET results = ESQL_QUERY('FROM ' || index || ' | STATS count=COUNT(*)');
    RETURN results;
''')

skill = client.skills.create_from_builder(builder)
print(f"Created skill: {skill.name}")
```

### Execute a Skill

```python
result = client.skills.execute("analyze_logs", index="logs-*", threshold=0.95)
print(result)
```

### Test a Skill

```python
test_result = client.skills.test(
    "analyze_logs",
    params={"index": "logs-*"},
    expect={"status": "success"}
)
print(f"Test passed: {test_result['passed']}")
```

### Generate a Skill with AI

```python
skill = client.skills.generate(
    goal="Create a skill that monitors disk usage and alerts when over 80%",
    name="disk_monitor"
)
print(f"Generated: {skill.name}")
```

## Managing Connectors

Connectors enable integration with external services.

### List Connectors

```python
connectors = client.connectors.list()
for conn in connectors:
    print(f"{conn.name} ({conn.type}): {'enabled' if conn.enabled else 'disabled'}")
```

### Create a Connector

```python
from moltler import ConnectorConfig

config = ConnectorConfig(
    type="github",
    token="ghp_xxxx",
    organization="my-org"
)

connector = client.connectors.create(
    name="my_github",
    connector_type="github",
    config=config,
    description="GitHub integration for my-org",
    target_index="github-data"
)
```

### Test Connectivity

```python
result = client.connectors.test("my_github")
if result.get("connected"):
    print("Connection successful!")
```

### Sync Data

```python
# Sync all entities
sync_result = client.connectors.sync("my_github")
print(f"Synced {sync_result.docs_synced} documents")

# Sync specific entity
sync_result = client.connectors.sync("my_github", entity="issues")
```

### Query Connector Data

```python
# Query issues from Jira
issues = client.connectors.query(
    "my_jira",
    "issues",
    where="status = 'Open' AND priority = 'High'",
    limit=50,
    order_by="created",
    ascending=False
)

for issue in issues:
    print(f"[{issue['key']}] {issue['summary']}")
```

### Execute Actions

```python
# Create a GitHub issue
result = client.connectors.exec(
    "my_github",
    "create_issue",
    owner="my-org",
    repo="my-repo",
    title="Bug: Something is broken",
    body="Description of the issue..."
)
print(f"Created issue #{result['number']}")
```

## Managing Agents

Agents are autonomous entities that execute skills based on goals and triggers.

### List Agents

```python
agents = client.agents.list()
for agent in agents:
    print(f"{agent.name}: {agent.goal}")
    print(f"  Mode: {agent.execution_mode}")
    print(f"  Enabled: {agent.enabled}")
```

### Create an Agent

```python
from moltler.agents import AgentBuilder

builder = AgentBuilder("incident_responder")
builder.goal("Automatically respond to production incidents")
builder.skill("diagnose_issue", mode="approval")
builder.skill("notify_team")
builder.skill("create_ticket")
builder.execution_mode("supervised")
builder.trigger_on_alert("HighCPUUsage")
builder.trigger_on_schedule("every 5 minutes")
builder.model("gpt-4")

agent = client.agents.create_from_builder(builder)
print(f"Created agent: {agent.name}")
```

### Trigger an Agent

```python
# Manual trigger
result = client.agents.trigger(
    "incident_responder",
    context={"priority": "high", "source": "manual"}
)
print(f"Execution ID: {result.execution_id}")
print(f"Status: {result.status}")
```

### View Execution History

```python
history = client.agents.get_history("incident_responder", limit=5)
for execution in history:
    print(f"[{execution.execution_id}] {execution.status}")
    print(f"  Started: {execution.started_at}")
    for step in execution.steps:
        print(f"    - {step.phase}: {step.data.get('action', 'N/A')}")
```

### Enable/Disable Agents

```python
# Disable an agent
client.agents.disable("incident_responder")

# Enable an agent
client.agents.enable("incident_responder")

# Change execution mode
client.agents.set_execution_mode("incident_responder", "dry_run")
```

## Raw Queries

Execute raw elastic-script statements:

```python
result = client.execute("SHOW PROCEDURES")
print(result.data)

# With arguments
result = client.execute("CALL my_procedure()", args={"param1": "value1"})
```

## Error Handling

```python
from moltler import (
    MoltlerError,
    ConnectionError,
    SkillNotFoundError,
    ExecutionError
)

try:
    skill = client.skills.get("nonexistent")
except SkillNotFoundError as e:
    print(f"Skill not found: {e.skill_name}")
except ConnectionError as e:
    print(f"Connection failed: {e}")
except ExecutionError as e:
    print(f"Execution failed: {e}")
    print(f"Statement: {e.statement}")
except MoltlerError as e:
    print(f"Moltler error: {e}")
```

## Context Manager

```python
# Automatic connection and cleanup
with Moltler(host="localhost") as client:
    skills = client.skills.list()
    print(f"Found {len(skills)} skills")
# Connection automatically closed
```

## Environment Variables

The SDK supports configuration via environment variables:

- `MOLTLER_HOST` - Elasticsearch host (default: localhost)
- `MOLTLER_PORT` - Elasticsearch port (default: 9200)
- `MOLTLER_USERNAME` - Username (default: elastic-admin)
- `MOLTLER_PASSWORD` - Password (default: elastic-password)
- `MOLTLER_USE_SSL` - Use SSL/TLS (default: false)

```python
import os
os.environ["MOLTLER_HOST"] = "my-es-cluster.example.com"
os.environ["MOLTLER_PASSWORD"] = "secret"

# Uses environment variables
client = Moltler()
```

## License

Apache License 2.0

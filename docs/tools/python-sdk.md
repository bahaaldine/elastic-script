# Python SDK

The Moltler Python SDK provides a high-level Python interface for managing skills, connectors, and agents.

## Installation

```bash
# From the sdk directory
cd elastic-script/sdk
pip install -e .

# Or install from pip (when published)
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

# List skills
for skill in client.skills.list():
    print(f"{skill.name} v{skill.version}")

# Execute a skill
result = client.skills.execute("check_health", service="api-gateway")
print(result)

# Query a connector
issues = client.connectors.query(
    "github_ops",
    "issues",
    where="state = 'open'",
    limit=10
)
for issue in issues:
    print(f"{issue['number']}: {issue['title']}")
```

---

## Skills API

### List Skills

```python
skills = client.skills.list()

for skill in skills:
    print(f"{skill.name} v{skill.version}: {skill.description}")
```

### Get Skill Details

```python
skill = client.skills.get("check_health")

print(f"Parameters: {skill.parameters}")
print(f"Returns: {skill.return_type}")
print(f"Tests: {len(skill.tests)} defined")
```

### Call Skill

```python
# Positional parameters
result = client.skills.call("check_health", "api-gateway")

# Named parameters
result = client.skills.call(
    "analyze_logs",
    index_pattern="logs-*",
    time_range="24h",
    min_occurrences=10
)

# Async call
import asyncio

async def check_all_services():
    tasks = [
        client.skills.call_async("check_health", service=s)
        for s in ["api", "web", "worker"]
    ]
    return await asyncio.gather(*tasks)

results = asyncio.run(check_all_services())
```

### Create Skill

```python
client.skills.create("""
CREATE SKILL my_python_skill
VERSION '1.0.0'
DESCRIPTION 'Created from Python'
PARAMETERS (name STRING)
RETURNS STRING
BEGIN
    RETURN 'Hello, ' || name || '!';
END SKILL;
""")
```

### Generate Skill with AI

```python
skill_code = client.skills.generate(
    description="Monitor disk usage and alert when above 80%",
    parameters={
        "threshold_percent": {"type": "number", "default": 80}
    },
    include_tests=True
)

print(skill_code)
client.skills.create(skill_code)
```

---

## Connectors API

### List Connectors

```python
connectors = client.connectors.list()

for conn in connectors:
    print(f"{conn.name} ({conn.type}): {conn.status}")
```

### Query Connector

```python
# Simple query
issues = client.connectors.query("github_org.issues")

# With filters
issues = client.connectors.query(
    "github_org.issues",
    where={
        "state": "open",
        "labels": {"contains": "bug"}
    },
    order_by="created_at DESC",
    limit=50
)

# Iterate results
for issue in issues:
    print(issue)
```

### Execute Action

```python
result = client.connectors.execute(
    "github_org.issues.create",
    {
        "repo": "myrepo",
        "title": "Issue from Python SDK",
        "body": "Created programmatically",
        "labels": ["automated"]
    }
)

print(f"Created issue: {result['number']}")
```

### Create Connector

```python
client.connectors.create(
    name="my_github",
    type="github",
    config={
        "token": "{{secrets.github_token}}",
        "org": "mycompany"
    }
)
```

---

## Agents API

### List Agents

```python
agents = client.agents.list()

for agent in agents:
    print(f"{agent.name}: {agent.status}")
```

### Start/Stop Agent

```python
client.agents.start("production_guardian")
client.agents.stop("production_guardian")
```

### Trigger Agent

```python
# Manual trigger
execution = client.agents.trigger("incident_responder", {
    "alert_id": "alert-123",
    "severity": "high"
})

print(f"Execution ID: {execution.id}")
```

### Monitor Execution

```python
execution = client.agents.get_execution("exec-abc123")

print(f"Status: {execution.status}")
print(f"Duration: {execution.duration}")

for step in execution.steps:
    print(f"  {step.skill}: {step.status}")
```

### Handle Approvals

```python
# Get pending approvals
approvals = client.agents.get_pending_approvals()

for approval in approvals:
    print(f"Agent: {approval.agent}")
    print(f"Action: {approval.action}")
    print(f"Details: {approval.details}")
    
    # Approve or reject
    if input("Approve? (y/n): ").lower() == "y":
        client.agents.approve(approval.id)
    else:
        client.agents.reject(approval.id, reason="Not appropriate")
```

---

## Execute Raw elastic-script

```python
# Execute any elastic-script
result = client.execute("""
DECLARE total = 0;
FOR i IN 1..10 LOOP
    SET total = total + i;
END LOOP;
RETURN total;
""")

print(result)  # 55
```

---

## Context Manager

```python
from moltler import Moltler

with Moltler(host="localhost:9200") as client:
    result = client.skills.call("check_health")
    # Connection automatically closed
```

---

## Async Client

```python
from moltler import AsyncMoltler
import asyncio

async def main():
    async with AsyncMoltler(host="localhost:9200") as client:
        # Parallel skill calls
        results = await asyncio.gather(
            client.skills.call("check_health", service="api"),
            client.skills.call("check_health", service="web"),
            client.skills.call("check_health", service="worker"),
        )
        
        for result in results:
            print(result)

asyncio.run(main())
```

---

## Events & Callbacks

```python
# Subscribe to agent events
def on_approval_needed(event):
    print(f"Approval needed: {event.action}")
    # Auto-approve low-risk actions
    if event.risk_level == "low":
        event.approve()

client.events.subscribe("approval_needed", on_approval_needed)

# Subscribe to execution events
def on_execution_complete(event):
    print(f"Execution {event.execution_id} completed: {event.status}")
    if event.status == "failed":
        # Handle failure
        send_alert(event)

client.events.subscribe("execution_complete", on_execution_complete)

# Keep listening
client.events.listen()
```

---

## Error Handling

```python
from moltler import Moltler, MoltlerError, SkillNotFoundError, ExecutionError

try:
    result = client.skills.call("nonexistent_skill")
except SkillNotFoundError as e:
    print(f"Skill not found: {e.skill_name}")
except ExecutionError as e:
    print(f"Execution failed: {e.message}")
    print(f"Details: {e.details}")
except MoltlerError as e:
    print(f"General error: {e}")
```

---

## Configuration

### Environment Variables

```bash
export MOLTLER_HOST=localhost:9200
export MOLTLER_USERNAME=elastic-admin
export MOLTLER_PASSWORD=elastic-password
```

```python
# Automatically uses environment variables
client = Moltler()
```

### Configuration File

```yaml
# ~/.moltler/config.yaml
default:
  host: localhost:9200
  username: elastic-admin
  password: ${MOLTLER_PASSWORD}

production:
  host: es-prod.company.com:9200
  api_key: ${MOLTLER_API_KEY}
```

```python
# Use named profile
client = Moltler(profile="production")
```

---

## Examples

### Health Check Dashboard

```python
from moltler import Moltler

client = Moltler()

def get_system_health():
    # Get health for all services
    services = ["api", "web", "worker", "db"]
    
    results = {}
    for service in services:
        try:
            health = client.skills.call("check_health", service=service)
            results[service] = health
        except Exception as e:
            results[service] = {"status": "error", "error": str(e)}
    
    return results

health = get_system_health()
for service, status in health.items():
    emoji = "✅" if status.get("status") == "healthy" else "❌"
    print(f"{emoji} {service}: {status.get('status')}")
```

### Incident Response Bot

```python
from moltler import Moltler
import slack_sdk

client = Moltler()
slack = slack_sdk.WebClient(token=os.environ["SLACK_TOKEN"])

def handle_incident(alert):
    # Trigger incident responder agent
    execution = client.agents.trigger("incident_responder", {
        "alert": alert,
        "auto_approve_low_risk": True
    })
    
    # Post to Slack
    slack.chat_postMessage(
        channel="#incidents",
        text=f"🚨 Incident detected: {alert['title']}\n"
             f"Agent execution started: {execution.id}"
    )
    
    # Wait for completion
    while execution.status == "running":
        time.sleep(5)
        execution = client.agents.get_execution(execution.id)
    
    # Report outcome
    slack.chat_postMessage(
        channel="#incidents",
        text=f"Incident response complete: {execution.status}\n"
             f"Actions taken: {len(execution.steps)}"
    )

# Listen for alerts
while True:
    alerts = client.connectors.query(
        "datadog.monitors",
        where={"status": "Alert"}
    )
    
    for alert in alerts:
        if not already_handled(alert):
            handle_incident(alert)
    
    time.sleep(60)
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-microsoft-visual-studio-code:{ .lg .middle } __VSCode Extension__

    IDE integration.

    [:octicons-arrow-right-24: VSCode Extension](vscode.md)

-   :material-notebook:{ .lg .middle } __Jupyter Integration__

    Use Moltler in notebooks.

    [:octicons-arrow-right-24: Jupyter](jupyter.md)

</div>

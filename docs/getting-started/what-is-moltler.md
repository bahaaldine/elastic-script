# What is Moltler?

**Moltler** is an AI skills creation framework that transforms how teams build and deploy operational automation on Elasticsearch.

## The Vision

Modern operations teams are drowning in:

- **Scattered knowledge** - Runbooks in Confluence, scripts in GitHub, procedures in people's heads
- **Tool fatigue** - Dozens of monitoring, alerting, and incident tools that don't integrate
- **Manual toil** - Repetitive tasks that consume engineer time
- **AI uncertainty** - Wanting to leverage AI but worried about uncontrolled actions

**Moltler** solves these problems by providing a unified framework to:

1. **Capture operational knowledge as Skills** - Versioned, testable, shareable procedures
2. **Connect to any data source** - Query GitHub, Jira, Datadog, and more with familiar syntax
3. **Deploy autonomous Agents** - Let AI execute skills based on goals, with full observability
4. **Observe everything** - Every action is logged, traced, and auditable

---

## Key Concepts

### Skills

A **Skill** is a reusable unit of operational knowledge:

```sql
CREATE SKILL diagnose_memory_leak
VERSION '1.0.0'
DESCRIPTION 'Identifies processes causing memory leaks'
PARAMETERS (
    host STRING,
    threshold_gb NUMBER DEFAULT 8
)
RETURNS DOCUMENT
BEGIN
    -- Query memory metrics
    DECLARE metrics CURSOR FOR
        FROM metrics-system-*
        | WHERE host.name == host
        | WHERE system.memory.used.bytes > threshold_gb * 1e9
        | SORT @timestamp DESC
        | LIMIT 10;
    
    -- Analyze and return findings
    OPEN metrics;
    DECLARE findings ARRAY = [];
    
    FOR m IN metrics LOOP
        SET findings = ARRAY_APPEND(findings, {
            "timestamp": m.@timestamp,
            "used_gb": m.system.memory.used.bytes / 1e9,
            "process": m.process.name
        });
    END LOOP;
    
    CLOSE metrics;
    RETURN {"host": host, "findings": findings};
END SKILL;
```

Skills are:

- **Versioned** - Track changes over time
- **Parameterized** - Accept typed inputs
- **Testable** - Include built-in test assertions
- **Composable** - Call other skills

### Connectors

A **Connector** pulls data from external services:

```sql
CREATE CONNECTOR jira_project
TYPE 'jira'
CONFIG {
    "url": "https://mycompany.atlassian.net",
    "email": "{{secrets.jira_email}}",
    "token": "{{secrets.jira_token}}",
    "project": "OPS"
};
```

Once created, query it like any data source:

```sql
-- Get all open incidents
SELECT * FROM jira_project.issues
WHERE type = 'Incident' AND status != 'Resolved';

-- Sync to Elasticsearch
SYNC CONNECTOR jira_project TO jira-issues-*;
```

### Agents

An **Agent** autonomously executes skills to achieve goals:

```sql
CREATE AGENT sre_assistant
GOAL 'Respond to production incidents quickly and effectively'
SKILLS [
    diagnose_memory_leak,
    check_recent_deployments,
    rollback_service,
    notify_oncall,
    create_jira_ticket
]
EXECUTION human_approval  -- Require human approval for actions
BEGIN
    -- Agent decides which skills to use based on context
END AGENT;
```

Agents are:

- **Goal-oriented** - Define the outcome, not the steps
- **Observable** - Every decision is logged
- **Controllable** - Human-in-the-loop for critical actions

---

## The Name

🦌 **Moltler** combines two concepts:

- **Molt** - The process of shedding and renewal, representing how agents evolve and improve
- **Antler** - The signature of our elk mascot (a nod to the Elastic Stack's heritage)

Just like an elk grows stronger antlers each season, Moltler helps your agents evolve and improve over time.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Moltler                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐     │
│  │ Skills  │   │Connectors│  │ Agents  │   │   AI    │     │
│  │ Engine  │   │  Engine  │  │ Runtime │   │ Engine  │     │
│  └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘     │
│       │             │             │             │           │
│       └─────────────┴─────────────┴─────────────┘           │
│                           │                                  │
│              ┌────────────┴────────────┐                    │
│              │     elastic-script      │                    │
│              │     Runtime Engine      │                    │
│              └────────────┬────────────┘                    │
│                           │                                  │
├───────────────────────────┼──────────────────────────────────┤
│                           ▼                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                   Elasticsearch                         │ │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────┐  │ │
│  │  │  Logs   │ │ Metrics │ │ Traces  │ │ Skills Store │  │ │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────────┘  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## Comparison

### vs. Ansible/Terraform

| Feature | Ansible/Terraform | Moltler |
|---------|-------------------|---------|
| **Purpose** | Infrastructure provisioning | Operational automation |
| **Data awareness** | Limited | Native ES|QL queries |
| **AI integration** | Bolt-on | Native LLM functions |
| **Observability** | External tools | Built-in tracing |
| **Agents** | No | Yes |

### vs. Runbook Platforms (PagerDuty, Shoreline)

| Feature | Runbook Platforms | Moltler |
|---------|-------------------|---------|
| **Data source** | Limited integrations | Any via Connectors |
| **Language** | Proprietary/YAML | elastic-script (SQL-like) |
| **Self-hosted** | Usually SaaS | Yes, on your Elasticsearch |
| **AI agents** | Limited | Full agent runtime |

### vs. Custom Python Scripts

| Feature | Python Scripts | Moltler |
|---------|----------------|---------|
| **Execution** | Various (cron, Lambda) | Native in Elasticsearch |
| **Observability** | DIY | Built-in APM/tracing |
| **Versioning** | Git | Built-in skill versions |
| **Testing** | DIY | Built-in test framework |
| **AI generation** | Complex | `GENERATE SKILL FROM "..."` |

---

## Getting Started

Ready to build your first skill?

[:octicons-arrow-right-24: Installation](installation.md){ .md-button .md-button--primary }
[:octicons-arrow-right-24: Quick Start](quick-start.md){ .md-button }

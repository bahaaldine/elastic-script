# Moltler

<div class="hero" markdown>

## :material-creation: The AI Skills Creation Framework for Elasticsearch

**Build, test, and deploy AI-powered skills that automate your operations.**

[Get Started](getting-started/what-is-moltler.md){ .md-button .md-button--primary }
[View on GitHub](https://github.com/bahaaldine/elastic-script){ .md-button }

</div>

---

<div class="grid cards" markdown>

-   :material-package-variant:{ .lg .middle } __Skills__

    ---

    Create reusable, versioned skills that encapsulate operational knowledge. Share them across teams or publish to the marketplace.

    [:octicons-arrow-right-24: Create Your First Skill](getting-started/first-skill.md)

-   :material-connection:{ .lg .middle } __Connectors__

    ---

    Pull data from GitHub, Jira, Datadog, Salesforce, and more. Query external services with familiar syntax.

    [:octicons-arrow-right-24: Browse Connectors](connectors/overview.md)

-   :material-robot:{ .lg .middle } __Agents__

    ---

    Deploy autonomous agents that execute skills based on goals. Monitor their decisions with full observability.

    [:octicons-arrow-right-24: Build Agents](agents/overview.md)

-   :material-creation:{ .lg .middle } __AI-Powered__

    ---

    Generate skills from natural language descriptions. Let AI help you write, test, and improve your automation.

    [:octicons-arrow-right-24: Generate Skills](ai/generate-skills.md)

</div>

---

## What is Moltler?

**Moltler** is an AI skills creation framework that runs on Elasticsearch. It enables you to:

1. **Define Skills** - Encapsulate operational knowledge as reusable, versioned procedures
2. **Connect Data Sources** - Query GitHub, Jira, Datadog, and other services natively
3. **Deploy Agents** - Create autonomous agents that execute skills to achieve goals
4. **Observe Everything** - Full tracing, logging, and debugging in Kibana

### The Name

🦌 **Moltler** combines "molt" (the process of shedding and renewal) with "antler" (the signature of our elk mascot). Just like an elk grows stronger antlers each season, Moltler helps your agents evolve and improve over time.

---

## Why Moltler?

### The Problem

Modern operations teams face:

- **Scattered knowledge** - Runbooks in wikis, scripts in repos, processes in people's heads
- **Tool sprawl** - Dozens of tools that don't talk to each other
- **Manual toil** - Repetitive tasks that should be automated
- **AI risk** - Wanting to use AI but worried about uncontrolled actions

### The Moltler Solution

| Challenge | Moltler Solution |
|-----------|------------------|
| **Scattered knowledge** | Skills capture operational procedures in versioned, testable code |
| **Tool sprawl** | Connectors unify data access with a single query language |
| **Manual toil** | Agents execute skills autonomously based on conditions |
| **AI risk** | Bounded execution inside Elasticsearch with full observability |

---

## Quick Example

### 1. Create a Skill

```sql
CREATE SKILL check_deployment_health
VERSION '1.0.0'
DESCRIPTION 'Verifies deployment health across services'
PARAMETERS (
    service_name STRING,
    threshold NUMBER DEFAULT 0.95
)
RETURNS DOCUMENT
BEGIN
    -- Query service metrics
    DECLARE metrics CURSOR FOR
        FROM metrics-*
        | WHERE service.name == service_name
        | WHERE @timestamp > NOW() - 5 MINUTES
        | STATS success_rate = AVG(success);
    
    OPEN metrics;
    FETCH metrics INTO result;
    CLOSE metrics;
    
    -- Check threshold
    IF result.success_rate < threshold THEN
        RETURN {"status": "unhealthy", "rate": result.success_rate};
    END IF;
    
    RETURN {"status": "healthy", "rate": result.success_rate};
END SKILL;
```

### 2. Test the Skill

```sql
TEST SKILL check_deployment_health
WITH service_name = 'api-gateway', threshold = 0.90
EXPECT status = 'healthy';
```

### 3. Create an Agent

```sql
CREATE AGENT deployment_guardian
GOAL 'Ensure all critical services maintain 99% uptime'
SKILLS [check_deployment_health, notify_oncall, scale_service]
TRIGGERS [
    ON SCHEDULE '*/5 * * * *',  -- Every 5 minutes
    ON ALERT 'high-error-rate'
]
BEGIN
    -- Agent decides which skills to use based on context
END AGENT;
```

---

## Core Concepts

### Skills

Skills are **versioned, testable units of operational knowledge**:

```sql
CREATE SKILL analyze_logs VERSION '2.0.0'
DESCRIPTION 'Analyzes log patterns for anomalies'
PARAMETERS (index_pattern STRING, lookback STRING DEFAULT '1h')
RETURNS ARRAY
BEGIN
    -- Implementation
END SKILL;
```

Features:

- **Versioning** - Track changes, roll back, run A/B tests
- **Parameters** - Typed inputs with defaults and validation
- **Testing** - Built-in test framework with assertions
- **Dependencies** - Skills can require other skills

[:octicons-arrow-right-24: Learn more about Skills](skills/overview.md)

### Connectors

Connectors **pull data from external services** into Elasticsearch:

```sql
-- Create a connector
CREATE CONNECTOR github_ops
TYPE 'github'
CONFIG {
    "token": "{{secrets.github_token}}",
    "org": "mycompany"
};

-- Query it like any data source
SELECT * FROM github_ops.issues 
WHERE state = 'open' AND labels CONTAINS 'incident';
```

Supported connectors:

- **DevOps**: GitHub, GitLab, Jira, Confluence
- **Monitoring**: Datadog, New Relic, Prometheus
- **Communication**: Slack, PagerDuty, Opsgenie
- **Cloud**: AWS, GCP, Azure
- **CRM**: Salesforce, Zendesk, HubSpot

[:octicons-arrow-right-24: Explore Connectors](connectors/overview.md)

### Agents

Agents are **autonomous executors** that use skills to achieve goals:

```sql
CREATE AGENT incident_responder
GOAL 'Investigate and mitigate production incidents'
SKILLS [
    diagnose_issue,
    check_recent_deployments,
    rollback_deployment,
    notify_team
]
EXECUTION human_approval  -- Require approval for actions
BEGIN
    -- Agent autonomously selects skills based on context
END AGENT;
```

Features:

- **Goal-oriented** - Define what, not how
- **Human-in-the-loop** - Approval workflows for critical actions
- **Observable** - Every decision is logged and traceable
- **Learnable** - Improve based on outcomes

[:octicons-arrow-right-24: Build Agents](agents/overview.md)

---

## Installation

=== "Quick Start"

    ```bash
    git clone https://github.com/bahaaldine/elastic-script.git
    cd elastic-script
    ./scripts/quick-start.sh
    ```
    
    This starts:
    
    - **Elasticsearch** on port 9200
    - **Kibana** on port 5601
    - **Moltler CLI** ready to use
    - **Jupyter Notebooks** on port 8888

=== "CLI Only"

    ```bash
    pip install moltler
    moltler init
    ```

=== "Docker"

    ```bash
    docker run -p 9200:9200 bahaaldine/moltler:latest
    ```

---

## The Technology

Moltler is powered by **elastic-script**, a procedural language designed for:

- **AI agents** to generate and execute
- **Humans** to read and debug
- **Elasticsearch** to run at scale

The language provides:

- 118+ built-in functions
- Native ES|QL integration
- Async execution with continuations
- Distributed tracing via OpenTelemetry

[:octicons-arrow-right-24: elastic-script Language Reference](language/overview.md)

---

## What's Next?

<div class="grid cards" markdown>

-   :material-rocket-launch:{ .lg .middle } __Get Started__

    Create your first skill in 5 minutes.

    [:octicons-arrow-right-24: Quick Start](getting-started/quick-start.md)

-   :material-book-open-page-variant:{ .lg .middle } __Browse Examples__

    Real-world skills for observability, incident response, and DevOps.

    [:octicons-arrow-right-24: Examples](examples/observability-skills.md)

-   :material-school:{ .lg .middle } __Tutorials__

    Step-by-step guides for common use cases.

    [:octicons-arrow-right-24: Tutorials](notebooks/getting-started.md)

-   :material-map:{ .lg .middle } __Roadmap__

    See what's coming next.

    [:octicons-arrow-right-24: Roadmap](roadmap.md)

</div>

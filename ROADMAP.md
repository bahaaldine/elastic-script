# Moltler Roadmap

## The Vision

**Moltler** is the AI Skills Creation Framework for Elasticsearch, powered by **elastic-script**.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MOLTLER                                         │
│                 "The AI Skills Creation Framework"                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                 │
│   │    SKILLS    │    │    AGENTS    │    │  CONNECTORS  │                 │
│   │  Reusable    │    │  Autonomous  │    │  Data from   │                 │
│   │  Automation  │    │  Executors   │    │  Everywhere  │                 │
│   └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                 │
│          │                   │                   │                          │
│          └───────────────────┴───────────────────┘                          │
│                              │                                               │
│                              ▼                                               │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                      ELASTIC-SCRIPT                                  │   │
│   │              Procedural Language for Elasticsearch                   │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                      ELASTICSEARCH                                   │   │
│   │                   Storage • Search • Analytics                       │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 0: Connectors & Data Layer

> **Goal**: Enable skills to pull data from anywhere with minimal setup.

### 0.1 Connector Framework Core

#### Features
- [ ] `CREATE CONNECTOR name TYPE type CREDENTIALS ...`
- [ ] `DROP CONNECTOR name`
- [ ] `SHOW CONNECTORS`
- [ ] `SHOW CONNECTOR name` (details, status, last sync)
- [ ] `ALTER CONNECTOR name SET ...`
- [ ] `TEST CONNECTOR name` (verify credentials work)

#### Grammar Changes
```antlr
connector_statement
    : create_connector_statement
    | drop_connector_statement
    | show_connectors_statement
    | alter_connector_statement
    | test_connector_statement
    | sync_connector_statement
    ;

create_connector_statement
    : CREATE CONNECTOR ID TYPE connector_type 
      (CREDENTIALS SECRET STRING)?
      (AUTH (OAUTH | API_KEY | BASIC))?
      (SYNC EVERY interval)?
      (OPTIONS connector_options)?
    ;
```

#### Implementation
- [ ] `ConnectorDefinition` - Stores connector metadata
- [ ] `ConnectorRegistry` - Manages connector lifecycle
- [ ] `ConnectorStatementHandler` - Handles DDL statements
- [ ] `.moltler_connectors` index for persistence
- [ ] Secrets integration (environment variables, Elasticsearch keystore)

#### Tests
- [ ] Unit: ConnectorDefinition serialization/deserialization
- [ ] Unit: ConnectorRegistry CRUD operations
- [ ] Unit: Grammar parsing for all connector statements
- [ ] Integration: Create, show, drop connector lifecycle
- [ ] Integration: Credential validation
- [ ] E2E: Notebook with connector examples

#### Documentation
- [ ] `docs/connectors/overview.md` - Connector concepts
- [ ] `docs/connectors/creating-connectors.md` - How to create
- [ ] `docs/connectors/authentication.md` - OAuth, API keys, secrets
- [ ] `docs/reference/connector-statements.md` - DDL reference

---

### 0.2 Connector Query Interface

#### Features
- [ ] Query connector data directly: `FROM connector_name.entity`
- [ ] Write back to connectors: `CONNECTOR_EXEC connector_name action`
- [ ] Real-time vs cached queries
- [ ] Query translation layer (ES|QL to connector API)

#### Grammar Changes
```antlr
connector_query
    : FROM connector_id DOT entity_name (query_clauses)?
    ;

connector_exec_statement
    : CONNECTOR_EXEC connector_id action_name (WITH expression)?
    ;
```

#### Implementation
- [ ] `ConnectorQueryExecutor` - Translates queries to API calls
- [ ] `ConnectorDataSource` - Abstraction for data sources
- [ ] Caching layer for frequently accessed data
- [ ] Pagination handling for large datasets

#### Tests
- [ ] Unit: Query translation for each connector type
- [ ] Unit: Caching behavior
- [ ] Integration: Query across connector types
- [ ] Integration: Write-back operations
- [ ] E2E: Skill using connector data

#### Documentation
- [ ] `docs/connectors/querying-data.md` - How to query
- [ ] `docs/connectors/write-back.md` - Modifying external data
- [ ] Examples in each connector's docs

---

### 0.3 Sync Engine

#### Features
- [ ] `SYNC CONNECTOR name` - Manual full sync
- [ ] `SYNC CONNECTOR name SINCE timestamp` - Incremental sync
- [ ] Scheduled sync: `SYNC EVERY 5 MINUTES`
- [ ] Webhook receivers for real-time updates
- [ ] Sync status and history

#### Implementation
- [ ] `SyncScheduler` - Manages sync schedules
- [ ] `SyncExecutor` - Performs sync operations
- [ ] `SyncHistory` - Tracks sync results
- [ ] Webhook endpoint for real-time events
- [ ] Conflict resolution for overlapping syncs

#### Tests
- [ ] Unit: Sync scheduling logic
- [ ] Unit: Incremental sync delta detection
- [ ] Integration: Full sync lifecycle
- [ ] Integration: Webhook event processing
- [ ] E2E: Scheduled sync over time

#### Documentation
- [ ] `docs/connectors/sync-strategies.md` - Sync modes
- [ ] `docs/connectors/webhooks.md` - Real-time updates
- [ ] `docs/connectors/troubleshooting.md` - Common issues

---

### 0.4 Core Connectors (Tier 1)

Each connector needs:
- [ ] Implementation class
- [ ] OAuth/API key authentication
- [ ] Entity definitions (what can be queried)
- [ ] Rate limiting
- [ ] Error handling
- [ ] Tests (unit + integration)
- [ ] Documentation

#### GitHub Connector
```sql
CREATE CONNECTOR github_org
  TYPE GITHUB
  AUTH OAUTH
  SCOPE ['repo', 'read:org'];

-- Query examples
FROM github_org.repositories WHERE language = 'Java';
FROM github_org.pull_requests WHERE state = 'open';
FROM github_org.issues WHERE labels CONTAINS 'bug';
```

**Entities**: repositories, pull_requests, issues, commits, workflows, actions

#### Jira Connector
```sql
CREATE CONNECTOR jira_prod
  TYPE JIRA
  URL 'https://company.atlassian.net'
  CREDENTIALS SECRET 'jira_token';

-- Query examples
FROM jira_prod.issues WHERE project = 'OPS' AND status != 'Done';
FROM jira_prod.sprints WHERE state = 'active';
```

**Entities**: issues, projects, sprints, boards, users, comments

#### Datadog Connector
```sql
CREATE CONNECTOR datadog_prod
  TYPE DATADOG
  API_KEY SECRET 'dd_api_key'
  APP_KEY SECRET 'dd_app_key';

-- Query examples
FROM datadog_prod.metrics WHERE name = 'system.cpu.user' AND timeframe = '1h';
FROM datadog_prod.monitors WHERE status = 'Alert';
FROM datadog_prod.incidents WHERE state = 'active';
```

**Entities**: metrics, monitors, incidents, dashboards, logs

#### Salesforce Connector
```sql
CREATE CONNECTOR salesforce_prod
  TYPE SALESFORCE
  AUTH OAUTH
  INSTANCE 'na1';

-- Query examples
FROM salesforce_prod.opportunities WHERE stage = 'Negotiation' AND amount > 100000;
FROM salesforce_prod.accounts WHERE type = 'Customer';
FROM salesforce_prod.contacts WHERE email LIKE '%@company.com';
```

**Entities**: accounts, contacts, opportunities, leads, cases, tasks

#### Zendesk Connector
```sql
CREATE CONNECTOR zendesk_support
  TYPE ZENDESK
  SUBDOMAIN 'company'
  CREDENTIALS SECRET 'zendesk_token';

-- Query examples  
FROM zendesk_support.tickets WHERE status = 'open' AND priority = 'urgent';
FROM zendesk_support.users WHERE role = 'agent';
```

**Entities**: tickets, users, organizations, groups, satisfaction_ratings

#### Tests per Connector
- [ ] Unit: Authentication flow
- [ ] Unit: Entity query translation
- [ ] Unit: Rate limiting behavior
- [ ] Unit: Error response handling
- [ ] Integration: Real API calls (with sandbox/test accounts)
- [ ] E2E: Connector in a skill

#### Documentation per Connector
- [ ] `docs/connectors/github.md`
- [ ] `docs/connectors/jira.md`
- [ ] `docs/connectors/datadog.md`
- [ ] `docs/connectors/salesforce.md`
- [ ] `docs/connectors/zendesk.md`

---

### 0.5 Connector SDK

#### Features
- [ ] Base classes for building connectors
- [ ] Authentication helpers (OAuth, API key, etc.)
- [ ] Rate limiting utilities
- [ ] Pagination helpers
- [ ] Testing framework for connectors

#### Implementation
```java
public abstract class ConnectorBase {
    abstract void authenticate();
    abstract List<Entity> getEntities();
    abstract QueryResult executeQuery(ConnectorQuery query);
    abstract void executeAction(ConnectorAction action);
}

public class GitHubConnector extends ConnectorBase {
    // Implementation
}
```

#### Documentation
- [ ] `docs/connectors/building-connectors.md` - SDK guide
- [ ] `docs/connectors/connector-api.md` - API reference
- [ ] Example: Building a custom connector

---

## Phase 1: Skills Foundation

> **Goal**: Make skills robust, versioned, testable, and composable.

### 1.1 Skill Versioning & Metadata

#### Features
- [ ] `CREATE SKILL name VERSION '1.0.0' ...`
- [ ] `UPGRADE SKILL name TO VERSION '2.0.0'`
- [ ] `SHOW SKILL name VERSIONS` - List all versions
- [ ] `CALL SKILL name VERSION '1.x'` - Call specific version
- [ ] Semantic versioning support (major.minor.patch)
- [ ] Default version selection (latest stable)

#### Grammar Changes
```antlr
create_skill_statement
    : CREATE SKILL ID VERSION STRING
      LPAREN skill_param_list? RPAREN
      (RETURNS datatype)?
      DESCRIPTION STRING
      (EXAMPLES string_list)?
      (TAGS string_list)?
      (DEPRECATED STRING)?
      PROCEDURE ID (LPAREN argument_list? RPAREN)?
      END SKILL
    ;
```

#### Implementation
- [ ] `SkillVersion` class with semver parsing
- [ ] Version storage in `.moltler_skills` index
- [ ] Version resolution logic (latest, specific, range)
- [ ] Deprecation warnings

#### Tests
- [ ] Unit: Semantic version parsing and comparison
- [ ] Unit: Version resolution (1.x, ^1.0.0, etc.)
- [ ] Integration: Multiple versions of same skill
- [ ] Integration: Upgrade path testing
- [ ] E2E: Skill versioning in notebooks

#### Documentation
- [ ] `docs/skills/versioning.md` - Version concepts
- [ ] `docs/skills/upgrading.md` - Migration guide
- [ ] Best practices for versioning

---

### 1.2 Skill Dependencies

#### Features
- [ ] `SKILL DEPENDS ON other_skill VERSION '1.x'`
- [ ] Automatic dependency resolution
- [ ] Circular dependency detection
- [ ] Dependency tree visualization

#### Grammar Changes
```antlr
skill_dependencies
    : DEPENDS ON skill_dependency_list
    ;

skill_dependency
    : ID (VERSION STRING)?
    ;
```

#### Implementation
- [ ] `SkillDependencyResolver` - Resolves dependency tree
- [ ] Dependency injection into skill context
- [ ] Version conflict resolution

#### Tests
- [ ] Unit: Dependency graph construction
- [ ] Unit: Circular dependency detection
- [ ] Unit: Version conflict resolution
- [ ] Integration: Skill with dependencies
- [ ] E2E: Complex dependency chains

#### Documentation
- [ ] `docs/skills/dependencies.md` - Dependency management
- [ ] Examples of skill composition

---

### 1.3 Skill Testing Framework

#### Features
- [ ] `TEST SKILL name WITH inputs EXPECT outputs`
- [ ] `TEST SKILL name WITH inputs EXPECT ERROR`
- [ ] Mock connectors for testing
- [ ] Test fixtures and data
- [ ] Coverage reporting
- [ ] `moltler test` CLI command

#### Grammar Changes
```antlr
test_skill_statement
    : TEST SKILL ID 
      WITH expression
      (EXPECT expression | EXPECT ERROR STRING?)
      (USING MOCKS mock_list)?
    ;
```

#### Implementation
- [ ] `SkillTestRunner` - Executes skill tests
- [ ] `MockConnector` - Fake connector for testing
- [ ] `TestContext` - Isolated execution context
- [ ] Test result reporting

#### Tests
- [ ] Unit: Test parsing and execution
- [ ] Unit: Mock connector behavior
- [ ] Integration: Test suites for skills
- [ ] E2E: CI/CD integration

#### Documentation
- [ ] `docs/skills/testing.md` - Testing guide
- [ ] `docs/skills/mocking.md` - Mock connectors
- [ ] `docs/cli/moltler-test.md` - CLI reference

---

### 1.4 Skill Templates & Scaffolding

#### Features
- [ ] `moltler init skill --name my_skill`
- [ ] `moltler init skill --template observability`
- [ ] Pre-built templates for common patterns
- [ ] Customizable templates

#### Templates
- [ ] `basic` - Minimal skill structure
- [ ] `observability` - Monitoring and alerting
- [ ] `incident-response` - PagerDuty/Slack integration
- [ ] `data-pipeline` - ETL patterns
- [ ] `ai-powered` - LLM integration

#### Implementation
- [ ] Template storage and management
- [ ] Variable substitution in templates
- [ ] Interactive prompts for template params

#### Tests
- [ ] Unit: Template rendering
- [ ] Integration: Scaffold and run
- [ ] E2E: Full workflow with templates

#### Documentation
- [ ] `docs/cli/moltler-init.md` - Scaffolding guide
- [ ] `docs/skills/templates.md` - Available templates

---

### 1.5 Skill Validation & Linting

#### Features
- [ ] `moltler lint` - Check skill for issues
- [ ] `moltler validate` - Validate skill structure
- [ ] Pre-commit hooks
- [ ] IDE integration

#### Lint Rules
- [ ] Unused variables
- [ ] Missing error handling
- [ ] Inefficient queries
- [ ] Security issues (hardcoded secrets)
- [ ] Deprecated function usage
- [ ] Missing documentation

#### Implementation
- [ ] `SkillLinter` - Static analysis
- [ ] `SkillValidator` - Structure validation
- [ ] Configurable rule sets

#### Tests
- [ ] Unit: Each lint rule
- [ ] Integration: Linting full skills
- [ ] E2E: CI/CD integration

#### Documentation
- [ ] `docs/cli/moltler-lint.md` - Linting guide
- [ ] `docs/skills/best-practices.md` - Code quality

---

## Phase 2: AI-Powered Creation

> **Goal**: Use AI to generate, improve, and recommend skills.

### 2.1 Natural Language to Skill

#### Features
- [ ] `GENERATE SKILL FROM "description"` - Create skill from NL
- [ ] `GENERATE SKILL FROM "description" USING MODEL 'gpt-4'`
- [ ] Interactive refinement: "Add error handling"
- [ ] Multiple generation attempts with ranking

#### Grammar Changes
```antlr
generate_skill_statement
    : GENERATE SKILL (ID)? FROM STRING
      (USING MODEL STRING)?
      (WITH CONNECTORS connector_list)?
      (SAVE AS ID)?
    ;
```

#### Implementation
- [ ] `SkillGenerator` - LLM-powered generation
- [ ] Prompt engineering for skill generation
- [ ] Validation of generated code
- [ ] Iterative refinement loop

#### Prompts
```
System: You are an expert at creating Moltler skills using elastic-script.
A skill is a reusable automation that runs inside Elasticsearch.

Available connectors: {connectors}
Available functions: {functions}

Generate a skill that: {user_description}

Requirements:
- Handle errors gracefully
- Add logging with PRINT
- Follow naming conventions
- Include parameter validation
```

#### Tests
- [ ] Unit: Prompt construction
- [ ] Unit: Generated code validation
- [ ] Integration: End-to-end generation
- [ ] E2E: Generate and run skill

#### Documentation
- [ ] `docs/ai/generating-skills.md` - Generation guide
- [ ] `docs/ai/prompts.md` - Prompt best practices
- [ ] Examples gallery

---

### 2.2 Skill Improvement & Analysis

#### Features
- [ ] `IMPROVE SKILL name` - AI suggests improvements
- [ ] `ANALYZE SKILL name` - Performance analysis
- [ ] `EXPLAIN SKILL name` - Generate documentation
- [ ] Learning from execution failures

#### Implementation
- [ ] `SkillAnalyzer` - Code analysis
- [ ] `SkillImprover` - Suggestion generation
- [ ] `SkillDocGenerator` - Auto-documentation
- [ ] Execution history analysis

#### Tests
- [ ] Unit: Analysis algorithms
- [ ] Integration: Improvement suggestions
- [ ] E2E: Full improvement workflow

#### Documentation
- [ ] `docs/ai/improving-skills.md`
- [ ] `docs/ai/analysis.md`

---

### 2.3 Intent Resolution & Recommendation

#### Features
- [ ] `RECOMMEND SKILLS FOR "problem description"`
- [ ] Context-aware skill suggestions
- [ ] "Did you mean?" for similar skills
- [ ] Skill search by capability

#### Implementation
- [ ] Skill embedding for similarity search
- [ ] Context understanding from recent queries
- [ ] Ranking algorithm for recommendations

#### Tests
- [ ] Unit: Embedding generation
- [ ] Unit: Similarity search
- [ ] Integration: Recommendation quality
- [ ] E2E: Interactive recommendations

#### Documentation
- [ ] `docs/ai/recommendations.md`
- [ ] `docs/skills/discovery.md`

---

## Phase 3: Agent Runtime

> **Goal**: Autonomous agents that use skills to achieve goals.

### 3.1 Agent Definition

#### Features
- [ ] `CREATE AGENT name GOAL "description" ...`
- [ ] `DROP AGENT name`
- [ ] `SHOW AGENTS`
- [ ] `SHOW AGENT name` (status, history, metrics)
- [ ] `START AGENT name` / `STOP AGENT name`

#### Grammar Changes
```antlr
create_agent_statement
    : CREATE AGENT ID
      GOAL STRING
      SKILLS LBRACKET skill_list RBRACKET
      (SCHEDULE schedule_expression)?
      (TRIGGERS trigger_list)?
      (CONSTRAINTS constraint_list)?
      (APPROVAL_REQUIRED FOR action_patterns)?
      END AGENT
    ;
```

#### Example
```sql
CREATE AGENT ops_guardian
  GOAL "Keep production systems healthy and respond to incidents"
  SKILLS [
    health_checker,
    pod_restarter,
    disk_cleaner,
    alert_acknowledger,
    incident_resolver
  ]
  SCHEDULE EVERY 5 MINUTES
  TRIGGERS [
    ON ALERT FROM pagerduty,
    ON EVENT 'pod_crash' FROM kubernetes
  ]
  CONSTRAINTS [
    MAX_ACTIONS_PER_HOUR 100,
    NO_ACTIONS_ON ['database_*']
  ]
  APPROVAL_REQUIRED FOR ['delete_*', 'scale_down_*']
END AGENT;
```

#### Implementation
- [ ] `AgentDefinition` - Stores agent config
- [ ] `AgentRegistry` - Manages agents
- [ ] `AgentStatementHandler` - Handles DDL
- [ ] `.moltler_agents` index for persistence

#### Tests
- [ ] Unit: Agent definition parsing
- [ ] Unit: Constraint validation
- [ ] Integration: Agent lifecycle
- [ ] E2E: Running agent

#### Documentation
- [ ] `docs/agents/overview.md` - Agent concepts
- [ ] `docs/agents/creating-agents.md`
- [ ] `docs/agents/constraints.md`

---

### 3.2 Agent Execution Loop

#### Features
- [ ] Observe: Collect context from connectors
- [ ] Orient: Analyze situation
- [ ] Decide: Select skill to execute
- [ ] Act: Execute skill
- [ ] Learn: Update from results

#### Implementation
```
┌─────────────────────────────────────────────────────────────┐
│                    AGENT EXECUTION LOOP                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌──────────┐     ┌──────────┐     ┌──────────┐           │
│   │ OBSERVE  │────▶│  ORIENT  │────▶│  DECIDE  │           │
│   │          │     │          │     │          │           │
│   │ Collect  │     │ Analyze  │     │ Select   │           │
│   │ context  │     │ situation│     │ skill    │           │
│   └──────────┘     └──────────┘     └────┬─────┘           │
│        ▲                                  │                  │
│        │                                  ▼                  │
│   ┌────┴─────┐                      ┌──────────┐           │
│   │  LEARN   │◀─────────────────────│   ACT    │           │
│   │          │                      │          │           │
│   │ Update   │                      │ Execute  │           │
│   │ from     │                      │ skill    │           │
│   │ results  │                      │          │           │
│   └──────────┘                      └──────────┘           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

- [ ] `AgentExecutor` - Main loop
- [ ] `ContextCollector` - Gathers relevant data
- [ ] `SkillSelector` - Chooses which skill to run
- [ ] `ActionExecutor` - Runs skills
- [ ] `ResultAnalyzer` - Processes outcomes

#### Tests
- [ ] Unit: Each component
- [ ] Integration: Full loop execution
- [ ] E2E: Agent responding to events

#### Documentation
- [ ] `docs/agents/execution-model.md`
- [ ] `docs/agents/skill-selection.md`

---

### 3.3 Human-in-the-Loop

#### Features
- [ ] Approval workflows for risky actions
- [ ] Slack/Email approval requests
- [ ] Timeout with default action
- [ ] Audit trail of approvals

#### Implementation
- [ ] `ApprovalRequest` - Pending approval
- [ ] `ApprovalHandler` - Sends requests
- [ ] `ApprovalWatcher` - Monitors responses
- [ ] Integration with Slack, email, etc.

#### Example
```sql
-- Agent wants to scale down, needs approval
APPROVAL_REQUIRED FOR ['scale_down_*']

-- Sends to Slack:
-- "Agent ops_guardian wants to execute: scale_down_pods(cluster='prod', count=5)
-- React with ✅ to approve or ❌ to deny"
```

#### Tests
- [ ] Unit: Approval flow logic
- [ ] Integration: Slack approval
- [ ] E2E: Full approval workflow

#### Documentation
- [ ] `docs/agents/approvals.md`
- [ ] `docs/agents/human-in-the-loop.md`

---

### 3.4 Agent Observability

#### Features
- [ ] Agent execution traces
- [ ] Skill invocation metrics
- [ ] Goal progress tracking
- [ ] Alert on agent failures
- [ ] Dashboard for agent status

#### Implementation
- [ ] APM integration for traces
- [ ] Metrics collection
- [ ] Progress indicators
- [ ] Alerting rules

#### Tests
- [ ] Integration: Trace generation
- [ ] Integration: Metrics collection
- [ ] E2E: Full observability stack

#### Documentation
- [ ] `docs/agents/monitoring.md`
- [ ] `docs/agents/troubleshooting.md`

---

## Phase 4: Developer Experience

> **Goal**: Make Moltler delightful to use.

### 4.1 CLI Enhancements

#### Features
- [ ] `moltler init` - Project scaffolding
- [ ] `moltler skill create/list/run/test/publish`
- [ ] `moltler connector create/list/sync/test`
- [ ] `moltler agent create/start/stop/status`
- [ ] `moltler hub search/install/publish`
- [ ] Interactive mode improvements
- [ ] Progress bars and spinners
- [ ] Rich output formatting

#### Implementation
- [ ] Command structure using Click
- [ ] Output formatting with Rich
- [ ] Progress tracking
- [ ] Error messages with suggestions

#### Tests
- [ ] Unit: Each command
- [ ] Integration: Full workflows
- [ ] E2E: Real-world scenarios

#### Documentation
- [ ] `docs/cli/overview.md`
- [ ] `docs/cli/commands.md` - Full reference
- [ ] Cheat sheet

---

### 4.2 Python SDK

#### Features
```python
from moltler import Moltler, Skill, Connector

# Initialize
moltler = Moltler(host="localhost:9200")

# Skills
skill = moltler.skills.get("my_skill")
result = skill.run(param1="value1")

# Or inline
result = moltler.run("""
    CREATE PROCEDURE temp() BEGIN
        PRINT 'Hello from Python!';
    END PROCEDURE;
    CALL temp();
""")

# Connectors
connector = moltler.connectors.get("github_org")
repos = connector.query("repositories WHERE language = 'Python'")

# Agents
agent = moltler.agents.get("ops_guardian")
agent.start()
status = agent.status()
```

#### Implementation
- [ ] `moltler-python` package
- [ ] REST API client
- [ ] Async support
- [ ] Type hints

#### Tests
- [ ] Unit: All SDK methods
- [ ] Integration: With running Elasticsearch
- [ ] E2E: Complex workflows

#### Documentation
- [ ] `docs/sdk/python/quickstart.md`
- [ ] `docs/sdk/python/api-reference.md`
- [ ] Example notebooks

---

### 4.3 JavaScript/TypeScript SDK

#### Features
```typescript
import { Moltler } from '@moltler/sdk';

const moltler = new Moltler({ host: 'localhost:9200' });

// Skills
const result = await moltler.skills.run('my_skill', { param1: 'value1' });

// Connectors
const repos = await moltler.connectors.query('github_org', {
  entity: 'repositories',
  where: { language: 'TypeScript' }
});
```

#### Implementation
- [ ] `@moltler/sdk` npm package
- [ ] TypeScript definitions
- [ ] Browser and Node.js support

#### Tests & Docs
- [ ] Full test suite
- [ ] Complete documentation

---

### 4.4 VSCode Extension

#### Features
- [ ] Syntax highlighting for elastic-script
- [ ] IntelliSense for keywords, functions
- [ ] Skill snippets
- [ ] Run skill from editor
- [ ] Connector browser
- [ ] Skill testing integration

#### Implementation
- [ ] Language server protocol (LSP)
- [ ] TextMate grammar
- [ ] Custom commands

#### Tests & Docs
- [ ] Extension tests
- [ ] Marketplace documentation

---

### 4.5 Web UI (Moltler Studio)

#### Features
- [ ] Visual skill builder
- [ ] Connector management
- [ ] Agent dashboard
- [ ] Execution history
- [ ] Skill marketplace browser
- [ ] Code editor with syntax highlighting

#### Implementation
- [ ] React/Next.js frontend
- [ ] REST API backend
- [ ] Real-time updates via WebSocket

#### Tests & Docs
- [ ] Component tests
- [ ] E2E tests with Playwright
- [ ] User guide

---

## Phase 5: Runtime & Observability

> **Goal**: Production-ready execution with full visibility.

### 5.1 Execution Tracing

#### Features
- [ ] Full APM integration
- [ ] Trace per skill execution
- [ ] Span per statement
- [ ] Distributed tracing across skills

#### Implementation
- [ ] Enhance `EScriptTracer`
- [ ] APM agent attachment
- [ ] Trace context propagation

#### Tests & Docs
- [ ] Integration with Elastic APM
- [ ] Kibana dashboards

---

### 5.2 Metrics & Monitoring

#### Features
- [ ] Skills executed per second
- [ ] Success/failure rates
- [ ] Latency percentiles
- [ ] Token usage (LLM)
- [ ] Connector API calls
- [ ] Custom metrics from skills

#### Implementation
- [ ] Metrics collection
- [ ] Elasticsearch metrics index
- [ ] Pre-built dashboards

---

### 5.3 Audit Logging

#### Features
- [ ] Who ran what skill when
- [ ] Parameter values (redacted secrets)
- [ ] Results and errors
- [ ] Approval decisions
- [ ] Connector access

#### Implementation
- [ ] Audit log index
- [ ] Retention policies
- [ ] Search interface

---

### 5.4 Rate Limiting & Quotas

#### Features
- [ ] Per-user rate limits
- [ ] Per-skill rate limits
- [ ] Connector API rate limiting
- [ ] Token usage quotas
- [ ] Configurable limits

#### Implementation
- [ ] Rate limiter component
- [ ] Quota tracking
- [ ] Limit exceeded handling

---

### 5.5 Security

#### Features
- [ ] RBAC for skills and connectors
- [ ] Secret management
- [ ] Skill signing and verification
- [ ] Sandbox execution
- [ ] Audit trail

#### Implementation
- [ ] Permission system
- [ ] Elasticsearch security integration
- [ ] Code signing infrastructure

---

## Phase 6: Ecosystem & Distribution

> **Goal**: Build a thriving community around Moltler.

### 6.1 Moltler Hub (Skill Registry)

#### Features
- [ ] `moltler publish skill_name`
- [ ] `moltler install org/skill_name`
- [ ] Public and private registries
- [ ] Versioning and dependencies
- [ ] Search and discovery
- [ ] Reviews and ratings

#### Implementation
- [ ] Hub API
- [ ] CLI integration
- [ ] Web interface
- [ ] Package format

---

### 6.2 Pre-built Skill Packs

#### Observability Pack
- [ ] `health_checker` - Check system health
- [ ] `log_analyzer` - Analyze log patterns
- [ ] `metric_alerter` - Alert on metric thresholds
- [ ] `anomaly_detector` - Detect anomalies
- [ ] `dashboard_generator` - Create dashboards
- [ ] `slo_tracker` - Track SLOs

#### Incident Response Pack
- [ ] `incident_acknowledger` - Acknowledge alerts
- [ ] `runbook_executor` - Execute runbooks
- [ ] `escalation_manager` - Handle escalations
- [ ] `postmortem_generator` - Generate postmortems
- [ ] `communication_sender` - Send status updates

#### DevOps Pack
- [ ] `deployment_checker` - Verify deployments
- [ ] `rollback_executor` - Rollback changes
- [ ] `feature_flag_manager` - Manage flags
- [ ] `config_validator` - Validate configs
- [ ] `secret_rotator` - Rotate secrets

#### Data Pack
- [ ] `data_quality_checker` - Check data quality
- [ ] `schema_validator` - Validate schemas
- [ ] `etl_runner` - Run ETL jobs
- [ ] `backup_verifier` - Verify backups
- [ ] `retention_manager` - Manage retention

#### Security Pack
- [ ] `vulnerability_scanner` - Scan for vulns
- [ ] `access_reviewer` - Review access
- [ ] `compliance_checker` - Check compliance
- [ ] `threat_detector` - Detect threats
- [ ] `incident_responder` - Respond to security incidents

---

### 6.3 Connector Marketplace

#### Features
- [ ] Community-built connectors
- [ ] Connector templates
- [ ] Quality certification
- [ ] Usage analytics

---

### 6.4 Documentation Site

#### Structure
```
docs/
├── getting-started/
│   ├── installation.md
│   ├── quickstart.md
│   └── first-skill.md
├── concepts/
│   ├── skills.md
│   ├── connectors.md
│   ├── agents.md
│   └── elastic-script.md
├── guides/
│   ├── building-skills.md
│   ├── testing-skills.md
│   ├── deploying-skills.md
│   └── best-practices.md
├── connectors/
│   ├── overview.md
│   ├── github.md
│   ├── jira.md
│   └── ...
├── reference/
│   ├── elastic-script.md
│   ├── cli.md
│   ├── api.md
│   └── functions.md
├── tutorials/
│   ├── build-monitor-skill.md
│   ├── build-incident-skill.md
│   └── ...
└── examples/
    └── ...
```

---

### 6.5 Community

#### Features
- [ ] GitHub Discussions
- [ ] Discord/Slack community
- [ ] Contributing guide
- [ ] Code of conduct
- [ ] Skill contribution guidelines
- [ ] Connector contribution guidelines

---

## Testing Strategy

### Test Pyramid

```
                    ╱╲
                   ╱  ╲
                  ╱ E2E╲           Notebooks, full workflows
                 ╱──────╲
                ╱        ╲
               ╱Integration╲       Component interactions
              ╱────────────╲
             ╱              ╲
            ╱     Unit       ╲     Individual functions
           ╱──────────────────╲
```

### Test Categories

1. **Unit Tests** (per class/function)
   - Run with `./gradlew test`
   - Fast, isolated, no external deps
   - Target: 80%+ coverage

2. **Integration Tests** (component interaction)
   - Run with `./gradlew integTest`
   - Uses test Elasticsearch cluster
   - Tests handlers, executors, registries

3. **E2E Tests** (full workflows)
   - Run with `./tests/e2e/run_tests.sh`
   - Jupyter notebooks as tests
   - Real-world scenarios

4. **Connector Tests**
   - Sandbox/test accounts for each service
   - Mock mode for CI
   - Real API tests for release

### CI/CD Pipeline

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  unit-tests:
    - ./gradlew test
  
  integration-tests:
    - Start ES cluster
    - ./gradlew integTest
  
  e2e-tests:
    - Start ES + Jupyter
    - ./tests/e2e/run_tests.sh
  
  lint:
    - ./gradlew checkstyle
    - moltler lint
  
  docs:
    - Build documentation
    - Deploy to GitHub Pages
```

---

## Release Schedule

### MVP (v0.1.0)
- [ ] Connector framework + 3 connectors
- [ ] Skill versioning
- [ ] CLI improvements
- [ ] Basic documentation

### Beta (v0.5.0)
- [ ] All Tier 1 connectors
- [ ] Skill testing framework
- [ ] GENERATE SKILL (AI)
- [ ] Python SDK
- [ ] Complete documentation

### GA (v1.0.0)
- [ ] Agent runtime
- [ ] Moltler Hub
- [ ] Pre-built skill packs
- [ ] Enterprise features
- [ ] Production hardening

---

## Success Metrics

### Adoption
- [ ] 1000+ GitHub stars
- [ ] 100+ skills in Hub
- [ ] 50+ connectors
- [ ] 10+ enterprise users

### Quality
- [ ] 90%+ test coverage
- [ ] <1% error rate in production
- [ ] <100ms p95 skill execution latency

### Community
- [ ] 50+ contributors
- [ ] 500+ Discord members
- [ ] 20+ community connectors

---

*Last updated: January 22, 2026*

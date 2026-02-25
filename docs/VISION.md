# Moltler: Vision & Technical Strategy

**Document Type:** Engineering Vision Document  
**Status:** Draft for Review  
**Author:** Moltler Team  
**Last Updated:** February 2026  

---

## Executive Summary

Moltler is a **skills framework for Elasticsearch** that enables users to build, share, and run reusable operations on their data. It addresses a fundamental gap in how organizations operationalize their Elasticsearch investments: the disconnect between platform capabilities and operational knowledge.

This document outlines the vision, problem space, technical approach, and strategic rationale for Moltler.

---

## Table of Contents

1. [The Problem](#the-problem)
2. [Vision](#vision)
3. [Why Now](#why-now)
4. [Technical Approach](#technical-approach)
5. [Why This Approach vs Alternatives](#why-this-approach-vs-alternatives)
6. [What Moltler Involves](#what-moltler-involves)
7. [Architecture](#architecture)
8. [Use Cases](#use-cases)
9. [Success Metrics](#success-metrics)
10. [Roadmap](#roadmap)
11. [Open Questions](#open-questions)

---

## The Problem

### Operational Knowledge is Trapped

Organizations invest heavily in Elasticsearch for observability, security, and search. They build sophisticated data pipelines, create detection rules, and develop operational procedures. But this knowledge is:

1. **Fragmented** - Spread across runbooks, wikis, Slack threads, and individual experts' heads
2. **Not reusable** - The same query patterns are reinvented across teams
3. **Not shareable** - Hard to package and distribute operational knowledge
4. **Not testable** - No way to validate that procedures still work
5. **Not versioned** - Changes aren't tracked, rollbacks are impossible

### The Expert Dependency Problem

When an incident occurs at 3 AM:
- Junior engineers struggle to know *what* to query
- Senior engineers get paged because they know *how* to investigate
- AI assistants can't help because they don't understand your data

The expertise required to effectively operate Elasticsearch is substantial:
- ES|QL or Query DSL syntax
- Index patterns and mappings
- Aggregation pipelines
- Domain-specific knowledge (what fields exist, what values mean)

### The AI Gap

AI coding assistants (Cursor, Claude, Copilot) are transforming development workflows. But they can't help with Elasticsearch operations because:

1. **No access** - They can't query your cluster
2. **No context** - They don't know your index patterns, fields, or data model
3. **No procedures** - They don't know your organization's operational playbooks

---

## Vision

**Make Elasticsearch operational knowledge as shareable and reusable as code.**

Moltler enables:

| Today | With Moltler |
|-------|--------------|
| Runbooks in Confluence | Executable skills in code |
| Copy-paste queries from Slack | `RUN SKILL get_recent_errors()` |
| "Ask Sarah, she knows" | AI assistant uses the same skill |
| Reinvent queries per team | Share skills via MoltlerHub |
| No validation | Skills tested in CI/CD |

### Core Principles

1. **Skills as Code** - Operational knowledge should be versioned, tested, and reviewed like any other code
2. **Run Where Your Data Lives** - Skills execute inside Elasticsearch, not external scripts calling APIs
3. **AI-Native** - Skills are discoverable and invocable by AI agents via MCP
4. **Community-Driven** - A hub for sharing skills, like npm for Node or PyPI for Python

---

## Why Now

### 1. AI Agents Need Tools

The rise of AI coding assistants (Cursor, Claude Code, Copilot) has created demand for tools that AI can use. MCP (Model Context Protocol) provides a standard interface. Moltler makes Elasticsearch operations available as MCP tools.

### 2. ES|QL Maturity

ES|QL (Elasticsearch Query Language) provides a SQL-like interface that's:
- Easier to read and write than Query DSL
- Composable (pipe operations)
- Suitable for procedural extension

### 3. Observability/Security Complexity

Modern observability and security involve:
- Multiple data sources (logs, metrics, traces, security events)
- Complex correlation (across services, time windows)
- Domain expertise (what constitutes an anomaly)

This complexity demands reusable, packaged solutions—not ad-hoc queries.

### 4. Platform Teams Need Leverage

Platform teams can't scale by answering tickets. They need to package their expertise into self-service tools that application teams can use independently.

---

## Technical Approach

### elastic-script: A Procedural Language for Elasticsearch

Moltler is powered by **elastic-script**, a procedural scripting language that runs *inside* Elasticsearch as a plugin. It extends ES|QL with:

```sql
CREATE SKILL get_recent_errors
  VERSION '1.0.0'
  DESCRIPTION 'Find recent error logs with context'
  (
    time_range STRING DEFAULT '1h',
    service STRING DEFAULT NULL
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;
  
  SET results = ESQL_QUERY(
    'FROM logs-* 
     | WHERE log.level == "ERROR" 
       AND @timestamp > NOW() - INTERVAL ' || time_range || '
     | SORT @timestamp DESC 
     | LIMIT 100'
  );
  
  IF service IS NOT NULL THEN
    SET results = ARRAY_FILTER(results, r -> r.service.name == service);
  END IF;
  
  RETURN results;
END SKILL;
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Plugin, not external service** | Runs inside ES for performance, security, no network hops |
| **Procedural extension of ES|QL** | Familiar SQL-like syntax, leverage ES|QL ecosystem |
| **Skills stored in ES index** | Skills are data, searchable, replicable |
| **MCP-native** | First-class AI agent integration |
| **Statically typed parameters** | Validation, documentation, IDE support |

### Why a New Language?

We evaluated alternatives:

| Approach | Pros | Cons |
|----------|------|------|
| **Painless scripts** | Built-in | No control flow, limited scope, hard to test |
| **External Python/JS** | Full language | Network latency, auth complexity, deployment burden |
| **Stored queries** | Simple | No parameters, no logic, no reuse |
| **elastic-script** | Full control flow, runs in ES, testable | New language to learn |

elastic-script exists because:
1. Painless is too limited (no procedures, no ES|QL)
2. External scripts add operational burden
3. We need *procedural* logic (if/then, loops, error handling)
4. We need *metadata* (versioning, descriptions, parameters)

---

## Why This Approach vs Alternatives

### Alternative 1: Just Use ES|QL

**Argument:** "ES|QL is powerful enough, just save queries."

**Why insufficient:**
- No parameters - can't customize queries at runtime
- No control flow - can't branch based on results
- No composition - can't call one query from another
- No metadata - no versioning, descriptions, or testing

### Alternative 2: External Scripting (Python/JS)

**Argument:** "Write Python scripts that call the ES API."

**Why insufficient:**
- **Operational burden** - Deploy, manage, monitor external services
- **Network latency** - Round trips for every query
- **Auth complexity** - Manage credentials outside ES
- **Not ES-native** - Can't leverage ES security, replication, clustering

### Alternative 3: Kibana Saved Objects

**Argument:** "Save searches in Kibana."

**Why insufficient:**
- UI-centric, not API-first
- No versioning or testing
- Can't compose or parameterize
- Not AI-accessible

### Alternative 4: Transform/Ingest Pipelines

**Argument:** "Use built-in ES pipelines."

**Why insufficient:**
- Different purpose (ETL, not operations)
- No ad-hoc execution
- Can't return results to users
- Limited control flow

### Our Approach: Skills as First-Class Citizens

Moltler treats operational procedures as **first-class citizens** with:
- Versioning
- Parameters with types and defaults
- Descriptions for humans and AI
- Test frameworks
- Package management (CLI, Hub)
- AI agent integration (MCP)

---

## What Moltler Involves

### 1. elastic-script Plugin

The core runtime that executes skills inside Elasticsearch.

- **Language features:** Variables, control flow, functions, error handling
- **ES|QL integration:** `ESQL_QUERY()` built-in function
- **150+ built-in functions:** String, array, date, document, HTTP, LLM, Kibana, etc.
- **Stored procedures:** Skills persisted in `.escript_procedures` index

### 2. Skill Package Format

Skills are defined in `.sql` files with metadata in `.yaml`:

```
skills/
└── elastic/
    └── observability/
        └── get-recent-errors/
            ├── skill.sql      # The skill code
            ├── skill.yaml     # Metadata (name, version, tags)
            └── tests.yaml     # Test cases
```

### 3. MoltlerHub

Web portal for discovering and sharing skills: **https://hub.moltler.dev**

- Browse 180+ community skills
- Filter by category (observability, security, search, etc.)
- View skill documentation and examples
- Generate installation commands
- Skill Builder for creating new skills

### 4. Moltler CLI

Command-line tool for managing skills:

```bash
./moltler-cli.sh install elastic/observability/get-recent-errors
./moltler-cli.sh install --all
./moltler-cli.sh run get-recent-errors --time_range 2h
./moltler-cli.sh list
```

### 5. MCP Bridge

Connects AI agents to Moltler skills via Model Context Protocol:

- Python server that bridges MCP to Elasticsearch
- Skills exposed as MCP tools
- Works with Cursor, Claude, VS Code, etc.

### 6. Test Framework

Automated validation for skills:

```yaml
# tests.yaml
tests:
  - name: "Returns results for valid time range"
    params:
      time_range: "1h"
    expect:
      status: success
      result_type: array
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           USER INTERFACES                           │
├──────────────┬──────────────┬──────────────┬───────────────────────┤
│   REST API   │     CLI      │  MoltlerHub  │     AI Agents         │
│  /_escript   │ moltler-cli  │   (Web UI)   │ (Cursor, Claude, etc) │
└──────┬───────┴──────┬───────┴──────┬───────┴───────────┬───────────┘
       │              │              │                   │
       └──────────────┴──────────────┴───────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        MCP BRIDGE (Optional)                        │
│                   moltler_mcp_server.py                             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         ELASTICSEARCH                               │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    elastic-script Plugin                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐│  │
│  │  │   Parser    │  │  Executor   │  │    Built-in Functions   ││  │
│  │  │  (ANTLR4)   │  │  (Handlers) │  │  (150+ functions)       ││  │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘│  │
│  │  ┌─────────────────────────────────────────────────────────┐  │  │
│  │  │              Procedure Storage (.escript_procedures)    │  │  │
│  │  └─────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │                    Your Data Indices                            ││
│  │          logs-*, metrics-*, apm-*, security-*, etc.             ││
│  └─────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **User/AI** invokes skill via REST, CLI, or MCP
2. **elastic-script plugin** parses and executes the skill
3. **Built-in functions** (like `ESQL_QUERY`) access data indices
4. **Results** returned to caller

---

## Use Cases

### 1. Observability: Incident Investigation

**Before:** Engineer searches Kibana manually, writes ad-hoc queries, asks senior team member for help.

**After:**
```
User: "I got paged for payment-service. What's happening?"

AI: [Uses get_recent_errors skill]
    "I found 47 errors in payment-service in the last hour.
     Most common: 'Connection timeout to payment-gateway'.
     Let me check transaction latency..."
    
    [Uses get_slow_transactions skill]
    "Transaction times spiked at 9:45 AM. P99 went from 200ms to 12s."
```

### 2. Security: Threat Hunting

**Before:** Security analyst manually queries across indices, correlates in spreadsheets.

**After:**
```sql
RUN SKILL hunt_ioc WITH ioc = '192.168.1.100';
-- Returns: all matches across logs, auth, network flow, DNS
```

### 3. Platform: Self-Service Health Checks

**Before:** Application teams open tickets asking "is my cluster healthy?"

**After:**
```sql
RUN SKILL analyze_my_cluster();
-- Returns: index health, shard distribution, recommendations
```

### 4. Search: Query Analysis

**Before:** Search relevance issues require expert investigation.

**After:**
```sql
RUN SKILL analyze_search_quality WITH index = 'products';
-- Returns: click-through rates, zero-result queries, suggestions
```

---

## Success Metrics

### Adoption Metrics

| Metric | Target (6 months) |
|--------|-------------------|
| Skills in Hub | 250+ |
| GitHub stars | 1,000+ |
| Active installations | 500+ |
| MCP connections | 1,000+ |
| Community contributors | 20+ |

### Technical Metrics

| Metric | Target |
|--------|--------|
| Skill execution P99 | < 500ms (simple skills) |
| CI/CD pass rate | 100% |
| Supported ES versions | 8.x, 9.x |

### User Success Metrics

| Metric | Target |
|--------|--------|
| Time to first skill run | < 10 minutes |
| Skills per active user | 5+ |
| Skill test coverage | > 80% |

---

## Roadmap

### Phase 1: Foundation (Complete)
- [x] elastic-script language and plugin
- [x] 180+ community skills
- [x] MoltlerHub web portal
- [x] Moltler CLI
- [x] MCP bridge for AI agents
- [x] CI/CD and testing framework

### Phase 2: Scale & Polish (Current)
- [ ] Kibana integration (run skills from Kibana)
- [ ] Skill versioning and upgrades
- [ ] Private skill registries (enterprise)
- [ ] Performance optimization
- [ ] Multi-cluster support

### Phase 3: Enterprise
- [ ] RBAC for skills (who can run what)
- [ ] Audit logging
- [ ] Skill marketplace (paid skills)
- [ ] SaaS offering (hosted MoltlerHub)

### Phase 4: Intelligence
- [ ] AI-generated skills from natural language
- [ ] Skill recommendations based on data
- [ ] Automatic skill optimization

---

## Open Questions

### Technical

1. **Skill isolation:** How do we prevent skills from affecting each other or the cluster?
2. **Resource limits:** How do we limit CPU/memory per skill execution?
3. **Multi-tenancy:** How do skills work in multi-tenant deployments?

### Product

1. **Monetization:** Should there be a paid tier? What features?
2. **Enterprise vs Community:** What's the line between open source and enterprise?
3. **Elastic relationship:** How does Moltler relate to official Elastic products?

### Community

1. **Contribution guidelines:** How do we ensure skill quality?
2. **Governance:** Who decides what skills are "official"?
3. **Support model:** How do users get help?

---

## Conclusion

Moltler represents a new paradigm for Elasticsearch operations: **Skills as Code**. By treating operational knowledge as versioned, testable, shareable artifacts, we unlock:

1. **Leverage** - Build once, run everywhere
2. **Quality** - Test skills like code
3. **Collaboration** - Share via Hub
4. **AI-readiness** - Native MCP support

The foundation is built. The community is growing. Now is the time to shape the future of Elasticsearch operations.

---

## Appendix: Key Links

| Resource | URL |
|----------|-----|
| MoltlerHub | https://hub.moltler.dev |
| GitHub | https://github.com/bahaaldine/moltler |
| Documentation | https://bahaaldine.github.io/moltler |
| Connect AI Tools | https://hub.moltler.dev/connect |

---

*This document is a living artifact. Feedback welcome via GitHub issues or internal channels.*

# Moltler Skills Architecture

This document explains the three-layer architecture of Moltler capabilities and how they relate to the [Cursor Agent Skills standard](https://cursor.com/docs/context/skills).

## Three Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                     AGENT SKILLS (Layer 3)                       │
│   .agents/skills/ - Instructions for AI agents (Cursor, etc.)   │
│   Format: SKILL.md with YAML frontmatter                        │
│   Purpose: Teach agents WHEN and HOW to use capabilities        │
└─────────────────────────────────────────────────────────────────┘
                              ↓ references
┌─────────────────────────────────────────────────────────────────┐
│                    MOLTLER SKILLS (Layer 2)                      │
│   hub/skills/ - Reusable procedures (elastic-script)            │
│   Format: skill.yaml + skill.sql                                │
│   Purpose: Pre-built automation that combines tools             │
└─────────────────────────────────────────────────────────────────┘
                              ↓ uses
┌─────────────────────────────────────────────────────────────────┐
│                       TOOLS (Layer 1)                            │
│   Built-in functions in elastic-script                          │
│   Format: Java classes (e.g., ES_INDEX, ES_SEARCH)              │
│   Purpose: Atomic operations that do one thing                  │
└─────────────────────────────────────────────────────────────────┘
```

## Layer 1: Tools (Built-in Functions)

**Location:** `elastic-script/src/main/java/.../functions/builtin/`

These are atomic functions that perform single operations:

```sql
-- Examples of tools
ES_INDEX('users', {'name': 'John'})     -- Index a document
ES_SEARCH('logs', {'query': {...}})     -- Search documents
ES_CLUSTER_HEALTH()                      -- Get cluster health
SLACK_SEND('#alerts', 'Hello')          -- Send Slack message
```

**Characteristics:**
- Implemented in Java
- Single responsibility
- No business logic
- 320+ functions across 32 categories

## Layer 2: Moltler Skills (Procedures)

**Location:** `hub/skills/`

These combine multiple tools into reusable automation:

```sql
-- Example: cluster_health_check skill
CREATE SKILL cluster_health_check
VERSION '1.0.0'
BEGIN
    DECLARE health DOCUMENT;
    DECLARE stats DOCUMENT;
    
    SET health = ES_CLUSTER_HEALTH();  -- Uses tool
    SET stats = ES_CLUSTER_STATS();    -- Uses tool
    
    -- Combines and processes results
    RETURN {
        'status': health['status'],
        'nodes': health['number_of_nodes'],
        ...
    };
END SKILL;
```

**Characteristics:**
- Written in elastic-script
- Combine multiple tools
- Include business logic
- Stored in Elasticsearch
- 200+ skills across categories

## Layer 3: Agent Skills (Instructions)

**Location:** `.agents/skills/`

These teach AI agents (Cursor, Claude, etc.) when and how to use the capabilities:

```markdown
---
name: cluster-management
description: Monitor and manage Elasticsearch cluster health...
---

# Cluster Management

## When to Use
- User asks about cluster health
- User needs to check node status
...

## Available Functions
| Function | Description |
|----------|-------------|
| ES_CLUSTER_HEALTH() | Get cluster health |
...

## Pre-built Skills (Moltler)
| Skill | Description |
|-------|-------------|
| cluster_health_check() | Comprehensive health report |
...
```

**Characteristics:**
- Markdown with YAML frontmatter
- Instructions, not code
- Reference both tools and Moltler skills
- Discoverable by AI agents
- 10 skill categories

## Directory Structure

```
moltler/
├── .agents/
│   └── skills/                    # Agent Skills (Layer 3)
│       ├── moltler-index/
│       │   └── SKILL.md
│       ├── elasticsearch-ops/
│       │   ├── SKILL.md
│       │   └── scripts/
│       │       └── bulk_reindex.sql
│       ├── search-query/
│       │   └── SKILL.md
│       └── ...
│
├── hub/
│   └── skills/                    # Moltler Skills (Layer 2)
│       ├── elastic/
│       │   ├── observability/
│       │   │   └── get-recent-errors/
│       │   │       ├── skill.yaml
│       │   │       ├── skill.sql
│       │   │       └── tests.yaml
│       │   ├── security/
│       │   ├── search/
│       │   └── ...
│       └── sfdc/
│
├── elastic-script/
│   └── src/main/java/.../functions/
│       └── builtin/               # Tools (Layer 1)
│           ├── elasticsearch/
│           │   ├── DocumentApiFunctions.java
│           │   ├── SearchApiFunctions.java
│           │   └── ...
│           ├── kibana/
│           └── ...
│
└── moltler-hub/                   # Web UI for browsing
    └── src/
```

## Publishing Strategy

### For AI Agents (Cursor, Claude, etc.)

The `.agents/skills/` directory is automatically discovered by Cursor. Users can also install via GitHub:

```
Cursor Settings → Rules → Add Rule → Remote Rule (GitHub)
→ https://github.com/bahaaldine/moltler.git
```

Skills appear in Cursor's "Agent Decides" section and can be invoked via `/skill-name`.

### For MoltlerHub (Web UI)

The `moltler-hub/` Next.js app displays skills from `hub/skills/`. Skills are:
1. Parsed from `skill.yaml` files
2. Generated into `src/data/skills.ts`
3. Displayed with categories, parameters, and examples

### For Direct Use (elastic-script)

Users can run Moltler skills directly:

```sql
-- Install a skill
INSTALL SKILL FROM 'https://github.com/bahaaldine/moltler/hub/skills/elastic/observability/get-recent-errors';

-- Run the skill
RUN SKILL get_recent_errors(60, 50);
```

### For npm/pip Packages

The Python SDK (`moltler` package) provides programmatic access:

```python
from moltler import Moltler

client = Moltler()
result = client.skills.run('get_recent_errors', minutes=60)
```

## How to Add New Capabilities

### Adding a Tool (Layer 1)

1. Create Java class in `functions/builtin/`
2. Implement function with `@FunctionSpec` annotation
3. Register in `BuiltInFunctionRegistry`
4. Add tests

### Adding a Moltler Skill (Layer 2)

1. Create directory in `hub/skills/elastic/{category}/{skill-name}/`
2. Create `skill.yaml` with metadata
3. Create `skill.sql` with elastic-script code
4. Optionally add `tests.yaml`
5. Run sync script to update MoltlerHub

### Adding an Agent Skill (Layer 3)

1. Create or update SKILL.md in `.agents/skills/{category}/`
2. Include YAML frontmatter (name, description)
3. Document when to use, available functions, examples
4. Optionally add scripts in `scripts/` directory

## Relationship Summary

| Layer | What | Where | Format | Purpose |
|-------|------|-------|--------|---------|
| 3 | Agent Skills | `.agents/skills/` | SKILL.md | Teach AI agents |
| 2 | Moltler Skills | `hub/skills/` | skill.yaml + .sql | Reusable automation |
| 1 | Tools | Java code | .java | Atomic operations |

## Publishing Checklist

When adding new capabilities:

- [ ] **Layer 1**: Add tool function if needed
- [ ] **Layer 2**: Create Moltler skill combining tools
- [ ] **Layer 3**: Update Agent Skill to reference new capability
- [ ] **MoltlerHub**: Regenerate skills.ts for web UI
- [ ] **Documentation**: Update CLAUDE.md function count

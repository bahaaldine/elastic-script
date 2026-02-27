# Moltler Agent Skills

This directory contains Agent Skills following the [Cursor Agent Skills standard](https://cursor.com/docs/context/skills). These skills teach AI agents (Cursor, Claude, etc.) how to use Moltler/elastic-script capabilities.

## Installation

### In Cursor

Agent Skills are automatically discovered from the `.agents/skills/` directory when you open this project in Cursor.

To install from GitHub into any project:

1. Open Cursor Settings (`Cmd+Shift+J` / `Ctrl+Shift+J`)
2. Navigate to **Rules**
3. Click **Add Rule** → **Remote Rule (GitHub)**
4. Enter: `https://github.com/bahaaldine/moltler.git`

### In Other AI Agents

Skills follow the open Agent Skills standard. Check your agent's documentation for installation instructions.

## Available Skills

| Skill | Description | Invoke |
|-------|-------------|--------|
| `moltler-index` | Master index of all skills - use this first | `/moltler-index` |
| `elasticsearch-ops` | Document CRUD, index management | `/elasticsearch-ops` |
| `search-query` | Search, ES\|QL, aggregations | `/search-query` |
| `cluster-management` | Cluster health, nodes, tasks | `/cluster-management` |
| `observability` | Logs, metrics, traces, APM | `/observability` |
| `security-ops` | Users, roles, API keys | `/security-ops` |
| `ml-inference` | ML jobs, embeddings, LLMs | `/ml-inference` |
| `data-management` | ILM, snapshots, pipelines | `/data-management` |
| `alerting-response` | Alerts, notifications | `/alerting-response` |
| `integrations` | AWS, K8s, CI/CD | `/integrations` |

## Usage

### Automatic Invocation

Skills are automatically invoked by the agent when relevant. For example, if you ask "check my cluster health", the agent will automatically use the `cluster-management` skill.

### Manual Invocation

Type `/skill-name` in chat to explicitly invoke a skill:

```
/moltler-index
/elasticsearch-ops
/observability
```

## Structure

Each skill directory contains:

```
skill-name/
├── SKILL.md          # Main skill definition (required)
├── scripts/          # Optional executable scripts
│   └── example.sql
├── references/       # Optional additional documentation
└── assets/           # Optional static resources
```

## Creating New Skills

1. Create a new directory under `.agents/skills/`
2. Create a `SKILL.md` file with YAML frontmatter:

```markdown
---
name: my-skill
description: Short description for agent context matching
---

# My Skill

Instructions for the agent...

## When to Use
- Use when...

## Available Functions
- `ES_FUNCTION()` - Does something
```

3. Optionally add `scripts/`, `references/`, or `assets/` directories

## Relationship to Moltler Skills

| Layer | Location | Format | Purpose |
|-------|----------|--------|---------|
| Agent Skills | `.agents/skills/` | SKILL.md | Instructions for AI agents |
| Moltler Skills | `hub/skills/` | skill.yaml + .sql | Runnable procedures |
| Tools | Java code | .java | Atomic functions |

Agent Skills reference both Moltler Skills (pre-built procedures) and Tools (built-in functions).

## Learn More

- [Agent Skills Standard](https://agentskills.io/)
- [Cursor Documentation](https://cursor.com/docs/context/skills)
- [Moltler Documentation](https://bahaaldine.github.io/moltler/)
- [Architecture Guide](../../docs/SKILLS_ARCHITECTURE.md)

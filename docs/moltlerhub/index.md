# MoltlerHub

The official skill repository for Moltler - **155 skills** across 13 categories.

---

## Quick Start

```bash
# Install all skills
cd hub
./moltler-cli.sh install --all

# List installed skills
./moltler-cli.sh installed

# Run a skill
./moltler-cli.sh run get-recent-errors
```

**Want to contribute a skill?** See the [Contributing Guide](contributing.md).

---

## Current Status

MoltlerHub is currently a **GitHub-based repository** at `hub/skills/`. Skills are:

- Installed via CLI: `./moltler-cli.sh install <skill-name>`
- Exposed via MCP endpoint: `/_escript/mcp`
- Browsable in the `hub/` directory

## Vision (Roadmap)

MoltlerHub will become a full marketplace where:

- **Users** discover and install skills created by the community
- **Contributors** publish and share skills they've built
- **AI Agents** programmatically explore available capabilities

Think of it as **npm for Elasticsearch skills** or **Docker Hub for runbooks**.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MoltlerHub                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Official   │  │  Community  │  │  Verified   │  │   Private   │        │
│  │   Skills    │  │   Skills    │  │  Partners   │  │  (Org Only) │        │
│  │             │  │             │  │             │  │             │        │
│  │ • elastic/  │  │ • user/     │  │ • datadog/  │  │ • acme-corp/│        │
│  │   logs      │  │   custom    │  │   metrics   │  │   internal  │        │
│  │ • elastic/  │  │ • user/     │  │ • pagerduty/│  │             │        │
│  │   apm       │  │   workflow  │  │   oncall    │  │             │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                           Discovery APIs                                     │
│  • Web UI (hub.moltler.dev)                                                 │
│  • CLI (moltler search, moltler install)                                    │
│  • MCP (tools/list, tools/search)                                           │
│  • REST API (/api/v1/skills)                                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Core Concepts

### Skill Packages

A skill package contains everything needed to use a skill:

```
@elastic/logs-analyzer
├── skill.yaml           # Skill metadata
├── skill.sql            # Skill definition (CREATE SKILL ...)
├── README.md            # Documentation
├── examples/            # Usage examples
│   ├── basic.sql
│   └── advanced.sql
├── tests/               # Test cases
│   └── test_skill.sql
└── CHANGELOG.md         # Version history
```

### Namespaces

Skills are organized by publisher namespace:

| Namespace | Description | Example |
|-----------|-------------|---------|
| `@elastic/` | Official Elastic skills | `@elastic/apm-service-health` |
| `@verified/` | Verified partner skills | `@verified/datadog-metrics` |
| `@community/` | Community-contributed | `@community/k8s-pod-analyzer` |
| `@{username}/` | User-published skills | `@johndoe/custom-alerter` |
| `@{org}/` | Organization private | `@acme-corp/internal-workflow` |

### Versioning

Skills follow semantic versioning:

```
@elastic/log-analyzer@1.2.3
                      │ │ │
                      │ │ └── Patch: Bug fixes
                      │ └──── Minor: New features, backward compatible
                      └────── Major: Breaking changes
```

---

## User Experience

### Web UI (hub.moltler.dev)

**Homepage**
```
┌─────────────────────────────────────────────────────────────────┐
│  🔍 Search skills...                              [Search]      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  📦 Featured Skills                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │ @elastic/    │ │ @elastic/    │ │ @elastic/    │            │
│  │ log-analyzer │ │ apm-health   │ │ siem-hunt    │            │
│  │ ⭐ 4.8 (234) │ │ ⭐ 4.9 (189) │ │ ⭐ 4.7 (156) │            │
│  │ 12.3k installs│ │ 8.7k installs│ │ 5.2k installs│            │
│  └──────────────┘ └──────────────┘ └──────────────┘            │
│                                                                 │
│  📂 Categories                                                  │
│  [Observability] [Security] [Search] [ML] [Integrations]       │
│                                                                 │
│  🔥 Trending This Week                                          │
│  1. @community/k8s-troubleshooter  ↑ 234%                      │
│  2. @elastic/cloud-cost-analyzer   ↑ 156%                      │
│  3. @verified/slack-incident-bot   ↑ 89%                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Skill Detail Page**
```
┌─────────────────────────────────────────────────────────────────┐
│  @elastic/log-analyzer                           [Install]      │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│  Analyze application logs for errors, patterns, and anomalies   │
│                                                                 │
│  ⭐ 4.8 (234 reviews)  │  📥 12.3k installs  │  📅 Updated 2d ago│
│                                                                 │
│  [Overview] [Versions] [Dependencies] [Examples] [Reviews]      │
│  ───────────────────────────────────────────────────────────────│
│                                                                 │
│  ## Installation                                                │
│                                                                 │
│  ```bash                                                        │
│  moltler install @elastic/log-analyzer                          │
│  ```                                                            │
│                                                                 │
│  ## Usage                                                       │
│                                                                 │
│  ```sql                                                         │
│  RUN SKILL @elastic/log-analyzer                                │
│    WITH index_pattern = 'logs-*'                                │
│    AND time_range = '24h'                                       │
│  ```                                                            │
│                                                                 │
│  ## Parameters                                                  │
│  ┌────────────────┬──────────┬─────────────────────────────┐   │
│  │ Parameter      │ Type     │ Description                 │   │
│  ├────────────────┼──────────┼─────────────────────────────┤   │
│  │ index_pattern  │ STRING   │ Index pattern to search     │   │
│  │ time_range     │ STRING   │ Time range (e.g., 1h, 24h)  │   │
│  │ min_severity   │ STRING   │ Minimum log level           │   │
│  └────────────────┴──────────┴─────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### CLI

```bash
# Search for skills
moltler search "log analyzer"
moltler search --category observability

# View skill details
moltler info @elastic/log-analyzer

# Install a skill
moltler install @elastic/log-analyzer
moltler install @elastic/log-analyzer@1.2.3  # Specific version

# List installed skills
moltler list

# Update skills
moltler update @elastic/log-analyzer
moltler update --all

# Uninstall
moltler uninstall @elastic/log-analyzer

# Publish your skill
moltler publish ./my-skill/
```

### MCP Integration (for AI Agents)

```json
// List all available skills in the hub
{
  "jsonrpc": "2.0",
  "method": "hub/search",
  "params": {
    "query": "analyze logs",
    "category": "observability",
    "limit": 10
  },
  "id": 1
}

// Get skill details
{
  "jsonrpc": "2.0",
  "method": "hub/get",
  "params": {
    "name": "@elastic/log-analyzer"
  },
  "id": 2
}

// Install skill to local instance
{
  "jsonrpc": "2.0",
  "method": "hub/install",
  "params": {
    "name": "@elastic/log-analyzer",
    "version": "latest"
  },
  "id": 3
}
```

---

## Contributor Experience

### Publishing a Skill

**1. Create skill package structure**

```bash
mkdir my-awesome-skill
cd my-awesome-skill
moltler init  # Creates template
```

**2. Define your skill**

`skill.yaml`:
```yaml
name: my-awesome-skill
version: 1.0.0
description: Does something awesome with your data
author: johndoe
license: Apache-2.0

# Categorization
category: observability
tags:
  - logs
  - analysis
  - troubleshooting

# Dependencies (other skills this depends on)
dependencies:
  "@elastic/esql-helpers": "^1.0.0"

# Elasticsearch requirements
requirements:
  elasticsearch: ">=8.0.0"
  features:
    - esql
    - ml  # Optional, for ML-powered skills

# Entry point
main: skill.sql
```

`skill.sql`:
```sql
CREATE SKILL my_awesome_skill
  VERSION '1.0.0'
  DESCRIPTION 'Does something awesome with your data. Use this when you need to analyze patterns or troubleshoot issues.'
  AUTHOR 'johndoe'
  TAGS ['observability', 'analysis']
  (
    input_param STRING DESCRIPTION 'The input to process'
  )
  RETURNS DOCUMENT
BEGIN
  -- Skill implementation
  RETURN {'result': 'awesome'};
END SKILL;
```

**3. Test locally**

```bash
moltler test ./
```

**4. Publish**

```bash
moltler login
moltler publish ./
```

### Skill Review Process

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Submit    │────▶│   Review    │────▶│   Approve   │────▶│  Published  │
│   (PR/API)  │     │  (Automated │     │  (Manual    │     │  (Live on   │
│             │     │   + Human)  │     │   for some) │     │   Hub)      │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │
                    ┌──────┴──────┐
                    │  Checks:    │
                    │  • Syntax   │
                    │  • Security │
                    │  • Tests    │
                    │  • Docs     │
                    └─────────────┘
```

**Automated Checks:**
- Syntax validation (valid SQL/skill definition)
- Security scan (no dangerous operations)
- Test execution (all tests pass)
- Documentation check (README exists)
- License check (valid OSS license)

**Manual Review (for verified status):**
- Code quality review
- Best practices compliance
- No malicious behavior

---

## Technical Architecture

### Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MoltlerHub                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │   Hub Web UI    │  │    Hub API      │  │  Hub Registry   │             │
│  │   (React)       │  │   (REST/MCP)    │  │  (ES Index)     │             │
│  │                 │  │                 │  │                 │             │
│  │  • Browse       │  │  • /search      │  │ .moltlerhub-    │             │
│  │  • Search       │  │  • /packages    │  │   skills        │             │
│  │  • Install      │  │  • /publish     │  │ .moltlerhub-    │             │
│  │  • Publish      │  │  • /download    │  │   users         │             │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘             │
│           │                    │                    │                       │
│           └────────────────────┼────────────────────┘                       │
│                                │                                            │
│                    ┌───────────▼───────────┐                               │
│                    │   GitHub Repository   │                               │
│                    │   (Source of Truth)   │                               │
│                    │                       │                               │
│                    │  moltler/hub          │                               │
│                    │  ├── skills/          │                               │
│                    │  │   ├── elastic/     │                               │
│                    │  │   ├── community/   │                               │
│                    │  │   └── verified/    │                               │
│                    │  └── .github/         │                               │
│                    │      └── workflows/   │                               │
│                    └───────────────────────┘                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Model

**Skill Package (Elasticsearch Index)**

```json
{
  "name": "@elastic/log-analyzer",
  "version": "1.2.3",
  "description": "Analyze application logs for errors and patterns",
  "author": "elastic",
  "license": "Elastic-2.0",
  "category": "observability",
  "tags": ["logs", "analysis", "errors"],
  "created_at": "2026-01-15T10:00:00Z",
  "updated_at": "2026-02-10T14:30:00Z",
  "downloads": 12345,
  "stars": 234,
  "verified": true,
  "skill_definition": "CREATE SKILL log_analyzer ...",
  "readme": "# Log Analyzer\n\nAnalyze your logs...",
  "examples": [...],
  "parameters": [
    {
      "name": "index_pattern",
      "type": "STRING",
      "required": false,
      "default": "logs-*",
      "description": "Index pattern to search"
    }
  ],
  "dependencies": [],
  "requirements": {
    "elasticsearch": ">=8.0.0"
  },
  "versions": [
    {"version": "1.2.3", "published_at": "2026-02-10T14:30:00Z"},
    {"version": "1.2.2", "published_at": "2026-02-01T10:00:00Z"},
    {"version": "1.0.0", "published_at": "2026-01-15T10:00:00Z"}
  ]
}
```

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/skills` | GET | List/search skills |
| `/api/v1/skills/{name}` | GET | Get skill details |
| `/api/v1/skills/{name}/versions` | GET | List versions |
| `/api/v1/skills/{name}/download` | GET | Download skill package |
| `/api/v1/skills` | POST | Publish new skill |
| `/api/v1/skills/{name}` | PUT | Update skill |
| `/api/v1/skills/{name}` | DELETE | Unpublish skill |
| `/api/v1/skills/{name}/star` | POST | Star a skill |
| `/api/v1/skills/{name}/reviews` | GET/POST | Reviews |
| `/api/v1/categories` | GET | List categories |
| `/api/v1/users/{username}/skills` | GET | User's skills |

---

## Agent Integration

### Hub Discovery Skills

Meta-skills that let AI agents explore the Hub:

```sql
-- Search the MoltlerHub
CREATE SKILL hub_search
  DESCRIPTION 'Search MoltlerHub for skills by keyword, category, or capability. Use this to find skills that can help accomplish a task.'
  (
    query STRING DESCRIPTION 'Search query',
    category STRING DEFAULT NULL DESCRIPTION 'Filter by category',
    limit INT DEFAULT 10 DESCRIPTION 'Max results'
  )
  RETURNS ARRAY
BEGIN
  -- Query the hub registry
  RETURN HTTP_GET('https://hub.moltler.dev/api/v1/skills', {
    'q': query,
    'category': category,
    'limit': limit
  });
END SKILL;

-- Get skill details from hub
CREATE SKILL hub_get_skill
  DESCRIPTION 'Get detailed information about a skill from MoltlerHub'
  (
    skill_name STRING DESCRIPTION 'Full skill name (e.g., @elastic/log-analyzer)'
  )
  RETURNS DOCUMENT
BEGIN
  RETURN HTTP_GET('https://hub.moltler.dev/api/v1/skills/' || skill_name);
END SKILL;

-- Install skill from hub
CREATE SKILL hub_install
  DESCRIPTION 'Install a skill from MoltlerHub to the local Moltler instance'
  (
    skill_name STRING DESCRIPTION 'Full skill name',
    version STRING DEFAULT 'latest' DESCRIPTION 'Version to install'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE skill_def = HTTP_GET('https://hub.moltler.dev/api/v1/skills/' || skill_name || '/download');
  -- Execute the skill definition
  EXECUTE IMMEDIATE skill_def.skill_sql;
  RETURN {'installed': skill_name, 'version': skill_def.version};
END SKILL;

-- Recommend skills based on context
CREATE SKILL hub_recommend
  DESCRIPTION 'Get skill recommendations based on current context or use case'
  (
    use_case STRING DESCRIPTION 'Description of what you want to accomplish',
    installed_skills ARRAY DEFAULT [] DESCRIPTION 'Already installed skills'
  )
  RETURNS ARRAY
BEGIN
  RETURN HTTP_POST('https://hub.moltler.dev/api/v1/recommend', {
    'use_case': use_case,
    'installed': installed_skills
  });
END SKILL;
```

### Agent Workflow Example

```
User: "I need to investigate why my API is slow"

Agent:
1. Calls hub_search("api performance slow latency")
2. Finds: @elastic/apm-latency-analyzer, @elastic/trace-investigator
3. Calls hub_install("@elastic/apm-latency-analyzer")
4. Runs: RUN SKILL apm_latency_analyzer WITH service='api-gateway'
5. Returns: "Found 3 slow endpoints. Top issue: /api/v1/search averaging 2.3s"
```

---

## Implementation Phases

### Phase 1: Foundation (4 weeks)

**GitHub-based Registry**
- [ ] Create `moltler/hub` repository structure
- [ ] Define skill package format (`skill.yaml`, `skill.sql`)
- [ ] Build GitHub Actions for validation/publishing
- [ ] Create initial set of @elastic/* official skills

**Basic CLI**
- [ ] `moltler search` - Search skills in repo
- [ ] `moltler install` - Install from repo
- [ ] `moltler list` - List installed skills

### Phase 2: Web UI (4 weeks)

**Hub Website**
- [ ] React-based web UI
- [ ] Skill browsing and search
- [ ] Skill detail pages
- [ ] Category navigation
- [ ] User authentication (GitHub OAuth)

### Phase 3: API & MCP (3 weeks)

**REST API**
- [ ] All CRUD endpoints
- [ ] Search and filtering
- [ ] Download endpoint
- [ ] Rate limiting

**MCP Integration**
- [ ] `hub/search` method
- [ ] `hub/get` method
- [ ] `hub/install` method

### Phase 4: Community Features (3 weeks)

**Social Features**
- [ ] Star/favorite skills
- [ ] Reviews and ratings
- [ ] Download statistics
- [ ] User profiles

**Publishing**
- [ ] Web-based skill submission
- [ ] Automated validation
- [ ] Verification badges

### Phase 5: Enterprise (Future)

**Private Registries**
- [ ] Organization namespaces
- [ ] Private skill hosting
- [ ] RBAC for skill access
- [ ] Enterprise SSO

---

## See Also

- [Skills Roadmap](skills-roadmap.md) - Complete list of planned skills
- [Creating Skills](creating-skills.md) - How to build skills
- [MCP Bridge](../tools/mcp-bridge.md) - AI agent integration

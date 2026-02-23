# Quick Start

Get Moltler running on your existing Elasticsearch cluster in 3 steps.

---

## Prerequisites

- Elasticsearch 9.x running
- Admin access to install plugins
- `curl` and `git` installed

---

## Step 1: Install the Plugin

Download and install the plugin on **each node** in your cluster:

```bash
# Download the plugin (check releases for your ES version)
wget https://github.com/bahaaldine/moltler/releases/download/v1.0.0/x-pack-escript-9.4.0-SNAPSHOT.zip

# Install the plugin
sudo elasticsearch-plugin install file:///path/to/x-pack-escript-9.4.0-SNAPSHOT.zip

# Restart Elasticsearch
sudo systemctl restart elasticsearch
```

**Verify the plugin is loaded:**

```bash
curl -u elastic:password http://localhost:9200/_cat/plugins | grep escript
```

---

## Step 2: Install Skills

**One-liner installation:**

```bash
curl -sSL https://hub.moltler.dev/install.sh | bash
```

**With custom credentials:**

```bash
curl -sSL https://hub.moltler.dev/install.sh | bash -s -- \
  --es-url https://your-cluster:9200 \
  --es-user elastic \
  --es-password your-password
```

**Install only specific categories:**

```bash
# Just observability skills
curl -sSL https://hub.moltler.dev/install.sh | bash -s -- --category observability

# Just security skills
curl -sSL https://hub.moltler.dev/install.sh | bash -s -- --category security
```

---

## Step 3: Run Your First Skill

```bash
# Find recent errors
curl -u elastic:password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_recent_errors()"}'
```

**More examples:**

```bash
# Hunt for an IOC (security)
curl -u elastic:password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL hunt_ioc WITH ioc = '\''192.168.1.100'\''"}'

# Get slow transactions (observability)
curl -u elastic:password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL get_slow_transactions()"}'

# Semantic search (search)
curl -u elastic:password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "RUN SKILL semantic_search WITH query = '\''pricing information'\''"}'
```

---

## That's It!

You now have:
- ✅ elastic-script plugin running in your cluster
- ✅ 155+ skills installed
- ✅ Ready to run skills via REST API

---

## What's Next?

| I want to... | Do this |
|--------------|---------|
| Browse all skills | Visit [MoltlerHub](https://hub.moltler.dev) |
| Use the CLI | `~/.moltler/moltler/hub/moltler-cli.sh list` |
| Connect AI agents | [MCP Integration](../mcp.md) |
| Build my own skills | [Creating Skills](../skills/creating-skills.md) |

---

## The Journey

```
┌─────────────────────────────────────────────────────────────────┐
│                        YOUR JOURNEY                              │
│                                                                  │
│  1. INSTALL PLUGIN                                               │
│     └── elasticsearch-plugin install x-pack-escript.zip         │
│                     ↓                                            │
│  2. INSTALL SKILLS                                               │
│     └── curl https://hub.moltler.dev/install.sh | bash          │
│                     ↓                                            │
│  3. RUN SKILLS                                                   │
│     └── RUN SKILL get_recent_errors()                           │
│                     ↓                                            │
│  4. (Optional) BUILD YOUR OWN                                   │
│     └── CREATE SKILL my_skill ...                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Alternative: Local Development Setup

If you want to develop skills or contribute, use the full development setup:

```bash
git clone --recurse-submodules https://github.com/bahaaldine/moltler.git
cd moltler
./scripts/quick-start.sh
```

This builds everything from source and runs a local ES cluster for development.

[Full Installation Guide →](installation.md)

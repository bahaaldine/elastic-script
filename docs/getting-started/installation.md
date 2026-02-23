# Installation

Get Moltler running in your environment.

---

## Architecture Overview

Moltler consists of three components:

| Component | Purpose | Required |
|-----------|---------|----------|
| **elastic-script plugin** | Runs skills inside Elasticsearch | ✅ Yes |
| **Moltler CLI** | Install/manage skills from terminal | ✅ Yes |
| **MoltlerHub** | Web portal to browse/discover skills | Optional |
| **Moltler MCP** | Bridge for AI agents to use skills | Optional |

```
┌─────────────────────────────────────────────────────────────────┐
│                        USER / AI AGENT                          │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  MoltlerHub   │    │  Moltler CLI  │    │  Moltler MCP  │
│  (Web Portal) │    │  (Terminal)   │    │  (AI Bridge)  │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│               Elasticsearch + elastic-script plugin             │
│                        (Skills Runtime)                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Quick Start (All-in-One)

The fastest way to get everything running:

```bash
# Clone the repository
git clone --recurse-submodules https://github.com/bahaaldine/moltler.git
cd moltler

# Start Elasticsearch with the plugin + install all skills
./scripts/quick-start.sh

# (Optional) Start MoltlerHub web portal
cd moltler-hub && npm install && npm run dev
```

This gives you:
- ✅ Elasticsearch on `http://localhost:9200`
- ✅ 155+ skills installed
- ✅ MoltlerHub on `http://localhost:3000` (optional)

---

## Step-by-Step Installation

### 1. Elasticsearch + elastic-script Plugin

**Option A: Use the provided setup (recommended)**

```bash
git clone --recurse-submodules https://github.com/bahaaldine/moltler.git
cd moltler
./scripts/quick-start.sh
```

**Option B: Install plugin on existing Elasticsearch**

```bash
# Build the plugin
cd elastic-script/elasticsearch
./gradlew :x-pack:plugin:elastic-script:build

# Install on your ES cluster
elasticsearch-plugin install file:///path/to/elastic-script-*.zip
```

**Verify installation:**

```bash
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "PRINT '\''Hello Moltler!'\''"}'
```

---

### 2. Moltler CLI

The CLI is a bash script included in the repository:

```bash
cd moltler/hub

# List available skills
./moltler-cli.sh list

# Install all skills
./moltler-cli.sh install --all

# Install specific skill
./moltler-cli.sh install get-recent-errors

# Run a skill
./moltler-cli.sh run get-recent-errors
```

**Make it globally available (optional):**

```bash
# Add to your PATH
echo 'export PATH="$PATH:/path/to/moltler/hub"' >> ~/.zshrc
source ~/.zshrc

# Now use from anywhere
moltler-cli.sh list
```

---

### 3. MoltlerHub (Web Portal)

Browse and discover skills through a web interface:

```bash
cd moltler/moltler-hub

# Install dependencies
npm install

# Start development server
npm run dev
```

Open `http://localhost:3000` to browse skills.

**For production deployment:**

```bash
# Deploy to Vercel
npx vercel

# Or build static site
npm run build
```

---

### 4. Moltler MCP (AI Agent Bridge)

Connect AI assistants (Claude, Cursor, etc.) to your skills:

**The MCP endpoint is built into the elastic-script plugin:**

```bash
# List available skills via MCP
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'

# Call a skill via MCP
curl -u elastic-admin:elastic-password http://localhost:9200/_escript/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {"name": "get_recent_errors", "arguments": {}},
    "id": 1
  }'
```

**Connect to Cursor IDE:**

Add to `.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "moltler": {
      "url": "http://localhost:9200/_escript/mcp",
      "headers": {
        "Authorization": "Basic ZWxhc3RpYy1hZG1pbjplbGFzdGljLXBhc3N3b3Jk"
      }
    }
  }
}
```

---

## What's Next?

| I want to... | Do this |
|--------------|---------|
| Run my first skill | [Quick Start Guide](quick-start.md) |
| Browse available skills | [MoltlerHub](https://hub.moltler.dev) or `./moltler-cli.sh list` |
| Build my own skill | [Creating Skills](../skills/creating-skills.md) |
| Connect an AI assistant | [MCP Integration](../mcp.md) |

---

## Troubleshooting

### Plugin not loading

```bash
# Check plugin is installed
curl -u elastic-admin:elastic-password http://localhost:9200/_cat/plugins

# Should show: elastic-script
```

### Skills not found

```bash
# Install skills first
cd hub && ./moltler-cli.sh install --all

# Verify
./moltler-cli.sh installed
```

### MCP connection issues

```bash
# Test MCP endpoint directly
curl http://localhost:9200/_escript/mcp \
  -d '{"jsonrpc": "2.0", "method": "initialize", "id": 1}'
```

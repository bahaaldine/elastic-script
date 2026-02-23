# Installation

Choose your installation path based on your use case.

---

## Choose Your Path

| I want to... | Path |
|--------------|------|
| Try Moltler quickly (demo/development) | [Path A: Quick Start](#path-a-quick-start-demoevaluation) |
| Install on my existing Elasticsearch cluster | [Path B: Existing Cluster](#path-b-existing-elasticsearch-cluster) |

---

## Path A: Quick Start (Demo/Evaluation)

**Best for:** Trying Moltler, development, demos, learning

This path runs a local Elasticsearch instance with the plugin pre-installed.

### Prerequisites

- Git
- Java 17+
- Node.js 18+ (for MoltlerHub)

### Steps

```bash
# 1. Clone the repository
git clone --recurse-submodules https://github.com/bahaaldine/moltler.git
cd moltler

# 2. Start Elasticsearch with the plugin (builds automatically)
./scripts/quick-start.sh

# 3. Install skills
cd hub && ./moltler-cli.sh install --all

# 4. Run your first skill
./moltler-cli.sh run get-recent-errors

# 5. (Optional) Start MoltlerHub web portal
cd .. && ./scripts/quick-start.sh --hub
```

**Result:** Elasticsearch on `localhost:9200` with 155+ skills installed.

---

## Path B: Existing Elasticsearch Cluster

**Best for:** Production deployments, adding Moltler to existing infrastructure

### Prerequisites

- Elasticsearch 8.x cluster (running)
- Admin access to install plugins
- Java 17+ (for building the plugin)

### Step 1: Download the Plugin

Download the pre-built plugin that matches your Elasticsearch version:

**[📦 Download from GitHub Releases](https://github.com/bahaaldine/moltler/releases)**

| Elasticsearch | Download |
|---------------|----------|
| 9.4.0 | `elastic-script-9.4.0.zip` |
| 9.3.0 | `elastic-script-9.3.0.zip` |
| 9.2.0 | `elastic-script-9.2.0.zip` |
| 9.1.0 | `elastic-script-9.1.0.zip` |
| 9.0.0 | `elastic-script-9.0.0.zip` |
| 8.17.0 | `elastic-script-8.17.0.zip` |
| 8.16.0 | `elastic-script-8.16.0.zip` |
| 8.15.0 | `elastic-script-8.15.0.zip` |

```bash
# Example: Download for ES 9.4.0
wget https://github.com/bahaaldine/moltler/releases/download/v1.0.0/elastic-script-9.4.0.zip
```

??? note "Build from Source (alternative)"
    If you need to build from source (custom version, modifications):
    
    ```bash
    git clone --recurse-submodules https://github.com/bahaaldine/moltler.git
    cd moltler/elastic-script/elasticsearch
    ./gradlew :x-pack:plugin:elastic-script:build -x test
    ls -la x-pack/plugin/elastic-script/build/distributions/
    ```

### Step 2: Install on Each Node

```bash
# Copy the plugin zip to each Elasticsearch node
scp elastic-script-9.4.0.zip user@es-node:/tmp/

# SSH to each node and install
ssh user@es-node

# Install the plugin
sudo /usr/share/elasticsearch/bin/elasticsearch-plugin install \
    file:///tmp/elastic-script-9.4.0.zip

# Restart Elasticsearch
sudo systemctl restart elasticsearch
```

!!! warning "Repeat for each node"
    The plugin must be installed on **every node** in your cluster.
    After installing on all nodes, restart them one at a time (rolling restart).

### Step 3: Verify Installation

```bash
# Check plugin is loaded
curl -u elastic:password https://your-cluster:9200/_cat/plugins

# Test the plugin
curl -u elastic:password https://your-cluster:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "PRINT '\''Hello Moltler!'\''"}'
```

### Step 4: Configure the CLI

```bash
# Back on your local machine
cd moltler/hub

# Set environment variables for your cluster
export ES_URL="https://your-cluster:9200"
export ES_USER="elastic"
export ES_PASSWORD="your-password"

# Install skills
./moltler-cli.sh install --all

# Verify
./moltler-cli.sh installed
```

### Step 5: (Optional) Deploy MoltlerHub

For team access to browse skills, deploy MoltlerHub:

```bash
cd moltler/moltler-hub

# Configure for your cluster
echo "NEXT_PUBLIC_ES_URL=https://your-cluster:9200" > .env.local

# Build and deploy
npm install
npm run build

# Deploy to your preferred platform (Vercel, Docker, etc.)
```

---

## Architecture Overview

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

| Component | Where it runs | Required |
|-----------|---------------|----------|
| **elastic-script plugin** | On each ES node | ✅ Yes |
| **Moltler CLI** | Your laptop/CI server | ✅ Yes |
| **MoltlerHub** | Web server or localhost | Optional |
| **Moltler MCP** | Built into the plugin | Optional |

---

## Cloud Deployments

### Elastic Cloud

!!! note "Coming Soon"
    Native Elastic Cloud support is on the roadmap. For now, use self-managed clusters.

### Kubernetes / ECK

For Elastic Cloud on Kubernetes (ECK), add the plugin to your custom image:

```dockerfile
FROM docker.elastic.co/elasticsearch/elasticsearch:8.x.x

# Copy pre-built plugin
COPY elastic-script-*.zip /tmp/

# Install plugin
RUN bin/elasticsearch-plugin install file:///tmp/elastic-script-*.zip
```

---

## Troubleshooting

### Plugin not loading

```bash
# Check plugin is installed
curl -u elastic:password https://your-cluster:9200/_cat/plugins

# Check ES logs for errors
tail -100 /var/log/elasticsearch/elasticsearch.log | grep -i escript
```

### Version mismatch

The plugin must match your Elasticsearch version. Build against the matching branch:

```bash
cd moltler/elastic-script/elasticsearch
git checkout 8.x  # Match your ES version
./gradlew :x-pack:plugin:elastic-script:build -x test
```

### Skills not found

```bash
# Verify CLI is configured for your cluster
echo $ES_URL  # Should show your cluster URL

# Install skills
./moltler-cli.sh install --all

# List installed
./moltler-cli.sh installed
```

### Connection issues

```bash
# Test basic connectivity
curl -u elastic:password https://your-cluster:9200

# Check TLS settings if using HTTPS
curl -k -u elastic:password https://your-cluster:9200  # Skip cert validation
```

---

## Next Steps

| I want to... | Go here |
|--------------|---------|
| Run my first skill | [Quick Start Guide](quick-start.md) |
| Browse available skills | [MoltlerHub](../moltlerhub/index.md) or `./moltler-cli.sh list` |
| Build my own skill | [Creating Skills](../skills/creating-skills.md) |
| Connect an AI assistant | [MCP Integration](../mcp.md) |

# Installation

## Prerequisites

Before installing elastic-script, ensure you have:

- **Java 21+** - Required for Elasticsearch 9.x
- **Git** - For cloning the repository
- **8GB+ RAM** - Elasticsearch needs memory

Optional for development:
- **Python 3.9+** - For Jupyter notebooks
- **OpenAI API Key** - For AI/LLM features

## Quick Start (Recommended)

The fastest way to get started:

```bash
# Clone the repository
git clone https://github.com/bahaaldine/elastic-script.git
cd elastic-script

# Run the quick-start script
./scripts/quick-start.sh
```

This will:

1. ✅ Check prerequisites
2. ✅ Build the elastic-script plugin
3. ✅ Start Elasticsearch with the plugin
4. ✅ Load sample data (360 documents across 6 indices)
5. ✅ Set up Jupyter notebooks
6. ✅ Print curl examples

!!! tip "OpenAI API Key"
    The script will prompt for your OpenAI API key (optional).
    This enables AI functions like `LLM_COMPLETE()` and `LLM_SUMMARIZE()`.

## Manual Installation

### 1. Clone the Repository

```bash
git clone https://github.com/bahaaldine/elastic-script.git
cd elastic-script

# Initialize the Elasticsearch submodule
git submodule init
git submodule update
```

### 2. Build the Plugin

```bash
cd elasticsearch
./gradlew :x-pack:plugin:elastic-script:assemble
```

The plugin ZIP will be at:
```
x-pack/plugin/elastic-script/build/distributions/elastic-script-*.zip
```

### 3. Start Elasticsearch

For development:

```bash
./gradlew :run
```

For production, install the plugin:

```bash
bin/elasticsearch-plugin install file:///path/to/elastic-script-9.4.0-SNAPSHOT.zip
bin/elasticsearch
```

## Verify Installation

Check that elastic-script is working:

```bash
# Health check
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CREATE PROCEDURE test() BEGIN RETURN 42; END PROCEDURE;"}'

# Expected: {"result":{"id":"test","index":".elastic_script_procedures","result":"created"}}

curl -u elastic-admin:elastic-password http://localhost:9200/_escript \
  -H "Content-Type: application/json" \
  -d '{"query": "CALL test()"}'

# Expected: {"result": 42}
```

## Default Credentials

When using `./gradlew :run` (development mode):

| Setting | Value |
|---------|-------|
| URL | `http://localhost:9200` |
| Username | `elastic-admin` |
| Password | `elastic-password` |

## Sample Data

The quick-start script loads sample data into these indices:

| Index | Documents | Description |
|-------|-----------|-------------|
| `logs-sample` | 100 | Application logs with ERROR, WARN, INFO levels |
| `metrics-sample` | 80 | System metrics (CPU, memory, latency) |
| `users-sample` | 30 | User profiles with roles |
| `orders-sample` | 50 | E-commerce orders |
| `products-sample` | 40 | Product catalog |
| `security-events` | 60 | Audit events |

## Stopping Services

```bash
# Stop everything
./scripts/quick-start.sh --stop

# Stop only Jupyter
./scripts/quick-start.sh --stop-notebooks

# Stop only Kibana (if running)
./scripts/quick-start.sh --stop-kibana
```

## Check Status

```bash
./scripts/quick-start.sh --status
```

## Next Steps

- [Quick Start Guide](quick-start.md) - Your first procedure
- [Jupyter Setup](jupyter-setup.md) - Interactive development
- [Language Overview](../language/overview.md) - Learn the syntax

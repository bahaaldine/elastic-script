#!/bin/bash
# Setup Moltler MCP Bridge for AI Agent Integration
# This script configures Claude Desktop, Cursor, or other MCP-compatible agents

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║${NC}  Moltler MCP Bridge Setup"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Install Python dependencies
echo -e "${GREEN}▶${NC} Installing Python dependencies..."
pip3 install -r "$SCRIPT_DIR/requirements.txt" -q
echo -e "${GREEN}✔${NC} Dependencies installed"

# Make server executable
chmod +x "$SCRIPT_DIR/moltler_mcp_server.py"

# Detect OS and configure
case "$OSTYPE" in
    darwin*)
        CLAUDE_CONFIG_DIR="$HOME/Library/Application Support/Claude"
        ;;
    linux*)
        CLAUDE_CONFIG_DIR="$HOME/.config/Claude"
        ;;
    *)
        CLAUDE_CONFIG_DIR="$HOME/.config/Claude"
        ;;
esac

echo ""
echo -e "${BLUE}Choose your AI agent to configure:${NC}"
echo "  1) Claude Desktop"
echo "  2) Cursor"
echo "  3) Show manual configuration"
echo "  4) Test MCP connection"
echo ""
read -p "Enter choice [1-4]: " choice

case $choice in
    1)
        # Claude Desktop configuration
        echo ""
        echo -e "${GREEN}▶${NC} Configuring Claude Desktop..."
        
        mkdir -p "$CLAUDE_CONFIG_DIR"
        CONFIG_FILE="$CLAUDE_CONFIG_DIR/claude_desktop_config.json"
        
        # Create or update config
        if [ -f "$CONFIG_FILE" ]; then
            echo -e "${YELLOW}⚠${NC} Existing config found. Backing up..."
            cp "$CONFIG_FILE" "$CONFIG_FILE.backup"
        fi
        
        cat > "$CONFIG_FILE" << EOF
{
  "mcpServers": {
    "moltler-skills": {
      "command": "python3",
      "args": [
        "$SCRIPT_DIR/moltler_mcp_server.py"
      ],
      "env": {
        "ES_URL": "http://localhost:9200",
        "ES_USER": "elastic-admin",
        "ES_PASSWORD": "elastic-password"
      }
    }
  }
}
EOF
        
        echo -e "${GREEN}✔${NC} Claude Desktop configured!"
        echo ""
        echo "  Config file: $CONFIG_FILE"
        echo ""
        echo -e "${YELLOW}Next steps:${NC}"
        echo "  1. Make sure Elasticsearch is running (./scripts/quick-start.sh)"
        echo "  2. Restart Claude Desktop"
        echo "  3. Look for 'moltler-skills' in the tools menu (hammer icon)"
        echo ""
        ;;
    
    2)
        # Cursor configuration
        echo ""
        echo -e "${GREEN}▶${NC} Configuring Cursor..."
        
        CURSOR_CONFIG_DIR="$HOME/.cursor"
        mkdir -p "$CURSOR_CONFIG_DIR"
        
        cat > "$CURSOR_CONFIG_DIR/mcp.json" << EOF
{
  "mcpServers": {
    "moltler-skills": {
      "command": "python3",
      "args": ["$SCRIPT_DIR/moltler_mcp_server.py"],
      "env": {
        "ES_URL": "http://localhost:9200",
        "ES_USER": "elastic-admin", 
        "ES_PASSWORD": "elastic-password"
      }
    }
  }
}
EOF
        
        echo -e "${GREEN}✔${NC} Cursor configured!"
        echo ""
        echo "  Config file: $CURSOR_CONFIG_DIR/mcp.json"
        echo ""
        echo -e "${YELLOW}Next steps:${NC}"
        echo "  1. Make sure Elasticsearch is running (./scripts/quick-start.sh)"
        echo "  2. Restart Cursor"
        echo "  3. The Moltler skills will be available as tools"
        echo ""
        ;;
    
    3)
        # Manual configuration
        echo ""
        echo -e "${BLUE}Manual Configuration${NC}"
        echo ""
        echo "Add this to your MCP client configuration:"
        echo ""
        cat << EOF
{
  "mcpServers": {
    "moltler-skills": {
      "command": "python3",
      "args": ["$SCRIPT_DIR/moltler_mcp_server.py"],
      "env": {
        "ES_URL": "http://localhost:9200",
        "ES_USER": "elastic-admin",
        "ES_PASSWORD": "elastic-password"
      }
    }
  }
}
EOF
        echo ""
        echo "Or run the server directly to test:"
        echo "  python3 $SCRIPT_DIR/moltler_mcp_server.py"
        echo ""
        ;;
    
    4)
        # Test connection
        echo ""
        echo -e "${GREEN}▶${NC} Testing MCP connection to Elasticsearch..."
        
        # Check if ES is running
        if ! curl -s -u elastic-admin:elastic-password http://localhost:9200 > /dev/null 2>&1; then
            echo -e "${YELLOW}⚠${NC} Elasticsearch is not running!"
            echo "  Start it with: ./scripts/quick-start.sh"
            exit 1
        fi
        echo -e "${GREEN}✔${NC} Elasticsearch is running"
        
        # Test MCP endpoint
        echo -e "${GREEN}▶${NC} Testing MCP tools/list..."
        RESULT=$(curl -s -u elastic-admin:elastic-password \
            -X POST http://localhost:9200/_escript/mcp \
            -H "Content-Type: application/json" \
            -d '{"jsonrpc":"2.0","method":"tools/list","id":1}')
        
        if echo "$RESULT" | grep -q '"tools"'; then
            TOOL_COUNT=$(echo "$RESULT" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('result',{}).get('tools',[])))" 2>/dev/null || echo "?")
            echo -e "${GREEN}✔${NC} MCP endpoint working! Found $TOOL_COUNT tools/skills"
            echo ""
            echo "Available skills:"
            echo "$RESULT" | python3 -c "
import sys, json
data = json.load(sys.stdin)
tools = data.get('result', {}).get('tools', [])
for t in tools[:10]:
    print(f\"  - {t['name']}: {t.get('description', 'No description')[:60]}\")
if len(tools) > 10:
    print(f'  ... and {len(tools)-10} more')
" 2>/dev/null || echo "$RESULT"
        else
            echo -e "${YELLOW}⚠${NC} MCP endpoint returned unexpected response:"
            echo "$RESULT"
        fi
        echo ""
        ;;
    
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "Once configured, AI agents can use your Moltler skills!"
echo ""
echo "Example prompts to try in Claude/Cursor:"
echo "  - 'Use moltler to check the cluster health'"
echo "  - 'Call the count_logs_by_level skill'"
echo "  - 'Get recent errors using moltler'"
echo ""

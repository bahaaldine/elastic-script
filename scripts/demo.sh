#!/bin/bash
# Moltler Demo - See the value in 30 seconds
set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic-admin}"
ES_PASSWORD="${ES_PASSWORD:-elastic-password}"

clear
echo -e "${BOLD}${CYAN}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   ███╗   ███╗ ██████╗ ██╗  ████████╗██╗     ███████╗██████╗   ║"
echo "║   ████╗ ████║██╔═══██╗██║  ╚══██╔══╝██║     ██╔════╝██╔══██╗  ║"
echo "║   ██╔████╔██║██║   ██║██║     ██║   ██║     █████╗  ██████╔╝  ║"
echo "║   ██║╚██╔╝██║██║   ██║██║     ██║   ██║     ██╔══╝  ██╔══██╗  ║"
echo "║   ██║ ╚═╝ ██║╚██████╔╝███████╗██║   ███████╗███████╗██║  ██║  ║"
echo "║   ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚═╝   ╚══════╝╚══════╝╚═╝  ╚═╝  ║"
echo "║                                                               ║"
echo "║         Pre-built skills for Elasticsearch                    ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo ""

run_skill() {
    local skill="$1"
    local display_name="$2"
    
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}${GREEN}▶ $display_name${NC}"
    echo -e "${YELLOW}  RUN SKILL $skill${NC}"
    echo ""
    
    result=$(curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL/_escript" \
        -H "Content-Type: application/json" \
        -d "{\"query\": \"RUN SKILL $skill\"}" 2>/dev/null)
    
    if command -v jq &> /dev/null; then
        echo "$result" | jq -C '.result // .' 2>/dev/null || echo "$result"
    else
        echo "$result"
    fi
    echo ""
}

# Check connection
echo -e "${BLUE}Checking Elasticsearch connection...${NC}"
if ! curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL" > /dev/null 2>&1; then
    echo -e "${RED}Cannot connect to Elasticsearch at $ES_URL${NC}"
    echo ""
    echo "Start Moltler first:"
    echo "  ./scripts/quick-start.sh"
    exit 1
fi
echo -e "${GREEN}Connected to $ES_URL${NC}"
echo ""

# Check if skills are installed
skill_count=$(curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL/.escript_skills/_count" 2>/dev/null | grep -o '"count":[0-9]*' | cut -d: -f2 || echo "0")
if [ "$skill_count" -lt 5 ]; then
    echo -e "${YELLOW}Installing skills first...${NC}"
    cd "$(dirname "$0")/../hub" && ./moltler-cli.sh install --all > /dev/null 2>&1 || true
    cd - > /dev/null
    echo -e "${GREEN}Skills installed.${NC}"
    echo ""
fi

echo -e "${BOLD}Let's solve some real problems:${NC}"
echo ""
sleep 1

# Demo 1: Observability
echo -e "${CYAN}${BOLD}═══ OBSERVABILITY ═══${NC}"
run_skill "get_recent_errors(limit => 3)" "Find recent errors"
sleep 2

# Demo 2: Search
echo -e "${CYAN}${BOLD}═══ SEARCH ═══${NC}"
run_skill "top_values(index_pattern => 'logs-*', field => 'level', limit => 5)" "What log levels exist?"
sleep 2

# Demo 3: Show installed skills
echo -e "${CYAN}${BOLD}═══ AVAILABLE SKILLS ═══${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${GREEN}▶ What skills are installed?${NC}"
echo -e "${YELLOW}  SHOW SKILLS${NC}"
echo ""

result=$(curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL/_escript" \
    -H "Content-Type: application/json" \
    -d '{"query": "SHOW SKILLS"}' 2>/dev/null)

if command -v jq &> /dev/null; then
    count=$(echo "$result" | jq -r '.result.count // 0')
    echo -e "${GREEN}$count skills available${NC}"
    echo ""
    echo "Sample skills:"
    echo "$result" | jq -r '.result.skills[:10][] | "  • \(.name): \(.description // "No description")"' 2>/dev/null || echo "$result"
else
    echo "$result"
fi
echo ""

# Final summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BOLD}${GREEN}That's Moltler.${NC}"
echo ""
echo "Pre-built skills that solve real problems on your Elasticsearch data."
echo ""
echo -e "${BOLD}Next steps:${NC}"
echo ""
echo -e "  ${CYAN}Browse skills:${NC}    ./hub/moltler-cli.sh list"
echo -e "  ${CYAN}Run a skill:${NC}      curl localhost:9200/_escript -d '{\"query\": \"RUN SKILL skill_name()\"}'"
echo -e "  ${CYAN}Connect AI:${NC}       Use MCP endpoint at /_escript/mcp"
echo -e "  ${CYAN}Build your own:${NC}   See docs/skills/creating-skills.md"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

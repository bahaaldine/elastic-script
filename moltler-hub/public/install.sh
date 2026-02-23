#!/bin/bash
# Moltler Skills Installer
# Usage: curl -sSL https://hub.moltler.dev/install.sh | bash
#
# This script installs the Moltler CLI and all skills into your Elasticsearch cluster.

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "  __  __       _ _   _           "
echo " |  \/  | ___ | | |_| | ___ _ __ "
echo " | |\/| |/ _ \| | __| |/ _ \ '__|"
echo " | |  | | (_) | | |_| |  __/ |   "
echo " |_|  |_|\___/|_|\__|_|\___|_|   "
echo -e "${NC}"
echo "Skills Framework for Elasticsearch"
echo ""

# Configuration
ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic}"
ES_PASSWORD="${ES_PASSWORD:-}"
MOLTLER_VERSION="${MOLTLER_VERSION:-main}"
INSTALL_DIR="${INSTALL_DIR:-$HOME/.moltler}"

# Parse arguments
INSTALL_ALL=true
CATEGORY=""
while [[ $# -gt 0 ]]; do
  case $1 in
    --es-url) ES_URL="$2"; shift 2 ;;
    --es-user) ES_USER="$2"; shift 2 ;;
    --es-password) ES_PASSWORD="$2"; shift 2 ;;
    --category) CATEGORY="$2"; INSTALL_ALL=false; shift 2 ;;
    --help)
      echo "Usage: curl -sSL https://hub.moltler.dev/install.sh | bash -s -- [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --es-url URL        Elasticsearch URL (default: http://localhost:9200)"
      echo "  --es-user USER      Elasticsearch user (default: elastic)"
      echo "  --es-password PASS  Elasticsearch password"
      echo "  --category CAT      Install only skills from category (e.g., observability)"
      echo ""
      echo "Environment variables:"
      echo "  ES_URL, ES_USER, ES_PASSWORD can also be set as env vars"
      exit 0
      ;;
    *) shift ;;
  esac
done

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is required but not installed.${NC}"
    exit 1
fi

if ! command -v git &> /dev/null; then
    echo -e "${RED}Error: git is required but not installed.${NC}"
    exit 1
fi

# Test Elasticsearch connection
echo -e "${YELLOW}Testing Elasticsearch connection...${NC}"

AUTH_HEADER=""
if [ -n "$ES_PASSWORD" ]; then
    AUTH_HEADER="-u ${ES_USER}:${ES_PASSWORD}"
fi

if ! curl -s $AUTH_HEADER "$ES_URL" > /dev/null 2>&1; then
    echo -e "${RED}Error: Cannot connect to Elasticsearch at $ES_URL${NC}"
    echo ""
    echo "Make sure:"
    echo "  1. Elasticsearch is running"
    echo "  2. The elastic-script plugin is installed"
    echo "  3. Credentials are correct (use --es-user and --es-password)"
    exit 1
fi

echo -e "${GREEN}✓ Connected to Elasticsearch${NC}"

# Check if plugin is installed
PLUGIN_CHECK=$(curl -s $AUTH_HEADER "$ES_URL/_cat/plugins" 2>/dev/null | grep -c "escript" || echo "0")
if [ "$PLUGIN_CHECK" = "0" ]; then
    echo -e "${RED}Error: elastic-script plugin is not installed.${NC}"
    echo ""
    echo "Install the plugin first:"
    echo "  elasticsearch-plugin install https://github.com/bahaaldine/moltler/releases/download/v1.0.0/x-pack-escript-9.4.0-SNAPSHOT.zip"
    echo "  systemctl restart elasticsearch"
    exit 1
fi

echo -e "${GREEN}✓ elastic-script plugin detected${NC}"

# Download Moltler CLI
echo ""
echo -e "${YELLOW}Downloading Moltler...${NC}"

mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"

if [ -d "moltler" ]; then
    echo "Updating existing installation..."
    cd moltler
    git pull --quiet
else
    git clone --quiet --depth 1 https://github.com/bahaaldine/moltler.git
    cd moltler
fi

echo -e "${GREEN}✓ Moltler downloaded to $INSTALL_DIR/moltler${NC}"

# Install skills
echo ""
echo -e "${YELLOW}Installing skills...${NC}"

cd hub

# Export credentials for the CLI
export ES_URL
export ES_USER
export ES_PASSWORD

if [ "$INSTALL_ALL" = true ]; then
    ./moltler-cli.sh install --all
else
    ./moltler-cli.sh install --all --category "$CATEGORY"
fi

# Count installed skills
SKILL_COUNT=$(./moltler-cli.sh installed 2>/dev/null | grep -c "✓" || echo "0")

echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Moltler installed successfully!${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${BLUE}Skills installed:${NC} $SKILL_COUNT"
echo -e "  ${BLUE}Elasticsearch:${NC}   $ES_URL"
echo ""
echo "Quick start:"
echo ""
echo "  # Run a skill"
echo "  curl $AUTH_HEADER $ES_URL/_escript \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"query\": \"RUN SKILL get_recent_errors()\"}'"
echo ""
echo "  # List installed skills"
echo "  $INSTALL_DIR/moltler/hub/moltler-cli.sh installed"
echo ""
echo "  # Browse skills at https://hub.moltler.dev"
echo ""
echo -e "${YELLOW}Tip:${NC} Add the CLI to your PATH:"
echo "  echo 'export PATH=\"\$PATH:$INSTALL_DIR/moltler/hub\"' >> ~/.bashrc"
echo ""

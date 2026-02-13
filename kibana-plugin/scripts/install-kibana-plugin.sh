#!/bin/bash
#
# Moltler Kibana Plugin Installer
#
# This script sets up the Kibana development environment and installs the Moltler plugin.
# It follows the "frictionless onboarding" principle - one command, works immediately.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGIN_DIR="$SCRIPT_DIR/../plugins/moltler"
KIBANA_DIR="$SCRIPT_DIR/../kibana"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  🦌 Moltler Kibana Plugin Installer${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

print_step() {
    echo -e "${GREEN}▶${NC} $1"
}

print_info() {
    echo -e "  ${YELLOW}ℹ${NC} $1"
}

print_success() {
    echo -e "  ${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "  ${RED}✗${NC} $1"
}

check_prerequisites() {
    print_step "Checking prerequisites..."
    
    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js is required but not installed."
        print_info "Install via: nvm install 22"
        exit 1
    fi
    
    # Kibana requires specific Node.js version (22.x for Kibana 9.x)
    NODE_VERSION=$(node -v | sed 's/v//' | cut -d. -f1)
    NODE_FULL=$(node -v)
    
    if [ "$NODE_VERSION" != "22" ]; then
        print_error "Kibana 9.x requires Node.js 22.x. Current: $NODE_FULL"
        echo ""
        print_info "To install the correct version:"
        print_info "  1. Install nvm: https://github.com/nvm-sh/nvm"
        print_info "  2. Run: nvm install 22.22.0"
        print_info "  3. Run: nvm use 22.22.0"
        print_info "  4. Re-run this script"
        echo ""
        
        # Check if nvm is available
        if command -v nvm &> /dev/null || [ -f "$HOME/.nvm/nvm.sh" ]; then
            print_info "nvm detected! Try: nvm install 22.22.0 && nvm use 22.22.0"
        fi
        exit 1
    fi
    print_success "Node.js $NODE_FULL"
    
    # Check yarn
    if ! command -v yarn &> /dev/null; then
        print_info "Installing yarn..."
        npm install -g yarn
    fi
    print_success "yarn $(yarn -v)"
    
    # Check git
    if ! command -v git &> /dev/null; then
        print_error "git is required but not installed."
        exit 1
    fi
    print_success "git $(git --version | awk '{print $3}')"
}

clone_kibana() {
    print_step "Setting up Kibana source..."
    
    if [ -d "$KIBANA_DIR" ]; then
        print_info "Kibana directory exists, checking version..."
        cd "$KIBANA_DIR"
        CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "detached")
        print_success "Kibana already cloned (branch: $CURRENT_BRANCH)"
        
        # Pull latest if on main
        if [ "$CURRENT_BRANCH" = "main" ]; then
            print_info "Updating Kibana (this may take a while)..."
            git pull --ff-only 2>/dev/null || true
        fi
    else
        print_info "Cloning Kibana repository (this will take several minutes)..."
        print_info "Repository size is ~2GB, please be patient..."
        
        # Shallow clone for faster setup
        git clone --depth 1 https://github.com/elastic/kibana.git "$KIBANA_DIR"
        print_success "Kibana cloned successfully"
    fi
}

link_plugin() {
    print_step "Linking Moltler plugin..."
    
    PLUGINS_DIR="$KIBANA_DIR/plugins"
    mkdir -p "$PLUGINS_DIR"
    
    # Create symlink to our plugin
    LINK_PATH="$PLUGINS_DIR/moltler"
    if [ -L "$LINK_PATH" ]; then
        rm "$LINK_PATH"
    fi
    if [ -d "$LINK_PATH" ]; then
        rm -rf "$LINK_PATH"
    fi
    
    ln -s "$PLUGIN_DIR" "$LINK_PATH"
    print_success "Plugin linked to $LINK_PATH"
}

bootstrap_kibana() {
    print_step "Bootstrapping Kibana (this takes 5-10 minutes on first run)..."
    
    cd "$KIBANA_DIR"
    
    # Check if already bootstrapped
    if [ -d "node_modules" ] && [ -d "node_modules/@kbn" ]; then
        print_info "Kibana already bootstrapped, skipping..."
        print_success "Dependencies ready"
        return
    fi
    
    print_info "Running yarn kbn bootstrap..."
    yarn kbn bootstrap
    print_success "Bootstrap complete"
}

print_usage() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}  Installation Complete!${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "  To start Kibana with the Moltler plugin:"
    echo ""
    echo "    1. Make sure Elasticsearch is running:"
    echo "       ${YELLOW}cd ../.. && ./scripts/quick-start.sh${NC}"
    echo ""
    echo "    2. Start Kibana in development mode:"
    echo "       ${YELLOW}cd kibana-plugin/kibana && yarn start${NC}"
    echo ""
    echo "    3. Open Kibana in your browser:"
    echo "       ${BLUE}http://localhost:5601${NC}"
    echo ""
    echo "    4. Find Moltler in the sidebar menu under Management"
    echo ""
    echo "  Alternative: Run in watch mode for development:"
    echo "    ${YELLOW}cd kibana-plugin/plugins/moltler && yarn dev --watch${NC}"
    echo "    (Then start Kibana in another terminal)"
    echo ""
}

# Main execution
print_header

case "${1:-install}" in
    install)
        check_prerequisites
        clone_kibana
        link_plugin
        bootstrap_kibana
        print_usage
        ;;
    link)
        link_plugin
        print_success "Plugin linked. Run 'yarn start' in kibana directory."
        ;;
    start)
        if [ ! -d "$KIBANA_DIR" ]; then
            print_error "Kibana not installed. Run './install-kibana-plugin.sh install' first."
            exit 1
        fi
        cd "$KIBANA_DIR"
        print_step "Starting Kibana..."
        yarn start
        ;;
    *)
        echo "Usage: $0 {install|link|start}"
        echo ""
        echo "  install  - Full installation (clone Kibana, link plugin, bootstrap)"
        echo "  link     - Only link the plugin (if Kibana already exists)"
        echo "  start    - Start Kibana with the plugin"
        exit 1
        ;;
esac

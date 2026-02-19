#!/bin/bash
# Moltler CLI - Manage skills from MoltlerHub
set -euo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic-admin}"
ES_PASSWORD="${ES_PASSWORD:-elastic-password}"
HUB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

usage() {
    cat << EOF
Moltler CLI - Manage Elasticsearch skills

USAGE:
    moltler <command> [options]

COMMANDS:
    install         Install a skill or all skills from the hub
    list            List available skills in the hub
    search          Search for skills by keyword
    test            Test installed skills
    status          Check Elasticsearch connection

OPTIONS:
    --all           Install all skills
    --category      Filter by category (meta, observability, search, security)
    
EXAMPLES:
    moltler install --all                    # Install all skills
    moltler install count-logs-by-level      # Install specific skill
    moltler list --category observability    # List observability skills
    moltler search "error"                   # Search for skills
    moltler test                             # Test all installed skills

ENVIRONMENT:
    ES_URL          Elasticsearch URL (default: http://localhost:9200)
    ES_USER         Username (default: elastic-admin)
    ES_PASSWORD     Password (default: elastic-password)
EOF
}

check_es() {
    if ! curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL" > /dev/null 2>&1; then
        log_error "Cannot connect to Elasticsearch at $ES_URL"
        exit 1
    fi
}

run_escript() {
    local query="$1"
    curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL/_escript" \
        -H "Content-Type: application/json" \
        -d "{\"query\": $(echo "$query" | jq -Rs .)}"
}

install_skill() {
    local skill_sql="$1"
    local skill_name="$2"
    
    log_info "Installing skill: $skill_name"
    
    local sql_content
    sql_content=$(cat "$skill_sql")
    
    local response
    response=$(run_escript "$sql_content")
    
    if echo "$response" | grep -q '"success"' || echo "$response" | grep -q '"result"'; then
        log_success "Installed: $skill_name"
        return 0
    else
        log_warn "May have issue with: $skill_name"
        echo "$response" | head -c 200
        echo
        return 0  # Don't fail, continue with other skills
    fi
}

install_all_skills() {
    local category="${1:-}"
    local count=0
    local failed=0
    
    log_info "Installing skills from MoltlerHub..."
    check_es
    
    local search_path="$HUB_DIR/skills/elastic"
    if [ -n "$category" ]; then
        search_path="$HUB_DIR/skills/elastic/$category"
    fi
    
    for skill_dir in $(find "$search_path" -name "skill.sql" -type f | sort); do
        local dir_name
        dir_name=$(dirname "$skill_dir")
        local skill_name
        skill_name=$(basename "$dir_name")
        
        if install_skill "$skill_dir" "$skill_name"; then
            ((count++))
        else
            ((failed++))
        fi
    done
    
    echo
    log_success "Installed $count skills"
    if [ $failed -gt 0 ]; then
        log_warn "$failed skills had issues"
    fi
}

list_skills() {
    local category="${1:-}"
    
    log_info "Available skills in MoltlerHub:"
    echo
    
    local search_path="$HUB_DIR/skills/elastic"
    
    if [ -n "$category" ]; then
        echo -e "${BLUE}Category: $category${NC}"
        search_path="$HUB_DIR/skills/elastic/$category"
    fi
    
    for skill_yaml in $(find "$search_path" -name "skill.yaml" -type f | sort); do
        local name desc
        name=$(grep "^name:" "$skill_yaml" | cut -d: -f2 | tr -d ' ')
        desc=$(grep "^description:" "$skill_yaml" | cut -d: -f2-)
        echo -e "  ${GREEN}$name${NC}:$desc"
    done
}

search_skills() {
    local query="$1"
    
    log_info "Searching for: $query"
    echo
    
    for skill_yaml in $(find "$HUB_DIR/skills/elastic" -name "skill.yaml" -type f); do
        if grep -qi "$query" "$skill_yaml"; then
            local name desc
            name=$(grep "^name:" "$skill_yaml" | cut -d: -f2 | tr -d ' ')
            desc=$(grep "^description:" "$skill_yaml" | cut -d: -f2-)
            local category
            category=$(echo "$skill_yaml" | rev | cut -d/ -f3 | rev)
            echo -e "  ${GREEN}$name${NC} [$category]:$desc"
        fi
    done
}

test_skills() {
    log_info "Testing installed skills..."
    check_es
    
    echo
    
    # Test list_all_skills
    log_info "Testing: list_all_skills"
    local response
    response=$(run_escript "RUN SKILL list_all_skills()")
    if echo "$response" | grep -q '"result"'; then
        log_success "list_all_skills works"
    else
        log_warn "list_all_skills may have issues"
    fi
    
    # Test search_skills
    log_info "Testing: search_skills"
    response=$(run_escript "RUN SKILL search_skills(query => 'logs')")
    if echo "$response" | grep -q '"result"'; then
        log_success "search_skills works"
    else
        log_warn "search_skills may have issues"
    fi
    
    # Test count_logs_by_level
    log_info "Testing: count_logs_by_level"
    response=$(run_escript "RUN SKILL count_logs_by_level()")
    if echo "$response" | grep -q '"result"' || echo "$response" | grep -q 'count'; then
        log_success "count_logs_by_level works"
    else
        log_warn "count_logs_by_level may have issues (check if logs-sample exists)"
    fi
    
    echo
    log_success "Skill tests completed"
}

status_cmd() {
    log_info "Checking Elasticsearch connection..."
    
    if curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL" > /dev/null 2>&1; then
        log_success "Connected to $ES_URL"
        
        # Count installed skills
        local skill_count
        skill_count=$(curl -s -u "$ES_USER:$ES_PASSWORD" "$ES_URL/.escript_skills/_count" 2>/dev/null | grep -o '"count":[0-9]*' | cut -d: -f2 || echo "0")
        log_info "Installed skills: $skill_count"
    else
        log_error "Cannot connect to $ES_URL"
    fi
}

# Main command router
case "${1:-help}" in
    install)
        if [ "${2:-}" == "--all" ]; then
            install_all_skills "${3:-}"
        elif [ -n "${2:-}" ] && [ "${2:-}" != "--category" ]; then
            # Install specific skill
            skill_path=$(find "$HUB_DIR/skills/elastic" -name "skill.sql" -path "*/$2/*" | head -1)
            if [ -n "$skill_path" ]; then
                check_es
                install_skill "$skill_path" "$2"
            else
                log_error "Skill not found: $2"
                exit 1
            fi
        else
            log_error "Specify --all or a skill name"
            exit 1
        fi
        ;;
    list)
        category=""
        if [ "${2:-}" == "--category" ] && [ -n "${3:-}" ]; then
            category="$3"
        fi
        list_skills "$category"
        ;;
    search)
        if [ -z "${2:-}" ]; then
            log_error "Please provide a search query"
            exit 1
        fi
        search_skills "$2"
        ;;
    test)
        test_skills
        ;;
    status)
        status_cmd
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        log_error "Unknown command: $1"
        usage
        exit 1
        ;;
esac

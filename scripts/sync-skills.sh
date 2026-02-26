#!/bin/bash
# Sync skills from hub/skills/ to moltler-hub/src/data/
# Also generates Agent Skills index

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Moltler Skills Sync ==="
echo ""

# 1. Generate skills.json from hub/skills/
echo "1. Generating skills.json from hub/skills/..."

SKILLS_JSON="$PROJECT_ROOT/moltler-hub/src/data/skills.json"
SKILLS_TS="$PROJECT_ROOT/moltler-hub/src/data/skills.ts"

# Create a Python script to parse skills
python3 << 'PYTHON_SCRIPT'
import os
import json
import yaml
from pathlib import Path

project_root = os.environ.get('PROJECT_ROOT', '.')
hub_skills = Path(project_root) / 'hub' / 'skills'
output_json = Path(project_root) / 'moltler-hub' / 'src' / 'data' / 'skills.json'

skills = []

def parse_yaml_simple(content):
    """Simple YAML-like parser for skill.yaml files"""
    result = {}
    current_key = None
    current_list = None
    
    for line in content.split('\n'):
        line = line.rstrip()
        if not line or line.startswith('#'):
            continue
            
        # Check for list item
        if line.startswith('  - '):
            if current_list is not None:
                item = line[4:].strip()
                if item.startswith("'") or item.startswith('"'):
                    item = item[1:-1]
                current_list.append(item)
            continue
            
        # Check for key: value
        if ':' in line and not line.startswith(' '):
            parts = line.split(':', 1)
            key = parts[0].strip()
            value = parts[1].strip() if len(parts) > 1 else ''
            
            if value.startswith('[') and value.endswith(']'):
                # Inline array
                items = value[1:-1].split(',')
                result[key] = [i.strip().strip("'\"") for i in items if i.strip()]
                current_list = None
            elif value == '' or value.startswith('['):
                # Start of list
                result[key] = []
                current_list = result[key]
            else:
                # Simple value
                if value.startswith("'") or value.startswith('"'):
                    value = value[1:-1]
                result[key] = value
                current_list = None
            current_key = key
    
    return result

def process_skill_dir(skill_path, category, vendor='elastic'):
    """Process a skill directory and return skill metadata"""
    yaml_file = skill_path / 'skill.yaml'
    sql_file = skill_path / 'skill.sql'
    
    if not yaml_file.exists():
        return None
        
    try:
        with open(yaml_file, 'r') as f:
            content = f.read()
        
        data = parse_yaml_simple(content)
        
        skill = {
            'name': data.get('name', skill_path.name),
            'displayName': data.get('displayName', skill_path.name.replace('-', ' ').title()),
            'description': data.get('description', ''),
            'version': data.get('version', '1.0.0'),
            'author': data.get('author', vendor),
            'category': category,
            'tags': data.get('tags', [category]),
            'parameters': [],
            'returns': data.get('returns', {}).get('type', 'DOCUMENT') if isinstance(data.get('returns'), dict) else data.get('returns', 'DOCUMENT'),
            'sourceUrl': f'https://github.com/bahaaldine/moltler/tree/main/hub/skills/{vendor}/{category}/{skill_path.name}'
        }
        
        return skill
    except Exception as e:
        print(f"  Warning: Failed to parse {yaml_file}: {e}")
        return None

# Process all skills
for vendor_dir in hub_skills.iterdir():
    if not vendor_dir.is_dir():
        continue
    vendor = vendor_dir.name
    
    for category_dir in vendor_dir.iterdir():
        if not category_dir.is_dir():
            continue
        category = category_dir.name
        
        for skill_dir in category_dir.iterdir():
            if not skill_dir.is_dir():
                continue
            
            skill = process_skill_dir(skill_dir, category, vendor)
            if skill:
                skills.append(skill)

# Sort by category, then name
skills.sort(key=lambda s: (s['category'], s['name']))

# Write JSON
output_json.parent.mkdir(parents=True, exist_ok=True)
with open(output_json, 'w') as f:
    json.dump(skills, f, indent=2)

print(f"  Generated {len(skills)} skills")
PYTHON_SCRIPT

export PROJECT_ROOT
python3 -c "
import os
import json
from pathlib import Path

project_root = os.environ.get('PROJECT_ROOT', '.')
hub_skills = Path(project_root) / 'hub' / 'skills'
output_json = Path(project_root) / 'moltler-hub' / 'src' / 'data' / 'skills.json'

skills = []

def parse_yaml_simple(content):
    result = {}
    current_list = None
    
    for line in content.split('\n'):
        line = line.rstrip()
        if not line or line.startswith('#'):
            continue
        if line.startswith('  - '):
            if current_list is not None:
                item = line[4:].strip().strip(\"'\\\"\")
                current_list.append(item)
            continue
        if ':' in line and not line.startswith(' '):
            parts = line.split(':', 1)
            key = parts[0].strip()
            value = parts[1].strip() if len(parts) > 1 else ''
            if value.startswith('[') and value.endswith(']'):
                items = value[1:-1].split(',')
                result[key] = [i.strip().strip(\"'\\\"\") for i in items if i.strip()]
                current_list = None
            elif value == '':
                result[key] = []
                current_list = result[key]
            else:
                result[key] = value.strip(\"'\\\"\")
                current_list = None
    return result

for vendor_dir in hub_skills.iterdir():
    if not vendor_dir.is_dir():
        continue
    vendor = vendor_dir.name
    for category_dir in vendor_dir.iterdir():
        if not category_dir.is_dir():
            continue
        category = category_dir.name
        for skill_dir in category_dir.iterdir():
            if not skill_dir.is_dir():
                continue
            yaml_file = skill_dir / 'skill.yaml'
            if not yaml_file.exists():
                continue
            try:
                with open(yaml_file, 'r') as f:
                    data = parse_yaml_simple(f.read())
                skill = {
                    'name': data.get('name', skill_dir.name),
                    'displayName': data.get('displayName', skill_dir.name.replace('-', ' ').title()),
                    'description': data.get('description', ''),
                    'version': data.get('version', '1.0.0'),
                    'author': data.get('author', vendor),
                    'category': category,
                    'tags': data.get('tags', [category]),
                    'parameters': [],
                    'returns': 'DOCUMENT',
                    'sourceUrl': f'https://github.com/bahaaldine/moltler/tree/main/hub/skills/{vendor}/{category}/{skill_dir.name}'
                }
                skills.append(skill)
            except Exception as e:
                print(f'  Warning: {yaml_file}: {e}')

skills.sort(key=lambda s: (s['category'], s['name']))
output_json.parent.mkdir(parents=True, exist_ok=True)
with open(output_json, 'w') as f:
    json.dump(skills, f, indent=2)
print(f'  Generated {len(skills)} skills to skills.json')
"

# 2. Generate skills.ts from skills.json
echo "2. Generating skills.ts..."

cat > "$SKILLS_TS" << 'EOF'
// Auto-generated - do not edit
import type { Skill } from '@/lib/skills';
import skillsData from './skills.json';

export const SKILLS: Skill[] = skillsData as Skill[];
EOF

echo "  Generated skills.ts"

# 3. List Agent Skills
echo ""
echo "3. Agent Skills in .agents/skills/:"
AGENT_SKILLS_DIR="$PROJECT_ROOT/.agents/skills"
if [ -d "$AGENT_SKILLS_DIR" ]; then
    for skill_dir in "$AGENT_SKILLS_DIR"/*/; do
        if [ -f "$skill_dir/SKILL.md" ]; then
            skill_name=$(basename "$skill_dir")
            echo "  - $skill_name"
        fi
    done
else
    echo "  (none found)"
fi

# 4. Summary
echo ""
echo "=== Sync Complete ==="
echo ""
echo "Moltler Skills (Layer 2): $(find "$PROJECT_ROOT/hub/skills" -name "skill.yaml" | wc -l | tr -d ' ') skills in hub/skills/"
echo "Agent Skills (Layer 3):   $(find "$AGENT_SKILLS_DIR" -name "SKILL.md" 2>/dev/null | wc -l | tr -d ' ') skills in .agents/skills/"
echo ""
echo "To publish:"
echo "  1. Commit changes: git add -A && git commit -m 'chore: Sync skills'"
echo "  2. Push to GitHub: git push"
echo "  3. Agent Skills are auto-discovered by Cursor from .agents/skills/"

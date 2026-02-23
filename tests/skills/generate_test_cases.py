#!/usr/bin/env python3
"""
Generate test case files for all skills.

This script creates tests.yaml files for each skill based on:
1. The skill definition (parameters, return type)
2. Category-specific test patterns
3. Best practices for testing
"""

import os
import re
import yaml
from pathlib import Path
from typing import Dict, List, Any, Optional


class SkillTestGenerator:
    """Generate test cases for skills."""
    
    # Category-specific test fixtures and parameters
    CATEGORY_FIXTURES = {
        'observability': {
            'fixtures': ['logs-test'],
            'params': {
                'index_pattern': 'logs-test',
                'time_range': '24h',
                'service_name': 'api-gateway'
            }
        },
        'security': {
            'fixtures': ['security-test'],
            'params': {
                'ioc': '192.168.1.100',
                'user_name': 'admin',
                'threshold': 50
            }
        },
        'apm': {
            'fixtures': ['apm-test'],
            'params': {
                'service_name': 'frontend',
                'threshold_ms': 1000,
                'time_range': '1h'
            }
        },
        'metrics': {
            'fixtures': ['metrics-test'],
            'params': {
                'host_name': 'host-01',
                'time_range': '1h'
            }
        },
        'search': {
            'fixtures': ['content-test'],
            'params': {
                'index': 'content-test',
                'query': 'technology'
            }
        },
        'cluster': {
            'fixtures': [],
            'params': {}
        },
        'ml': {
            'fixtures': [],
            'params': {
                'job_id': 'test-job'
            }
        },
        'alerting': {
            'fixtures': [],
            'params': {
                'rule_id': 'test-rule'
            }
        },
        'fleet': {
            'fixtures': [],
            'params': {}
        },
        'integrations': {
            'fixtures': [],
            'params': {}
        },
        'enterprise-search': {
            'fixtures': [],
            'params': {}
        },
        'agent-builder': {
            'fixtures': [],
            'params': {}
        }
    }
    
    # Skills that require external services and should be skipped
    SKIP_SKILLS = {
        'send-slack-message': 'Requires Slack credentials',
        'send-email': 'Requires email server',
        'trigger-pagerduty': 'Requires PagerDuty API key',
        'send-opsgenie-alert': 'Requires OpsGenie API key',
        'create-servicenow-incident': 'Requires ServiceNow credentials',
        'send-teams-message': 'Requires Teams webhook',
        'invoke-aws-lambda': 'Requires AWS credentials',
        'trigger-github-workflow': 'Requires GitHub token',
        'create-jira-issue': 'Requires Jira credentials',
        'send-webhook': 'Requires external webhook endpoint'
    }
    
    def __init__(self, skills_dir: Path):
        self.skills_dir = Path(skills_dir)
    
    def parse_skill_sql(self, sql_path: Path) -> Optional[Dict[str, Any]]:
        """Parse skill.sql to extract metadata."""
        content = sql_path.read_text()
        
        # Extract skill name
        name_match = re.search(r'CREATE\s+SKILL\s+(\w+)', content, re.IGNORECASE)
        if not name_match:
            return None
        
        skill_name = name_match.group(1)
        
        # Extract parameters
        params_match = re.search(r'\(([^)]*)\)\s*RETURNS', content, re.IGNORECASE | re.DOTALL)
        parameters = []
        
        if params_match:
            params_str = params_match.group(1)
            # Parse each parameter
            param_pattern = r"(\w+)\s+(\w+)(?:\s+DESCRIPTION\s+'[^']*')?(?:\s+DEFAULT\s+([^\s,]+))?"
            for match in re.finditer(param_pattern, params_str, re.IGNORECASE):
                param_name, param_type, default = match.groups()
                parameters.append({
                    'name': param_name,
                    'type': param_type.upper(),
                    'default': default.strip("'\"") if default else None,
                    'required': default is None
                })
        
        # Extract return type
        returns_match = re.search(r'RETURNS\s+(\w+)', content, re.IGNORECASE)
        returns = returns_match.group(1).upper() if returns_match else 'ARRAY'
        
        return {
            'name': skill_name,
            'parameters': parameters,
            'returns': returns
        }
    
    def generate_test_params(self, skill: Dict, category: str) -> Dict[str, Any]:
        """Generate test parameters for a skill."""
        category_config = self.CATEGORY_FIXTURES.get(category, {})
        category_params = category_config.get('params', {})
        
        test_params = {}
        
        for param in skill['parameters']:
            param_name = param['name']
            param_type = param['type']
            
            # Use category-specific param if available
            if param_name in category_params:
                test_params[param_name] = category_params[param_name]
                continue
            
            # Use default if available
            if param['default']:
                continue  # Will use default
            
            # Generate test value based on type
            if param_type == 'STRING':
                if 'index' in param_name.lower():
                    test_params[param_name] = 'logs-test'
                elif 'pattern' in param_name.lower():
                    test_params[param_name] = 'logs-test'
                elif 'id' in param_name.lower():
                    test_params[param_name] = 'test-id-001'
                elif 'name' in param_name.lower():
                    test_params[param_name] = 'test-name'
                elif 'query' in param_name.lower():
                    test_params[param_name] = 'test query'
                else:
                    test_params[param_name] = 'test-value'
            elif param_type == 'INT':
                if 'limit' in param_name.lower():
                    test_params[param_name] = 10
                elif 'threshold' in param_name.lower():
                    test_params[param_name] = 50
                else:
                    test_params[param_name] = 100
            elif param_type == 'BOOLEAN':
                test_params[param_name] = True
            elif param_type == 'ARRAY':
                test_params[param_name] = ['item1', 'item2']
        
        return test_params
    
    def generate_tests_yaml(self, skill_dir: Path) -> Optional[Dict]:
        """Generate tests.yaml content for a skill."""
        sql_path = skill_dir / 'skill.sql'
        if not sql_path.exists():
            return None
        
        skill = self.parse_skill_sql(sql_path)
        if not skill:
            return None
        
        # Determine category
        parts = skill_dir.relative_to(self.skills_dir).parts
        category = parts[0] if parts else 'unknown'
        skill_name = skill_dir.name
        
        # Check if skill should be skipped
        skip = skill_name in self.SKIP_SKILLS
        skip_reason = self.SKIP_SKILLS.get(skill_name, '')
        
        # Get fixtures
        category_config = self.CATEGORY_FIXTURES.get(category, {})
        fixtures = category_config.get('fixtures', [])
        
        # Generate test parameters
        test_params = self.generate_test_params(skill, category)
        
        # Build test cases
        tests = []
        
        # Default test case
        default_test = {
            'name': 'default_execution',
            'description': f'Test {skill["name"]} with default/test parameters'
        }
        
        if test_params:
            default_test['parameters'] = test_params
        
        if skip:
            default_test['skip'] = True
            default_test['skip_reason'] = skip_reason
        
        if fixtures:
            default_test['fixtures_required'] = fixtures
        
        # Expected results based on return type
        if skill['returns'] == 'ARRAY':
            default_test['expected_result_type'] = 'array'
        elif skill['returns'] == 'DOCUMENT':
            default_test['expected_result_type'] = 'object'
        
        tests.append(default_test)
        
        return {
            'skill': skill['name'],
            'category': category,
            'tests': tests
        }
    
    def generate_all(self, overwrite: bool = False) -> Dict[str, int]:
        """Generate tests.yaml for all skills."""
        stats = {'created': 0, 'skipped': 0, 'errors': 0}
        
        for skill_sql in self.skills_dir.rglob('skill.sql'):
            skill_dir = skill_sql.parent
            tests_yaml_path = skill_dir / 'tests.yaml'
            
            # Skip if exists and not overwriting
            if tests_yaml_path.exists() and not overwrite:
                stats['skipped'] += 1
                continue
            
            try:
                tests_content = self.generate_tests_yaml(skill_dir)
                if tests_content:
                    with open(tests_yaml_path, 'w') as f:
                        yaml.dump(tests_content, f, default_flow_style=False, sort_keys=False)
                    stats['created'] += 1
                    print(f"✓ Generated: {skill_dir.name}")
                else:
                    stats['errors'] += 1
                    print(f"✗ Failed to parse: {skill_dir.name}")
            except Exception as e:
                stats['errors'] += 1
                print(f"✗ Error: {skill_dir.name} - {e}")
        
        return stats


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="Generate test cases for skills")
    parser.add_argument("--skills-dir", default="hub/skills/elastic", help="Skills directory")
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing tests.yaml")
    parser.add_argument("--category", help="Only generate for specific category")
    
    args = parser.parse_args()
    
    # Find repo root
    script_dir = Path(__file__).parent
    repo_root = script_dir.parent.parent
    skills_dir = repo_root / args.skills_dir
    
    if args.category:
        skills_dir = skills_dir / args.category
    
    if not skills_dir.exists():
        print(f"Skills directory not found: {skills_dir}")
        return 1
    
    print(f"Generating test cases for skills in: {skills_dir}\n")
    
    generator = SkillTestGenerator(repo_root / "hub/skills/elastic")
    stats = generator.generate_all(overwrite=args.overwrite)
    
    print(f"\n{'='*40}")
    print(f"Created: {stats['created']}")
    print(f"Skipped: {stats['skipped']}")
    print(f"Errors:  {stats['errors']}")
    print(f"{'='*40}")
    
    return 0 if stats['errors'] == 0 else 1


if __name__ == "__main__":
    exit(main())

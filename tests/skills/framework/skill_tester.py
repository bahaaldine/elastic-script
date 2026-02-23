"""
Core skill testing functionality.
"""

import os
import re
import time
import yaml
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from dataclasses import dataclass, field
from enum import Enum

from .es_client import ElasticsearchTestClient


class TestStatus(Enum):
    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"


@dataclass
class SkillTestResult:
    """Result of a skill test."""
    skill_name: str
    skill_path: str
    status: TestStatus
    duration_ms: float = 0.0
    message: str = ""
    details: Dict[str, Any] = field(default_factory=dict)
    
    # Test phases
    syntax_valid: bool = False
    install_success: bool = False
    execution_success: bool = False
    return_valid: bool = False
    
    # Execution details
    response: Optional[Dict] = None
    error: Optional[str] = None


@dataclass
class SkillDefinition:
    """Parsed skill definition."""
    name: str
    version: str
    description: str
    author: str
    tags: List[str]
    parameters: List[Dict[str, Any]]
    returns: str
    sql_content: str
    path: Path
    
    # From skill.yaml if present
    category: Optional[str] = None
    requirements: Optional[Dict] = None


class SkillParser:
    """Parse skill SQL and YAML files."""
    
    SKILL_PATTERN = re.compile(
        r"CREATE\s+SKILL\s+(\w+)\s*"
        r"VERSION\s+'([^']+)'\s*"
        r"DESCRIPTION\s+'([^']+)'\s*"
        r"AUTHOR\s+'([^']+)'\s*"
        r"TAGS\s+\[([^\]]+)\]\s*"
        r"\(([^)]*)\)\s*"
        r"RETURNS\s+(\w+)",
        re.IGNORECASE | re.DOTALL
    )
    
    PARAM_PATTERN = re.compile(
        r"(\w+)\s+(\w+)(?:\s+DESCRIPTION\s+'([^']*)')?(?:\s+DEFAULT\s+([^\s,]+))?",
        re.IGNORECASE
    )
    
    @classmethod
    def parse_skill_sql(cls, sql_content: str, path: Path) -> Optional[SkillDefinition]:
        """Parse a skill.sql file into a SkillDefinition."""
        match = cls.SKILL_PATTERN.search(sql_content)
        if not match:
            return None
        
        name, version, description, author, tags_str, params_str, returns = match.groups()
        
        # Parse tags
        tags = [t.strip().strip("'\"") for t in tags_str.split(',')]
        
        # Parse parameters
        parameters = []
        for param_match in cls.PARAM_PATTERN.finditer(params_str):
            param_name, param_type, param_desc, param_default = param_match.groups()
            parameters.append({
                'name': param_name,
                'type': param_type,
                'description': param_desc or '',
                'default': param_default.strip("'\"") if param_default else None,
                'required': param_default is None
            })
        
        return SkillDefinition(
            name=name,
            version=version,
            description=description,
            author=author,
            tags=tags,
            parameters=parameters,
            returns=returns.upper(),
            sql_content=sql_content,
            path=path
        )
    
    @classmethod
    def parse_skill_yaml(cls, yaml_path: Path) -> Optional[Dict]:
        """Parse a skill.yaml file."""
        if not yaml_path.exists():
            return None
        
        with open(yaml_path) as f:
            return yaml.safe_load(f)
    
    @classmethod
    def load_skill(cls, skill_dir: Path) -> Optional[SkillDefinition]:
        """Load a complete skill from a directory."""
        sql_path = skill_dir / "skill.sql"
        yaml_path = skill_dir / "skill.yaml"
        
        if not sql_path.exists():
            return None
        
        with open(sql_path) as f:
            sql_content = f.read()
        
        skill = cls.parse_skill_sql(sql_content, sql_path)
        if not skill:
            return None
        
        # Enhance with YAML metadata if present
        yaml_data = cls.parse_skill_yaml(yaml_path)
        if yaml_data:
            skill.category = yaml_data.get('category')
            skill.requirements = yaml_data.get('requirements')
        
        return skill


@dataclass
class TestCase:
    """A test case for a skill."""
    name: str
    description: str = ""
    parameters: Dict[str, Any] = field(default_factory=dict)
    expected_status: str = "success"
    expected_result_type: Optional[str] = None
    expected_min_results: Optional[int] = None
    expected_max_results: Optional[int] = None
    expected_fields: List[str] = field(default_factory=list)
    skip: bool = False
    skip_reason: str = ""
    fixtures_required: List[str] = field(default_factory=list)


class SkillTester:
    """Test runner for Moltler skills."""
    
    def __init__(self, es_client: ElasticsearchTestClient, skills_dir: Path):
        self.es_client = es_client
        self.skills_dir = Path(skills_dir)
        self.parser = SkillParser()
        self.results: List[SkillTestResult] = []
    
    def discover_skills(self, category: Optional[str] = None) -> List[Path]:
        """Discover all skill directories."""
        skills = []
        
        for skill_sql in self.skills_dir.rglob("skill.sql"):
            skill_dir = skill_sql.parent
            
            # Filter by category if specified
            if category:
                parts = skill_dir.relative_to(self.skills_dir).parts
                if len(parts) >= 2 and parts[1] != category:
                    continue
            
            skills.append(skill_dir)
        
        return sorted(skills)
    
    def load_test_cases(self, skill_dir: Path) -> List[TestCase]:
        """Load test cases for a skill."""
        test_yaml = skill_dir / "tests.yaml"
        
        if test_yaml.exists():
            with open(test_yaml) as f:
                data = yaml.safe_load(f)
                return [TestCase(**tc) for tc in data.get('tests', [])]
        
        # Generate default test case
        return [TestCase(
            name="default",
            description="Default execution test"
        )]
    
    def test_syntax(self, skill: SkillDefinition) -> Tuple[bool, str]:
        """Validate skill SQL syntax."""
        required_fields = ['name', 'version', 'description', 'author', 'returns']
        
        for field in required_fields:
            if not getattr(skill, field, None):
                return False, f"Missing required field: {field}"
        
        if skill.returns not in ('ARRAY', 'DOCUMENT', 'STRING', 'INT', 'BOOLEAN'):
            return False, f"Invalid return type: {skill.returns}"
        
        if not skill.sql_content.strip().endswith(';'):
            return False, "Skill definition must end with semicolon"
        
        if 'BEGIN' not in skill.sql_content.upper():
            return False, "Skill must have BEGIN block"
        
        if 'END SKILL' not in skill.sql_content.upper():
            return False, "Skill must have END SKILL"
        
        return True, "Syntax valid"
    
    def test_install(self, skill: SkillDefinition) -> Tuple[bool, str, Optional[Dict]]:
        """Test skill installation."""
        result = self.es_client.install_skill(skill.sql_content)
        
        if result['success']:
            return True, "Installation successful", result['response']
        else:
            error_msg = result['response'].get('error', {}).get('reason', 'Unknown error')
            return False, f"Installation failed: {error_msg}", result['response']
    
    def test_execution(self, skill: SkillDefinition, test_case: TestCase) -> Tuple[bool, str, Optional[Dict]]:
        """Test skill execution."""
        try:
            result = self.es_client.run_skill(skill.name, test_case.parameters or None)
            
            if not result['success']:
                error_msg = result['response'].get('error', {}).get('reason', 'Unknown error')
                return False, f"Execution failed: {error_msg}", result['response']
            
            # Validate expected status
            if test_case.expected_status == "error" and result['success']:
                return False, "Expected error but got success", result['response']
            
            return True, "Execution successful", result['response']
            
        except Exception as e:
            return False, f"Execution error: {str(e)}", None
    
    def test_return_value(self, skill: SkillDefinition, response: Dict, test_case: TestCase) -> Tuple[bool, str]:
        """Validate return value."""
        result_data = response.get('result')
        
        # Check return type
        if skill.returns == 'ARRAY':
            if not isinstance(result_data, list):
                return False, f"Expected ARRAY, got {type(result_data).__name__}"
            
            # Check result count bounds
            if test_case.expected_min_results is not None:
                if len(result_data) < test_case.expected_min_results:
                    return False, f"Expected at least {test_case.expected_min_results} results, got {len(result_data)}"
            
            if test_case.expected_max_results is not None:
                if len(result_data) > test_case.expected_max_results:
                    return False, f"Expected at most {test_case.expected_max_results} results, got {len(result_data)}"
            
            # Check expected fields in first result
            if test_case.expected_fields and result_data:
                first_result = result_data[0]
                for field in test_case.expected_fields:
                    if field not in first_result:
                        return False, f"Missing expected field: {field}"
        
        elif skill.returns == 'DOCUMENT':
            if not isinstance(result_data, dict):
                return False, f"Expected DOCUMENT, got {type(result_data).__name__}"
        
        return True, "Return value valid"
    
    def run_skill_test(self, skill_dir: Path) -> SkillTestResult:
        """Run all tests for a single skill."""
        start_time = time.time()
        
        # Load skill
        skill = self.parser.load_skill(skill_dir)
        if not skill:
            return SkillTestResult(
                skill_name=skill_dir.name,
                skill_path=str(skill_dir),
                status=TestStatus.ERROR,
                message="Failed to parse skill definition"
            )
        
        result = SkillTestResult(
            skill_name=skill.name,
            skill_path=str(skill_dir),
            status=TestStatus.PASSED
        )
        
        # Phase 1: Syntax validation
        syntax_ok, syntax_msg = self.test_syntax(skill)
        result.syntax_valid = syntax_ok
        if not syntax_ok:
            result.status = TestStatus.FAILED
            result.message = syntax_msg
            result.duration_ms = (time.time() - start_time) * 1000
            return result
        
        # Phase 2: Installation
        install_ok, install_msg, install_response = self.test_install(skill)
        result.install_success = install_ok
        if not install_ok:
            result.status = TestStatus.FAILED
            result.message = install_msg
            result.response = install_response
            result.duration_ms = (time.time() - start_time) * 1000
            return result
        
        # Phase 3: Execution tests
        test_cases = self.load_test_cases(skill_dir)
        
        for test_case in test_cases:
            if test_case.skip:
                continue
            
            exec_ok, exec_msg, exec_response = self.test_execution(skill, test_case)
            result.execution_success = exec_ok
            result.response = exec_response
            
            if not exec_ok:
                result.status = TestStatus.FAILED
                result.message = f"Test '{test_case.name}': {exec_msg}"
                result.duration_ms = (time.time() - start_time) * 1000
                return result
            
            # Phase 4: Return value validation
            if exec_response:
                return_ok, return_msg = self.test_return_value(skill, exec_response, test_case)
                result.return_valid = return_ok
                
                if not return_ok:
                    result.status = TestStatus.FAILED
                    result.message = f"Test '{test_case.name}': {return_msg}"
                    result.duration_ms = (time.time() - start_time) * 1000
                    return result
        
        result.status = TestStatus.PASSED
        result.message = "All tests passed"
        result.duration_ms = (time.time() - start_time) * 1000
        return result
    
    def run_all_tests(self, category: Optional[str] = None, 
                      fail_fast: bool = False) -> List[SkillTestResult]:
        """Run tests for all discovered skills."""
        skill_dirs = self.discover_skills(category)
        self.results = []
        
        for skill_dir in skill_dirs:
            result = self.run_skill_test(skill_dir)
            self.results.append(result)
            
            if fail_fast and result.status == TestStatus.FAILED:
                break
        
        return self.results
    
    def get_summary(self) -> Dict[str, Any]:
        """Get test summary statistics."""
        total = len(self.results)
        passed = sum(1 for r in self.results if r.status == TestStatus.PASSED)
        failed = sum(1 for r in self.results if r.status == TestStatus.FAILED)
        skipped = sum(1 for r in self.results if r.status == TestStatus.SKIPPED)
        errors = sum(1 for r in self.results if r.status == TestStatus.ERROR)
        
        total_duration = sum(r.duration_ms for r in self.results)
        
        return {
            'total': total,
            'passed': passed,
            'failed': failed,
            'skipped': skipped,
            'errors': errors,
            'pass_rate': (passed / total * 100) if total > 0 else 0,
            'total_duration_ms': total_duration,
            'failed_skills': [r.skill_name for r in self.results if r.status == TestStatus.FAILED]
        }

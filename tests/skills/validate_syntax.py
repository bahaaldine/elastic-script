#!/usr/bin/env python3
"""
Skill Syntax Validator

Validates all skill SQL files without requiring Elasticsearch.
This is useful for CI and pre-commit hooks.
"""

import re
import sys
from pathlib import Path
from dataclasses import dataclass
from typing import List, Tuple, Optional


@dataclass
class ValidationResult:
    skill_name: str
    skill_path: str
    valid: bool
    errors: List[str]
    warnings: List[str]


class SkillValidator:
    """Validate skill SQL syntax."""
    
    REQUIRED_PATTERNS = [
        (r'CREATE\s+SKILL\s+\w+', "Missing CREATE SKILL statement"),
        (r"VERSION\s+'[^']+'", "Missing VERSION declaration"),
        (r"DESCRIPTION\s+'[^']+'", "Missing DESCRIPTION"),
        (r"AUTHOR\s+'[^']+'", "Missing AUTHOR"),
        (r'RETURNS\s+\w+', "Missing RETURNS type"),
        (r'\bBEGIN\b', "Missing BEGIN block"),
        (r'\bEND\s+SKILL\b', "Missing END SKILL"),
    ]
    
    VALID_RETURN_TYPES = ['ARRAY', 'DOCUMENT', 'STRING', 'INT', 'BOOLEAN', 'FLOAT']
    
    def __init__(self, skills_dir: Path):
        self.skills_dir = Path(skills_dir)
    
    def validate_skill(self, skill_sql_path: Path) -> ValidationResult:
        """Validate a single skill.sql file."""
        errors = []
        warnings = []
        
        try:
            content = skill_sql_path.read_text()
        except Exception as e:
            return ValidationResult(
                skill_name=skill_sql_path.parent.name,
                skill_path=str(skill_sql_path),
                valid=False,
                errors=[f"Cannot read file: {e}"],
                warnings=[]
            )
        
        # Extract skill name
        name_match = re.search(r'CREATE\s+SKILL\s+(\w+)', content, re.IGNORECASE)
        skill_name = name_match.group(1) if name_match else skill_sql_path.parent.name
        
        # Check required patterns
        for pattern, error_msg in self.REQUIRED_PATTERNS:
            if not re.search(pattern, content, re.IGNORECASE):
                errors.append(error_msg)
        
        # Check return type is valid - look for RETURNS after closing paren, before BEGIN
        # Match pattern: ) followed by optional whitespace, then RETURNS TYPE
        returns_match = re.search(r'\)\s*RETURNS\s+(\w+)', content, re.IGNORECASE)
        if returns_match:
            return_type = returns_match.group(1).upper()
            if return_type not in self.VALID_RETURN_TYPES:
                errors.append(f"Invalid return type: {return_type}")
        
        # Check file ends with semicolon
        if not content.strip().endswith(';'):
            errors.append("Skill must end with semicolon")
        
        # Check for common issues (warnings)
        if 'ESQL_QUERY' in content:
            # Check for SQL injection risks
            if re.search(r"ESQL_QUERY\s*\(\s*'[^']*\|\|", content):
                warnings.append("Potential SQL injection - string concatenation in ESQL_QUERY")
        
        # Check TAGS format
        tags_match = re.search(r'TAGS\s+\[([^\]]+)\]', content, re.IGNORECASE)
        if tags_match:
            tags_content = tags_match.group(1)
            if not re.match(r"^[\s'\"a-zA-Z0-9_,-]+$", tags_content):
                warnings.append("Tags may have unusual characters")
        
        # Check parameter definitions
        params_match = re.search(r'\(([^)]*)\)\s*RETURNS', content, re.IGNORECASE | re.DOTALL)
        if params_match:
            params_str = params_match.group(1).strip()
            if params_str and not re.search(r'\w+\s+\w+', params_str):
                warnings.append("Parameters may be incorrectly formatted")
        
        return ValidationResult(
            skill_name=skill_name,
            skill_path=str(skill_sql_path),
            valid=len(errors) == 0,
            errors=errors,
            warnings=warnings
        )
    
    def validate_all(self) -> List[ValidationResult]:
        """Validate all skills."""
        results = []
        
        for skill_sql in sorted(self.skills_dir.rglob('skill.sql')):
            result = self.validate_skill(skill_sql)
            results.append(result)
        
        return results


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="Validate skill syntax")
    parser.add_argument("--skills-dir", default="hub/skills/elastic", help="Skills directory")
    parser.add_argument("--category", help="Validate specific category only")
    parser.add_argument("--verbose", "-v", action="store_true", help="Show all results")
    parser.add_argument("--warnings", "-w", action="store_true", help="Show warnings")
    
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
    
    print("=" * 60)
    print("SKILL SYNTAX VALIDATION")
    print("=" * 60)
    print(f"\nValidating skills in: {skills_dir}\n")
    
    validator = SkillValidator(skills_dir)
    results = validator.validate_all()
    
    # Print results
    valid_count = 0
    invalid_count = 0
    warning_count = 0
    
    for result in results:
        if result.valid:
            valid_count += 1
            if args.verbose:
                print(f"\033[92m✓\033[0m {result.skill_name}")
        else:
            invalid_count += 1
            print(f"\033[91m✗\033[0m {result.skill_name}")
            for error in result.errors:
                print(f"    - {error}")
        
        if args.warnings and result.warnings:
            warning_count += len(result.warnings)
            for warning in result.warnings:
                print(f"    \033[93m⚠\033[0m {warning}")
    
    # Summary
    print("\n" + "-" * 60)
    print(f"Total: {len(results)} | Valid: {valid_count} | Invalid: {invalid_count}")
    
    if args.warnings:
        print(f"Warnings: {warning_count}")
    
    pass_rate = (valid_count / len(results) * 100) if results else 0
    color = "\033[92m" if pass_rate == 100 else ("\033[93m" if pass_rate >= 90 else "\033[91m")
    print(f"Pass Rate: {color}{pass_rate:.1f}%\033[0m")
    print("=" * 60)
    
    # List failed skills
    if invalid_count > 0:
        print("\n\033[91mFailed Skills:\033[0m")
        for result in results:
            if not result.valid:
                print(f"  - {result.skill_name}: {', '.join(result.errors)}")
    
    return 1 if invalid_count > 0 else 0


if __name__ == "__main__":
    sys.exit(main())

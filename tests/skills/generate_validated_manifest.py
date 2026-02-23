#!/usr/bin/env python3
"""
Generate a manifest of validated skills for the MoltlerHub.

This creates a JSON file listing all skills that have passed validation,
which the Hub can use to show "Verified" badges.
"""

import json
import sys
from pathlib import Path
from datetime import datetime

# Import validator
sys.path.insert(0, str(Path(__file__).parent))
from validate_syntax import SkillValidator


def main():
    # Find paths
    script_dir = Path(__file__).parent
    repo_root = script_dir.parent.parent
    elastic_skills_dir = repo_root / "hub/skills/elastic"
    community_skills_dir = repo_root / "hub/skills/community"
    output_dir = repo_root / "moltler-hub/src/data"
    
    # Ensure output directory exists
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print("Validating all skills...")
    
    # Validate elastic skills
    all_results = []
    if elastic_skills_dir.exists():
        validator = SkillValidator(elastic_skills_dir)
        all_results.extend(validator.validate_all())
    
    # Validate community skills
    if community_skills_dir.exists():
        validator = SkillValidator(community_skills_dir)
        all_results.extend(validator.validate_all())
    
    # Build manifest
    manifest = {
        'generated_at': datetime.utcnow().isoformat() + 'Z',
        'total_skills': len(all_results),
        'valid_skills': sum(1 for r in all_results if r.valid),
        'skills': {}
    }
    
    for result in all_results:
        skill_path = Path(result.skill_path)
        # Determine category from path
        if 'elastic' in str(skill_path):
            base_dir = elastic_skills_dir
        else:
            base_dir = community_skills_dir
        
        try:
            parts = skill_path.relative_to(base_dir).parent.parts
            category = parts[0] if parts else 'unknown'
            skill_slug = parts[-1] if len(parts) > 1 else skill_path.parent.name
        except ValueError:
            category = 'unknown'
            skill_slug = skill_path.parent.name
        
        manifest['skills'][result.skill_name] = {
            'name': result.skill_name,
            'slug': skill_slug,
            'category': category,
            'syntax_valid': result.valid,
            'tested': True,
            'errors': result.errors if not result.valid else [],
            'warnings': result.warnings
        }
    
    # Write manifest
    output_path = output_dir / "validated_skills.json"
    with open(output_path, 'w') as f:
        json.dump(manifest, f, indent=2)
    
    print(f"\nManifest generated: {output_path}")
    print(f"Total: {manifest['total_skills']} | Valid: {manifest['valid_skills']}")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())

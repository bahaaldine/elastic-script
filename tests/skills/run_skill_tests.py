#!/usr/bin/env python3
"""
Moltler Skill Test Runner

Comprehensive testing for all Moltler skills.

Usage:
    python run_skill_tests.py                    # Run all tests
    python run_skill_tests.py --category security  # Test specific category
    python run_skill_tests.py --skill hunt_ioc     # Test specific skill
    python run_skill_tests.py --setup-fixtures     # Setup test data only
    python run_skill_tests.py --verbose            # Verbose output
    python run_skill_tests.py --fail-fast          # Stop on first failure
"""

import argparse
import sys
import os
from pathlib import Path

# Add framework to path
sys.path.insert(0, str(Path(__file__).parent))

from framework.es_client import ElasticsearchTestClient, ESConfig
from framework.skill_tester import SkillTester, SkillTestResult, TestStatus
from framework.fixtures import FixtureManager
from framework.reporter import TestReporter


def check_prerequisites(es_client: ElasticsearchTestClient) -> bool:
    """Check that Elasticsearch and plugin are available."""
    print("Checking prerequisites...")
    
    if not es_client.is_available():
        print("❌ Elasticsearch is not available")
        print(f"   URL: {es_client.config.url}")
        print("   Make sure Elasticsearch is running")
        return False
    print(f"✓ Elasticsearch available at {es_client.config.url}")
    
    version = es_client.get_version()
    if version:
        print(f"✓ Elasticsearch version: {version}")
    
    if not es_client.is_plugin_installed():
        print("❌ elastic-script plugin is not installed")
        print("   Run: ./scripts/quick-start.sh")
        return False
    print("✓ elastic-script plugin installed")
    
    return True


def setup_fixtures(es_client: ElasticsearchTestClient, verbose: bool = False) -> bool:
    """Set up test fixtures."""
    print("\nSetting up test fixtures...")
    
    fixture_manager = FixtureManager(es_client)
    
    if fixture_manager.setup_all():
        print("✓ Test fixtures created")
        
        if verbose:
            status = fixture_manager.get_fixture_status()
            for index, info in status.items():
                print(f"  - {index}: {info['document_count']} documents")
        
        return True
    else:
        print("❌ Failed to create test fixtures")
        return False


def run_tests(
    es_client: ElasticsearchTestClient,
    skills_dir: Path,
    category: str = None,
    skill_name: str = None,
    fail_fast: bool = False,
    verbose: bool = False
) -> int:
    """Run skill tests and return exit code."""
    
    tester = SkillTester(es_client, skills_dir)
    
    # Discover skills
    skill_dirs = tester.discover_skills(category)
    
    # Filter to specific skill if requested
    if skill_name:
        skill_dirs = [d for d in skill_dirs if d.name == skill_name or d.name == skill_name.replace('_', '-')]
        if not skill_dirs:
            print(f"❌ Skill not found: {skill_name}")
            return 1
    
    print(f"\nDiscovered {len(skill_dirs)} skills to test")
    
    if len(skill_dirs) == 0:
        print("No skills found to test")
        return 0
    
    # Run tests
    print("\nRunning tests...\n")
    results = []
    
    for i, skill_dir in enumerate(skill_dirs, 1):
        if verbose:
            print(f"[{i}/{len(skill_dirs)}] Testing {skill_dir.name}...", end=" ", flush=True)
        
        result = tester.run_skill_test(skill_dir)
        results.append(result)
        
        if verbose:
            if result.status == TestStatus.PASSED:
                print(f"✓ ({result.duration_ms:.0f}ms)")
            else:
                print(f"✗ {result.message}")
        
        if fail_fast and result.status == TestStatus.FAILED:
            print("\n⚠ Stopping due to --fail-fast")
            break
    
    # Generate reports
    reporter = TestReporter(results)
    reporter.print_summary(verbose)
    
    report_paths = reporter.generate_all_reports()
    print(f"\nReports generated:")
    for format_name, path in report_paths.items():
        print(f"  - {format_name}: {path}")
    
    # Return exit code based on results
    failed_count = sum(1 for r in results if r.status in (TestStatus.FAILED, TestStatus.ERROR))
    return 1 if failed_count > 0 else 0


def main():
    parser = argparse.ArgumentParser(description="Moltler Skill Test Runner")
    parser.add_argument("--category", "-c", help="Test skills in specific category")
    parser.add_argument("--skill", "-s", help="Test specific skill by name")
    parser.add_argument("--setup-fixtures", action="store_true", help="Only setup test fixtures")
    parser.add_argument("--teardown-fixtures", action="store_true", help="Remove test fixtures")
    parser.add_argument("--fail-fast", "-x", action="store_true", help="Stop on first failure")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")
    parser.add_argument("--skip-fixtures", action="store_true", help="Skip fixture setup")
    parser.add_argument("--es-url", default=os.getenv("ES_URL", "http://localhost:9200"), help="Elasticsearch URL")
    parser.add_argument("--es-user", default=os.getenv("ES_USERNAME", "elastic-admin"), help="Elasticsearch username")
    parser.add_argument("--es-pass", default=os.getenv("ES_PASSWORD", "elastic-password"), help="Elasticsearch password")
    parser.add_argument("--skills-dir", default="hub/skills/elastic", help="Skills directory relative to repo root")
    
    args = parser.parse_args()
    
    # Find repo root
    script_dir = Path(__file__).parent
    repo_root = script_dir.parent.parent
    skills_dir = repo_root / args.skills_dir
    
    if not skills_dir.exists():
        print(f"❌ Skills directory not found: {skills_dir}")
        return 1
    
    # Configure ES client
    config = ESConfig(
        url=args.es_url,
        username=args.es_user,
        password=args.es_pass
    )
    es_client = ElasticsearchTestClient(config)
    
    # Check prerequisites
    if not check_prerequisites(es_client):
        return 1
    
    # Handle fixture-only operations
    if args.teardown_fixtures:
        print("\nTearing down fixtures...")
        fixture_manager = FixtureManager(es_client)
        fixture_manager.installed_fixtures = ['logs-test', 'metrics-test', 'security-test', 'apm-test', 'content-test']
        fixture_manager.teardown_all()
        print("✓ Fixtures removed")
        return 0
    
    if args.setup_fixtures:
        return 0 if setup_fixtures(es_client, args.verbose) else 1
    
    # Setup fixtures unless skipped
    if not args.skip_fixtures:
        if not setup_fixtures(es_client, args.verbose):
            return 1
    
    # Run tests
    return run_tests(
        es_client=es_client,
        skills_dir=skills_dir,
        category=args.category,
        skill_name=args.skill,
        fail_fast=args.fail_fast,
        verbose=args.verbose
    )


if __name__ == "__main__":
    sys.exit(main())

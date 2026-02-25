#!/usr/bin/env python3
"""
ESQL Query Test Runner
======================

Tests the ESQL queries embedded in skill.sql files by running them directly
against an Elasticsearch cluster. This validates the queries independently
of the elastic-script plugin (which may not be installed on cloud clusters).

Usage:
    # Run all skills
    python test_esql_queries.py

    # Run a single skill by directory name
    python test_esql_queries.py --skill detect-change-points

    # Run skills from a specific category
    python test_esql_queries.py --category community/observability

    # Verbose output with full query and response
    python test_esql_queries.py --verbose

    # Set up test fixtures first
    python test_esql_queries.py --setup-fixtures

    # Tear down test fixtures after
    python test_esql_queries.py --teardown-fixtures
"""

import argparse
import json
import os
import re
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

# Add parent to path so we can import framework
sys.path.insert(0, str(Path(__file__).parent))

from framework.es_client import ElasticsearchTestClient, ESConfig
from framework.fixtures import FixtureManager


# ---------------------------------------------------------------------------
# ESQL extraction from skill.sql files
# ---------------------------------------------------------------------------

# Default parameter values from CREATE SKILL declarations
PARAM_DEFAULT_PATTERN = re.compile(
    r"(\w+)\s+(?:STRING|INT|INTEGER|FLOAT|DOUBLE|BOOLEAN)\s+"
    r"(?:DESCRIPTION\s+'[^']*'\s+)?"
    r"DEFAULT\s+(?:'([^']*)'|([\w.]+))",
    re.IGNORECASE
)


def _find_matching_paren(text: str, start: int) -> int:
    """Find the closing ')' that matches the '(' at position start."""
    depth = 0
    in_single_quote = False
    i = start
    while i < len(text):
        ch = text[i]
        if in_single_quote:
            if ch == "'" and i + 1 < len(text) and text[i + 1] == "'":
                i += 2  # escaped quote
                continue
            elif ch == "'":
                in_single_quote = False
        else:
            if ch == "'":
                in_single_quote = True
            elif ch == '(':
                depth += 1
            elif ch == ')':
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return -1


def extract_esql_queries(skill_sql: str) -> List[str]:
    """Extract ESQL query expressions from ESQL_QUERY(...) calls.

    Handles both simple string literals and concatenated expressions like:
        ESQL_QUERY('FROM ' || index_pattern || ' | LIMIT ' || limit)
    """
    results = []
    pattern = re.compile(r'ESQL_QUERY\s*\(', re.IGNORECASE)
    for m in pattern.finditer(skill_sql):
        open_pos = m.end() - 1  # position of '('
        close_pos = _find_matching_paren(skill_sql, open_pos)
        if close_pos == -1:
            continue
        inner = skill_sql[open_pos + 1:close_pos].strip()
        results.append(inner)
    return results


def extract_param_defaults(skill_sql: str) -> Dict[str, str]:
    """Extract parameter names and their default values from a skill definition."""
    defaults = {}
    for m in PARAM_DEFAULT_PATTERN.finditer(skill_sql):
        name = m.group(1)
        value = m.group(2) if m.group(2) is not None else m.group(3)
        defaults[name] = value
    return defaults


# Map of well-known skill parameters to sensible test values.
# These are used when a skill concatenates a parameter into its ESQL query
# and we need a concrete value to make it runnable.
TEST_SUBSTITUTIONS = {
    # Index patterns — use the test fixture indices
    'index_pattern': 'logs-test',
    'index': 'logs-test',
    # Fields
    'field': 'log.level',
    'metric_field': 'system.cpu.total.pct',
    'date_field': '@timestamp',
    'text_field': 'message',
    'vector_field': 'embedding',
    'group_field': 'host.name',
    # Values
    'query': 'error',
    'metric': 'cpu',
    'service': 'api-gateway',
    'service_name': 'api-gateway',
    'time_range': '7d',
    'interval': '1d',
    'limit': '10',
    'k': '10',
    'threshold': '2.0',
    'std_dev_threshold': '3.0',
    'threshold_pct': '20.0',
    'min_count': '1',
}


def make_query_runnable(raw_expr: str, param_defaults: Dict[str, str]) -> Optional[str]:
    """Resolve a concatenated ESQL_QUERY expression into a runnable query string.

    Input is the full expression inside ESQL_QUERY(...), e.g.:
        'FROM ' || index_pattern || ' | LIMIT ' || limit

    Returns the assembled query string with variables substituted, or None
    if the expression can't be resolved.
    """
    expr = raw_expr.strip()

    # If it's a single quoted string with no concatenation, just unwrap it
    if expr.startswith("'") and expr.endswith("'") and '||' not in expr:
        return re.sub(r'\s+', ' ', expr[1:-1].replace("''", "'")).strip()

    # Merge defaults with test substitutions
    subs = {**TEST_SUBSTITUTIONS}
    for k, v in param_defaults.items():
        if v and v != 'NULL':
            subs[k] = v

    # Split on || and resolve each part
    parts = re.split(r'\s*\|\|\s*', expr)
    resolved = []
    for part in parts:
        part = part.strip()
        if not part:
            continue

        # Quoted string literal
        if part.startswith("'") and part.endswith("'"):
            resolved.append(part[1:-1].replace("''", "'"))
        # Known variable
        elif part in subs:
            resolved.append(str(subs[part]))
        # Numeric literal
        elif re.match(r'^[\d.]+$', part):
            resolved.append(part)
        else:
            # Unknown variable — can't resolve
            return None

    query = ''.join(resolved)
    query = re.sub(r'\s+', ' ', query).strip()
    return query if query else None


# ---------------------------------------------------------------------------
# Test execution
# ---------------------------------------------------------------------------

@dataclass
class QueryTestResult:
    """Result of testing a single ESQL query."""
    skill_name: str
    query_index: int
    query: str
    success: bool
    columns: List[Dict[str, str]]
    row_count: int
    error: Optional[str] = None
    duration_ms: float = 0.0
    skipped: bool = False
    skip_reason: Optional[str] = None


def find_skill_dirs(base_dirs: List[str], category: Optional[str] = None,
                    skill: Optional[str] = None) -> List[Path]:
    """Find skill directories matching filters."""
    results = []
    for base in base_dirs:
        base_path = Path(base)
        if not base_path.exists():
            continue
        for skill_sql in sorted(base_path.rglob('skill.sql')):
            skill_dir = skill_sql.parent
            rel = skill_dir.relative_to(base_path)

            if skill and skill_dir.name != skill:
                continue
            if category and not str(rel).startswith(category):
                continue
            results.append(skill_dir)
    return results


def test_skill_queries(es: ElasticsearchTestClient, skill_dir: Path,
                       verbose: bool = False) -> List[QueryTestResult]:
    """Test all ESQL queries in a skill.sql file."""
    skill_sql_path = skill_dir / 'skill.sql'
    skill_sql = skill_sql_path.read_text()
    skill_name = skill_dir.name

    raw_queries = extract_esql_queries(skill_sql)
    param_defaults = extract_param_defaults(skill_sql)

    if not raw_queries:
        return [QueryTestResult(
            skill_name=skill_name, query_index=0, query='',
            success=True, columns=[], row_count=0,
            skipped=True, skip_reason='No ESQL_QUERY() calls found'
        )]

    results = []
    for i, raw_q in enumerate(raw_queries):
        runnable = make_query_runnable(raw_q, param_defaults)
        if runnable is None:
            results.append(QueryTestResult(
                skill_name=skill_name, query_index=i, query=raw_q,
                success=True, columns=[], row_count=0,
                skipped=True,
                skip_reason='Could not resolve all variable substitutions'
            ))
            continue

        if verbose:
            print(f"  Query {i+1}: {runnable[:120]}...")

        start = time.time()
        resp = es.run_esql(runnable)
        duration = (time.time() - start) * 1000

        result = QueryTestResult(
            skill_name=skill_name,
            query_index=i,
            query=runnable,
            success=resp['success'],
            columns=resp['columns'],
            row_count=len(resp['values']),
            error=resp.get('error'),
            duration_ms=duration,
        )
        results.append(result)

        if verbose:
            status = 'PASS' if result.success else 'FAIL'
            print(f"    [{status}] {result.row_count} rows, {result.duration_ms:.0f}ms")
            if result.error:
                print(f"    Error: {result.error}")

    return results


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description='Test ESQL queries from skill files')
    parser.add_argument('--skill', help='Test a specific skill by directory name')
    parser.add_argument('--category', help='Filter by category path (e.g. community/observability)')
    parser.add_argument('--setup-fixtures', action='store_true', help='Set up test fixtures first')
    parser.add_argument('--teardown-fixtures', action='store_true', help='Tear down fixtures after')
    parser.add_argument('--verbose', '-v', action='store_true', help='Show query details')
    parser.add_argument('--skills-dir', action='append',
                        default=['hub/skills/elastic', 'hub/skills/community'],
                        help='Base directories to scan (can specify multiple)')
    args = parser.parse_args()

    config = ESConfig.from_env()
    es = ElasticsearchTestClient(config)

    if not es.is_available():
        print(f"ERROR: Cannot connect to Elasticsearch at {config.url}")
        print(f"  Auth mode: {'API key (cloud)' if config.is_cloud else 'basic auth (local)'}")
        sys.exit(1)

    version = es.get_version()
    print(f"Connected to Elasticsearch {version} at {config.url}")
    print(f"Auth mode: {'API key (cloud)' if config.is_cloud else 'basic auth (local)'}")

    # Fixture setup
    if args.setup_fixtures:
        print("\nSetting up test fixtures...")
        fm = FixtureManager(es)
        if fm.setup_all():
            print("  Fixtures ready.")
        else:
            print("  WARNING: Some fixtures failed to set up.")

    # Find skills
    skill_dirs = find_skill_dirs(args.skills_dir, category=args.category, skill=args.skill)
    if not skill_dirs:
        print("No skills found matching filters.")
        sys.exit(1)

    print(f"\nTesting ESQL queries from {len(skill_dirs)} skill(s)...\n")

    all_results: List[QueryTestResult] = []
    passed = 0
    failed = 0
    skipped = 0

    for skill_dir in skill_dirs:
        rel_path = str(skill_dir).split('hub/skills/')[-1] if 'hub/skills/' in str(skill_dir) else skill_dir.name
        print(f"  {rel_path}", end='')

        results = test_skill_queries(es, skill_dir, verbose=args.verbose)
        all_results.extend(results)

        skill_pass = all(r.success for r in results if not r.skipped)
        skill_skip = all(r.skipped for r in results)

        if skill_skip:
            print(f" ... SKIP ({results[0].skip_reason})")
            skipped += 1
        elif skill_pass:
            query_count = sum(1 for r in results if not r.skipped)
            total_rows = sum(r.row_count for r in results if not r.skipped)
            print(f" ... PASS ({query_count} queries, {total_rows} rows)")
            passed += 1
        else:
            print(f" ... FAIL")
            for r in results:
                if not r.success and not r.skipped:
                    print(f"    Query {r.query_index+1}: {r.error}")
            failed += 1

    # Summary
    print(f"\n{'='*60}")
    print(f"Results: {passed} passed, {failed} failed, {skipped} skipped")
    print(f"{'='*60}")

    # Teardown
    if args.teardown_fixtures:
        print("\nTearing down test fixtures...")
        fm = FixtureManager(es)
        fm.installed_fixtures = ['logs-test', 'metrics-test', 'security-test', 'apm-test', 'content-test']
        fm.teardown_all()
        print("  Done.")

    sys.exit(1 if failed > 0 else 0)


if __name__ == '__main__':
    main()

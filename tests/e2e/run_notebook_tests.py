#!/usr/bin/env python3
"""
E2E Test Runner for elastic-script notebooks.

This script programmatically executes Jupyter notebooks and validates that:
1. All cells execute without errors
2. Expected outputs are present
3. The elastic-script kernel is functioning correctly

Usage:
    python run_notebook_tests.py                    # Run all notebooks
    python run_notebook_tests.py --notebook 01      # Run specific notebook
    python run_notebook_tests.py --verbose          # Verbose output
    python run_notebook_tests.py --no-prompt        # Skip OpenAI key prompt (for CI)

Environment Variables:
    OPENAI_API_KEY    - If set, AI tests will run without prompting
"""

import argparse
import html
import json
import os
import shutil
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Optional, List, Dict, Any

# Try to import nbclient for notebook execution
try:
    import nbformat
    from nbclient import NotebookClient
    from nbclient.exceptions import CellExecutionError
except ImportError:
    print("Installing required packages...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "nbformat", "nbclient"])
    import nbformat
    from nbclient import NotebookClient
    from nbclient.exceptions import CellExecutionError

# Configuration
NOTEBOOKS_DIR = Path(__file__).parent.parent.parent / "notebooks"
ELASTICSEARCH_URL = "http://localhost:9200"
ES_USER = "elastic-admin"
ES_PASSWORD = "elastic-password"
SKIP_CONFIG_PATH = Path(__file__).parent / "skip_cells.json"
REPORTS_DIR = Path(__file__).parent / "reports"

# OpenAI API key for AI notebooks
OPENAI_API_KEY = None


def check_openai_key() -> bool:
    """Check if OpenAI API key is available, prompt if not."""
    global OPENAI_API_KEY
    OPENAI_API_KEY = os.environ.get('OPENAI_API_KEY')
    if OPENAI_API_KEY:
        print("✅ OpenAI API key found in environment")
        return True
    print("\n🔑 OpenAI API Key")
    print("   The AI notebook (03-ai-observability) requires an OpenAI API key.")
    print("   You can:")
    print("   1. Enter your key now")
    print("   2. Press Enter to skip AI tests")
    print("   3. Set OPENAI_API_KEY environment variable before running")
    print()
    try:
        key = input("   Enter OpenAI API key (or press Enter to skip): ").strip()
        if key:
            OPENAI_API_KEY = key
            os.environ['OPENAI_API_KEY'] = key
            print("   ✅ API key set for this session")
            return True
        else:
            print("   ⏭️  Skipping AI tests")
            return False
    except (EOFError, KeyboardInterrupt):
        print("\n   ⏭️  Skipping AI tests")
        return False


def load_skip_config(has_openai_key: bool = False) -> Dict[str, Any]:
    """Load skip configuration for notebooks with known issues."""
    config = {}
    if SKIP_CONFIG_PATH.exists():
        with open(SKIP_CONFIG_PATH, 'r') as f:
            config = json.load(f)
    if has_openai_key and "03-ai-observability.ipynb" in config:
        del config["03-ai-observability.ipynb"]
    return config


def clear_reports_dir():
    """Clear the reports directory before running tests."""
    if REPORTS_DIR.exists():
        shutil.rmtree(REPORTS_DIR)
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    print(f"📁 Reports directory: {REPORTS_DIR}")


class TestResult:
    """Result of a notebook test execution."""
    def __init__(self, notebook_name: str):
        self.notebook_name = notebook_name
        self.passed = True
        self.cell_results: List[Dict[str, Any]] = []
        self.error_message: Optional[str] = None
        self.duration_seconds: float = 0
        self.skipped: bool = False

    def add_cell_result(self, cell_index: int, passed: bool, output: str = "", error: str = ""):
        self.cell_results.append({
            "index": cell_index,
            "passed": passed,
            "output": output[:500] if output else "",
            "error": error
        })
        if not passed:
            self.passed = False

    def __str__(self):
        status = "✅ PASSED" if self.passed else "❌ FAILED"
        return f"{self.notebook_name}: {status} ({self.duration_seconds:.1f}s)"


def check_elasticsearch_running() -> bool:
    """Check if Elasticsearch is running and accessible."""
    import urllib.request
    import base64
    try:
        credentials = base64.b64encode(f"{ES_USER}:{ES_PASSWORD}".encode()).decode()
        req = urllib.request.Request(ELASTICSEARCH_URL)
        req.add_header("Authorization", f"Basic {credentials}")
        with urllib.request.urlopen(req, timeout=5) as response:
            return response.status == 200
    except Exception:
        return False


def get_notebooks() -> List[Path]:
    """Get list of test notebooks, sorted by name."""
    notebooks = []
    for nb_path in NOTEBOOKS_DIR.glob("*.ipynb"):
        if ".ipynb_checkpoints" in str(nb_path):
            continue
        notebooks.append(nb_path)
    return sorted(notebooks)


def execute_notebook(notebook_path: Path, verbose: bool = False, skip_cells: List[int] = None) -> TestResult:
    """Execute a notebook and return test results."""
    if skip_cells is None:
        skip_cells = []
    result = TestResult(notebook_path.name)
    start_time = time.time()
    try:
        with open(notebook_path, 'r', encoding='utf-8') as f:
            nb = nbformat.read(f, as_version=4)
        kernel_name = nb.metadata.get('kernelspec', {}).get('name', '')
        if kernel_name != 'plesql_kernel' and verbose:
            print(f"  ⚠️  Notebook uses kernel '{kernel_name}', not 'plesql_kernel'")
        for i in skip_cells:
            if i < len(nb.cells):
                nb.cells[i].cell_type = 'raw'
                if verbose:
                    print(f"    Cell {i}: ⏭️ SKIPPED (known issue)")
        client = NotebookClient(
            nb,
            timeout=120,
            kernel_name='plesql_kernel',
            resources={'metadata': {'path': str(NOTEBOOKS_DIR)}}
        )
        if verbose:
            print(f"  Executing {len(nb.cells)} cells...")
        try:
            client.execute()
            for i, cell in enumerate(nb.cells):
                if i in skip_cells:
                    result.add_cell_result(i, True, "", "SKIPPED")
                    continue
                if cell.cell_type == 'code':
                    outputs = cell.get('outputs', [])
                    has_error = any(
                        o.get('output_type') == 'error' or o.get('name') == 'stderr'
                        for o in outputs
                    )
                    output_text = ""
                    for o in outputs:
                        if 'text' in o:
                            output_text += o['text']
                        elif 'data' in o and 'text/plain' in o['data']:
                            output_text += o['data']['text/plain']
                    if has_error:
                        error_text = next(
                            (o.get('evalue', '') for o in outputs if o.get('output_type') == 'error'),
                            "Unknown error"
                        )
                        result.add_cell_result(i, False, output_text, error_text)
                        if verbose:
                            print(f"    Cell {i}: ❌ Error - {error_text[:100]}")
                    else:
                        result.add_cell_result(i, True, output_text)
                        if verbose and output_text:
                            print(f"    Cell {i}: ✅ OK")
        except CellExecutionError as e:
            result.passed = False
            result.error_message = str(e)
            if verbose:
                print(f"  ❌ Cell execution error: {e}")
    except Exception as e:
        result.passed = False
        result.error_message = str(e)
        if verbose:
            print(f"  ❌ Error: {e}")
    result.duration_seconds = time.time() - start_time
    return result


def generate_html_report(results: List[TestResult], start_time: datetime) -> str:
    """Generate an HTML report for the test results."""
    end_time = datetime.now()
    duration = (end_time - start_time).total_seconds()
    passed = sum(1 for r in results if r.passed)
    failed = len(results) - passed
    skipped = sum(1 for r in results if r.skipped)
    status_class = "failed" if failed > 0 else "passed"
    status_text = "FAILED" if failed > 0 else "PASSED"
    status_emoji = "❌" if failed > 0 else "✅"

    html_content = f'''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>E2E Test Report - {start_time.strftime('%Y-%m-%d %H:%M:%S')}</title>
    <style>
        :root {{
            --bg-primary: #0d1117;
            --bg-secondary: #161b22;
            --bg-tertiary: #21262d;
            --text-primary: #c9d1d9;
            --text-secondary: #8b949e;
            --border-color: #30363d;
            --success-color: #238636;
            --success-bg: #1b4721;
            --failure-color: #da3633;
            --failure-bg: #4a1d1d;
            --skip-color: #9e6a03;
            --skip-bg: #3d2e00;
            --accent-color: #58a6ff;
        }}
        * {{ box-sizing: border-box; margin: 0; padding: 0; }}
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: var(--bg-primary);
            color: var(--text-primary);
            line-height: 1.6;
            padding: 2rem;
        }}
        .container {{ max-width: 1200px; margin: 0 auto; }}
        header {{ text-align: center; margin-bottom: 2rem; padding-bottom: 2rem; border-bottom: 1px solid var(--border-color); }}
        h1 {{ font-size: 2rem; margin-bottom: 0.5rem; }}
        .timestamp {{ color: var(--text-secondary); font-size: 0.9rem; }}
        .summary {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 2rem; }}
        .summary-card {{ background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; padding: 1.5rem; text-align: center; }}
        .summary-card.passed {{ border-left: 4px solid var(--success-color); }}
        .summary-card.failed {{ border-left: 4px solid var(--failure-color); }}
        .summary-card.skipped {{ border-left: 4px solid var(--skip-color); }}
        .summary-card.duration {{ border-left: 4px solid var(--accent-color); }}
        .summary-value {{ font-size: 2.5rem; font-weight: bold; }}
        .summary-label {{ color: var(--text-secondary); font-size: 0.85rem; text-transform: uppercase; }}
        .status-badge {{ display: inline-block; padding: 0.5rem 1.5rem; border-radius: 20px; font-weight: bold; margin: 1rem 0; }}
        .status-badge.passed {{ background: var(--success-bg); color: #3fb950; }}
        .status-badge.failed {{ background: var(--failure-bg); color: #f85149; }}
        .notebook-card {{ background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; margin-bottom: 1rem; overflow: hidden; }}
        .notebook-header {{ display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.5rem; background: var(--bg-tertiary); cursor: pointer; }}
        .notebook-header:hover {{ background: #2d333b; }}
        .notebook-name {{ font-weight: 600; font-size: 1.1rem; }}
        .notebook-meta {{ display: flex; gap: 1rem; align-items: center; }}
        .badge {{ padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.8rem; font-weight: 500; }}
        .badge.passed {{ background: var(--success-bg); color: #3fb950; }}
        .badge.failed {{ background: var(--failure-bg); color: #f85149; }}
        .badge.skipped {{ background: var(--skip-bg); color: #d29922; }}
        .notebook-details {{ padding: 1rem 1.5rem; display: none; border-top: 1px solid var(--border-color); }}
        .notebook-details.show {{ display: block; }}
        .cell-list {{ list-style: none; }}
        .cell-item {{ display: flex; align-items: flex-start; padding: 0.5rem 0; border-bottom: 1px solid var(--border-color); }}
        .cell-item:last-child {{ border-bottom: none; }}
        .cell-index {{ width: 60px; color: var(--text-secondary); font-family: monospace; }}
        .cell-status {{ width: 24px; margin-right: 0.5rem; }}
        .cell-output {{ flex: 1; font-family: monospace; font-size: 0.85rem; color: var(--text-secondary); white-space: pre-wrap; max-height: 200px; overflow-y: auto; background: var(--bg-tertiary); padding: 0.5rem; border-radius: 4px; }}
        .cell-error {{ color: #f85149; }}
        .error-message {{ background: var(--failure-bg); color: #f85149; padding: 1rem; border-radius: 4px; margin-top: 0.5rem; font-family: monospace; font-size: 0.85rem; white-space: pre-wrap; }}
        footer {{ text-align: center; margin-top: 3rem; padding-top: 2rem; border-top: 1px solid var(--border-color); color: var(--text-secondary); font-size: 0.85rem; }}
        footer a {{ color: var(--accent-color); text-decoration: none; }}
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>📓 E2E Test Report</h1>
            <p class="timestamp">Generated: {end_time.strftime('%Y-%m-%d %H:%M:%S')}</p>
            <div class="status-badge {status_class}">{status_emoji} {status_text}</div>
        </header>
        <div class="summary">
            <div class="summary-card passed"><div class="summary-value">{passed}</div><div class="summary-label">Passed</div></div>
            <div class="summary-card failed"><div class="summary-value">{failed}</div><div class="summary-label">Failed</div></div>
            <div class="summary-card skipped"><div class="summary-value">{skipped}</div><div class="summary-label">Skipped</div></div>
            <div class="summary-card duration"><div class="summary-value">{duration:.1f}s</div><div class="summary-label">Duration</div></div>
        </div>
        <div class="notebook-results">
            <h2>📋 Notebook Results</h2>
'''

    for result in results:
        status = "skipped" if result.skipped else ("passed" if result.passed else "failed")
        badge_text = "SKIPPED" if result.skipped else ("PASSED" if result.passed else "FAILED")
        html_content += f'''
            <div class="notebook-card">
                <div class="notebook-header" onclick="toggleDetails(this)">
                    <span class="notebook-name">📓 {html.escape(result.notebook_name)}</span>
                    <div class="notebook-meta">
                        <span>{result.duration_seconds:.1f}s</span>
                        <span class="badge {status}">{badge_text}</span>
                    </div>
                </div>
                <div class="notebook-details">
'''
        if result.error_message:
            html_content += f'<div class="error-message">{html.escape(result.error_message)}</div>'
        if result.cell_results:
            html_content += '<ul class="cell-list">'
            for cell in result.cell_results:
                cell_status = "⏭️" if cell.get("error") == "SKIPPED" else ("✅" if cell["passed"] else "❌")
                output = cell.get("output", "")
                error = cell.get("error", "")
                display_text = error if error else output
                error_class = ' cell-error' if error and error != 'SKIPPED' else ''
                html_content += f'''
                <li class="cell-item">
                    <span class="cell-index">Cell {cell["index"]}</span>
                    <span class="cell-status">{cell_status}</span>
                    <div class="cell-output{error_class}">{html.escape(display_text) or '(no output)'}</div>
                </li>'''
            html_content += '</ul>'
        html_content += '</div></div>'

    html_content += '''
        </div>
        <footer>
            <p>elastic-script E2E Test Suite</p>
        </footer>
    </div>
    <script>
        function toggleDetails(header) {
            header.nextElementSibling.classList.toggle('show');
        }
        document.querySelectorAll('.notebook-card').forEach(card => {
            if (card.querySelector('.badge.failed')) {
                card.querySelector('.notebook-details').classList.add('show');
            }
        });
    </script>
</body>
</html>
'''
    return html_content


def save_html_report(results: List[TestResult], start_time: datetime) -> Path:
    """Save HTML report to the reports directory."""
    report_content = generate_html_report(results, start_time)
    timestamp = start_time.strftime('%Y%m%d_%H%M%S')
    report_path = REPORTS_DIR / f"report_{timestamp}.html"
    latest_path = REPORTS_DIR / "latest.html"
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report_content)
    with open(latest_path, 'w', encoding='utf-8') as f:
        f.write(report_content)
    return report_path


def run_tests(notebook_filter: Optional[str] = None, verbose: bool = False, skip_openai_prompt: bool = False) -> List[TestResult]:
    """Run all notebook tests."""
    results = []
    print("🔍 Checking Elasticsearch...")
    if not check_elasticsearch_running():
        print("❌ Elasticsearch is not running!")
        print("   Run: ./scripts/quick-start.sh")
        sys.exit(1)
    print("✅ Elasticsearch is running")
    has_openai_key = False
    if not skip_openai_prompt:
        needs_openai = notebook_filter is None or "03" in (notebook_filter or "") or "ai" in (notebook_filter or "").lower()
        if needs_openai:
            has_openai_key = check_openai_key()
        else:
            has_openai_key = bool(os.environ.get('OPENAI_API_KEY'))
    else:
        has_openai_key = bool(os.environ.get('OPENAI_API_KEY'))
    skip_config = load_skip_config(has_openai_key)
    print()
    notebooks = get_notebooks()
    if notebook_filter:
        notebooks = [nb for nb in notebooks if notebook_filter in nb.name]
    if not notebooks:
        print(f"No notebooks found matching filter: {notebook_filter}")
        return []
    print(f"📓 Running {len(notebooks)} notebook(s)...")
    print("=" * 60)
    for nb_path in notebooks:
        nb_name = nb_path.name
        nb_skip_info = skip_config.get(nb_name, {})
        if nb_skip_info.get("skip_all", False):
            reason = nb_skip_info.get("reason", "No reason provided")
            print(f"\n⏭️  {nb_name} - SKIPPED")
            print(f"   Reason: {reason}")
            result = TestResult(nb_name)
            result.passed = True
            result.skipped = True
            result.error_message = f"SKIPPED: {reason}"
            results.append(result)
            continue
        print(f"\n▶️  {nb_name}")
        result = execute_notebook(nb_path, verbose, nb_skip_info.get("skip_cells", []))
        results.append(result)
        if result.passed:
            print(f"   ✅ PASSED ({result.duration_seconds:.1f}s)")
        else:
            print(f"   ❌ FAILED: {result.error_message or 'See cell errors'}")
    return results


def print_summary(results: List[TestResult], report_path: Optional[Path] = None):
    """Print test summary."""
    print("\n" + "=" * 60)
    print("📊 TEST SUMMARY")
    print("=" * 60)
    passed = sum(1 for r in results if r.passed)
    failed = len(results) - passed
    total_time = sum(r.duration_seconds for r in results)
    for result in results:
        status = "⏭️" if result.skipped else ("✅" if result.passed else "❌")
        print(f"  {status} {result.notebook_name} ({result.duration_seconds:.1f}s)")
    print("-" * 60)
    print(f"Total: {passed} passed, {failed} failed ({total_time:.1f}s)")
    if report_path:
        print(f"\n📄 HTML Report: {report_path}")
        print(f"   Latest report: {REPORTS_DIR / 'latest.html'}")
    if failed > 0:
        print("\n❌ Some tests failed!")
        return 1
    else:
        print("\n✅ All tests passed!")
        return 0


def main():
    parser = argparse.ArgumentParser(description="Run E2E notebook tests for elastic-script")
    parser.add_argument("--notebook", "-n", help="Filter notebooks by name (e.g., '01', 'getting-started')")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")
    parser.add_argument("--list", "-l", action="store_true", help="List available notebooks")
    parser.add_argument("--no-prompt", action="store_true", help="Don't prompt for OpenAI key (for CI)")
    parser.add_argument("--no-report", action="store_true", help="Don't generate HTML report")
    args = parser.parse_args()
    if args.list:
        print("Available notebooks:")
        for nb in get_notebooks():
            print(f"  - {nb.name}")
        return 0
    if not args.no_report:
        clear_reports_dir()
    start_time = datetime.now()
    results = run_tests(args.notebook, args.verbose, args.no_prompt)
    report_path = None
    if not args.no_report and results:
        report_path = save_html_report(results, start_time)
    return print_summary(results, report_path)


if __name__ == "__main__":
    sys.exit(main())

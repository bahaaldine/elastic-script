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
"""

import argparse
import json
import os
import subprocess
import sys
import time
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


def load_skip_config() -> Dict[str, Any]:
    """Load skip configuration for notebooks with known issues."""
    if SKIP_CONFIG_PATH.exists():
        with open(SKIP_CONFIG_PATH, 'r') as f:
            return json.load(f)
    return {}


class TestResult:
    """Result of a notebook test execution."""
    
    def __init__(self, notebook_name: str):
        self.notebook_name = notebook_name
        self.passed = True
        self.cell_results: List[Dict[str, Any]] = []
        self.error_message: Optional[str] = None
        self.duration_seconds: float = 0
    
    def add_cell_result(self, cell_index: int, passed: bool, output: str = "", error: str = ""):
        self.cell_results.append({
            "index": cell_index,
            "passed": passed,
            "output": output[:500] if output else "",  # Truncate long outputs
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
        # Skip checkpoint files
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
        # Read the notebook
        with open(notebook_path, 'r', encoding='utf-8') as f:
            nb = nbformat.read(f, as_version=4)
        
        # Check if notebook has the right kernel
        kernel_name = nb.metadata.get('kernelspec', {}).get('name', '')
        if kernel_name != 'plesql_kernel':
            if verbose:
                print(f"  ⚠️  Notebook uses kernel '{kernel_name}', not 'plesql_kernel'")
        
        # Mark cells to skip by making them markdown temporarily
        original_types = {}
        for i in skip_cells:
            if i < len(nb.cells):
                original_types[i] = nb.cells[i].cell_type
                nb.cells[i].cell_type = 'raw'  # Skip by changing type
                if verbose:
                    print(f"    Cell {i}: ⏭️ SKIPPED (known issue)")
        
        # Create notebook client
        client = NotebookClient(
            nb,
            timeout=120,  # 2 minutes per cell
            kernel_name='plesql_kernel',
            resources={'metadata': {'path': str(NOTEBOOKS_DIR)}}
        )
        
        # Execute the notebook
        if verbose:
            print(f"  Executing {len(nb.cells)} cells...")
        
        try:
            client.execute()
            
            # Check each cell for outputs
            for i, cell in enumerate(nb.cells):
                # Skip cells that were marked to skip
                if i in skip_cells:
                    result.add_cell_result(i, True, "", "SKIPPED")
                    continue
                    
                if cell.cell_type == 'code':
                    outputs = cell.get('outputs', [])
                    has_error = any(
                        o.get('output_type') == 'error' or
                        o.get('name') == 'stderr'
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


def run_tests(notebook_filter: Optional[str] = None, verbose: bool = False) -> List[TestResult]:
    """Run all notebook tests."""
    results = []
    skip_config = load_skip_config()
    
    # Check Elasticsearch
    print("🔍 Checking Elasticsearch...")
    if not check_elasticsearch_running():
        print("❌ Elasticsearch is not running!")
        print("   Run: ./scripts/quick-start.sh")
        sys.exit(1)
    print("✅ Elasticsearch is running")
    print()
    
    # Get notebooks
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
        
        # Check if entire notebook should be skipped
        if nb_skip_info.get("skip_all", False):
            reason = nb_skip_info.get("reason", "No reason provided")
            print(f"\n⏭️  {nb_name} - SKIPPED")
            print(f"   Reason: {reason}")
            result = TestResult(nb_name)
            result.passed = True  # Count skipped as passed
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


def print_summary(results: List[TestResult]):
    """Print test summary."""
    print("\n" + "=" * 60)
    print("📊 TEST SUMMARY")
    print("=" * 60)
    
    passed = sum(1 for r in results if r.passed)
    failed = len(results) - passed
    total_time = sum(r.duration_seconds for r in results)
    
    for result in results:
        status = "✅" if result.passed else "❌"
        print(f"  {status} {result.notebook_name} ({result.duration_seconds:.1f}s)")
    
    print("-" * 60)
    print(f"Total: {passed} passed, {failed} failed ({total_time:.1f}s)")
    
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
    args = parser.parse_args()
    
    if args.list:
        print("Available notebooks:")
        for nb in get_notebooks():
            print(f"  - {nb.name}")
        return 0
    
    results = run_tests(args.notebook, args.verbose)
    return print_summary(results)


if __name__ == "__main__":
    sys.exit(main())

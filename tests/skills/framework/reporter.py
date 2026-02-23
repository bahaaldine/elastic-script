"""
Test result reporting and output formatting.
"""

import json
import sys
from datetime import datetime
from pathlib import Path
from typing import List, Optional
from dataclasses import asdict

from .skill_tester import SkillTestResult, TestStatus


class TestReporter:
    """Generate test reports in various formats."""
    
    def __init__(self, results: List[SkillTestResult], output_dir: Optional[Path] = None):
        self.results = results
        self.output_dir = output_dir or Path("tests/skills/results")
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    def print_summary(self, verbose: bool = False) -> None:
        """Print test summary to console."""
        total = len(self.results)
        passed = sum(1 for r in self.results if r.status == TestStatus.PASSED)
        failed = sum(1 for r in self.results if r.status == TestStatus.FAILED)
        skipped = sum(1 for r in self.results if r.status == TestStatus.SKIPPED)
        errors = sum(1 for r in self.results if r.status == TestStatus.ERROR)
        
        print("\n" + "=" * 60)
        print("SKILL TEST RESULTS")
        print("=" * 60)
        
        # Status symbols
        status_symbols = {
            TestStatus.PASSED: "✓",
            TestStatus.FAILED: "✗",
            TestStatus.SKIPPED: "○",
            TestStatus.ERROR: "!"
        }
        
        status_colors = {
            TestStatus.PASSED: "\033[92m",  # Green
            TestStatus.FAILED: "\033[91m",  # Red
            TestStatus.SKIPPED: "\033[93m", # Yellow
            TestStatus.ERROR: "\033[91m"    # Red
        }
        reset = "\033[0m"
        
        # Print each result
        for result in sorted(self.results, key=lambda r: (r.status != TestStatus.PASSED, r.skill_name)):
            symbol = status_symbols[result.status]
            color = status_colors[result.status]
            
            print(f"{color}{symbol}{reset} {result.skill_name} ({result.duration_ms:.0f}ms)")
            
            if verbose or result.status != TestStatus.PASSED:
                if result.message:
                    print(f"    {result.message}")
        
        # Print summary
        print("\n" + "-" * 60)
        print(f"Total: {total} | Passed: {passed} | Failed: {failed} | Skipped: {skipped} | Errors: {errors}")
        
        pass_rate = (passed / total * 100) if total > 0 else 0
        color = "\033[92m" if pass_rate == 100 else ("\033[93m" if pass_rate >= 80 else "\033[91m")
        print(f"Pass Rate: {color}{pass_rate:.1f}%{reset}")
        print("=" * 60)
        
        # Print failed skills
        if failed > 0 or errors > 0:
            print("\n\033[91mFailed Skills:\033[0m")
            for r in self.results:
                if r.status in (TestStatus.FAILED, TestStatus.ERROR):
                    print(f"  - {r.skill_name}: {r.message}")
    
    def generate_json_report(self, filename: str = "test_results.json") -> Path:
        """Generate JSON report."""
        report = {
            'timestamp': datetime.utcnow().isoformat(),
            'summary': {
                'total': len(self.results),
                'passed': sum(1 for r in self.results if r.status == TestStatus.PASSED),
                'failed': sum(1 for r in self.results if r.status == TestStatus.FAILED),
                'skipped': sum(1 for r in self.results if r.status == TestStatus.SKIPPED),
                'errors': sum(1 for r in self.results if r.status == TestStatus.ERROR)
            },
            'results': []
        }
        
        for result in self.results:
            result_dict = {
                'skill_name': result.skill_name,
                'skill_path': result.skill_path,
                'status': result.status.value,
                'duration_ms': result.duration_ms,
                'message': result.message,
                'syntax_valid': result.syntax_valid,
                'install_success': result.install_success,
                'execution_success': result.execution_success,
                'return_valid': result.return_valid
            }
            report['results'].append(result_dict)
        
        output_path = self.output_dir / filename
        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        return output_path
    
    def generate_markdown_report(self, filename: str = "test_results.md") -> Path:
        """Generate Markdown report."""
        total = len(self.results)
        passed = sum(1 for r in self.results if r.status == TestStatus.PASSED)
        failed = sum(1 for r in self.results if r.status == TestStatus.FAILED)
        pass_rate = (passed / total * 100) if total > 0 else 0
        
        lines = [
            "# Skill Test Results",
            "",
            f"**Date:** {datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S UTC')}",
            "",
            "## Summary",
            "",
            f"| Metric | Value |",
            f"|--------|-------|",
            f"| Total Skills | {total} |",
            f"| Passed | {passed} |",
            f"| Failed | {failed} |",
            f"| Pass Rate | {pass_rate:.1f}% |",
            "",
        ]
        
        # Group by category
        categories = {}
        for result in self.results:
            parts = Path(result.skill_path).parts
            category = parts[-3] if len(parts) >= 3 else "unknown"
            if category not in categories:
                categories[category] = []
            categories[category].append(result)
        
        for category, results in sorted(categories.items()):
            cat_passed = sum(1 for r in results if r.status == TestStatus.PASSED)
            lines.append(f"## {category.title()} ({cat_passed}/{len(results)})")
            lines.append("")
            lines.append("| Skill | Status | Duration | Message |")
            lines.append("|-------|--------|----------|---------|")
            
            for r in sorted(results, key=lambda x: x.skill_name):
                status_emoji = "✅" if r.status == TestStatus.PASSED else "❌"
                lines.append(f"| {r.skill_name} | {status_emoji} | {r.duration_ms:.0f}ms | {r.message[:50]}... |")
            
            lines.append("")
        
        # Failed skills detail
        failed_results = [r for r in self.results if r.status != TestStatus.PASSED]
        if failed_results:
            lines.append("## Failed Skills Detail")
            lines.append("")
            for r in failed_results:
                lines.append(f"### {r.skill_name}")
                lines.append(f"- **Path:** `{r.skill_path}`")
                lines.append(f"- **Status:** {r.status.value}")
                lines.append(f"- **Message:** {r.message}")
                lines.append("")
        
        output_path = self.output_dir / filename
        with open(output_path, 'w') as f:
            f.write('\n'.join(lines))
        
        return output_path
    
    def generate_junit_xml(self, filename: str = "test_results.xml") -> Path:
        """Generate JUnit XML report for CI integration."""
        total = len(self.results)
        failed = sum(1 for r in self.results if r.status == TestStatus.FAILED)
        errors = sum(1 for r in self.results if r.status == TestStatus.ERROR)
        skipped = sum(1 for r in self.results if r.status == TestStatus.SKIPPED)
        total_time = sum(r.duration_ms for r in self.results) / 1000
        
        lines = [
            '<?xml version="1.0" encoding="UTF-8"?>',
            f'<testsuite name="SkillTests" tests="{total}" failures="{failed}" errors="{errors}" skipped="{skipped}" time="{total_time:.3f}">',
        ]
        
        for result in self.results:
            # Extract category from path
            parts = Path(result.skill_path).parts
            category = parts[-3] if len(parts) >= 3 else "unknown"
            
            lines.append(f'  <testcase classname="skills.{category}" name="{result.skill_name}" time="{result.duration_ms/1000:.3f}">')
            
            if result.status == TestStatus.FAILED:
                lines.append(f'    <failure message="{self._escape_xml(result.message)}" />')
            elif result.status == TestStatus.ERROR:
                lines.append(f'    <error message="{self._escape_xml(result.message)}" />')
            elif result.status == TestStatus.SKIPPED:
                lines.append(f'    <skipped message="{self._escape_xml(result.message)}" />')
            
            lines.append('  </testcase>')
        
        lines.append('</testsuite>')
        
        output_path = self.output_dir / filename
        with open(output_path, 'w') as f:
            f.write('\n'.join(lines))
        
        return output_path
    
    def generate_badge_json(self, filename: str = "badge.json") -> Path:
        """Generate JSON for shields.io badge."""
        total = len(self.results)
        passed = sum(1 for r in self.results if r.status == TestStatus.PASSED)
        pass_rate = (passed / total * 100) if total > 0 else 0
        
        if pass_rate == 100:
            color = "brightgreen"
        elif pass_rate >= 90:
            color = "green"
        elif pass_rate >= 70:
            color = "yellow"
        else:
            color = "red"
        
        badge = {
            "schemaVersion": 1,
            "label": "skills",
            "message": f"{passed}/{total} passing",
            "color": color
        }
        
        output_path = self.output_dir / filename
        with open(output_path, 'w') as f:
            json.dump(badge, f, indent=2)
        
        return output_path
    
    @staticmethod
    def _escape_xml(text: str) -> str:
        """Escape special XML characters."""
        return (text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace('"', "&quot;")
            .replace("'", "&apos;"))
    
    def generate_all_reports(self) -> dict:
        """Generate all report formats."""
        return {
            'json': str(self.generate_json_report()),
            'markdown': str(self.generate_markdown_report()),
            'junit': str(self.generate_junit_xml()),
            'badge': str(self.generate_badge_json())
        }

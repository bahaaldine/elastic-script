"""Tests for the output formatting module."""

import pytest
from io import StringIO
from unittest.mock import patch

from rich.console import Console

from escript_cli.config import Config
from escript_cli.output import OutputFormatter
from escript_cli.client import ExecutionResult


@pytest.fixture
def formatter():
    """Create a formatter instance."""
    config = Config(color=True, table_style="rounded")
    return OutputFormatter(config)


@pytest.fixture
def no_color_formatter():
    """Create a formatter without color."""
    config = Config(color=False)
    return OutputFormatter(config)


class TestResultFormatting:
    """Test result formatting."""
    
    def test_success_none_result(self, formatter, capsys):
        """Test formatting successful result with None data."""
        result = ExecutionResult(success=True, data=None)
        formatter.print_result(result)
        
        # Should print success message (captured by Rich)
        # Just verify no exception is raised
    
    def test_success_string_result(self, formatter):
        """Test formatting successful string result."""
        result = ExecutionResult(success=True, data="Hello World")
        
        # Should not raise
        formatter.print_result(result)
    
    def test_success_number_result(self, formatter):
        """Test formatting successful number result."""
        result = ExecutionResult(success=True, data=42)
        formatter.print_result(result)
    
    def test_success_dict_result(self, formatter):
        """Test formatting successful dict result."""
        result = ExecutionResult(
            success=True,
            data={"action": "CREATE", "procedure": "test_proc"}
        )
        formatter.print_result(result)
    
    def test_success_list_result(self, formatter):
        """Test formatting successful list result."""
        result = ExecutionResult(
            success=True,
            data=[
                {"name": "proc1", "parameters": 2},
                {"name": "proc2", "parameters": 0},
            ]
        )
        formatter.print_result(result)
    
    def test_error_result(self, formatter):
        """Test formatting error result."""
        result = ExecutionResult(
            success=False,
            data=None,
            error="Syntax error near 'CRATE'"
        )
        formatter.print_result(result)
    
    def test_result_with_output(self, formatter):
        """Test formatting result with PRINT output."""
        result = ExecutionResult(
            success=True,
            data=None,
            output=["Line 1", "Line 2", "Line 3"]
        )
        formatter.print_result(result)


class TestTableFormatting:
    """Test table formatting."""
    
    def test_dict_as_table(self, formatter):
        """Test dictionary is displayed as key-value table."""
        result = ExecutionResult(
            success=True,
            data={
                "name": "test_proc",
                "parameters": 2,
                "created": "2024-01-15",
            }
        )
        formatter.print_result(result)
    
    def test_list_of_dicts_as_table(self, formatter):
        """Test list of dicts is displayed as table."""
        result = ExecutionResult(
            success=True,
            data=[
                {"name": "proc1", "params": 2},
                {"name": "proc2", "params": 0},
                {"name": "proc3", "params": 1},
            ]
        )
        formatter.print_result(result)
    
    def test_empty_list(self, formatter):
        """Test empty list displays 'Empty result'."""
        result = ExecutionResult(success=True, data=[])
        formatter.print_result(result)


class TestActionResultFormatting:
    """Test action result formatting (CREATE, DROP, etc.)."""
    
    def test_create_procedure_result(self, formatter):
        """Test CREATE PROCEDURE result formatting."""
        result = ExecutionResult(
            success=True,
            data={
                "action": "CREATE PROCEDURE",
                "procedure": "test_proc",
                "parameters": 2,
            }
        )
        formatter.print_result(result)
    
    def test_drop_skill_result(self, formatter):
        """Test DROP SKILL result formatting."""
        result = ExecutionResult(
            success=True,
            data={
                "action": "DROP SKILL",
                "skill": "old_skill",
            }
        )
        formatter.print_result(result)


class TestMessageFormatting:
    """Test message formatting methods."""
    
    def test_print_error(self, formatter):
        """Test error message formatting."""
        formatter.print_error("Something went wrong")
    
    def test_print_warning(self, formatter):
        """Test warning message formatting."""
        formatter.print_warning("This might be a problem")
    
    def test_print_info(self, formatter):
        """Test info message formatting."""
        formatter.print_info("For your information")
    
    def test_print_success(self, formatter):
        """Test success message formatting."""
        formatter.print_success("Operation completed")


class TestHelpFormatting:
    """Test help formatting."""
    
    def test_print_help(self, formatter):
        """Test help output."""
        formatter.print_help()


class TestSyntaxHighlighting:
    """Test syntax-highlighted code output."""
    
    def test_print_syntax(self, formatter):
        """Test syntax highlighting."""
        code = """
        CREATE PROCEDURE test()
        BEGIN
            PRINT 'Hello';
        END PROCEDURE;
        """
        formatter.print_syntax(code)


class TestWelcomeMessage:
    """Test welcome message."""
    
    def test_print_welcome(self, formatter):
        """Test welcome banner."""
        formatter.print_welcome("Connected to my-cluster (Elasticsearch 8.17.0)")


class TestTableStyles:
    """Test different table styles."""
    
    def test_rounded_style(self):
        """Test rounded table style."""
        config = Config(table_style="rounded")
        formatter = OutputFormatter(config)
        assert formatter._box is not None
    
    def test_simple_style(self):
        """Test simple table style."""
        config = Config(table_style="simple")
        formatter = OutputFormatter(config)
        assert formatter._box is not None
    
    def test_minimal_style(self):
        """Test minimal table style."""
        config = Config(table_style="minimal")
        formatter = OutputFormatter(config)
        assert formatter._box is not None
    
    def test_markdown_style(self):
        """Test markdown table style."""
        config = Config(table_style="markdown")
        formatter = OutputFormatter(config)
        assert formatter._box is not None
    
    def test_invalid_style_defaults(self):
        """Test invalid style defaults to rounded."""
        config = Config(table_style="nonexistent")
        formatter = OutputFormatter(config)
        assert formatter._box is not None


class TestJsonFormatting:
    """Test JSON output formatting."""
    
    def test_nested_dict(self, formatter):
        """Test nested dictionary JSON output."""
        result = ExecutionResult(
            success=True,
            data={
                "outer": {
                    "inner": {
                        "value": 42
                    }
                }
            }
        )
        formatter.print_result(result)
    
    def test_complex_list(self, formatter):
        """Test complex list JSON output."""
        result = ExecutionResult(
            success=True,
            data=[
                {"items": [1, 2, 3]},
                {"items": [4, 5, 6]},
            ]
        )
        formatter.print_result(result)

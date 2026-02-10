"""Tests for the auto-completion module."""

import pytest
from prompt_toolkit.document import Document

from escript_cli.completer import ElasticScriptCompleter


@pytest.fixture
def completer():
    """Create a completer instance."""
    return ElasticScriptCompleter()


class TestKeywordCompletion:
    """Test keyword completion."""
    
    def test_complete_create(self, completer):
        """Test completing 'CRE' to 'CREATE'."""
        doc = Document('CRE')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'CREATE' in texts
    
    def test_complete_procedure(self, completer):
        """Test completing 'PROC' to 'PROCEDURE'."""
        doc = Document('PROC')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'PROCEDURE' in texts
    
    def test_complete_after_create(self, completer):
        """Test completing after CREATE suggests object types."""
        doc = Document('CREATE PR')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'PROCEDURE' in texts
    
    def test_case_insensitive(self, completer):
        """Test case-insensitive completion."""
        doc = Document('cre')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'CREATE' in texts


class TestFunctionCompletion:
    """Test function completion."""
    
    def test_complete_esql(self, completer):
        """Test completing 'ESQ' to 'ESQL_QUERY'."""
        doc = Document('ESQ')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'ESQL_QUERY' in texts
    
    def test_complete_array_functions(self, completer):
        """Test completing 'ARRAY_' suggests array functions."""
        doc = Document('ARRAY_L')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'ARRAY_LENGTH' in texts
    
    def test_complete_llm_functions(self, completer):
        """Test completing 'LLM_' suggests LLM functions."""
        doc = Document('LLM_')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'LLM_COMPLETE' in texts
        assert 'LLM_CHAT' in texts


class TestContextAwareCompletion:
    """Test context-aware completion."""
    
    def test_after_call_suggests_procedures(self, completer):
        """Test that CALL suggests procedures."""
        # Add some procedures
        completer.update_procedures(['my_proc', 'another_proc'])
        
        doc = Document('CALL my')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'my_proc' in texts
    
    def test_after_show_suggests_objects(self, completer):
        """Test that SHOW suggests object types."""
        doc = Document('SHOW PROC')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        # PROCEDURE or PROCEDURES both valid
        assert 'PROCEDURE' in texts or 'PROCEDURES' in texts
    
    def test_after_drop_suggests_objects(self, completer):
        """Test that DROP suggests object types."""
        doc = Document('DROP PROC')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'PROCEDURE' in texts


class TestUserDefinedCompletion:
    """Test completion of user-defined objects."""
    
    def test_procedure_completion(self, completer):
        """Test procedure name completion."""
        completer.update_procedures(['analyze_logs', 'cleanup_old', 'process_data'])
        
        doc = Document('CALL ana')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'analyze_logs' in texts
    
    def test_skill_completion(self, completer):
        """Test skill name completion."""
        completer.update_skills(['check_health', 'generate_report'])
        
        doc = Document('check')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'check_health' in texts
    
    def test_function_completion(self, completer):
        """Test user function completion."""
        completer.update_functions(['calculate_total', 'format_date'])
        
        doc = Document('calc')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'calculate_total' in texts


class TestVariableCompletion:
    """Test variable completion."""
    
    def test_at_error(self, completer):
        """Test @error variable completion."""
        doc = Document('@err')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert '@error' in texts
    
    def test_at_result(self, completer):
        """Test @result variable completion."""
        doc = Document('@res')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert '@result' in texts or '@results' in texts


class TestNamespaceCompletion:
    """Test namespace method completion."""
    
    def test_namespace_suggestions(self, completer):
        """Test namespace name completion."""
        doc = Document('STRI')
        completions = list(completer.get_completions(doc, None))
        
        texts = [c.text for c in completions]
        assert 'STRING' in texts


class TestMetadata:
    """Test completion metadata."""
    
    def test_keyword_meta(self, completer):
        """Test that keywords have correct metadata."""
        doc = Document('CREA')
        completions = list(completer.get_completions(doc, None))
        
        for c in completions:
            if c.text == 'CREATE':
                # display_meta can be string or FormattedText
                meta = str(c.display_meta) if c.display_meta else ''
                assert 'keyword' in meta
                break
    
    def test_function_meta(self, completer):
        """Test that functions have correct metadata."""
        doc = Document('ESQL')
        completions = list(completer.get_completions(doc, None))
        
        for c in completions:
            if c.text == 'ESQL_QUERY':
                # display_meta can be string or FormattedText
                meta = str(c.display_meta) if c.display_meta else ''
                assert 'function' in meta
                break
    
    def test_procedure_meta(self, completer):
        """Test that procedures have correct metadata."""
        completer.update_procedures(['test_proc'])
        
        doc = Document('test_')
        completions = list(completer.get_completions(doc, None))
        
        for c in completions:
            if c.text == 'test_proc':
                # display_meta can be string or FormattedText
                meta = str(c.display_meta) if c.display_meta else ''
                assert 'procedure' in meta
                break


class TestEdgeCases:
    """Test edge cases in completion."""
    
    def test_empty_input(self, completer):
        """Test completion with empty input."""
        doc = Document('')
        completions = list(completer.get_completions(doc, None))
        
        # Should return nothing for empty input
        assert len(completions) == 0
    
    def test_whitespace_only(self, completer):
        """Test completion with whitespace only."""
        doc = Document('   ')
        completions = list(completer.get_completions(doc, None))
        
        # Should return nothing for whitespace
        assert len(completions) == 0
    
    def test_no_matches(self, completer):
        """Test completion with no matches."""
        doc = Document('XYZNONEXISTENT')
        completions = list(completer.get_completions(doc, None))
        
        # Should return nothing for non-matching input
        assert len(completions) == 0

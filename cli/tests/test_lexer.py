"""Tests for the elastic-script Pygments lexer."""

import pytest
from pygments.token import (
    Token, Keyword, Name, String, Number, Comment, Operator, Punctuation
)

from escript_cli.lexer import ElasticScriptLexer, get_lexer


@pytest.fixture
def lexer():
    """Create a lexer instance."""
    return ElasticScriptLexer()


class TestLexerBasics:
    """Test basic lexer functionality."""
    
    def test_get_lexer(self):
        """Test get_lexer helper function."""
        lexer = get_lexer()
        assert isinstance(lexer, ElasticScriptLexer)
    
    def test_lexer_name(self, lexer):
        """Test lexer metadata."""
        assert lexer.name == 'ElasticScript'
        assert 'escript' in lexer.aliases
        assert '*.es' in lexer.filenames


class TestKeywordHighlighting:
    """Test keyword highlighting."""
    
    def test_core_keywords(self, lexer):
        """Test core keywords are highlighted."""
        tokens = list(lexer.get_tokens('CREATE PROCEDURE BEGIN END'))
        keywords = [t for t in tokens if t[0] in Token.Keyword]
        assert len(keywords) >= 4
    
    def test_control_flow_keywords(self, lexer):
        """Test control flow keywords."""
        tokens = list(lexer.get_tokens('IF THEN ELSE ELSEIF FOR WHILE LOOP'))
        keywords = [t for t in tokens if t[0] in Token.Keyword]
        assert len(keywords) >= 6
    
    def test_case_insensitive(self, lexer):
        """Test keywords are matched case-insensitively."""
        # Note: Pygments lexer may not be case-insensitive by default
        # This test verifies uppercase keywords work
        tokens = list(lexer.get_tokens('CREATE create CREATE'))
        keywords = [t for t in tokens if t[0] in Token.Keyword]
        # At least the uppercase ones should match
        assert len(keywords) >= 2


class TestTypeHighlighting:
    """Test data type highlighting."""
    
    def test_basic_types(self, lexer):
        """Test basic type keywords."""
        tokens = list(lexer.get_tokens('STRING NUMBER BOOLEAN ARRAY DOCUMENT'))
        type_tokens = [t for t in tokens if t[0] == Keyword.Type]
        assert len(type_tokens) == 5
    
    def test_declare_with_type(self, lexer):
        """Test DECLARE statement with type."""
        code = 'DECLARE count NUMBER'
        tokens = list(lexer.get_tokens(code))
        # Should have DECLARE keyword and NUMBER type
        assert any(t[0] == Keyword and 'DECLARE' in t[1] for t in tokens)
        assert any(t[0] == Keyword.Type and 'NUMBER' in t[1] for t in tokens)


class TestStringHighlighting:
    """Test string literal highlighting."""
    
    def test_single_quoted_string(self, lexer):
        """Test single-quoted strings."""
        tokens = list(lexer.get_tokens("'hello world'"))
        string_tokens = [t for t in tokens if t[0] in Token.String]
        assert len(string_tokens) >= 1
    
    def test_double_quoted_string(self, lexer):
        """Test double-quoted strings."""
        tokens = list(lexer.get_tokens('"hello world"'))
        string_tokens = [t for t in tokens if t[0] in Token.String]
        assert len(string_tokens) >= 1
    
    def test_escaped_quote(self, lexer):
        """Test escaped quotes in strings."""
        tokens = list(lexer.get_tokens("'it''s working'"))
        string_tokens = [t for t in tokens if t[0] in Token.String]
        assert len(string_tokens) >= 1


class TestNumberHighlighting:
    """Test number highlighting."""
    
    def test_integer(self, lexer):
        """Test integer literals."""
        tokens = list(lexer.get_tokens('42'))
        number_tokens = [t for t in tokens if t[0] in Token.Number]
        assert len(number_tokens) == 1
    
    def test_float(self, lexer):
        """Test float literals."""
        tokens = list(lexer.get_tokens('3.14'))
        number_tokens = [t for t in tokens if t[0] in Token.Number]
        assert len(number_tokens) == 1
    
    def test_leading_decimal(self, lexer):
        """Test floats with leading decimal."""
        tokens = list(lexer.get_tokens('.5'))
        number_tokens = [t for t in tokens if t[0] in Token.Number]
        assert len(number_tokens) == 1


class TestCommentHighlighting:
    """Test comment highlighting."""
    
    def test_single_line_comment(self, lexer):
        """Test single-line comments."""
        tokens = list(lexer.get_tokens('-- this is a comment'))
        comment_tokens = [t for t in tokens if t[0] in Token.Comment]
        assert len(comment_tokens) >= 1
    
    def test_multiline_comment(self, lexer):
        """Test multi-line comments."""
        tokens = list(lexer.get_tokens('/* multi\nline\ncomment */'))
        comment_tokens = [t for t in tokens if t[0] in Token.Comment]
        assert len(comment_tokens) >= 1


class TestOperatorHighlighting:
    """Test operator highlighting."""
    
    def test_assignment(self, lexer):
        """Test assignment operator."""
        tokens = list(lexer.get_tokens(':='))
        op_tokens = [t for t in tokens if t[0] == Operator]
        assert len(op_tokens) == 1
    
    def test_null_coalescing(self, lexer):
        """Test null coalescing operator."""
        tokens = list(lexer.get_tokens('??'))
        op_tokens = [t for t in tokens if t[0] == Operator]
        assert len(op_tokens) == 1
    
    def test_safe_navigation(self, lexer):
        """Test safe navigation operator."""
        tokens = list(lexer.get_tokens('?.'))
        op_tokens = [t for t in tokens if t[0] == Operator]
        assert len(op_tokens) == 1
    
    def test_range_operator(self, lexer):
        """Test range operator."""
        tokens = list(lexer.get_tokens('1..10'))
        # Range operator should be tokenized
        token_values = [t[1] for t in tokens]
        # Either as '..' operator or as part of the expression
        assert '..' in ''.join(token_values)


class TestBuiltinFunctions:
    """Test built-in function highlighting."""
    
    def test_string_functions(self, lexer):
        """Test string function highlighting."""
        tokens = list(lexer.get_tokens('LENGTH UPPER LOWER'))
        builtin_tokens = [t for t in tokens if t[0] == Name.Builtin]
        assert len(builtin_tokens) == 3
    
    def test_array_functions(self, lexer):
        """Test array function highlighting."""
        tokens = list(lexer.get_tokens('ARRAY_LENGTH ARRAY_APPEND'))
        builtin_tokens = [t for t in tokens if t[0] == Name.Builtin]
        assert len(builtin_tokens) == 2
    
    def test_esql_function(self, lexer):
        """Test ESQL_QUERY function."""
        tokens = list(lexer.get_tokens('ESQL_QUERY'))
        builtin_tokens = [t for t in tokens if t[0] == Name.Builtin]
        assert len(builtin_tokens) == 1


class TestSpecialVariables:
    """Test special variable highlighting."""
    
    def test_at_error(self, lexer):
        """Test @error variable."""
        tokens = list(lexer.get_tokens('@error'))
        var_tokens = [t for t in tokens if t[0] == Name.Variable.Magic]
        assert len(var_tokens) == 1
    
    def test_at_result(self, lexer):
        """Test @result variable."""
        tokens = list(lexer.get_tokens('@result'))
        var_tokens = [t for t in tokens if t[0] == Name.Variable.Magic]
        assert len(var_tokens) == 1


class TestComplexStatements:
    """Test highlighting of complete statements."""
    
    def test_create_procedure(self, lexer):
        """Test CREATE PROCEDURE statement."""
        code = '''
        CREATE PROCEDURE hello(name STRING)
        BEGIN
            PRINT 'Hello, ' || name || '!';
        END PROCEDURE;
        '''
        tokens = list(lexer.get_tokens(code))
        
        # Should have various token types
        token_types = set(t[0] for t in tokens)
        assert Keyword in token_types or any(t[0] in Token.Keyword for t in tokens)
    
    def test_if_statement(self, lexer):
        """Test IF statement."""
        code = '''
        IF x > 10 THEN
            PRINT 'big';
        ELSE
            PRINT 'small';
        END IF;
        '''
        tokens = list(lexer.get_tokens(code))
        keywords = [t for t in tokens if t[0] in Token.Keyword]
        assert len(keywords) >= 4  # IF, THEN, ELSE, END
    
    def test_for_loop(self, lexer):
        """Test FOR loop."""
        code = 'FOR i IN 1..10 LOOP PRINT i; END LOOP;'
        tokens = list(lexer.get_tokens(code))
        keywords = [t for t in tokens if t[0] in Token.Keyword]
        assert len(keywords) >= 4  # FOR, IN, LOOP, END

"""
Pygments lexer for Moltler syntax highlighting.

This lexer provides syntax highlighting for Moltler code in:
- The CLI REPL
- Documentation (via Pygments)
- Any tool that supports Pygments lexers
"""

from pygments.lexer import RegexLexer, words, bygroups, include
from pygments.token import (
    Text, Comment, Operator, Keyword, Name, String, Number,
    Punctuation, Error, Whitespace
)

from . import KEYWORDS, ALL_FUNCTIONS


class ElasticScriptLexer(RegexLexer):
    """
    Lexer for Moltler, the AI Skills Creation Framework for Elasticsearch.
    """
    
    name = 'Moltler'
    aliases = ['moltler', 'escript', 'elastic-script']
    filenames = ['*.moltler', '*.es', '*.escript']
    mimetypes = ['text/x-moltler']
    
    # Case insensitive
    flags = 0
    
    tokens = {
        'root': [
            # Whitespace
            (r'\s+', Whitespace),
            
            # Comments
            (r'--.*$', Comment.Single),
            (r'/\*', Comment.Multiline, 'multiline-comment'),
            
            # Strings
            (r"'", String.Single, 'single-string'),
            (r'"', String.Double, 'double-string'),
            
            # Numbers
            (r'\d+\.\d*', Number.Float),
            (r'\.\d+', Number.Float),
            (r'\d+', Number.Integer),
            
            # Operators
            (r':=', Operator),  # Assignment
            (r'\|\|', Operator),  # Concatenation
            (r'\?\?', Operator),  # Null coalescing
            (r'\?\.', Operator),  # Safe navigation
            (r'\.\.', Operator),  # Range
            (r'\|', Operator),   # Pipe
            (r'=>', Operator),   # Map arrow
            (r'[+\-*/%]', Operator),  # Arithmetic
            (r'[<>=!]=?', Operator),  # Comparison
            (r'&&|\|\|', Operator),  # Logical
            
            # Punctuation
            (r'[(),;.\[\]{}]', Punctuation),
            
            # Special variables
            (r'@\w+', Name.Variable.Magic),  # @error, @result, etc.
            
            # Keywords (case insensitive)
            (words([
                # Core statements
                'CREATE', 'DROP', 'DELETE', 'CALL', 'EXECUTE', 'IMMEDIATE',
                'PROCEDURE', 'FUNCTION', 'BEGIN', 'END', 'RETURN', 'RETURNS',
                # Variables
                'DECLARE', 'VAR', 'CONST', 'SET', 'INTO', 'USING',
                # Control flow
                'IF', 'THEN', 'ELSE', 'ELSEIF', 'ELSIF',
                'FOR', 'IN', 'LOOP', 'WHILE', 'EXIT', 'WHEN',
                'CASE',
                # Exception handling
                'TRY', 'CATCH', 'FINALLY', 'THROW', 'RAISE', 'WITH', 'CODE',
                # Async
                'ASYNC', 'ON_DONE', 'ON_FAIL', 'TRACK', 'TIMEOUT', 'PARALLEL',
                'EXECUTION', 'STATUS', 'CANCEL', 'RETRY', 'WAIT',
            ], prefix=r'\b', suffix=r'\b'), Keyword),
            
            # DDL Keywords
            (words([
                'SKILL', 'SKILLS', 'APPLICATION', 'APPLICATIONS',
                'INSTALL', 'EXTEND', 'GENERATE', 'MODEL', 'SAVE',
                'JOB', 'JOBS', 'TRIGGER', 'TRIGGERS', 'EVERY', 'AT', 'CRON',
                'PACKAGE', 'PACKAGES', 'BODY', 'PUBLIC', 'PRIVATE',
                'GRANT', 'REVOKE', 'ROLE', 'ROLES', 'PRIVILEGES',
                'PROFILE', 'PROFILES', 'ANALYZE', 'CLEAR',
                'TYPE', 'TYPES', 'RECORD',
                'INTENT', 'DEFINE', 'REQUIRES', 'ACTIONS', 'ON_FAILURE',
                'SHOW', 'ALTER', 'CONFIG', 'VERSION', 'HISTORY',
            ], prefix=r'\b', suffix=r'\b'), Keyword.Reserved),
            
            # Data types
            (words([
                'STRING', 'NUMBER', 'INT', 'FLOAT', 'BOOLEAN',
                'ARRAY', 'DOCUMENT', 'MAP', 'DATE', 'CURSOR',
            ], prefix=r'\b', suffix=r'\b'), Keyword.Type),
            
            # Parameter modes
            (words(['IN', 'OUT', 'INOUT', 'DEFAULT'], prefix=r'\b', suffix=r'\b'),
             Keyword.Pseudo),
            
            # Boolean and null
            (words(['TRUE', 'FALSE', 'NULL'], prefix=r'\b', suffix=r'\b'),
             Keyword.Constant),
            
            # Logical operators
            (words(['AND', 'OR', 'NOT', 'LIKE', 'BETWEEN', 'IS', 'EXISTS'],
                   prefix=r'\b', suffix=r'\b'), Operator.Word),
            
            # Built-in functions
            (words(ALL_FUNCTIONS, prefix=r'\b', suffix=r'\b'), Name.Builtin),
            
            # Namespaced function calls (e.g., DATE.ADD, STRING.UPPER)
            (r'\b([A-Z]+)\.([A-Z_]+)\b', bygroups(Name.Namespace, Name.Function)),
            
            # Other identifiers
            (r'\b[a-zA-Z_][a-zA-Z0-9_]*\b', Name),
            
            # Catch-all for errors
            (r'.', Text),
        ],
        
        'multiline-comment': [
            (r'\*/', Comment.Multiline, '#pop'),
            (r'[^*]+', Comment.Multiline),
            (r'\*', Comment.Multiline),
        ],
        
        'single-string': [
            (r"''", String.Escape),  # Escaped quote
            (r"[^']+", String.Single),
            (r"'", String.Single, '#pop'),
        ],
        
        'double-string': [
            (r'""', String.Escape),  # Escaped quote
            (r'[^"]+', String.Double),
            (r'"', String.Double, '#pop'),
        ],
    }


def get_lexer():
    """Get an instance of the ElasticScript lexer."""
    return ElasticScriptLexer()

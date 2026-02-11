"""
Auto-completion for Moltler CLI.

Provides:
- Keyword completion
- Built-in function completion
- Procedure/function name completion (from server)
- Smart context-aware completion
"""

from typing import List, Optional, Iterable
from prompt_toolkit.completion import Completer, Completion, CompleteEvent
from prompt_toolkit.document import Document

from . import KEYWORDS, ALL_FUNCTIONS, BUILTIN_FUNCTIONS


class ElasticScriptCompleter(Completer):
    """
    Auto-completer for elastic-script (Moltler).
    
    Provides completions for:
    - Keywords (CREATE, PROCEDURE, SKILL, CONNECTOR, AGENT, etc.)
    - Built-in functions (ESQL_QUERY, ARRAY_LENGTH, etc.)
    - User procedures (loaded from server)
    - User functions (loaded from server)
    - Skills (loaded from server)
    - Connectors (loaded from server)
    - Agents (loaded from server)
    """
    
    def __init__(self):
        self._procedures: List[str] = []
        self._functions: List[str] = []
        self._skills: List[str] = []
        self._connectors: List[str] = []
        self._agents: List[str] = []
        self._variables: List[str] = []
        
        # Build completion lists
        self._keywords = sorted(set(KEYWORDS))
        self._builtins = sorted(set(ALL_FUNCTIONS))
        
        # Moltler-specific keywords
        self._moltler_keywords = [
            # Skill keywords
            "SKILL", "SKILLS", "VERSION", "AUTHOR", "TAGS", "REQUIRES",
            "TEST", "GENERATE", "FROM", "AS",
            # Connector keywords
            "CONNECTOR", "CONNECTORS", "SYNC", "ENTITY", "FULL", "INCREMENTAL",
            "EXEC", "QUERY", "WHERE", "LIMIT", "ORDER", "BY", "ASC", "DESC",
            # Agent keywords
            "AGENT", "AGENTS", "GOAL", "TRIGGER", "MODEL", "EXECUTION",
            "AUTONOMOUS", "SUPERVISED", "DRY_RUN", "HUMAN_APPROVAL",
            "HISTORY", "ENABLE", "DISABLE",
            # Connector types
            "GITHUB", "JIRA", "DATADOG", "PAGERDUTY", "SLACK",
        ]
        self._keywords = sorted(set(self._keywords + self._moltler_keywords))
        
        # Namespace completions
        self._namespaces = [
            "STRING", "NUMBER", "ARRAY", "DATE", "DOCUMENT", "MAP",
            "ESQL", "LLM", "INFERENCE", "SLACK", "AWS", "K8S",
            "PAGERDUTY", "TF_CLOUD", "GITHUB", "GITLAB", "JENKINS", "S3", "HTTP"
        ]
    
    def update_connectors(self, connectors: List[str]):
        """Update the list of known connectors."""
        self._connectors = sorted(connectors)
    
    def update_agents(self, agents: List[str]):
        """Update the list of known agents."""
        self._agents = sorted(agents)
    
    def update_procedures(self, procedures: List[str]):
        """Update the list of known procedures."""
        self._procedures = sorted(procedures)
    
    def update_functions(self, functions: List[str]):
        """Update the list of known user functions."""
        self._functions = sorted(functions)
    
    def update_skills(self, skills: List[str]):
        """Update the list of known skills."""
        self._skills = sorted(skills)
    
    def add_variable(self, name: str):
        """Add a variable to completions."""
        if name not in self._variables:
            self._variables.append(name)
    
    def get_completions(
        self, document: Document, complete_event: CompleteEvent
    ) -> Iterable[Completion]:
        """Get completions for the current input."""
        
        text_before = document.text_before_cursor
        word = document.get_word_before_cursor(WORD=True).upper()
        
        if not word:
            return
        
        # Check context for smarter completions
        context = self._get_context(text_before)
        
        # Generate completions based on context
        if context == "after_call":
            # After CALL, suggest procedures
            yield from self._complete_from_list(word, self._procedures, "procedure")
        
        elif context == "after_create":
            # After CREATE, suggest object types
            for kw in ["PROCEDURE", "FUNCTION", "SKILL", "APPLICATION", "JOB", "TRIGGER", "TYPE", "PACKAGE", "ROLE", "CONNECTOR", "AGENT"]:
                if kw.startswith(word):
                    yield Completion(kw, start_position=-len(word), display_meta="keyword")
        
        elif context == "after_drop" or context == "after_delete":
            # After DROP/DELETE, suggest object types
            for kw in ["PROCEDURE", "FUNCTION", "SKILL", "APPLICATION", "JOB", "TRIGGER", "TYPE", "PACKAGE", "ROLE", "CONNECTOR", "AGENT"]:
                if kw.startswith(word):
                    yield Completion(kw, start_position=-len(word), display_meta="keyword")
        
        elif context == "after_show":
            # After SHOW, suggest showable things
            for kw in ["PROCEDURES", "FUNCTIONS", "SKILLS", "APPLICATIONS", "JOBS", "TRIGGERS", "TYPES", "PACKAGES", "ROLES", "PROFILES", "PROCEDURE", "FUNCTION", "SKILL", "APPLICATION", "CONNECTORS", "CONNECTOR", "AGENTS", "AGENT"]:
                if kw.startswith(word):
                    yield Completion(kw, start_position=-len(word), display_meta="keyword")
        
        elif context == "after_sync":
            # After SYNC, suggest CONNECTOR
            for kw in ["CONNECTOR"]:
                if kw.startswith(word):
                    yield Completion(kw, start_position=-len(word), display_meta="keyword")
            # Also suggest known connectors
            yield from self._complete_from_list(word, self._connectors, "connector")
        
        elif context == "after_test":
            # After TEST, suggest CONNECTOR or SKILL
            for kw in ["CONNECTOR", "SKILL"]:
                if kw.startswith(word):
                    yield Completion(kw, start_position=-len(word), display_meta="keyword")
        
        elif context == "after_trigger":
            # After TRIGGER, suggest AGENT
            for kw in ["AGENT"]:
                if kw.startswith(word):
                    yield Completion(kw, start_position=-len(word), display_meta="keyword")
            # Also suggest known agents
            yield from self._complete_from_list(word, self._agents, "agent")
        
        elif context == "after_exec":
            # After EXEC, suggest connectors
            yield from self._complete_from_list(word, self._connectors, "connector")
        
        elif context == "after_query":
            # After QUERY, suggest connectors
            yield from self._complete_from_list(word, self._connectors, "connector")
        
        elif context == "after_generate":
            # After GENERATE, suggest SKILL
            for kw in ["SKILL"]:
                if kw.startswith(word):
                    yield Completion(kw, start_position=-len(word), display_meta="keyword")
        
        elif context == "after_dot":
            # After namespace., suggest methods
            namespace = self._get_namespace(text_before)
            if namespace:
                yield from self._complete_namespace_methods(word, namespace)
        
        else:
            # General completions
            yield from self._complete_from_list(word, self._keywords, "keyword")
            yield from self._complete_from_list(word, self._builtins, "function")
            yield from self._complete_from_list(word, self._procedures, "procedure")
            yield from self._complete_from_list(word, self._functions, "user func")
            yield from self._complete_from_list(word, self._skills, "skill")
            yield from self._complete_from_list(word, self._connectors, "connector")
            yield from self._complete_from_list(word, self._agents, "agent")
            yield from self._complete_from_list(word, self._namespaces, "namespace")
            
            # Variable completions (with @)
            if word.startswith("@"):
                for var in ["@error", "@result", "@results"]:
                    if var.upper().startswith(word):
                        yield Completion(var, start_position=-len(word), display_meta="variable")
    
    def _get_context(self, text: str) -> Optional[str]:
        """Determine the completion context from preceding text."""
        text = text.upper().strip()
        
        if text.endswith("CALL ") or text.endswith("CALL"):
            return "after_call"
        if text.endswith("CREATE ") or text.endswith("CREATE"):
            return "after_create"
        if text.endswith("DROP ") or text.endswith("DROP"):
            return "after_drop"
        if text.endswith("DELETE ") or text.endswith("DELETE"):
            return "after_delete"
        if text.endswith("SHOW ") or text.endswith("SHOW"):
            return "after_show"
        if text.endswith("SYNC ") or text.endswith("SYNC"):
            return "after_sync"
        if text.endswith("TEST ") or text.endswith("TEST"):
            return "after_test"
        if text.endswith("TRIGGER ") or text.endswith("TRIGGER"):
            return "after_trigger"
        if text.endswith("EXEC ") or text.endswith("EXEC"):
            return "after_exec"
        if text.endswith("QUERY ") or text.endswith("QUERY"):
            return "after_query"
        if text.endswith("GENERATE ") or text.endswith("GENERATE"):
            return "after_generate"
        if text.endswith("."):
            return "after_dot"
        
        return None
    
    def _get_namespace(self, text: str) -> Optional[str]:
        """Extract namespace from text like 'STRING.'"""
        text = text.upper().strip()
        for ns in self._namespaces:
            if text.endswith(f"{ns}."):
                return ns
        return None
    
    def _complete_namespace_methods(self, word: str, namespace: str) -> Iterable[Completion]:
        """Complete methods for a namespace."""
        # Map namespace to function category
        ns_to_category = {
            "STRING": "string",
            "NUMBER": "number",
            "ARRAY": "array",
            "DATE": "date",
            "DOCUMENT": "document",
            "MAP": "map",
        }
        
        category = ns_to_category.get(namespace)
        if category and category in BUILTIN_FUNCTIONS:
            # Get method names (remove prefix)
            prefix = namespace + "_"
            for func in BUILTIN_FUNCTIONS[category]:
                method = func.replace(prefix, "").replace(category.upper() + "_", "")
                if method.startswith(word) or func.startswith(word):
                    yield Completion(
                        method,
                        start_position=-len(word),
                        display_meta="method"
                    )
    
    def _complete_from_list(
        self, word: str, items: List[str], meta: str
    ) -> Iterable[Completion]:
        """Generate completions from a list of items."""
        word_upper = word.upper()
        for item in items:
            if item.upper().startswith(word_upper):
                yield Completion(
                    item,
                    start_position=-len(word),
                    display_meta=meta
                )

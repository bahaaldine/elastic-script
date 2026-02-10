"""
elastic-script CLI - A beautiful command-line interface for elastic-script.

Features:
- Syntax highlighting for elastic-script
- Auto-completion for keywords and functions
- Multi-line editing with smart indentation
- Beautiful output with tables and panels
- Persistent command history
- Script execution mode
"""

__version__ = "0.1.0"
__author__ = "Bahaaldine Azarmi"

# elastic-script keywords for highlighting and completion
KEYWORDS = [
    # Core
    "CREATE", "DROP", "DELETE", "CALL", "EXECUTE", "IMMEDIATE",
    "PROCEDURE", "FUNCTION", "BEGIN", "END", "RETURN", "RETURNS",
    # Variables
    "DECLARE", "VAR", "CONST", "SET", "INTO", "USING",
    # Types
    "STRING", "NUMBER", "INT", "FLOAT", "BOOLEAN", "ARRAY", "DOCUMENT", "MAP", "DATE",
    # Control flow
    "IF", "THEN", "ELSE", "ELSEIF", "END IF", "ELSIF",
    "FOR", "IN", "LOOP", "END LOOP", "WHILE", "EXIT", "WHEN",
    "CASE", "END CASE",
    # Exception handling
    "TRY", "CATCH", "FINALLY", "THROW", "RAISE", "WITH", "CODE",
    # Parameters
    "IN", "OUT", "INOUT", "DEFAULT",
    # Async
    "ASYNC", "ON_DONE", "ON_FAIL", "TRACK", "TIMEOUT", "PARALLEL",
    "EXECUTION", "STATUS", "CANCEL", "RETRY", "WAIT",
    # Skills & Applications
    "SKILL", "SKILLS", "APPLICATION", "APPLICATIONS",
    "INSTALL", "EXTEND", "GENERATE", "FROM", "MODEL", "SAVE", "AS",
    "DESCRIPTION", "EXAMPLES", "SOURCES", "INTENTS",
    "PAUSE", "RESUME", "ENABLE", "DISABLE",
    # Jobs & Triggers
    "JOB", "JOBS", "TRIGGER", "TRIGGERS", "EVERY", "AT", "CRON",
    # Packages
    "PACKAGE", "PACKAGES", "BODY", "PUBLIC", "PRIVATE",
    # Security
    "GRANT", "REVOKE", "ROLE", "ROLES", "TO", "ON", "ALL", "PRIVILEGES",
    # Profiling
    "PROFILE", "PROFILES", "ANALYZE", "CLEAR",
    # Types
    "TYPE", "TYPES", "RECORD",
    # Intents
    "INTENT", "DEFINE", "REQUIRES", "ACTIONS", "ON_FAILURE",
    # Misc
    "SHOW", "ALTER", "CONFIG", "VERSION", "HISTORY", "LIMIT",
    "PRINT", "NULL", "TRUE", "FALSE", "AND", "OR", "NOT",
    "LIKE", "BETWEEN", "IS", "EXISTS",
]

# Built-in functions organized by category
BUILTIN_FUNCTIONS = {
    "string": [
        "LENGTH", "SUBSTR", "UPPER", "LOWER", "TRIM", "LTRIM", "RTRIM",
        "REPLACE", "INSTR", "LPAD", "RPAD", "SPLIT", "CONCAT",
        "REGEXP_REPLACE", "REGEXP_SUBSTR", "REVERSE", "INITCAP", "ENV",
    ],
    "number": [
        "ABS", "CEIL", "FLOOR", "ROUND", "TRUNC", "MOD",
        "POWER", "SQRT", "EXP", "LOG", "SIGN",
    ],
    "array": [
        "ARRAY_LENGTH", "ARRAY_APPEND", "ARRAY_PREPEND", "ARRAY_REMOVE",
        "ARRAY_CONTAINS", "ARRAY_DISTINCT", "ARRAY_JOIN", "ARRAY_FLATTEN",
        "ARRAY_REVERSE", "ARRAY_SLICE", "ARRAY_MAP", "ARRAY_FILTER",
        "ARRAY_REDUCE", "ARRAY_FIND", "ARRAY_FIND_INDEX",
        "ARRAY_EVERY", "ARRAY_SOME", "ARRAY_SORT",
    ],
    "date": [
        "CURRENT_DATE", "CURRENT_TIMESTAMP",
        "DATE_ADD", "DATE_SUB", "DATE_DIFF",
        "EXTRACT_YEAR", "EXTRACT_MONTH", "EXTRACT_DAY",
    ],
    "document": [
        "DOCUMENT_GET", "DOCUMENT_KEYS", "DOCUMENT_VALUES",
        "DOCUMENT_CONTAINS", "DOCUMENT_MERGE", "DOCUMENT_REMOVE",
    ],
    "map": [
        "MAP_GET", "MAP_PUT", "MAP_REMOVE", "MAP_KEYS", "MAP_VALUES",
        "MAP_CONTAINS", "MAP_SIZE", "MAP_MERGE",
    ],
    "elasticsearch": [
        "ESQL_QUERY", "INDEX_DOCUMENT", "INDEX_BULK", "UPDATE_DOCUMENT",
        "GET_DOCUMENT", "REFRESH_INDEX",
    ],
    "openai": [
        "LLM_COMPLETE", "LLM_CHAT", "LLM_EMBED",
        "LLM_SUMMARIZE", "LLM_CLASSIFY", "LLM_EXTRACT",
    ],
    "inference": [
        "INFERENCE_CREATE_ENDPOINT", "INFERENCE_DELETE_ENDPOINT",
        "INFERENCE_LIST_ENDPOINTS", "INFERENCE_GET_ENDPOINT",
        "INFERENCE", "INFERENCE_CHAT", "INFERENCE_EMBED", "INFERENCE_RERANK",
    ],
    "introspection": [
        "ESCRIPT_FUNCTIONS", "ESCRIPT_FUNCTION",
        "ESCRIPT_PROCEDURES", "ESCRIPT_PROCEDURE",
        "ESCRIPT_VARIABLES", "ESCRIPT_CAPABILITIES",
        "ESCRIPT_INTENTS", "ESCRIPT_INTENT",
    ],
    "integrations": [
        # Slack
        "SLACK_SEND", "SLACK_SEND_BLOCKS", "SLACK_WEBHOOK",
        "SLACK_LIST_CHANNELS", "SLACK_POST_REACTION",
        # AWS
        "AWS_LAMBDA_INVOKE", "AWS_SSM_RUN", "AWS_SSM_STATUS",
        "AWS_ASG_DESCRIBE", "AWS_ASG_SET_CAPACITY",
        # Kubernetes
        "K8S_GET", "K8S_PATCH", "K8S_SCALE",
        # PagerDuty
        "PAGERDUTY_TRIGGER", "PAGERDUTY_ACKNOWLEDGE", "PAGERDUTY_RESOLVE",
        "PAGERDUTY_GET_INCIDENT", "PAGERDUTY_LIST_INCIDENTS", "PAGERDUTY_ADD_NOTE",
        # Terraform
        "TF_CLOUD_RUN", "TF_CLOUD_STATUS", "TF_CLOUD_WAIT",
        "TF_CLOUD_OUTPUTS", "TF_CLOUD_CANCEL", "TF_CLOUD_LIST_WORKSPACES",
        # CI/CD
        "GITHUB_WORKFLOW", "GITHUB_WORKFLOW_STATUS",
        "GITLAB_PIPELINE", "GITLAB_PIPELINE_STATUS",
        "JENKINS_BUILD", "JENKINS_STATUS",
        # S3
        "S3_GET", "S3_PUT", "S3_LIST",
        # HTTP
        "HTTP_GET", "HTTP_POST", "WEBHOOK",
    ],
}

# Flatten all functions for completion
ALL_FUNCTIONS = []
for category, funcs in BUILTIN_FUNCTIONS.items():
    ALL_FUNCTIONS.extend(funcs)

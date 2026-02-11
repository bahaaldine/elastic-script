"""
Moltler SDK Exceptions.

Custom exception classes for the Moltler SDK.
"""


class MoltlerError(Exception):
    """Base exception for all Moltler errors."""
    
    def __init__(self, message: str, details: dict = None):
        super().__init__(message)
        self.message = message
        self.details = details or {}


class ConnectionError(MoltlerError):
    """Raised when connection to Elasticsearch fails."""
    pass


class AuthenticationError(MoltlerError):
    """Raised when authentication fails."""
    pass


class SkillNotFoundError(MoltlerError):
    """Raised when a skill is not found."""
    
    def __init__(self, skill_name: str):
        super().__init__(f"Skill not found: {skill_name}")
        self.skill_name = skill_name


class ConnectorNotFoundError(MoltlerError):
    """Raised when a connector is not found."""
    
    def __init__(self, connector_name: str):
        super().__init__(f"Connector not found: {connector_name}")
        self.connector_name = connector_name


class AgentNotFoundError(MoltlerError):
    """Raised when an agent is not found."""
    
    def __init__(self, agent_name: str):
        super().__init__(f"Agent not found: {agent_name}")
        self.agent_name = agent_name


class ExecutionError(MoltlerError):
    """Raised when script execution fails."""
    
    def __init__(self, message: str, statement: str = None, details: dict = None):
        super().__init__(message, details)
        self.statement = statement


class ValidationError(MoltlerError):
    """Raised when validation fails."""
    pass


class SyncError(MoltlerError):
    """Raised when connector sync fails."""
    
    def __init__(self, connector_name: str, message: str, details: dict = None):
        super().__init__(message, details)
        self.connector_name = connector_name


class TestFailedError(MoltlerError):
    """Raised when a skill test fails."""
    
    def __init__(self, skill_name: str, expected: any, actual: any):
        message = f"Test failed for skill '{skill_name}': expected {expected}, got {actual}"
        super().__init__(message)
        self.skill_name = skill_name
        self.expected = expected
        self.actual = actual

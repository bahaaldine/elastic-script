"""
Moltler Python SDK

A high-level Python SDK for the Moltler AI Skills Creation Framework.

Example usage:

    from moltler import Moltler
    
    # Connect to Moltler
    client = Moltler(host="localhost", port=9200, username="elastic-admin", password="...")
    
    # Create a skill
    skill = client.skills.create(
        name="analyze_logs",
        version="1.0.0",
        description="Analyze logs for anomalies",
        body="..."
    )
    
    # List connectors
    connectors = client.connectors.list()
    
    # Trigger an agent
    result = client.agents.trigger("my_agent", context={"priority": "high"})
"""

__version__ = "0.1.0"
__author__ = "Moltler Team"

from .client import Moltler
from .skills import Skill, SkillBuilder
from .connectors import Connector, ConnectorConfig
from .agents import Agent, AgentExecutionResult
from .exceptions import (
    MoltlerError,
    ConnectionError,
    AuthenticationError,
    SkillNotFoundError,
    ConnectorNotFoundError,
    AgentNotFoundError,
    ExecutionError,
)

__all__ = [
    "Moltler",
    "Skill",
    "SkillBuilder", 
    "Connector",
    "ConnectorConfig",
    "Agent",
    "AgentExecutionResult",
    "MoltlerError",
    "ConnectionError",
    "AuthenticationError",
    "SkillNotFoundError",
    "ConnectorNotFoundError",
    "AgentNotFoundError",
    "ExecutionError",
]

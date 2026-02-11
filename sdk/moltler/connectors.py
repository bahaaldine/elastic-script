"""
Moltler Connectors Management.

Provides APIs for creating, managing, and using connectors.
"""

from typing import List, Optional, Dict, Any
from dataclasses import dataclass, field
from enum import Enum

from .exceptions import ConnectorNotFoundError, ExecutionError, SyncError


class ConnectorType(Enum):
    """Supported connector types."""
    GITHUB = "github"
    JIRA = "jira"
    DATADOG = "datadog"
    PAGERDUTY = "pagerduty"
    SLACK = "slack"
    AWS = "aws"
    KUBERNETES = "kubernetes"
    GENERIC_HTTP = "http"


@dataclass
class ConnectorConfig:
    """Configuration for a connector."""
    type: str
    url: str = None
    token: str = None
    api_key: str = None
    username: str = None
    password: str = None
    project: str = None
    organization: str = None
    site: str = None
    extra: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        config = {}
        if self.url:
            config["url"] = self.url
        if self.token:
            config["token"] = self.token
        if self.api_key:
            config["api_key"] = self.api_key
        if self.username:
            config["username"] = self.username
        if self.password:
            config["password"] = self.password
        if self.project:
            config["project"] = self.project
        if self.organization:
            config["organization"] = self.organization
        if self.site:
            config["site"] = self.site
        config.update(self.extra)
        return config


@dataclass 
class SyncState:
    """State of a connector sync operation."""
    connector: str
    entity: str = None
    status: str = None
    last_sync: int = None
    docs_synced: int = 0
    target_index: str = None
    error: str = None


@dataclass
class Connector:
    """Represents a Moltler connector."""
    name: str
    type: str
    config: Dict[str, Any] = field(default_factory=dict)
    enabled: bool = True
    description: str = None
    target_index: str = None
    sync_schedule: str = None
    created_at: int = None
    updated_at: int = None
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "Connector":
        """Create a Connector from a dictionary."""
        return cls(
            name=data.get("name"),
            type=data.get("type"),
            config=data.get("config", {}),
            enabled=data.get("enabled", True),
            description=data.get("description"),
            target_index=data.get("target_index"),
            sync_schedule=data.get("sync_schedule"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )


class ConnectorManager:
    """
    Manager for connector operations.
    
    Accessed via `client.connectors`.
    """
    
    def __init__(self, client):
        self._client = client
    
    def list(self) -> List[Connector]:
        """
        List all configured connectors.
        
        Returns:
            List of Connector objects
        """
        result = self._client.execute("SHOW CONNECTORS")
        if not result.success:
            return []
        
        connectors = []
        data = result.data
        if isinstance(data, dict) and "connectors" in data:
            for conn_data in data["connectors"]:
                connectors.append(Connector.from_dict(conn_data))
        
        return connectors
    
    def get(self, name: str) -> Connector:
        """
        Get a connector by name.
        
        Args:
            name: Connector name
            
        Returns:
            Connector object
            
        Raises:
            ConnectorNotFoundError: If connector not found
        """
        result = self._client.execute(f"SHOW CONNECTOR {name}")
        if not result.success:
            raise ConnectorNotFoundError(name)
        
        return Connector.from_dict(result.data)
    
    def create(
        self,
        name: str,
        connector_type: str,
        config: ConnectorConfig = None,
        description: str = None,
        target_index: str = None,
        sync_schedule: str = None,
    ) -> Connector:
        """
        Create a new connector.
        
        Args:
            name: Connector name
            connector_type: Type (github, jira, datadog, etc.)
            config: ConnectorConfig or dict with credentials
            description: Optional description
            target_index: Optional target index for synced data
            sync_schedule: Optional sync schedule (e.g., "every 5 minutes")
            
        Returns:
            Created Connector object
        """
        stmt = f"CREATE CONNECTOR {name} TYPE '{connector_type}'"
        
        if description:
            stmt += f" DESCRIPTION '{_escape(description)}'"
        
        if target_index:
            stmt += f" INTO '{target_index}'"
        
        if sync_schedule:
            stmt += f" SCHEDULE '{sync_schedule}'"
        
        # Add config as JSON-like document
        if config:
            config_dict = config.to_dict() if isinstance(config, ConnectorConfig) else config
            config_str = _dict_to_doc(config_dict)
            stmt += f" CONFIG {config_str}"
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Failed to create connector", statement=stmt)
        
        return self.get(name)
    
    def drop(self, name: str) -> bool:
        """
        Drop a connector.
        
        Args:
            name: Connector name
            
        Returns:
            True if successful
        """
        result = self._client.execute(f"DROP CONNECTOR {name}")
        return result.success
    
    def test(self, name: str) -> Dict[str, Any]:
        """
        Test connectivity to a connector's external service.
        
        Args:
            name: Connector name
            
        Returns:
            Test result dictionary
        """
        result = self._client.execute(f"TEST CONNECTOR {name}")
        if not result.success:
            raise ExecutionError(result.error or "Test failed")
        
        return result.data
    
    def sync(
        self,
        name: str,
        entity: str = None,
        full: bool = False
    ) -> SyncState:
        """
        Sync data from a connector into Elasticsearch.
        
        Args:
            name: Connector name
            entity: Optional specific entity to sync (e.g., "issues", "repos")
            full: If True, perform full sync ignoring incremental state
            
        Returns:
            SyncState with sync results
            
        Raises:
            SyncError: If sync fails
        """
        stmt = f"SYNC CONNECTOR {name}"
        if entity:
            stmt += f" ENTITY '{entity}'"
        if full:
            stmt += " FULL"
        
        result = self._client.execute(stmt)
        if not result.success:
            raise SyncError(name, result.error or "Sync failed")
        
        data = result.data if isinstance(result.data, dict) else {}
        return SyncState(
            connector=name,
            entity=entity,
            status=data.get("status", "completed"),
            last_sync=data.get("last_sync"),
            docs_synced=data.get("docs_synced", 0),
            target_index=data.get("target_index"),
        )
    
    def enable(self, name: str) -> bool:
        """
        Enable a connector.
        
        Args:
            name: Connector name
            
        Returns:
            True if successful
        """
        result = self._client.execute(f"ENABLE CONNECTOR {name}")
        return result.success
    
    def disable(self, name: str) -> bool:
        """
        Disable a connector.
        
        Args:
            name: Connector name
            
        Returns:
            True if successful
        """
        result = self._client.execute(f"DISABLE CONNECTOR {name}")
        return result.success
    
    def exec(self, name: str, action: str, **kwargs) -> Any:
        """
        Execute an action on a connector.
        
        Args:
            name: Connector name
            action: Action name (e.g., "create_issue", "list_repos")
            **kwargs: Action parameters
            
        Returns:
            Action result
            
        Example:
            >>> client.connectors.exec("my_github", "create_issue",
            ...     owner="org", repo="repo", title="Bug fix")
        """
        if kwargs:
            args_str = ", ".join(f"{k} = '{v}'" for k, v in kwargs.items())
            stmt = f"EXEC {name}.{action}({args_str})"
        else:
            stmt = f"EXEC {name}.{action}()"
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Action failed", statement=stmt)
        
        return result.data
    
    def query(
        self,
        name: str,
        entity: str,
        where: str = None,
        limit: int = None,
        order_by: str = None,
        ascending: bool = True,
    ) -> List[Dict[str, Any]]:
        """
        Query entities from a connector.
        
        Args:
            name: Connector name
            entity: Entity type (e.g., "issues", "repos", "monitors")
            where: Optional filter expression
            limit: Optional result limit
            order_by: Optional sort field
            ascending: Sort order (default True)
            
        Returns:
            List of matching entities
            
        Example:
            >>> issues = client.connectors.query("my_jira", "issues",
            ...     where="status = 'Open'", limit=50)
        """
        stmt = f"QUERY {name}.{entity}"
        
        if where:
            stmt += f" WHERE {where}"
        
        if limit:
            stmt += f" LIMIT {limit}"
        
        if order_by:
            stmt += f" ORDER BY {order_by}"
            stmt += " ASC" if ascending else " DESC"
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Query failed", statement=stmt)
        
        if isinstance(result.data, list):
            return result.data
        elif isinstance(result.data, dict) and "results" in result.data:
            return result.data["results"]
        else:
            return [result.data] if result.data else []
    
    def get_sync_history(self, name: str, limit: int = 10) -> List[SyncState]:
        """
        Get sync history for a connector.
        
        Args:
            name: Connector name
            limit: Number of records to return
            
        Returns:
            List of SyncState objects
        """
        result = self._client.execute(f"SHOW CONNECTOR {name} SYNC HISTORY LIMIT {limit}")
        if not result.success:
            return []
        
        history = []
        data = result.data
        if isinstance(data, dict) and "history" in data:
            for h in data["history"]:
                history.append(SyncState(
                    connector=name,
                    entity=h.get("entity"),
                    status=h.get("status"),
                    last_sync=h.get("timestamp"),
                    docs_synced=h.get("docs_synced", 0),
                    target_index=h.get("target_index"),
                    error=h.get("error"),
                ))
        
        return history


def _escape(s: str) -> str:
    """Escape single quotes in a string."""
    return s.replace("'", "''")


def _dict_to_doc(d: Dict[str, Any]) -> str:
    """Convert a dictionary to a document literal string."""
    pairs = []
    for k, v in d.items():
        if isinstance(v, str):
            pairs.append(f'"{k}": "{v}"')
        elif isinstance(v, bool):
            pairs.append(f'"{k}": {"true" if v else "false"}')
        elif v is None:
            pairs.append(f'"{k}": null')
        else:
            pairs.append(f'"{k}": {v}')
    return "{" + ", ".join(pairs) + "}"

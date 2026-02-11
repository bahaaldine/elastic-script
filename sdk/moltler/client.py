"""
Moltler Client.

The main entry point for interacting with the Moltler framework.
"""

import os
from typing import Optional, Dict, Any
from dataclasses import dataclass

import requests
from requests.auth import HTTPBasicAuth

from .skills import SkillManager
from .connectors import ConnectorManager
from .agents import AgentManager
from .exceptions import ConnectionError, AuthenticationError, ExecutionError


@dataclass
class ExecutionResult:
    """Result of a Moltler execution."""
    success: bool
    data: Any
    error: Optional[str] = None
    execution_id: Optional[str] = None
    output: Optional[list] = None
    elapsed_ms: Optional[float] = None


class Moltler:
    """
    Moltler Client for AI Skills Creation Framework.
    
    Provides high-level APIs for managing skills, connectors, and agents.
    
    Example:
        >>> from moltler import Moltler
        >>> client = Moltler(host="localhost", port=9200)
        >>> 
        >>> # List skills
        >>> for skill in client.skills.list():
        ...     print(skill.name, skill.version)
        >>> 
        >>> # Execute a skill
        >>> result = client.skills.execute("analyze_logs", data={"index": "logs-*"})
        >>> 
        >>> # Trigger an agent
        >>> execution = client.agents.trigger("incident_responder")
    """
    
    def __init__(
        self,
        host: str = None,
        port: int = None,
        username: str = None,
        password: str = None,
        use_ssl: bool = None,
        verify_certs: bool = True,
        timeout: int = 300,
    ):
        """
        Initialize the Moltler client.
        
        Args:
            host: Elasticsearch host (default: localhost or MOLTLER_HOST env)
            port: Elasticsearch port (default: 9200 or MOLTLER_PORT env)
            username: Username (default: MOLTLER_USERNAME env)
            password: Password (default: MOLTLER_PASSWORD env)
            use_ssl: Use SSL/TLS (default: True if port is 443)
            verify_certs: Verify SSL certificates (default: True)
            timeout: Request timeout in seconds (default: 300)
        """
        self.host = host or os.getenv("MOLTLER_HOST", "localhost")
        self.port = port or int(os.getenv("MOLTLER_PORT", "9200"))
        self.username = username or os.getenv("MOLTLER_USERNAME", "elastic-admin")
        self.password = password or os.getenv("MOLTLER_PASSWORD", "elastic-password")
        
        if use_ssl is not None:
            self.use_ssl = use_ssl
        else:
            ssl_env = os.getenv("MOLTLER_USE_SSL", "").lower()
            self.use_ssl = ssl_env in ("true", "1", "yes") or self.port == 443
        
        self.verify_certs = verify_certs
        self.timeout = timeout
        
        # Build base URL
        scheme = "https" if self.use_ssl else "http"
        self._base_url = f"{scheme}://{self.host}:{self.port}"
        
        # Create session
        self._session = requests.Session()
        self._session.auth = HTTPBasicAuth(self.username, self.password)
        self._session.verify = self.verify_certs
        
        # Initialize managers
        self._skills = SkillManager(self)
        self._connectors = ConnectorManager(self)
        self._agents = AgentManager(self)
        
        # Connection state
        self._connected = False
        self._cluster_name = None
        self._version = None
    
    @property
    def base_url(self) -> str:
        """Get the base URL for Elasticsearch."""
        return self._base_url
    
    @property
    def skills(self) -> "SkillManager":
        """Access the skill manager."""
        return self._skills
    
    @property
    def connectors(self) -> "ConnectorManager":
        """Access the connector manager."""
        return self._connectors
    
    @property
    def agents(self) -> "AgentManager":
        """Access the agent manager."""
        return self._agents
    
    def connect(self) -> bool:
        """
        Test connection to Elasticsearch and verify Moltler is available.
        
        Returns:
            True if connection successful
            
        Raises:
            ConnectionError: If connection fails
            AuthenticationError: If authentication fails
        """
        try:
            response = self._session.get(f"{self._base_url}/", timeout=5)
            
            if response.status_code == 401:
                raise AuthenticationError("Authentication failed - check credentials")
            
            if response.status_code != 200:
                raise ConnectionError(f"Connection failed: HTTP {response.status_code}")
            
            info = response.json()
            self._cluster_name = info.get("cluster_name", "unknown")
            self._version = info.get("version", {}).get("number", "unknown")
            self._connected = True
            
            return True
            
        except requests.exceptions.ConnectionError:
            raise ConnectionError(f"Cannot connect to {self._base_url}")
        except requests.exceptions.Timeout:
            raise ConnectionError("Connection timeout")
    
    @property
    def connected(self) -> bool:
        """Check if connected to Elasticsearch."""
        return self._connected
    
    @property
    def cluster_name(self) -> Optional[str]:
        """Get the cluster name (after connection)."""
        return self._cluster_name
    
    @property
    def version(self) -> Optional[str]:
        """Get the Elasticsearch version (after connection)."""
        return self._version
    
    def execute(self, statement: str, args: Dict[str, Any] = None) -> ExecutionResult:
        """
        Execute a Moltler/elastic-script statement.
        
        Args:
            statement: The statement to execute
            args: Optional arguments for procedure/skill calls
            
        Returns:
            ExecutionResult with success status and data/error
            
        Raises:
            ExecutionError: If execution fails
        """
        try:
            payload = {"query": statement}
            if args:
                payload["args"] = args
            
            response = self._session.post(
                f"{self._base_url}/_escript",
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=self.timeout,
            )
            
            if response.status_code == 200:
                data = response.json()
                if isinstance(data, dict):
                    return ExecutionResult(
                        success=True,
                        data=data.get("result", data),
                        execution_id=data.get("execution_id"),
                        output=data.get("output"),
                        elapsed_ms=data.get("elapsed_ms"),
                    )
                else:
                    return ExecutionResult(success=True, data=data)
            else:
                error_data = response.json() if response.text else {}
                if isinstance(error_data, dict):
                    error_field = error_data.get("error")
                    if isinstance(error_field, dict):
                        error_msg = error_field.get("reason", str(error_field))
                    elif isinstance(error_field, str):
                        error_msg = error_field
                    else:
                        error_msg = error_data.get("message") or f"HTTP {response.status_code}"
                else:
                    error_msg = str(error_data) or f"HTTP {response.status_code}"
                
                return ExecutionResult(success=False, data=None, error=error_msg)
                
        except requests.exceptions.Timeout:
            raise ExecutionError("Query timeout", statement=statement)
        except requests.exceptions.ConnectionError:
            raise ExecutionError(f"Lost connection to {self._base_url}", statement=statement)
    
    def raw_query(self, method: str, path: str, body: Dict = None) -> Dict:
        """
        Execute a raw Elasticsearch query.
        
        Args:
            method: HTTP method (GET, POST, PUT, DELETE)
            path: API path (e.g., "/_cat/indices")
            body: Request body (optional)
            
        Returns:
            Response data as dictionary
        """
        url = f"{self._base_url}{path}"
        response = self._session.request(method, url, json=body, timeout=self.timeout)
        return response.json()
    
    def close(self):
        """Close the client session."""
        self._session.close()
        self._connected = False
    
    def __enter__(self):
        """Context manager entry."""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()
        return False

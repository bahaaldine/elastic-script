"""
Elasticsearch client for elastic-script execution.

Handles:
- Connection management
- Query execution
- Error handling
"""

import json
from typing import Any, Dict, Optional, Tuple
from dataclasses import dataclass

import requests
from requests.auth import HTTPBasicAuth

from .config import Config


@dataclass
class ExecutionResult:
    """Result of an elastic-script execution."""
    success: bool
    data: Any
    error: Optional[str] = None
    execution_id: Optional[str] = None
    output: Optional[list] = None  # PRINT output
    elapsed_ms: Optional[float] = None


class ElasticScriptClient:
    """Client for executing elastic-script queries."""
    
    def __init__(self, config: Config):
        self.config = config
        self._session = requests.Session()
        self._session.auth = HTTPBasicAuth(config.username, config.password)
        self._session.verify = config.verify_certs
        self._connected = False
    
    @property
    def base_url(self) -> str:
        return self.config.url
    
    def test_connection(self) -> Tuple[bool, str]:
        """
        Test connection to Elasticsearch.
        
        Returns:
            Tuple of (success, message)
        """
        try:
            response = self._session.get(
                f"{self.base_url}/",
                timeout=5
            )
            if response.status_code == 200:
                info = response.json()
                version = info.get("version", {}).get("number", "unknown")
                cluster = info.get("cluster_name", "unknown")
                self._connected = True
                return True, f"Connected to {cluster} (Elasticsearch {version})"
            else:
                return False, f"Connection failed: HTTP {response.status_code}"
        except requests.exceptions.ConnectionError:
            return False, f"Cannot connect to {self.base_url}"
        except requests.exceptions.Timeout:
            return False, "Connection timeout"
        except Exception as e:
            return False, f"Connection error: {e}"
    
    def execute(self, query: str, args: Optional[Dict[str, Any]] = None) -> ExecutionResult:
        """
        Execute an elastic-script query.
        
        Args:
            query: The elastic-script code to execute
            args: Optional arguments for procedure calls
            
        Returns:
            ExecutionResult with success status and data/error
        """
        try:
            payload = {"query": query}
            if args:
                payload["args"] = args
            
            response = self._session.post(
                f"{self.base_url}/_escript",
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=300  # 5 minute timeout for long queries
            )
            
            if response.status_code == 200:
                data = response.json()
                # Handle both dict and non-dict responses
                if isinstance(data, dict):
                    return ExecutionResult(
                        success=True,
                        data=data.get("result", data),
                        execution_id=data.get("execution_id"),
                        output=data.get("output"),
                        elapsed_ms=data.get("elapsed_ms"),
                    )
                else:
                    # Simple response (string, number, etc.)
                    return ExecutionResult(
                        success=True,
                        data=data,
                    )
            else:
                error_data = response.json() if response.text else {}
                # Handle different error response formats
                if isinstance(error_data, dict):
                    error_field = error_data.get("error")
                    if isinstance(error_field, dict):
                        # Elasticsearch-style error: {"error": {"reason": "..."}}
                        error_msg = error_field.get("reason", str(error_field))
                    elif isinstance(error_field, str):
                        # Simple error: {"error": "message"}
                        error_msg = error_field
                    else:
                        error_msg = error_data.get("message") or f"HTTP {response.status_code}"
                else:
                    error_msg = str(error_data) or f"HTTP {response.status_code}"
                return ExecutionResult(
                    success=False,
                    data=None,
                    error=error_msg,
                )
        
        except requests.exceptions.Timeout:
            return ExecutionResult(
                success=False,
                data=None,
                error="Query timeout (exceeded 5 minutes)",
            )
        except requests.exceptions.ConnectionError:
            return ExecutionResult(
                success=False,
                data=None,
                error=f"Lost connection to {self.base_url}",
            )
        except json.JSONDecodeError as e:
            return ExecutionResult(
                success=False,
                data=None,
                error=f"Invalid response from server: {e}",
            )
        except Exception as e:
            return ExecutionResult(
                success=False,
                data=None,
                error=str(e),
            )
    
    def list_procedures(self) -> ExecutionResult:
        """List all stored procedures."""
        return self.execute("CALL ESCRIPT_PROCEDURES()")
    
    def list_functions(self) -> ExecutionResult:
        """List all stored functions."""
        return self.execute("CALL ESCRIPT_FUNCTIONS()")
    
    def list_skills(self) -> ExecutionResult:
        """List all standalone skills."""
        return self.execute("SHOW SKILLS")
    
    def list_connectors(self) -> ExecutionResult:
        """List all configured connectors."""
        return self.execute("SHOW CONNECTORS")
    
    def list_agents(self) -> ExecutionResult:
        """List all configured agents."""
        return self.execute("SHOW AGENTS")
    
    def get_skill(self, name: str) -> ExecutionResult:
        """Get details of a specific skill."""
        return self.execute(f"SHOW SKILL {name}")
    
    def get_connector(self, name: str) -> ExecutionResult:
        """Get details of a specific connector."""
        return self.execute(f"SHOW CONNECTOR {name}")
    
    def get_agent(self, name: str) -> ExecutionResult:
        """Get details of a specific agent."""
        return self.execute(f"SHOW AGENT {name}")
    
    def test_connector(self, name: str) -> ExecutionResult:
        """Test connectivity to a connector's external service."""
        return self.execute(f"TEST CONNECTOR {name}")
    
    def sync_connector(self, name: str, entity: Optional[str] = None, full: bool = False) -> ExecutionResult:
        """Sync data from a connector."""
        stmt = f"SYNC CONNECTOR {name}"
        if entity:
            stmt += f" ENTITY '{entity}'"
        if full:
            stmt += " FULL"
        return self.execute(stmt)
    
    def trigger_agent(self, name: str, context: Optional[Dict[str, Any]] = None) -> ExecutionResult:
        """Manually trigger an agent."""
        import json as json_lib
        stmt = f"TRIGGER AGENT {name}"
        if context:
            stmt += f" WITH {json_lib.dumps(context)}"
        return self.execute(stmt)
    
    def get_agent_history(self, name: str) -> ExecutionResult:
        """Get execution history for an agent."""
        return self.execute(f"SHOW AGENT {name} HISTORY")
    
    def enable_agent(self, name: str) -> ExecutionResult:
        """Enable an agent."""
        return self.execute(f"ENABLE AGENT {name}")
    
    def disable_agent(self, name: str) -> ExecutionResult:
        """Disable an agent."""
        return self.execute(f"DISABLE AGENT {name}")
    
    def test_skill(self, name: str, params: Optional[Dict[str, Any]] = None, 
                   expect: Optional[str] = None) -> ExecutionResult:
        """Test a skill with given parameters."""
        stmt = f"TEST SKILL {name}"
        if params:
            param_str = ", ".join(f"{k} = '{v}'" for k, v in params.items())
            stmt += f" WITH {param_str}"
        if expect:
            stmt += f" EXPECT {expect}"
        return self.execute(stmt)
    
    def generate_skill(self, goal: str, name: Optional[str] = None) -> ExecutionResult:
        """Generate a skill using AI based on a goal."""
        stmt = f"GENERATE SKILL FROM '{goal}'"
        if name:
            stmt += f" AS {name}"
        return self.execute(stmt)
    
    def exec_connector(self, connector: str, action: str, **kwargs) -> ExecutionResult:
        """Execute an action on a connector."""
        args_str = ", ".join(f"{k} = '{v}'" for k, v in kwargs.items())
        stmt = f"EXEC {connector}.{action}({args_str})"
        return self.execute(stmt)
    
    def query_connector(self, connector: str, entity: str, 
                       where: Optional[str] = None, 
                       limit: Optional[int] = None) -> ExecutionResult:
        """Query an entity from a connector."""
        stmt = f"QUERY {connector}.{entity}"
        if where:
            stmt += f" WHERE {where}"
        if limit:
            stmt += f" LIMIT {limit}"
        return self.execute(stmt)
    
    def get_cluster_info(self) -> Dict[str, Any]:
        """Get Elasticsearch cluster information."""
        try:
            response = self._session.get(f"{self.base_url}/", timeout=5)
            if response.status_code == 200:
                return response.json()
        except:
            pass
        return {}
    
    def close(self):
        """Close the client session."""
        self._session.close()

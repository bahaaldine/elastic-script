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
                return ExecutionResult(
                    success=True,
                    data=data.get("result", data),
                    execution_id=data.get("execution_id"),
                    output=data.get("output"),
                    elapsed_ms=data.get("elapsed_ms"),
                )
            else:
                error_data = response.json() if response.text else {}
                error_msg = (
                    error_data.get("error", {}).get("reason")
                    or error_data.get("message")
                    or f"HTTP {response.status_code}"
                )
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

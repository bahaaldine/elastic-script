"""
Elasticsearch client for skill testing.
"""

import os
import json
import requests
from typing import Any, Dict, List, Optional
from dataclasses import dataclass
import base64


@dataclass
class ESConfig:
    """Elasticsearch connection configuration."""
    url: str = "http://localhost:9200"
    username: str = "elastic-admin"
    password: str = "elastic-password"
    
    @property
    def auth_header(self) -> str:
        credentials = f"{self.username}:{self.password}"
        return base64.b64encode(credentials.encode()).decode()
    
    @classmethod
    def from_env(cls) -> 'ESConfig':
        return cls(
            url=os.getenv('ES_URL', 'http://localhost:9200'),
            username=os.getenv('ES_USERNAME', 'elastic-admin'),
            password=os.getenv('ES_PASSWORD', 'elastic-password')
        )


class ElasticsearchTestClient:
    """Client for interacting with Elasticsearch and running skills."""
    
    def __init__(self, config: Optional[ESConfig] = None):
        self.config = config or ESConfig.from_env()
        self.session = requests.Session()
        self.session.auth = (self.config.username, self.config.password)
        self.session.headers['Content-Type'] = 'application/json'
    
    def is_available(self) -> bool:
        """Check if Elasticsearch is available."""
        try:
            response = self.session.get(f"{self.config.url}/_cluster/health", timeout=5)
            return response.status_code == 200
        except Exception:
            return False
    
    def get_version(self) -> Optional[str]:
        """Get Elasticsearch version."""
        try:
            response = self.session.get(self.config.url, timeout=5)
            if response.status_code == 200:
                return response.json().get('version', {}).get('number')
        except Exception:
            pass
        return None
    
    def is_plugin_installed(self) -> bool:
        """Check if elastic-script plugin is installed."""
        try:
            response = self.session.get(f"{self.config.url}/_cat/plugins?format=json", timeout=5)
            if response.status_code == 200:
                plugins = response.json()
                return any(p.get('component') == 'elastic-script' for p in plugins)
        except Exception:
            pass
        return False
    
    def run_escript(self, query: str) -> Dict[str, Any]:
        """Execute an elastic-script query."""
        response = self.session.post(
            f"{self.config.url}/_escript",
            json={"query": query},
            timeout=30
        )
        return {
            'status_code': response.status_code,
            'response': response.json() if response.text else {},
            'success': response.status_code == 200
        }
    
    def install_skill(self, skill_sql: str) -> Dict[str, Any]:
        """Install a skill from SQL definition."""
        return self.run_escript(skill_sql)
    
    def run_skill(self, skill_name: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Run an installed skill with optional parameters."""
        if params:
            param_str = ", ".join(f"{k} = {self._format_param(v)}" for k, v in params.items())
            query = f"RUN SKILL {skill_name} WITH {param_str}"
        else:
            query = f"RUN SKILL {skill_name}()"
        return self.run_escript(query)
    
    def _format_param(self, value: Any) -> str:
        """Format a parameter value for the query."""
        if isinstance(value, str):
            return f"'{value}'"
        elif isinstance(value, bool):
            return str(value).upper()
        elif isinstance(value, (int, float)):
            return str(value)
        elif isinstance(value, list):
            return json.dumps(value)
        else:
            return f"'{str(value)}'"
    
    def create_index(self, index_name: str, mappings: Optional[Dict] = None, settings: Optional[Dict] = None) -> bool:
        """Create an index with optional mappings and settings."""
        body = {}
        if mappings:
            body['mappings'] = mappings
        if settings:
            body['settings'] = settings
        
        response = self.session.put(
            f"{self.config.url}/{index_name}",
            json=body if body else None,
            timeout=10
        )
        return response.status_code in (200, 201)
    
    def delete_index(self, index_name: str) -> bool:
        """Delete an index."""
        response = self.session.delete(
            f"{self.config.url}/{index_name}",
            timeout=10
        )
        return response.status_code in (200, 404)
    
    def bulk_index(self, index_name: str, documents: List[Dict]) -> bool:
        """Bulk index documents."""
        if not documents:
            return True
        
        bulk_body = ""
        for doc in documents:
            action = json.dumps({"index": {"_index": index_name}})
            doc_json = json.dumps(doc)
            bulk_body += f"{action}\n{doc_json}\n"
        
        response = self.session.post(
            f"{self.config.url}/_bulk",
            data=bulk_body,
            headers={'Content-Type': 'application/x-ndjson'},
            timeout=30
        )
        
        if response.status_code == 200:
            result = response.json()
            return not result.get('errors', False)
        return False
    
    def refresh_index(self, index_name: str) -> bool:
        """Refresh an index to make documents searchable."""
        response = self.session.post(
            f"{self.config.url}/{index_name}/_refresh",
            timeout=10
        )
        return response.status_code == 200
    
    def index_exists(self, index_name: str) -> bool:
        """Check if an index exists."""
        response = self.session.head(
            f"{self.config.url}/{index_name}",
            timeout=5
        )
        return response.status_code == 200
    
    def count_documents(self, index_name: str) -> int:
        """Count documents in an index."""
        response = self.session.get(
            f"{self.config.url}/{index_name}/_count",
            timeout=5
        )
        if response.status_code == 200:
            return response.json().get('count', 0)
        return 0

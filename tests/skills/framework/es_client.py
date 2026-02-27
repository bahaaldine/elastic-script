"""
Elasticsearch client for skill testing.

Supports two authentication modes:
  1. Username/password (default, for local dev clusters)
  2. API key + Cloud ID (for Elastic Cloud, reads from .env)

Set ELASTICSEARCH_API_KEY and ELASTICSEARCH_CLOUD_ID in .env for cloud auth.
"""

import os
import json
import re
import requests
from typing import Any, Dict, List, Optional
from dataclasses import dataclass, field
import base64


def _load_dotenv(path: str = '.env') -> Dict[str, str]:
    """Load .env file into a dict without requiring python-dotenv."""
    env = {}
    try:
        with open(path) as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                if '=' in line:
                    key, value = line.split('=', 1)
                    env[key.strip()] = value.strip()
    except FileNotFoundError:
        pass
    return env


def _cloud_id_to_url(cloud_id: str) -> str:
    """Decode an Elastic Cloud ID to an Elasticsearch URL.

    Cloud ID format: <name>:<base64(host$es_uuid.es$kibana_uuid.kb)>
    """
    _, encoded = cloud_id.split(':', 1)
    decoded = base64.b64decode(encoded).decode()
    parts = decoded.split('$')
    host = parts[0]
    es_uuid = parts[1] if len(parts) > 1 else ''
    return f"https://{es_uuid}.{host}:443" if es_uuid else f"https://{host}:443"


@dataclass
class ESConfig:
    """Elasticsearch connection configuration.

    Supports two auth modes:
      - username/password (local clusters)
      - api_key (Elastic Cloud via ELASTICSEARCH_API_KEY / ELASTICSEARCH_CLOUD_ID)
    """
    url: str = "http://localhost:9200"
    username: str = "elastic-admin"
    password: str = "elastic-password"
    api_key: Optional[str] = None
    cloud_id: Optional[str] = None

    @property
    def is_cloud(self) -> bool:
        return self.api_key is not None

    @property
    def auth_header(self) -> str:
        if self.api_key:
            return f"ApiKey {self.api_key}"
        credentials = f"{self.username}:{self.password}"
        return f"Basic {base64.b64encode(credentials.encode()).decode()}"

    @classmethod
    def from_env(cls) -> 'ESConfig':
        """Build config from environment variables, falling back to .env file."""
        dotenv = _load_dotenv()

        api_key = os.getenv('ELASTICSEARCH_API_KEY') or dotenv.get('ELASTICSEARCH_API_KEY')
        cloud_id = os.getenv('ELASTICSEARCH_CLOUD_ID') or dotenv.get('ELASTICSEARCH_CLOUD_ID')

        if api_key and cloud_id:
            url = _cloud_id_to_url(cloud_id)
            return cls(url=url, api_key=api_key, cloud_id=cloud_id)

        return cls(
            url=os.getenv('ES_URL', 'http://localhost:9200'),
            username=os.getenv('ES_USERNAME', 'elastic-admin'),
            password=os.getenv('ES_PASSWORD', 'elastic-password')
        )


class ElasticsearchTestClient:
    """Client for interacting with Elasticsearch and running skills.

    Automatically detects auth mode from ESConfig:
      - API key auth for cloud clusters
      - Basic auth for local clusters
    """

    def __init__(self, config: Optional[ESConfig] = None):
        self.config = config or ESConfig.from_env()
        self.session = requests.Session()
        if self.config.is_cloud:
            self.session.headers['Authorization'] = f"ApiKey {self.config.api_key}"
        else:
            self.session.auth = (self.config.username, self.config.password)
        self.session.headers['Content-Type'] = 'application/json'
    
    def is_available(self) -> bool:
        """Check if Elasticsearch is available.

        Uses the root endpoint (/) instead of /_cluster/health because
        /_cluster/health is not available on serverless clusters.
        """
        try:
            response = self.session.get(self.config.url, timeout=5)
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

    def run_esql(self, query: str) -> Dict[str, Any]:
        """Execute an ES|QL query directly against the /_query endpoint.

        Returns dict with:
          - success: bool
          - status_code: int
          - columns: list of {name, type} dicts
          - values: list of row arrays
          - rows: list of dicts (column_name -> value) for convenience
          - error: error message if failed
        """
        response = self.session.post(
            f"{self.config.url}/_query",
            json={"query": query},
            timeout=30
        )
        result: Dict[str, Any] = {
            'success': response.status_code == 200,
            'status_code': response.status_code,
            'columns': [],
            'values': [],
            'rows': [],
            'error': None,
        }
        try:
            body = response.json()
        except Exception:
            result['error'] = response.text
            return result

        if response.status_code != 200:
            result['error'] = body.get('error', {}).get('reason', response.text)
            return result

        columns = body.get('columns', [])
        values = body.get('values', [])
        result['columns'] = columns
        result['values'] = values

        col_names = [c['name'] for c in columns]
        result['rows'] = [dict(zip(col_names, row)) for row in values]
        return result

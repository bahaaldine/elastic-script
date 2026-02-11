"""
Tests for Moltler client.
"""

import pytest
import responses
from unittest.mock import patch

from moltler import Moltler
from moltler.exceptions import ConnectionError, AuthenticationError, ExecutionError


class TestMoltlerClient:
    """Tests for the Moltler client."""
    
    def test_init_defaults(self):
        """Test client initialization with defaults."""
        client = Moltler()
        assert client.host == "localhost"
        assert client.port == 9200
        assert client.username == "elastic-admin"
        assert client.base_url == "http://localhost:9200"
    
    def test_init_with_params(self):
        """Test client initialization with custom parameters."""
        client = Moltler(
            host="es.example.com",
            port=443,
            username="user",
            password="pass",
            use_ssl=True
        )
        assert client.host == "es.example.com"
        assert client.port == 443
        assert client.use_ssl is True
        assert client.base_url == "https://es.example.com:443"
    
    @patch.dict('os.environ', {
        'MOLTLER_HOST': 'env-host',
        'MOLTLER_PORT': '9201',
        'MOLTLER_USERNAME': 'env-user',
        'MOLTLER_PASSWORD': 'env-pass',
    })
    def test_init_from_env(self):
        """Test client initialization from environment variables."""
        client = Moltler()
        assert client.host == "env-host"
        assert client.port == 9201
        assert client.username == "env-user"
        assert client.password == "env-pass"
    
    @responses.activate
    def test_connect_success(self):
        """Test successful connection."""
        responses.add(
            responses.GET,
            "http://localhost:9200/",
            json={
                "cluster_name": "test-cluster",
                "version": {"number": "8.15.0"}
            },
            status=200
        )
        
        client = Moltler()
        result = client.connect()
        
        assert result is True
        assert client.connected is True
        assert client.cluster_name == "test-cluster"
        assert client.version == "8.15.0"
    
    @responses.activate
    def test_connect_auth_failure(self):
        """Test connection with authentication failure."""
        responses.add(
            responses.GET,
            "http://localhost:9200/",
            status=401
        )
        
        client = Moltler()
        with pytest.raises(AuthenticationError):
            client.connect()
    
    @responses.activate
    def test_connect_failure(self):
        """Test connection failure."""
        responses.add(
            responses.GET,
            "http://localhost:9200/",
            body=Exception("Connection refused")
        )
        
        client = Moltler()
        with pytest.raises(ConnectionError):
            client.connect()
    
    @responses.activate
    def test_execute_success(self):
        """Test successful execution."""
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"result": {"status": "ok"}},
            status=200
        )
        
        client = Moltler()
        result = client.execute("SHOW SKILLS")
        
        assert result.success is True
        assert result.data == {"status": "ok"}
    
    @responses.activate
    def test_execute_error(self):
        """Test execution with error response."""
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"error": "Syntax error"},
            status=400
        )
        
        client = Moltler()
        result = client.execute("INVALID SYNTAX")
        
        assert result.success is False
        assert "Syntax error" in result.error
    
    @responses.activate
    def test_context_manager(self):
        """Test context manager."""
        responses.add(
            responses.GET,
            "http://localhost:9200/",
            json={"cluster_name": "test", "version": {"number": "8.15.0"}},
            status=200
        )
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"result": []},
            status=200
        )
        
        with Moltler() as client:
            assert client.connected is True
            result = client.execute("SHOW SKILLS")
            assert result.success is True
        
        # After context exit, client should be closed
        assert client.connected is False


class TestSkillManager:
    """Tests for the skill manager."""
    
    @responses.activate
    def test_list_skills(self):
        """Test listing skills."""
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"result": {
                "skills": [
                    {"name": "skill1", "version": "1.0.0"},
                    {"name": "skill2", "version": "2.0.0"}
                ]
            }},
            status=200
        )
        
        client = Moltler()
        skills = client.skills.list()
        
        assert len(skills) == 2
        assert skills[0].name == "skill1"
        assert skills[1].version == "2.0.0"
    
    @responses.activate
    def test_get_skill(self):
        """Test getting a skill."""
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"result": {
                "name": "test_skill",
                "version": "1.0.0",
                "description": "A test skill",
                "author": "Test Author"
            }},
            status=200
        )
        
        client = Moltler()
        skill = client.skills.get("test_skill")
        
        assert skill.name == "test_skill"
        assert skill.version == "1.0.0"
        assert skill.description == "A test skill"


class TestConnectorManager:
    """Tests for the connector manager."""
    
    @responses.activate
    def test_list_connectors(self):
        """Test listing connectors."""
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"result": {
                "connectors": [
                    {"name": "github1", "type": "github", "enabled": True},
                    {"name": "jira1", "type": "jira", "enabled": False}
                ]
            }},
            status=200
        )
        
        client = Moltler()
        connectors = client.connectors.list()
        
        assert len(connectors) == 2
        assert connectors[0].name == "github1"
        assert connectors[0].type == "github"
        assert connectors[1].enabled is False
    
    @responses.activate
    def test_sync_connector(self):
        """Test syncing a connector."""
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"result": {
                "status": "completed",
                "docs_synced": 150
            }},
            status=200
        )
        
        client = Moltler()
        result = client.connectors.sync("my_github")
        
        assert result.status == "completed"
        assert result.docs_synced == 150


class TestAgentManager:
    """Tests for the agent manager."""
    
    @responses.activate
    def test_list_agents(self):
        """Test listing agents."""
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"result": {
                "agents": [
                    {"name": "agent1", "goal": "Do something", "enabled": True},
                    {"name": "agent2", "goal": "Do other thing", "enabled": False}
                ]
            }},
            status=200
        )
        
        client = Moltler()
        agents = client.agents.list()
        
        assert len(agents) == 2
        assert agents[0].name == "agent1"
        assert agents[0].goal == "Do something"
    
    @responses.activate
    def test_trigger_agent(self):
        """Test triggering an agent."""
        responses.add(
            responses.POST,
            "http://localhost:9200/_escript",
            json={"result": {
                "execution_id": "exec-123",
                "agent": "test_agent",
                "status": "completed"
            }},
            status=200
        )
        
        client = Moltler()
        result = client.agents.trigger("test_agent", context={"priority": "high"})
        
        assert result.execution_id == "exec-123"
        assert result.status == "completed"

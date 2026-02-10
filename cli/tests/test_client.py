"""Tests for the Elasticsearch client module."""

import pytest
from unittest.mock import Mock, patch, MagicMock
import json

from escript_cli.config import Config
from escript_cli.client import ElasticScriptClient, ExecutionResult


@pytest.fixture
def config():
    """Create a config instance."""
    return Config(
        host="localhost",
        port=9200,
        username="elastic-admin",
        password="elastic-password",
    )


@pytest.fixture
def client(config):
    """Create a client instance."""
    return ElasticScriptClient(config)


class TestExecutionResult:
    """Test ExecutionResult dataclass."""
    
    def test_success_result(self):
        """Test successful result."""
        result = ExecutionResult(
            success=True,
            data={"status": "ok"},
        )
        assert result.success is True
        assert result.data == {"status": "ok"}
        assert result.error is None
    
    def test_error_result(self):
        """Test error result."""
        result = ExecutionResult(
            success=False,
            data=None,
            error="Something went wrong",
        )
        assert result.success is False
        assert result.error == "Something went wrong"
    
    def test_result_with_metadata(self):
        """Test result with metadata."""
        result = ExecutionResult(
            success=True,
            data=["item1", "item2"],
            execution_id="exec-123",
            output=["PRINT line 1"],
            elapsed_ms=42.5,
        )
        assert result.execution_id == "exec-123"
        assert result.output == ["PRINT line 1"]
        assert result.elapsed_ms == 42.5


class TestClientInit:
    """Test client initialization."""
    
    def test_base_url(self, client, config):
        """Test base URL generation."""
        assert client.base_url == "http://localhost:9200"
    
    def test_ssl_url(self):
        """Test HTTPS URL with SSL."""
        config = Config(
            host="secure.example.com",
            port=9243,
            use_ssl=True,
        )
        client = ElasticScriptClient(config)
        assert client.base_url == "https://secure.example.com:9243"


class TestConnectionTest:
    """Test connection testing."""
    
    def test_successful_connection(self, client):
        """Test successful connection."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "cluster_name": "test-cluster",
            "version": {"number": "8.17.0"},
        }
        
        with patch.object(client._session, 'get', return_value=mock_response):
            success, message = client.test_connection()
        
        assert success is True
        assert "Connected to test-cluster" in message
        assert "8.17.0" in message
    
    def test_failed_connection(self, client):
        """Test failed connection (HTTP error)."""
        mock_response = Mock()
        mock_response.status_code = 401
        
        with patch.object(client._session, 'get', return_value=mock_response):
            success, message = client.test_connection()
        
        assert success is False
        assert "401" in message
    
    def test_connection_error(self, client):
        """Test connection error (network issue)."""
        import requests
        
        with patch.object(client._session, 'get', 
                         side_effect=requests.exceptions.ConnectionError()):
            success, message = client.test_connection()
        
        assert success is False
        assert "Cannot connect" in message
    
    def test_connection_timeout(self, client):
        """Test connection timeout."""
        import requests
        
        with patch.object(client._session, 'get',
                         side_effect=requests.exceptions.Timeout()):
            success, message = client.test_connection()
        
        assert success is False
        assert "timeout" in message.lower()


class TestQueryExecution:
    """Test query execution."""
    
    def test_successful_query(self, client):
        """Test successful query execution."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "result": {"action": "CREATE", "procedure": "test"},
        }
        
        with patch.object(client._session, 'post', return_value=mock_response):
            result = client.execute("CREATE PROCEDURE test() BEGIN END PROCEDURE;")
        
        assert result.success is True
        assert result.data == {"action": "CREATE", "procedure": "test"}
    
    def test_query_with_output(self, client):
        """Test query with PRINT output."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "result": None,
            "output": ["Hello", "World"],
            "elapsed_ms": 15.5,
        }
        
        with patch.object(client._session, 'post', return_value=mock_response):
            result = client.execute("PRINT 'Hello'; PRINT 'World';")
        
        assert result.success is True
        assert result.output == ["Hello", "World"]
        assert result.elapsed_ms == 15.5
    
    def test_query_with_args(self, client):
        """Test query with arguments."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"result": "ok"}
        
        with patch.object(client._session, 'post', return_value=mock_response) as mock_post:
            result = client.execute("CALL proc(@arg)", args={"arg": "value"})
        
        # Verify args were passed
        call_args = mock_post.call_args
        payload = call_args.kwargs.get('json') or call_args[1].get('json')
        assert payload["args"] == {"arg": "value"}
    
    def test_query_error_response(self, client):
        """Test query error response."""
        mock_response = Mock()
        mock_response.status_code = 400
        mock_response.text = '{"error": {"reason": "Syntax error"}}'
        mock_response.json.return_value = {
            "error": {"reason": "Syntax error"}
        }
        
        with patch.object(client._session, 'post', return_value=mock_response):
            result = client.execute("INVALID QUERY")
        
        assert result.success is False
        assert "Syntax error" in result.error
    
    def test_query_timeout(self, client):
        """Test query timeout."""
        import requests
        
        with patch.object(client._session, 'post',
                         side_effect=requests.exceptions.Timeout()):
            result = client.execute("SELECT * FROM huge_table")
        
        assert result.success is False
        assert "timeout" in result.error.lower()
    
    def test_query_connection_lost(self, client):
        """Test connection lost during query."""
        import requests
        
        with patch.object(client._session, 'post',
                         side_effect=requests.exceptions.ConnectionError()):
            result = client.execute("CALL long_running()")
        
        assert result.success is False
        assert "connection" in result.error.lower()


class TestHelperMethods:
    """Test helper methods."""
    
    def test_list_procedures(self, client):
        """Test list_procedures helper."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "result": [{"name": "proc1"}, {"name": "proc2"}]
        }
        
        with patch.object(client._session, 'post', return_value=mock_response) as mock_post:
            result = client.list_procedures()
        
        # Verify correct query was sent
        call_args = mock_post.call_args
        payload = call_args.kwargs.get('json') or call_args[1].get('json')
        assert "ESCRIPT_PROCEDURES" in payload["query"]
    
    def test_list_skills(self, client):
        """Test list_skills helper."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "result": [{"name": "skill1"}]
        }
        
        with patch.object(client._session, 'post', return_value=mock_response) as mock_post:
            result = client.list_skills()
        
        # Verify correct query was sent
        call_args = mock_post.call_args
        payload = call_args.kwargs.get('json') or call_args[1].get('json')
        assert "SHOW SKILLS" in payload["query"]
    
    def test_get_cluster_info(self, client):
        """Test get_cluster_info helper."""
        mock_response = Mock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "cluster_name": "my-cluster",
            "version": {"number": "8.17.0"},
        }
        
        with patch.object(client._session, 'get', return_value=mock_response):
            info = client.get_cluster_info()
        
        assert info["cluster_name"] == "my-cluster"
    
    def test_get_cluster_info_error(self, client):
        """Test get_cluster_info with error."""
        with patch.object(client._session, 'get', side_effect=Exception("error")):
            info = client.get_cluster_info()
        
        assert info == {}


class TestClientClose:
    """Test client cleanup."""
    
    def test_close(self, client):
        """Test close method."""
        with patch.object(client._session, 'close') as mock_close:
            client.close()
            mock_close.assert_called_once()

"""
Tests for Moltler connectors module.
"""

import pytest
from moltler.connectors import Connector, ConnectorConfig, SyncState


class TestConnectorConfig:
    """Tests for ConnectorConfig."""
    
    def test_to_dict(self):
        """Test converting config to dictionary."""
        config = ConnectorConfig(
            type="github",
            token="ghp_xxxx",
            organization="my-org"
        )
        
        d = config.to_dict()
        
        assert d["token"] == "ghp_xxxx"
        assert d["organization"] == "my-org"
        assert "url" not in d  # None values excluded
    
    def test_to_dict_with_extra(self):
        """Test config with extra fields."""
        config = ConnectorConfig(
            type="custom",
            extra={"custom_field": "value"}
        )
        
        d = config.to_dict()
        
        assert d["custom_field"] == "value"


class TestConnector:
    """Tests for Connector class."""
    
    def test_from_dict(self):
        """Test creating connector from dictionary."""
        data = {
            "name": "my_github",
            "type": "github",
            "config": {"token": "xxx"},
            "enabled": True,
            "description": "GitHub connector"
        }
        
        connector = Connector.from_dict(data)
        
        assert connector.name == "my_github"
        assert connector.type == "github"
        assert connector.enabled is True
        assert connector.config["token"] == "xxx"
    
    def test_from_dict_minimal(self):
        """Test creating connector from minimal dictionary."""
        data = {"name": "minimal", "type": "http"}
        
        connector = Connector.from_dict(data)
        
        assert connector.name == "minimal"
        assert connector.enabled is True  # default
        assert connector.config == {}


class TestSyncState:
    """Tests for SyncState class."""
    
    def test_sync_state(self):
        """Test SyncState creation."""
        state = SyncState(
            connector="my_github",
            entity="issues",
            status="completed",
            docs_synced=100
        )
        
        assert state.connector == "my_github"
        assert state.entity == "issues"
        assert state.docs_synced == 100

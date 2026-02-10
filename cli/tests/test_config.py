"""Tests for the configuration module."""

import os
import pytest
from pathlib import Path
from tempfile import TemporaryDirectory

from escript_cli.config import Config, SAMPLE_CONFIG


class TestConfigDefaults:
    """Test default configuration values."""
    
    def test_default_host(self):
        """Test default host."""
        config = Config()
        assert config.host == "localhost"
    
    def test_default_port(self):
        """Test default port."""
        config = Config()
        assert config.port == 9200
    
    def test_default_credentials(self):
        """Test default credentials."""
        config = Config()
        assert config.username == "elastic-admin"
        assert config.password == "elastic-password"
    
    def test_default_ssl(self):
        """Test SSL defaults to off."""
        config = Config()
        assert config.use_ssl is False
    
    def test_default_multiline(self):
        """Test multiline defaults to on."""
        config = Config()
        assert config.multiline is True
    
    def test_default_color(self):
        """Test color defaults to on."""
        config = Config()
        assert config.color is True


class TestConfigUrl:
    """Test URL generation."""
    
    def test_http_url(self):
        """Test HTTP URL generation."""
        config = Config(host="localhost", port=9200, use_ssl=False)
        assert config.url == "http://localhost:9200"
    
    def test_https_url(self):
        """Test HTTPS URL generation."""
        config = Config(host="elastic.example.com", port=9243, use_ssl=True)
        assert config.url == "https://elastic.example.com:9243"


class TestConfigHistoryPath:
    """Test history path handling."""
    
    def test_expands_tilde(self):
        """Test that ~ is expanded in history path."""
        config = Config(history_file="~/.escript_history")
        assert "~" not in str(config.history_path)
        assert config.history_path.is_absolute()
    
    def test_custom_history_path(self):
        """Test custom history path."""
        config = Config(history_file="/tmp/my_history")
        assert config.history_path == Path("/tmp/my_history")


class TestConfigFromDict:
    """Test loading config from dictionary (TOML file)."""
    
    def test_connection_section(self):
        """Test loading connection settings."""
        data = {
            "connection": {
                "host": "prod.example.com",
                "port": 9243,
                "username": "admin",
                "password": "secret",
                "use_ssl": True,
            }
        }
        config = Config._from_dict(data)
        
        assert config.host == "prod.example.com"
        assert config.port == 9243
        assert config.username == "admin"
        assert config.password == "secret"
        assert config.use_ssl is True
    
    def test_repl_section(self):
        """Test loading REPL settings."""
        data = {
            "repl": {
                "multiline": False,
                "vi_mode": True,
                "history_size": 5000,
            }
        }
        config = Config._from_dict(data)
        
        assert config.multiline is False
        assert config.vi_mode is True
        assert config.history_size == 5000
    
    def test_output_section(self):
        """Test loading output settings."""
        data = {
            "output": {
                "color": False,
                "table_style": "simple",
                "json_indent": 4,
                "max_width": 120,
            }
        }
        config = Config._from_dict(data)
        
        assert config.color is False
        assert config.table_style == "simple"
        assert config.json_indent == 4
        assert config.max_width == 120
    
    def test_partial_config(self):
        """Test that partial config doesn't break defaults."""
        data = {"connection": {"host": "custom-host"}}
        config = Config._from_dict(data)
        
        assert config.host == "custom-host"
        assert config.port == 9200  # Default preserved


class TestConfigEnvironment:
    """Test environment variable overrides."""
    
    def test_host_override(self, monkeypatch):
        """Test ESCRIPT_HOST override."""
        monkeypatch.setenv("ESCRIPT_HOST", "env-host")
        config = Config()
        config = Config._apply_env(config)
        assert config.host == "env-host"
    
    def test_port_override(self, monkeypatch):
        """Test ESCRIPT_PORT override."""
        monkeypatch.setenv("ESCRIPT_PORT", "9300")
        config = Config()
        config = Config._apply_env(config)
        assert config.port == 9300
    
    def test_ssl_override_true(self, monkeypatch):
        """Test ESCRIPT_USE_SSL=true."""
        monkeypatch.setenv("ESCRIPT_USE_SSL", "true")
        config = Config()
        config = Config._apply_env(config)
        assert config.use_ssl is True
    
    def test_ssl_override_false(self, monkeypatch):
        """Test ESCRIPT_USE_SSL=false."""
        monkeypatch.setenv("ESCRIPT_USE_SSL", "false")
        config = Config(use_ssl=True)  # Start with True
        config = Config._apply_env(config)
        assert config.use_ssl is False
    
    def test_invalid_port_ignored(self, monkeypatch):
        """Test that invalid port value is ignored."""
        monkeypatch.setenv("ESCRIPT_PORT", "not-a-number")
        config = Config()
        config = Config._apply_env(config)
        assert config.port == 9200  # Default preserved


class TestConfigSave:
    """Test saving configuration to file."""
    
    def test_save_creates_file(self):
        """Test that save creates a config file."""
        with TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / ".escriptrc"
            config = Config()
            config.save(path)
            
            assert path.exists()
    
    def test_save_content(self):
        """Test saved file content."""
        with TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / ".escriptrc"
            config = Config(
                host="test-host",
                port=9300,
                username="testuser",
            )
            config.save(path)
            
            content = path.read_text()
            assert "test-host" in content
            assert "9300" in content
            assert "testuser" in content


class TestSampleConfig:
    """Test sample configuration."""
    
    def test_sample_config_is_valid_toml(self):
        """Test that sample config is valid TOML."""
        import toml
        
        # Should not raise
        data = toml.loads(SAMPLE_CONFIG)
        
        assert "connection" in data
        assert "repl" in data
        assert "output" in data
    
    def test_sample_config_has_all_sections(self):
        """Test sample config has all expected sections."""
        import toml
        
        data = toml.loads(SAMPLE_CONFIG)
        
        assert "host" in data["connection"]
        assert "port" in data["connection"]
        assert "multiline" in data["repl"]
        assert "color" in data["output"]

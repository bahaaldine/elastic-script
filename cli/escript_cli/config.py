"""
Configuration management for Moltler CLI.

Supports:
- ~/.moltlerrc (TOML format)
- Environment variables (MOLTLER_*)
- Command-line overrides
"""

import os
from pathlib import Path
from dataclasses import dataclass, field
from typing import Optional

try:
    import toml
except ImportError:
    toml = None


@dataclass
class Config:
    """Configuration for the Moltler CLI."""
    
    # Connection settings
    host: str = "localhost"
    port: int = 9200
    username: str = "elastic-admin"
    password: str = "elastic-password"
    use_ssl: bool = False
    verify_certs: bool = True
    
    # REPL settings
    multiline: bool = True
    vi_mode: bool = False
    history_file: str = "~/.moltler_history"
    history_size: int = 10000
    
    # Output settings
    color: bool = True
    table_style: str = "rounded"  # rounded, simple, minimal, markdown
    max_width: Optional[int] = None  # None = auto
    json_indent: int = 2
    
    # Editor settings
    editor: str = "vim"
    
    @property
    def url(self) -> str:
        """Get the full Elasticsearch URL."""
        scheme = "https" if self.use_ssl else "http"
        return f"{scheme}://{self.host}:{self.port}"
    
    @property
    def history_path(self) -> Path:
        """Get the expanded history file path."""
        return Path(self.history_file).expanduser()
    
    @classmethod
    def load(cls) -> "Config":
        """
        Load configuration from file and environment.
        
        Priority (highest to lowest):
        1. Environment variables (MOLTLER_*)
        2. ~/.moltlerrc
        3. Default values
        """
        config = cls()
        
        # Load from file
        config_file = Path.home() / ".moltlerrc"
        if config_file.exists() and toml is not None:
            try:
                data = toml.load(config_file)
                config = cls._from_dict(data)
            except Exception as e:
                print(f"Warning: Could not load {config_file}: {e}")
        
        # Override with environment variables
        config = cls._apply_env(config)
        
        return config
    
    @classmethod
    def _from_dict(cls, data: dict) -> "Config":
        """Create config from dictionary (TOML file)."""
        config = cls()
        
        # Connection section
        if "connection" in data:
            conn = data["connection"]
            if "host" in conn:
                config.host = conn["host"]
            if "port" in conn:
                config.port = conn["port"]
            if "username" in conn:
                config.username = conn["username"]
            if "password" in conn:
                config.password = conn["password"]
            if "use_ssl" in conn:
                config.use_ssl = conn["use_ssl"]
            if "verify_certs" in conn:
                config.verify_certs = conn["verify_certs"]
        
        # REPL section
        if "repl" in data:
            repl = data["repl"]
            if "multiline" in repl:
                config.multiline = repl["multiline"]
            if "vi_mode" in repl:
                config.vi_mode = repl["vi_mode"]
            if "history_file" in repl:
                config.history_file = repl["history_file"]
            if "history_size" in repl:
                config.history_size = repl["history_size"]
        
        # Output section
        if "output" in data:
            out = data["output"]
            if "color" in out:
                config.color = out["color"]
            if "table_style" in out:
                config.table_style = out["table_style"]
            if "max_width" in out:
                config.max_width = out["max_width"]
            if "json_indent" in out:
                config.json_indent = out["json_indent"]
        
        # Editor section
        if "editor" in data:
            config.editor = data["editor"].get("command", config.editor)
        
        return config
    
    @classmethod
    def _apply_env(cls, config: "Config") -> "Config":
        """Apply environment variable overrides."""
        env_mappings = {
            "MOLTLER_HOST": ("host", str),
            "MOLTLER_PORT": ("port", int),
            "MOLTLER_USERNAME": ("username", str),
            "MOLTLER_PASSWORD": ("password", str),
            "MOLTLER_USE_SSL": ("use_ssl", lambda x: x.lower() in ("true", "1", "yes")),
            "MOLTLER_COLOR": ("color", lambda x: x.lower() in ("true", "1", "yes")),
            "MOLTLER_VI_MODE": ("vi_mode", lambda x: x.lower() in ("true", "1", "yes")),
        }
        
        for env_var, (attr, converter) in env_mappings.items():
            value = os.environ.get(env_var)
            if value is not None:
                try:
                    setattr(config, attr, converter(value))
                except (ValueError, TypeError):
                    pass
        
        return config
    
    def save(self, path: Optional[Path] = None) -> None:
        """Save configuration to file."""
        if toml is None:
            raise ImportError("toml package required to save config")
        
        if path is None:
            path = Path.home() / ".moltlerrc"
        
        data = {
            "connection": {
                "host": self.host,
                "port": self.port,
                "username": self.username,
                "password": self.password,
                "use_ssl": self.use_ssl,
                "verify_certs": self.verify_certs,
            },
            "repl": {
                "multiline": self.multiline,
                "vi_mode": self.vi_mode,
                "history_file": self.history_file,
                "history_size": self.history_size,
            },
            "output": {
                "color": self.color,
                "table_style": self.table_style,
                "json_indent": self.json_indent,
            },
            "editor": {
                "command": self.editor,
            },
        }
        
        if self.max_width is not None:
            data["output"]["max_width"] = self.max_width
        
        with open(path, "w") as f:
            toml.dump(data, f)


# Sample config file content
SAMPLE_CONFIG = '''# Moltler CLI Configuration
# Save this file as ~/.moltlerrc

[connection]
host = "localhost"
port = 9200
username = "elastic-admin"
password = "elastic-password"
use_ssl = false
verify_certs = true

[repl]
multiline = true
vi_mode = false
history_file = "~/.moltler_history"
history_size = 10000

[output]
color = true
table_style = "rounded"  # rounded, simple, minimal, markdown
json_indent = 2
# max_width = 120  # Uncomment to set fixed width

[editor]
command = "vim"
'''

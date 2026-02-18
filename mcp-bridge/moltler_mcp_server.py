#!/usr/bin/env python3
"""
Moltler MCP Server - Exposes elastic-script skills to AI agents via Model Context Protocol.

This server acts as a bridge between MCP-compatible AI agents (Claude Desktop, Cursor, etc.)
and the Moltler/elastic-script skills running in Elasticsearch.

Usage:
    python moltler_mcp_server.py [--es-url URL] [--es-user USER] [--es-password PASSWORD]

Environment variables:
    ES_URL: Elasticsearch URL (default: http://localhost:9200)
    ES_USER: Elasticsearch username (default: elastic-admin)
    ES_PASSWORD: Elasticsearch password (default: elastic-password)
"""

import sys
import json
import asyncio
import argparse
import os
from typing import Any
import httpx

# MCP Protocol constants
MCP_VERSION = "2024-11-05"
SERVER_NAME = "moltler-skills"
SERVER_VERSION = "1.0.0"

class MoltlerMCPServer:
    """MCP Server that bridges to Elasticsearch's Moltler skills."""
    
    def __init__(self, es_url: str, es_user: str, es_password: str):
        self.es_url = es_url.rstrip('/')
        self.es_auth = (es_user, es_password)
        self.mcp_endpoint = f"{self.es_url}/_escript/mcp"
        self.skills_cache = {}
        self.initialized = False
        
    async def _call_es_mcp(self, method: str, params: dict = None) -> dict:
        """Call the Elasticsearch MCP endpoint."""
        payload = {
            "jsonrpc": "2.0",
            "method": method,
            "id": 1
        }
        if params:
            payload["params"] = params
            
        async with httpx.AsyncClient() as client:
            try:
                response = await client.post(
                    self.mcp_endpoint,
                    json=payload,
                    auth=self.es_auth,
                    timeout=30.0
                )
                return response.json()
            except Exception as e:
                return {"error": {"code": -32603, "message": str(e)}}
    
    async def handle_initialize(self, params: dict) -> dict:
        """Handle MCP initialize request."""
        self.initialized = True
        return {
            "protocolVersion": MCP_VERSION,
            "capabilities": {
                "tools": {"listChanged": True}
            },
            "serverInfo": {
                "name": SERVER_NAME,
                "version": SERVER_VERSION
            }
        }
    
    async def handle_tools_list(self) -> dict:
        """Handle tools/list request - fetches skills from ES."""
        result = await self._call_es_mcp("tools/list")
        if "result" in result:
            # Cache skills for later use
            tools = result["result"].get("tools", [])
            for tool in tools:
                self.skills_cache[tool["name"]] = tool
            return result["result"]
        return {"tools": []}
    
    async def handle_tools_call(self, params: dict) -> dict:
        """Handle tools/call request - executes a skill in ES."""
        tool_name = params.get("name")
        arguments = params.get("arguments", {})
        
        result = await self._call_es_mcp("tools/call", {
            "name": tool_name,
            "arguments": arguments
        })
        
        if "result" in result:
            return result["result"]
        elif "error" in result:
            return {
                "content": [{"type": "text", "text": f"Error: {result['error']}"}],
                "isError": True
            }
        return result
    
    async def handle_request(self, request: dict) -> dict:
        """Route MCP request to appropriate handler."""
        method = request.get("method", "")
        params = request.get("params", {})
        request_id = request.get("id")
        
        try:
            if method == "initialize":
                result = await self.handle_initialize(params)
            elif method == "notifications/initialized":
                return None  # No response for notifications
            elif method == "tools/list":
                result = await self.handle_tools_list()
            elif method == "tools/call":
                result = await self.handle_tools_call(params)
            elif method == "ping":
                result = {}
            else:
                return {
                    "jsonrpc": "2.0",
                    "error": {"code": -32601, "message": f"Method not found: {method}"},
                    "id": request_id
                }
            
            return {
                "jsonrpc": "2.0",
                "result": result,
                "id": request_id
            }
        except Exception as e:
            return {
                "jsonrpc": "2.0",
                "error": {"code": -32603, "message": str(e)},
                "id": request_id
            }
    
    async def run_stdio(self):
        """Run the MCP server using stdio transport."""
        reader = asyncio.StreamReader()
        protocol = asyncio.StreamReaderProtocol(reader)
        await asyncio.get_event_loop().connect_read_pipe(lambda: protocol, sys.stdin)
        
        writer_transport, writer_protocol = await asyncio.get_event_loop().connect_write_pipe(
            asyncio.streams.FlowControlMixin, sys.stdout
        )
        writer = asyncio.StreamWriter(writer_transport, writer_protocol, reader, asyncio.get_event_loop())
        
        while True:
            try:
                line = await reader.readline()
                if not line:
                    break
                    
                request = json.loads(line.decode('utf-8').strip())
                response = await self.handle_request(request)
                
                if response:  # Don't respond to notifications
                    writer.write((json.dumps(response) + '\n').encode('utf-8'))
                    await writer.drain()
                    
            except json.JSONDecodeError as e:
                error_response = {
                    "jsonrpc": "2.0",
                    "error": {"code": -32700, "message": f"Parse error: {e}"},
                    "id": None
                }
                writer.write((json.dumps(error_response) + '\n').encode('utf-8'))
                await writer.drain()
            except Exception as e:
                sys.stderr.write(f"Error: {e}\n")
                sys.stderr.flush()


def main():
    parser = argparse.ArgumentParser(description="Moltler MCP Server")
    parser.add_argument("--es-url", default=os.getenv("ES_URL", "http://localhost:9200"),
                        help="Elasticsearch URL")
    parser.add_argument("--es-user", default=os.getenv("ES_USER", "elastic-admin"),
                        help="Elasticsearch username")
    parser.add_argument("--es-password", default=os.getenv("ES_PASSWORD", "elastic-password"),
                        help="Elasticsearch password")
    args = parser.parse_args()
    
    server = MoltlerMCPServer(args.es_url, args.es_user, args.es_password)
    asyncio.run(server.run_stdio())


if __name__ == "__main__":
    main()

#!/usr/bin/env node

/**
 * MCP Bridge for Moltler/elastic-script
 * 
 * This is a stdio-to-HTTP bridge that allows MCP clients (like Claude Desktop)
 * to communicate with the Elasticsearch MCP endpoint.
 * 
 * Usage:
 *   npx @moltler/mcp-bridge --es-url http://localhost:9200
 * 
 * Or configure in Claude Desktop:
 *   {
 *     "mcpServers": {
 *       "moltler": {
 *         "command": "npx",
 *         "args": ["@moltler/mcp-bridge", "--es-url", "http://localhost:9200"]
 *       }
 *     }
 *   }
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool,
} from '@modelcontextprotocol/sdk/types.js';

// Parse command line arguments
function parseArgs(): { esUrl: string; username: string; password: string } {
  const args = process.argv.slice(2);
  let esUrl = 'http://localhost:9200';
  let username = 'elastic-admin';
  let password = 'elastic-password';

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--es-url' && args[i + 1]) {
      esUrl = args[++i];
    } else if (args[i] === '--username' && args[i + 1]) {
      username = args[++i];
    } else if (args[i] === '--password' && args[i + 1]) {
      password = args[++i];
    } else if (args[i] === '--help') {
      console.error(`
Moltler MCP Bridge - Connect AI agents to Elasticsearch skills

Usage:
  moltler-mcp [options]

Options:
  --es-url <url>       Elasticsearch URL (default: http://localhost:9200)
  --username <user>    Elasticsearch username (default: elastic-admin)
  --password <pass>    Elasticsearch password (default: elastic-password)
  --help               Show this help message

Environment Variables:
  ELASTICSEARCH_URL      Alternative to --es-url
  ELASTICSEARCH_USERNAME Alternative to --username
  ELASTICSEARCH_PASSWORD Alternative to --password
`);
      process.exit(0);
    }
  }

  // Also check environment variables
  esUrl = process.env.ELASTICSEARCH_URL || esUrl;
  username = process.env.ELASTICSEARCH_USERNAME || username;
  password = process.env.ELASTICSEARCH_PASSWORD || password;

  return { esUrl, username, password };
}

// Make a request to the Elasticsearch MCP endpoint
async function mcpRequest(
  esUrl: string,
  auth: string,
  method: string,
  params?: Record<string, unknown>
): Promise<unknown> {
  const response = await fetch(`${esUrl}/_escript/mcp`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Basic ${auth}`,
    },
    body: JSON.stringify({
      jsonrpc: '2.0',
      method,
      params,
      id: Date.now(),
    }),
  });

  if (!response.ok) {
    throw new Error(`HTTP error: ${response.status} ${response.statusText}`);
  }

  const json = await response.json() as { result?: unknown; error?: { message: string } };
  
  if (json.error) {
    throw new Error(json.error.message);
  }

  return json.result;
}

async function main() {
  const { esUrl, username, password } = parseArgs();
  const auth = Buffer.from(`${username}:${password}`).toString('base64');

  // Create MCP server
  const server = new Server(
    {
      name: 'moltler-elasticsearch',
      version: '1.0.0',
    },
    {
      capabilities: {
        tools: {},
      },
    }
  );

  // Handle tools/list
  server.setRequestHandler(ListToolsRequestSchema, async () => {
    try {
      const result = await mcpRequest(esUrl, auth, 'tools/list') as { tools: Tool[] };
      return { tools: result.tools || [] };
    } catch (error) {
      console.error('Failed to list tools:', error);
      return { tools: [] };
    }
  });

  // Handle tools/call
  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    try {
      const result = await mcpRequest(esUrl, auth, 'tools/call', {
        name: request.params.name,
        arguments: request.params.arguments,
      }) as { content: Array<{ type: string; text: string }>; isError: boolean };

      return {
        content: result.content || [{ type: 'text', text: 'No result' }],
        isError: result.isError || false,
      };
    } catch (error) {
      return {
        content: [{ type: 'text', text: `Error: ${error instanceof Error ? error.message : String(error)}` }],
        isError: true,
      };
    }
  });

  // Connect via stdio
  const transport = new StdioServerTransport();
  await server.connect(transport);

  console.error(`Moltler MCP Bridge connected to ${esUrl}`);
}

main().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});

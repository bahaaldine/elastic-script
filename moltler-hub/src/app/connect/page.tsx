'use client';

import Link from 'next/link';
import { useState } from 'react';

type Client = 'cursor' | 'claude-desktop' | 'claude-code' | 'vscode' | 'windsurf' | 'cline' | 'zed';

interface ClientInfo {
  name: string;
  icon: string;
  description: string;
  configPath?: string;
  docsUrl?: string;
}

const CLIENTS: Record<Client, ClientInfo> = {
  cursor: {
    name: 'Cursor',
    icon: '⚡',
    description: 'AI-powered code editor',
    configPath: '.cursor/mcp.json',
    docsUrl: 'https://docs.cursor.com/context/model-context-protocol',
  },
  'claude-desktop': {
    name: 'Claude Desktop',
    icon: '🤖',
    description: 'Anthropic\'s desktop app',
    configPath: '~/Library/Application Support/Claude/claude_desktop_config.json',
    docsUrl: 'https://docs.anthropic.com/en/docs/claude-desktop/mcp',
  },
  'claude-code': {
    name: 'Claude Code',
    icon: '💻',
    description: 'Claude\'s CLI coding agent',
    docsUrl: 'https://docs.anthropic.com/en/docs/claude-code',
  },
  vscode: {
    name: 'VS Code',
    icon: '📝',
    description: 'With GitHub Copilot Chat',
    configPath: '.vscode/mcp.json',
    docsUrl: 'https://code.visualstudio.com/docs/copilot/chat/mcp-servers',
  },
  windsurf: {
    name: 'Windsurf',
    icon: '🏄',
    description: 'AI-native IDE by Codeium',
    configPath: '~/.codeium/windsurf/mcp_config.json',
  },
  cline: {
    name: 'Cline',
    icon: '🔧',
    description: 'Autonomous coding agent',
    docsUrl: 'https://github.com/cline/cline',
  },
  zed: {
    name: 'Zed',
    icon: '⚡',
    description: 'High-performance code editor',
    configPath: '~/.config/zed/settings.json',
  },
};

export default function ConnectPage() {
  const [selectedClient, setSelectedClient] = useState<Client>('cursor');
  const [esUrl, setEsUrl] = useState('http://localhost:9200');
  const [esUser, setEsUser] = useState('elastic-admin');
  const [esPassword, setEsPassword] = useState('elastic-password');
  const [copied, setCopied] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopied(id);
    setTimeout(() => setCopied(null), 2000);
  };

  const getConfig = (client: Client): string => {
    const authHeader = btoa(`${esUser}:${esPassword}`);
    
    switch (client) {
      case 'cursor':
        return JSON.stringify({
          mcpServers: {
            moltler: {
              command: "python",
              args: ["<path-to-moltler>/mcp-bridge/moltler_mcp_server.py"],
              env: {
                ES_URL: esUrl,
                ES_USER: esUser,
                ES_PASSWORD: esPassword
              }
            }
          }
        }, null, 2);

      case 'claude-desktop':
        return JSON.stringify({
          mcpServers: {
            moltler: {
              command: "python",
              args: ["<path-to-moltler>/mcp-bridge/moltler_mcp_server.py"],
              env: {
                ES_URL: esUrl,
                ES_USER: esUser,
                ES_PASSWORD: esPassword
              }
            }
          }
        }, null, 2);

      case 'claude-code':
        return `claude mcp add moltler \\
  --command "python" \\
  --args "<path-to-moltler>/mcp-bridge/moltler_mcp_server.py" \\
  --env ES_URL="${esUrl}" \\
  --env ES_USER="${esUser}" \\
  --env ES_PASSWORD="${esPassword}"`;

      case 'vscode':
        return JSON.stringify({
          servers: {
            moltler: {
              command: "python",
              args: ["<path-to-moltler>/mcp-bridge/moltler_mcp_server.py"],
              env: {
                ES_URL: esUrl,
                ES_USER: esUser,
                ES_PASSWORD: esPassword
              }
            }
          }
        }, null, 2);

      case 'windsurf':
        return JSON.stringify({
          mcpServers: {
            moltler: {
              command: "python",
              args: ["<path-to-moltler>/mcp-bridge/moltler_mcp_server.py"],
              env: {
                ES_URL: esUrl,
                ES_USER: esUser,
                ES_PASSWORD: esPassword
              }
            }
          }
        }, null, 2);

      case 'cline':
        return JSON.stringify({
          mcpServers: {
            moltler: {
              command: "python",
              args: ["<path-to-moltler>/mcp-bridge/moltler_mcp_server.py"],
              env: {
                ES_URL: esUrl,
                ES_USER: esUser,
                ES_PASSWORD: esPassword
              }
            }
          }
        }, null, 2);

      case 'zed':
        return JSON.stringify({
          context_servers: {
            moltler: {
              command: {
                path: "python",
                args: ["<path-to-moltler>/mcp-bridge/moltler_mcp_server.py"],
                env: {
                  ES_URL: esUrl,
                  ES_USER: esUser,
                  ES_PASSWORD: esPassword
                }
              }
            }
          }
        }, null, 2);

      default:
        return '';
    }
  };

  const getHttpConfig = (): string => {
    const authHeader = btoa(`${esUser}:${esPassword}`);
    return JSON.stringify({
      mcpServers: {
        moltler: {
          url: `${esUrl}/_escript/mcp`,
          headers: {
            Authorization: `Basic ${authHeader}`
          }
        }
      }
    }, null, 2);
  };

  const client = CLIENTS[selectedClient];
  const config = getConfig(selectedClient);

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-950 text-white">
      {/* Header */}
      <header className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-3">
            <div className="w-10 h-10 bg-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-xl">⚡</span>
            </div>
            <span className="text-xl font-bold">Moltler</span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link href="/skills" className="hover:text-purple-400 transition">Skills</Link>
            <Link href="/packs" className="hover:text-purple-400 transition">Packs</Link>
            <Link href="/connect" className="text-purple-400 font-semibold">Connect</Link>
            <Link href="/docs" className="hover:text-purple-400 transition">Docs</Link>
          </nav>
        </div>
      </header>

      {/* Hero */}
      <section className="py-12 px-4">
        <div className="container mx-auto max-w-4xl text-center">
          <h1 className="text-3xl md:text-4xl font-bold mb-3">
            Connect Your AI Assistant
          </h1>
          <p className="text-gray-400 max-w-2xl mx-auto">
            Add Moltler skills to your favorite AI tools. Select your client below to get the configuration snippet.
          </p>
        </div>
      </section>

      {/* Main Content */}
      <section className="px-4 pb-16">
        <div className="container mx-auto max-w-4xl">
          {/* Client Selector */}
          <div className="mb-8">
            <h2 className="text-lg font-semibold mb-4">1. Select your AI client</h2>
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3">
              {(Object.keys(CLIENTS) as Client[]).map((key) => (
                <button
                  key={key}
                  onClick={() => setSelectedClient(key)}
                  className={`p-4 rounded-xl border text-center transition ${
                    selectedClient === key
                      ? 'bg-purple-600 border-purple-500'
                      : 'bg-gray-800 border-gray-700 hover:border-purple-500'
                  }`}
                >
                  <div className="text-2xl mb-1">{CLIENTS[key].icon}</div>
                  <div className="text-sm font-medium">{CLIENTS[key].name}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Elasticsearch Config */}
          <div className="mb-8 bg-gray-800 rounded-xl border border-gray-700 p-6">
            <h2 className="text-lg font-semibold mb-4">2. Configure your Elasticsearch connection</h2>
            <div className="grid md:grid-cols-3 gap-4">
              <div>
                <label className="text-sm text-gray-400 block mb-2">Elasticsearch URL</label>
                <input
                  type="text"
                  value={esUrl}
                  onChange={(e) => setEsUrl(e.target.value)}
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-2 focus:border-purple-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="text-sm text-gray-400 block mb-2">Username</label>
                <input
                  type="text"
                  value={esUser}
                  onChange={(e) => setEsUser(e.target.value)}
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-2 focus:border-purple-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="text-sm text-gray-400 block mb-2">Password</label>
                <input
                  type="password"
                  value={esPassword}
                  onChange={(e) => setEsPassword(e.target.value)}
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-2 focus:border-purple-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Config Output */}
          <div className="mb-8 bg-gray-800 rounded-xl border border-gray-700 p-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-lg font-semibold">3. Copy configuration for {client.name}</h2>
                {client.configPath && (
                  <p className="text-sm text-gray-400 mt-1">
                    Add to: <code className="bg-gray-900 px-2 py-0.5 rounded">{client.configPath}</code>
                  </p>
                )}
              </div>
              <button
                onClick={() => copyToClipboard(config, 'config')}
                className="px-4 py-2 bg-purple-600 hover:bg-purple-500 rounded-lg font-medium transition"
              >
                {copied === 'config' ? '✓ Copied!' : 'Copy'}
              </button>
            </div>
            <pre className="bg-gray-900 rounded-lg p-4 overflow-x-auto">
              <code className="text-green-400 text-sm">{config}</code>
            </pre>

            <div className="mt-4 p-4 bg-blue-900/20 border border-blue-700/50 rounded-lg">
              <p className="text-sm text-blue-300">
                <strong>Note:</strong> Replace <code className="bg-gray-900 px-1 rounded">&lt;path-to-moltler&gt;</code> with the actual path where you cloned the Moltler repository.
              </p>
            </div>
          </div>

          {/* Alternative HTTP Config */}
          <div className="mb-8 bg-gray-800 rounded-xl border border-gray-700 p-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-lg font-semibold">Alternative: Direct HTTP Connection</h2>
                <p className="text-sm text-gray-400 mt-1">
                  If your client supports HTTP MCP endpoints
                </p>
              </div>
              <button
                onClick={() => copyToClipboard(getHttpConfig(), 'http')}
                className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg font-medium transition"
              >
                {copied === 'http' ? '✓ Copied!' : 'Copy'}
              </button>
            </div>
            <pre className="bg-gray-900 rounded-lg p-4 overflow-x-auto">
              <code className="text-green-400 text-sm">{getHttpConfig()}</code>
            </pre>
          </div>

          {/* Next Steps */}
          <div className="bg-gradient-to-r from-purple-900/30 to-gray-800 rounded-xl border border-purple-700/50 p-6">
            <h2 className="text-lg font-semibold mb-4">4. After connecting</h2>
            <ol className="space-y-3 text-gray-300">
              <li className="flex gap-3">
                <span className="flex-shrink-0 w-6 h-6 bg-purple-600 rounded-full flex items-center justify-center text-sm">1</span>
                <span>Restart your {client.name} application</span>
              </li>
              <li className="flex gap-3">
                <span className="flex-shrink-0 w-6 h-6 bg-purple-600 rounded-full flex items-center justify-center text-sm">2</span>
                <span>Look for "moltler" in your MCP servers list</span>
              </li>
              <li className="flex gap-3">
                <span className="flex-shrink-0 w-6 h-6 bg-purple-600 rounded-full flex items-center justify-center text-sm">3</span>
                <span>Ask your AI: "What Moltler skills are available?"</span>
              </li>
            </ol>

            <div className="mt-6 p-4 bg-gray-900/50 rounded-lg">
              <p className="text-sm text-purple-300 mb-2">Example prompts to try:</p>
              <ul className="text-sm text-gray-400 space-y-1">
                <li>"Show me recent errors from my logs"</li>
                <li>"Check the health of my Elasticsearch cluster"</li>
                <li>"Find slow API transactions in the last hour"</li>
                <li>"List all available security detection rules"</li>
              </ul>
            </div>
          </div>

          {/* Requirements */}
          <div className="mt-8 p-6 bg-gray-800 rounded-xl border border-gray-700">
            <h2 className="text-lg font-semibold mb-4">Requirements</h2>
            <div className="grid md:grid-cols-2 gap-4 text-sm">
              <div className="flex items-start gap-3">
                <span className="text-green-400">✓</span>
                <div>
                  <strong>Elasticsearch</strong>
                  <p className="text-gray-400">With the Moltler plugin installed</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <span className="text-green-400">✓</span>
                <div>
                  <strong>Python 3.8+</strong>
                  <p className="text-gray-400">For the MCP bridge server</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <span className="text-green-400">✓</span>
                <div>
                  <strong>httpx</strong>
                  <p className="text-gray-400"><code>pip install httpx</code></p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <span className="text-green-400">✓</span>
                <div>
                  <strong>Skills installed</strong>
                  <p className="text-gray-400"><code>./moltler-cli.sh install --all</code></p>
                </div>
              </div>
            </div>
          </div>

          {/* Links */}
          <div className="mt-8 flex flex-wrap justify-center gap-4">
            <a
              href="https://github.com/bahaaldine/moltler"
              target="_blank"
              rel="noopener noreferrer"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg transition flex items-center gap-2"
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
              View on GitHub
            </a>
            <Link
              href="/skills"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg transition"
            >
              Browse 180+ Skills
            </Link>
            <Link
              href="/docs"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg transition"
            >
              Full Documentation
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-gray-800 py-8 px-4">
        <div className="container mx-auto max-w-4xl flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-lg">⚡</span>
            </div>
            <span className="font-bold">Moltler</span>
          </div>
          <div className="text-sm text-gray-500">
            © 2026 Moltler. Open Source.
          </div>
        </div>
      </footer>
    </div>
  );
}

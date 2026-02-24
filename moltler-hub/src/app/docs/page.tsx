'use client';

import Link from 'next/link';

export default function DocsPage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-950 text-white">
      {/* Header */}
      <header className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-3">
            <div className="w-10 h-10 bg-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-xl">⚡</span>
            </div>
            <span className="text-xl font-bold">MoltlerHub</span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link href="/skills" className="hover:text-purple-400 transition">Browse Skills</Link>
            <Link href="/docs" className="text-purple-400">Docs</Link>
            <a
              href="https://github.com/bahaaldine/moltler"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-purple-400 transition"
            >
              GitHub
            </a>
          </nav>
        </div>
      </header>

      <div className="container mx-auto px-4 py-12 max-w-4xl">
        <h1 className="text-4xl font-bold mb-8">Getting Started with Moltler</h1>

        {/* Installation Paths */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold mb-4">Choose Your Installation Path</h2>
          
          <div className="grid md:grid-cols-3 gap-6">
            <div className="p-6 bg-gray-800 rounded-lg border border-gray-700">
              <div className="text-3xl mb-3">🚀</div>
              <h3 className="text-xl font-semibold mb-2">Quick Start</h3>
              <p className="text-gray-400 mb-4">
                Try Moltler in 5 minutes. Runs a local Elasticsearch cluster with everything pre-configured.
              </p>
              <p className="text-sm text-gray-500 mb-4">Best for: Demo, evaluation, development</p>
              <a href="#quick-start" className="text-purple-400 hover:text-purple-300">
                Get started →
              </a>
            </div>
            
            <div className="p-6 bg-gray-800 rounded-lg border border-gray-700">
              <div className="text-3xl mb-3">🏢</div>
              <h3 className="text-xl font-semibold mb-2">Existing Cluster</h3>
              <p className="text-gray-400 mb-4">
                Install the plugin on your production Elasticsearch cluster. Download pre-built releases.
              </p>
              <p className="text-sm text-gray-500 mb-4">Best for: Production, enterprise</p>
              <a href="#existing-cluster" className="text-purple-400 hover:text-purple-300">
                Install on existing cluster →
              </a>
            </div>

            <div className="p-6 bg-gradient-to-br from-purple-900/50 to-gray-800 rounded-lg border border-purple-600/50">
              <div className="text-3xl mb-3">🤖</div>
              <h3 className="text-xl font-semibold mb-2">Connect to Cursor</h3>
              <p className="text-gray-400 mb-4">
                Add Moltler skills to Cursor IDE. Your AI assistant gets access to all 180+ skills.
              </p>
              <p className="text-sm text-gray-500 mb-4">Best for: AI-assisted workflows</p>
              <a href="#cursor-setup" className="text-purple-400 hover:text-purple-300">
                Connect Cursor →
              </a>
            </div>
          </div>
        </section>

        {/* Quick Start */}
        <section id="quick-start" className="mb-12">
          <h2 className="text-2xl font-semibold mb-4">Quick Start (5 Minutes)</h2>
          
          <div className="space-y-6">
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">One Command Setup</h3>
              <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
                <code className="text-green-400">{`git clone --recurse-submodules https://github.com/bahaaldine/moltler.git
cd moltler && ./scripts/quick-start.sh`}</code>
              </pre>
              <p className="text-gray-400 text-sm mt-3">
                Builds the plugin, starts Elasticsearch, and you're ready to go.
              </p>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">Install Skills</h3>
              <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
                <code className="text-green-400">{`cd hub && ./moltler-cli.sh install --all`}</code>
              </pre>
              <p className="text-gray-400 text-sm mt-3">
                Installs all 155+ skills into your cluster
              </p>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">Run Your First Skill</h3>
              <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
                <code className="text-green-400">{`curl -u elastic-admin:elastic-password http://localhost:9200/_escript \\
  -H "Content-Type: application/json" \\
  -d '{"query": "RUN SKILL get_recent_errors()"}'`}</code>
              </pre>
            </div>
          </div>
        </section>

        {/* Existing Cluster */}
        <section id="existing-cluster" className="mb-12">
          <h2 className="text-2xl font-semibold mb-4">Install on Existing Cluster</h2>
          
          <div className="space-y-6">
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">1. Download the Plugin</h3>
              <p className="text-gray-400 mb-4">
                Download the pre-built plugin zip that matches your Elasticsearch version:
              </p>
              <div className="bg-gray-900 p-4 rounded">
                <a 
                  href="https://github.com/bahaaldine/moltler/releases"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-purple-400 hover:text-purple-300 flex items-center gap-2"
                >
                  📦 View Releases on GitHub →
                </a>
              </div>
              <div className="mt-4">
                <p className="text-gray-400 text-sm mb-2">Available versions:</p>
                <div className="grid grid-cols-4 gap-2 text-sm">
                  {['9.4.0', '9.3.0', '9.2.0', '9.1.0', '9.0.0', '8.17.0', '8.16.0', '8.15.0'].map((v) => (
                    <span key={v} className="bg-gray-700 px-2 py-1 rounded text-center">ES {v}</span>
                  ))}
                </div>
              </div>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">2. Install on Each Node</h3>
              <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
                <code className="text-green-400">{`# Download the plugin (example for ES 9.4.0)
wget https://github.com/bahaaldine/moltler/releases/download/v1.0.0/elastic-script-9.4.0.zip

# Install the plugin
sudo /usr/share/elasticsearch/bin/elasticsearch-plugin install \\
  file:///path/to/elastic-script-9.4.0.zip

# Restart Elasticsearch
sudo systemctl restart elasticsearch`}</code>
              </pre>
              <p className="text-gray-400 text-sm mt-3">
                Repeat on each node in your cluster
              </p>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">3. Configure CLI for Your Cluster</h3>
              <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
                <code className="text-green-400">{`git clone https://github.com/bahaaldine/moltler.git
cd moltler/hub

# Configure for your cluster
export ES_URL="https://your-cluster:9200"
export ES_USER="elastic"
export ES_PASSWORD="your-password"

# Install skills
./moltler-cli.sh install --all`}</code>
              </pre>
            </div>
          </div>
        </section>

        {/* Architecture */}
        <section id="architecture" className="mb-12">
          <h2 className="text-2xl font-semibold mb-4">Architecture</h2>
          
          <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
            <pre className="text-sm text-gray-300 overflow-x-auto">
{`┌─────────────────────────────────────────────────────────────────┐
│                        USER / AI AGENT                          │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  MoltlerHub   │    │  Moltler CLI  │    │  Moltler MCP  │
│  (Web Portal) │    │  (Terminal)   │    │  (AI Bridge)  │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│               Elasticsearch + elastic-script plugin             │
│                        (Skills Runtime)                         │
└─────────────────────────────────────────────────────────────────┘`}
            </pre>
            
            <div className="mt-6 grid md:grid-cols-2 gap-4">
              <div className="p-4 bg-gray-900 rounded">
                <h4 className="font-semibold mb-2">elastic-script plugin</h4>
                <p className="text-sm text-gray-400">
                  Runs inside Elasticsearch. Executes skills on your data.
                </p>
                <span className="text-xs text-green-400">Required</span>
              </div>
              <div className="p-4 bg-gray-900 rounded">
                <h4 className="font-semibold mb-2">Moltler CLI</h4>
                <p className="text-sm text-gray-400">
                  Command-line tool to install and manage skills.
                </p>
                <span className="text-xs text-green-400">Required</span>
              </div>
              <div className="p-4 bg-gray-900 rounded">
                <h4 className="font-semibold mb-2">MoltlerHub</h4>
                <p className="text-sm text-gray-400">
                  Web portal to browse, search, and discover skills.
                </p>
                <span className="text-xs text-gray-400">Optional</span>
              </div>
              <div className="p-4 bg-gray-900 rounded">
                <h4 className="font-semibold mb-2">Moltler MCP</h4>
                <p className="text-sm text-gray-400">
                  Bridge for AI agents (Claude, Cursor, etc.) to use skills.
                </p>
                <span className="text-xs text-gray-400">Optional</span>
              </div>
            </div>
          </div>
        </section>

        {/* Connect to Cursor */}
        <section id="cursor-setup" className="mb-12">
          <h2 className="text-2xl font-semibold mb-4">Connect to Cursor IDE</h2>
          
          <div className="bg-gradient-to-r from-purple-900/20 to-gray-800 rounded-lg p-6 border border-purple-600/30 mb-6">
            <p className="text-gray-300">
              <strong className="text-purple-300">One config, all skills.</strong> When you connect Cursor to Moltler, 
              your AI assistant automatically gets access to all 180+ skills. No need to install skills individually.
            </p>
          </div>

          <div className="space-y-6">
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">Step 1: Create MCP Config File</h3>
              <p className="text-gray-400 text-sm mb-3">
                Create <code className="bg-gray-900 px-1 rounded">.cursor/mcp.json</code> in your project root (or global config):
              </p>
              <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
                <code className="text-green-400">{`{
  "mcpServers": {
    "moltler": {
      "command": "python",
      "args": ["/path/to/moltler/mcp-bridge/moltler_mcp_server.py"],
      "env": {
        "ES_URL": "http://localhost:9200",
        "ES_USER": "elastic-admin",
        "ES_PASSWORD": "elastic-password"
      }
    }
  }
}`}</code>
              </pre>
              <p className="text-gray-400 text-sm mt-3">
                Update the path and credentials for your environment.
              </p>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">Step 2: Install Dependencies</h3>
              <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
                <code className="text-green-400">{`pip install httpx`}</code>
              </pre>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">Step 3: Restart Cursor</h3>
              <p className="text-gray-400">
                Restart Cursor IDE. The Moltler MCP server will appear in your MCP servers list.
                Your AI assistant now has access to all skills.
              </p>
            </div>

            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <h3 className="font-semibold mb-3">Alternative: Direct HTTP (if supported)</h3>
              <p className="text-gray-400 text-sm mb-3">
                If your MCP client supports HTTP endpoints directly:
              </p>
              <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
                <code className="text-green-400">{`{
  "mcpServers": {
    "moltler": {
      "url": "http://localhost:9200/_escript/mcp",
      "headers": {
        "Authorization": "Basic ZWxhc3RpYy1hZG1pbjplbGFzdGljLXBhc3N3b3Jk"
      }
    }
  }
}`}</code>
              </pre>
            </div>
          </div>
        </section>

        {/* Connect AI Agents */}
        <section id="ai-agents" className="mb-12">
          <h2 className="text-2xl font-semibold mb-4">What Your AI Can Do</h2>
          
          <div className="space-y-6">
            <div className="bg-gradient-to-r from-purple-900/30 to-gray-800 rounded-lg p-6 border border-purple-700/50">
              <h4 className="font-semibold mb-4 text-purple-300">The Magic Moment</h4>
              <div className="space-y-4 text-sm">
                <div className="flex gap-3">
                  <span className="text-blue-400 font-medium">You:</span>
                  <span className="text-gray-300">"I got paged for a production incident. Payment service seems down."</span>
                </div>
                <div className="flex gap-3">
                  <span className="text-green-400 font-medium">Agent:</span>
                  <span className="text-gray-400 italic">Uses get_recent_errors skill...</span>
                </div>
                <div className="bg-gray-900/50 p-3 rounded text-gray-300">
                  "I found 47 errors in payment-service. Most are 'Connection timeout to gateway'. Let me check transaction latency..."
                </div>
                <div className="flex gap-3">
                  <span className="text-green-400 font-medium">Agent:</span>
                  <span className="text-gray-400 italic">Uses get_slow_transactions skill...</span>
                </div>
                <div className="bg-gray-900/50 p-3 rounded text-gray-300">
                  "Payment gateway calls averaging 12s (10x normal). Issue started at 9:45 AM. Want me to correlate with recent deployments?"
                </div>
              </div>
              <p className="text-purple-300 text-sm mt-4">
                Your AI assistant can now investigate your Elasticsearch data like a senior engineer.
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-4">
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <div className="text-2xl mb-2">🔍</div>
                <h4 className="font-semibold mb-1">Observability</h4>
                <p className="text-sm text-gray-400">Query logs, analyze errors, find slow services</p>
              </div>
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <div className="text-2xl mb-2">🛡️</div>
                <h4 className="font-semibold mb-1">Security</h4>
                <p className="text-sm text-gray-400">Detect threats, investigate incidents, manage alerts</p>
              </div>
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <div className="text-2xl mb-2">⚙️</div>
                <h4 className="font-semibold mb-1">Cluster Ops</h4>
                <p className="text-sm text-gray-400">Health checks, index management, ILM policies</p>
              </div>
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <div className="text-2xl mb-2">🎯</div>
                <h4 className="font-semibold mb-1">Kibana</h4>
                <p className="text-sm text-gray-400">Dashboards, alerts, cases, Fleet, ML jobs</p>
              </div>
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <div className="text-2xl mb-2">🤖</div>
                <h4 className="font-semibold mb-1">AI/LLM</h4>
                <p className="text-sm text-gray-400">Summarize, classify, generate with inference API</p>
              </div>
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <div className="text-2xl mb-2">🔧</div>
                <h4 className="font-semibold mb-1">Runbooks</h4>
                <p className="text-sm text-gray-400">Slack, PagerDuty, AWS, Kubernetes, Terraform</p>
              </div>
            </div>

            <p className="text-gray-400 text-sm">
              Works with: <span className="text-purple-400">Cursor</span>, <span className="text-purple-400">Claude Desktop</span>, <span className="text-purple-400">Cline</span>, <span className="text-purple-400">Windsurf</span>, and any MCP-compatible agent.
            </p>
          </div>
        </section>

        {/* Next Steps */}
        <section className="mb-12">
          <h2 className="text-2xl font-semibold mb-4">Next Steps</h2>
          
          <div className="grid md:grid-cols-2 gap-4">
            <Link 
              href="/skills" 
              className="p-6 bg-gray-800 rounded-lg border border-gray-700 hover:border-purple-500 transition"
            >
              <h3 className="font-semibold mb-2">Browse Skills →</h3>
              <p className="text-sm text-gray-400">Discover 155+ community-built skills</p>
            </Link>
            <a 
              href="https://bahaaldine.github.io/moltler/skills/creating-skills/"
              target="_blank"
              rel="noopener noreferrer"
              className="p-6 bg-gray-800 rounded-lg border border-gray-700 hover:border-purple-500 transition"
            >
              <h3 className="font-semibold mb-2">Create Your Own →</h3>
              <p className="text-sm text-gray-400">Build and publish your own skills</p>
            </a>
            <a 
              href="https://bahaaldine.github.io/moltler/"
              target="_blank"
              rel="noopener noreferrer"
              className="p-6 bg-gray-800 rounded-lg border border-gray-700 hover:border-purple-500 transition"
            >
              <h3 className="font-semibold mb-2">Full Documentation →</h3>
              <p className="text-sm text-gray-400">Complete guides and reference</p>
            </a>
            <a 
              href="https://github.com/bahaaldine/moltler/blob/main/hub/CONTRIBUTING.md"
              target="_blank"
              rel="noopener noreferrer"
              className="p-6 bg-gray-800 rounded-lg border border-gray-700 hover:border-purple-500 transition"
            >
              <h3 className="font-semibold mb-2">Contribute →</h3>
              <p className="text-sm text-gray-400">Add your skill to MoltlerHub</p>
            </a>
          </div>
        </section>
      </div>

      {/* Footer */}
      <footer className="border-t border-gray-800 py-8 px-4">
        <div className="container mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-lg">⚡</span>
            </div>
            <span className="font-bold">MoltlerHub</span>
          </div>
          <div className="flex items-center gap-6 text-sm text-gray-400">
            <a href="https://github.com/bahaaldine/moltler" target="_blank" rel="noopener noreferrer" className="hover:text-white transition">
              GitHub
            </a>
            <a href="https://bahaaldine.github.io/moltler/" target="_blank" rel="noopener noreferrer" className="hover:text-white transition">
              Documentation
            </a>
          </div>
          <div className="text-sm text-gray-500">
            © 2026 Moltler. Open Source.
          </div>
        </div>
      </footer>
    </div>
  );
}

'use client';

import Link from 'next/link';
import { useState } from 'react';
import { SAMPLE_SKILLS, CATEGORIES } from '@/lib/skills';

export default function Home() {
  const [esUrl, setEsUrl] = useState('http://localhost:9200');
  const [copied, setCopied] = useState<string | null>(null);
  const totalSkills = 155;

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopied(id);
    setTimeout(() => setCopied(null), 2000);
  };

  const installCommand = `curl -sSL https://hub.moltler.dev/install.sh | bash -s -- --es-url ${esUrl}`;
  
  const cursorConfig = `{
  "mcpServers": {
    "moltler": {
      "url": "${esUrl}/_escript/mcp",
      "headers": {
        "Authorization": "Basic YOUR_BASE64_CREDENTIALS"
      }
    }
  }
}`;

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
            <Link href="/docs" className="hover:text-purple-400 transition">Docs</Link>
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

      {/* Hero - Focused on existing ES users */}
      <section className="py-16 px-4">
        <div className="container mx-auto text-center max-w-4xl">
          <div className="inline-block px-4 py-1 bg-purple-600/20 text-purple-400 rounded-full text-sm mb-6">
            For Elasticsearch Users
          </div>
          <h1 className="text-5xl font-bold mb-6">
            Supercharge Your Elasticsearch with{' '}
            <span className="text-purple-400">AI-Ready Skills</span>
          </h1>
          <p className="text-xl text-gray-400 mb-4">
            You already have Elasticsearch. Now give your AI agents the power to query it.
          </p>
          <p className="text-lg text-gray-500 mb-8">
            {totalSkills}+ pre-built skills for Observability, Security, and Search.
            <br />
            Works with Cursor, Claude, Cline, and any MCP-compatible agent.
          </p>
        </div>
      </section>

      {/* Interactive Getting Started */}
      <section className="py-8 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-4xl">
          <h2 className="text-2xl font-bold text-center mb-8">Get Started in 3 Steps</h2>
          
          {/* Step 1: Install Plugin */}
          <div className="mb-8">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-8 h-8 bg-purple-600 rounded-full flex items-center justify-center font-bold">1</div>
              <h3 className="text-xl font-semibold">Install the Plugin</h3>
            </div>
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700 ml-11">
              <p className="text-gray-400 mb-4">
                Download from{' '}
                <a href="https://github.com/bahaaldine/moltler/releases" target="_blank" rel="noopener noreferrer" className="text-purple-400 hover:text-purple-300">
                  GitHub Releases
                </a>
                {' '}and install on each node:
              </p>
              <div className="bg-gray-900 rounded p-4 relative">
                <pre className="text-sm text-green-400 overflow-x-auto">
{`elasticsearch-plugin install file:///path/to/x-pack-escript.zip
systemctl restart elasticsearch`}
                </pre>
              </div>
            </div>
          </div>

          {/* Step 2: Install Skills */}
          <div className="mb-8">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-8 h-8 bg-purple-600 rounded-full flex items-center justify-center font-bold">2</div>
              <h3 className="text-xl font-semibold">Install Skills</h3>
            </div>
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700 ml-11">
              <p className="text-gray-400 mb-4">Enter your Elasticsearch URL:</p>
              <input
                type="text"
                value={esUrl}
                onChange={(e) => setEsUrl(e.target.value)}
                className="w-full bg-gray-900 border border-gray-700 rounded px-4 py-2 mb-4 focus:border-purple-500 focus:outline-none"
                placeholder="http://localhost:9200"
              />
              <div className="bg-gray-900 rounded p-4 relative">
                <pre className="text-sm text-green-400 overflow-x-auto pr-20">{installCommand}</pre>
                <button
                  onClick={() => copyToClipboard(installCommand, 'install')}
                  className="absolute top-3 right-3 px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-xs transition"
                >
                  {copied === 'install' ? '✓ Copied' : 'Copy'}
                </button>
              </div>
              <p className="text-gray-500 text-sm mt-3">
                This installs all {totalSkills}+ skills into your cluster.
              </p>
            </div>
          </div>

          {/* Step 3: Connect Agent */}
          <div className="mb-8">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-8 h-8 bg-purple-600 rounded-full flex items-center justify-center font-bold">3</div>
              <h3 className="text-xl font-semibold">Connect Your AI Agent</h3>
            </div>
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700 ml-11">
              <div className="flex gap-2 mb-4">
                <span className="px-3 py-1 bg-purple-600/20 text-purple-400 rounded text-sm">Cursor</span>
                <span className="px-3 py-1 bg-gray-700 text-gray-400 rounded text-sm">Claude</span>
                <span className="px-3 py-1 bg-gray-700 text-gray-400 rounded text-sm">Cline</span>
              </div>
              <p className="text-gray-400 mb-4">
                Add to <code className="bg-gray-900 px-2 py-0.5 rounded">.cursor/mcp.json</code>:
              </p>
              <div className="bg-gray-900 rounded p-4 relative">
                <pre className="text-sm text-green-400 overflow-x-auto pr-20">{cursorConfig}</pre>
                <button
                  onClick={() => copyToClipboard(cursorConfig, 'cursor')}
                  className="absolute top-3 right-3 px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-xs transition"
                >
                  {copied === 'cursor' ? '✓ Copied' : 'Copy'}
                </button>
              </div>
              <p className="text-gray-500 text-sm mt-3">
                Restart your agent, then start asking questions about your data.
              </p>
            </div>
          </div>

          {/* Done! */}
          <div className="text-center py-8">
            <div className="inline-flex items-center gap-2 px-6 py-3 bg-green-600/20 text-green-400 rounded-lg">
              <span className="text-xl">✓</span>
              <span className="font-medium">You're ready! Start asking your AI agent about your Elasticsearch data.</span>
            </div>
          </div>
        </div>
      </section>

      {/* The Magic Moment */}
      <section className="py-16 px-4">
        <div className="container mx-auto max-w-4xl">
          <h2 className="text-2xl font-bold text-center mb-8">What It Looks Like</h2>
          
          <div className="bg-gradient-to-r from-purple-900/30 to-gray-800 rounded-xl p-8 border border-purple-700/50">
            <div className="space-y-6">
              <div className="flex gap-4">
                <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center shrink-0">
                  <span>👤</span>
                </div>
                <div className="bg-gray-800 rounded-lg p-4 flex-1">
                  <p className="text-gray-200">"I got paged for a production incident. The payment service is throwing errors."</p>
                </div>
              </div>
              
              <div className="flex gap-4">
                <div className="w-10 h-10 bg-purple-600 rounded-full flex items-center justify-center shrink-0">
                  <span>🤖</span>
                </div>
                <div className="flex-1 space-y-3">
                  <div className="text-sm text-purple-400 italic">Using skill: get_recent_errors</div>
                  <div className="bg-gray-800 rounded-lg p-4">
                    <p className="text-gray-200">I found <strong>47 errors</strong> in payment-service in the last hour. The most common error is:</p>
                    <pre className="mt-2 text-sm bg-gray-900 p-3 rounded text-red-400">Connection timeout to payment gateway (35 occurrences)</pre>
                    <p className="text-gray-400 mt-3">Let me check transaction latency...</p>
                  </div>
                  <div className="text-sm text-purple-400 italic">Using skill: get_slow_transactions</div>
                  <div className="bg-gray-800 rounded-lg p-4">
                    <p className="text-gray-200">Payment gateway calls are averaging <strong>12.3 seconds</strong> (normally 1.2s). The latency spike started at <strong>9:45 AM</strong>.</p>
                    <p className="text-gray-400 mt-3">This correlates with the timeout errors. Would you like me to check for related deployments or infrastructure changes?</p>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="mt-8 pt-6 border-t border-purple-700/30 text-center">
              <p className="text-purple-300">
                Your AI agent investigates your Elasticsearch data like a senior engineer.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Skills by Solution */}
      <section className="py-16 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-5xl">
          <h2 className="text-2xl font-bold text-center mb-8">Skills for Every Use Case</h2>
          
          <div className="grid md:grid-cols-3 gap-6">
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <div className="text-3xl mb-3">📊</div>
              <h3 className="text-xl font-semibold mb-2">Observability</h3>
              <p className="text-gray-400 text-sm mb-4">Investigate incidents, analyze logs, monitor services</p>
              <ul className="text-sm space-y-2 text-gray-300">
                <li>• get_recent_errors</li>
                <li>• get_slow_transactions</li>
                <li>• correlate_logs</li>
                <li>• count_logs_by_level</li>
              </ul>
              <Link href="/skills?category=observability" className="text-purple-400 text-sm mt-4 block hover:text-purple-300">
                View all →
              </Link>
            </div>
            
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <div className="text-3xl mb-3">🛡️</div>
              <h3 className="text-xl font-semibold mb-2">Security</h3>
              <p className="text-gray-400 text-sm mb-4">Hunt threats, investigate alerts, assess risk</p>
              <ul className="text-sm space-y-2 text-gray-300">
                <li>• hunt_ioc</li>
                <li>• get_risky_users</li>
                <li>• get_failed_logins</li>
                <li>• search_security_events</li>
              </ul>
              <Link href="/skills?category=security" className="text-purple-400 text-sm mt-4 block hover:text-purple-300">
                View all →
              </Link>
            </div>
            
            <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
              <div className="text-3xl mb-3">🔍</div>
              <h3 className="text-xl font-semibold mb-2">Search</h3>
              <p className="text-gray-400 text-sm mb-4">Query documents, aggregations, semantic search</p>
              <ul className="text-sm space-y-2 text-gray-300">
                <li>• semantic_search</li>
                <li>• fuzzy_search</li>
                <li>• top_values</li>
                <li>• get_search_analytics</li>
              </ul>
              <Link href="/skills?category=search" className="text-purple-400 text-sm mt-4 block hover:text-purple-300">
                View all →
              </Link>
            </div>
          </div>
          
          <div className="text-center mt-8">
            <Link
              href="/skills"
              className="inline-block px-6 py-3 bg-purple-600 hover:bg-purple-700 rounded-lg font-medium transition"
            >
              Browse All {totalSkills}+ Skills
            </Link>
          </div>
        </div>
      </section>

      {/* Build Your Own */}
      <section className="py-16 px-4">
        <div className="container mx-auto max-w-4xl text-center">
          <h2 className="text-2xl font-bold mb-4">Build Your Own Skills</h2>
          <p className="text-gray-400 mb-8">
            Know ES|QL? You already know how to build skills.
          </p>
          
          <div className="bg-gray-800 rounded-lg p-6 border border-gray-700 text-left max-w-2xl mx-auto">
            <pre className="text-sm overflow-x-auto">
              <code className="text-green-400">{`CREATE SKILL find_user_activity
  DESCRIPTION 'Find all activity for a user'
  (user_id STRING)
  RETURNS ARRAY
BEGIN
  RETURN ESQL_QUERY('
    FROM logs-* 
    | WHERE user.id == "' || user_id || '"
    | SORT @timestamp DESC
    | LIMIT 100
  ');
END SKILL;`}</code>
            </pre>
          </div>
          
          <div className="mt-6">
            <a
              href="https://bahaaldine.github.io/moltler/skills/creating-skills/"
              target="_blank"
              rel="noopener noreferrer"
              className="text-purple-400 hover:text-purple-300"
            >
              Learn more about creating skills →
            </a>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-16 px-4 bg-gradient-to-r from-purple-900/30 to-gray-900">
        <div className="container mx-auto text-center max-w-2xl">
          <h2 className="text-3xl font-bold mb-4">Ready to Augment Your Elasticsearch?</h2>
          <p className="text-gray-400 mb-8">
            Join Elasticsearch users who are letting AI agents query their data.
          </p>
          <div className="flex justify-center gap-4">
            <a
              href="#"
              onClick={(e) => { e.preventDefault(); window.scrollTo({ top: 0, behavior: 'smooth' }); }}
              className="px-6 py-3 bg-purple-600 hover:bg-purple-700 rounded-lg font-medium transition"
            >
              Get Started
            </a>
            <a
              href="https://github.com/bahaaldine/moltler"
              target="_blank"
              rel="noopener noreferrer"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 rounded-lg font-medium transition border border-gray-700"
            >
              View on GitHub
            </a>
          </div>
        </div>
      </section>

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
            <a href="https://github.com/bahaaldine/moltler/blob/main/hub/CONTRIBUTING.md" target="_blank" rel="noopener noreferrer" className="hover:text-white transition">
              Contribute
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

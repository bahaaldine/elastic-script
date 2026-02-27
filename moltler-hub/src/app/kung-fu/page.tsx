'use client';

import Link from 'next/link';
import { useState } from 'react';

export default function KungFuPage() {
  const [copied, setCopied] = useState(false);
  const repoUrl = 'https://github.com/bahaaldine/moltler.git';

  const copyToClipboard = () => {
    navigator.clipboard.writeText(repoUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="min-h-screen bg-black text-white overflow-hidden">
      {/* Matrix-style background */}
      <div className="fixed inset-0 opacity-10 pointer-events-none overflow-hidden">
        <div className="matrix-rain text-green-500 text-xs font-mono whitespace-pre leading-none">
          {Array(50).fill(0).map((_, i) => (
            <div key={i} className="inline-block animate-pulse" style={{ animationDelay: `${i * 0.1}s` }}>
              {Array(100).fill(0).map(() => String.fromCharCode(0x30A0 + Math.random() * 96)).join('')}
            </div>
          ))}
        </div>
      </div>

      {/* Header */}
      <header className="relative z-10 border-b border-green-900/50">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-3">
            <div className="w-10 h-10 bg-green-600 rounded-lg flex items-center justify-center">
              <span className="text-xl">⚡</span>
            </div>
            <span className="text-xl font-bold text-green-400">Moltler</span>
          </Link>
          <nav className="flex items-center gap-6 text-green-400">
            <Link href="/skills" className="hover:text-green-300 transition">Skills</Link>
            <Link href="/agent-skills" className="hover:text-green-300 transition">Agent Skills</Link>
            <a href={repoUrl} target="_blank" rel="noopener noreferrer" className="hover:text-green-300 transition">GitHub</a>
          </nav>
        </div>
      </header>

      {/* Hero */}
      <section className="relative z-10 py-20 px-4">
        <div className="container mx-auto text-center max-w-4xl">
          {/* Quote */}
          <div className="mb-8">
            <p className="text-green-500 text-lg italic mb-2">"I know kung fu."</p>
            <p className="text-green-700 text-sm">— Neo, The Matrix</p>
          </div>

          {/* Main headline */}
          <h1 className="text-5xl md:text-7xl font-bold mb-6 tracking-tight">
            <span className="text-green-400">320+ Elasticsearch Skills</span>
            <br />
            <span className="text-white">Downloaded to Your AI</span>
            <br />
            <span className="text-green-600">In 10 Seconds</span>
          </h1>

          <p className="text-xl text-gray-400 mb-12 max-w-2xl mx-auto">
            One GitHub URL. Zero configuration. Instant mastery of Elasticsearch, 
            observability, security, ML, and more.
          </p>

          {/* Installation */}
          <div className="bg-gray-900/80 backdrop-blur rounded-2xl p-8 border border-green-900/50 max-w-2xl mx-auto mb-12">
            <h2 className="text-green-400 text-sm uppercase tracking-wider mb-4">Installation (10 seconds)</h2>
            
            <ol className="text-left space-y-4 mb-6">
              <li className="flex items-start gap-3">
                <span className="bg-green-600 text-black w-6 h-6 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0">1</span>
                <span className="text-gray-300">Open Cursor Settings: <kbd className="bg-gray-800 px-2 py-0.5 rounded text-sm">Cmd+Shift+J</kbd></span>
              </li>
              <li className="flex items-start gap-3">
                <span className="bg-green-600 text-black w-6 h-6 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0">2</span>
                <span className="text-gray-300">Click <strong>Rules</strong> → <strong>Add Rule</strong> → <strong>Remote Rule (GitHub)</strong></span>
              </li>
              <li className="flex items-start gap-3">
                <span className="bg-green-600 text-black w-6 h-6 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0">3</span>
                <span className="text-gray-300">Paste this URL:</span>
              </li>
            </ol>

            <div className="bg-black rounded-lg p-4 flex items-center justify-between border border-green-800">
              <code className="text-green-400 font-mono">{repoUrl}</code>
              <button
                onClick={copyToClipboard}
                className="bg-green-600 hover:bg-green-500 text-black px-4 py-2 rounded font-semibold transition"
              >
                {copied ? '✓ Copied' : 'Copy'}
              </button>
            </div>

            <p className="text-green-600 text-sm mt-4">That's it. You know kung fu now.</p>
          </div>

          {/* Try it */}
          <div className="text-left max-w-2xl mx-auto">
            <h2 className="text-green-400 text-sm uppercase tracking-wider mb-4">Try These in Cursor Chat</h2>
            
            <div className="grid gap-3">
              {[
                { prompt: 'Check my cluster health', result: 'Runs ES_CLUSTER_HEALTH() with analysis' },
                { prompt: 'Find errors in the last hour', result: 'Writes and executes ES|QL query' },
                { prompt: 'What services are throwing exceptions?', result: 'Builds aggregation, analyzes results' },
                { prompt: 'Hunt for this IP: 10.0.0.50', result: 'Runs security threat hunt' },
                { prompt: 'Send a Slack alert about high CPU', result: 'Creates alert with Slack action' },
              ].map((item, i) => (
                <div key={i} className="bg-gray-900/50 rounded-lg p-4 border border-gray-800 hover:border-green-800 transition">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="text-green-500">→</span>
                    <code className="text-white font-mono text-sm">"{item.prompt}"</code>
                  </div>
                  <p className="text-gray-500 text-sm ml-6">{item.result}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Capabilities Grid */}
      <section className="relative z-10 py-16 px-4 bg-gray-950/80">
        <div className="container mx-auto max-w-5xl">
          <h2 className="text-2xl font-bold text-center mb-8 text-green-400">What You Just Learned</h2>
          
          <div className="grid md:grid-cols-3 gap-4">
            {[
              { icon: '📦', title: 'Document Operations', skills: 'Index, Get, Update, Delete, Bulk, Reindex' },
              { icon: '🔍', title: 'Search & Query', skills: 'ES|QL, Full-text, Aggregations, Vector' },
              { icon: '🖥️', title: 'Cluster Management', skills: 'Health, Stats, Nodes, Tasks, Settings' },
              { icon: '👁️', title: 'Observability', skills: 'Logs, Metrics, Traces, APM' },
              { icon: '🛡️', title: 'Security', skills: 'Users, Roles, API Keys, Threat Hunting' },
              { icon: '🤖', title: 'Machine Learning', skills: 'Anomaly Detection, Inference, Embeddings' },
              { icon: '💾', title: 'Data Management', skills: 'ILM, Snapshots, Data Streams, Pipelines' },
              { icon: '🔔', title: 'Alerting', skills: 'Rules, Connectors, Actions' },
              { icon: '🔗', title: 'Integrations', skills: 'Slack, PagerDuty, AWS, K8s, CI/CD' },
            ].map((cat, i) => (
              <div key={i} className="bg-gray-900 rounded-lg p-4 border border-gray-800">
                <div className="text-2xl mb-2">{cat.icon}</div>
                <h3 className="font-semibold text-white mb-1">{cat.title}</h3>
                <p className="text-gray-500 text-sm">{cat.skills}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="relative z-10 py-16 px-4">
        <div className="container mx-auto text-center max-w-2xl">
          <blockquote className="text-2xl text-green-400 italic mb-4">
            "Free your mind."
          </blockquote>
          <p className="text-green-700 mb-8">— Morpheus</p>
          
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link
              href="/skills"
              className="px-6 py-3 bg-green-600 hover:bg-green-500 text-black rounded-lg font-semibold transition"
            >
              Browse All Skills
            </Link>
            <a
              href={repoUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg font-semibold transition"
            >
              View on GitHub
            </a>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="relative z-10 border-t border-green-900/30 py-6 px-4">
        <div className="container mx-auto text-center text-green-800 text-sm">
          <p>Moltler — The skills framework for Elasticsearch</p>
        </div>
      </footer>
    </div>
  );
}

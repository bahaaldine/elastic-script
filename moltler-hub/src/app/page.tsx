'use client';

import Link from 'next/link';
import { useState } from 'react';
import { SKILL_PACKS, PACK_CATEGORIES } from '@/lib/packs';
import validatedSkills from '@/data/validated_skills.json';

export default function Home() {
  const [esUrl, setEsUrl] = useState('http://localhost:9200');
  const [copied, setCopied] = useState<string | null>(null);
  const [activeStep, setActiveStep] = useState<number | null>(null);
  
  // Get validation stats from manifest
  const validationData = validatedSkills as { total_skills: number; valid_skills: number; skills: Record<string, unknown> };
  const totalSkills = validationData.total_skills || 155;
  const validatedCount = validationData.valid_skills || 155;
  const validationRate = totalSkills > 0 ? Math.round((validatedCount / totalSkills) * 100) : 100;
  const totalPacks = SKILL_PACKS.length;

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

  const steps = [
    {
      num: 1,
      title: 'Install Plugin',
      summary: 'Download & install on ES nodes',
      content: (
        <div className="space-y-3">
          <p className="text-gray-400 text-sm">
            Download from{' '}
            <a href="https://github.com/bahaaldine/moltler/releases" target="_blank" rel="noopener noreferrer" className="text-purple-400 hover:text-purple-300">
              GitHub Releases
            </a>
          </p>
          <div className="bg-gray-900 rounded p-3 text-sm">
            <pre className="text-green-400 overflow-x-auto">{`elasticsearch-plugin install file:///path/to/x-pack-escript.zip
systemctl restart elasticsearch`}</pre>
          </div>
        </div>
      )
    },
    {
      num: 2,
      title: 'Install Skills',
      summary: 'One command installs all skills',
      content: (
        <div className="space-y-3">
          <div className="flex gap-2">
            <input
              type="text"
              value={esUrl}
              onChange={(e) => setEsUrl(e.target.value)}
              className="flex-1 bg-gray-900 border border-gray-700 rounded px-3 py-1.5 text-sm focus:border-purple-500 focus:outline-none"
              placeholder="http://localhost:9200"
            />
          </div>
          <div className="bg-gray-900 rounded p-3 relative">
            <pre className="text-green-400 text-sm overflow-x-auto pr-16">{installCommand}</pre>
            <button
              onClick={() => copyToClipboard(installCommand, 'install')}
              className="absolute top-2 right-2 px-2 py-1 bg-gray-700 hover:bg-gray-600 rounded text-xs"
            >
              {copied === 'install' ? '✓' : 'Copy'}
            </button>
          </div>
        </div>
      )
    },
    {
      num: 3,
      title: 'Connect Agent',
      summary: 'Add MCP config to your AI',
      content: (
        <div className="space-y-3">
          <div className="flex gap-2 text-xs">
            <span className="px-2 py-0.5 bg-purple-600/20 text-purple-400 rounded">Cursor</span>
            <span className="px-2 py-0.5 bg-gray-700 text-gray-400 rounded">Claude</span>
            <span className="px-2 py-0.5 bg-gray-700 text-gray-400 rounded">Cline</span>
          </div>
          <div className="bg-gray-900 rounded p-3 relative">
            <pre className="text-green-400 text-xs overflow-x-auto pr-16">{cursorConfig}</pre>
            <button
              onClick={() => copyToClipboard(cursorConfig, 'cursor')}
              className="absolute top-2 right-2 px-2 py-1 bg-gray-700 hover:bg-gray-600 rounded text-xs"
            >
              {copied === 'cursor' ? '✓' : 'Copy'}
            </button>
          </div>
        </div>
      )
    }
  ];

  const featuredPacks = SKILL_PACKS.slice(0, 4);

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
            <Link href="/packs" className="hover:text-purple-400 transition">Skill Packs</Link>
            <Link href="/skills" className="hover:text-purple-400 transition">Skills</Link>
            <Link href="/docs" className="hover:text-purple-400 transition">Docs</Link>
            <a href="https://github.com/bahaaldine/moltler" target="_blank" rel="noopener noreferrer" className="hover:text-purple-400 transition">
              GitHub
            </a>
          </nav>
        </div>
      </header>

      {/* Hero - Compact */}
      <section className="py-10 px-4">
        <div className="container mx-auto text-center max-w-4xl">
          <div className="inline-block px-3 py-1 bg-purple-600/20 text-purple-400 rounded-full text-sm mb-4">
            For Elasticsearch Users
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4">
            Supercharge Your Elasticsearch with{' '}
            <span className="text-purple-400">AI-Ready Skills</span>
          </h1>
          <p className="text-lg text-gray-400 mb-2">
            You already have Elasticsearch. Now give your AI agents the power to query it.
          </p>
          <p className="text-gray-500">
            {totalPacks} Skill Packs • {totalSkills}+ Skills • Works with Cursor, Claude, Cline
          </p>
        </div>
      </section>

      {/* Compact Getting Started */}
      <section className="py-4 px-4">
        <div className="container mx-auto max-w-5xl">
          {/* Horizontal Steps */}
          <div className="flex items-center justify-center gap-2 mb-4 flex-wrap">
            {steps.map((step, idx) => (
              <div key={step.num} className="flex items-center">
                <button
                  onClick={() => setActiveStep(activeStep === step.num ? null : step.num)}
                  className={`flex items-center gap-2 px-4 py-2 rounded-lg transition ${
                    activeStep === step.num
                      ? 'bg-purple-600 text-white'
                      : 'bg-gray-800 hover:bg-gray-700 text-gray-300'
                  }`}
                >
                  <span className={`w-6 h-6 rounded-full flex items-center justify-center text-sm font-bold ${
                    activeStep === step.num ? 'bg-white text-purple-600' : 'bg-gray-700'
                  }`}>
                    {step.num}
                  </span>
                  <span className="font-medium">{step.title}</span>
                </button>
                {idx < steps.length - 1 && (
                  <div className="w-8 h-px bg-gray-700 mx-1 hidden sm:block" />
                )}
              </div>
            ))}
            <div className="w-8 h-px bg-gray-700 mx-1 hidden sm:block" />
            <div className="flex items-center gap-2 px-4 py-2 bg-green-600/20 text-green-400 rounded-lg">
              <span>✓</span>
              <span className="font-medium">Ready!</span>
            </div>
          </div>

          {/* Expandable Step Content */}
          {activeStep && (
            <div className="bg-gray-800 rounded-lg p-4 border border-gray-700 max-w-2xl mx-auto animate-fadeIn">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-semibold">{steps[activeStep - 1].title}</h3>
                <button
                  onClick={() => setActiveStep(null)}
                  className="text-gray-400 hover:text-white"
                >
                  ✕
                </button>
              </div>
              {steps[activeStep - 1].content}
            </div>
          )}

          {/* Collapsed hint */}
          {!activeStep && (
            <p className="text-center text-gray-500 text-sm">
              Click a step above to see instructions
            </p>
          )}
        </div>
      </section>

      {/* Featured Skill Packs */}
      <section className="py-8 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-6xl">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold mb-1">Skill Packs</h2>
              <p className="text-gray-400 text-sm">Complete workflows bundled together</p>
            </div>
            <Link href="/packs" className="text-purple-400 hover:text-purple-300 text-sm">
              View all {totalPacks} packs →
            </Link>
          </div>
          
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
            {featuredPacks.map((pack) => (
              <Link
                key={pack.id}
                href={`/packs/${pack.id}`}
                className="bg-gray-800 rounded-xl p-5 border border-gray-700 hover:border-purple-500 transition group"
              >
                <div className="flex items-start justify-between mb-3">
                  <span className="text-3xl">{pack.icon}</span>
                  <span className="text-xs text-gray-500">v{pack.version}</span>
                </div>
                <h3 className="font-semibold mb-1 group-hover:text-purple-400 transition">{pack.name}</h3>
                <p className="text-gray-400 text-sm mb-3 line-clamp-2">{pack.description}</p>
                <div className="flex items-center justify-between text-xs text-gray-500">
                  <span>{pack.skills.length} skills</span>
                  <span>{pack.installCount?.toLocaleString()} installs</span>
                </div>
              </Link>
            ))}
          </div>

          {/* Pack Categories */}
          <div className="flex items-center justify-center gap-3 mt-6">
            {PACK_CATEGORIES.map((cat) => (
              <Link
                key={cat.id}
                href={`/packs?category=${cat.id}`}
                className="flex items-center gap-2 px-4 py-2 bg-gray-800 hover:bg-gray-700 rounded-lg transition text-sm"
              >
                <span>{cat.icon}</span>
                <span>{cat.name}</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Two Column: Magic Moment + Skills by Category */}
      <section className="py-8 px-4">
        <div className="container mx-auto max-w-6xl">
          <div className="grid lg:grid-cols-2 gap-8">
            {/* Magic Moment - Compact */}
            <div className="bg-gradient-to-r from-purple-900/30 to-gray-800 rounded-xl p-6 border border-purple-700/50">
              <h3 className="text-lg font-semibold mb-4 text-purple-300">What It Looks Like</h3>
              <div className="space-y-4 text-sm">
                <div className="flex gap-3">
                  <span className="text-blue-400">You:</span>
                  <span className="text-gray-300">"Payment service is throwing errors"</span>
                </div>
                <div className="flex gap-3">
                  <span className="text-green-400">Agent:</span>
                  <span className="text-gray-400 text-xs italic">uses get_recent_errors</span>
                </div>
                <div className="bg-gray-900/50 p-3 rounded text-gray-300 text-sm">
                  Found <strong>47 errors</strong>: "Connection timeout to payment gateway"
                </div>
                <div className="flex gap-3">
                  <span className="text-green-400">Agent:</span>
                  <span className="text-gray-400 text-xs italic">uses get_slow_transactions</span>
                </div>
                <div className="bg-gray-900/50 p-3 rounded text-gray-300 text-sm">
                  Gateway calls averaging <strong>12.3s</strong> (normally 1.2s). Started 9:45 AM.
                </div>
              </div>
            </div>

            {/* Individual Skills by Solution */}
            <div>
              <h3 className="text-lg font-semibold mb-4">Or Browse Individual Skills</h3>
              <div className="grid grid-cols-3 gap-3">
                <Link href="/skills?category=observability" className="bg-gray-800 rounded-lg p-4 border border-gray-700 hover:border-purple-500 transition">
                  <div className="text-2xl mb-2">📊</div>
                  <h4 className="font-medium text-sm mb-1">Observability</h4>
                  <p className="text-xs text-gray-500">Logs, APM, metrics</p>
                </Link>
                <Link href="/skills?category=security" className="bg-gray-800 rounded-lg p-4 border border-gray-700 hover:border-purple-500 transition">
                  <div className="text-2xl mb-2">🛡️</div>
                  <h4 className="font-medium text-sm mb-1">Security</h4>
                  <p className="text-xs text-gray-500">Threat hunting, SIEM</p>
                </Link>
                <Link href="/skills?category=search" className="bg-gray-800 rounded-lg p-4 border border-gray-700 hover:border-purple-500 transition">
                  <div className="text-2xl mb-2">🔍</div>
                  <h4 className="font-medium text-sm mb-1">Search</h4>
                  <p className="text-xs text-gray-500">Semantic, aggregations</p>
                </Link>
              </div>
              <div className="mt-4">
                <Link
                  href="/skills"
                  className="inline-block w-full text-center px-4 py-3 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg font-medium transition"
                >
                  Browse All {totalSkills}+ Skills
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Popular Skills - Quick Browse */}
      <section className="py-6 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-6xl">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold">Popular Skills</h3>
            <Link href="/skills" className="text-purple-400 text-sm hover:text-purple-300">
              View all →
            </Link>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {['get_recent_errors', 'hunt_ioc', 'semantic_search', 'get_slow_transactions', 'get_risky_users', 'correlate_logs'].map((skill) => (
              <Link
                key={skill}
                href={`/skills/${skill}`}
                className="bg-gray-800 rounded-lg p-3 border border-gray-700 hover:border-purple-500 transition text-center"
              >
                <code className="text-sm text-purple-400">{skill}</code>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Create Your Own Pack */}
      <section className="py-8 px-4">
        <div className="container mx-auto max-w-4xl">
          <div className="bg-gradient-to-r from-gray-800 to-gray-900 rounded-xl p-6 border border-gray-700">
            <div className="flex flex-col lg:flex-row items-start gap-6">
              <div className="flex-1">
                <h3 className="text-xl font-semibold mb-2">Create Your Own Skill Pack</h3>
                <p className="text-gray-400 text-sm mb-4">
                  Bundle your skills into reusable packs. Share with your team or publish to the Hub.
                </p>
                <a
                  href="https://bahaaldine.github.io/moltler/skills/skill-packs/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 text-purple-400 text-sm hover:text-purple-300"
                >
                  Learn how to create packs →
                </a>
              </div>
              <div className="flex-1 bg-gray-900 rounded-lg p-4 border border-gray-700 w-full">
                <pre className="text-xs overflow-x-auto">
                  <code className="text-green-400">{`CREATE SKILL PACK incident_response
VERSION '1.0.0'
DESCRIPTION 'Complete incident response automation'
AUTHOR 'sre-team'
SKILLS [
    detect_incident@1.0.0,
    triage_incident@1.0.0,
    notify_oncall@2.0.0,
    create_postmortem@1.0.0,
    track_resolution@1.0.0
];`}</code>
                </pre>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Quality Assurance */}
      <section className="py-6 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-4xl">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-4">
              <div className={`flex items-center gap-2 px-4 py-2 rounded-lg border ${
                validationRate === 100 
                  ? 'bg-green-600/20 border-green-600/30' 
                  : 'bg-yellow-600/20 border-yellow-600/30'
              }`}>
                <svg className={`w-5 h-5 ${validationRate === 100 ? 'text-green-400' : 'text-yellow-400'}`} fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                </svg>
                <span className={`font-medium ${validationRate === 100 ? 'text-green-400' : 'text-yellow-400'}`}>
                  {validatedCount}/{totalSkills} Validated ({validationRate}%)
                </span>
              </div>
              <p className="text-gray-400 text-sm">
                All skills pass syntax validation and are tested before release
              </p>
            </div>
            <a
              href="https://github.com/bahaaldine/moltler/actions/workflows/skill-tests.yml"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition"
            >
              <span>View CI/CD</span>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-gray-800 py-6 px-4">
        <div className="container mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-lg">⚡</span>
            </div>
            <span className="font-bold">MoltlerHub</span>
          </div>
          <div className="flex items-center gap-6 text-sm text-gray-400">
            <Link href="/packs" className="hover:text-white transition">Skill Packs</Link>
            <Link href="/skills" className="hover:text-white transition">Skills</Link>
            <a href="https://github.com/bahaaldine/moltler" target="_blank" rel="noopener noreferrer" className="hover:text-white transition">GitHub</a>
            <a href="https://bahaaldine.github.io/moltler/" target="_blank" rel="noopener noreferrer" className="hover:text-white transition">Docs</a>
          </div>
          <div className="text-sm text-gray-500">© 2026 Moltler</div>
        </div>
      </footer>
    </div>
  );
}

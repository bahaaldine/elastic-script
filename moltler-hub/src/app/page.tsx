'use client';

import Link from 'next/link';
import { useState } from 'react';
import { SKILL_PACKS } from '@/lib/packs';
import validatedSkills from '@/data/validated_skills.json';

export default function Home() {
  const [showInstall, setShowInstall] = useState(false);
  const [esUrl, setEsUrl] = useState('http://localhost:9200');
  const [copied, setCopied] = useState(false);
  
  const validationData = validatedSkills as { total_skills: number; valid_skills: number };
  const totalSkills = validationData.total_skills || 188;
  
  // Get starter packs first, then other packs
  const starterPacks = SKILL_PACKS.filter(p => p.isStarter);
  const totalPacks = SKILL_PACKS.length;

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const installCommand = `curl -sSL https://hub.moltler.dev/install.sh | bash -s -- --es-url ${esUrl}`;

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
            <Link href="/builder" className="hover:text-purple-400 transition">Builder</Link>
            <a href="https://bahaaldine.github.io/moltler/" target="_blank" rel="noopener noreferrer" className="hover:text-purple-400 transition">Docs</a>
            <a href="https://github.com/bahaaldine/moltler" target="_blank" rel="noopener noreferrer" className="hover:text-purple-400 transition">GitHub</a>
          </nav>
        </div>
      </header>

      {/* Hero - Skills as Code */}
      <section className="py-16 px-4">
        <div className="container mx-auto text-center max-w-3xl">
          <div className="inline-block px-3 py-1 bg-purple-600/20 text-purple-400 rounded-full text-sm mb-4">
            Skills as Code
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4">
            The Skills Framework for{' '}
            <span className="text-purple-400">Elasticsearch</span>
          </h1>
          <p className="text-xl text-gray-400 mb-2">
            Build, share, and run skills on your data.
          </p>
          <p className="text-gray-500 mb-8">
            Works with Cursor, Claude, Cline, Jupyter, REST API, and more.
          </p>
          
          {/* Primary CTA */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <button
              onClick={() => setShowInstall(!showInstall)}
              className="px-6 py-3 bg-purple-600 hover:bg-purple-500 rounded-lg font-semibold transition flex items-center gap-2"
            >
              Get Started
              <svg className={`w-4 h-4 transition ${showInstall ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            <Link
              href="/skills"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg font-semibold transition"
            >
              Browse {totalSkills} Skills
            </Link>
            <Link
              href="/docs#cursor-setup"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg font-semibold transition flex items-center gap-2"
            >
              <span>🤖</span> Connect to Cursor
            </Link>
          </div>

          {/* Expandable Install */}
          {showInstall && (
            <div className="mt-8 bg-gray-800 rounded-xl p-6 border border-gray-700 text-left max-w-2xl mx-auto">
              <div className="space-y-4">
                <div>
                  <label className="text-sm text-gray-400 block mb-2">1. Your Elasticsearch URL</label>
                  <input
                    type="text"
                    value={esUrl}
                    onChange={(e) => setEsUrl(e.target.value)}
                    className="w-full bg-gray-900 border border-gray-700 rounded px-4 py-2 focus:border-purple-500 focus:outline-none"
                    placeholder="http://localhost:9200"
                  />
                </div>
                <div>
                  <label className="text-sm text-gray-400 block mb-2">2. Install skills</label>
                  <div className="bg-gray-900 rounded p-4 relative">
                    <pre className="text-green-400 text-sm overflow-x-auto pr-16">{installCommand}</pre>
                    <button
                      onClick={() => copyToClipboard(installCommand)}
                      className="absolute top-3 right-3 px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm"
                    >
                      {copied ? '✓ Copied' : 'Copy'}
                    </button>
                  </div>
                </div>
                <div className="pt-2 border-t border-gray-700">
                  <p className="text-sm text-gray-400">
                    3. Try it: <code className="text-purple-400">RUN SKILL what_can_i_do()</code>
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Starter Packs - Role based */}
      <section className="py-12 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-5xl">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold">Choose Your Starter Pack</h2>
              <p className="text-gray-500 text-sm mt-1">Pre-built skill bundles for your role</p>
            </div>
            <Link href="/packs" className="text-purple-400 hover:text-purple-300 text-sm">
              All {totalPacks} packs →
            </Link>
          </div>
          
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
            {starterPacks.map((pack) => (
              <Link
                key={pack.id}
                href={`/packs/${pack.id}`}
                className="bg-gray-800 rounded-xl p-5 border border-gray-700 hover:border-purple-500 transition group"
              >
                <div className="flex items-start justify-between mb-3">
                  <span className="text-3xl">{pack.icon}</span>
                  <span className="text-xs bg-yellow-600/20 text-yellow-400 px-2 py-0.5 rounded">Starter</span>
                </div>
                <h3 className="font-semibold mb-2 group-hover:text-purple-400 transition">{pack.name}</h3>
                <p className="text-gray-400 text-sm line-clamp-2">{pack.description}</p>
                <p className="text-xs text-gray-500 mt-3">{pack.skills.length} skills included</p>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Skills as Code Value Prop */}
      <section className="py-12 px-4">
        <div className="container mx-auto max-w-5xl">
          <div className="grid md:grid-cols-3 gap-6">
            <div className="text-center p-6">
              <div className="text-3xl mb-3">📝</div>
              <h3 className="font-semibold mb-2">Version Controlled</h3>
              <p className="text-sm text-gray-400">Skills are code. Store in Git, review changes, track history.</p>
            </div>
            <div className="text-center p-6">
              <div className="text-3xl mb-3">✅</div>
              <h3 className="font-semibold mb-2">Tested</h3>
              <p className="text-sm text-gray-400">Every skill is validated before release. 100% pass rate.</p>
            </div>
            <div className="text-center p-6">
              <div className="text-3xl mb-3">🔄</div>
              <h3 className="font-semibold mb-2">Portable</h3>
              <p className="text-sm text-gray-400">Same skill works everywhere: AI agents, notebooks, CLI, REST.</p>
            </div>
          </div>
        </div>
      </section>

      {/* Skill Builder CTA */}
      <section className="py-12 px-4">
        <div className="container mx-auto max-w-3xl">
          <div className="bg-gradient-to-r from-purple-900/50 to-purple-800/30 rounded-2xl border border-purple-700 p-8 text-center">
            <div className="inline-block px-3 py-1 bg-purple-600/30 text-purple-300 rounded-full text-sm mb-4">
              New
            </div>
            <h2 className="text-2xl font-bold mb-3">Create Your Own Skill</h2>
            <p className="text-gray-400 mb-6 max-w-lg mx-auto">
              Describe what you need in plain English and get working skill code. No syntax to memorize.
            </p>
            <Link
              href="/builder"
              className="inline-flex items-center gap-2 px-6 py-3 bg-purple-600 hover:bg-purple-500 rounded-lg font-semibold transition"
            >
              Open Skill Builder
              <span className="text-purple-300">✨</span>
            </Link>
          </div>
        </div>
      </section>

      {/* Browse by Category */}
      <section className="py-12 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-5xl">
          <h2 className="text-2xl font-bold mb-6 text-center">Browse by Category</h2>
          
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Link href="/skills?category=observability" className="bg-gray-800 rounded-xl p-6 border border-gray-700 hover:border-purple-500 transition text-center">
              <div className="text-4xl mb-3">📊</div>
              <h3 className="font-semibold mb-1">Observability</h3>
              <p className="text-sm text-gray-500">Logs, APM, metrics</p>
            </Link>
            <Link href="/skills?category=security" className="bg-gray-800 rounded-xl p-6 border border-gray-700 hover:border-purple-500 transition text-center">
              <div className="text-4xl mb-3">🛡️</div>
              <h3 className="font-semibold mb-1">Security</h3>
              <p className="text-sm text-gray-500">Threat hunting, SIEM</p>
            </Link>
            <Link href="/skills?category=search" className="bg-gray-800 rounded-xl p-6 border border-gray-700 hover:border-purple-500 transition text-center">
              <div className="text-4xl mb-3">🔍</div>
              <h3 className="font-semibold mb-1">Search</h3>
              <p className="text-sm text-gray-500">Semantic, aggregations</p>
            </Link>
            <Link href="/skills?category=cluster" className="bg-gray-800 rounded-xl p-6 border border-gray-700 hover:border-purple-500 transition text-center">
              <div className="text-4xl mb-3">⚙️</div>
              <h3 className="font-semibold mb-1">Cluster</h3>
              <p className="text-sm text-gray-500">Health, nodes, shards</p>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-gray-800 py-8 px-4 mt-8">
        <div className="container mx-auto max-w-5xl">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 bg-purple-600 rounded-lg flex items-center justify-center">
                <span className="text-lg">⚡</span>
              </div>
              <div>
                <span className="font-bold">Moltler</span>
                <span className="text-xs text-gray-500 ml-2">The skills framework for Elasticsearch</span>
              </div>
            </div>
            <div className="flex items-center gap-6 text-sm text-gray-400">
              <Link href="/skills" className="hover:text-white transition">Skills</Link>
              <Link href="/packs" className="hover:text-white transition">Packs</Link>
              <Link href="/builder" className="hover:text-white transition">Builder</Link>
              <a href="https://bahaaldine.github.io/moltler/" target="_blank" rel="noopener noreferrer" className="hover:text-white transition">Docs</a>
              <a href="https://github.com/bahaaldine/moltler" target="_blank" rel="noopener noreferrer" className="hover:text-white transition">GitHub</a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

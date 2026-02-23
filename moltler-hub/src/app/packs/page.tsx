'use client';

import Link from 'next/link';
import { useState } from 'react';
import { SKILL_PACKS, PACK_CATEGORIES, SkillPack } from '@/lib/packs';

export default function PacksPage() {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  const filteredPacks = SKILL_PACKS.filter((pack) => {
    const matchesCategory = !selectedCategory || pack.category === selectedCategory;
    const matchesSearch = !searchQuery || 
      pack.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      pack.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      pack.skills.some(s => s.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesCategory && matchesSearch;
  });

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
            <Link href="/packs" className="text-purple-400">Skill Packs</Link>
            <Link href="/skills" className="hover:text-purple-400 transition">Skills</Link>
            <Link href="/docs" className="hover:text-purple-400 transition">Docs</Link>
            <a href="https://github.com/bahaaldine/moltler" target="_blank" rel="noopener noreferrer" className="hover:text-purple-400 transition">
              GitHub
            </a>
          </nav>
        </div>
      </header>

      {/* Hero */}
      <section className="py-10 px-4 border-b border-gray-800">
        <div className="container mx-auto max-w-6xl">
          <h1 className="text-3xl md:text-4xl font-bold mb-3">
            Skill Packs
          </h1>
          <p className="text-gray-400 text-lg mb-6">
            Complete workflows bundled together. Install once, get everything you need.
          </p>

          {/* Search */}
          <div className="flex flex-col md:flex-row gap-4">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search packs..."
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 focus:border-purple-500 focus:outline-none"
            />
            <div className="flex gap-2 flex-wrap">
              <button
                onClick={() => setSelectedCategory(null)}
                className={`px-4 py-2 rounded-lg transition ${
                  !selectedCategory
                    ? 'bg-purple-600 text-white'
                    : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                }`}
              >
                All
              </button>
              {PACK_CATEGORIES.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => setSelectedCategory(cat.id)}
                  className={`flex items-center gap-2 px-4 py-2 rounded-lg transition ${
                    selectedCategory === cat.id
                      ? 'bg-purple-600 text-white'
                      : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                  }`}
                >
                  <span>{cat.icon}</span>
                  <span>{cat.name}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Packs Grid */}
      <section className="py-8 px-4">
        <div className="container mx-auto max-w-6xl">
          <p className="text-gray-400 mb-6">
            {filteredPacks.length} pack{filteredPacks.length !== 1 ? 's' : ''} found
          </p>
          
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredPacks.map((pack) => (
              <PackCard key={pack.id} pack={pack} />
            ))}
          </div>

          {filteredPacks.length === 0 && (
            <div className="text-center py-12">
              <p className="text-gray-400">No packs found matching your criteria.</p>
              <button
                onClick={() => { setSelectedCategory(null); setSearchQuery(''); }}
                className="mt-4 text-purple-400 hover:text-purple-300"
              >
                Clear filters
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Create Your Own */}
      <section className="py-8 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-4xl text-center">
          <h2 className="text-xl font-semibold mb-3">Create Your Own Pack</h2>
          <p className="text-gray-400 mb-4">
            Bundle your skills into reusable packs and share with the community.
          </p>
          <a
            href="https://bahaaldine.github.io/moltler/skills/skill-packs/"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-block px-6 py-3 bg-purple-600 hover:bg-purple-700 rounded-lg font-medium transition"
          >
            Learn How
          </a>
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
          </div>
          <div className="text-sm text-gray-500">© 2026 Moltler</div>
        </div>
      </footer>
    </div>
  );
}

function PackCard({ pack }: { pack: SkillPack }) {
  const categoryColors: Record<string, string> = {
    observability: 'bg-purple-600/20 text-purple-400',
    security: 'bg-red-600/20 text-red-400',
    search: 'bg-blue-600/20 text-blue-400',
    automation: 'bg-green-600/20 text-green-400',
  };

  return (
    <Link
      href={`/packs/${pack.id}`}
      className="bg-gray-800 rounded-xl p-6 border border-gray-700 hover:border-purple-500 transition group"
    >
      <div className="flex items-start justify-between mb-4">
        <span className="text-4xl">{pack.icon}</span>
        <span className={`text-xs px-2 py-1 rounded ${categoryColors[pack.category] || 'bg-gray-700'}`}>
          {pack.category}
        </span>
      </div>
      
      <h3 className="text-xl font-semibold mb-2 group-hover:text-purple-400 transition">
        {pack.name}
      </h3>
      <p className="text-gray-400 text-sm mb-4 line-clamp-2">
        {pack.description}
      </p>

      <div className="mb-4">
        <p className="text-xs text-gray-500 mb-2">Includes {pack.skills.length} skills:</p>
        <div className="flex flex-wrap gap-1">
          {pack.skills.slice(0, 3).map((skill) => (
            <span key={skill} className="text-xs bg-gray-700 px-2 py-1 rounded">
              {skill.split('@')[0]}
            </span>
          ))}
          {pack.skills.length > 3 && (
            <span className="text-xs text-gray-500 px-2 py-1">
              +{pack.skills.length - 3} more
            </span>
          )}
        </div>
      </div>

      <div className="flex items-center justify-between text-xs text-gray-500 pt-4 border-t border-gray-700">
        <span>by {pack.author}</span>
        <div className="flex items-center gap-3">
          <span>v{pack.version}</span>
          <span>{pack.installCount?.toLocaleString()} installs</span>
        </div>
      </div>
    </Link>
  );
}

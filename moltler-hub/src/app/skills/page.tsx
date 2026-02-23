'use client';

import { useState, useMemo, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { SAMPLE_SKILLS, CATEGORIES, searchSkills, filterByCategory, type Skill } from '@/lib/skills';

function SkillsContent() {
  const searchParams = useSearchParams();
  const initialCategory = searchParams.get('category') || '';
  
  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState(initialCategory);
  
  const filteredSkills = useMemo(() => {
    let skills = SAMPLE_SKILLS;
    
    if (selectedCategory) {
      skills = filterByCategory(skills, selectedCategory);
    }
    
    if (query) {
      skills = searchSkills(skills, query);
    }
    
    return skills;
  }, [query, selectedCategory]);

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
            <Link href="/skills" className="text-purple-400">Browse Skills</Link>
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

      <div className="container mx-auto px-4 py-8">
        <div className="flex gap-8">
          {/* Sidebar */}
          <aside className="w-64 flex-shrink-0">
            <div className="sticky top-8">
              <h2 className="text-lg font-semibold mb-4">Categories</h2>
              <ul className="space-y-1">
                <li>
                  <button
                    onClick={() => setSelectedCategory('')}
                    className={`w-full text-left px-3 py-2 rounded-lg transition ${
                      selectedCategory === ''
                        ? 'bg-purple-600 text-white'
                        : 'hover:bg-gray-800 text-gray-400'
                    }`}
                  >
                    All Skills
                  </button>
                </li>
                {Object.entries(CATEGORIES).map(([id, cat]) => (
                  <li key={id}>
                    <button
                      onClick={() => setSelectedCategory(id)}
                      className={`w-full text-left px-3 py-2 rounded-lg transition flex items-center gap-2 ${
                        selectedCategory === id
                          ? 'bg-purple-600 text-white'
                          : 'hover:bg-gray-800 text-gray-400'
                      }`}
                    >
                      <span>{cat.icon}</span>
                      <span>{cat.name}</span>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          </aside>

          {/* Main Content */}
          <main className="flex-1">
            {/* Search */}
            <div className="mb-8">
              <div className="flex items-center bg-gray-800 rounded-lg border border-gray-700 focus-within:border-purple-500 transition">
                <span className="px-4 text-gray-400">🔍</span>
                <input
                  type="text"
                  placeholder="Search skills..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="flex-1 bg-transparent py-3 outline-none text-white"
                />
                {query && (
                  <button
                    onClick={() => setQuery('')}
                    className="px-4 text-gray-400 hover:text-white"
                  >
                    ✕
                  </button>
                )}
              </div>
            </div>

            {/* Results Header */}
            <div className="flex items-center justify-between mb-6">
              <h1 className="text-2xl font-bold">
                {selectedCategory
                  ? CATEGORIES[selectedCategory]?.name || selectedCategory
                  : 'All Skills'}
              </h1>
              <span className="text-gray-400">
                {filteredSkills.length} skill{filteredSkills.length !== 1 ? 's' : ''}
              </span>
            </div>

            {/* Skills Grid */}
            {filteredSkills.length > 0 ? (
              <div className="grid md:grid-cols-2 gap-4">
                {filteredSkills.map((skill) => (
                  <SkillCard key={skill.name} skill={skill} />
                ))}
              </div>
            ) : (
              <div className="text-center py-12">
                <div className="text-4xl mb-4">🔍</div>
                <h3 className="text-xl font-semibold mb-2">No skills found</h3>
                <p className="text-gray-400">
                  Try a different search term or category
                </p>
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}

function SkillCard({ skill }: { skill: Skill }) {
  return (
    <Link
      href={`/skills/${skill.name}`}
      className="p-6 bg-gray-800 rounded-lg hover:bg-gray-750 border border-gray-700 hover:border-purple-500 transition block"
    >
      <div className="flex items-start justify-between mb-3">
        <div>
          <h3 className="font-semibold">{skill.displayName}</h3>
          <p className="text-sm text-gray-500">@elastic/{skill.name}</p>
        </div>
        <span className="text-xs bg-purple-600/20 text-purple-400 px-2 py-1 rounded">
          {CATEGORIES[skill.category]?.icon} {skill.category}
        </span>
      </div>
      <p className="text-sm text-gray-400 mb-4 line-clamp-2">{skill.description}</p>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-xs text-gray-500">
          <span>v{skill.version}</span>
          <span>•</span>
          <span>by {skill.author}</span>
        </div>
        <div className="flex items-center gap-1">
          {skill.tags.slice(0, 2).map((tag) => (
            <span
              key={tag}
              className="text-xs bg-gray-700 text-gray-300 px-2 py-0.5 rounded"
            >
              {tag}
            </span>
          ))}
        </div>
      </div>
    </Link>
  );
}

export default function SkillsPage() {
  return (
    <Suspense fallback={<SkillsLoading />}>
      <SkillsContent />
    </Suspense>
  );
}

function SkillsLoading() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-950 text-white flex items-center justify-center">
      <div className="text-center">
        <div className="text-4xl mb-4">⚡</div>
        <p className="text-gray-400">Loading skills...</p>
      </div>
    </div>
  );
}

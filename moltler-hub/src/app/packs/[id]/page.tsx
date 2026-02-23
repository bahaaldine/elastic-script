'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useState } from 'react';
import { SKILL_PACKS, PACK_CATEGORIES } from '@/lib/packs';
import { SAMPLE_SKILLS } from '@/lib/skills';

export default function PackDetailPage() {
  const params = useParams();
  const packId = params.id as string;
  const pack = SKILL_PACKS.find(p => p.id === packId);
  const [copied, setCopied] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopied(id);
    setTimeout(() => setCopied(null), 2000);
  };

  if (!pack) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-950 text-white flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold mb-4">Pack Not Found</h1>
          <Link href="/packs" className="text-purple-400 hover:text-purple-300">
            ← Back to Packs
          </Link>
        </div>
      </div>
    );
  }

  const installCommand = `moltler pack install ${pack.id}`;
  const categoryInfo = PACK_CATEGORIES.find(c => c.id === pack.category);

  const categoryColors: Record<string, string> = {
    observability: 'bg-purple-600/20 text-purple-400 border-purple-600/50',
    security: 'bg-red-600/20 text-red-400 border-red-600/50',
    search: 'bg-blue-600/20 text-blue-400 border-blue-600/50',
    automation: 'bg-green-600/20 text-green-400 border-green-600/50',
  };

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
          </nav>
        </div>
      </header>

      {/* Breadcrumb */}
      <div className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center gap-2 text-sm text-gray-400">
            <Link href="/packs" className="hover:text-white">Packs</Link>
            <span>/</span>
            <span className="text-white">{pack.name}</span>
          </div>
        </div>
      </div>

      {/* Pack Header */}
      <section className="py-8 px-4 border-b border-gray-800">
        <div className="container mx-auto max-w-4xl">
          <div className="flex items-start gap-6">
            <div className="text-6xl">{pack.icon}</div>
            <div className="flex-1">
              <div className="flex items-center gap-3 mb-2">
                <h1 className="text-3xl font-bold">{pack.name}</h1>
                <span className={`text-xs px-3 py-1 rounded border ${categoryColors[pack.category]}`}>
                  {categoryInfo?.icon} {pack.category}
                </span>
              </div>
              <p className="text-gray-400 text-lg mb-4">{pack.description}</p>
              <div className="flex items-center gap-6 text-sm text-gray-500">
                <span>by <strong className="text-white">{pack.author}</strong></span>
                <span>v{pack.version}</span>
                <span>{pack.installCount?.toLocaleString()} installs</span>
                <span>{pack.skills.length} skills</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Install Section */}
      <section className="py-6 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-4xl">
          <h2 className="text-lg font-semibold mb-3">Install</h2>
          <div className="bg-gray-800 rounded-lg p-4 border border-gray-700 relative">
            <pre className="text-green-400 overflow-x-auto pr-20">{installCommand}</pre>
            <button
              onClick={() => copyToClipboard(installCommand, 'install')}
              className="absolute top-3 right-3 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 rounded text-sm"
            >
              {copied === 'install' ? '✓ Copied' : 'Copy'}
            </button>
          </div>
          <p className="text-gray-500 text-sm mt-3">
            Or install individual skills from the list below.
          </p>
        </div>
      </section>

      {/* Skills List */}
      <section className="py-8 px-4">
        <div className="container mx-auto max-w-4xl">
          <h2 className="text-xl font-semibold mb-4">Included Skills ({pack.skills.length})</h2>
          <div className="space-y-3">
            {pack.skills.map((skill) => {
              const [name, version] = skill.split('@');
              const matchingSkill = SAMPLE_SKILLS.find(s => s.name === name || s.name === name.replace(/_/g, '-'));
              const skillSlug = matchingSkill?.name || name;
              return (
                <div
                  key={skill}
                  className="bg-gray-800 rounded-lg p-4 border border-gray-700 flex items-center justify-between"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 bg-purple-600/20 rounded-lg flex items-center justify-center">
                      <span className="text-purple-400">⚡</span>
                    </div>
                    <div>
                      <code className="text-purple-400 font-medium">{name}</code>
                      <span className="text-gray-500 text-sm ml-2">@{version}</span>
                    </div>
                  </div>
                  {matchingSkill ? (
                    <Link
                      href={`/skills/${skillSlug}`}
                      className="text-sm text-gray-400 hover:text-white transition"
                    >
                      View →
                    </Link>
                  ) : (
                    <span className="text-xs bg-gray-700 text-gray-400 px-2 py-1 rounded">
                      Pack only
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Pack Definition */}
      <section className="py-8 px-4 bg-gray-900/50">
        <div className="container mx-auto max-w-4xl">
          <h2 className="text-xl font-semibold mb-4">Pack Definition</h2>
          <div className="bg-gray-800 rounded-lg p-4 border border-gray-700 relative">
            <pre className="text-green-400 text-sm overflow-x-auto">
{`CREATE SKILL PACK ${pack.id.replace(/-/g, '_')}
VERSION '${pack.version}'
DESCRIPTION '${pack.description}'
AUTHOR '${pack.author}'
SKILLS [
${pack.skills.map(s => `    ${s}`).join(',\n')}
];`}
            </pre>
            <button
              onClick={() => copyToClipboard(`CREATE SKILL PACK ${pack.id.replace(/-/g, '_')}\nVERSION '${pack.version}'\nDESCRIPTION '${pack.description}'\nAUTHOR '${pack.author}'\nSKILLS [\n${pack.skills.map(s => `    ${s}`).join(',\n')}\n];`, 'definition')}
              className="absolute top-3 right-3 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 rounded text-sm"
            >
              {copied === 'definition' ? '✓ Copied' : 'Copy'}
            </button>
          </div>
        </div>
      </section>

      {/* Related Packs */}
      <section className="py-8 px-4">
        <div className="container mx-auto max-w-4xl">
          <h2 className="text-xl font-semibold mb-4">Related Packs</h2>
          <div className="grid md:grid-cols-3 gap-4">
            {SKILL_PACKS.filter(p => p.category === pack.category && p.id !== pack.id)
              .slice(0, 3)
              .map((relatedPack) => (
                <Link
                  key={relatedPack.id}
                  href={`/packs/${relatedPack.id}`}
                  className="bg-gray-800 rounded-lg p-4 border border-gray-700 hover:border-purple-500 transition"
                >
                  <div className="flex items-center gap-3 mb-2">
                    <span className="text-2xl">{relatedPack.icon}</span>
                    <span className="font-medium">{relatedPack.name}</span>
                  </div>
                  <p className="text-gray-400 text-sm line-clamp-2">{relatedPack.description}</p>
                </Link>
              ))}
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
          </div>
          <div className="text-sm text-gray-500">© 2026 Moltler</div>
        </div>
      </footer>
    </div>
  );
}

import Link from 'next/link';
import { SAMPLE_SKILLS, getCategories, CATEGORIES } from '@/lib/skills';

export default function Home() {
  const skills = SAMPLE_SKILLS;
  const categories = getCategories(skills);
  const featuredSkills = skills.slice(0, 6);
  const totalSkills = 155; // From the hub

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

      {/* Hero */}
      <section className="py-20 px-4">
        <div className="container mx-auto text-center">
          <h1 className="text-5xl font-bold mb-6">
            The Skills Marketplace for{' '}
            <span className="text-purple-400">Elasticsearch</span>
          </h1>
          <p className="text-xl text-gray-400 mb-8 max-w-2xl mx-auto">
            Discover, share, and run skills on your Elasticsearch data.
            {totalSkills}+ community-built skills for Observability, Security, and Search.
          </p>
          
          {/* Search */}
          <div className="max-w-xl mx-auto mb-12">
            <Link href="/skills">
              <div className="flex items-center bg-gray-800 rounded-lg border border-gray-700 hover:border-purple-500 transition cursor-pointer">
                <span className="px-4 text-gray-400">🔍</span>
                <input
                  type="text"
                  placeholder="Search skills... (e.g., 'analyze logs', 'hunt threats')"
                  className="flex-1 bg-transparent py-4 outline-none text-white"
                  readOnly
                />
                <span className="px-4 text-gray-500 text-sm">Press Enter</span>
              </div>
            </Link>
          </div>

          {/* Stats */}
          <div className="flex justify-center gap-12 mb-12">
            <div className="text-center">
              <div className="text-4xl font-bold text-purple-400">{totalSkills}+</div>
              <div className="text-gray-400">Skills Available</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-purple-400">{Object.keys(CATEGORIES).length}</div>
              <div className="text-gray-400">Categories</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-purple-400">3</div>
              <div className="text-gray-400">Solutions</div>
            </div>
          </div>

          {/* CTA */}
          <div className="flex justify-center gap-4">
            <Link
              href="/skills"
              className="px-6 py-3 bg-purple-600 hover:bg-purple-700 rounded-lg font-medium transition"
            >
              Browse Skills
            </Link>
            <a
              href="https://github.com/bahaaldine/moltler/blob/main/hub/CONTRIBUTING.md"
              target="_blank"
              rel="noopener noreferrer"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 rounded-lg font-medium transition border border-gray-700"
            >
              Contribute a Skill
            </a>
          </div>
        </div>
      </section>

      {/* Categories */}
      <section className="py-16 px-4 bg-gray-900/50">
        <div className="container mx-auto">
          <h2 className="text-3xl font-bold text-center mb-8">Skill Categories</h2>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {Object.entries(CATEGORIES).slice(0, 8).map(([id, cat]) => (
              <Link
                key={id}
                href={`/skills?category=${id}`}
                className="p-6 bg-gray-800 rounded-lg hover:bg-gray-750 hover:border-purple-500 border border-gray-700 transition"
              >
                <div className="text-3xl mb-2">{cat.icon}</div>
                <h3 className="font-semibold mb-1">{cat.name}</h3>
                <p className="text-sm text-gray-400">{cat.description}</p>
              </Link>
            ))}
          </div>
          <div className="text-center mt-8">
            <Link href="/skills" className="text-purple-400 hover:text-purple-300 transition">
              View all categories →
            </Link>
          </div>
        </div>
      </section>

      {/* Featured Skills */}
      <section className="py-16 px-4">
        <div className="container mx-auto">
          <h2 className="text-3xl font-bold text-center mb-8">Featured Skills</h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {featuredSkills.map((skill) => (
              <Link
                key={skill.name}
                href={`/skills/${skill.name}`}
                className="p-6 bg-gray-800 rounded-lg hover:bg-gray-750 border border-gray-700 hover:border-purple-500 transition"
              >
                <div className="flex items-start justify-between mb-3">
                  <h3 className="font-semibold">{skill.displayName}</h3>
                  <span className="text-xs bg-purple-600/20 text-purple-400 px-2 py-1 rounded">
                    {skill.category}
                  </span>
                </div>
                <p className="text-sm text-gray-400 mb-4 line-clamp-2">{skill.description}</p>
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  <span>v{skill.version}</span>
                  <span>•</span>
                  <span>by {skill.author}</span>
                </div>
              </Link>
            ))}
          </div>
          <div className="text-center mt-8">
            <Link
              href="/skills"
              className="px-6 py-3 bg-gray-800 hover:bg-gray-700 rounded-lg font-medium transition border border-gray-700 inline-block"
            >
              Browse All Skills
            </Link>
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className="py-16 px-4 bg-gray-900/50">
        <div className="container mx-auto">
          <h2 className="text-3xl font-bold text-center mb-12">How It Works</h2>
          <div className="grid md:grid-cols-3 gap-8 max-w-4xl mx-auto">
            <div className="text-center">
              <div className="w-16 h-16 bg-purple-600/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-2xl">1️⃣</span>
              </div>
              <h3 className="font-semibold mb-2">Find a Skill</h3>
              <p className="text-gray-400 text-sm">
                Browse or search for skills that solve your problem
              </p>
            </div>
            <div className="text-center">
              <div className="w-16 h-16 bg-purple-600/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-2xl">2️⃣</span>
              </div>
              <h3 className="font-semibold mb-2">Install It</h3>
              <p className="text-gray-400 text-sm">
                Use the CLI or copy the skill definition to your instance
              </p>
            </div>
            <div className="text-center">
              <div className="w-16 h-16 bg-purple-600/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-2xl">3️⃣</span>
              </div>
              <h3 className="font-semibold mb-2">Run It</h3>
              <p className="text-gray-400 text-sm">
                Execute the skill via REST API or connect to AI agents
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Quick Install */}
      <section className="py-16 px-4">
        <div className="container mx-auto max-w-3xl">
          <h2 className="text-3xl font-bold text-center mb-8">Quick Install</h2>
          <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
            <div className="mb-4">
              <span className="text-gray-400 text-sm">Install all skills:</span>
            </div>
            <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto">
              <code className="text-green-400">
{`git clone https://github.com/bahaaldine/moltler.git
cd moltler/hub
./moltler-cli.sh install --all`}
              </code>
            </pre>
            <div className="mt-4 text-gray-400 text-sm">
              Or install a specific skill:
            </div>
            <pre className="bg-gray-900 p-4 rounded text-sm overflow-x-auto mt-2">
              <code className="text-green-400">
{`./moltler-cli.sh install get-recent-errors`}
              </code>
            </pre>
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

import Link from 'next/link';
import { notFound } from 'next/navigation';
import { SAMPLE_SKILLS, CATEGORIES } from '@/lib/skills';

interface Props {
  params: Promise<{ slug: string }>;
}

export default async function SkillDetailPage({ params }: Props) {
  const { slug } = await params;
  const skill = SAMPLE_SKILLS.find((s) => s.name === slug);

  if (!skill) {
    notFound();
  }

  const category = CATEGORIES[skill.category];

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

      <div className="container mx-auto px-4 py-8">
        {/* Breadcrumb */}
        <nav className="text-sm mb-6">
          <Link href="/skills" className="text-gray-400 hover:text-white transition">
            Skills
          </Link>
          <span className="text-gray-600 mx-2">/</span>
          <Link
            href={`/skills?category=${skill.category}`}
            className="text-gray-400 hover:text-white transition"
          >
            {category?.name || skill.category}
          </Link>
          <span className="text-gray-600 mx-2">/</span>
          <span className="text-white">{skill.displayName}</span>
        </nav>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <div className="lg:col-span-2">
            {/* Header */}
            <div className="mb-8">
              <div className="flex items-center gap-3 mb-4">
                <span className="text-3xl">{category?.icon || '📦'}</span>
                <div>
                  <h1 className="text-3xl font-bold">{skill.displayName}</h1>
                  <p className="text-gray-400">@elastic/{skill.name}</p>
                </div>
              </div>
              <p className="text-lg text-gray-300">{skill.description}</p>
            </div>

            {/* Usage */}
            <section className="mb-8">
              <h2 className="text-xl font-semibold mb-4">Usage</h2>
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <pre className="text-sm overflow-x-auto">
                  <code className="text-green-400">
{`RUN SKILL ${skill.name.replace(/-/g, '_')}()`}
                  </code>
                </pre>
              </div>
              {skill.parameters.length > 0 && (
                <div className="mt-4 bg-gray-800 rounded-lg p-4 border border-gray-700">
                  <p className="text-gray-400 text-sm mb-2">With parameters:</p>
                  <pre className="text-sm overflow-x-auto">
                    <code className="text-green-400">
{`RUN SKILL ${skill.name.replace(/-/g, '_')} WITH ${skill.parameters
  .map((p) => `${p.name} = ${p.type === 'STRING' ? `'value'` : p.default || '0'}`)
  .join(', ')}`}
                    </code>
                  </pre>
                </div>
              )}
            </section>

            {/* Parameters */}
            {skill.parameters.length > 0 && (
              <section className="mb-8">
                <h2 className="text-xl font-semibold mb-4">Parameters</h2>
                <div className="bg-gray-800 rounded-lg border border-gray-700 overflow-hidden">
                  <table className="w-full text-sm">
                    <thead className="bg-gray-900">
                      <tr>
                        <th className="text-left px-4 py-3 text-gray-400 font-medium">Name</th>
                        <th className="text-left px-4 py-3 text-gray-400 font-medium">Type</th>
                        <th className="text-left px-4 py-3 text-gray-400 font-medium">Default</th>
                        <th className="text-left px-4 py-3 text-gray-400 font-medium">Description</th>
                      </tr>
                    </thead>
                    <tbody>
                      {skill.parameters.map((param) => (
                        <tr key={param.name} className="border-t border-gray-700">
                          <td className="px-4 py-3 font-mono text-purple-400">{param.name}</td>
                          <td className="px-4 py-3 text-gray-300">{param.type}</td>
                          <td className="px-4 py-3 text-gray-400">{param.default || '—'}</td>
                          <td className="px-4 py-3 text-gray-300">{param.description}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}

            {/* Returns */}
            <section className="mb-8">
              <h2 className="text-xl font-semibold mb-4">Returns</h2>
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <span className="font-mono text-purple-400">{skill.returns}</span>
                <span className="text-gray-400 ml-2">
                  {skill.returns === 'ARRAY' && '— An array of matching documents'}
                  {skill.returns === 'DOCUMENT' && '— A single document object'}
                </span>
              </div>
            </section>

            {/* Example */}
            <section className="mb-8">
              <h2 className="text-xl font-semibold mb-4">Example</h2>
              <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
                <pre className="text-sm overflow-x-auto">
                  <code className="text-green-400">
{`-- Using curl
curl -u elastic-admin:elastic-password http://localhost:9200/_escript \\
  -H "Content-Type: application/json" \\
  -d '{"query": "RUN SKILL ${skill.name.replace(/-/g, '_')}()"}'

-- Response
{
  "result": [...],
  "_meta": {
    "execution_id": "abc123",
    "duration_ms": 45
  }
}`}
                  </code>
                </pre>
              </div>
            </section>
          </div>

          {/* Sidebar */}
          <aside>
            <div className="sticky top-8 space-y-6">
              {/* Install */}
              <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
                <h3 className="font-semibold mb-4">Install</h3>
                <div className="bg-gray-900 rounded p-3 mb-4">
                  <code className="text-sm text-green-400">
                    ./moltler-cli.sh install {skill.name}
                  </code>
                </div>
                <a
                  href={skill.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full block text-center px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg font-medium transition"
                >
                  View Source
                </a>
              </div>

              {/* Metadata */}
              <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
                <h3 className="font-semibold mb-4">Details</h3>
                <dl className="space-y-3 text-sm">
                  <div>
                    <dt className="text-gray-400">Version</dt>
                    <dd className="font-medium">{skill.version}</dd>
                  </div>
                  <div>
                    <dt className="text-gray-400">Author</dt>
                    <dd className="font-medium">@{skill.author}</dd>
                  </div>
                  <div>
                    <dt className="text-gray-400">Category</dt>
                    <dd>
                      <Link
                        href={`/skills?category=${skill.category}`}
                        className="text-purple-400 hover:text-purple-300"
                      >
                        {category?.icon} {category?.name || skill.category}
                      </Link>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-gray-400">License</dt>
                    <dd className="font-medium">Elastic-2.0</dd>
                  </div>
                </dl>
              </div>

              {/* Tags */}
              <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
                <h3 className="font-semibold mb-4">Tags</h3>
                <div className="flex flex-wrap gap-2">
                  {skill.tags.map((tag) => (
                    <span
                      key={tag}
                      className="text-sm bg-gray-700 text-gray-300 px-3 py-1 rounded-full"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
}

// Generate static params for all skills
export function generateStaticParams() {
  return SAMPLE_SKILLS.map((skill) => ({
    slug: skill.name,
  }));
}

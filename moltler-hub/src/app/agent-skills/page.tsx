import { AGENT_SKILLS, AGENT_SKILL_CATEGORIES } from '@/lib/agent-skills';

export const metadata = {
  title: 'Agent Skills | MoltlerHub',
  description: 'AI Agent Skills following the Cursor Agent Skills standard',
};

export default function AgentSkillsPage() {
  return (
    <main className="min-h-screen bg-gradient-to-b from-gray-900 to-black text-white">
      <div className="max-w-6xl mx-auto px-4 py-16">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold mb-4">
            Agent Skills
          </h1>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            AI Agent Skills following the{' '}
            <a 
              href="https://cursor.com/docs/context/skills" 
              target="_blank" 
              rel="noopener noreferrer"
              className="text-blue-400 hover:underline"
            >
              Cursor Agent Skills standard
            </a>
            . These skills teach AI agents when and how to use Moltler capabilities.
          </p>
        </div>

        {/* Installation */}
        <div className="bg-gray-800 rounded-lg p-6 mb-12">
          <h2 className="text-xl font-semibold mb-4">Installation</h2>
          <p className="text-gray-300 mb-4">
            Agent Skills are automatically discovered from <code className="bg-gray-700 px-2 py-1 rounded">.agents/skills/</code> directory.
            To install in Cursor:
          </p>
          <ol className="list-decimal list-inside space-y-2 text-gray-300">
            <li>Open Cursor Settings (Cmd+Shift+J / Ctrl+Shift+J)</li>
            <li>Navigate to <strong>Rules</strong></li>
            <li>Click <strong>Add Rule</strong> → <strong>Remote Rule (GitHub)</strong></li>
            <li>Enter: <code className="bg-gray-700 px-2 py-1 rounded">https://github.com/bahaaldine/moltler</code></li>
          </ol>
        </div>

        {/* Skills by Category */}
        {Object.entries(AGENT_SKILL_CATEGORIES).map(([categoryId, category]) => (
          <div key={categoryId} className="mb-12">
            <h2 className="text-2xl font-semibold mb-2 flex items-center gap-2">
              <span>{category.icon}</span>
              {category.name}
            </h2>
            <p className="text-gray-400 mb-6">{category.description}</p>
            
            <div className="grid md:grid-cols-2 gap-4">
              {category.skills.map(skillName => {
                const skill = AGENT_SKILLS.find(s => s.name === skillName);
                if (!skill) return null;
                
                return (
                  <div 
                    key={skill.name}
                    className="bg-gray-800 rounded-lg p-6 hover:bg-gray-750 transition-colors"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="text-lg font-semibold text-white">
                        {skill.displayName}
                      </h3>
                      {skill.hasScripts && (
                        <span className="bg-green-600 text-white text-xs px-2 py-1 rounded">
                          Has Scripts
                        </span>
                      )}
                    </div>
                    <p className="text-gray-400 text-sm mb-4">
                      {skill.description}
                    </p>
                    <div className="flex items-center gap-4 text-sm">
                      <a 
                        href={`https://github.com/bahaaldine/moltler/tree/main/${skill.path}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-blue-400 hover:underline"
                      >
                        View on GitHub →
                      </a>
                      {skill.scripts && skill.scripts.length > 0 && (
                        <span className="text-gray-500">
                          Scripts: {skill.scripts.join(', ')}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}

        {/* Three Layer Architecture */}
        <div className="bg-gray-800 rounded-lg p-6 mt-12">
          <h2 className="text-xl font-semibold mb-4">Understanding the Architecture</h2>
          <div className="space-y-4">
            <div className="border-l-4 border-purple-500 pl-4">
              <h3 className="font-semibold text-purple-400">Layer 3: Agent Skills</h3>
              <p className="text-gray-400 text-sm">
                Instructions for AI agents (Cursor, Claude) on when and how to use capabilities.
                Located in <code className="bg-gray-700 px-1 rounded">.agents/skills/</code>
              </p>
            </div>
            <div className="border-l-4 border-blue-500 pl-4">
              <h3 className="font-semibold text-blue-400">Layer 2: Moltler Skills</h3>
              <p className="text-gray-400 text-sm">
                Reusable elastic-script procedures that combine multiple tools.
                Located in <code className="bg-gray-700 px-1 rounded">hub/skills/</code>
              </p>
            </div>
            <div className="border-l-4 border-green-500 pl-4">
              <h3 className="font-semibold text-green-400">Layer 1: Tools</h3>
              <p className="text-gray-400 text-sm">
                Atomic built-in functions (320+) like ES_INDEX, ES_SEARCH.
                Implemented in Java.
              </p>
            </div>
          </div>
          <div className="mt-4">
            <a 
              href="https://github.com/bahaaldine/moltler/blob/main/docs/SKILLS_ARCHITECTURE.md"
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-400 hover:underline text-sm"
            >
              Read full architecture documentation →
            </a>
          </div>
        </div>
      </div>
    </main>
  );
}

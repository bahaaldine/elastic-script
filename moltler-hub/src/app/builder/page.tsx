'use client';

import Link from 'next/link';
import { useState } from 'react';

const EXAMPLE_PROMPTS = [
  "Find all error logs from the last hour and group by service",
  "Get the top 10 slowest API requests",
  "Summarize CPU usage across all hosts",
  "Find failed login attempts by user",
  "List documents with missing required fields",
];

const SKILL_TEMPLATE = `CREATE SKILL my_skill
  VERSION '1.0.0'
  DESCRIPTION 'Description of what this skill does'
  AUTHOR 'your_name'
  TAGS ['category', 'type']
  (
    param_name STRING DESCRIPTION 'Parameter description' DEFAULT 'default'
  )
  RETURNS ARRAY
BEGIN
  DECLARE result ARRAY;
  
  SET result = ESQL_QUERY('FROM logs-* | LIMIT 10');
  
  RETURN result;
END SKILL;`;

export default function SkillBuilder() {
  const [mode, setMode] = useState<'describe' | 'code'>('describe');
  const [description, setDescription] = useState('');
  const [skillName, setSkillName] = useState('');
  const [skillCode, setSkillCode] = useState(SKILL_TEMPLATE);
  const [generatedCode, setGeneratedCode] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [validationResult, setValidationResult] = useState<null | { valid: boolean; errors: string[]; warnings: string[] }>(null);
  const [esUrl, setEsUrl] = useState('http://localhost:9200');
  const [esUser, setEsUser] = useState('elastic-admin');
  const [esPassword, setEsPassword] = useState('elastic-password');
  const [copySuccess, setCopySuccess] = useState(false);
  const [step, setStep] = useState(1);
  const [generationError, setGenerationError] = useState('');
  const [showEsConfig, setShowEsConfig] = useState(false);

  const handleGenerate = async () => {
    setIsGenerating(true);
    setValidationResult(null);
    setGenerationError('');
    
    const name = skillName || 'custom_skill';
    
    try {
      // Call the generate_skill skill via Elasticsearch
      const query = `RUN SKILL generate_skill(
        '${description.replace(/'/g, "''")}',
        '${name}'
      )`;
      
      const response = await fetch(`${esUrl}/_escript`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Basic ' + btoa(`${esUser}:${esPassword}`),
        },
        body: JSON.stringify({ query }),
      });
      
      if (!response.ok) {
        throw new Error(`Elasticsearch returned ${response.status}: ${response.statusText}`);
      }
      
      const result = await response.json();
      
      // Extract generated code from the skill response
      if (result.result?.generated_code) {
        setGeneratedCode(result.result.generated_code);
        setStep(2);
      } else if (result.result?.rows?.[0]?.generated_code) {
        setGeneratedCode(result.result.rows[0].generated_code);
        setStep(2);
      } else {
        // Fallback to local generation if no LLM response
        throw new Error('No generated code in response. Using local template.');
      }
    } catch (error) {
      console.error('Generation error:', error);
      
      // Fallback: Generate locally without LLM
      setGenerationError(`Could not connect to Elasticsearch for AI generation. Using template instead. (${error instanceof Error ? error.message : 'Unknown error'})`);
      
      const generated = `CREATE SKILL ${name}
  VERSION '1.0.0'
  DESCRIPTION '${description}'
  AUTHOR 'user'
  TAGS ['custom', 'generated']
  (
    time_range STRING DESCRIPTION 'Time range to query' DEFAULT '1h',
    limit INTEGER DESCRIPTION 'Maximum results' DEFAULT 100
  )
  RETURNS ARRAY
BEGIN
  DECLARE results ARRAY;
  
  -- TODO: Implement based on: ${description}
  -- Connect to Elasticsearch with inference API for AI-powered generation
  SET results = ESQL_QUERY(
    'FROM logs-* | WHERE @timestamp > NOW() - INTERVAL 1 HOUR | LIMIT ' || limit
  );
  
  RETURN results;
END SKILL;`;
      
      setGeneratedCode(generated);
      setStep(2);
    }
    
    setIsGenerating(false);
  };

  const validateCode = () => {
    const code = mode === 'describe' ? generatedCode : skillCode;
    const errors: string[] = [];
    const warnings: string[] = [];
    
    if (!code.includes('CREATE SKILL')) errors.push('Missing CREATE SKILL statement');
    if (!code.includes('END SKILL')) errors.push('Missing END SKILL statement');
    if (!code.includes('BEGIN')) errors.push('Missing BEGIN statement');
    if (!code.includes('RETURNS')) errors.push('Missing RETURNS clause');
    if (!code.includes('VERSION')) warnings.push('Missing VERSION clause');
    if (!code.includes('DESCRIPTION')) warnings.push('Missing DESCRIPTION clause');
    if (!code.includes('RETURN ')) warnings.push('No RETURN statement found');
    
    setValidationResult({
      valid: errors.length === 0,
      errors,
      warnings
    });
  };

  const copyInstallCommand = () => {
    const code = mode === 'describe' ? generatedCode : skillCode;
    const command = `curl -X POST "${esUrl}/_escript" -H "Content-Type: application/json" -d '{"query": ${JSON.stringify(code)}}'`;
    navigator.clipboard.writeText(command);
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  const copyCode = () => {
    const code = mode === 'describe' ? generatedCode : skillCode;
    navigator.clipboard.writeText(code);
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
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
            <span className="text-xl font-bold">Moltler</span>
          </Link>
          <nav className="flex items-center gap-6">
            <Link href="/skills" className="hover:text-purple-400 transition">Skills</Link>
            <Link href="/packs" className="hover:text-purple-400 transition">Packs</Link>
            <Link href="/builder" className="text-purple-400 font-semibold">Builder</Link>
            <a href="https://bahaaldine.github.io/moltler/" target="_blank" rel="noopener noreferrer" className="hover:text-purple-400 transition">Docs</a>
          </nav>
        </div>
      </header>

      {/* Hero */}
      <section className="py-12 px-4">
        <div className="container mx-auto max-w-4xl text-center">
          <div className="inline-block px-3 py-1 bg-purple-600/20 text-purple-400 rounded-full text-sm mb-4">
            New
          </div>
          <h1 className="text-3xl md:text-4xl font-bold mb-3">
            Skill Builder
          </h1>
          <p className="text-gray-400 mb-8 max-w-2xl mx-auto">
            Create custom skills in seconds. Describe what you need in plain English or write code directly.
          </p>

          {/* Mode Toggle */}
          <div className="inline-flex bg-gray-800 rounded-lg p-1 mb-8">
            <button
              onClick={() => setMode('describe')}
              className={`px-6 py-2 rounded-md text-sm font-medium transition ${
                mode === 'describe' 
                  ? 'bg-purple-600 text-white' 
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              Describe in English
            </button>
            <button
              onClick={() => setMode('code')}
              className={`px-6 py-2 rounded-md text-sm font-medium transition ${
                mode === 'code' 
                  ? 'bg-purple-600 text-white' 
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              Write Code
            </button>
          </div>
        </div>
      </section>

      {/* Builder Content */}
      <section className="px-4 pb-16">
        <div className="container mx-auto max-w-4xl">
          {mode === 'describe' ? (
            /* Natural Language Mode */
            <div className="space-y-6">
              {/* Step 1: Describe */}
              <div className={`bg-gray-800 rounded-xl border ${step === 1 ? 'border-purple-500' : 'border-gray-700'} p-6`}>
                <div className="flex items-center gap-3 mb-4">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${
                    step >= 1 ? 'bg-purple-600' : 'bg-gray-700'
                  }`}>1</div>
                  <h2 className="text-lg font-semibold">Describe what you need</h2>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="text-sm text-gray-400 block mb-2">Skill Name (optional)</label>
                    <input
                      type="text"
                      value={skillName}
                      onChange={(e) => setSkillName(e.target.value.replace(/\s+/g, '_').toLowerCase())}
                      placeholder="e.g., find_error_logs"
                      className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 focus:border-purple-500 focus:outline-none"
                    />
                  </div>
                  
                  <div>
                    <label className="text-sm text-gray-400 block mb-2">What should this skill do?</label>
                    <textarea
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="Describe in plain English what you want the skill to do..."
                      rows={4}
                      className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 focus:border-purple-500 focus:outline-none resize-none"
                    />
                  </div>

                  {/* Example prompts */}
                  <div>
                    <p className="text-sm text-gray-500 mb-2">Try an example:</p>
                    <div className="flex flex-wrap gap-2">
                      {EXAMPLE_PROMPTS.map((prompt, i) => (
                        <button
                          key={i}
                          onClick={() => setDescription(prompt)}
                          className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded-full text-sm text-gray-300 transition"
                        >
                          {prompt}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Elasticsearch Connection */}
                  <div className="border-t border-gray-700 pt-4">
                    <button
                      onClick={() => setShowEsConfig(!showEsConfig)}
                      className="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition"
                    >
                      <svg className={`w-4 h-4 transition ${showEsConfig ? 'rotate-90' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                      Elasticsearch Connection (for AI generation)
                    </button>
                    
                    {showEsConfig && (
                      <div className="mt-3 p-4 bg-gray-900 rounded-lg space-y-3">
                        <p className="text-xs text-gray-500">
                          Connect to your Elasticsearch with the Moltler plugin and an inference endpoint configured for AI-powered generation.
                        </p>
                        <div className="grid grid-cols-2 gap-3">
                          <div className="col-span-2">
                            <label className="text-xs text-gray-400 block mb-1">URL</label>
                            <input
                              type="text"
                              value={esUrl}
                              onChange={(e) => setEsUrl(e.target.value)}
                              placeholder="http://localhost:9200"
                              className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm focus:border-purple-500 focus:outline-none"
                            />
                          </div>
                          <div>
                            <label className="text-xs text-gray-400 block mb-1">Username</label>
                            <input
                              type="text"
                              value={esUser}
                              onChange={(e) => setEsUser(e.target.value)}
                              placeholder="elastic"
                              className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm focus:border-purple-500 focus:outline-none"
                            />
                          </div>
                          <div>
                            <label className="text-xs text-gray-400 block mb-1">Password</label>
                            <input
                              type="password"
                              value={esPassword}
                              onChange={(e) => setEsPassword(e.target.value)}
                              placeholder="••••••••"
                              className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm focus:border-purple-500 focus:outline-none"
                            />
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {generationError && (
                    <div className="p-3 bg-yellow-900/30 border border-yellow-700 rounded-lg">
                      <p className="text-sm text-yellow-300">{generationError}</p>
                    </div>
                  )}

                  <button
                    onClick={handleGenerate}
                    disabled={!description || isGenerating}
                    className="w-full py-3 bg-purple-600 hover:bg-purple-500 disabled:bg-gray-700 disabled:cursor-not-allowed rounded-lg font-semibold transition flex items-center justify-center gap-2"
                  >
                    {isGenerating ? (
                      <>
                        <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                        </svg>
                        Generating with AI...
                      </>
                    ) : (
                      <>
                        Generate Skill
                        <span className="text-purple-300">✨</span>
                      </>
                    )}
                  </button>
                </div>
              </div>

              {/* Step 2: Review */}
              {generatedCode && (
                <div className={`bg-gray-800 rounded-xl border ${step === 2 ? 'border-purple-500' : 'border-gray-700'} p-6`}>
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-3">
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${
                        step >= 2 ? 'bg-purple-600' : 'bg-gray-700'
                      }`}>2</div>
                      <h2 className="text-lg font-semibold">Review & Edit</h2>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={validateCode}
                        className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm transition"
                      >
                        Validate
                      </button>
                      <button
                        onClick={copyCode}
                        className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm transition"
                      >
                        {copySuccess ? '✓ Copied' : 'Copy'}
                      </button>
                    </div>
                  </div>

                  <textarea
                    value={generatedCode}
                    onChange={(e) => setGeneratedCode(e.target.value)}
                    rows={16}
                    className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 font-mono text-sm text-green-400 focus:border-purple-500 focus:outline-none resize-none"
                  />

                  {/* Validation Results */}
                  {validationResult && (
                    <div className={`mt-4 p-4 rounded-lg ${validationResult.valid ? 'bg-green-900/30 border border-green-700' : 'bg-red-900/30 border border-red-700'}`}>
                      <div className="flex items-center gap-2 mb-2">
                        {validationResult.valid ? (
                          <>
                            <span className="text-green-400 text-lg">✓</span>
                            <span className="font-semibold text-green-400">Valid skill</span>
                          </>
                        ) : (
                          <>
                            <span className="text-red-400 text-lg">✗</span>
                            <span className="font-semibold text-red-400">Validation failed</span>
                          </>
                        )}
                      </div>
                      {validationResult.errors.length > 0 && (
                        <ul className="text-sm text-red-300 ml-6 list-disc">
                          {validationResult.errors.map((e, i) => <li key={i}>{e}</li>)}
                        </ul>
                      )}
                      {validationResult.warnings.length > 0 && (
                        <ul className="text-sm text-yellow-300 ml-6 list-disc mt-2">
                          {validationResult.warnings.map((w, i) => <li key={i}>{w}</li>)}
                        </ul>
                      )}
                    </div>
                  )}

                  <button
                    onClick={() => setStep(3)}
                    disabled={validationResult !== null && !validationResult.valid}
                    className="w-full mt-4 py-3 bg-purple-600 hover:bg-purple-500 disabled:bg-gray-700 disabled:cursor-not-allowed rounded-lg font-semibold transition"
                  >
                    Continue to Install
                  </button>
                </div>
              )}

              {/* Step 3: Install */}
              {step >= 3 && (
                <div className="bg-gray-800 rounded-xl border border-purple-500 p-6">
                  <div className="flex items-center gap-3 mb-4">
                    <div className="w-8 h-8 bg-purple-600 rounded-full flex items-center justify-center text-sm font-bold">3</div>
                    <h2 className="text-lg font-semibold">Install Your Skill</h2>
                  </div>

                  <div className="space-y-4">
                    <div>
                      <label className="text-sm text-gray-400 block mb-2">Elasticsearch URL</label>
                      <input
                        type="text"
                        value={esUrl}
                        onChange={(e) => setEsUrl(e.target.value)}
                        className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 focus:border-purple-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <p className="text-sm text-gray-400 mb-2">Run this command:</p>
                      <div className="bg-gray-900 rounded-lg p-4 relative">
                        <pre className="text-green-400 text-sm overflow-x-auto pr-16 whitespace-pre-wrap break-all">
                          {`curl -X POST "${esUrl}/_escript" \\
  -H "Content-Type: application/json" \\
  -d '{"query": "..."}'`}
                        </pre>
                        <button
                          onClick={copyInstallCommand}
                          className="absolute top-3 right-3 px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm"
                        >
                          {copySuccess ? '✓ Copied' : 'Copy Full'}
                        </button>
                      </div>
                    </div>

                    <div className="p-4 bg-purple-900/20 border border-purple-700 rounded-lg">
                      <p className="text-sm text-purple-300">
                        After installation, run your skill with:
                      </p>
                      <code className="text-purple-400 font-mono block mt-2">
                        RUN SKILL {skillName || 'custom_skill'}()
                      </code>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            /* Code Mode */
            <div className="space-y-6">
              <div className="bg-gray-800 rounded-xl border border-gray-700 p-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-semibold">Write Your Skill</h2>
                  <div className="flex gap-2">
                    <button
                      onClick={validateCode}
                      className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm transition"
                    >
                      Validate
                    </button>
                    <button
                      onClick={copyCode}
                      className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm transition"
                    >
                      {copySuccess ? '✓ Copied' : 'Copy'}
                    </button>
                  </div>
                </div>

                <textarea
                  value={skillCode}
                  onChange={(e) => setSkillCode(e.target.value)}
                  rows={20}
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 font-mono text-sm text-green-400 focus:border-purple-500 focus:outline-none resize-none"
                />

                {/* Validation Results */}
                {validationResult && (
                  <div className={`mt-4 p-4 rounded-lg ${validationResult.valid ? 'bg-green-900/30 border border-green-700' : 'bg-red-900/30 border border-red-700'}`}>
                    <div className="flex items-center gap-2 mb-2">
                      {validationResult.valid ? (
                        <>
                          <span className="text-green-400 text-lg">✓</span>
                          <span className="font-semibold text-green-400">Valid skill</span>
                        </>
                      ) : (
                        <>
                          <span className="text-red-400 text-lg">✗</span>
                          <span className="font-semibold text-red-400">Validation failed</span>
                        </>
                      )}
                    </div>
                    {validationResult.errors.length > 0 && (
                      <ul className="text-sm text-red-300 ml-6 list-disc">
                        {validationResult.errors.map((e, i) => <li key={i}>{e}</li>)}
                      </ul>
                    )}
                    {validationResult.warnings.length > 0 && (
                      <ul className="text-sm text-yellow-300 ml-6 list-disc mt-2">
                        {validationResult.warnings.map((w, i) => <li key={i}>{w}</li>)}
                      </ul>
                    )}
                  </div>
                )}
              </div>

              {/* Install Section */}
              <div className="bg-gray-800 rounded-xl border border-gray-700 p-6">
                <h2 className="text-lg font-semibold mb-4">Install</h2>
                
                <div className="space-y-4">
                  <div>
                    <label className="text-sm text-gray-400 block mb-2">Elasticsearch URL</label>
                    <input
                      type="text"
                      value={esUrl}
                      onChange={(e) => setEsUrl(e.target.value)}
                      className="w-full bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 focus:border-purple-500 focus:outline-none"
                    />
                  </div>

                  <button
                    onClick={copyInstallCommand}
                    className="w-full py-3 bg-purple-600 hover:bg-purple-500 rounded-lg font-semibold transition"
                  >
                    {copySuccess ? '✓ Copied Install Command' : 'Copy Install Command'}
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Help Section */}
          <div className="mt-12 text-center">
            <h3 className="text-lg font-semibold mb-4">Need Help?</h3>
            <div className="flex flex-wrap justify-center gap-4">
              <a
                href="https://bahaaldine.github.io/moltler/skills/creating-skills/"
                target="_blank"
                rel="noopener noreferrer"
                className="px-4 py-2 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg text-sm transition"
              >
                Creating Skills Guide
              </a>
              <a
                href="/skills"
                className="px-4 py-2 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg text-sm transition"
              >
                Browse Examples
              </a>
              <Link
                href="/"
                className="px-4 py-2 bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg text-sm transition"
              >
                Back to Home
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

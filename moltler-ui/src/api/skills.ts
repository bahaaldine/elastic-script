// API client for elastic-script backend
// API endpoints are proxied via Vite: /api/* -> http://localhost:9200/_escript/*

const API_BASE = '/api';

export interface Skill {
  name: string;
  type?: 'PROCEDURE' | 'FUNCTION' | 'SKILL';
  version?: string;
  description?: string;
  author?: string;
  tags?: string[];
  return_type?: string;
  procedure?: string;
  application?: string;
  parameters?: Parameter[];
  body?: string;
  documentation?: string;  // Generated skill.md content
  source_code?: string;    // Original source code
  mcp_spec?: Record<string, unknown>;
  created_at?: string;
  updated_at?: string;
}

export interface Parameter {
  name: string;
  type: string;
  mode?: 'IN' | 'OUT' | 'INOUT';
  description?: string;
  required?: boolean;
  default?: unknown;
}

export interface SkillsResponse {
  tools: Record<string, unknown>[];
  skills: Skill[];
  count: number;
}

export interface ExecutionResult {
  success: boolean;
  result?: unknown;
  output?: string;
  error?: string;
  duration_ms?: number;
}

export interface EScriptResponse {
  result?: unknown;
  success?: boolean;
  error?: string;
  message?: string;
}

// Fetch a single skill's full details
async function fetchSkillDetails(name: string): Promise<Skill | null> {
  try {
    const response = await fetch(`${API_BASE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: `SHOW SKILL ${name}` }),
    });
    
    if (!response.ok) return null;
    
    const data = await response.json();
    const s = data.result;
    if (!s) return null;
    
    // Use source_code if available, otherwise generate a representation
    let body = s.source_code as string;
    if (!body) {
      const params = (s.parameters || []).map((p: Record<string, unknown>) => 
        `${p.name} ${p.type}${p.default !== undefined ? ` DEFAULT ${JSON.stringify(p.default)}` : ''}`
      ).join(', ');
      
      body = `CREATE SKILL ${s.name}
  VERSION '1.0'
  DESCRIPTION '${(s.description || '').replace(/'/g, "''")}'
  ${s.author ? `AUTHOR '${s.author}'` : ''}
  ${s.tags?.length ? `TAGS [${s.tags.map((t: string) => `'${t}'`).join(', ')}]` : ''}
  ${params ? `(${params})` : ''}
  ${s.return_type ? `RETURNS ${s.return_type}` : ''}
BEGIN
  -- This skill wraps procedure: ${s.procedure || s.name}
  CALL ${s.procedure || s.name}();
END SKILL;`;
      body = body.replace(/\n\s*\n/g, '\n'); // Remove empty lines
    }

    return {
      name: s.name,
      type: 'SKILL',
      description: s.description,
      return_type: s.return_type,
      procedure: s.procedure,
      version: s.version,
      author: s.author,
      tags: s.tags,
      parameters: (s.parameters || []).map((p: Record<string, unknown>) => ({
        name: p.name as string,
        type: p.type as string,
        description: p.description as string,
        required: p.required as boolean,
        default: p.default,
      })),
      body: body,
      documentation: s.documentation as string,
      source_code: s.source_code as string,
    };
  } catch {
    return null;
  }
}

// Fetch skills using SHOW SKILLS command
export async function fetchSkills(): Promise<Skill[]> {
  try {
    const response = await fetch(`${API_BASE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: 'SHOW SKILLS' }),
    });
    
    if (!response.ok) {
      return [];
    }
    
    const data = await response.json();
    
    // SHOW SKILLS returns { result: { skills: [...], count: n, action: "SHOW SKILLS" } }
    const skillsList = data.result?.skills || [];
    
    // Fetch full details for each skill in parallel
    const detailedSkills = await Promise.all(
      skillsList.map((s: Record<string, unknown>) => fetchSkillDetails(s.name as string))
    );
    
    return detailedSkills.filter((s): s is Skill => s !== null);
  } catch {
    return [];
  }
}

// Fetch a single procedure's full details
async function fetchProcedureDetails(name: string): Promise<Skill | null> {
  try {
    const response = await fetch(`${API_BASE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: `SHOW PROCEDURE ${name}` }),
    });
    
    if (!response.ok) return null;
    
    const data = await response.json();
    const p = data.result;
    if (!p) return null;
    
    // The procedure body is in 'source' field from SHOW PROCEDURE
    const source = p.source || p.definition || p.body || p.procedure || '';
    const body = source.startsWith('CREATE') ? source : `CREATE ${source}`;
    
    return {
      name: p.name || name,
      type: 'PROCEDURE',
      description: p.description || '',
      body: body,
      parameters: parseParameters(p.parameters),
    };
  } catch {
    return null;
  }
}

// Fetch procedures using SHOW PROCEDURES
export async function fetchProcedures(): Promise<Skill[]> {
  try {
    const response = await fetch(`${API_BASE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: 'SHOW PROCEDURES' }),
    });
    
    if (!response.ok) {
      return [];
    }
    
    const data = await response.json();
    
    // SHOW PROCEDURES returns { result: { procedures: [...], count: n } }
    const proceduresList = data.result?.procedures || [];
    
    // Fetch full details for each procedure in parallel
    const detailedProcedures = await Promise.all(
      proceduresList.map((p: Record<string, unknown>) => fetchProcedureDetails(p.name as string))
    );
    
    return detailedProcedures.filter((p): p is Skill => p !== null);
  } catch {
    return [];
  }
}

// Fetch a single function's full details
async function fetchFunctionDetails(name: string): Promise<Skill | null> {
  try {
    const response = await fetch(`${API_BASE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: `SHOW FUNCTION ${name}` }),
    });
    
    if (!response.ok) return null;
    
    const data = await response.json();
    const f = data.result;
    if (!f) return null;
    
    // The function body is in 'source' field from SHOW FUNCTION
    const source = f.source || f.definition || f.body || '';
    const body = source.startsWith('CREATE') ? source : `CREATE ${source}`;
    
    return {
      name: f.name || name,
      type: 'FUNCTION',
      description: f.description || '',
      return_type: f.return_type || f.returnType || '',
      body: body,
      parameters: parseParameters(f.parameters),
    };
  } catch {
    return null;
  }
}

// Fetch functions using SHOW FUNCTIONS
export async function fetchFunctions(): Promise<Skill[]> {
  try {
    const response = await fetch(`${API_BASE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: 'SHOW FUNCTIONS' }),
    });
    
    if (!response.ok) {
      return [];
    }
    
    const data = await response.json();
    
    // SHOW FUNCTIONS returns { result: { functions: [...], count: n } }
    const functionsList = data.result?.functions || [];
    
    // Filter to only user-defined functions and fetch details
    const userFunctions = functionsList.filter(
      (f: Record<string, unknown>) => f.is_builtin !== true && f.isBuiltin !== true
    );
    
    const detailedFunctions = await Promise.all(
      userFunctions.map((f: Record<string, unknown>) => fetchFunctionDetails(f.name as string))
    );
    
    return detailedFunctions.filter((f): f is Skill => f !== null);
  } catch {
    return [];
  }
}

// Parse parameters from various formats
function parseParameters(params: unknown): Parameter[] {
  if (!params) return [];
  if (Array.isArray(params)) {
    return params.map(p => ({
      name: p.name || p.param_name || 'param',
      type: p.type || p.param_type || 'STRING',
      mode: p.mode || 'IN',
      description: p.description,
      required: p.required !== false,
      default: p.default_value || p.default,
    }));
  }
  return [];
}

// Get a single skill by name
export async function fetchSkill(name: string): Promise<Skill> {
  const response = await fetch(`${API_BASE}/skills/${encodeURIComponent(name)}`, {
    headers: {
      'Accept': 'application/json',
    },
  });
  
  if (!response.ok) {
    throw new Error(`Failed to fetch skill: ${response.statusText}`);
  }
  
  return response.json();
}

// Execute a skill with parameters
export async function invokeSkill(name: string, params?: Record<string, unknown>): Promise<ExecutionResult> {
  const response = await fetch(`${API_BASE}/skills/${encodeURIComponent(name)}/_invoke`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
    body: JSON.stringify(params || {}),
  });
  
  if (!response.ok) {
    throw new Error(`Failed to invoke skill: ${response.statusText}`);
  }
  
  return response.json();
}

// Execute raw elastic-script code
export async function executeCode(code: string): Promise<ExecutionResult> {
  const response = await fetch(`${API_BASE}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
    body: JSON.stringify({ query: code }),
  });
  
  const data: EScriptResponse = await response.json();
  
  return {
    success: !data.error,
    result: data.result,
    output: data.message,
    error: data.error,
  };
}

// Create or update a procedure/function/skill
export async function saveProcedure(code: string): Promise<ExecutionResult> {
  return executeCode(code);
}

// Delete a procedure, function, or skill
export async function deleteProcedure(name: string): Promise<ExecutionResult> {
  // Try dropping as each type
  const results = await Promise.allSettled([
    executeCode(`DROP PROCEDURE ${name}`),
    executeCode(`DROP FUNCTION ${name}`),
    executeCode(`DROP SKILL ${name}`),
  ]);
  
  // Return the first successful result
  for (const result of results) {
    if (result.status === 'fulfilled' && result.value.success) {
      return result.value;
    }
  }
  
  return { success: true };
}

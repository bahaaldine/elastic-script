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

// Fetch skills from the skills API (CREATE SKILL)
export async function fetchSkills(): Promise<Skill[]> {
  try {
    const response = await fetch(`${API_BASE}/skills`, {
      headers: {
        'Accept': 'application/json',
      },
    });
    
    if (!response.ok) {
      return [];
    }
    
    const data: SkillsResponse = await response.json();
    return (data.skills || []).map(s => ({
      ...s,
      type: 'SKILL' as const,
    }));
  } catch {
    return [];
  }
}

// Fetch procedures using ESCRIPT_PROCEDURES()
export async function fetchProcedures(): Promise<Skill[]> {
  try {
    const response = await fetch(`${API_BASE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: 'SELECT * FROM ESCRIPT_PROCEDURES()' }),
    });
    
    if (!response.ok) {
      return [];
    }
    
    const data = await response.json();
    
    // The result might be in different formats
    let procedures: Record<string, unknown>[] = [];
    
    if (Array.isArray(data.result)) {
      procedures = data.result;
    } else if (data.result && Array.isArray(data.result.rows)) {
      procedures = data.result.rows;
    } else if (Array.isArray(data)) {
      procedures = data;
    }
    
    return procedures.map((p: Record<string, unknown>) => ({
      name: (p.name as string) || (p.procedure_name as string) || 'unknown',
      type: 'PROCEDURE' as const,
      description: (p.description as string) || '',
      body: (p.body as string) || (p.definition as string) || '',
      parameters: parseParameters(p.parameters || p.params),
      created_at: p.created_at as string,
      updated_at: p.updated_at as string,
    }));
  } catch {
    return [];
  }
}

// Fetch functions using ESCRIPT_FUNCTIONS() - user-defined functions
export async function fetchFunctions(): Promise<Skill[]> {
  try {
    // First try to get stored user-defined functions
    const response = await fetch(`${API_BASE}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: 'SELECT * FROM ESCRIPT_FUNCTIONS()' }),
    });
    
    if (!response.ok) {
      return [];
    }
    
    const data = await response.json();
    
    let functions: Record<string, unknown>[] = [];
    
    if (Array.isArray(data.result)) {
      functions = data.result;
    } else if (data.result && Array.isArray(data.result.rows)) {
      functions = data.result.rows;
    } else if (Array.isArray(data)) {
      functions = data;
    }
    
    // Filter to only show user-defined functions (not built-ins)
    return functions
      .filter((f: Record<string, unknown>) => f.is_builtin !== true && f.isBuiltin !== true)
      .map((f: Record<string, unknown>) => ({
        name: (f.name as string) || 'unknown',
        type: 'FUNCTION' as const,
        description: (f.description as string) || '',
        return_type: (f.return_type as string) || (f.returnType as string) || '',
        body: (f.body as string) || (f.definition as string) || '',
        parameters: parseParameters(f.parameters || f.params),
        created_at: f.created_at as string,
        updated_at: f.updated_at as string,
      }));
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

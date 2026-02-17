// API client for elastic-script backend
// API endpoints are proxied via Vite: /api/* -> http://localhost:9200/_escript/*

const API_BASE = '/api';

export interface Skill {
  name: string;
  type?: 'PROCEDURE' | 'FUNCTION' | 'SKILL';
  description?: string;
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

// Fetch all skills (both from skills API and procedures)
export async function fetchSkills(): Promise<Skill[]> {
  // First try to get procedures (the main source for demo)
  const procedures = await fetchProcedures();
  
  // Then try to get skills from the skills API
  try {
    const response = await fetch(`${API_BASE}/skills`, {
      headers: {
        'Accept': 'application/json',
      },
    });
    
    if (response.ok) {
      const data: SkillsResponse = await response.json();
      const skills = (data.skills || []).map(s => ({
        ...s,
        type: 'SKILL' as const,
      }));
      
      // Combine and dedupe by name
      const combined = [...procedures];
      for (const skill of skills) {
        if (!combined.find(p => p.name === skill.name)) {
          combined.push(skill);
        }
      }
      return combined;
    }
  } catch {
    // Skills API might not have data, that's ok
  }
  
  return procedures;
}

// Fetch procedures using ESCRIPT_PROCEDURES()
async function fetchProcedures(): Promise<Skill[]> {
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

// List all procedures (stored procedures in the system)
export async function listProcedures(): Promise<Skill[]> {
  const result = await executeCode('CALL ESCRIPT_PROCEDURES()');
  if (result.success && Array.isArray(result.result)) {
    return result.result.map((p: Record<string, unknown>) => ({
      name: p.name as string,
      description: p.description as string,
      procedure: p.name as string,
      parameters: [],
    }));
  }
  return [];
}

// Create or update a procedure
export async function saveProcedure(code: string): Promise<ExecutionResult> {
  return executeCode(code);
}

// Delete a procedure
export async function deleteProcedure(name: string): Promise<ExecutionResult> {
  return executeCode(`DROP PROCEDURE ${name}`);
}

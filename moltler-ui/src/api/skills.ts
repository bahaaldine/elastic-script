// API client for elastic-script backend
// API endpoints are proxied via Vite: /api/* -> http://localhost:9200/_escript/*

const API_BASE = '/api';

export interface Skill {
  name: string;
  type?: 'PROCEDURE' | 'FUNCTION';
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

// Fetch all skills
export async function fetchSkills(): Promise<Skill[]> {
  const response = await fetch(`${API_BASE}/skills`, {
    headers: {
      'Accept': 'application/json',
    },
  });
  
  if (!response.ok) {
    throw new Error(`Failed to fetch skills: ${response.statusText}`);
  }
  
  const data: SkillsResponse = await response.json();
  return data.skills || [];
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

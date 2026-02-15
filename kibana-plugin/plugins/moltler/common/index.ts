export const PLUGIN_ID = 'moltler';
export const PLUGIN_NAME = 'Moltler';

/**
 * Skill definition as stored in .escript_skills index
 */
export interface SkillDefinition {
  name: string;
  version: string;
  description: string;
  author: string;
  tags: string[];
  parameters: SkillParameter[];
  returnType: string;
  procedureName: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface SkillParameter {
  name: string;
  type: string;
  mode: 'IN' | 'OUT' | 'INOUT';
  defaultValue?: string;
  description?: string;
}

/**
 * API response types
 */
export interface SkillsListResponse {
  skills: SkillDefinition[];
  count: number;
}

export interface SkillExecutionResponse {
  success: boolean;
  result?: unknown;
  error?: string;
  executionTime?: number;
}

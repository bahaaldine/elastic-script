import type { HttpSetup } from '@kbn/core/public';
import type { SkillDefinition, SkillsListResponse, SkillExecutionResponse } from '../../common';

/**
 * API service for Moltler plugin
 */
export class MoltlerApiService {
  constructor(private readonly http: HttpSetup) {}

  /**
   * Fetch all skills
   */
  async getSkills(): Promise<SkillsListResponse> {
    return this.http.get('/api/moltler/skills');
  }

  /**
   * Fetch a specific skill by name
   */
  async getSkill(name: string): Promise<{ skill: SkillDefinition }> {
    return this.http.get(`/api/moltler/skills/${name}`);
  }

  /**
   * Run a skill
   */
  async runSkill(
    name: string,
    parameters?: Record<string, unknown>
  ): Promise<SkillExecutionResponse> {
    return this.http.post(`/api/moltler/skills/${name}/run`, {
      body: JSON.stringify({ parameters: parameters || {} }),
    });
  }

  /**
   * Get available indices
   */
  async getIndices(pattern?: string): Promise<{ indices: any[]; count: number }> {
    const query = pattern ? `?pattern=${encodeURIComponent(pattern)}` : '';
    return this.http.get(`/api/moltler/indices${query}`);
  }
}

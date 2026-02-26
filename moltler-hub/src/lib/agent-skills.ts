// Agent Skills data (following Cursor Agent Skills standard)

export interface AgentSkill {
  name: string;
  displayName: string;
  description: string;
  path: string;
  hasScripts: boolean;
  scripts?: string[];
}

// Agent Skills available in .agents/skills/
export const AGENT_SKILLS: AgentSkill[] = [
  {
    name: 'moltler-index',
    displayName: 'Moltler Index',
    description: 'Master index of all Moltler/elastic-script capabilities. Use this skill first to understand what skills are available.',
    path: '.agents/skills/moltler-index',
    hasScripts: false,
  },
  {
    name: 'elasticsearch-ops',
    displayName: 'Elasticsearch Operations',
    description: 'Perform Elasticsearch operations including document CRUD, index management, and bulk operations.',
    path: '.agents/skills/elasticsearch-ops',
    hasScripts: true,
    scripts: ['bulk_reindex.sql'],
  },
  {
    name: 'search-query',
    displayName: 'Search & Query',
    description: 'Execute searches, aggregations, and queries against Elasticsearch.',
    path: '.agents/skills/search-query',
    hasScripts: false,
  },
  {
    name: 'cluster-management',
    displayName: 'Cluster Management',
    description: 'Monitor and manage Elasticsearch cluster health, nodes, tasks, and settings.',
    path: '.agents/skills/cluster-management',
    hasScripts: true,
    scripts: ['health_check.sql'],
  },
  {
    name: 'observability',
    displayName: 'Observability',
    description: 'Analyze logs, metrics, traces, and APM data in Elasticsearch.',
    path: '.agents/skills/observability',
    hasScripts: true,
    scripts: ['error_summary.sql'],
  },
  {
    name: 'security-ops',
    displayName: 'Security Operations',
    description: 'Manage Elasticsearch security including users, roles, API keys, and security features.',
    path: '.agents/skills/security-ops',
    hasScripts: false,
  },
  {
    name: 'ml-inference',
    displayName: 'Machine Learning & Inference',
    description: 'Use machine learning and AI capabilities including ML jobs, trained models, embeddings, and LLM integration.',
    path: '.agents/skills/ml-inference',
    hasScripts: false,
  },
  {
    name: 'data-management',
    displayName: 'Data Management',
    description: 'Manage data lifecycle including ILM policies, data streams, snapshots, and ingest pipelines.',
    path: '.agents/skills/data-management',
    hasScripts: false,
  },
  {
    name: 'alerting-response',
    displayName: 'Alerting & Incident Response',
    description: 'Manage alerts, create alert rules, and respond to incidents with automated actions.',
    path: '.agents/skills/alerting-response',
    hasScripts: false,
  },
  {
    name: 'integrations',
    displayName: 'Integrations',
    description: 'Integrate with external services including AWS, Kubernetes, CI/CD, Terraform, and webhooks.',
    path: '.agents/skills/integrations',
    hasScripts: false,
  },
];

// Categories for Agent Skills
export const AGENT_SKILL_CATEGORIES = {
  'discovery': {
    name: 'Discovery',
    description: 'Find the right skill for your task',
    icon: '🔍',
    skills: ['moltler-index'],
  },
  'data-operations': {
    name: 'Data Operations',
    description: 'Work with Elasticsearch data',
    icon: '📦',
    skills: ['elasticsearch-ops', 'search-query', 'data-management'],
  },
  'infrastructure': {
    name: 'Infrastructure',
    description: 'Manage clusters and infrastructure',
    icon: '🖥️',
    skills: ['cluster-management', 'security-ops'],
  },
  'monitoring': {
    name: 'Monitoring & Response',
    description: 'Observe and respond to issues',
    icon: '👁️',
    skills: ['observability', 'alerting-response'],
  },
  'ai-automation': {
    name: 'AI & Automation',
    description: 'ML, AI, and external integrations',
    icon: '🤖',
    skills: ['ml-inference', 'integrations'],
  },
};

export function getAgentSkillByName(name: string): AgentSkill | undefined {
  return AGENT_SKILLS.find(s => s.name === name);
}

export function getAgentSkillsByCategory(categoryId: string): AgentSkill[] {
  const category = AGENT_SKILL_CATEGORIES[categoryId as keyof typeof AGENT_SKILL_CATEGORIES];
  if (!category) return [];
  return category.skills
    .map(name => AGENT_SKILLS.find(s => s.name === name))
    .filter((s): s is AgentSkill => s !== undefined);
}

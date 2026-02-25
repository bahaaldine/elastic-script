// Skills data fetching and types

export interface Skill {
  name: string;
  displayName: string;
  description: string;
  version: string;
  author: string;
  category: string;
  tags: string[];
  parameters: Parameter[];
  returns: string;
  sourceUrl: string;
  readmeUrl?: string;
}

export interface Parameter {
  name: string;
  type: string;
  description: string;
  default?: string;
  required: boolean;
}

export interface Category {
  id: string;
  name: string;
  description: string;
  icon: string;
  count: number;
}

// Categories with metadata
export const CATEGORIES: Record<string, { name: string; description: string; icon: string }> = {
  observability: {
    name: 'Observability',
    description: 'Logs, metrics, APM, and monitoring skills',
    icon: '👁️',
  },
  security: {
    name: 'Security',
    description: 'Threat hunting, SIEM, and risk assessment',
    icon: '🛡️',
  },
  search: {
    name: 'Search',
    description: 'Document search, aggregations, and analytics',
    icon: '🔍',
  },
  apm: {
    name: 'APM',
    description: 'Application performance monitoring and tracing',
    icon: '📊',
  },
  metrics: {
    name: 'Metrics',
    description: 'Infrastructure and system metrics',
    icon: '📈',
  },
  cluster: {
    name: 'Cluster',
    description: 'Elasticsearch cluster management',
    icon: '🖥️',
  },
  ml: {
    name: 'Machine Learning',
    description: 'ML jobs, anomaly detection, and inference',
    icon: '🤖',
  },
  alerting: {
    name: 'Alerting',
    description: 'Alert rules, connectors, and notifications',
    icon: '🔔',
  },
  fleet: {
    name: 'Fleet',
    description: 'Elastic Agent and Fleet management',
    icon: '🚀',
  },
  integrations: {
    name: 'Integrations',
    description: 'External service integrations',
    icon: '🔗',
  },
  'enterprise-search': {
    name: 'Enterprise Search',
    description: 'Search applications and analytics',
    icon: '🏢',
  },
  'agent-builder': {
    name: 'Agent Builder',
    description: 'AI agent creation and management',
    icon: '🤖',
  },
  meta: {
    name: 'Meta',
    description: 'Skills about skills',
    icon: '🔧',
  },
  workflows: {
    name: 'Workflows',
    description: 'Elastic Workflows automation and management',
    icon: '⚡',
  },
  kibana: {
    name: 'Kibana',
    description: 'Kibana API management - alerting, cases, dashboards, Fleet, ML, and more',
    icon: '🎯',
  },
  sfdc: {
    name: 'Salesforce',
    description: 'Salesforce CRM - accounts, opportunities, cases, analytics, and AI',
    icon: '☁️',
  },
  'first-class-commands': {
    name: 'First-Class Commands',
    description: 'Native Elasticsearch commands - INDEX, SEARCH, REFRESH, DELETE, CREATE INDEX',
    icon: '⚡',
  },
};

// Parse skill.yaml content
export function parseSkillYaml(content: string): Partial<Skill> {
  const lines = content.split('\n');
  const skill: Record<string, unknown> = {};
  
  for (const line of lines) {
    const match = line.match(/^(\w+):\s*(.*)$/);
    if (match) {
      const [, key, value] = match;
      if (value.startsWith("'") || value.startsWith('"')) {
        skill[key] = value.slice(1, -1);
      } else if (value.startsWith('[')) {
        // Simple array parsing
        skill[key] = value.slice(1, -1).split(',').map(s => s.trim().replace(/['"]/g, ''));
      } else {
        skill[key] = value;
      }
    }
  }
  
  return skill as Partial<Skill>;
}

// Fetch skills from GitHub
export async function fetchSkillsFromGitHub(): Promise<Skill[]> {
  const baseUrl = 'https://api.github.com/repos/bahaaldine/moltler/contents/hub/skills/elastic';
  
  try {
    const response = await fetch(baseUrl, {
      headers: {
        'Accept': 'application/vnd.github.v3+json',
      },
      next: { revalidate: 3600 }, // Cache for 1 hour
    });
    
    if (!response.ok) {
      console.error('Failed to fetch from GitHub:', response.status);
      return [];
    }
    
    const categories = await response.json();
    const skills: Skill[] = [];
    
    for (const category of categories) {
      if (category.type !== 'dir') continue;
      
      const categoryResponse = await fetch(category.url, {
        headers: { 'Accept': 'application/vnd.github.v3+json' },
        next: { revalidate: 3600 },
      });
      
      if (!categoryResponse.ok) continue;
      
      const skillDirs = await categoryResponse.json();
      
      for (const skillDir of skillDirs) {
        if (skillDir.type !== 'dir') continue;
        
        // Create skill from directory name
        const skill: Skill = {
          name: skillDir.name,
          displayName: skillDir.name.replace(/-/g, ' ').replace(/\b\w/g, (l: string) => l.toUpperCase()),
          description: `${skillDir.name.replace(/-/g, ' ')} skill`,
          version: '1.0.0',
          author: 'elastic',
          category: category.name,
          tags: [category.name],
          parameters: [],
          returns: 'ARRAY',
          sourceUrl: `https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/${category.name}/${skillDir.name}`,
        };
        
        skills.push(skill);
      }
    }
    
    return skills;
  } catch (error) {
    console.error('Error fetching skills:', error);
    return [];
  }
}

// Import generated skills data
import skillsData from '@/data/skills.json';

// Get skills from local data (for static generation)
export function getLocalSkills(): Skill[] {
  return skillsData as Skill[];
}

// All skills from the hub
export const SAMPLE_SKILLS: Skill[] = skillsData as Skill[];

// Legacy sample for reference
export const SAMPLE_SKILLS_LEGACY: Skill[] = [
  {
    name: 'get-recent-errors',
    displayName: 'Get Recent Errors',
    description: 'Get recent ERROR level logs from the specified index pattern. Use this to quickly identify errors in your applications.',
    version: '1.0.0',
    author: 'elastic',
    category: 'observability',
    tags: ['logs', 'errors', 'troubleshooting'],
    parameters: [
      { name: 'index_pattern', type: 'STRING', description: 'Index pattern to search', default: 'logs-*', required: false },
      { name: 'limit', type: 'INT', description: 'Maximum number of results', default: '20', required: false },
    ],
    returns: 'ARRAY',
    sourceUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/get-recent-errors',
  },
  {
    name: 'count-logs-by-level',
    displayName: 'Count Logs by Level',
    description: 'Count logs grouped by log level (ERROR, WARN, INFO, DEBUG). Great for understanding log distribution.',
    version: '1.0.0',
    author: 'elastic',
    category: 'observability',
    tags: ['logs', 'aggregation', 'analytics'],
    parameters: [
      { name: 'index_pattern', type: 'STRING', description: 'Index pattern to search', default: 'logs-*', required: false },
    ],
    returns: 'ARRAY',
    sourceUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/observability/count-logs-by-level',
  },
  {
    name: 'hunt-ioc',
    displayName: 'Hunt IOC',
    description: 'Search for indicators of compromise (IOC) like IP addresses, hashes, or domains across security data.',
    version: '1.0.0',
    author: 'elastic',
    category: 'security',
    tags: ['threat-hunting', 'ioc', 'siem'],
    parameters: [
      { name: 'ioc', type: 'STRING', description: 'IOC to search for (IP, hash, domain)', required: true },
      { name: 'ioc_type', type: 'STRING', description: 'Type of IOC', default: 'auto', required: false },
    ],
    returns: 'ARRAY',
    sourceUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/hunt-ioc',
  },
  {
    name: 'get-risky-users',
    displayName: 'Get Risky Users',
    description: 'Get users with risk scores above the specified threshold. Identify potentially compromised accounts.',
    version: '1.0.0',
    author: 'elastic',
    category: 'security',
    tags: ['risk', 'users', 'ueba'],
    parameters: [
      { name: 'threshold', type: 'INT', description: 'Minimum risk score', default: '50', required: false },
    ],
    returns: 'ARRAY',
    sourceUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/security/get-risky-users',
  },
  {
    name: 'semantic-search',
    displayName: 'Semantic Search',
    description: 'Search documents by meaning using vector embeddings. Find relevant content even without exact keyword matches.',
    version: '1.0.0',
    author: 'elastic',
    category: 'search',
    tags: ['vector', 'embeddings', 'ai'],
    parameters: [
      { name: 'query', type: 'STRING', description: 'Search query', required: true },
      { name: 'index', type: 'STRING', description: 'Index to search', required: true },
    ],
    returns: 'ARRAY',
    sourceUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/search/semantic-search',
  },
  {
    name: 'get-slow-transactions',
    displayName: 'Get Slow Transactions',
    description: 'Find APM transactions that exceed a latency threshold. Identify performance bottlenecks.',
    version: '1.0.0',
    author: 'elastic',
    category: 'apm',
    tags: ['performance', 'latency', 'transactions'],
    parameters: [
      { name: 'service', type: 'STRING', description: 'Service name', required: false },
      { name: 'threshold_ms', type: 'INT', description: 'Latency threshold in ms', default: '1000', required: false },
    ],
    returns: 'ARRAY',
    sourceUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/apm/get-slow-transactions',
  },
  {
    name: 'get-cluster-health',
    displayName: 'Get Cluster Health',
    description: 'Check Elasticsearch cluster health status including node count and shard allocation.',
    version: '1.0.0',
    author: 'elastic',
    category: 'cluster',
    tags: ['health', 'monitoring', 'admin'],
    parameters: [],
    returns: 'DOCUMENT',
    sourceUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/cluster/get-cluster-health',
  },
  {
    name: 'get-anomalies',
    displayName: 'Get Anomalies',
    description: 'Get anomaly detection results from ML jobs. Find unusual patterns in your data.',
    version: '1.0.0',
    author: 'elastic',
    category: 'ml',
    tags: ['anomaly', 'detection', 'machine-learning'],
    parameters: [
      { name: 'job_id', type: 'STRING', description: 'ML job ID', required: true },
      { name: 'severity', type: 'INT', description: 'Minimum anomaly score', default: '50', required: false },
    ],
    returns: 'ARRAY',
    sourceUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic/ml/get-anomalies',
  },
];

// Get all unique categories with counts
export function getCategories(skills: Skill[]): Category[] {
  const categoryCounts: Record<string, number> = {};
  
  for (const skill of skills) {
    categoryCounts[skill.category] = (categoryCounts[skill.category] || 0) + 1;
  }
  
  return Object.entries(categoryCounts).map(([id, count]) => ({
    id,
    name: CATEGORIES[id]?.name || id,
    description: CATEGORIES[id]?.description || '',
    icon: CATEGORIES[id]?.icon || '📦',
    count,
  }));
}

// Search skills
export function searchSkills(skills: Skill[], query: string): Skill[] {
  const lowerQuery = query.toLowerCase();
  return skills.filter(skill =>
    skill.name.toLowerCase().includes(lowerQuery) ||
    skill.displayName.toLowerCase().includes(lowerQuery) ||
    skill.description.toLowerCase().includes(lowerQuery) ||
    skill.tags.some(tag => tag.toLowerCase().includes(lowerQuery))
  );
}

// Filter skills by category
export function filterByCategory(skills: Skill[], category: string): Skill[] {
  return skills.filter(skill => skill.category === category);
}
